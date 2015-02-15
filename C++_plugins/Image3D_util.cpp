/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "Image3D_util.h"
using namespace boost;

float max(Image3D<float> *I) {
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	float maxVal = MIN_FLOAT;
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				maxVal = std::max(maxVal, (*I)(x, y, z));
			}
		}
	}
	return maxVal;
}

float min(Image3D<float> *I) {
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	float minVal = MAX_FLOAT;
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				minVal = std::min(minVal, (*I)(x, y, z));
			}
		}
	}
	return minVal;
}

float mean(Image3D<float> *I) {
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	float sum = 0;
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				sum = sum + (*I)(x, y, z);
			}
		}
	}
	float mean = sum / (float) (dimx * dimy * dimz);
	return mean;
}

void perim(Image3D<float> *I, int thickness) {
	// create temporary image to store intermediate results
	Image3D<float> erodedI(*I);

	// get eroded image
	erode(&erodedI, thickness);

	// get perimeter
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				if ((*I)(x, y, z) > BWTHRESH && erodedI(x, y, z) > BWTHRESH) {
					(*I)(x, y, z) = 0;
				}
			}
		}
	}
}

void erode(Image3D<float> *I, int kernelSz) {

	// create temporary image to store result
	Image3D<float> temp(*I);

	// calculate eroded image, store in temp
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				// get minimum voxel in window
				float minVal = (*I)(x, y, z);
				for (int dx = -kernelSz; dx <= kernelSz; dx++) {
					for (int dy = -kernelSz; dy <= kernelSz; dy++) {
						for (int dz = -kernelSz; dz <= kernelSz; dz++) {
							int xd = x + dx, yd = y + dy, zd = z + dz;
							if (xd >= 0 && xd < dimx && yd >= 0 && yd < dimy
									&& zd >= 0 && zd < dimz) {
								minVal = std::min(minVal, (*I)(xd, yd, zd));
							}
						}
					}
				}
				// set to minimum voxel
				temp(x, y, z) = minVal;
			}
		}
	}
	// copy temp
	(*I) = temp;
}

void erode_multithreaded(Image3D<float> *I, int kernelSz) {
	// create temporary image to store result
	Image3D<float> *temp = new Image3D<float> (*I);

	// calculate eroded image, store in temp
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
#if defined USELIBDISPATCH
	dispatch_apply((size_t)dimz, dispatch_get_global_queue(0,0), ^(size_t zz) {
#else
	for (int zz = 0; zz < dimz; zz++) {
#endif
		int z = (int) zz;
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				// get minimum voxel in window
				float minVal = (*I)(x, y, z);
				for (int dx = -kernelSz; dx <= kernelSz; dx++) {
					for (int dy = -kernelSz; dy <= kernelSz; dy++) {
						for (int dz = -kernelSz; dz <= kernelSz; dz++) {
							int xd = x + dx;
							int yd = y + dy;
							int zd = z + dz;
							if (xd >= 0 && xd < dimx && yd >= 0 && yd < dimy
									&& zd >= 0 && zd < dimz) {
								minVal = std::min(minVal, (*I)(xd, yd, zd));
							}
						}
					}
				}
				// set to minimum voxel
				(*temp)(x, y, z) = minVal;
			}
		}
#if defined USELIBDISPATCH
	});
#else
	}
#endif
	// copy temp
	(*I) = (*temp);
}

void dilate_multithreaded(Image3D<float> *I, int kernelSz) {

	// create temporary image to store result
	Image3D<float> *temp = new Image3D<float> (*I);

	// calculate eroded image, store in temp
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
#if defined USELIBDISPATCH
	dispatch_apply((size_t)dimz, dispatch_get_global_queue(0,0), ^(size_t zz) {
#else
	for (int zz = 0; zz < dimz; zz++) {
#endif
		int z = (int) zz;
		for (int x = 0; x < dimx; x++) {
			for (int y = 0; y < dimy; y++) {
				// get minimum voxel in window
				float maxVal = (*I)(x, y, z);
				for (int dx = -kernelSz; dx <= kernelSz; dx++) {
					for (int dy = -kernelSz; dy <= kernelSz; dy++) {
						for (int dz = -kernelSz; dz <= kernelSz; dz++) {
							int xd = x + dx;
							int yd = y + dy;
							int zd = z + dz;
							if (xd >= 0 && xd < dimx && yd >= 0 && yd < dimy
									&& zd >= 0 && zd < dimz) {
								maxVal = std::max(maxVal, (*I)(xd, yd, zd));
							}
						}
					}
				}
				// set to minimum voxel
				(*temp)(x, y, z) = maxVal;
			}
		}
#if defined USELIBDISPATCH
	});
#else
	}
#endif
	// copy temp
	(*I) = (*temp);
}

/*!
 *
 * Direction is "x", "y", or "z"
 *
 */
