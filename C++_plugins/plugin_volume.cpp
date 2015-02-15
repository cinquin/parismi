/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_volume.h"

using namespace std;

/*!
 * This plugin calculates the volume of each segmentation mask in a protobuf file
 */
void volume(TextIO* inputText, TextIO* outputText, CallbackFunctions *cb) {
	log(cb, 4, "Calculating segmentation mask volume");

	// read input protobuf file
	Protobuf proto(cb);
	proto.readProto(inputText);

	// calculate volume
	Array1D<float> volume(cb);
	volume.resize(proto.getNumberOfSeeds());
	for (int i = 0; i < proto.getNumberOfSeeds(); i++) {
		Array1D<int> x(cb), y(cb), z(cb);
		proto.getSparseSegmentationCoordinates("full", i, &x, &y, &z);
		volume(i) = (float) (x.size());
	}

	// set to protobuf file and write
	proto.setList("volume", &volume);
	proto.writeProto(outputText);
}
