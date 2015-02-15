/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_rescaleZ.h"
using namespace boost::math;
using namespace boost;

static void rescale_image_z(Image3D<float> *I, float rescale_val,
		CallbackFunctions *cb, int method, Image3D<float> *out) {
	// get dimensions of input and output images
	int dimx, dimy, dimz_in;
	I->getDimensions(dimx, dimy, dimz_in);
	int dimz_out = (int) (rescale_val * dimz_in);
	out->resize(dimx, dimy, dimz_out);

	// get rescaled z-axis.  Example: zaxis_transform[3] = 2.5 means that slice 3 in the output image is equivalent to slice 2.5 in the input image
	Array1D<float> *zaxis_transform = new Array1D<float> (cb);
	zaxis_transform->resize(dimz_out);
	for (int i = 0; i < dimz_out; i++) {
		(*zaxis_transform)(i) = (float) i * (dimz_in - 1) / (dimz_out - 1);
	}
	// sometimes math is a little off
	(*zaxis_transform)(dimz_out - 1) = roundf((*zaxis_transform)(dimz_out - 1));

	// nearest neighbor interpolation
	if (method == 1) {
#if defined USELIBDISPATCH
		dispatch_apply((unsigned int)dimz_out, dispatch_get_global_queue(0, 0), ^(size_t z) {
#else
		for (int z = 0; z < dimz_out; z++) {
#endif
			int z_in = iround((*zaxis_transform)((int) z));
			for (int x = 0; x < dimx; x++) {
				for (int y = 0; y < dimy; y++) {
					(*out)(x, y, (int) z) = (*I)(x, y, z_in);
				}
			}
#if defined USELIBDISPATCH
		});
#else
		}
#endif
		// linear interpolation
	} else if (method == 2) {
#if defined USELIBDISPATCH
		dispatch_apply((unsigned int)dimz_out, dispatch_get_global_queue(0, 0), ^(size_t z) {
#else
		for (int z = 0; z < dimz_out; z++) {
#endif
			int z_high = iround(ceil((*zaxis_transform)((int) z)));
			int z_low = iround(floor((*zaxis_transform)((int) z)));
			float z_val = (*zaxis_transform)((int) z);
			for (int x = 0; x < dimx; x++) {
				for (int y = 0; y < dimy; y++) {
					float in_high = (*I)(x, y, z_high);
					float in_low = (*I)(x, y, z_low);
					(*out)(x, y, (int) z) = (in_high - in_low)
							* (z_val - z_low) + in_low;
				}
			}
#if defined USELIBDISPATCH
		});
#else
		}
#endif
	} else {
		log(cb, 1, "Invalid choice of zRescale method");
		throw 999;
	}

	// clean up
	delete zaxis_transform;
	zaxis_transform = NULL;

}

void rescaleZ(ImageIO* inputImage, ImageIO* outputImage, CallbackFunctions *cb) {
	log(cb, 4, "Rescaling image (z-direction");
	Image3D<float> *I = new Image3D<float> (cb);
	I->read(inputImage);

	// get rescale parameters
	log(cb, 4, "Prompt: 0 0 RESCALE_FACTOR");
	const char **work_storage = cb->getMoreWork();
	float rescaleFactor = lexical_cast<float> (work_storage[2]);
	cb->freeGetMoreWork(work_storage);

	// do rescaling
	Image3D<float> *out = new Image3D<float> (cb);
	rescale_image_z(I, rescaleFactor, cb, 2, out);

	// write output image
	out->write(outputImage);
	delete I;
	I = NULL;
	delete out;
	out = NULL;
}
