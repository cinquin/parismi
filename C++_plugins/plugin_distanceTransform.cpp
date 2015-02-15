/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_distanceTransform.h"

void distanceTransform(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "computing distance transform image");

	Image3D<float> I(cb);
	I.read(inputImage); // read in input image
	bwdist(&I); // run distance transform where all voxels <0.5 are set to 0, all voxels >0.5 are set to 1
	I.write(outputImage); // write output image
}
