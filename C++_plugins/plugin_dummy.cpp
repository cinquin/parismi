/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_dummy.h"
using namespace boost;

/*!
 * Description: This plugin shows how to use basic image processing functionality
 * This plugin reads an input image (input.tif), reads 3 integers from the standard input (X,Y,Z), then does
 * following operations: (1) Retrieves the pixel value of input.tif at x=X, y=Y, z=Z, (2) multiplies this pixel value by 2,
 * (3) writes an output image (output.tif) that is identical to the input image, except that the pixel value at x,y,z has
 * been replaced by the new pixel value calculated in (2)
 *
 * Usage in terminal:
 * ./segpipeline_FREEBSD input.tif 0 0 0 0 0 0 0 dummyPlugin 32 output.tif
 * >> X Y Z
 *
 */

void dummyPlugin(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "Dummy plugin for testing");

	// read in input image
	Image3D<float> I(cb);
	I.read(inputImage);

	// get parameters from standard input
	log(cb, 4, "Prompt: X Y Z");
	const char **work_storage = cb->getMoreWork();
	int x = lexical_cast<int> (work_storage[0]);
	int y = lexical_cast<int> (work_storage[1]);
	int z = lexical_cast<int> (work_storage[2]);
	cb->freeGetMoreWork(work_storage);

	// get pixel value at I(x,y,z)
	float pixelValue = I(x, y, z);
	float newPixelValue = pixelValue * 2;

	// print to standard output
	log(cb, 4, "old pixel value = %f, new pixel value = %f", pixelValue,
			newPixelValue);

	// replace old pixel value in I(x,y,z)
	I(x, y, z) = newPixelValue;

	// write output image
	I.write(outputImage);
}
