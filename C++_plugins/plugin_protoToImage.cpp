/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "plugin_protoToImage.h"

using namespace std;

void proto2image(TextIO* inputText, ImageIO* outputImage, CallbackFunctions *cb) {

	log(cb, 4, "Drawing protobuf segmentation");

	// read input protobuf file
	Protobuf proto(cb);
	proto.readProto(inputText);

	// get parameters
	log(cb, 4, "Prompt: 0 0 COLOR");
	const char **work_storage = cb->getMoreWork();
	string fieldName(work_storage[2]);
	cb->freeGetMoreWork(work_storage);

	// get segmentation image where segmentations are colored by protobuf_index+1
	Image3D<float> segmentation(cb);
	Array1D<float> color(cb);
	proto.getList(fieldName.c_str(), &color);
	proto.drawSegmentationImage("Perimeter", &segmentation, &color);

	// write out segmentation image
	segmentation.write(outputImage);

}
