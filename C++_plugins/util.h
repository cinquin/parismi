/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "CallbackFunctions.h"
#include <cstdarg>
#include <stdio.h>
#include <stdarg.h>
#include "definitions.h"
#include <math.h>

#ifndef ATOMIC_OPS
#define ATOMIC_OPS
void atomic_increment(std::atomic<unsigned int> *i);
#endif

void log(CallbackFunctions *cb, int logLevel, const char *fmt, ...);
int sgn(int x);
float sgn(float x);
bool isCloseTo(float a, float b);
