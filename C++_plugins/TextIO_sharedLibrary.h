/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef TEXTIOSHAREDLIBRARY_H_
#define TEXTIOSHAREDLIBRARY_H_

#include "CallbackFunctions.h"
#include "TextIO.h"
#include "definitions.h"
#include "util.h"

class TextIO_sharedLibrary: public TextIO {
private:
	CallbackFunctions *cb;
	float *transferBuffer;
	std::string fileName;

public:
	/*!
	 * Constructor
	 * \param callback A struct of useful callback functions
	 */
	TextIO_sharedLibrary(CallbackFunctions *callback, float *buffer);

	/*!
	 * Sets the path and file name of the input text file.
	 * \param name Path and file name of the input text file
	 */
	void setFileName(const char *name);

	/*!
	 * This method returns the path and file name of the input text file.
	 * \return Path and file name of the input text file
	 */
	std::string getFileName();

	/*!
	 * This method reads a text file
	 * \return A struct containing "data" (a character array) and "dataArraySize" (length of the character array)
	 */
	std::string readText();

	/*!
	 * This method writes a text file
	 */
	void writeText(std::string data);
};

#endif /* TEXTIOSHAREDLIBRARY_H_ */
