/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang and Olivier Cinquin.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "ActiveContourSeed.h"

/*!
 * Constructor
 */
ActiveContourSeed::ActiveContourSeed(Protobuf *seedData, int protobuf_index,
		bool useCellSpecificParameters,
		ActiveContourParameters *default_parameters,
		Image3D<float> *fullSegmentation, Image3D<float> *g_full,
		CallbackFunctions *callback) {
	// set up callback
	cb = callback;

	// set active contour parameters
	p_ = new ActiveContourParameters;
	p_->hsz = default_parameters->hsz;
	p_->tMax = default_parameters->tMax;
	p_->dt = default_parameters->dt;
	p_->sussmanInterval = default_parameters->sussmanInterval;
	p_->c = default_parameters->c;
	p_->d = default_parameters->d;
	p_->epsilon = default_parameters->epsilon;
	p_->r = default_parameters->r;
	p_->narrowBandThreshold = default_parameters->narrowBandThreshold;
	p_->res_x = default_parameters->res_x;
	p_->res_y = default_parameters->res_y;
	p_->res_z = default_parameters->res_z;

	// future work: option to use cell specific parameters
	useCellSpecificParameters = 0;
	if (useCellSpecificParameters) {
		log(cb,0,"Functionality to use cell specific parameters is not implemented, using default parameters");
	}

	// set up miscellaneous parameters
	_tElapse = 0;
	float xC, yC, zC;
	seedData->getSeedCenter(protobuf_index, xC, yC, zC);
	_xC = (int) xC;
	_yC = (int) yC;
	_zC = (int) zC;
	_seedIdx = seedData->getSeedIdx(protobuf_index);
	if (_seedIdx == 0.0f) {
		BOOST_THROW_EXCEPTION(Exception("Error: cannot set an active contour seed index to 0; this is reserved for background voxels"));
	}
	_hszX = (int) (p_->hsz / p_->res_x);
	_hszY = (int) (p_->hsz / p_->res_y);
	_hszZ = (int) (p_->hsz / p_->res_z);
	_dimx_Rel = 2 * _hszX + 1;
	_dimy_Rel = 2 * _hszY + 1;
	_dimz_Rel = 2 * _hszZ + 1;
	fullSegmentation->getDimensions(_dimx_Abs, _dimy_Abs, _dimz_Abs);

	// set up narrow band storage
	narrowBand_x = new Array1D<int> (cb);
	narrowBand_y = new Array1D<int> (cb);
	narrowBand_z = new Array1D<int> (cb);
	narrowBand_DphiDt = new Array1D<float> (cb);
	narrowBand_x->resize(_dimx_Rel * _dimy_Rel * _dimz_Rel);
	narrowBand_y->resize(_dimx_Rel * _dimy_Rel * _dimz_Rel);
	narrowBand_z->resize(_dimx_Rel * _dimy_Rel * _dimz_Rel);
	narrowBand_DphiDt->resize(_dimx_Rel * _dimy_Rel * _dimz_Rel);

	// set up fullSeg
	fullSeg = fullSegmentation;

	// set up phi
	phi_ = new Image3D<float> (cb);

	initializePhi(protobuf_index, seedData);

	// set up g
	g = new Image3D<float> (cb);
	g_x = new Image3D<float> (cb);
	g_y = new Image3D<float> (cb);
	g_z = new Image3D<float> (cb);
	initializeG(g_full);
}

/*!
 * destructor
 */
ActiveContourSeed::~ActiveContourSeed() {
	delete phi_;
	phi_ = NULL;
	delete g;
	g = NULL;
	delete g_x;
	phi_ = NULL;
	delete g_y;
	phi_ = NULL;
	delete g_z;
	phi_ = NULL;
	delete p_;
	p_ = NULL;
	delete narrowBand_DphiDt;
	narrowBand_DphiDt = NULL;
	delete narrowBand_x;
	narrowBand_x = NULL;
	delete narrowBand_y;
	narrowBand_y = NULL;
	delete narrowBand_z;
	narrowBand_z = NULL;
}

int ActiveContourSeed::getTmax() {
	return (int) p_->tMax;
}

