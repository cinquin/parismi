/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "util.h"
#include <atomic>

bool isCloseTo(float a, float b) {
	if (fabsf(a - b) < EPS) {
		return true;
	} else {
		return false;
	}
}

void atomic_increment(std::atomic<unsigned int> *i) {
	std::atomic_fetch_add_explicit(i, (unsigned int) 1, std::memory_order_relaxed);
}

/*!
 * Provides logging functionality
 */
__attribute__((__format__ (__printf__, 3, 0)))
void log(CallbackFunctions *cb, int logLevel, const char *fmt, ...) {

	if (logLevel > cb->logThreshold)
		return;
	int size;
	va_list adx, adx2;

	va_start(adx, fmt);
	va_copy(adx2, adx);
	size = vsnprintf(NULL, 0, fmt, adx);
	va_end(adx);

	if (size > MAX_LOG_SZ - 2) {
		size = MAX_LOG_SZ - 2;
	}
	char logMessage[MAX_LOG_SZ];
	(void) vsnprintf(logMessage, (unsigned int) size + 1, fmt, adx2);

	cb->log(logMessage, logLevel);
	va_end(adx2);
}

/*! function:		sgn
 * 	Description:	matlab "sign" function
 */
int sgn(int x) {
	if (x > 0)
		return 1;
	if (x < 0)
		return -1;
	return 0;
}

float sgn(float x) {
	if (x >= 0) {
		return 1;
	} else {
		return -1;
	}
}

