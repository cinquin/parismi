// Code in this file was written based on MATLAB code written by Jimmy Shen.  Attribution below

// 3D Bresenham's line generation
// Author: Jimmy Shen
// http://www.mathworks.com/matlabcentral/fileexchange/21057-3d-bresenham-s-line-generation
// Copyright 2008 Jimmy Shen.
// Use, modification and distribution is covered by the BSD license found at: http://www.mathworks.com/matlabcentral/fileexchange/21057-3d-bresenham-s-line-generation


#include "Array1D.h"

void bresenhamLine(Array1D<int> *xOut, Array1D<int> *yOut, Array1D<int> *zOut,
		int x2, int y2, int z2);
