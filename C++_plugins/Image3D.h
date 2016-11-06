/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef IMAGE3D_H_
#define IMAGE3D_H_

#include "CallbackFunctions.h"
#include <boost/multi_array.hpp>
#include "ImageIO.h"
#include "definitions.h"
#include "eigenvalue_eigenvector_3x3_matrix.h"
#include <boost/math/special_functions/fpclassify.hpp>
#include "util.h"

template<class T>
class Image3D {
private:
	boost::multi_array<T, 3>* __restrict__ I;
	float pixelsPerMicron_x;
	float pixelsPerMicron_y;
	float pixelsPerMicron_z;
	CallbackFunctions *cb;

public:
	/*!
	 * Constructor
	 */
	Image3D(CallbackFunctions *callback) {
		I = new boost::multi_array<T, 3>(boost::extents[0][0][0]);
		cb = callback;
		pixelsPerMicron_x = 1;
		pixelsPerMicron_y = 1;
		pixelsPerMicron_z = 1;
	}

	/*
	 * Copy contructor
	 */
	Image3D(const Image3D& other) {
		// deep copy of boost matrix
		int dimx, dimy, dimz;
		other.getDimensions(dimx, dimy, dimz);
		I = new boost::multi_array<T, 3>(boost::extents[dimx][dimy][dimz]);
		(*I) = *(other.I);

		pixelsPerMicron_x = other.pixelsPerMicron_x;
		pixelsPerMicron_y = other.pixelsPerMicron_y;
		pixelsPerMicron_z = other.pixelsPerMicron_z;
		cb = other.cb;
	}

	/*!
	 * destructor
	 */
	~Image3D() {
		delete I;
		I = NULL;
	}

	/*!
	 * Access/mutate x,y,z voxel of image.
	 */
	T& operator()(int x, int y, int z) {
		return (*I)[x][y][z];
	}

