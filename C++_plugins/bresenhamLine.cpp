// Code in this file was written based on MATLAB code written by Jimmy Shen.  Attribution below

// 3D Bresenham's line generation
// Author: Jimmy Shen
// http://www.mathworks.com/matlabcentral/fileexchange/21057-3d-bresenham-s-line-generation
// Copyright 2008 Jimmy Shen.
// Use, modification and distribution is covered by the BSD license found at: http://www.mathworks.com/matlabcentral/fileexchange/21057-3d-bresenham-s-line-generation

#include "bresenhamLine.h"

using namespace std;

/*!
 * Returns xOut,yOut,zOut which are x,y,z coordinates of the discrete
 * ray emanating from (0,0,0) to (x2,y2,z2).
 */
void bresenhamLine(Array1D<int> *xOut, Array1D<int> *yOut, Array1D<int> *zOut,
		int x2, int y2, int z2) {
	// allocate space to store x,y,z coordinates of discrete ray
	int maxLength = max(max(abs(y2) + 1, abs(x2) + 1), abs(z2) + 1);
	xOut->resize(maxLength);
	yOut->resize(maxLength);
	zOut->resize(maxLength);

	// calculate discrete line
	int x1 = 0, y1 = 0, z1 = 0;
	int dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
	int ax = abs(dx) * 2, ay = abs(dy) * 2, az = abs(dz) * 2;
	int sx = sgn(dx), sy = sgn(dy), sz = sgn(dz);
	int x = x1, y = y1, z = z1, idx = 0;
	int xd, yd, zd;
	if (ax >= max(ay, az)) {
		yd = ay - ax / 2;
		zd = az - ax / 2;
		while (1) {
			(*xOut)(idx) = x;
			(*yOut)(idx) = y;
			(*zOut)(idx) = z;
			idx++;
			if (x == x2) {
				break;
			}
			if (yd >= 0) {
				y = y + sy;
				yd = yd - ax;
			}
			if (zd >= 0) {
				z = z + sz;
				zd = zd - ax;
			}
			x = x + sx;
			yd = yd + ay;
			zd = zd + az;
		}
	} else if (ay >= max(ax, az)) {
		xd = ax - ay / 2;
		zd = az - ay / 2;
		while (1) {
			(*xOut)(idx) = x;
			(*yOut)(idx) = y;
			(*zOut)(idx) = z;
			idx++;
			if (y == y2) {
				break;
			}
			if (xd >= 0) {
				x = x + sx;
				xd = xd - ay;
			}
			if (zd >= 0) {
				z = z + sz;
				zd = zd - ay;
			}
			y = y + sy;
			xd = xd + ax;
			zd = zd + az;
		}
	} else if (az >= max(ax, ay)) {
		xd = ax - az / 2;
		yd = ay - az / 2;
		while (1) {
			(*xOut)(idx) = x;
			(*yOut)(idx) = y;
			(*zOut)(idx) = z;
			idx++;
			if (z == z2) {
				break;
			}
			if (xd >= 0) {
				x = x + sx;
				xd = xd - az;
			}
			if (yd >= 0) {
				y = y + sy;
				yd = yd - az;
			}
			z = z + sz;
			xd = xd + ax;
			yd = yd + ay;
		}
	}
	// resize
	xOut->resize(idx);
	yOut->resize(idx);
	zOut->resize(idx);
}
