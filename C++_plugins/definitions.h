/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#define MAX_RAY_LENGTH 1500					// maximum ray length for plugin CDiameterPlugins
#define LOG_LEVEL_BINARY 5					// logging message threshold for standalone executable
#define MAX_LOG_SZ 1000						// maximum size of logging message
#define	DISPLAY_PROGRESSBAR_BINARY 1		// true to display progress bar for standalone executable
#define NUMBER_OF_STANDARD_INPUTS_BINARY 100 // number of space delimited entries to read using getMoreWork from binary
#define NUMBER_OF_IMAGE_INPUTS 5			// number of input images for the plugin to read
#define NUMBER_OF_TEXT_INPUTS 3				// number of text inputs for the plugin to read
#define NUMBER_OF_COMMANDS 2				// number of commands for the plugin to read
#define NUMBER_OF_IMAGE_OUTPUTS 6			// number of output images for the plugin to read
#define NUMBER_OF_TEXT_OUTPUTS 1			// number of text outputs for the plugin to write
#define MIN_FLOAT -std::numeric_limits<float>::max()
#define MAX_FLOAT std::numeric_limits<float>::max()
#define MAX_EIG_ITER 10000					// maximum number of iterations used to find eigenvalues
#define INF 1000000				// infinity
#define NUMBER_OF_STANDARD_DEVIATIONS_IN_KERNEL 5
#define PI 3.1415926535f
#define MAX_PROTOBUF_SZ 299999999
#define BWTHRESH 0.5						// value used to threshold bw images
//#define NARROWBANDTHRESH 1.5				// value used to determine whether phi voxel falls within narrow band
#define EPS 0.0001f
#define IMAGEJ_ARG_MAXLENGTH 500
#define BOOST_ENABLE_ASSERT_HANDLER
#include <boost/multi_array.hpp>

#include "protobuf_package.pb.h"
#include <google/protobuf/descriptor.h>
#include <google/protobuf/io/zero_copy_stream_impl_lite.h>
typedef protobuf_package::ProtobufDirectory ProtobufDirectory;
typedef protobuf_package::ProtobufInfo ProtobufInfo;

