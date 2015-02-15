/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_boxNormalize.h"
using namespace boost::math;
using namespace boost;

static void getUniformKernel(multi_array<float, 1> *kernel, float resolution,
		float support) {
	int support_scaled = iround(support / resolution);
	kernel->resize(boost::extents[support_scaled]);
	for (int i = 0; i < support_scaled; i++) {
		(*kernel)[i] = 1 / (float) support_scaled;
	}
}

void boxNormalize(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "box normalizing image");
	Image3D<float> I(cb);
	I.read(inputImage); // read input image
	Image3D<float> I_original(I);

	// get blurring parameters
	log(cb, 4, "Prompt: 0 0 SUPPORT RES_X RES_Y RES_Z");
	const char **work_storage = cb->getMoreWork();
	float support = lexical_cast<float> (work_storage[2]);
	float res_x = lexical_cast<float> (work_storage[3]);
	float res_y = lexical_cast<float> (work_storage[4]);
	float res_z = lexical_cast<float> (work_storage[5]);
	cb->freeGetMoreWork(work_storage);

	// get convolution kernels
	multi_array<float, 1> *kernel_x = new multi_array<float, 1> ;
	multi_array<float, 1> *kernel_y = new multi_array<float, 1> ;
	multi_array<float, 1> *kernel_z = new multi_array<float, 1> ;
	getUniformKernel(kernel_x, res_x, support);
	getUniformKernel(kernel_y, res_y, support);
	getUniformKernel(kernel_z, res_z, support);

	// do convolutions
	convolve1D(&I, kernel_x, "x", cb);
	cb->progressReport(33);
	convolve1D(&I, kernel_y, "y", cb);
	cb->progressReport(66);
	convolve1D(&I, kernel_z, "z", cb);
	cb->progressReport(99);

	// divide
	I_original / I;

	// write output image
	I_original.write(outputImage);
}
