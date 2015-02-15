
/*
 Copyright (C) 2006 Pedro Felzenszwalb
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA
 */

/* distance transform */

#include "dt.h"

template<class T>
inline T square(const T &x) {
	return x * x;
}

/* dt of 1d function using squared distance */
void dt(float *f, int n, float *d) {
	int *v = new int[n];
	float *z = new float[n + 1];
	int k = 0;
	v[0] = 0;
	z[0] = -INF;
	z[1] = +INF;
	for (int q = 1; q <= n - 1; q++) {
		float s = ((f[q] + square(q)) - (f[v[k]] + square(v[k]))) / (2 * q - 2
				* v[k]);
		while (s <= z[k]) {
			k--;
			s = ((f[q] + square(q)) - (f[v[k]] + square(v[k]))) / (2 * q - 2
					* v[k]);
		}
		k++;
		v[k] = q;
		z[k] = s;
		z[k + 1] = +INF;
	}

	k = 0;
	for (int q = 0; q <= n - 1; q++) {
		while (z[k + 1] < q)
			k++;
		d[q] = square(q - v[k]) + f[v[k]];
	}

	delete[] v;
	delete[] z;
}