void ActiveContourSeed::getSparseCoordinates(Array1D<int> *xS,
		Array1D<int> *yS, Array1D<int> *zS, Image3D<float> *bw) {

	// resize x,y,z to maximum size
	int dimx, dimy, dimz;
	bw->getDimensions(dimx, dimy, dimz);
	xS->resize(dimx * dimy * dimz);
	yS->resize(dimx * dimy * dimz);
	zS->resize(dimx * dimy * dimz);

	// iterate through bw and store absolute coordinates of voxels above threshold in x,y,z
	int pixelCount = 0;
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				if ((*bw)(x, y, z) > float(BWTHRESH)) {
					int xAbs, yAbs, zAbs;
					relToAbs(xAbs, yAbs, zAbs, x, y, z);
					(*xS)(pixelCount) = xAbs;
					(*yS)(pixelCount) = yAbs;
					(*zS)(pixelCount) = zAbs;
					pixelCount++;
				}
			}
		}
	}

	// resize x,y,z to minimum size
	xS->resize(pixelCount);
	yS->resize(pixelCount);
	zS->resize(pixelCount);
}

void ActiveContourSeed::getSparseCoordinates_full(Array1D<int> *xS,
		Array1D<int> *yS, Array1D<int> *zS) {

	// get binary segmentation
	Image3D<float> bw(*phi_);
	for (int x = 0; x < _dimx_Rel; x++) {
		for (int y = 0; y < _dimy_Rel; y++) {
			for (int z = 0; z < _dimz_Rel; z++) {
				if (bw(x, y, z) <= 0) {
					bw(x, y, z) = 1;
				} else {
					bw(x, y, z) = 0;
				}
			}
		}
	}

	// get sparse coordinates
	getSparseCoordinates(xS, yS, zS, &bw);
}

void ActiveContourSeed::getSparseCoordinates_perim(Array1D<int> *xS,
		Array1D<int> *yS, Array1D<int> *zS) {

	// get binary segmentation
	Image3D<float> bw(*phi_);
	for (int x = 0; x < _dimx_Rel; x++) {
		for (int y = 0; y < _dimy_Rel; y++) {
			for (int z = 0; z < _dimz_Rel; z++) {
				if (bw(x, y, z) <= 0) {
					bw(x, y, z) = 1;
				} else {
					bw(x, y, z) = 0;
				}
			}
		}
	}
	perim(&bw, 1);

	// get sparse coordinates
	getSparseCoordinates(xS, yS, zS, &bw);
}

/*!
 * Initialize g
 */
void ActiveContourSeed::initializeG(Image3D<float> *g_full) {
	// set resolution
	g->setResolution(p_->res_x, p_->res_y, p_->res_z);
	g_x->setResolution(p_->res_x, p_->res_y, p_->res_z);
	g_y->setResolution(p_->res_x, p_->res_y, p_->res_z);
	g_z->setResolution(p_->res_x, p_->res_y, p_->res_z);

	// initialize image
	g->resize(_dimx_Rel, _dimy_Rel, _dimz_Rel);
	g_x->resize(_dimx_Rel, _dimy_Rel, _dimz_Rel);
	g_y->resize(_dimx_Rel, _dimy_Rel, _dimz_Rel);
	g_z->resize(_dimx_Rel, _dimy_Rel, _dimz_Rel);

	// get g subwindow
	for (int x = 0; x < _dimx_Rel; x++) {
		for (int y = 0; y < _dimy_Rel; y++) {
			for (int z = 0; z < _dimz_Rel; z++) {
				int xAbs, yAbs, zAbs;
				relToAbs(xAbs, yAbs, zAbs, x, y, z);
				(*g)(x, y, z) = (*g_full)(xAbs, yAbs, zAbs);
			}
		}
	}

	// pre-calculate partial derivatives of g
	for (int x = 0; x < _dimx_Rel; x++) {
		for (int y = 0; y < _dimy_Rel; y++) {
			for (int z = 0; z < _dimz_Rel; z++) {
				(*g_x)(x, y, z) = g->getDdxVal(x, y, z);
				(*g_y)(x, y, z) = g->getDdyVal(x, y, z);
				(*g_z)(x, y, z) = g->getDdzVal(x, y, z);
			}
		}
	}

}

