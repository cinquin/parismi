/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.IOException;

/**
 * Allows for multi-threaded, efficient access to pixel data provided by an ImageAccessor.
 *
 */
public class SliceAccessorAdapter implements SliceAccessor {

	@Override
	public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, float[] destinationArray) throws IOException {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, short[] destinationArray) throws IOException {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, byte[] destinationArray) throws IOException {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public float getFloat(int x, int y, int z) throws IOException {
		throw new RuntimeException("Unimplemented");
	}

}
