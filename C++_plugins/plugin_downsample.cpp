/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_downsample.h"

static void scaleDown(Image3D<float> *I, int dimx_new, int dimy_new,
		int dimz_new) {
	// make a copy of the original image
	Image3D<float> *I_original = new Image3D<float> (*I);

	// get new dimensions of rescaled image and resize
	int dimx_old, dimy_old, dimz_old;
	I->getDimensions(dimx_old, dimy_old, dimz_old);
	I->resize(dimx_new, dimy_new, dimz_new);

	// calculate factors used to convert from new, resized indices to old indices
	float factor_x = (float) (dimx_old - 1) / (dimx_new - 1);
	float factor_y = (float) (dimy_old - 1) / (dimy_new - 1);
	float factor_z = (float) (dimz_old - 1) / (dimz_new - 1);

	// resize image
#if defined USELIBDISPATCH
	dispatch_apply((unsigned int)dimx_new, dispatch_get_global_queue(0,0), ^(size_t x) {
#else
	for (int x = 0; x < dimx_new; x++) {
#endif
		for (int y = 0; y < dimy_new; y++) {
			for (int z = 0; z < dimz_new; z++) {
				float x_old = factor_x * x;
				float y_old = factor_y * y;
				float z_old = factor_z * z;
				float newPixelValue = I_original->interpolate(x_old, y_old,
						z_old);
				(*I)((int) x, y, z) = newPixelValue;
			}
		}
#if defined USELIBDISPATCH
	});
#else
	}
#endif
	// clean up
	delete I_original;
	I_original = NULL;
}

/*!
 * Downsamples an image.
 *   Arguments:	input.tif - input image to be downsampled
 *   			output.tif - downsampled output image
 *   			DIM_X - x dimension of downsampled output image (int value)
 *   			DIM_Y - x dimension of downsampled output image (int value)
 *   			DIM_Z - x dimension of downsampled output image (int value)
 *   Syntax:	$BIN input.tif ... ... downsample 32 output.tif ... ... << EOT
 *   			0 0 DIM_X DIM_Y DIM_Z
 *   			EOT
 *   Notes:		Downsampling is done through linear interpolation. Also note
 *   			that this plugin does not update x,y,z resolution in the metadata.
 */
void downSample(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "Resizing image");

	// read in input image
	Image3D<float> *I = new Image3D<float> (cb);
	I->read(inputImage);

	// get parameters
	log(cb, 4, "Prompt: 0 0 DIM_X DIM_Y DIM_Z");
	const char **work_storage = cb->getMoreWork();
	int dimx_new = atoi(work_storage[2]);
	int dimy_new = atoi(work_storage[3]);
	int dimz_new = atoi(work_storage[4]);
	cb->freeGetMoreWork(work_storage);

#if defined INCLUDESANITYCHECKS
	int dimx, dimy, dimz;
	I->getDimensions(dimx,dimy,dimz);
	// Sanity check
	if (dimx_new>dimx || dimy_new>dimy || dimz_new>dimz) {
		log(cb,1,"Image must be made strictly smaller");
		throw 999;
	}
#endif

	// resize image
	scaleDown(I, dimx_new, dimy_new, dimz_new);

	// write output image
	I->write(outputImage);
	delete I;
	I = NULL;
}
