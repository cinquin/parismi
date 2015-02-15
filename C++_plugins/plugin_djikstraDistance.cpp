/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_djikstraDistance.h"
using namespace boost;
using namespace std;

/*!
 * Given segmentation image I, returns a 2D matrix neighborMatrix with following properties
 *   1) neighborMatrix(i,j,0) gives the connectivity between protobuf seeds i,j.
 *      neighborMatrix(i,j,0) is 1 if protobuf seeds i,j are neighbors, 0 if not
 */
static void getNeighborMatrix_surfaceAreaMethod(Image3D<int> *I,
		Image3D<int> *neighborMatrix, float connectivityThreshold,
		CallbackFunctions *cb) {

	// get number of cells
	int numCells = I->max();

	// initialize storage of cell surface area
	Array1D<int> surfaceArea(cb);
	surfaceArea.resize(numCells);

	// initialize storage of connected pixels.  connectedPixelCount(i,j) gives the number of pixels from segmentation i that are touching pixels from segmentation j
	Image3D<int> connectedPixelCount(cb);
	connectedPixelCount.resize(numCells, numCells, 1);

	// storage vector for previously counted pixels
	Array1D<int> previouslyCountedPixels(cb);
	previouslyCountedPixels.resize(
			(2 * PIXEL_SEARCH_RANGE + 1) * (2 * PIXEL_SEARCH_RANGE + 1) * (2
					* PIXEL_SEARCH_RANGE + 1));

	// calculate connectedPixelCount
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	for (int x = 1; x < dimx - 1; x++) {
		for (int y = 1; y < dimy - 1; y++) {
			for (int z = 1; z < dimz - 1; z++) {
				int source = (*I)(x, y, z);
				int sourceIdx = source - 1;
				if (source == 0) {
					continue;
				}
				bool isPerimeter = false;
				int counter = 0;
				for (int dx = -PIXEL_SEARCH_RANGE; dx <= PIXEL_SEARCH_RANGE; dx++) {
					for (int dy = -PIXEL_SEARCH_RANGE; dy <= PIXEL_SEARCH_RANGE; dy++) {
						for (int dz = -PIXEL_SEARCH_RANGE; dz
								<= PIXEL_SEARCH_RANGE; dz++) {
							// check neighboring pixel
							int neighbor = (*I)(x + dx, y + dy, z + dz);
							int neighborIdx = neighbor - 1;

							// determine if source pixel is a perimeter pixel
							if (neighbor != source && !isPerimeter) {
								surfaceArea(sourceIdx)++;
								isPerimeter = true;
							}

							// check if neighbor belongs to a different segmentation
							if (neighbor != source && neighbor != 0) {
								// check if we have counted neighbor before
								bool countedNeighborBefore = false;
								for (int i = 0; i < counter; i++) {
									if (neighbor == previouslyCountedPixels(i)) {
										countedNeighborBefore = true;
										break;
									}
								}
								// if we have not counted the neighbor before, then increment connected component count.  Also record neighbor as counted
								if (!countedNeighborBefore) {
									connectedPixelCount(sourceIdx, neighborIdx,
											0)++;
									previouslyCountedPixels(counter) = neighbor;
									counter++;
								}
							}
						}
					}
				}
			}
		}
	}

	// generate neighborMatrix from connectedPixelCount
	neighborMatrix->resize(numCells, numCells, 1);
	for (int i = 0; i < numCells; i++) {
		for (int j = 0; j < numCells; j++) {
			float sa = (float) surfaceArea(i);
			float numTouchingPixels = (float) connectedPixelCount(i, j, 0);
			if (numTouchingPixels / sa > connectivityThreshold) {
				(*neighborMatrix)(i, j, 0) = 1;
			}
		}
	}

}

/*!
 * source is the protobuf index of the source node
 */
