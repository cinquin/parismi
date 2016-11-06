/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_topLayer.h"
using namespace std;
using namespace boost::math;

/*!
 * Gets z-projection of 3D image looking down from the top of the z-axis
 */
static void zProjection(Image3D<float> *I, CallbackFunctions *cb) {
	// initialize output (2D projection image)
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	Image3D<float> output(cb);
	output.resize(dimx, dimy, 1);

	// get projection image
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				if (!isCloseTo((*I)(x, y, z), 0)) {
					output(x, y, 0) = (*I)(x, y, z);
					break;
				}
			}
		}
	}

	// return output image
	(*I) = output;
}

void topLayer(TextIO* inputText, TextIO *outputText, CallbackFunctions *cb) {
	log(cb, 4, "Calculating top-layer metric");

	// read input protobuf file
	Protobuf proto(cb);
	proto.readProto(inputText);

	// get segmentation image where segmentations are colored by protobuf_index+1
	Image3D<float> segmentation(cb);
	Array1D<float> color(cb);
	proto.getList("protobuf_index", &color);
	color + 1;
	proto.drawSegmentationImage("full", &segmentation, &color);

	// get 2D projection of segmentation image
	Image3D<float> segmentation_zProjection(segmentation);
	zProjection(&segmentation_zProjection, cb);

	// get exposed surface areas of 2D projection image and store in surfaceArea_exposed, where surfaceArea_exposed(i) is the exposed surface area of seed with protobuf index i
	Array1D<float> surfaceArea_exposed(cb);
	surfaceArea_exposed.resize(proto.getNumberOfSeeds());
	int dimx, dimy, dummy;
	segmentation_zProjection.getDimensions(dimx, dimy, dummy);
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			int protoIdx = iround(segmentation_zProjection(x, y, 0)) - 1;
			if (protoIdx != -1) {
				surfaceArea_exposed(protoIdx)++;
			}
		}
	}

	// get total surface areas of segmentation masks and store in surfaceArea_total, where surfaceArea_total(i) is the total surface area of segmentation mask with protobuf index i
	Array1D<float> surfaceArea_total(cb);
	surfaceArea_total.resize(proto.getNumberOfSeeds());
	for (int i = 0; i < proto.getNumberOfSeeds(); i++) {
		Image3D<float> mask(cb);
		proto.applySegmentationMask(i, &segmentation, &mask);
		zProjection(&mask, cb);
		surfaceArea_total(i) = (float) mask.nnz();
	}

	// calculate percent exposed surface area and store in protobuf file
	Array1D<float> ratio(surfaceArea_exposed);
	ratio / surfaceArea_total;
	proto.setList("top_layer", &ratio);

	// write out protobuf file
	proto.writeProto(outputText);
}
