/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_normalize.h"

/*!
 * \section normalize_description Description
 * This plugin normalizes all pixel values between 0 and 1.
 * \section Usage
 * ./segpipeline_v3 input.tif ... ... normalize 32 output.tif
 */
void normalize(ImageIO* inputImage, ImageIO* outputImage, CallbackFunctions *cb) {
	log(cb, 4, "Normalizing pixel intensities between [0,1]");

	Image3D<float> I(cb);
	I.read(inputImage); // read in input image
	I - min(&I);
	I / max(&I); // do normalize
	I.write(outputImage); // write output image
}