static void djikstra(Image3D<int> *neighborMatrix, Array1D<float> *source,
		Array1D<int> *dist, int initialDistance) {

	// initialize vectors dist (node distances) and Q (node indices)
	int numV, dummy1, dummy2;
	neighborMatrix->getDimensions(numV, dummy1, dummy2);
	dist->resize(numV);
	dist->fill(INF);

	for (int i = 0; i < source->size(); i++) {
		float annotationValue = (*source)(i);
		if (annotationValue > BWTHRESH) {
			(*dist)(i) = initialDistance;
		}
	}

	vector<int> Q;
	for (int i = 0; i < numV; i++) {
		Q.push_back(i);
	}

	while (Q.size() != 0) {
		// find vertex with smallest distance
		int minDist = INF;
		int Q_idx = -1;
		int n_idx = -1;
		for (unsigned int i = 0; i < Q.size(); i++) {
			int nodeIdx = Q[i];
			int d = (*dist)(nodeIdx);
			if (d < minDist) {
				Q_idx = (int) i;
				n_idx = nodeIdx;
				minDist = d;
			}
		}

		// stop condition
		if (minDist == INF) {
			break;
		}

		// remove vertex from Q
		Q.erase(Q.begin() + Q_idx);

		// find neighbors of removed vertex
		vector<int> neighborIndices;
		for (int i = 0; i < numV; i++) {
			if ((*neighborMatrix)(n_idx, i, 0) == 1) {
				neighborIndices.push_back(i);
			}
		}

		// set distance for neighbors
		for (unsigned int i = 0; i < neighborIndices.size(); i++) {
			int potentialDistance = minDist + 1;
			if (potentialDistance < (*dist)(neighborIndices[i])) {
				(*dist)(neighborIndices[i]) = potentialDistance;
			}
		}
	}

}

/*!
 * Returns 1 if a row1 annotation is found
 * Returns 0 if a dtc annotation is found
 * Returns 1 if left/right annotation found
 * sourceSeedAnnotations updated to contain row1/dtc/leftRight annotations
 */
static int identifySourceSeed(Protobuf *proto, vector<string> *dtcFieldnames,
		vector<string> *row1Fieldnames, CallbackFunctions *cb,
		Array1D<float> *sourceSeedAnnotations) {
	// check if we should define source seed based on left/right annotation
	if ((*dtcFieldnames)[0].compare("left") == 0
			|| (*dtcFieldnames)[0].compare("right") == 0) {

		// get x-position of seeds, geodesic distances of seeds
		Array1D<float> xList(cb);
		Array1D<float> geodesicDistance(cb);
		proto->getList("seed_x", &xList);
		proto->getList((*row1Fieldnames)[0].c_str(), &geodesicDistance);

		// get index of left-most, right-most seed
		int minIdx = geodesicDistance.minIdx();
		float x_minGeodesicDistance = xList(minIdx);
		int maxIdx = geodesicDistance.maxIdx();
		float x_maxGeodesicDistance = xList(maxIdx);
		int leftIdx, rightIdx;
		if (x_minGeodesicDistance < x_maxGeodesicDistance) {
			leftIdx = minIdx;
			rightIdx = maxIdx;
		} else {
			leftIdx = maxIdx;
			rightIdx = minIdx;
		}

		// get index of source seed
		int sourceIdx;
		if ((*dtcFieldnames)[0].compare("left") == 0) {
			sourceIdx = leftIdx;
		} else if ((*dtcFieldnames)[0].compare("right") == 0) {
			sourceIdx = rightIdx;
		} else {
			log(cb, 1, "Error: Invalid choice of left/right");
			throw 999;
		}

		// set sourceSeedAnnotations
		int sz = xList.size();
		sourceSeedAnnotations->resize(sz);
		for (int i = 0; i < sz; i++) {
			if (i == sourceIdx) {
				(*sourceSeedAnnotations)(i) = 1;
			} else {
				(*sourceSeedAnnotations)(i) = 0;
			}
		}

		// return
		return 1;
	}

	// check for row1 annotations.  Return 1 if found
	int notFound_row1 = 1;
	for (unsigned int i = 0; i < row1Fieldnames->size(); i++) {
		notFound_row1 = proto->getList((*row1Fieldnames)[i].c_str(),
				sourceSeedAnnotations);
		if (!notFound_row1) {
			return 1;
		}
	}

	// check for dtc annotations.  Return 0 if found
	int notFound_dtc = 1;
	for (unsigned int i = 0; i < dtcFieldnames->size(); i++) {
		notFound_dtc = proto->getList((*dtcFieldnames)[i].c_str(),
				sourceSeedAnnotations);
		if (!notFound_dtc) {
			return 0;
		}
	}

	// error because need row1 or dtc
	log(cb, 1, "Error: DTC annotation not found");
	throw 999;

}

