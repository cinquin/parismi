/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef EXCEPTION
#define EXCEPTION
#include <execinfo.h>
#include <sys/param.h>
#include "definitions.h"
struct Exception: public boost::exception, public std::runtime_error {
	char *new_message;
	Exception(const Exception &e);
	~Exception() noexcept;
	Exception(const char * const fmt, ...);
	const char* what() const noexcept;
};
#endif
