/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "boostAbort.h"
#include <iostream>
#include "CallbackFunctions.h"
#include "callback_binary.h"
#include "ImageIO.h"
#include "ImageIO_binary.h"
#include "TextIO.h"
#include "TextIO_binary.h"
#include "imageProcessingPlugins.h"
using namespace std;

int main(int argc, char * const argv[]) {

	// Sanity check: make sure enough arguments
	if (argc<NUMBER_OF_IMAGE_INPUTS+NUMBER_OF_TEXT_INPUTS+NUMBER_OF_COMMANDS+NUMBER_OF_IMAGE_OUTPUTS+NUMBER_OF_TEXT_OUTPUTS+1) {
		printf("Not enough arguments\n");
		return 1;
	}

	// cb is a struct containing pointers to callback functions
  	CallbackFunctions *cb = new CallbackFunctions;
  	cb->log = &log;
	cb->progressReport=&progressReport;
	cb->getMoreWork=&getMoreWork;
	cb->freeGetMoreWork=&freeGetMoreWork;
	cb->getProtobufMetadata=&getProtobufMetadata;
	cb->shouldInterrupt=&shouldInterrupt;
	cb->logThreshold=LOG_LEVEL_BINARY;

	// imageInput is an vector of ImageIO classes that provide image input functionality
	vector<ImageIO*> imageInput;
	// textInput is a vector of TextIO classes that provide text input functionality
	vector<TextIO*> textInput;
	// imageOutput is an vector of ImageIO classes that provide image output functionality
	vector<ImageIO*> imageOutput;
	// textOutput is a vector of TextIO classes that provide text output functionality
	vector<TextIO*> textOutput;

	char *command=NULL;
	int returnValue=0;
	try {
		//boost::array<short, 5> aShort = {0, 1, 2} ;
		//short st=aShort[5];
		for (int i=1; i<=NUMBER_OF_IMAGE_INPUTS; i++) {
			ImageIO_binary *iio = new ImageIO_binary(cb);
			iio->setFileName(argv[i]);
			imageInput.push_back(iio);
		}

		for (int i=1;i<=NUMBER_OF_TEXT_INPUTS;i++) {
			TextIO_binary *tio = new TextIO_binary(cb);
			tio->setFileName(argv[NUMBER_OF_IMAGE_INPUTS+i]);
			textInput.push_back(tio);
		}

		for (int i=1; i<=NUMBER_OF_IMAGE_OUTPUTS; i++) {
			ImageIO_binary *iio = new ImageIO_binary(cb);
			iio->setFileName(argv[NUMBER_OF_IMAGE_INPUTS+NUMBER_OF_TEXT_INPUTS+NUMBER_OF_COMMANDS+i]);
			imageOutput.push_back(iio);
		}

		for (int i=1;i<=NUMBER_OF_TEXT_OUTPUTS;i++) {
			TextIO_binary *tio = new TextIO_binary(cb);
			tio->setFileName(argv[NUMBER_OF_IMAGE_INPUTS+NUMBER_OF_TEXT_INPUTS+NUMBER_OF_COMMANDS+NUMBER_OF_IMAGE_OUTPUTS+i]);
			textOutput.push_back(tio);
		}

		// get command is a string that tells the plugin what to do
		command = argv[NUMBER_OF_IMAGE_INPUTS+NUMBER_OF_TEXT_INPUTS+1];

		// run plugin
		returnValue = imageProcessingPlugins(imageInput,textInput,command,imageOutput,textOutput,cb);

//        } catch (std::exception const & ex) {
//                log(cb,1,"Catching exception %s",ex.what());
	} catch (...) {
		log(cb,1,"ERROR; catching exception: %s",boost::current_exception_diagnostic_information().c_str());
		returnValue=1;
	}

	// free memory
	for (unsigned int i=0; i<imageInput.size(); i++) {
		delete imageInput[i]; imageInput[i]=NULL;
	}
	for (unsigned int i=0; i<textInput.size(); i++) {
		delete textInput[i]; textInput[i]=NULL;
	}
	for (unsigned int i=0; i<imageOutput.size(); i++) {
		delete imageOutput[i]; imageOutput[i]=NULL;
	}
	for (unsigned int i=0; i<textOutput.size(); i++) {
		delete textOutput[i]; textOutput[i]=NULL;
	}
	delete cb; cb=NULL;
	return returnValue;
}
