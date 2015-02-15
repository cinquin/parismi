/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_emptyImage.h"

/*!
 * \section emptyImage_description Description
 * This plugin creates an empty image (all zeros) the same size as input.tif
 * \section Usage
 * ./segpipeline_v3 input.tif ... ... normalize 32 output.tif
 */
void emptyImage(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "Setting all pixel values to 0");

	Image3D<float> I(cb);
	I.read(inputImage); // read in input image
	int dimx, dimy, dimz;
	I.getDimensions(dimx, dimy, dimz);
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				I(x, y, z) = 0;
			}
		}
	}

	I.write(outputImage); // write output image
}
