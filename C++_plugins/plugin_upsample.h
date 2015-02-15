/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "CallbackFunctions.h"
#include "TextIO.h"
#include "Image3D.h"
#include "Protobuf.h"
#include <dispatch/dispatch.h>
#include <boost/math/special_functions/round.hpp>
#include <vector>

void upSample(TextIO* textInput, TextIO* textOutput, CallbackFunctions *cb);
