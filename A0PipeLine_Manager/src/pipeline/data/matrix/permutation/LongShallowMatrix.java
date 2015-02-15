/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix.permutation;

import java.util.Arrays;

import pipeline.data.matrix.ILongIterator;

public class LongShallowMatrix extends AbstractLongMatrix {
	AbstractLongMatrix root;
	int[] positionInRoot;
	int[] axisPermutation;

	public LongShallowMatrix(Object[] axes, int[] sizes, AbstractLongMatrix root, int[] positionInRoot,
			int[] axisPermutation) {
		this.axes = axes;
		this.sizes = sizes;
		this.root = root;
		this.positionInRoot = positionInRoot.clone();
		this.axisPermutation = axisPermutation;
	}

	@Override
	public final long get_aux(int coordDepth, int[] coordinates) {
		storeCoordinateAsRoot(coordinates);
		return root.get(positionInRoot);
	}

	@Override
	public final AbstractLongMatrix getMatrix(int index) {
		return new LongShallowMatrix(Arrays.copyOfRange(axes, 1, axes.length), Arrays.copyOfRange(sizes, 1,
				sizes.length), root, positionInRoot, Arrays.copyOfRange(axisPermutation, 1, axisPermutation.length));
	}

	@Override
	public final void put(int[] coordinates, long value) {
		storeCoordinateAsRoot(coordinates);
		root.put(positionInRoot, value);
	}

	private final void storeCoordinateAsRoot(int[] coordinates) {
		for (int i = 0; i < coordinates.length; i++)
			positionInRoot[axisPermutation[i]] = coordinates[i];
	}

	@Override
	public ILongIterator getIterator() {
		// TODO Auto-generated method stub
		return null;
	}
}