/*!
 * Allows you to go from relative coordinates (i.e., coordinates based on window) to absolute
 *   coordinates (i.e., coordinates based on full image).
 */
void ActiveContourSeed::relToAbs(int &xAbs, int &yAbs, int &zAbs, int x, int y,
		int z) {
	xAbs = x - _hszX + _xC;
	yAbs = y - _hszY + _yC;
	zAbs = z - _hszZ + _zC;

	// handle boundary conditions
	if (xAbs < 0) {
		xAbs = 0;
	}
	if (yAbs < 0) {
		yAbs = 0;
	}
	if (zAbs < 0) {
		zAbs = 0;
	}
	if (xAbs >= _dimx_Abs) {
		xAbs = _dimx_Abs - 1;
	}
	if (yAbs >= _dimy_Abs) {
		yAbs = _dimy_Abs - 1;
	}
	if (zAbs >= _dimz_Abs) {
		zAbs = _dimz_Abs - 1;
	}
}

void ActiveContourSeed::absToRel(int &xRel, int &yRel, int &zRel, int x, int y,
		int z) {
	xRel = x - _xC + _hszX;
	yRel = y - _yC + _hszY;
	zRel = z - _zC + _hszZ;

	// handle boundary conditions
	if (xRel < 0) {
		xRel = 0;
	}
	if (yRel < 0) {
		yRel = 0;
	}
	if (zRel < 0) {
		zRel = 0;
	}
	if (xRel >= _dimx_Rel) {
		xRel = _dimx_Rel - 1;
	}
	if (yRel >= _dimy_Rel) {
		yRel = _dimy_Rel - 1;
	}
	if (zRel >= _dimz_Rel) {
		zRel = _dimz_Rel - 1;
	}
}

void ActiveContourSeed::updateFullSeg(Image3D<float> *phi) {
	for (int x = 0; x < _dimx_Rel; x++) {
		for (int y = 0; y < _dimy_Rel; y++) {
			for (int z = 0; z < _dimz_Rel; z++) {
				if ((*phi)(x, y, z) <= 0) {
					int xAbs, yAbs, zAbs;
					relToAbs(xAbs, yAbs, zAbs, x, y, z);
					float fsIdx = (*fullSeg)(xAbs, yAbs, zAbs);
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wfloat-equal"
					if (fsIdx > 0 && fsIdx != _seedIdx)
#pragma clang diagnostic pop
						(*fullSeg)(xAbs, yAbs, zAbs) = -2.0f;// Multiple contours collided at this position
					else if (fsIdx > 0) // Not already marked as collision area
						(*fullSeg)(xAbs, yAbs, zAbs) = _seedIdx;
				}
			}
		}
	}
}

/*!
 * Initialize phi
 */
void ActiveContourSeed::initializePhi(int protobuf_idx, Protobuf *proto) {

	// set resolution
	phi_->setResolution(p_->res_x, p_->res_y, p_->res_z);

	// initialize image
	phi_->resize(_dimx_Rel, _dimy_Rel, _dimz_Rel);

	// get cell centers, previous sparse segmentation from protobuf file
	Array1D<int> xAbs(cb), yAbs(cb), zAbs(cb);
	proto->getSparseSegmentationCoordinates("full", protobuf_idx, &xAbs, &yAbs,
			&zAbs);

	// if previous sparse segmentation is empty, then initialize phi from single point
	if (xAbs.size() == 0) {
		for (int x = 0; x < _dimx_Rel; x++) {
			for (int y = 0; y < _dimy_Rel; y++) {
				for (int z = 0; z < _dimz_Rel; z++) {
					float xRadial = (float) (x - _hszX);
					float yRadial = (float) (y - _hszY);
					float zRadial = (float) (z - _hszZ);
					(*phi_)(x, y, z) = sqrtf(
							xRadial * xRadial + yRadial * yRadial + zRadial
									* zRadial) - p_->r;
				}
			}
		}
		// otherwise, initialize phi using distance transform on previous segmentation
	} else {
		// get previous binary segmentation, store in phi
		for (int p = 0; p < xAbs.size(); p++) {
			int xRel, yRel, zRel;
			absToRel(xRel, yRel, zRel, xAbs(p), yAbs(p), zAbs(p));
			(*phi_)(xRel, yRel, zRel) = 1;
		}

		// get perimeter segmentation and store in phi.  Store original segmentation in temp
		Image3D<float> temp(*phi_);
		perim(phi_, 1);

		// calculate distance transform
		bwdist(phi_);

		// Make distances negative inside the segmentation
		for (int x = 0; x < _dimx_Rel; x++) {
			for (int y = 0; y < _dimy_Rel; y++) {
				for (int z = 0; z < _dimz_Rel; z++) {
					if (temp(x, y, z) > float(BWTHRESH)) {
						(*phi_)(x, y, z) = -(*phi_)(x, y, z);
					}
				}
			}
		}
	}

	updateFullSeg(phi_);
}

