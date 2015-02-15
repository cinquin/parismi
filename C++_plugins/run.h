/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include <cstdarg>
#include "definitions.h"
#include "CallbackFunctions.h"
#include <syslog.h>
#include "ImageIO.h"
#include "TextIO.h"
#include "TextIO_sharedLibrary.h"
#include "ImageIO_sharedLibrary.h"
#include "imageProcessingPlugins.h"
#include "gitVersion.h"
#include <boost/exception/diagnostic_information.hpp>
typedef std::vector<TextIO*>::size_type vTI;

extern "C" int run(float *transferBuffer, ImageDimensions *imageDimensions,
		CallbackFunctions *cb, const char *stringArg, ...);

extern "C" void getVersion(char * buffer, int maxLength);
