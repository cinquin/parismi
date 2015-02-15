/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "CallbackFunctions.h"
#include "ImageIO.h"
#include "TextIO.h"
#include "Image3D.h"
#include "Protobuf.h"
#include <boost/math/special_functions/round.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/lexical_cast.hpp>

#define PIXEL_SEARCH_RANGE 1
void djikstraDistance(TextIO* inputText, TextIO* outputText,
		CallbackFunctions *cb);