void ActiveContourSeed::writePhi(ImageIO *container) {
	phi_->write(container);
}
void ActiveContourSeed::writeG(ImageIO *container) {
	g->write(container);
}

void ActiveContourSeed::preUpdatePhi() {
	narrowBand_count = 0;
	for (int x = 0; x < _dimx_Rel; x++) {
		for (int y = 0; y < _dimy_Rel; y++) {
			for (int z = 0; z < _dimz_Rel; z++) {

				// check if voxel falls in narrow band range
				if ((*phi_)(x, y, z) > p_->narrowBandThreshold || (*phi_)(x, y,
						z) < -(p_->narrowBandThreshold)) {
					continue;
				}

				int xAbs, yAbs, zAbs;
				relToAbs(xAbs, yAbs, zAbs, x, y, z);
				float fsIdx = (*fullSeg)(xAbs, yAbs, zAbs);
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wfloat-equal"
				if (fsIdx == -2.0f) {
#pragma clang diagnostic pop
					// Old collision; do not update Phi here
					continue;
				}

				/*
				 // Check for collisions if active contours are running forward
				 if (p->c>=0) {
				 int xAbs,yAbs,zAbs;
				 relToAbs(xAbs,yAbs,zAbs,x,y,z);
				 if ((*fullSeg)(xAbs,yAbs,zAbs)!=0 && (*fullSeg)(xAbs,yAbs,zAbs)!=_seedIdx) {
				 continue;
				 }
				 }*/

				// calculate derivatives of phi
				float phix, phiy, phiz, phixx, phiyy, phizz, phixz, phixy,
						phiyz;
				if (isCloseTo(p_->res_x, 1.0f) && isCloseTo(p_->res_y, 1.0f)) {
					phi_->getFastDerivatives(x, y, z, phix, phiy, phiz, phixx,
							phiyy, phizz, phixy, phixz, phiyz);
				} else {
					phix = phi_->getDdxVal(x, y, z);
					phiy = phi_->getDdyVal(x, y, z);
					phiz = phi_->getDdzVal(x, y, z);
					phixx = phi_->getD2dx2Val(x, y, z);
					phiyy = phi_->getD2dy2Val(x, y, z);
					phizz = phi_->getD2dz2Val(x, y, z);
					phixz = phi_->getD2dxzVal(x, y, z);
					phixy = phi_->getD2dxyVal(x, y, z);
					phiyz = phi_->getD2dyzVal(x, y, z);
				}

				// calculate curvature
				float H = (phizz * phiy * phiy - 2.0f * phiz * phiy * phiyz
						+ phiyy * phiz * phiz + phizz * phix * phix + phiyy
						* phix * phix - 2.0f * phiz * phix * phixz - 2.0f
						* phiy * phix * phixy + phixx * phiz * phiz + phixx
						* phiy * phiy) / (powf(
						phix * phix + phiy * phiy + phiz * phiz, 1.5f) + EPS);
				H = H * sgn(p_->c);

				// calculate derivatives of g
				float G = 1 - (*g)(x, y, z);
				float Gx = (*g_x)(x, y, z);
				float Gy = (*g_y)(x, y, z);
				float Gz = (*g_z)(x, y, z);

				// calculate update
				float c = p_->c;
				float d = p_->d;
				float epsilon = p_->epsilon;
				float dphidt = -G * c * (1.0f - epsilon * H) * sqrtf(
						phix * phix + phiy * phiy + phiz * phiz) - d * (Gx
						* phix + Gy * phiy + Gz * phiz);

				(*narrowBand_DphiDt)(narrowBand_count) = dphidt;
				(*narrowBand_x)(narrowBand_count) = x;
				(*narrowBand_y)(narrowBand_count) = y;
				(*narrowBand_z)(narrowBand_count) = z;
				narrowBand_count++;
			}
		}
	}

	for (int k = 0; k < narrowBand_count; k++) {
		// Update fullSeg, which allows detection of collisions
		int x = (*narrowBand_x)(k);
		int y = (*narrowBand_y)(k);
		int z = (*narrowBand_z)(k);
		float dphidt = (*narrowBand_DphiDt)(k);
		float newPhi = (*phi_)(x, y, z) + (p_->dt) * dphidt;
		//(*phi)(x,y,z) = newPhi;

		if (newPhi <= 0) {
			int xAbs, yAbs, zAbs;
			relToAbs(xAbs, yAbs, zAbs, x, y, z);
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wfloat-equal"
			//Old collision; do not update fullseg
			if ((*fullSeg)(xAbs, yAbs, zAbs) == -2.0f) {
				continue;
			}

			if ((*fullSeg)(xAbs, yAbs, zAbs) != 0.0f &&
					(*fullSeg)(xAbs, yAbs, zAbs) != _seedIdx) {
				//New collision
				(*fullSeg)(xAbs, yAbs, zAbs) = -1.0f;
			} else {
				(*fullSeg)(xAbs, yAbs, zAbs) = _seedIdx;
				if ((*fullSeg)(xAbs, yAbs, zAbs) != _seedIdx) {
					//This might not be sufficient to take care of race conditions, but it's cheap
					(*fullSeg)(xAbs, yAbs, zAbs) = -1.0f;
				}
			}
#pragma clang diagnostic pop
		}
	}
}

