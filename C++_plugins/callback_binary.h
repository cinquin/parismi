/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "definitions.h"
#include <boost/algorithm/string.hpp>
#include <iostream>

void log(const char * message, int logLevel);
void progressReport(int progress);
const char** getMoreWork();
void freeGetMoreWork(const char** work_storage);
void getProtobufMetadata(const char *inputOrOutputName, char * buffer,
		int &bufferSize);
int shouldInterrupt();
