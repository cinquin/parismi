/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_reorderSeedIndices.h"

void reorderSeedIndices(TextIO* inputText, TextIO* outputText,
		CallbackFunctions *cb) {
	log(cb, 4, "Reorder seed indices");

	// read input protobuf files
	Protobuf proto(cb);
	proto.readProto(inputText);

	// reorder indices
	proto.reorderSeedIndices(0);

	// write output image
	proto.writeProto(outputText);

}
