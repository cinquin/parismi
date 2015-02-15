/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef IMAGEINPUTBINARY_H_
#define IMAGEINPUTBINARY_H_

#include "CallbackFunctions.h"
#include <tiffio.h>
#include "ImageIO.h"
#include "definitions.h"
#include "util.h"
#include <stdio.h>

class ImageIO_binary: public ImageIO {
private:
	const char *fileName;
	TIFF * fileDescriptor;
	CallbackFunctions *cb;

public:
	/*!
	 * Constructor
	 */
	ImageIO_binary(CallbackFunctions *callback);

	~ImageIO_binary() {
	}

	/*!
	 * Sets the path and file name of the input tiff file.
	 * \param name Path and file name of the input tiff file
	 */
	void setFileName(char *name);

	/*!
	 * This method returns the path and file name of the input tiff file.
	 * \return Path and file name of the input tiff file
	 */
	const char* getFileName();

	/*!
	 * This method opens the file for reading
	 * \return Returns 0 if the input file name is "0", i.e., the input file name is empty.  Returns 1 if file name is not "0"
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
	 * \param destinationArray Image will be copied into a 3D boost array.  This array should be appropriately sized.
	 * \param tiffSlice The slice of the image being read
	 * \param boostSlice The slice of the boost matrix that tiff slice is read into
	 * \param cachePolicy Unused argument
	 */
	void read(boost::multi_array<float, 3> *destinationArray, int tiffSlice,
			int boostSlice, int cachePolicy);
	void read(boost::multi_array<unsigned char, 3> *destinationArray,
			int tiffSlice, int boostSlice, int cachePolicy);

	/*!
	 * This method provides output functionality for 32-bit images.  It appends a specified slice of a boost matrix to the output file.  Note that the output file must be written sequentially.
	 * \param boostMatrix 3D boost array representing an image
	 * \param boostSlice The slice of inputPixels to append to output file
	 * \param cachePolicy unused argument
	 */
	void write(boost::multi_array<float, 3> *boostMatrix, int boostSlice,
			int cachePolicy);
	void write(boost::multi_array<unsigned char, 3> *boostMatrix,
			int boostSlice, int cachePolicy);

	/*!
	 * This method returns the x, y, z dimensions of the input file.
	 * \param dimx x-dimension
	 * \param dimy y-dimension
	 * \param dimz z-dimension
	 */
	void getDimensions(int &dimx, int &dimy, int &dimz);

	void writeToSlice(boost::multi_array<unsigned char, 3> *boostMatrix,
			int boostSlice, int cachePolicy, int outSlice);
};

#endif
