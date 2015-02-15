/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "CallbackFunctions.h"
#include "ImageIO.h"
#include "Image3D.h"
#include "TextIO.h"
#include "Protobuf.h"
#include "Array1D.h"
#include "util.h"
#include "diffuse.h"
#include "bresenhamLine.h"
#include <boost/math/special_functions/round.hpp>

#define NUM_UNIFORM_POINTS_ON_SPHERE 100		// number of uniform points on a sphere
#define LENGTH_BRESENHAM_LINE 1500				// maximum lenghth of bresenham line
void rayTraceFiller(TextIO* inputText, ImageIO* outputImage,
		CallbackFunctions *cb);
