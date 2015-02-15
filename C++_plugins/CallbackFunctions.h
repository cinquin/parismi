/*******************************************************************************
  * parismiPlugins v0.1
  * Copyright (c) 2009-2015 Michael Chiang and Olivier Cinquin.
  * All rights reserved. This program and the accompanying materials
  * are made available under a dual license: the two-clause BSD license or
  * the GNU Public License v2.
  ******************************************************************************/

#ifndef CALLBACKFUNCTIONS_H_
#define CALLBACKFUNCTIONS_H_

/**
 * Describes the dimensions of the input stack. The Java wrapper for the plugin should be able to determine from those
 * dimensions what the dimensions of the output stack should be, and will have that created automatically.
 */
struct ImageDimensions {
	int x, y, z, t, c;
};

/**
 * Gives a 5D bounding box to speed up pixel transfers
 */
struct BoundingBox {
	int x0, x1, y0, y1, z0, z1, t0, t1, c0, c1;
};

/**
 * \brief This structure contains pointers to functions that the C++ dynamic library and Java pipeline use to communicate.
 * DO NOT edit or change the ordering of this structure without updating the Java structure to exactly match it.
 * Otherwise the program will likely crash and take down the JVM along with it, without generating any informative
 * error messages.
 */
struct CallbackFunctions {
	/**
	 * Blocks until work is available, and returns a list of C-style strings containing arguments.
	 * Returns NULL if the plugin should terminate.
	 */
	const char** (*getMoreWork)();

	/**
	 * Returns the dimensions of input or output stack specified by inputOrOutputName.
	 * Returns null if input or output could not be found.
	 */
	ImageDimensions * (*getDimensions)(const char * inputOrOutputName);

	/**
	 * Returns the dimensions of input or output stack specified by inputOrOutputName.
	 * Return value is 0 if inputOrOutputName was found, 1 otherwise.
	 */
	int (*getDimensionsByRef)(const char * inputOrOutputName, int &x, int &y,
			int &z, int &t, int &c);

	/**
	 * Set dimensions
	 */
	int (*setDimensions)(const char * inputOrOutputName, int x, int y, int z,
			int t, int c);

	/**
	 * Copy the contents of transferBuffer (passed as first argument to "run") to output slice of index sliceIndex
	 * (indexing begins at 0). The output is automatically allocated by the Java end. If inputOrOutputName is null,
	 * write to the default output of the plugin. If not null, write to the designated stack.
	 * This call is not thread-safe.
	 * If ROI is not null, only copy pixels within it (only x and y coordinates are relevant).
	 * If multiThreaded IO is 1, writing to disk will be handled by the Java pipeline in a separate thread, and the
	 * call to setPixels will return before the data is actually written. This should lead to a significant performance gain if
	 * a sufficiently long computation is performed by the C plugin between each call to setPixels. If all slices are
	 * written in a burst, use multithreadedIO=0 to avoid incurring the cost of synchronization.
	 */
	void (*setPixels)(int sliceIndex, const char * inputOrOutputName,
			BoundingBox * ROI, int cachePolicy, int multiThreadedIO);

	void (*setPixel)(int sliceIndex, const char * inputOrOutputName, int x,
			int y, int cachePolicy, int multiThreadedIO, float value);

	/**
	 * Copy the contents of slice sliceIndex to transferBuffer (passed as first argument to "run").
	 * Indexing begins at 0. If inputOrOutputName is null, read from the default input of the plugin. If not null,
	 * read from the designated stack.
	 * This call is not thread-safe.
	 * If ROI is not null, only copy pixels within it (only x and y coordinates are relevant).
	 */
	void (*getPixels)(int sliceIndex, const char * inputOrOutputName,
			BoundingBox * ROI, int cachePolicy);

	/**
	 * Returns protobuf metadata associated with the input. Array must be preallocated by the caller. Upon return,
	 * bufferSize is set to the size of the data copied into buffer.
	 */
	void (*getProtobufMetadata)(const char * inputOrOutputName, char *buffer,
			int &bufferSize);

	/**
	 * Set protobuf metadata associated with the output. If inputOrOutputName is null,
	 * write to the default output of the plugin. If not null, write to the designated stack.
	 */
	void (*setProtobufMetadata)(const char * data, int dataSize,
			const char * inputOrOutputName);

	/**
	 * Returns true if the user or pipeline would like the plugin to interrupt its current computation, and perform a new
	 * getMoreWork() call. This should be polled at regular intervals (e.g. after computing each slice of a stack).
	 */
	int (*shouldInterrupt)();

	/**
	 * Set the progress displayed to the user to doneOutOf100 (between 0 and 100). It is crucial that this is called
	 * at regular intervals to give good feedback to the user. If it is not called often enough for tasks that take a long
	 * time to complete, the user might think that the program is stuck.
	 */
	void (*progressReport)(int doneOutOf100);

	void (*progressSetIndeterminate)(bool indeterminate);

	/**
	 * Ask pipeline to log the passed string, and display to the user if applicable. Note that anything printed directly to standard output or error will not
	 * be visible to the user.
	 * Log levels:
	 * 0: critical
	 * 1: error
	 * 2: warning
	 * 3: info
	 * 4: debug
	 * 5: verbose debug
	 * 6: verbose verbose debug
	 * 7: .....
	 */
	void (*log)(const char * message, int logLevel);

	void (*printCharacters)(const char * message, int size);

	int pipelineVersion;

	void (*freeGetMoreWork)(const char** work);

	int logThreshold;
};

#endif
