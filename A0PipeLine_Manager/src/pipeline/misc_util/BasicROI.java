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
 * Used by JNACallToNativeLibrary to communicate ROIs with external libraries, to speed up pixel transfers.
 *
 */
public class BasicROI extends Structure {
	public BasicROI() { // Need a 0-parameter constructor for the JNA
	}

	public int x0;
	public int x1;
	public int y0;
	public int y1;
	public int z0;
	public int z1;
	public int t0;
	public int t1;

	public BasicROI(int x0, int x1, int y0, int y1, int z0, int z1, int t0, int t1) {
		this.x0 = x0;
		this.x1 = x1;
		this.y0 = y0;
		this.y1 = y1;
		this.z0 = z0;
		this.z1 = z1;
		this.t0 = t0;
		this.t1 = t1;
	}

	public BasicROI(int x0, int x1, int y0, int y1) {
		this.x0 = x0;
		this.x1 = x1;
		this.y0 = y0;
		this.y1 = y1;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List getFieldOrder() {
		return Arrays.asList("x0", "x1", "y0", "y1", "z0", "z1", "t0", "t1");
	}
}
