/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "CallbackFunctions.h"
#include "TextIO.h"
#include "Protobuf.h"
#include "Array1D.h"
#include "util.h"

void clearSegmentation(TextIO* inputProtobuf, TextIO* outputProtobuf,
		CallbackFunctions *cb);
