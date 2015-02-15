/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "run.h"

using namespace std;

extern "C"
/**
 * Called to setup the plugin. transferBuffer is allocated by the Java end; it is a one-dimensional FLOAT array of length
 * slice width * slice height, and is used for input and output. imageDimensions describe the dimensions of the stack,
 * callbacks is a list of callback addresses to communicate with the pipeline, and stringArg is a variable-length
 * list of C-string arguments.
 * Once the plugin is setup, it should enter a loop that calls getMoreWork() to get a task to perform when one is available.
 * The call to getMoreWork will block, and only return when a task is actually available. The plugin should return when
 * getMoreWork() returns null.
 */

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
int run(float *transferBuffer, ImageDimensions *imageDimensions,
		CallbackFunctions *cb, const char *stringArg, ...) {
#pragma clang diagnostic pop
	// Check if callbacks, transferBuffer have been properly set up by caller
	if (cb == NULL) {
		syslog(0, "Null callbacks");
		return 1;
	}
	if (transferBuffer == NULL) {
		syslog(0, "NULL transfer buffer");
		log(cb, 0, "NULL transfer buffer");
		return 1;
	}

	log(cb, 4, "Entered c++ shared library");

	// get input arguments, equivalent to argv[i] in main
	va_list arg;
	va_start(arg,stringArg);
	char *argv[IMAGEJ_ARG_MAXLENGTH];
	for (int i = 0; i < NUMBER_OF_IMAGE_INPUTS + NUMBER_OF_TEXT_INPUTS
			+ NUMBER_OF_COMMANDS + NUMBER_OF_IMAGE_OUTPUTS
			+ NUMBER_OF_TEXT_OUTPUTS; i++) {
		argv[i] = va_arg(arg,char *);
	}
	va_end(arg);

	// Display arguments passed from pipeline for debugging
	log(cb, 4, "Arguments passed from pipeline:");
	for (int i = 0; i < NUMBER_OF_IMAGE_INPUTS + NUMBER_OF_TEXT_INPUTS
			+ NUMBER_OF_COMMANDS + NUMBER_OF_IMAGE_OUTPUTS
			+ NUMBER_OF_TEXT_OUTPUTS; i++) {
		log(cb, 4, argv[i]);
	}

	// create input/output objects
	vector<ImageIO*> imageInput;
	for (int i = 0; i < NUMBER_OF_IMAGE_INPUTS; i++) {
		ImageIO_sharedLibrary *iio = new ImageIO_sharedLibrary(cb,
				transferBuffer);
		iio->setFileName(argv[i]);
		imageInput.push_back(iio);
	}

	vector<TextIO*> textInput;
	for (int i = 0; i < NUMBER_OF_TEXT_INPUTS; i++) {
		TextIO_sharedLibrary *tio =
				new TextIO_sharedLibrary(cb, transferBuffer);
		tio->setFileName(argv[NUMBER_OF_IMAGE_INPUTS + i]);
		textInput.push_back(tio);
	}

	vector<ImageIO*> imageOutput;
	for (int i = 0; i < NUMBER_OF_IMAGE_OUTPUTS; i++) {
		ImageIO_sharedLibrary *iio = new ImageIO_sharedLibrary(cb,
				transferBuffer);
		iio->setFileName(
				argv[NUMBER_OF_IMAGE_INPUTS + NUMBER_OF_TEXT_INPUTS
						+ NUMBER_OF_COMMANDS + i]);
		imageOutput.push_back(iio);
	}

	vector<TextIO*> textOutput;
	for (int i = 0; i < NUMBER_OF_TEXT_OUTPUTS; i++) {
		TextIO_sharedLibrary *tio =
				new TextIO_sharedLibrary(cb, transferBuffer);
		tio->setFileName(
				argv[NUMBER_OF_IMAGE_INPUTS + NUMBER_OF_TEXT_INPUTS
						+ NUMBER_OF_COMMANDS + NUMBER_OF_IMAGE_OUTPUTS + i]);
		textOutput.push_back(tio);
	}

	// enter plugin
	char *command = argv[NUMBER_OF_IMAGE_INPUTS + NUMBER_OF_TEXT_INPUTS];

	int rn;

	try {
		rn = imageProcessingPlugins(imageInput, textInput, command,
				imageOutput, textOutput, cb);
		//        } catch (std::exception const & ex) {
		//                log(cb,1,"Catching exception %s",ex.what());
		//		rn = 1;
	} catch (...) {
		log(cb, 1, "ERROR; catching exception: %s",
				boost::current_exception_diagnostic_information().c_str());
		rn = 1;
	}

	// clean up
	for (vTI i = 0; i < imageInput.size(); i++) {
		delete imageInput[i];
		imageInput[i] = NULL;
	}
	for (vTI i = 0; i < textInput.size(); i++) {
		delete textInput[i];
		textInput[i] = NULL;
	}
	for (vTI i = 0; i < imageOutput.size(); i++) {
		delete imageOutput[i];
		imageOutput[i] = NULL;
	}
	for (vTI i = 0; i < textOutput.size(); i++) {
		delete textOutput[i];
		textOutput[i] = NULL;
	}

	log(cb, 4, "Exiting shared library");
	return rn;
}

extern "C" void getVersion(char * buffer, int maxLength) {
	for (int i = 0; i < maxLength; i++) {
		buffer[i] = gitVersion[i];
		if (gitVersion[i] == 0)
			break;
	}
	buffer[maxLength - 1] = 0;
}

