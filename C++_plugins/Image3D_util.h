/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "Image3D.h"
#include "definitions.h"
#include "dt.h"
#if defined USELIBDISPATCH
#include <dispatch/dispatch.h>
#endif

float max(Image3D<float> *I);
float min(Image3D<float> *I);
float mean(Image3D<float> *I);
void bwdist(Image3D<float> *I);
void convolve1D(Image3D<float> *I, boost::multi_array<float, 1> *kernel,
		const char *direction, CallbackFunctions *cb);
void erode(Image3D<float> *I, int kernelSz);
void perim(Image3D<float> *I, int thickness);
void erode_multithreaded(Image3D<float> *I, int kernelSz);
void dilate_multithreaded(Image3D<float> *I, int kernelSz);
