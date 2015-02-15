/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_activeContours.h"
#include "Image3D_util.h"
using namespace boost::math;
using namespace std;
using namespace boost;

void seeds2Proto(vector<ActiveContourSeed*> *seeds, Protobuf *proto,
		Image3D<float> *g, CallbackFunctions *cb) {
	// Sanity check
	vAC_index numSeeds = seeds->size();
	int numSeeds_proto = proto->getNumberOfSeeds();
	if ((int) numSeeds != numSeeds_proto) {
		log(cb, 1, "Error: seed numbers do not match");
		throw 999;
	}

	// calculate sparse coordinates for segmentation masks
	vector<Array1D<int>*> *xFull = new vector<Array1D<int>*> ;
	vector<Array1D<int>*> *yFull = new vector<Array1D<int>*> ;
	vector<Array1D<int>*> *zFull = new vector<Array1D<int>*> ;
	vector<Array1D<int>*> *xPerim = new vector<Array1D<int>*> ;
	vector<Array1D<int>*> *yPerim = new vector<Array1D<int>*> ;
	vector<Array1D<int>*> *zPerim = new vector<Array1D<int>*> ;
	xFull->resize(numSeeds);
	yFull->resize(numSeeds);
	zFull->resize(numSeeds);
	xPerim->resize(numSeeds);
	yPerim->resize(numSeeds);
	zPerim->resize(numSeeds);
#if defined USELIBDISPATCH
	dispatch_apply(numSeeds, dispatch_get_global_queue(0,0), ^(size_t i) {
#else
	for (size_t i = 0; i < numSeeds; i++) {
#endif
		Array1D<int> *xFull_singleSeed = new Array1D<int> (cb);
		Array1D<int> *yFull_singleSeed = new Array1D<int> (cb);
		Array1D<int> *zFull_singleSeed = new Array1D<int> (cb);
		Array1D<int> *xPerim_singleSeed = new Array1D<int> (cb);
		Array1D<int> *yPerim_singleSeed = new Array1D<int> (cb);
		Array1D<int> *zPerim_singleSeed = new Array1D<int> (cb);
		(*seeds)[i]->getSparseCoordinates_full(xFull_singleSeed,
				yFull_singleSeed, zFull_singleSeed);
		(*seeds)[i]->getSparseCoordinates_perim(xPerim_singleSeed,
				yPerim_singleSeed, zPerim_singleSeed);
		(*xFull)[i] = xFull_singleSeed;
		(*yFull)[i] = yFull_singleSeed;
		(*zFull)[i] = zFull_singleSeed;
		(*xPerim)[i] = xPerim_singleSeed;
		(*yPerim)[i] = yPerim_singleSeed;
		(*zPerim)[i] = zPerim_singleSeed;
#if defined USELIBDISPATCH
	});
#else
	}
#endif

	// put sparse coordinates into protobuf file
	for (vAC_index i = 0; i < numSeeds; i++) {
		proto->setSparseSegmentationCoordinates((int) i, (*xFull)[i],
				(*yFull)[i], (*zFull)[i], (*xPerim)[i], (*yPerim)[i],
				(*zPerim)[i]);
	}

	// put image dimensions into protobuf file
	int dimx, dimy, dimz;
	g->getDimensions(dimx, dimy, dimz);
	proto->setDimensions(dimx, dimy, dimz);

	// future work: put cell specific parameters into protobuf file

	// deallocate memory
	for (vAC_index i = 0; i < seeds->size(); i++) {
		delete (*xFull)[i];
		(*xFull)[i] = NULL;
		delete (*yFull)[i];
		(*yFull)[i] = NULL;
		delete (*zFull)[i];
		(*zFull)[i] = NULL;
		delete (*xPerim)[i];
		(*xPerim)[i] = NULL;
		delete (*yPerim)[i];
		(*yPerim)[i] = NULL;
		delete (*zPerim)[i];
		(*zPerim)[i] = NULL;
	}
	delete xFull;
	xFull = NULL;
	delete yFull;
	yFull = NULL;
	delete zFull;
	zFull = NULL;
	delete xPerim;
	xPerim = NULL;
	delete yPerim;
	yPerim = NULL;
	delete zPerim;
	zPerim = NULL;
}

