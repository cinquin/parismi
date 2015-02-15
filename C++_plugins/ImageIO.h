/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef IMAGEIO_H_
#define IMAGEIO_H_
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wweak-vtables"

#include <boost/multi_array.hpp>
class ImageIO {
public:

	virtual ~ImageIO() {
	}

	/*!
	 * Sets the file name of the image.
	 * \param name File name of image.
	 */
	virtual void setFileName(char *name)=0;

	/*!
	 * Returns file name of the image.
	 * \return File name of image
	 */
	virtual const char* getFileName()=0;

	/*!
	 * Opens file for reading
	 */
	virtual int openForSequentialRead()=0;

	/*!
	 * Opens file for writing
	 */
	virtual int openForSequentialWrite()=0;

	/*!
	 * This method closes a file after reading/writing is finished
	 */
	virtual void close()=0;

	/*!
	 * Copies a specified z-slice of an input file into a specified slice of a boost matrix
	 * \param destinationArray Image z-slice will be copied into this boost array
	 * \param tiffSlice Image z-slice that will be copied
	 * \param boostSlice Boost array z-slice that image will be copied into
	 * \param cachePolicy unused parameter
	 */
	virtual void read(boost::multi_array<float, 3> *destinationArray,
			int tiffSlice, int boostSlice, int cachePolicy)=0;
	virtual void read(boost::multi_array<unsigned char, 3> *destinationArray,
			int tiffSlice, int boostSlice, int cachePolicy)=0;

	/*!
	 * Appends specified z-slice of boost array to end of file.
	 * \param boostMatrix 3D boost array representing an image
	 * \param boostSlice The slice of inputPixels to append to output file
	 * \param cachePolicy unused parameter
	 */
	virtual void write(boost::multi_array<float, 3> *boostMatrix,
			int boostSlice, int cachePolicy)=0;
	virtual void write(boost::multi_array<unsigned char, 3> *boostMatrix,
			int boostSlice, int cachePolicy)=0;

	/*!
	 * This method reads the x, y, z dimensions of the input file
	 */
	virtual void getDimensions(int &dimx, int &dimy, int &dimz)=0;

	virtual void writeToSlice(
			boost::multi_array<unsigned char, 3> *boostMatrix, int boostSlice,
			int cachePolicy, int outSlice)=0;
};

#pragma clang diagnostic pop
#endif
