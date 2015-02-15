/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_dilate.h"

void imdilate(ImageIO* inputImage, ImageIO* outputImage, CallbackFunctions *cb) {
	log(cb, 4, "Dilating image");

	// read input image
	Image3D<float> *I = new Image3D<float> (cb);
	I->read(inputImage);

	// get dilate parameters
	log(cb, 4, "Prompt: 0 0 DILATE_KERNEL_SIZE");
	const char **work_storage = cb->getMoreWork();
	int ksz = atoi(work_storage[2]);
	cb->freeGetMoreWork(work_storage);

	// Do dilate
	dilate_multithreaded(I, ksz);

	// write output image
	I->write(outputImage);

	// clean up
	delete I;
	I = NULL;

}
