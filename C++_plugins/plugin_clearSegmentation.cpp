/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_clearSegmentation.h"

/*!
 * This plugin deletes embedded full and perimeter segmentations from protobuf file
 */
void clearSegmentation(TextIO* inputProtobuf, TextIO* outputProtobuf,
		CallbackFunctions *cb) {
	log(cb, 4, "Clearing everything but x,y,z coordinates from protobuf file");

	// read in input image
	Protobuf proto(cb);
	proto.readProto(inputProtobuf);

	// delete embedded segmentations
	proto.clearSegmentation();

	// write output
	proto.writeProto(outputProtobuf);

}
