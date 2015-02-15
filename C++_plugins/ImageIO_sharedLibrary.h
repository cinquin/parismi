/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef IMAGEIOSHAREDLIBRARY_H_
#define IMAGEIOSHAREDLIBRARY_H_

#include "CallbackFunctions.h"
#include "ImageIO.h"
#include "util.h"

class ImageIO_sharedLibrary : public ImageIO {
private:
	CallbackFunctions *cb;
	float *transferBuffer;
	const char *fileName;
	int zSlice;

public:
	/*!
	 * Class constructor
	 * \param callback A struct of useful callback functions
	 * \param buffer This float array is used to pass pixel values between plugin and pipeline
	 */
	ImageIO_sharedLibrary(CallbackFunctions *callback, float *buffer);

	/*!
	 * Sets the path and file name of the input file.
	 * \param name Path and file name of the input file
	 */
	void setFileName(char *name);

	/*!
	 * This method returns the path and file name of the input file.
	 * \return Path and file name of the input file
	 */
	const char* getFileName();

	/*!
	 * This method opens the file for reading
	 */
	int openForSequentialRead();

	/*!
	 * This method opens the file for writing
	 */
	int openForSequentialWrite();

	/*!
	 * This method closes a file after reading is finished
	 */
	void close();

	/*!
	 * This method provides input functionality for 32-bit images.  It copies a specified slice of an input file into a specified slice of a boost matrix
	 * \param destinationArray Image will be copied into a 3D boost array
	 * \param tiffSlice The slice of the image being read
	 * \param boostSlice The slice of the boost matrix that tiff slice is read into
	 * \param cachePolicy Unused parameter
	 */
	void read(boost::multi_array<float, 3> *destinationArray, int tiffSlice, int boostSlice, int cachePolicy);
	void read(boost::multi_array<unsigned char, 3> *destinationArray, int tiffSlice, int boostSlice, int cachePolicy);


	void write(boost::multi_array<float, 3> *boostMatrix, int boostSlice, int cachePolicy);
	void write(boost::multi_array<unsigned char, 3> *boostMatrix, int boostSlice, int cachePolicy);


	/*!
	 * This method reads the x, y, z dimensions of the input file.  Normally, you could just read the entire image into a boost matrix and check the boost dimensions, but sometimes you want to save on RAM
	 */
	void getDimensions(int &dimy, int &dimx, int &dimz);

	void writeToSlice(boost::multi_array<unsigned char,3> *boostMatrix, int boostSlice, int cachePolicy, int outSlice);

};

#endif /* IMAGEIOSHAREDLIBRARY_H_ */
