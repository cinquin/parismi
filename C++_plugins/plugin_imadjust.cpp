/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_imadjust.h"
using namespace boost;

void imadjust(ImageIO* inputImage, ImageIO* outputImage, CallbackFunctions *cb) {
	log(cb, 4, "Using imadjust to threshold image");
	__block Image3D<float> I(cb);
	I.read(inputImage); // read in input image

	// get blurring parameters
	log(cb, 4, "Prompt: 0 0 MINPERC MAXPERC");
	const char **work_storage = cb->getMoreWork();
	float minPerc = lexical_cast<float> (work_storage[2]);
	float maxPerc = lexical_cast<float> (work_storage[3]);
	cb->freeGetMoreWork(work_storage);

	// convert 3D image to 1D array and get user-specified percentiles
	Array1D<float> array(cb);
	array.im2array(&I);
	float thresholdLow = array.perctile(minPerc);
	float thresholdHigh = array.perctile(maxPerc);

	// do thresholding
	int dimx, dimy, dimz;
	I.getDimensions(dimx, dimy, dimz);
	std::atomic<unsigned int> progress_count{0u};
	std::atomic<unsigned int> *progress_count_ptr = &progress_count;
	__block volatile bool keepGoing = 1;
#if defined USELIBDISPATCH
	dispatch_apply((unsigned int)dimx, dispatch_get_global_queue(0,0), ^(size_t xx) {
#else
	for (int xx = 0; xx < dimx; xx++) {
#endif
		int x = (int) xx;
		if (keepGoing == 1) {
			if (cb->shouldInterrupt()) {
				log(cb, 4, "Interrupt signal received");
				keepGoing = 0;
			}
		} else {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					if (I(x, y, z) > thresholdHigh) {
						I(x, y, z) = thresholdHigh;
					} else if (I(x, y, z) < thresholdLow) {
						I(x, y, z) = thresholdLow;
					}
				}
			}
			atomic_increment(progress_count_ptr);
			cb->progressReport((100 * int(*progress_count_ptr)) / dimx);
		}
#if defined USELIBDISPATCH
	});
#else
	}
#endif

	// write output image
	I.write(outputImage);
}
