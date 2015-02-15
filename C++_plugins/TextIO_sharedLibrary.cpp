/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "TextIO_sharedLibrary.h"
using namespace std;

TextIO_sharedLibrary::TextIO_sharedLibrary(CallbackFunctions * callback,
		float *buffer) {
	cb = callback;
	transferBuffer = buffer;
}

void TextIO_sharedLibrary::setFileName(const char *name) {
	fileName = name;
}

string TextIO_sharedLibrary::getFileName() {
	return fileName;
}

string TextIO_sharedLibrary::readText() {

	// Sanity check
	if (strcmp(fileName.c_str(), "0") == 0) {
		log(cb, 1, "Error: Reading text file '0'");
		throw 999;
	}

	char* buffer = new char[MAX_PROTOBUF_SZ];
	int bufferSize = MAX_PROTOBUF_SZ;
	cb->getProtobufMetadata(fileName.c_str(), buffer, bufferSize);

	// Sanity checks
	if (bufferSize <= 0) {
		log(cb, 1, "Error: text buffer size < 0 (empty protobuf file?)");
		throw 999;
	}
	if (bufferSize >= MAX_PROTOBUF_SZ) {
		log(cb, 1,
				"Error: allocate more space to text buffer (MAX_PROTOBUF_SZ_IMAGEJ)");
		throw 999;
	}

	string rn;
	rn.assign(buffer, (unsigned int) bufferSize);
	delete buffer;
	buffer = NULL;
	return rn;
}

void TextIO_sharedLibrary::writeText(string data) {
	cb->setProtobufMetadata(data.c_str(), (int) data.length(), fileName.c_str());
}

