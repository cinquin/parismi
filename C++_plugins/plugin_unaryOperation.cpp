/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_unaryOperation.h"

/*!
 * \section unaryOperation_description Description
 * This plugin inverts an image
 * \section Usage
 * ./segpipeline_v3 input.tif ... ... unaryOperation 32 output.tif
 */
void unaryOperation(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "Normalizing pixel intensities between [0,1]");

	// read in input image
	Image3D<float> I(cb);
	I.read(inputImage);

	// perform unary operation
	int dimx, dimy, dimz;
	I.getDimensions(dimx, dimy, dimz);
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				I(x, y, z) = expf(-I(x, y, z));
			}
		}
	}

	// write output image
	I.write(outputImage);
}
