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
#include "Protobuf.h"
#include "Array1D.h"
#include "ActiveContourSeed.h"
#include "util.h"
#include <boost/lexical_cast.hpp>

typedef std::vector<ActiveContourSeed*>::size_type vAC_index;
void active_contours(ImageIO* inputImage, TextIO* inputText,
		ImageIO* outputImage, ImageIO* outputMovie, TextIO* outputText,
		bool recordMovie, CallbackFunctions *cb);
void run_active_contours(Protobuf *seedData, Image3D<float> *g,
		Image3D<float> *fullSegmentation, bool useCellSpecificParameters,
		ActiveContourParameters* default_parameters, Image3D<float> *movie,
		int movieSlice, int movieOption, CallbackFunctions *cb);
void recordMovie(Image3D<float> *movie, int t, int movieOption, int movieSlice,
		std::vector<ActiveContourSeed*> *seeds, CallbackFunctions *cb);
void seeds2Proto(std::vector<ActiveContourSeed*> *seeds, Protobuf *proto,
		Image3D<float> *g, CallbackFunctions *cb);