/*!
 *
 */
void run_active_contours(Protobuf *seedData, Image3D<float> *g,
		Image3D<float> *fullSegmentation, bool useCellSpecificParameters,
		ActiveContourParameters* default_parameters, Image3D<float> *movie,
		int movieSlice, int movieOption, CallbackFunctions *cb) {

	// initialize individual active contour seeds
	vector<ActiveContourSeed*> *seeds = new vector<ActiveContourSeed*> ;
	seeds->resize((vAC_index) seedData->getNumberOfSeeds());

#if defined USELIBDISPATCH
	dispatch_apply(seeds->size(), dispatch_get_global_queue(0,0), ^(size_t i) {
#else
	for (vAC_index i = 0; i < seeds->size(); i++) {
#endif
		ActiveContourSeed *seed = new ActiveContourSeed(seedData, (int) i,
				useCellSpecificParameters, default_parameters,
				fullSegmentation, g, cb);
		(*seeds)[i] = seed;
#if defined USELIBDISPATCH
	});
#else
	}
#endif

	// get maximum run time over all seeds
	int tMax = -1;
	for (vAC_index i = 0; i < seeds->size(); i++) {
		tMax = std::max(tMax, (*seeds)[i]->getTmax());
	}

	// initialize movie
	int dimx, dimy, dimz;
	fullSegmentation->getDimensions(dimx, dimy, dimz);
	if (movieOption == 1) {
		movie->resize(dimy, dimz, tMax);
	} else if (movieOption == 2) {
		movie->resize(dimx, dimz, tMax);
	} else if (movieOption == 3) {
		movie->resize(dimx, dimy, tMax);
	}

	// run active contours
	log(cb, 4, "Starting active contour run");
	for (int t = 0; t < tMax; t++) {
		cb->progressReport(t * 100 / tMax);

		// do active contours
#if defined USELIBDISPATCH
		dispatch_apply(seeds->size(), dispatch_get_global_queue(0,0), ^(size_t i) {
#else
		for (vAC_index i = 0; i < seeds->size(); i++) {
#endif
			(*seeds)[i]->step();
#if defined USELIBDISPATCH
		});
#else
		}
#endif

#if defined USELIBDISPATCH
		dispatch_apply(seeds->size(), dispatch_get_global_queue(0,0), ^(size_t i) {
#else
		for (vAC_index i = 0; i < seeds->size(); i++) {
#endif
			(*seeds)[i]->updatePhi();
#if defined USELIBDISPATCH
		});
#else
		}
#endif

		// record movie
		if (movieOption != 0) {
			recordMovie(movie, t, movieOption, movieSlice, seeds, cb);
		}
	}
	log(cb, 4, "Finishing active contour run");

	// stuff segmentation into protobuf file
	seeds2Proto(seeds, seedData, g, cb);

	// clean up
	for (vAC_index i = 0; i < seeds->size(); i++) {
		delete (*seeds)[i];
		(*seeds)[i] = NULL;
	}
	delete seeds;
	seeds = NULL;
}

/*!
 *
 */
