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

#ifdef FREEBSD
# include <machine/atomic.h>
#elif defined OSX
# include <libkern/OSAtomic.h>
# include <boost/numeric/conversion/cast.hpp>
#endif

#ifndef ATOMIC_OPS
#define ATOMIC_OPS
void atomic_increment(volatile uint32_t *i);
#endif

void log(CallbackFunctions *cb, int logLevel, const char *fmt, ...);
int sgn(int x);
float sgn(float x);
bool isEqual(float a, float b);
bool isUnequal(float a, float b);
