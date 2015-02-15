/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_blur.h"
using namespace boost::math;
using namespace boost;

static void getGaussianKernel(multi_array<float, 1> *kernel, float resolution,
		float sigma) {

	// calculate kernel size
	float support = sigma * 2 * NUMBER_OF_STANDARD_DEVIATIONS_IN_KERNEL; // kernel size should encapsulate most of Gaussian
	int support_scaled = iround(support / resolution);

	// make sure support is odd
	if (support_scaled % 2 == 0) {
		support_scaled++;
	}

	// calculate kernel
	float sigma_scaled = sigma / resolution;
	kernel->resize(boost::extents[support_scaled]);
	float sum = 0;
	for (int i = 0; i < support_scaled; i++) {
		float i_f = (float) i;
		float support_scaled_f = (float) support_scaled;
		(*kernel)[i] = 1.0f / sqrtf(2.0f * PI * sigma_scaled * sigma_scaled)
				* expf(
						-powf(i_f - (support_scaled_f - 1.0f) / 2.0f, 2.0f)
								/ (2.0f * sigma_scaled * sigma_scaled));
		sum += (*kernel)[i];
	}
	// normalize kernel
	for (int i = 0; i < support_scaled; i++) {
		(*kernel)[i] = (*kernel)[i] / sum;
	}
}

void gaussianBlur(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "blurring image");

	// read in input image
	Image3D<float> I(cb);
	I.read(inputImage);

	// get blurring parameters
	log(cb, 4, "Prompt: 0 0 SIGMA RES_X RES_Y RES_Z");
	const char **work_storage = cb->getMoreWork();
	float sigma = lexical_cast<float> (work_storage[2]);
	float res_x = lexical_cast<float> (work_storage[3]);
	float res_y = lexical_cast<float> (work_storage[4]);
	float res_z = lexical_cast<float> (work_storage[5]);
	cb->freeGetMoreWork(work_storage);

	// get convolution kernels
	multi_array<float, 1> *kernel_x = new multi_array<float, 1> ;
	multi_array<float, 1> *kernel_y = new multi_array<float, 1> ;
	multi_array<float, 1> *kernel_z = new multi_array<float, 1> ;
	getGaussianKernel(kernel_x, res_x, sigma);
	getGaussianKernel(kernel_y, res_y, sigma);
	getGaussianKernel(kernel_z, res_z, sigma);

	// do convolutions
	convolve1D(&I, kernel_x, "x", cb);
	cb->progressReport(33);
	convolve1D(&I, kernel_y, "y", cb);
	cb->progressReport(66);
	convolve1D(&I, kernel_z, "z", cb);
	cb->progressReport(99);

	// write output image
	I.write(outputImage);
}