void recordMovie(Image3D<float> *movie, int t, int movieOption, int movieSlice,
		vector<ActiveContourSeed*> *seeds, CallbackFunctions *cb) {
	for (vAC_index i = 0; i < seeds->size(); i++) {
		// yz movie
		if (movieOption == 1) {
			if ((*seeds)[i]->containsXSlice(movieSlice)) {
				Array1D<int> xList(cb), yList(cb), zList(cb);
				(*seeds)[i]->getSparseCoordinates_perim(&xList, &yList, &zList);
				int numPixels = xList.size();
				for (int j = 0; j < numPixels; j++) {
					if (xList(j) == movieSlice) {
						(*movie)(yList(j), zList(j), t)
								= (*seeds)[i]->getSeedIdx();
					}
				}
			}
		}

		// xz movie
		if (movieOption == 2) {
			if ((*seeds)[i]->containsYSlice(movieSlice)) {
				Array1D<int> xList(cb), yList(cb), zList(cb);
				(*seeds)[i]->getSparseCoordinates_perim(&xList, &yList, &zList);
				int numPixels = xList.size();
				for (int j = 0; j < numPixels; j++) {
					if (yList(j) == movieSlice) {
						(*movie)(xList(j), zList(j), t)
								= (*seeds)[i]->getSeedIdx();
					}
				}
			}
		}

		// xy movie
		if (movieOption == 3) {
			if ((*seeds)[i]->containsZSlice(movieSlice)) {
				Array1D<int> xList(cb), yList(cb), zList(cb);
				(*seeds)[i]->getSparseCoordinates_perim(&xList, &yList, &zList);
				int numPixels = xList.size();
				for (int j = 0; j < numPixels; j++) {
					if (zList(j) == movieSlice) {
						(*movie)(xList(j), yList(j), t)
								= (*seeds)[i]->getSeedIdx();
					}
				}
			}
		}
	}
}

/*!
 * Arguments:
 *   inputImage : Preprocessed membrane image
 *   inputText  : Protobuf file of previously segmented image
 *   outputImage: Output image of segmentation boundaries
 *   outputText : Output protobuf file containing segmentations
 *
 * getMoreWork Arguments:
 *   SEEDS: Protobuf file containing active contour data for individual seeds
 *   HSZ  : Default active contour window size
 *
 * Detail:
 *   inputText : This protobuf file contains data for a previously segmented image.
 *               It is used to set up collision boundaries so that newly added
 *               contours do not overlap with previous segmentations.
 *   SEEDS     : This protobuf file contains active contour data for individual seeds.
 *               It must contain the spatial position (x,y,z coordinate) of the seed.
 *               It may optionally contain cell specific active contour parameters such
 *               as TMAX, C, D, R, HSZ, etc.  If these cell specific parameters are not
 *               specified, then default parameters are used (see section "getMoreWork
 *               Arguments").  In addition, SEEDS may optionally contain the initial
 *               conditions for the active contour.  If these initial conditions are
 *               not specified, then the seed will initialize from a point with radius
 *               R.  SEEDS does not need to have seed indices specified.
 *
 * Behavior:
 *   This plugin reads two protobuf files: inputText and SEEDS, which contain previous
 *   segmentations and seeds you want to run active contours on, respectively.  The
 *   following are commonly used cases for inputText and SEEDS:
 *     - If you want to run active contours with no collisions with pre-existing
 *       segmentations, set inputText name to "0".
 *     - If you want to run active contours from some initial condition, set initial
 *       conditions in sparse segmentation coordinates in SEEDS.  If no initial conditions
 *       are set, then the active contour will be initialized from a point.
 */
