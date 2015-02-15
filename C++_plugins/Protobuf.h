/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef PROTOBUF_H_
#define PROTOBUF_H_

#include "TextIO.h"
#include "Array1D.h"
#include "CallbackFunctions.h"
#include "util.h"
#include "Image3D_util.h"
#include <iostream>
#include <boost/math/special_functions/round.hpp>

class Protobuf {

private:
	ProtobufDirectory proto;
	CallbackFunctions *cb;
	int _isEmpty;

public:

	Protobuf(CallbackFunctions *callback);
	~Protobuf();
	void displayUserFields();
	float getMaxSeedIdx();
	void reorderSeedIndices(int minIdx);
	float getSeedIdx(int protobuf_index);
	void setSeedIdx(int protobuf_index, float value);
	ProtobufDirectory *getProto();
	void calculateVolume(Array1D<float> *volume);
	void appendSeedData(Protobuf *other, Array1D<int> *protobuf_idx_other);
	void appendXYZ(Array1D<float> *x, Array1D<float> *y, Array1D<float> *z);
	void deleteSeedData(Array1D<int> *protobuf_index);
	void ismember(Array1D<int> *idx, Array1D<int> *idx_other, Protobuf *other);
	int readProto(TextIO *in);
	int writeProto(TextIO *out);
	int readProto(const char *fileName, TextIO *container);
	int parseStringToProtobuf(std::string str);
	void getDimensions(int &dimx, int &dimy, int &dimz);
	void setDimensions(int dimx, int dimy, int dimz);
	int setSparseSegmentationCoordinates(int protobuf_idx, Array1D<int> *xFull,
			Array1D<int> *yFull, Array1D<int> *zFull, Array1D<int> *xPerim,
			Array1D<int> *yPerim, Array1D<int> *zPerim);
	int
			getSparseSegmentationCoordinates(const char *fullOrPerimeter,
					int protobuf_idx, Array1D<int> *x, Array1D<int> *y,
					Array1D<int> *z);
	void getSeedCenter(int protobuf_idx, float &x, float &y, float &z);
	void drawIndividualSegmentation(const char *fullOrPerimeter,
			int protobuf_index, float color, Image3D<float> *I);
	void drawSegmentationImage(const char *fullOrPerimeter, Image3D<float> *I,
			Array1D<float> *color);
	int getNumberOfSeeds();
	int setList(const char *fieldName, Array1D<float> *list);
	int getList(const char *fieldName, Array1D<float> *list);
	void applySegmentationMask(int protobuf_index, Image3D<float> *I,
			Image3D<float> *output);
	void clearSegmentation();
	void thresholdSeedsByPosition(int dimx, int dimy, int dimz);

};

#endif /* PROTOBUF_H_ */
