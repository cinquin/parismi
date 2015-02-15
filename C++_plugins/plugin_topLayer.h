/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "TextIO.h"
#include "CallbackFunctions.h"
#include "Protobuf.h"
#include "util.h"
#include <boost/math/special_functions/round.hpp>
#include "Array1D.h"

void topLayer(TextIO* inputText, TextIO *outputText, CallbackFunctions *cb);