void convolve1D(Image3D<float> *I, multi_array<float, 1> *kernel,
		const char *direction, CallbackFunctions *cb) {
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	int kernelSz = (int) kernel->size();
	multi_array<float, 3> *output = new multi_array<float, 3> (
			boost::extents[dimx][dimy][dimz]);
	try {
		if (strcmp(direction, "x") == 0) {
			__block volatile bool keepGoing = 1;
#if defined USELIBDISPATCH
			dispatch_apply((size_t)dimz, dispatch_get_global_queue(0,0), ^(size_t zz) {
#else
			for (int zz = 0; zz < dimz; zz++) {
#endif
				int z = (int) zz;
				if (keepGoing == 1) {
					if (cb->shouldInterrupt()) {
						log(cb, 4, "Interrupt signal received");
						keepGoing = 0;
					} else {
						for (int x = 0; x < dimx; x++) {
							for (int y = 0; y < dimy; y++) {
								for (int i = 0; i < kernelSz; i++) {
									int xIter = x + i - kernelSz / 2;
									if (xIter < 0) {
										xIter = 0;
									}
									if (xIter >= dimx) {
										xIter = dimx - 1;
									}
									(*output)[x][y][z] = (*output)[x][y][z]
											+ (*kernel)[i] * (*I)(xIter, y, z);
								}
							}
						}
					}
				}
#if defined USELIBDISPATCH
			});
#else
			}
#endif
		} else if (strcmp(direction, "y") == 0) {
			__block volatile bool keepGoing = 1;
#if defined USELIBDISPATCH
			dispatch_apply((size_t)dimz, dispatch_get_global_queue(0,0), ^(size_t zz) {
#else
			for (int zz = 0; zz < dimz; zz++) {
#endif
				int z = (int) zz;
				if (keepGoing == 1) {
					if (cb->shouldInterrupt()) {
						log(cb, 4, "Interrupt signal received");
						keepGoing = 0;
					} else {
						for (int x = 0; x < dimx; x++) {
							for (int y = 0; y < dimy; y++) {
								for (int i = 0; i < kernelSz; i++) {
									int yIter = y + i - kernelSz / 2;
									if (yIter < 0) {
										yIter = 0;
									}
									if (yIter >= dimy) {
										yIter = dimy - 1;
									}
									(*output)[x][y][z] = (*output)[x][y][z]
											+ (*kernel)[i] * (*I)(x, yIter, z);
								}
							}
						}
					}
				}
#if defined USELIBDISPATCH
			});
#else
			}
#endif
		} else if (strcmp(direction, "z") == 0) {
			__block volatile bool keepGoing = 1;
#if defined USELIBDISPATCH
			dispatch_apply((size_t)dimz, dispatch_get_global_queue(0,0), ^(size_t zz) {
#else
			for (int zz = 0; zz < dimz; zz++) {
#endif
				int z = (int) zz;
				if (keepGoing == 1) {
					if (cb->shouldInterrupt()) {
						log(cb, 4, "Interrupt signal received");
						keepGoing = 0;
					} else {
						for (int x = 0; x < dimx; x++) {
							for (int y = 0; y < dimy; y++) {
								for (int i = 0; i < kernelSz; i++) {
									int zIter = z + i - kernelSz / 2;
									if (zIter < 0) {
										zIter = 0;
									}
									if (zIter >= dimz) {
										zIter = dimz - 1;
									}
									(*output)[x][y][z] = (*output)[x][y][z]
											+ (*kernel)[i] * (*I)(x, y, zIter);
								}
							}
						}
					}
				}
#if defined USELIBDISPATCH
			});
#else
			}
#endif
		} else {
			log(cb, 1, "Error: Invalid direction");
			throw 999;
		}
	} catch (...) {
		log(cb, 4, "Catching interrupt");
		delete output;
		output = NULL;
	}
	I->setBoost(output);
	delete output;
	output = NULL;
	return;
}

/*
 * Calculates 3D distance transform.
 */
void bwdist(Image3D<float> *I) {
	int dimx, dimy, dimz;
	I->getDimensions(dimx, dimy, dimz);
	int maxDim = std::max(std::max(dimx, dimy), dimz);
	float *f = new float[maxDim];
	float *d = new float[maxDim];

	// threshold image
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				if ((*I)(x, y, z) < BWTHRESH) {
					(*I)(x, y, z) = INF;
				} else {
					(*I)(x, y, z) = 0;
				}
			}
		}
	}

	// transform along x-axis
	for (int y = 0; y < dimy; y++) {
		for (int z = 0; z < dimz; z++) {
			for (int x = 0; x < dimx; x++) {
				f[x] = (*I)(x, y, z);
			}
			dt(f, dimx, d);
			for (int x = 0; x < dimx; x++) {
				(*I)(x, y, z) = d[x];
			}
		}
	}

	// transform along y-axis
	for (int x = 0; x < dimx; x++) {
		for (int z = 0; z < dimz; z++) {
			for (int y = 0; y < dimy; y++) {
				f[y] = (*I)(x, y, z);
			}
			dt(f, dimy, d);
			for (int y = 0; y < dimy; y++) {
				(*I)(x, y, z) = d[y];
			}
		}
	}

	// transform along z-axis
	for (int x = 0; x < dimx; x++) {
		for (int y = 0; y < dimy; y++) {
			for (int z = 0; z < dimz; z++) {
				f[z] = (*I)(x, y, z);
			}
			dt(f, dimz, d);
			for (int z = 0; z < dimz; z++) {
				(*I)(x, y, z) = d[z];
			}
		}
	}
	delete[] f;
	f = NULL;
	delete[] d;
	d = NULL;
}

