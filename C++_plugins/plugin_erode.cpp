/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_erode.h"

void imerode(ImageIO* inputImage, ImageIO* outputImage, CallbackFunctions *cb) {
	log(cb, 4, "Eroding image");

	// read input image
	Image3D<float> *I = new Image3D<float> (cb);
	I->read(inputImage);

	// get erode parameters
	log(cb, 4, "Prompt: 0 0 ERODE_KERNEL_SIZE");
	const char **work_storage = cb->getMoreWork();
	int ksz = atoi(work_storage[2]);
	cb->freeGetMoreWork(work_storage);

	// Do erode
	erode_multithreaded(I, ksz);

	// write output image
	I->write(outputImage);

	// clean up
	delete I;
	I = NULL;

}