	T max() {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		T maxVal = -INF;
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					maxVal = std::max(maxVal, (*I)[x][y][z]);
				}
			}
		}
		return maxVal;
	}

	void operator=(const Image3D& other) {
		// deep copy of boost matrix
		int dimx, dimy, dimz;
		other.getDimensions(dimx, dimy, dimz);
		I->resize(boost::extents[dimx][dimy][dimz]);
		(*I) = *(other.I);

		pixelsPerMicron_x = other.pixelsPerMicron_x;
		pixelsPerMicron_y = other.pixelsPerMicron_y;
		pixelsPerMicron_z = other.pixelsPerMicron_z;
		cb = other.cb;
	}

	/*!
	 * Add a constant value to all voxels of the image
	 */
	void operator+(T val) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = (*I)[x][y][z] + val;
				}
			}
		}
	}

	/*!
	 * Subtract a constant value to all voxels of the image
	 */
	void operator-(T val) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = (*I)[x][y][z] - val;
				}
			}
		}
	}

	/*!
	 * Multiply through all voxels with a constant value
	 */
	void operator*(T val) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = (*I)[x][y][z] * val;
				}
			}
		}
	}

	/*!
	 * Divide through all voxels with a constant value
	 */
	void operator/(T val) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = (*I)[x][y][z] / val;
				}
			}
		}
	}

	/*!
	 * Add another image voxel by voxel
	 */
	void operator+(Image3D I2) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int dimx2, dimy2, dimz2;
		I2.getDimensions(dimx2, dimy2, dimz2);
		if (dimx != dimx2 || dimy != dimy2 || dimz != dimz2) {
			log(
					cb,
					1,
					"Images are not the same size (dimx=%d,dimy=%d,dimz=%d vs. dimx=%d,dimy=%d,dimz=%d)",
					dimx, dimy, dimz, dimx2, dimy2, dimz2);
			throw 999;
		}
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = (*I)[x][y][z] + I2(x, y, z);
				}
			}
		}
	}

	/*!
	 * Subtract another image voxel by voxel
	 */
	void operator-(Image3D I2) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int dimx2, dimy2, dimz2;
		I2.getDimensions(dimx2, dimy2, dimz2);
		if (dimx != dimx2 || dimy != dimy2 || dimz != dimz2) {
			log(
					cb,
					1,
					"Images are not the same size (dimx=%d,dimy=%d,dimz=%d vs. dimx=%d,dimy=%d,dimz=%d)",
					dimx, dimy, dimz, dimx2, dimy2, dimz2);
			throw 999;
		}
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = (*I)[x][y][z] - I2(x, y, z);
				}
			}
		}
	}

	/*!
	 * Multiply another image voxel by voxel
	 */
	void operator*(Image3D I2) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int dimx2, dimy2, dimz2;
		I2.getDimensions(dimx2, dimy2, dimz2);
		if (dimx != dimx2 || dimy != dimy2 || dimz != dimz2) {
			log(
					cb,
					1,
					"Images are not the same size (dimx=%d,dimy=%d,dimz=%d vs. dimx=%d,dimy=%d,dimz=%d)",
					dimx, dimy, dimz, dimx2, dimy2, dimz2);
			throw 999;
		}
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = (*I)[x][y][z] * I2(x, y, z);
				}
			}
		}
	}

	/*!
	 * Divide another image voxel by voxel
	 */
	void operator/(Image3D I2) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int dimx2, dimy2, dimz2;
		I2.getDimensions(dimx2, dimy2, dimz2);
		if (dimx != dimx2 || dimy != dimy2 || dimz != dimz2) {
			log(
					cb,
					1,
					"Images are not the same size (dimx=%d,dimy=%d,dimz=%d vs. dimx=%d,dimy=%d,dimz=%d)",
					dimx, dimy, dimz, dimx2, dimy2, dimz2);
			throw 999;
		}
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = (*I)[x][y][z] / I2(x, y, z);
				}
			}
		}
	}

	/*!
	 * Reads the entire 3D image specified by inputImage into boost matrix I
	 */
	void read(ImageIO *inputImage) {
		inputImage->openForSequentialRead();
		int dimx, dimy, dimz;
		inputImage->getDimensions(dimx, dimy, dimz);
		I->resize(boost::extents[dimx][dimy][dimz]);
		for (int z = 0; z < dimz; z++) {
			inputImage->read(I, z, z, 0);
		}
		inputImage->close();
	}

	/*!
	 * Reads a single frame of 3D image into 2D image
	 * You must resize boost matrix manually (should be dimx,dimy,1)
	 * You must open/close inputImage manually with:
	 *   inputImage->openForSequentialRead();
	 *   inputImage->close();
	 */
	void readFrame(int zSlice, ImageIO* inputImage) {
		inputImage->read(I, zSlice, 0, 0);
	}

	/*!
	 * Writes a single frame into a 3D image
	 * You must reisze boost matrix manually (should be dimx,dimy,1)
	 * You must open/close outputImage manually
	 */
	void writeFrame(int zSlice, ImageIO* outputImage) {
		outputImage->write(I, zSlice, 0);
	}

	/*!
	 * Writes a single frame of 3D matrix to specific slice of output image.
	 * This method does not work binary-side (only via ImageJ).
	 */
	void writeFrameToSlice(int zSlice, ImageIO* outputImage, int outputSlice) {
		outputImage->writeToSlice(I, zSlice, 0, outputSlice);
	}

	/*!
	 * Sets boost matrix I
	 */
	void setBoost(boost::multi_array<T, 3> *input) {
		*I = *input;
	}

	/*!
	 * Writes the entire boost matrix I to image specified by outputImage
	 */
	void write(ImageIO *outputImage) {
		outputImage->openForSequentialWrite();
		int dimy, dimx, dimz;
		getDimensions(dimx, dimy, dimz);
		for (int z = 0; z < dimz; z++) {
			outputImage->write(I, z, 0);
		}
		outputImage->close();
	}

	/*!
	 * Returns x,y,z dimensions of the boost matrix.
	 */
	void getDimensions(int &dimx, int &dimy, int &dimz) const {
		const boost::multi_array_types::size_type* dim = I->shape();
		dimx = (int) dim[0];
		dimy = (int) dim[1];
		dimz = (int) dim[2];
	}

	/*!
	 * Sets x, y, z resolution of image
	 */
	void setResolution(float micronsPerPixelX, float micronsPerPixelY,
			float micronsPerPixelZ) {
		pixelsPerMicron_x = 1 / micronsPerPixelX;
		pixelsPerMicron_y = 1 / micronsPerPixelY;
		pixelsPerMicron_z = 1 / micronsPerPixelZ;
	}

	/*!
	 * Interpolate between pixels floor(xC), floor(yC), floor(zC)
	 *    and ceil(xC), ceil(yC), ceil(zC) in order to find value
	 *    at pixel xC,yC,zC
	 */
	T interpolate(float xC, float yC, float zC) {
		int x = (int) xC;
		int y = (int) yC;
		int z = (int) zC;

		float xd = xC - floorf(xC);
		float yd = yC - floorf(yC);
		float zd = zC - floorf(zC);

		// boundary cases
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int zp1 = z + 1, yp1 = y + 1, xp1 = x + 1;
		if (zp1 < 0) {
			zp1 = 0;
		}
		if (zp1 >= dimz) {
			zp1 = dimz - 1;
		}
		if (xp1 < 0) {
			xp1 = 0;
		}
		if (xp1 >= dimx) {
			xp1 = dimx - 1;
		}
		if (yp1 < 0) {
			yp1 = 0;
		}
		if (yp1 >= dimy) {
			yp1 = dimy - 1;
		}

		// trilinear interpolation
		float i1 = (*I)[x][y][z] * (1 - zd) + (*I)[x][y][zp1] * zd;
		float i2 = (*I)[x][yp1][z] * (1 - zd) + (*I)[x][yp1][zp1] * zd;
		float j1 = (*I)[xp1][y][z] * (1 - zd) + (*I)[xp1][y][zp1] * zd;
		float j2 = (*I)[xp1][yp1][z] * (1 - zd) + (*I)[xp1][yp1][zp1] * zd;
		float w1 = i1 * (1 - yd) + i2 * yd;
		float w2 = j1 * (1 - yd) + j2 * yd;
		return (T) (w1 * (1 - xd) + w2 * xd);
	}

	/*!
	 *  Trilinear interpolation
	 *  Interpolate between pixels x0,y0,z0 and x0+dx,y0+dy,z0+dz
	 *  dy,dx,dz should be 1, -1, or 0
	 */
	T interpolate(int x, int y, int z, int dx, int dy, int dz) {
		float xd = pixelsPerMicron_x * (float) abs(dx);
		float yd = pixelsPerMicron_y * (float) abs(dy);
		float zd = pixelsPerMicron_z * (float) abs(dz);

		// boundary cases
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int zp1 = z + dz, yp1 = y + dy, xp1 = x + dx;
		if (zp1 < 0) {
			zp1 = 0;
		}
		if (zp1 >= dimz) {
			zp1 = dimz - 1;
		}
		if (xp1 < 0) {
			xp1 = 0;
		}
		if (xp1 >= dimx) {
			xp1 = dimx - 1;
		}
		if (yp1 < 0) {
			yp1 = 0;
		}
		if (yp1 >= dimy) {
			yp1 = dimy - 1;
		}

		// trilinear interpolation
		float i1 = (*I)[x][y][z] * (1 - zd) + (*I)[x][y][zp1] * zd;
		float i2 = (*I)[x][yp1][z] * (1 - zd) + (*I)[x][yp1][zp1] * zd;
		float j1 = (*I)[xp1][y][z] * (1 - zd) + (*I)[xp1][y][zp1] * zd;
		float j2 = (*I)[xp1][yp1][z] * (1 - zd) + (*I)[xp1][yp1][zp1] * zd;
		float w1 = i1 * (1 - yd) + i2 * yd;
		float w2 = j1 * (1 - yd) + j2 * yd;
		return (T) (w1 * (1 - xd) + w2 * xd);
	}

	T getDdxVal(int x, int y, int z) {
		float I_xp1_y_z, I_xm1_y_z;
		if (isCloseTo(pixelsPerMicron_x, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int xp1 = x + 1, xm1 = x - 1;
			if (xp1 >= dimx) {
				xp1 = dimx - 1;
			}
			if (xm1 < 0) {
				xm1 = 0;
			}
			// get I(x+1,y,z) and I(x-1,y,z)
			I_xp1_y_z = (*I)[xp1][y][z];
			I_xm1_y_z = (*I)[xm1][y][z];
		} else {
			// get I(x+1,y,z) and I(x-1,y,z) through interpolation
			I_xp1_y_z = interpolate(x, y, z, 1, 0, 0);
			I_xm1_y_z = interpolate(x, y, z, -1, 0, 0);
		}
		// calculate derivative
		return 0.5f * (I_xp1_y_z - I_xm1_y_z);
	}

	T getD2dx2Val(int x, int y, int z) {
		float I_x_y_z, I_xp1_y_z, I_xm1_y_z;
		if (isCloseTo(pixelsPerMicron_x, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int xp1 = x + 1, xm1 = x - 1;
			if (xp1 >= dimx) {
				xp1 = dimx - 1;
			}
			if (xm1 < 0) {
				xm1 = 0;
			}
			// get I(x,y,z), I(x+1,y,z) and I(x-1,y,z)
			I_x_y_z = (*I)[x][y][z];
			I_xp1_y_z = (*I)[xp1][y][z];
			I_xm1_y_z = (*I)[xm1][y][z];
		} else {
			// get I(x,y,z), I(x+1,y,z) and I(x-1,y,z) through interpolation
			I_x_y_z = (*I)[x][y][z];
			I_xp1_y_z = interpolate(x, y, z, 1, 0, 0);
			I_xm1_y_z = interpolate(x, y, z, -1, 0, 0);
		}
		return (I_xp1_y_z - 2.0f * I_x_y_z + I_xm1_y_z);
	}

	T getDdyVal(int x, int y, int z) {
		float I_x_yp1_z, I_x_ym1_z;
		if (isCloseTo(pixelsPerMicron_y, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int yp1 = y + 1, ym1 = y - 1;
			if (yp1 >= dimy) {
				yp1 = dimy - 1;
			}
			if (ym1 < 0) {
				ym1 = 0;
			}
			// get I(x,y+1,z) and I(x,y-1,z)
			I_x_yp1_z = (*I)[x][yp1][z];
			I_x_ym1_z = (*I)[x][ym1][z];
		} else {
			// get I(x,y+1,z) and I(x,y-1,z) through interpolation
			I_x_yp1_z = interpolate(x, y, z, 0, 1, 0);
			I_x_ym1_z = interpolate(x, y, z, 0, -1, 0);
		}
		return 0.5f * (I_x_yp1_z - I_x_ym1_z);
	}

	T getD2dy2Val(int x, int y, int z) {
		float I_x_y_z, I_x_yp1_z, I_x_ym1_z;
		if (isCloseTo(pixelsPerMicron_y, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int yp1 = y + 1, ym1 = y - 1;
			if (yp1 >= dimy) {
				yp1 = dimy - 1;
			}
			if (ym1 < 0) {
				ym1 = 0;
			}
			// get I(x,y,z),I(x,y+1,z) and I(x,y-1,z)
			I_x_y_z = (*I)[x][y][z];
			I_x_yp1_z = (*I)[x][yp1][z];
			I_x_ym1_z = (*I)[x][ym1][z];
		} else {
			// get I(x,y,z), I(x,y+1,z) and I(x,y-1,z) through interpolation
			I_x_y_z = (*I)[x][y][z];
			I_x_yp1_z = interpolate(x, y, z, 0, 1, 0);
			I_x_ym1_z = interpolate(x, y, z, 0, -1, 0);
		}
		return I_x_yp1_z - 2.0f * I_x_y_z + I_x_ym1_z;
	}

	T getDdzVal(int x, int y, int z) {
		float I_x_y_zp1, I_x_y_zm1;
		if (isCloseTo(pixelsPerMicron_z, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int zp1 = z + 1, zm1 = z - 1;
			if (zp1 >= dimz) {
				zp1 = dimz - 1;
			}
			if (zm1 < 0) {
				zm1 = 0;
			}
			// get I(x,y,z+1) and I(x,y,z-1)
			I_x_y_zp1 = (*I)[x][y][zp1];
			I_x_y_zm1 = (*I)[x][y][zm1];
		} else {
			// get I(x,y,z+1) and I(x,y,z-1) through interpolation
			I_x_y_zp1 = interpolate(x, y, z, 0, 0, 1);
			I_x_y_zm1 = interpolate(x, y, z, 0, 0, -1);
		}

		return 0.5f * (I_x_y_zp1 - I_x_y_zm1);
	}

	T getD2dz2Val(int x, int y, int z) {
		float I_x_y_z, I_x_y_zp1, I_x_y_zm1;
		if (isCloseTo(pixelsPerMicron_z, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int zp1 = z + 1, zm1 = z - 1;
			if (zp1 >= dimz) {
				zp1 = dimz - 1;
			}
			if (zm1 < 0) {
				zm1 = 0;
			}
			// get I(x,y,z),I(x,y,z+1) and I(x,y,z-1)
			I_x_y_z = (*I)[x][y][z];
			I_x_y_zp1 = (*I)[x][y][zp1];
			I_x_y_zm1 = (*I)[x][y][zm1];
		} else {
			// get I(x,y,z),I(x,y,z+1) and I(x,y,z-1) through interpolation
			I_x_y_z = (*I)[x][y][z];
			I_x_y_zp1 = interpolate(x, y, z, 0, 0, 1);
			I_x_y_zm1 = interpolate(x, y, z, 0, 0, -1);
		}
		return I_x_y_zp1 - 2.0f * I_x_y_z + I_x_y_zm1;
	}

	T getD2dxzVal(int x, int y, int z) {
		float I_xm1_y_zp1, I_xp1_y_zm1, I_xp1_y_zp1, I_xm1_y_zm1;
		if (isCloseTo(pixelsPerMicron_x, 1) && isCloseTo(pixelsPerMicron_z, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int xp1 = x + 1, xm1 = x - 1;
			if (xp1 >= dimx) {
				xp1 = dimx - 1;
			}
			if (xm1 < 0) {
				xm1 = 0;
			}
			int zp1 = z + 1, zm1 = z - 1;
			if (zp1 >= dimz) {
				zp1 = dimz - 1;
			}
			if (zm1 < 0) {
				zm1 = 0;
			}
			// get values
			I_xm1_y_zp1 = (*I)[xm1][y][zp1];
			I_xp1_y_zm1 = (*I)[xp1][y][zm1];
			I_xp1_y_zp1 = (*I)[xp1][y][zp1];
			I_xm1_y_zm1 = (*I)[xm1][y][zm1];
		} else {
			I_xm1_y_zp1 = interpolate(x, y, z, -1, 0, 1);
			I_xp1_y_zm1 = interpolate(x, y, z, 1, 0, -1);
			I_xp1_y_zp1 = interpolate(x, y, z, 1, 0, 1);
			I_xm1_y_zm1 = interpolate(x, y, z, -1, 0, -1);
		}
		return -0.25f * I_xm1_y_zp1 - 0.25f * I_xp1_y_zm1 + 0.25f * I_xp1_y_zp1
				+ 0.25f * I_xm1_y_zm1;
	}

	T getD2dxyVal(int x, int y, int z) {
		float I_xm1_yp1_z, I_xp1_ym1_z, I_xp1_yp1_z, I_xm1_ym1_z;
		if (isCloseTo(pixelsPerMicron_x, 1) && isCloseTo(pixelsPerMicron_y, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int xp1 = x + 1, xm1 = x - 1;
			if (xp1 >= dimx) {
				xp1 = dimx - 1;
			}
			if (xm1 < 0) {
				xm1 = 0;
			}
			int yp1 = y + 1, ym1 = y - 1;
			if (yp1 >= dimy) {
				yp1 = dimy - 1;
			}
			if (ym1 < 0) {
				ym1 = 0;
			}
			// get values
			I_xm1_yp1_z = (*I)[xm1][yp1][z];
			I_xp1_ym1_z = (*I)[xp1][ym1][z];
			I_xp1_yp1_z = (*I)[xp1][yp1][z];
			I_xm1_ym1_z = (*I)[xm1][ym1][z];
		} else {
			I_xm1_yp1_z = interpolate(x, y, z, -1, 1, 0);
			I_xp1_ym1_z = interpolate(x, y, z, 1, -1, 0);
			I_xp1_yp1_z = interpolate(x, y, z, 1, 1, 0);
			I_xm1_ym1_z = interpolate(x, y, z, -1, -1, 0);
		}
		return -0.25f * I_xm1_yp1_z - 0.25f * I_xp1_ym1_z + 0.25f * I_xp1_yp1_z
				+ 0.25f * I_xm1_ym1_z;
	}

	T getD2dyzVal(int x, int y, int z) {
		float I_x_yp1_zm1, I_x_ym1_zp1, I_x_yp1_zp1, I_x_ym1_zm1;
		if (isCloseTo(pixelsPerMicron_y, 1) && isCloseTo(pixelsPerMicron_z, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int yp1 = y + 1, ym1 = y - 1;
			if (yp1 >= dimy) {
				yp1 = dimy - 1;
			}
			if (ym1 < 0) {
				ym1 = 0;
			}
			int zp1 = z + 1, zm1 = z - 1;
			if (zp1 >= dimz) {
				zp1 = dimz - 1;
			}
			if (zm1 < 0) {
				zm1 = 0;
			}
			// get values
			I_x_yp1_zm1 = (*I)[x][yp1][zm1];
			I_x_ym1_zp1 = (*I)[x][ym1][zp1];
			I_x_yp1_zp1 = (*I)[x][yp1][zp1];
			I_x_ym1_zm1 = (*I)[x][ym1][zm1];
		} else {
			I_x_yp1_zm1 = interpolate(x, y, z, 0, 1, -1);
			I_x_ym1_zp1 = interpolate(x, y, z, 0, -1, 1);
			I_x_yp1_zp1 = interpolate(x, y, z, 0, 1, 1);
			I_x_ym1_zm1 = interpolate(x, y, z, 0, -1, -1);
		}
		return -0.25f * I_x_yp1_zm1 - 0.25f * I_x_ym1_zp1 + 0.25f * I_x_yp1_zp1
				+ 0.25f * I_x_ym1_zm1;
	}

	/*!
	 * This method calculates derivatives simultaneously in order to save computational overhead
	 * This method assumes that x,y resolution is 1.  z resolution does not necessarily equal 1
	 */
	void getFastDerivatives(int x, int y, int z, float &ddx, float &ddy,
			float &ddz, float &d2dx2, float &d2dy2, float &d2dz2, float &d2dxy,
			float &d2dxz, float &d2dyz) {
		if (!isCloseTo(pixelsPerMicron_x, 1.0f) || !isCloseTo(pixelsPerMicron_y, 1.0f)) {
			log(cb,1,"Error: Fast derivatives require that x,y resolution is 1");
			throw 999;
		}

		// handle boundary cases
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int xp1 = x + 1, xm1 = x - 1, yp1 = y + 1, ym1 = y - 1, zp1 = z + 1,
				zm1 = z - 1;
		if (xp1 >= dimx) {
			xp1 = dimx - 1;
		}
		if (xm1 < 0) {
			xm1 = 0;
		}
		if (yp1 >= dimy) {
			yp1 = dimy - 1;
		}
		if (ym1 < 0) {
			ym1 = 0;
		}
		if (zp1 >= dimz) {
			zp1 = dimz - 1;
		}
		if (zm1 < 0) {
			zm1 = 0;
		}

		// get shared values
		float I_x_y_z = (*I)[x][y][z];
		float I_xp1_y_z = (*I)[xp1][y][z];
		float I_xm1_y_z = (*I)[xm1][y][z];
		float I_x_yp1_z = (*I)[x][yp1][z];
		float I_x_ym1_z = (*I)[x][ym1][z];
		float I_xm1_yp1_z = (*I)[xm1][yp1][z];
		float I_xp1_ym1_z = (*I)[xp1][ym1][z];
		float I_xp1_yp1_z = (*I)[xp1][yp1][z];
		float I_xm1_ym1_z = (*I)[xm1][ym1][z];
		float I_x_y_zp1 = (*I)[x][y][z] * (1 - pixelsPerMicron_z)
				+ (*I)[x][y][zp1] * pixelsPerMicron_z;
		float I_x_y_zm1 = (*I)[x][y][z] * (1 - pixelsPerMicron_z)
				+ (*I)[x][y][zm1] * pixelsPerMicron_z;
		float I_xm1_y_zp1 = (*I)[xm1][y][z] * (1 - pixelsPerMicron_z)
				+ (*I)[xm1][y][zp1] * pixelsPerMicron_z;
		float I_xp1_y_zm1 = (*I)[xp1][y][z] * (1 - pixelsPerMicron_z)
				+ (*I)[xp1][y][zm1] * pixelsPerMicron_z;
		float I_xm1_y_zm1 = (*I)[xm1][y][z] * (1 - pixelsPerMicron_z)
				+ (*I)[xm1][y][zm1] * pixelsPerMicron_z;
		float I_xp1_y_zp1 = (*I)[xp1][y][z] * (1 - pixelsPerMicron_z)
				+ (*I)[xp1][y][zp1] * pixelsPerMicron_z;
		float I_x_yp1_zm1 = (*I)[x][yp1][z] * (1 - pixelsPerMicron_z)
				+ (*I)[x][yp1][zm1] * pixelsPerMicron_z;
		float I_x_ym1_zp1 = (*I)[x][ym1][z] * (1 - pixelsPerMicron_z)
				+ (*I)[x][ym1][zp1] * pixelsPerMicron_z;
		float I_x_yp1_zp1 = (*I)[x][yp1][z] * (1 - pixelsPerMicron_z)
				+ (*I)[x][yp1][zp1] * pixelsPerMicron_z;
		float I_x_ym1_zm1 = (*I)[x][ym1][z] * (1 - pixelsPerMicron_z)
				+ (*I)[x][ym1][zm1] * pixelsPerMicron_z;

		// calculate derivatives
		ddx = 0.5f * (I_xp1_y_z - I_xm1_y_z);
		ddy = 0.5f * (I_x_yp1_z - I_x_ym1_z);
		ddz = 0.5f * (I_x_y_zp1 - I_x_y_zm1);
		d2dx2 = I_xp1_y_z - 2.0f * I_x_y_z + I_xm1_y_z;
		d2dy2 = I_x_yp1_z - 2.0f * I_x_y_z + I_x_ym1_z;
		d2dz2 = I_x_y_zp1 - 2.0f * I_x_y_z + I_x_y_zm1;
		d2dxz = -0.25f * I_xm1_y_zp1 - 0.25f * I_xp1_y_zm1 + 0.25f
				* I_xp1_y_zp1 + 0.25f * I_xm1_y_zm1;
		d2dxy = -0.25f * I_xm1_yp1_z - 0.25f * I_xp1_ym1_z + 0.25f
				* I_xp1_yp1_z + 0.25f * I_xm1_ym1_z;
		d2dyz = -0.25f * I_x_yp1_zm1 - 0.25f * I_x_ym1_zp1 + 0.25f
				* I_x_yp1_zp1 + 0.25f * I_x_ym1_zm1;
	}

	T getPrincipalCurvature(int x, int y, int z) {
		// create hessian matrix
		float hess[3][3], V[3][3], d[3];
		hess[0][0] = getD2dx2Val(x, y, z);
		hess[1][1] = getD2dy2Val(x, y, z);
		hess[2][2] = getD2dz2Val(x, y, z);
		hess[1][0] = getD2dxyVal(x, y, z);
		hess[0][1] = hess[1][0];
		hess[2][0] = getD2dxzVal(x, y, z);
		hess[0][2] = hess[2][0];
		hess[1][2] = getD2dyzVal(x, y, z);
		hess[2][1] = hess[1][2];

		// compute eigenvectors and eigenvalues
		eigen_decomposition(hess, V, d);
		// float lengthV=sqrt(V[0][0]*V[0][0] + V[1][0]*V[1][0] + V[2][0]*V[2][0]);
		// float xEig = V[0][0]/lengthV, yEig = V[1][0]/lengthV, float zEig = V[2][0]/lengthV;

		// compute eigenvalues and put into sheet-enhancing metric, assume that d is organized in increasing order
		float d1 = d[0];
		float d2 = d[1];
		float d3 = d[2];
		float a1 = .75, a2 = .75;
		float val = -d1 * expf(-d3 * d3 / (2.0f * powf(a1 * d1, 2.0f))) * expf(
				-d2 * d2 / (2.0f * powf(a2 * d1, 2.0f))); // sheet-like metric


		if (boost::math::isnan(val)) {
			val = 0;
		}

		return val;
	}

	T sussman(int x, int y, int z) {

		// get I(x,y,z), I(x-1,y,z), etc.
		float I_x_y_z, I_xm1_y_z, I_xp1_y_z, I_x_ym1_z, I_x_yp1_z, I_x_y_zm1,
				I_x_y_zp1;
		I_x_y_z = (*I)[x][y][z];
		if (isCloseTo(pixelsPerMicron_x, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int xp1 = x + 1, xm1 = x - 1;
			if (xp1 >= dimx) {
				xp1 = dimx - 1;
			}
			if (xm1 < 0) {
				xm1 = 0;
			}
			// get value
			I_xm1_y_z = (*I)[xm1][y][z];
			I_xp1_y_z = (*I)[xp1][y][z];
		} else {
			I_xm1_y_z = interpolate(x, y, z, -1, 0, 0);
			I_xp1_y_z = interpolate(x, y, z, 1, 0, 0);
		}
		if (isCloseTo(pixelsPerMicron_y, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int yp1 = y + 1, ym1 = y - 1;
			if (yp1 >= dimy) {
				yp1 = dimy - 1;
			}
			if (ym1 < 0) {
				ym1 = 0;
			}
			// get value
			I_x_ym1_z = (*I)[x][ym1][z];
			I_x_yp1_z = (*I)[x][yp1][z];
		} else {
			I_x_ym1_z = interpolate(x, y, z, 0, -1, 0);
			I_x_yp1_z = interpolate(x, y, z, 0, 1, 0);
		}
		if (isCloseTo(pixelsPerMicron_z, 1)) {
			// handle boundary cases
			int dimx, dimy, dimz;
			getDimensions(dimx, dimy, dimz);
			int zp1 = z + 1, zm1 = z - 1;
			if (zp1 >= dimz) {
				zp1 = dimz - 1;
			}
			if (zm1 < 0) {
				zm1 = 0;
			}
			// get value
			I_x_y_zm1 = (*I)[x][y][zm1];
			I_x_y_zp1 = (*I)[x][y][zp1];
		} else {
			I_x_y_zm1 = interpolate(x, y, z, 0, 0, -1);
			I_x_y_zp1 = interpolate(x, y, z, 0, 0, 1);
		}

		// calculate
		float ap = I_x_y_z - I_xm1_y_z;
		float bp = I_xp1_y_z - I_x_y_z;
		float cp = I_x_y_z - I_x_ym1_z;
		float dp = I_x_yp1_z - I_x_y_z;
		float ep = I_x_y_z - I_x_y_zm1;
		float fp = I_x_y_zp1 - I_x_y_z;
		float an, bn, cn, dn, en, fn;
		if (ap < 0) {
			an = ap;
			ap = 0;
		} else {
			an = 0;
		}
		if (bp < 0) {
			bn = bp;
			bp = 0;
		} else {
			bn = 0;
		}
		if (cp < 0) {
			cn = cp;
			cp = 0;
		} else {
			cn = 0;
		}
		if (dp < 0) {
			dn = dp;
			dp = 0;
		} else {
			dn = 0;
		}
		if (ep < 0) {
			en = ep;
			ep = 0;
		} else {
			en = 0;
		}
		if (fp < 0) {
			fn = fp;
			fp = 0;
		} else {
			fn = 0;
		}

		float dD;
		if ((*I)[x][y][z] > 0) {
			dD = sqrtf(
					std::max(ap * ap, bn * bn) + std::max(cp * cp, dn * dn)
							+ std::max(ep * ep, fn * fn)) - 1.0f;
		} else if ((*I)[x][y][z] < 0) {
			dD = sqrtf(
					std::max(an * an, bp * bp) + std::max(cn * cn, dp * dp)
							+ std::max(en * en, fp * fp)) - 1.0f;
		} else {
			dD = 0;
		}

		return dD * (*I)[x][y][z] / sqrtf((*I)[x][y][z] * (*I)[x][y][z] + 1.0f);
	}

	void resize(int dimx, int dimy, int dimz) {
		I->resize(boost::extents[dimx][dimy][dimz]);
	}

	void fill(T val) {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*I)[x][y][z] = val;
				}
			}
		}
	}

	void subview(int x1, int x2, int y1, int y2, int z1, int z2,
			Image3D<T> *output) {
		// Sanity check
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int dimx_new = x2 - x1 + 1;
		int dimy_new = y2 - y1 + 1;
		int dimz_new = z2 - z1 + 1;
		if (dimx_new < 1 || dimx_new > dimx || dimy_new < 1 || dimy_new > dimy
				|| dimz_new < 1 || dimz_new > dimz) {
			log(cb, 1, "Subview dimensions outside of image range");
			throw 999;
		}

		// get subview
		output->resize(dimx_new, dimy_new, dimz_new);
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					(*output)(x - x1, y - y1, z - z1) = (*I)[x][y][z];
				}
			}
		}
	}

	int nnz() {
		int count = 0;
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					if ((*I)[x][y][z] > float(BWTHRESH)) {
						count++;
					}
				}
			}
		}
		return count;
	}

	int numel() {
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		return dimx * dimy * dimz;
	}

	T perctile(float percentile) {
		// get index corresponding to given percentile
		int sz = numel();
		int idxPerc = (int) (percentile * float(sz - 1));

		// store matrix in 1D array temp
		boost::multi_array<T, 1> temp(boost::extents[sz]);
		int dimx, dimy, dimz;
		getDimensions(dimx, dimy, dimz);
		int count = 0;
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					temp[count] = (*I)[x][y][z];
					count++;
				}
			}
		}

		// sort and return appropriate percentile
		std::sort(temp.begin(), temp.end());
		return temp[idxPerc];
	}

};

#endif
