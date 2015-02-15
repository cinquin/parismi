/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_principalCurvature.h"
#include <dispatch/dispatch.h>
using namespace boost;

void principalCurvature(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb) {
	log(cb, 4, "calculating principal curvature");

	// read in input image
	Image3D<float> *I = new Image3D<float> (cb);
	I->read(inputImage);

	// get parameters
	log(cb, 4, "Prompt: 0 0 RES_X RES_Y RES_Z");
	const char **work_storage = cb->getMoreWork();
	float res_x = lexical_cast<float> (work_storage[2]);
	float res_y = lexical_cast<float> (work_storage[3]);
	float res_z = lexical_cast<float> (work_storage[4]);
	cb->freeGetMoreWork(work_storage);

	// calculate principal curvature
	I->setResolution(res_x, res_y, res_z);

	Image3D<float> *output = new Image3D<float> (*I);
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);

#if defined USELIBDISPATCH
	dispatch_apply((unsigned int)dimx,dispatch_get_global_queue(0,0),^(size_t x) {
#else
	for (int x = 0; x < dimx; x++) {
#endif
		if (!cb->shouldInterrupt()) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					float value = I->getPrincipalCurvature((int) x, y, z);
					(*output)((int) x, y, z) = value;
				}
			}
		}
#if defined USELIBDISPATCH
	});
#else
	}
#endif

	// write output image
	output->write(outputImage);

	delete I;
	I = NULL;
	delete output;
	output = NULL;
}
