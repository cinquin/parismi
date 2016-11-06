/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef ARRAY1D_H_
#define ARRAY1D_H_

#include "CallbackFunctions.h"
#include "Image3D.h"
#include <boost/multi_array.hpp>
#include "util.h"

template<class T>
class Array1D {
private:
	boost::multi_array<T, 1>* __restrict__ vec;
	CallbackFunctions *cb;

public:
	/*!
	 * Constructor
	 */
	Array1D(CallbackFunctions *callback) {
		vec = new boost::multi_array<T, 1>(boost::extents[0]);
		cb = callback;
	}

	/*
	 * Copy contructor
	 */
	Array1D(const Array1D& other) {
		// deep copy of boost matrix
		int sz = other.size();
		vec = new boost::multi_array<T, 1>(boost::extents[sz]);
		(*vec) = *(other.vec);
		cb = other.cb;
	}

	/*!
	 * destructor
	 */
	~Array1D() {
		delete vec;
		vec = NULL;
	}

	/*!
	 * Access/mutate element idx
	 */
	T& operator()(int idx) {
		return (*vec)[idx];
	}

	/*!
	 * Add constant number to all elements
	 */
	void operator+(T val) {
		int sz = size();
		for (int i = 0; i < sz; i++) {
			(*vec)[i] += val;
		}
	}

	/*!
	 * Divide through element by element
	 */
	void operator/(Array1D v2) {
		int sz1 = size(), sz2 = v2.size();
		if (sz1 != sz2) {
			log(cb, 1, "Arrays are not the same size (%d,%d)", sz1, sz2);
			throw 999;
		}
		for (int i = 0; i < sz1; i++) {
			(*vec)[i] = (*vec)[i] / v2(i);
		}
	}

	void display() {
		int sz = size();
		log(cb, 0, "Array size: %d", sz);

		std::stringstream ss;
		for (int i = 0; i < sz; i++) {
			ss << (*vec)[i] << ",";
		}
		log(cb, 0, ss.str().c_str());
	}

	void im2array(Image3D<T> *I) {
		int dimx, dimy, dimz;
		I->getDimensions(dimx, dimy, dimz);
		vec->resize(boost::extents[dimx * dimy * dimz]);
		int idx = 0;
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				for (int z = 0; z < dimz; z++) {
					(*vec)[idx] = (*I)(x, y, z);
					idx++;
				}
			}
		}
	}

	void resize(int sz) {
		// Sanity check
		if (sz < 0) {
			log(cb, 1, "Trying to resize Array1D <0 (%d)", sz);
			throw 999;
		}

		vec->resize(boost::extents[sz]);
	}

	/*
	 * Sorts vector in ascending order
	 */
	void sort() {
		std::sort(vec->begin(), vec->end());
	}

	/*!
	 * Finds minimum of vector
	 */
	T min() {
		T minimumValue = INF;
		for (int i = 0; i < size(); i++) {
			minimumValue = std::min(minimumValue, (*vec)[i]);
		}
		return minimumValue;
	}

	/*!
	 * Finds index of minimum value in vector
	 */
	int minIdx() {
		T minimumValue = INF;
		int minIdx = -1;
		for (int i = 0; i < size(); i++) {
			if ((*vec)[i] < minimumValue) {
				minimumValue = (*vec)[i];
				minIdx = i;
			}
		}
		return minIdx;
	}

	/*!
	 * Finds index of maximum value in vector
	 */
	int maxIdx() {
		T maximumValue = -INF;
		int maxIdx = -1;
		for (int i = 0; i < size(); i++) {
			if ((*vec)[i] > maximumValue) {
				maximumValue = (*vec)[i];
				maxIdx = i;
			}
		}
		return maxIdx;
	}

	/*!
	 * Finds maximum of vector
	 */
	T max() {
		T maximumValue = -INF;
		for (int i = 0; i < size(); i++) {
			maximumValue = std::max(maximumValue, (*vec)[i]);
		}
		return maximumValue;
	}

	T perctile(float percentile) {
		// get index corresponding to given percentile
		int idxPerc = (int) (percentile * (float) (vec->size() - 1));

		// get copy of vector
		int sz = size();
		boost::multi_array<T, 1> temp(boost::extents[sz]);
		temp = *vec;

		// sort copy and return appropriate percentile
		std::sort(temp.begin(), temp.end());
		return temp[idxPerc];
	}

	/*!
	 * Returns number of elements in boost matrix
	 */
	int size() const {
		return (int) (vec->size());
	}

	void fill(T val) {
		int sz = size();
		for (int i = 0; i < sz; i++) {
			(*vec)[i] = val;
		}
	}

	void ismember(Array1D<int> *idx, Array1D<int> *idx_other, Array1D<T> *other) {

		// initialize idx_other to store index of other that matches given element of self
		int sz = size();
		idx_other->resize(sz);
		idx_other->fill(-1);

		// check whether each element is found in other
		int sz_other = other->size();
		for (int i = 0; i < sz; i++) {
			for (int j = 0; j < sz_other; j++) {
				if (isCloseTo((*vec)[i], (*other)(j))) {
					(*idx_other)(i) = j;
					break;
				}
			}
		}

		// return indices of elements found in other
		idx->resize(sz);
		int count = 0;
		for (int i = 0; i < sz; i++) {
			if ((*idx_other)(i) != -1) {
				(*idx)(count) = i;
				(*idx_other)(count) = (*idx_other)(i);
				count++;
			}
		}
		idx->resize(count);
		idx_other->resize(count);
	}

	T sum() {
		T sum = 0;
		for (int i = 0; i < size(); i++) {
			sum += (*vec)[i];
		}
		return sum;
	}

	void push_back(T val) {
		int sz = size();
		vec->resize(boost::extents[sz + 1]);
		(*vec)[sz] = val;
	}
};

#endif
