/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "ImageIO.h"
#include "CallbackFunctions.h"
#include "Image3D.h"
#include "Image3D_util.h"
#include "util.h"

void distanceTransform(ImageIO* inputImage, ImageIO* outputImage,
		CallbackFunctions *cb);
