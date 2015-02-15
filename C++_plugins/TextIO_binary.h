/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef TEXTIOBINARY_H_
#define TEXTIOBINARY_H_

#include "TextIO.h"
#include "CallbackFunctions.h"
#include <string>
#include "util.h"

class TextIO_binary: public TextIO {
private:
	std::string fileName;
	CallbackFunctions *cb;

public:
	/*!
	 * Constructor
	 * \param cb A struct of useful callback functions
	 */
	TextIO_binary(CallbackFunctions *cb);

	~TextIO_binary() {
	}

	/*!
	 * Sets the path of input text file.
	 * \param name Path of input text file
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

#endif /* TEXTIOBINARY_H_ */
