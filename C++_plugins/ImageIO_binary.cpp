/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "ImageIO_binary.h"
using namespace boost;

ImageIO_binary::ImageIO_binary(CallbackFunctions *callback) {
	cb = callback;
	fileDescriptor = NULL;
}

void ImageIO_binary::setFileName(char *name) {
	fileName = name;
}

const char * ImageIO_binary::getFileName() {
	return fileName;
}

int ImageIO_binary::openForSequentialRead() {
	if (strcmp(fileName, "0") == 0) {
		return 0;
	} else {
		TIFF *tif = TIFFOpen(fileName, "r");
		if (tif == NULL) {
			log(cb, 1,
					"Error: Trying to read a tiff that does not exist, aborting");
			throw 999;
		}
		fileDescriptor = tif;
		return 1;
	}
}

int ImageIO_binary::openForSequentialWrite() {
	if (fileName == NULL) {
		log(cb, 1, "Error: filename is NULL");
		throw 999;
	}
	if (strcmp(fileName, "0") == 0) {
		log(cb, 1, "Error: Attempting to write image to '0' filename");
		throw 999;
	}
	TIFF *tif = TIFFOpen(fileName, "w4"); // use option w8 for bigTiff
	fileDescriptor = tif;
	return 0;
}

void ImageIO_binary::close() {
	if (fileDescriptor == NULL) {
		log(
				cb,
				1,
				"Error: ImageIO_binary::close() : Trying to close tiff before it has been opened");
		throw 999;
	}
	TIFFClose(fileDescriptor);
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
void ImageIO_binary::read(multi_array<float, 3> *destinationArray,
		int tiffSlice, int boostSlice, int cachePolicy) {
#pragma clang diagnostic pop
	if (fileDescriptor == NULL) {
		log(cb, 1, "Error: Trying to read tiff before it has been opened");
		throw 999;
	}
	TIFFSetDirectory(fileDescriptor, (uint16) tiffSlice);
	tdata_t buf = _TIFFmalloc(TIFFScanlineSize(fileDescriptor));
	int dimy, dimx;
	TIFFGetField(fileDescriptor, TIFFTAG_IMAGEWIDTH, &dimx);
	TIFFGetField(fileDescriptor, TIFFTAG_IMAGELENGTH, &dimy);

	for (int y = 0; y < dimy; y++) {
		TIFFReadScanline(fileDescriptor, buf, (unsigned int) y);
		for (int x = 0; x < dimx; x++) {
			(*destinationArray)[x][y][boostSlice] = ((float*) buf)[x];
		}
	}
	_TIFFfree(buf);
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
void ImageIO_binary::read(multi_array<unsigned char, 3> *destinationArray,
		int tiffSlice, int boostSlice, int cachePolicy) {
#pragma clang diagnostic pop
	if (fileDescriptor == NULL) {
		log(cb, 1, "Error: Trying to read tiff before it has been opened");
		throw 999;
	}
	TIFFSetDirectory(fileDescriptor, (uint16) tiffSlice);
	tdata_t buf = _TIFFmalloc(TIFFScanlineSize(fileDescriptor));
	int dimx, dimy;
	TIFFGetField(fileDescriptor, TIFFTAG_IMAGEWIDTH, &dimx);
	TIFFGetField(fileDescriptor, TIFFTAG_IMAGELENGTH, &dimy);

	for (int y = 0; y < dimy; y++) {
		TIFFReadScanline(fileDescriptor, buf, (uint32) y);
		for (int x = 0; x < dimx; x++) {
			(*destinationArray)[x][y][boostSlice] = ((unsigned char*) buf)[x];
		}
	}
	_TIFFfree(buf);
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
void ImageIO_binary::write(multi_array<float, 3> *boostMatrix, int boostSlice,
		int cachePolicy) {
#pragma clang diagnostic pop
	// sanity check
	if (fileDescriptor == NULL) {
		log(cb, 1, "Error: Trying to write tiff before it has been opened");
		throw 999;
	}

	// get image dimensions
	const boost::multi_array_types::size_type* dim = boostMatrix->shape();
	int dimx = (int) dim[0], dimy = (int) dim[1];

	// set required tiff metadata
	TIFFSetField(fileDescriptor, TIFFTAG_IMAGEWIDTH, dimx);
	TIFFSetField(fileDescriptor, TIFFTAG_IMAGELENGTH, dimy);
	TIFFSetField(fileDescriptor, TIFFTAG_SAMPLESPERPIXEL, 1); // number of channels per pixel
	TIFFSetField(fileDescriptor, TIFFTAG_BITSPERSAMPLE, 32); // 32 bit image
	TIFFSetField(fileDescriptor, TIFFTAG_ORIENTATION, ORIENTATION_TOPLEFT); // set the origin of the image.
	TIFFSetField(fileDescriptor, TIFFTAG_PLANARCONFIG, PLANARCONFIG_CONTIG);
	TIFFSetField(fileDescriptor, TIFFTAG_PHOTOMETRIC, PHOTOMETRIC_RGB);
	TIFFSetField(fileDescriptor, TIFFTAG_SAMPLEFORMAT, SAMPLEFORMAT_IEEEFP);

	// write pixels to tiff
	float *buf = (float *) _TIFFmalloc(TIFFScanlineSize(fileDescriptor));
	for (int y = 0; y < dimy; y++) {
		for (int x = 0; x < dimx; x++) {
			buf[x] = (*boostMatrix)[x][y][boostSlice];
		}
		if (TIFFWriteScanline(fileDescriptor, buf, (uint32) y, 0) == -1) {
			log(cb, 1, "TIFFWriteScanline error");
			throw 999;
		}
	}
	_TIFFfree(buf);
	if (!(TIFFWriteDirectory(fileDescriptor))) {
		log(cb, 1, "TIFFWriteDirectory failed, fatal error");
		throw 999;
	}
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
void ImageIO_binary::write(multi_array<unsigned char, 3> *boostMatrix,
		int boostSlice, int cachePolicy) {
#pragma clang diagnostic pop
	// sanity check
	if (fileDescriptor == NULL) {
		log(cb, 1, "Error: Trying to write tiff before it has been opened");
		throw 999;
	}

	// get image dimensions
	const boost::multi_array_types::size_type* dim = boostMatrix->shape();
	int dimx = (int) dim[0], dimy = (int) dim[1];

	// set required tiff metadata
	TIFFSetField(fileDescriptor, TIFFTAG_IMAGEWIDTH, dimx);
	TIFFSetField(fileDescriptor, TIFFTAG_IMAGELENGTH, dimy);
	TIFFSetField(fileDescriptor, TIFFTAG_SAMPLESPERPIXEL, 1); // number of channels per pixel
	TIFFSetField(fileDescriptor, TIFFTAG_BITSPERSAMPLE, 8); // 8 bit image
	TIFFSetField(fileDescriptor, TIFFTAG_ORIENTATION, ORIENTATION_TOPLEFT); // set the origin of the image.
	TIFFSetField(fileDescriptor, TIFFTAG_PHOTOMETRIC, PHOTOMETRIC_MINISBLACK);
	TIFFSetField(fileDescriptor, TIFFTAG_SAMPLEFORMAT, SAMPLEFORMAT_UINT);

	// write pixels to tiff
	unsigned char *buf = (unsigned char *) _TIFFmalloc(
			TIFFScanlineSize(fileDescriptor));
	for (int y = 0; y < dimy; y++) {
		for (int x = 0; x < dimx; x++) {
			buf[x] = (*boostMatrix)[x][y][boostSlice];
		}
		if (TIFFWriteScanline(fileDescriptor, buf, (uint32) y, 0) == -1) {
			log(cb, 1, "TIFFWriteScanline error");
			throw 999;
		}
	}
	_TIFFfree(buf);
	if (!(TIFFWriteDirectory(fileDescriptor))) {
		log(cb, 1, "TIFFWriteDirectory failed");
		throw 999;
	}
}

void ImageIO_binary::getDimensions(int &dimx, int &dimy, int &dimz) {
	if (fileDescriptor == NULL) {
		log(
				cb,
				1,
				"Error: ImageIO_binary::getDimensions() : Trying to read tiff before it has been opened");
		throw 999;
	}

	TIFF *tif = fileDescriptor;
	TIFFGetField(fileDescriptor, TIFFTAG_IMAGELENGTH, &dimy);
	TIFFGetField(fileDescriptor, TIFFTAG_IMAGEWIDTH, &dimx);
	TIFFSetDirectory(fileDescriptor, 0);
	dimz = 0;
	do {
		dimz++;
	} while (TIFFReadDirectory(tif));
}

/*!
 * This method is supposed to write a specific boost slice to a specific slice in an output tiff image.
 * However, libtiff does not provide convenient functionality to do so, so this method is not implemented.
 */
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
void ImageIO_binary::writeToSlice(
		boost::multi_array<unsigned char, 3> *boostMatrix, int boostSlice,
		int cachePolicy, int outSlice) {
#pragma clang diagnostic pop
	log(
			cb,
			1,
			"Error: ImageIO_binary::writeToSlice : libtiff does not provide convenient functionality to write to specific slice of tiff.  You probably meant to use ImageJ pipeline");
	throw 999;
}
