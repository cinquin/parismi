/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef TEXTINPUT_H_
#define TEXTINPUT_H_
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wweak-vtables"

#include "CallbackFunctions.h"
#include <string>
class TextIO {
public:
	virtual ~TextIO() {
	}

	/*!
	 * Sets the path and file name of the input text file.
	 * \param name Path and file name of the input text file
	 */
	virtual void setFileName(const char *name)=0;

	/*!
	 * This method returns the path and file name of the input text file.
	 * \return Path and file name of the input text file
	 */
	virtual std::string getFileName()=0;

	/*!
	 * This method reads a text file
	 * \return A struct containing "data" (a character array) and "dataArraySize" (length of the character array)
	 */
	virtual std::string readText()=0;

	/*!
	 * This method writes a text file
	 */
	virtual void writeText(std::string data)=0;

};

#pragma clang diagnostic pop
#endif /* TEXTINPUT_H_ */
