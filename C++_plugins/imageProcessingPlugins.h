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
#include <iostream>

int imageProcessingPlugins(std::vector<ImageIO*> imageInputs,
		std::vector<TextIO*> textInputs, char *command,
		std::vector<ImageIO*> imageOutputs, std::vector<TextIO*> textOutputs,
		CallbackFunctions *cb);
