/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

/**
 * Used by JNACallToNativeLibrary to communicate image dimensions data to external libraries.
 *
 */
public class SimpleImageDimensions extends Structure {
	public SimpleImageDimensions() { // Need a 0-parameter constructor for the JNA
	}

	public SimpleImageDimensions(int width, int height, int depth, int time) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.time = time;
		this.channels = -1;
	}

	public int width, height, depth, time, channels;

	@SuppressWarnings("rawtypes")
	@Override
	protected List getFieldOrder() {
		return Arrays.asList("width", "height", "depth", "time", "channels");
	}
}