void active_contours(ImageIO* inputImage, TextIO* inputText,
		ImageIO* outputImage, ImageIO* outputMovie, TextIO* outputText,
		bool recordMovie, CallbackFunctions *cb) {
	log(cb, 4, "Running active contours");

	// read preprocessed membrane image
	Image3D<float> *g = new Image3D<float> (cb);
	g->read(inputImage);

	// read pre-existing segmentations and create full segmentation image
	Protobuf *previousSegmentations = new Protobuf(cb);
	Image3D<float> *fullSegmentation = new Image3D<float> (cb);
	if (previousSegmentations->readProto(inputText)) { // If file is empty, then create empty full segmentation image
		int dimx, dimy, dimz;
		g->getDimensions(dimx, dimy, dimz);
		fullSegmentation->resize(dimx, dimy, dimz);
	} else { // otherwise, generate full segmentation image from previous segmentation
		// sanity check
		int dimx_g, dimy_g, dimz_g;
		g->getDimensions(dimx_g, dimy_g, dimz_g);
		int dimx_proto, dimy_proto, dimz_proto;
		previousSegmentations->getDimensions(dimx_proto, dimy_proto, dimz_proto);
		if (dimx_g != dimx_proto || dimy_g != dimy_proto || dimz_g
				!= dimz_proto) {
			log(
					cb,
					0,
					"Warning: Protobuf and image dimensions don't match (%d!=%d,%d!=%d,%d!=%d).  Setting protobuf dimensions to image dimensions",
					dimx_g, dimx_proto, dimy_g, dimy_proto, dimz_g, dimz_proto);
			previousSegmentations->setDimensions(dimx_g, dimy_g, dimz_g);
		}
		// generate full segmentation image
		Array1D<float> seed_indices(cb);
		previousSegmentations->getList("idx", &seed_indices);
		previousSegmentations->drawSegmentationImage("full", fullSegmentation,
				&seed_indices);
	}

	// create empty image to store movie
	Image3D<float> *movie = new Image3D<float> (cb);

	// getMoreWork parameters
	log(
			cb,
			4,
			"Prompt: 0 0 SEEDS HSZ TMAX DT SUSSMAN_INTERVAL C D EPSILON R USE_SPECIFIC_PARAM RESX RESY RESZ");
	const char **work_storage = cb->getMoreWork();
	string fileName(work_storage[2]);
	ActiveContourParameters defaultParameters;
	defaultParameters.hsz = lexical_cast<float> (work_storage[3]);
	defaultParameters.tMax = lexical_cast<float> (work_storage[4]);
	defaultParameters.dt = lexical_cast<float> (work_storage[5]);
	defaultParameters.sussmanInterval = lexical_cast<int> (work_storage[6]);
	defaultParameters.c = lexical_cast<float> (work_storage[7]);
	defaultParameters.d = lexical_cast<float> (work_storage[8]);
	defaultParameters.epsilon = lexical_cast<float> (work_storage[9]);
	defaultParameters.r = lexical_cast<float> (work_storage[10]);
	bool useCellSpecificParameters = (strcmp(work_storage[11], "true") == 0);
	defaultParameters.narrowBandThreshold = lexical_cast<float> (
			work_storage[12]);
	defaultParameters.res_x = lexical_cast<float> (work_storage[13]);
	defaultParameters.res_y = lexical_cast<float> (work_storage[14]);
	defaultParameters.res_z = lexical_cast<float> (work_storage[15]);
	int movieOption = atoi(work_storage[16]);
	int movieSlice = atoi(work_storage[17]);
	if (!recordMovie) {
		movieOption = 0;
		movieSlice = 0;
	}

	// read seeds to run/rerun and make sure that seed indices are unique
	Protobuf seedsToRun(cb);
	seedsToRun.readProto(fileName.c_str(), inputText);
	int seedIdxStart = (int) previousSegmentations->getMaxSeedIdx();
	int dimx, dimy, dimz;
	fullSegmentation->getDimensions(dimx, dimy, dimz);
	seedsToRun.thresholdSeedsByPosition(dimx, dimy, dimz);
	seedsToRun.reorderSeedIndices(seedIdxStart);

	// run active contours
	run_active_contours(&seedsToRun, g, fullSegmentation,
			useCellSpecificParameters, &defaultParameters, movie, movieSlice,
			movieOption, cb);

	// Future work: In order to implement real-time addition/deletion of seeds, seedsToRun appended into previousSegmentations

	// write output.  In the future, if real-time addition/deletion of seeds is implemented, previousSegmentations should be written out instead of seedsToRun
	Image3D<float> output(cb);
	Array1D<float> color(cb);
	seedsToRun.getList("idx", &color);
	seedsToRun.drawSegmentationImage("Perimeter", &output, &color);
	output.write(outputImage);
	seedsToRun.writeProto(outputText);
	if (movieOption != 0) {
		movie->write(outputMovie);
	}

	delete g;
	g = NULL;
	delete previousSegmentations;
	previousSegmentations = NULL;
	delete fullSegmentation;
	fullSegmentation = NULL;
	delete movie;
	movie = NULL;

}