/*!
 * Future work: Need to update so that source seeds are read from SOURCE_ANNOTATION_FIELDNAME and write to DJIKSTRA_FIELDNAME
 * Automatically scores cell row position by applying Djikstra's algorithm to cell connectivity map
 * generated through touching segmentations in protobuf file.
 *   Arguments:	input.proto - input protobuf file containing segmentation
 *   			output.proto - output protobuf file with appended cell row measurements.
 *   			SOURCE_ANNOTATION_FIELDNAME - field name (string) of annotations that specify cells
 *   				in row 1.  For instance, if SOURCE_ANNOTATION_FIELDNAME = "anno_dtc", then all
 *   				cells specified by field "anno_dtc" are in cell row 1, all neighboring cells
 *   				are in cell row 2, etc.
 *   Syntax:	$BIN ... input.proto ... djikstraDistance 32 ... output.proto ... << EOT
 *   			0 0 0 SOURCE_ANNOTATION_FIELDNAME
 *   			EOT
 *   Notes:		Cell row scores are recorded in field DJIKSTRA_FIELDNAME in output.proto
 */
void djikstraDistance(TextIO* inputText, TextIO* outputText,
		CallbackFunctions *cb) {
	log(cb, 4, "Computing Djikstra distances to DTC/row1/leftOrRight");

	// get parameters
	log(cb, 4, "Prompt: 0 0 0 SOURCE_CELL_FIELDNAMES");
	const char **work_storage = cb->getMoreWork();
	string sourceCellFieldNames(work_storage[3]);
	cb->freeGetMoreWork(work_storage);

	// get possible dtc field names, row1 annotation field names, connectivity threshold
	vector<string> splitStr;
	boost::split(splitStr, sourceCellFieldNames, boost::is_any_of(":"));
	vector<string> dtcFieldnames;
	boost::split(dtcFieldnames, splitStr[0], boost::is_any_of(","));
	vector<string> row1Fieldnames;
	boost::split(row1Fieldnames, splitStr[1], boost::is_any_of(","));
	float connectivityThreshold = lexical_cast<float> (splitStr[2].c_str());

	// read input protobuf files
	Protobuf proto(cb);
	proto.readProto(inputText);

	// get segmentation image where segmentations are colored by protobuf_index+1
	Image3D<float> I_float(cb);
	Array1D<float> color(cb);
	proto.getList("protobuf_index", &color);
	color + 1;
	proto.drawSegmentationImage("full", &I_float, &color);

	// convert float image to int image
	int dimx, dimy, dimz;
	I_float.getDimensions(dimx, dimy, dimz);
	Image3D<int> I(cb);
	I.resize(dimx, dimy, dimz);
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				I(x, y, z) = boost::math::iround(I_float(x, y, z));
			}
		}
	}

	// get network connectivity based on segmentation image
	Image3D<int> neighborMatrix(cb);
	getNeighborMatrix_surfaceAreaMethod(&I, &neighborMatrix,
			connectivityThreshold, cb);
	//getNeighborMatrix(&I,&neighborMatrix);

	// get djikstra distance to source seed
	Array1D<int> dist(cb);
	Array1D<float> source(cb);
	int initialDistance = identifySourceSeed(&proto, &dtcFieldnames,
			&row1Fieldnames, cb, &source);
	djikstra(&neighborMatrix, &source, &dist, initialDistance);

	// get output array.  Output array is float distance from source cell+1.  -1 if unconnected to source
	Array1D<float> dist_float(cb);
	dist_float.resize(dist.size());
	for (int i = 0; i < dist.size(); i++) {
		if (dist(i) == INF) {
			dist_float(i) = -1;
		} else {
			dist_float(i) = (float) dist(i);
		}
	}

	// write out protobuf file
	proto.setList("cellRowCounter", &dist_float);
	proto.writeProto(outputText);
}
