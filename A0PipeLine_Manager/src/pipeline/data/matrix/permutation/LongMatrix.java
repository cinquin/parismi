/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix.permutation;

import pipeline.data.matrix.ILongIterator;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class LongMatrix extends AbstractLongMatrix {
	AbstractLongMatrix[] data;
	boolean isBottom;

	public LongMatrix(int[] sizes, Object[] axes) {
		this(sizes, axes, sizes.length == 1 ? 1 : 2);
	}

	public LongMatrix(int[] sizes, Object[] axes, int numDimensionsToStoreInArray) {
		this(0, sizes, axes, numDimensionsToStoreInArray);
	}

	public LongMatrix(int depth, int[] sizes, Object[] axes, int numDimensionsToStoreInArray) {
		this.axes = axes;
		this.depth = depth;
		/*
		 * if(!isAxisSetValid(axes))
		 * System.err.println("Warning: invalid set of axes");
		 */
		this.sizes = sizes;
		if (numDimensionsToStoreInArray + depth >= sizes.length) {
			isBottom = true;
			int product = 1;
			for (int i = depth; i < sizes.length; i++) {
				product *= sizes[i];
			}
			data = new AbstractLongMatrix[product];
		} else {
			isBottom = false;
			data = new AbstractLongMatrix[sizes[depth]];
			for (int i = 0; i < sizes[depth]; i++) {
				data[i] = new LongMatrix(depth + 1, sizes, axes, numDimensionsToStoreInArray);
			}
		}
	}

	/**
	 * Is coordDepth required, or is coordDepth always equal to depth anyway?
	 */
	@Override
	protected final long get_aux(int coordDepth, int[] coordinates) {
		if (coordDepth != depth)
			Utils.log("coordDepth is " + coordDepth + " but depth is " + depth, LogLevel.INFO);
		return data[coordinates[coordDepth]].get_aux(coordDepth + 1, coordinates);
	}

	@Override
	public final void put(int[] coordinates, long value) {
		if (coordinates.length != sizes.length) {
			throw new IllegalArgumentException("Invalid dimensionality of coordinates to put in LongMatrix");
		}
		if (isBottom) {
			@SuppressWarnings("unused")
			int index = 0;
			int dimension = 1;
			for (int i = sizes.length - 1; i >= depth; i--) {
				if (i < coordinates.length)
					index += coordinates[i] * dimension;
				dimension *= sizes[i];
			}
			// data[index] = value;
		} else {
			((LongMatrix) data[coordinates[depth]]).put(coordinates, value);
		}
	}

	@Override
	public ILongIterator getIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AbstractLongMatrix getMatrix(int index) {
		// TODO Auto-generated method stub
		return null;
	}
}
