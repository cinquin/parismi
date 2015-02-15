/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#include "Exception.h"

Exception::Exception(const Exception &e) :
	std::runtime_error(e) {
	new_message = (char *) malloc(sizeof(char) * (strlen(e.new_message) + 1));
	strcpy(new_message, e.new_message);
}

__attribute__((__format__ (__printf__, 2, 0)))
Exception::Exception(const char * const fmt, ...) :
	std::runtime_error(fmt) {
	int message_size;
	va_list adx, adx2;
	va_start(adx, fmt);
	va_copy(adx2, adx);
	message_size = vsnprintf(NULL, 0, fmt, adx) + 1;
	va_end(adx);

	if (message_size > MAX_LOG_SZ - 2) {
		message_size = MAX_LOG_SZ - 2;
	}
	char *log_message = new char[message_size + 1];
	//char log_message[message_size+1];
	(void) vsnprintf(log_message, (unsigned int) message_size + 1, fmt, adx2);
	log_message[message_size - 1] = '\n';

#if defined(FREEBSD) && (__FreeBSD_version>=1000000)
	void *addrlist[100];
	size_t backtrace_length = backtrace(addrlist, 100);
	char **trace = backtrace_symbols_fmt(addrlist, backtrace_length, " %a <%n%D> at %f \n");
#else
	size_t backtrace_length = 0;
	char **trace = NULL;
#endif

	int string_length = message_size;
	for (unsigned int i = 0; i < backtrace_length; i++) {
		string_length += strlen(trace[i]);
	}

	new_message = (char *) malloc(
			sizeof(char) * ((unsigned int) string_length + 1));
	strncpy(new_message, log_message, (unsigned int) message_size);
	int offset = message_size;

	for (unsigned int i = 0; i < backtrace_length; i++) {
		strncpy(&new_message[offset], trace[i], strlen(trace[i]));
		offset += strlen(trace[i]);
	}
	new_message[offset] = 0;

	if (trace != NULL) {
		free(trace);
	}

	delete[] log_message;
}

const char* Exception::what() const throw () {
	return new_message;
}

Exception::~Exception() throw () {
	if (new_message != NULL) {
		free(new_message);
		new_message = NULL;
	}
}
