/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "ImageIO_sharedLibrary.h"
using namespace boost;

ImageIO_sharedLibrary::ImageIO_sharedLibrary(CallbackFunctions * callback,
		float *buffer) {
	cb = callback;
	transferBuffer = buffer;
}

void ImageIO_sharedLibrary::setFileName(char *name) {
	fileName = name;
}

const char * ImageIO_sharedLibrary::getFileName() {
	return fileName;
}

int ImageIO_sharedLibrary::openForSequentialRead() {
	if (strcmp(fileName, "0") == 0) {
		log(cb, 1, "Error: Reading 0 image");
		throw 999;
	}
	return 0;
}

int ImageIO_sharedLibrary::openForSequentialWrite() {
	zSlice = 0;
	return 0;
}

void ImageIO_sharedLibrary::close() {
	// Don't need to do anything
}

void ImageIO_sharedLibrary::read(multi_array<float, 3> *destinationArray,
		int tiffSlice, int boostSlice, int cachePolicy) {
	cachePolicy = 0;
	int dimx, dimy, dimz;
	getDimensions(dimx, dimy, dimz);
	cb->getPixels(tiffSlice, fileName, NULL, 0);

	int idx = 0;
	for (int y = 0; y < dimy; y++) {
		for (int x = 0; x < dimx; x++) {
			(*destinationArray)[x][y][boostSlice] = transferBuffer[idx];
			idx++;
		}
	}
}

void ImageIO_sharedLibrary::read(
		multi_array<unsigned char, 3> *destinationArray, int tiffSlice,
		int boostSlice, int cachePolicy) {
	cachePolicy = 0;
	int dimx, dimy, dimz;
	getDimensions(dimx, dimy, dimz);
	cb->getPixels(tiffSlice, fileName, NULL, 0);

	int idx = 0;
	for (int y = 0; y < dimy; y++) {
		for (int x = 0; x < dimx; x++) {
			(*destinationArray)[x][y][boostSlice]
					= ((unsigned char *) transferBuffer)[idx];
			idx++;
		}
	}
}

void ImageIO_sharedLibrary::write(multi_array<float, 3> *boostMatrix,
		int boostSlice, int cachePolicy) {
	cachePolicy = 0;
	// get image dimensions
	const boost::multi_array_types::size_type* dim = boostMatrix->shape();
	int dimx = (int) dim[0], dimy = (int) dim[1];

	int idx = 0;
	for (int y = 0; y < dimy; y++) {
		for (int x = 0; x < dimx; x++) {
			transferBuffer[idx] = (*boostMatrix)[x][y][boostSlice];
			idx++;
		}
	}
	cb->setPixels(zSlice, fileName, NULL, 0, 0);
	zSlice++;
}

void ImageIO_sharedLibrary::write(multi_array<unsigned char, 3> *boostMatrix,
		int boostSlice, int cachePolicy) {
	cachePolicy = 0;
	// get image dimensions
	const boost::multi_array_types::size_type* dim = boostMatrix->shape();
	int dimx = (int) dim[0], dimy = (int) dim[1];

	int idx = 0;
	for (int y = 0; y < dimy; y++) {
		for (int x = 0; x < dimx; x++) {
			((unsigned char *) transferBuffer)[idx]
					= (*boostMatrix)[x][y][boostSlice];
			idx++;
		}
	}
	cb->setPixels(zSlice, fileName, NULL, 0, 0);
	zSlice++;
}

void ImageIO_sharedLibrary::getDimensions(int &dimx, int &dimy, int &dimz) {
	int t, c;
	cb->getDimensionsByRef(fileName, dimx, dimy, dimz, t, c);
}

/*!
 * This method writes a specific boost slice to a specific slice in an output image
 */
void ImageIO_sharedLibrary::writeToSlice(
		boost::multi_array<unsigned char, 3> *boostMatrix, int boostSlice,
		int cachePolicy, int outSlice) {
	cachePolicy = 0;
	// get image dimensions
	const boost::multi_array_types::size_type* dim = boostMatrix->shape();
	int dimx = (int) dim[0], dimy = (int) dim[1];

	int idx = 0;
	for (int y = 0; y < dimy; y++) {
		for (int x = 0; x < dimx; x++) {
			((unsigned char *) transferBuffer)[idx]
					= (*boostMatrix)[x][y][boostSlice];
			idx++;
		}
	}
	cb->setPixels(outSlice, fileName, NULL, 0, 0);
}
