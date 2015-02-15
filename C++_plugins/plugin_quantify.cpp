/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_quantify.h"

using namespace std;

void quantify(CallbackFunctions *cb) {
	log(cb, 4, "Quantifying fluorescence intensity based on segmentation masks");
	log(cb, 0, "This plugin (quantify) is deprecated, use CellBallQuantify");
}
