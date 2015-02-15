/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "TextIO_binary.h"
#include <stdio.h>
#include <string.h>
#include <iostream>
#include <fstream>
#include <sstream>
using namespace std;

TextIO_binary::TextIO_binary(CallbackFunctions *callback){
	cb=callback;
}

void TextIO_binary::setFileName(const char *name){
	fileName = name;
}

string TextIO_binary::getFileName(){
	return fileName;
}

string TextIO_binary::readText(){

	// Sanity check
	if (strcmp(fileName.c_str(), "0")==0) {
		log(cb,1,"Error: Reading text file '0'");
		throw 999;
	}

	// read text file
	ifstream in(fileName.c_str());
	stringstream buffer;
	buffer << in.rdbuf();
	string contents(buffer.str());

	return contents;
}

void TextIO_binary::writeText(string data){
	std::ofstream outfile(fileName.c_str(), std::ios::out | std::ios::binary);
	outfile.write(data.c_str(),(int)data.length());
	outfile.close();
}


