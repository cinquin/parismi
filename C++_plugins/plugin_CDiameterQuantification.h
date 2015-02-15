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
#include "bresenhamLine.h"
#include <boost/lexical_cast.hpp>

void CDiameterQuantification(ImageIO* inputImage, TextIO* inputText,
		ImageIO* outputImage, TextIO* outputText, CallbackFunctions *cb);