void ActiveContourSeed::updatePhi() {
	if (isFinished()) {
		return;
	}
	for (int k = 0; k < narrowBand_count; k++) {
		int x = (*narrowBand_x)(k);
		int y = (*narrowBand_y)(k);
		int z = (*narrowBand_z)(k);
		int xAbs, yAbs, zAbs;
		relToAbs(xAbs, yAbs, zAbs, x, y, z);

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wfloat-equal"
		if ((*fullSeg)(xAbs, yAbs, zAbs) == -1.0f) {
			(*fullSeg)(xAbs, yAbs, zAbs) = -2.0f;
		}

		float dphidt = (*narrowBand_DphiDt)(k);
		float newPhi = (*phi_)(x, y, z) + (p_->dt) * dphidt;

		if ((*fullSeg)(xAbs, yAbs, zAbs) != -2.0f || newPhi > 0) {
#pragma clang diagnostic pop
			(*phi_)(x, y, z) = newPhi;
		}
	}

	// Reinitialize phi
	if (_tElapse % p_->sussmanInterval == 0) {
		sussmanReinitialization();
	}
}

void ActiveContourSeed::sussmanReinitialization() {
	for (int x = 0; x < _dimx_Rel; x++) {
		for (int y = 0; y < _dimy_Rel; y++) {
			for (int z = 0; z < _dimz_Rel; z++) {
				(*phi_)(x, y, z) = (*phi_)(x, y, z) - (p_->dt)
						* (phi_->sussman(x, y, z));
			}
		}
	}
}

bool ActiveContourSeed::isFinished() {
	return (float) _tElapse > p_->tMax;
}

void ActiveContourSeed::step() {

	// Do not update past tMax
	_tElapse++;
	if (isFinished()) {
		return;
	}

	preUpdatePhi();

}

float ActiveContourSeed::getSeedIdx() {
	return _seedIdx;
}

bool ActiveContourSeed::containsXSlice(int xSlice) {
	if (_xC - _hszX <= xSlice && _xC + _hszX >= xSlice) {
		return true;
	}
	return false;
}

bool ActiveContourSeed::containsYSlice(int ySlice) {
	if (_yC - _hszY <= ySlice && _yC + _hszY >= ySlice) {
		return true;
	}
	return false;
}

bool ActiveContourSeed::containsZSlice(int zSlice) {
	if (_zC - _hszZ <= zSlice && _zC + _hszZ >= zSlice) {
		return true;
	}
	return false;
}

