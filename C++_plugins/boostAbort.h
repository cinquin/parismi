// Transform Boost aborts into exceptions
// Copyright 2007 The Trustees of Indiana University.
// Use, modification and distribution is subject to the Boost Software
// License, Version 1.0. (See accompanying file LICENSE_1_0.txt or copy at
// http://www.boost.org/LICENSE_1_0.txt)
//  Boost.MultiArray Library
//  Authors: Ronald Garcia
//           Jeremy Siek
//           Andrew Lumsdaine
//  See http://www.boost.org/libs/multi_array for documentation.
//
// Using the BOOST.ASSERT mechanism to replace library assertions
// with exceptions

#ifndef OVERRIDE_BOOST_ABORT
#define OVERRIDE_BOOST_ABORT
#include "definitions.h"
#include "Exception.h"
#include <boost/exception/diagnostic_information.hpp>
#include <boost/throw_exception.hpp>
#include <boost/assert.hpp>
#include <stdexcept>
namespace boost {
__attribute__ ((noreturn))
void assertion_failed(char const* expr, char const* function, char const* file,
		long line) {
	BOOST_THROW_EXCEPTION(Exception("Boost assert failed: %s %s %s line %li", expr, function, file, line));
}

__attribute__ ((noreturn))
void assertion_failed_msg(char const * expr, char const * msg,
		char const * function, char const * file, long line) {
	BOOST_THROW_EXCEPTION(Exception("Boost assert failed: %s %s %s %s line %li", expr, msg, function, file, line));
}
}
#endif
