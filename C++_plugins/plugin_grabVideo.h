/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "util.h"
#include "ImageIO.h"
#include <dc1394/dc1394.h>
#include "Image3D.h"
#include "Array1D.h"
#include <iostream>
#include <fstream>
#include <sys/time.h>
#include "Image3D_util.h"
#include <boost/lexical_cast.hpp>

#define READ_FILE 0
#define READ_CAMERA_WRITE_ALL 1
#define READ_CAMERA_WRITE_MOVEMENT 2
#define READ_FILE_WRITE_MOV_SLICE_0 3
#define READ_CAMERA_NO_MOVEMENT_SCORING 4
#define MOVEMENT_NOT_SCORED -99999
#define CHECK_ERR(error,message,errorLevel) if (error!=DC1394_SUCCESS) {log(cb,errorLevel,message);}
#define CHECK_ERR_GOTO(error,message,errorLevel,gotoLabel) if (error!=DC1394_SUCCESS) {\
log(cb,errorLevel,message);\
goto gotoLabel;\
}

int grab_video(ImageIO *inputImage, ImageIO *inputImage2, ImageIO *outputImage,
		ImageIO *outputImage2, CallbackFunctions *cb);
