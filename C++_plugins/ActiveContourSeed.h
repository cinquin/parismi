/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef ACTIVECONTOURSEED_H_
#define ACTIVECONTOURSEED_H_

#include "CallbackFunctions.h"
#include "Array1D.h"
#include "Image3D.h"
#include "Image3D_util.h"
#include "Protobuf.h"
#include <cmath>
#include "util.h"
#include "Exception.h"

struct ActiveContourParameters {
	float hsz; // window radius
	float tMax; // how long to run active contours
	float dt; // time step
	int sussmanInterval;
	float c;
	float d; // how long to run the additional free-form active contours
	float epsilon;
	float r;
	float narrowBandThreshold;
	float res_x;
	float res_y;
	float res_z;
};

class ActiveContourSeed {
private:
	Image3D<float>* __restrict__ phi_;
	Image3D<float>* __restrict__ g;
	Image3D<float>* __restrict__ g_x;
	Image3D<float>* __restrict__ g_y;
	Image3D<float>* __restrict__ g_z;
	Image3D<float>* __restrict__ fullSeg; // this allows seeds to collide
	CallbackFunctions *cb;
	ActiveContourParameters *p_;
	int _tElapse;
	int _xC;
	int _yC;
	int _zC;
	float _seedIdx;
	int _dimx_Abs;
	int _dimy_Abs;
	int _dimz_Abs;
	int _dimx_Rel;
	int _dimy_Rel;
	int _dimz_Rel;
	int _hszX;
	int _hszY;
	int _hszZ;

	Array1D<float> *narrowBand_DphiDt;
	Array1D<int> *narrowBand_x;
	Array1D<int> *narrowBand_y;
	Array1D<int> *narrowBand_z;
	int narrowBand_count;

public:

	ActiveContourSeed(Protobuf *seedData, int protobuf_index,
			bool useCellSpecificParameters,
			ActiveContourParameters *default_parameters,
			Image3D<float> *fullSegmentation, Image3D<float> *g_full,
			CallbackFunctions *callback);

	~ActiveContourSeed();

	int getTmax();

	void getSparseCoordinates(Array1D<int> *xS, Array1D<int> *yS,
			Array1D<int> *zS, Image3D<float> *bw);

	void getSparseCoordinates_full(Array1D<int> *xS, Array1D<int> *yS,
			Array1D<int> *zS);

	void getSparseCoordinates_perim(Array1D<int> *xS, Array1D<int> *yS,
			Array1D<int> *zS);

	void initializeG(Image3D<float> *g_full);

	void relToAbs(int &xAbs, int &yAbs, int &zAbs, int x, int y, int z);

	void absToRel(int &xRel, int &yRel, int &zRel, int x, int y, int z);

	void updateFullSeg(Image3D<float> *phi);

	void initializePhi(int protobuf_idx, Protobuf *proto);

	void writePhi(ImageIO *container);

	void writeG(ImageIO *container);

	void preUpdatePhi();

	void updatePhi();

	void sussmanReinitialization();

	bool isFinished();

	void step();

	float getSeedIdx();

	bool containsXSlice(int xSlice);bool containsYSlice(int ySlice);bool
			containsZSlice(int zSlice);

};

#endif
