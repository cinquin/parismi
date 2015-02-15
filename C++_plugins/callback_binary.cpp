/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "callback_binary.h"
using namespace std;

/*!
 * Log message to terminal window. Convention is:
 *   - logLevel = 0 is a critical message
 *   - logLevel = 4 is a debugging message
 */
void log(const char * message, int logLevel) {
	if (logLevel <= LOG_LEVEL_BINARY) {
		cout << message << endl;
	}
}

/*!
 * Report percent completion 
 */
void progressReport(int progress) {
	if (DISPLAY_PROGRESSBAR_BINARY) {
		cout << "progress: " << progress << endl;
	}
}

/*!
 * Description: Parse parameters from standard input.
 * This function will read a string from the standard input.  It will split using spaces as delimiters
 * and return work_storage, an array of character arrays.  work_storage is stored on the heap and
 * must be freed manually.
 * If the string is "end", then work_storage will return null
 */
const char** getMoreWork() {
	// read standard input
	string input = "";
	getline(cin, input);

	// return NULL if user inputs "end"
	if (input.compare("end") == 0) {
		return NULL;
	}

	// otherwise, split the string by spaces
	vector<string> splitInput;
	boost::split(splitInput, input, boost::is_any_of(" "));
	if (splitInput.size() > NUMBER_OF_STANDARD_INPUTS_BINARY) {
		cerr
				<< "Too many entries to getMoreWork, increase NUMBER_OF_STANDARD_INPUTS_BINARY"
				<< endl;
		exit(1);
	}
	const char **work_storage =
			new const char *[NUMBER_OF_STANDARD_INPUTS_BINARY];
	for (unsigned int i = 0; i < NUMBER_OF_STANDARD_INPUTS_BINARY; i++) {
		if (i < splitInput.size()) {
			char *str = new char[splitInput[i].size() + 1];
			strcpy(str, splitInput[i].c_str());
			work_storage[i] = str;
		} else {
			char *str = new char[1];
			work_storage[i] = str;
		}
	}
	return work_storage;
}

/*!
 * Description: Free work_storage allocated in function getMoreWork()
 */
void freeGetMoreWork(const char** work_storage) {
	for (int i = 0; i < NUMBER_OF_STANDARD_INPUTS_BINARY; i++) {
		delete work_storage[i];
		work_storage[i] = NULL;
	}
	delete work_storage;
	work_storage = NULL;
}

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
void getProtobufMetadata(const char *inputOrOutputName, char * buffer,
		int &bufferSize) {
}
#pragma clang diagnostic pop

int shouldInterrupt() {
	return 0;
}
