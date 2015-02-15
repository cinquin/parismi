/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix.permutation;

import java.util.Arrays;
import java.util.Iterator;

import pipeline.data.matrix.ILongMatrix;

public abstract class AbstractLongMatrix implements ILongMatrix {
	protected Object[] axes;
	protected int[] sizes;
	protected int depth;

	public abstract AbstractLongMatrix getMatrix(int index);

	public AbstractLongMatrix getMatrix(int[] coordinates) {
		return getMatrix(0, coordinates);
	}

	private AbstractLongMatrix getMatrix(int coordDepth, int[] coordinates) {
		if (coordDepth == coordinates.length) {
			return getMatrix(coordinates[coordDepth]);
		} else {
			return getMatrix(coordinates[coordDepth]).getMatrix(coordDepth + 1, coordinates);
		}
	}

	@Override
	public final long get(int[] coordinates) {
		return get_aux(0, coordinates);
	}

	protected abstract long get_aux(int coordDepth, int[] coordinates);

	@Override
	public abstract void put(int[] coordinates, long value);

	@Override
	public final Iterator<ILongMatrix> getMatrixIterator(final Object[] axes) {
		return new Iterator<ILongMatrix>() {
			/**
			 * List of axis indeces that we need to iterate over
			 */
			int[] indicesToIterate = new int[AbstractLongMatrix.this.axes.length - axes.length];
			int[] coordinate = new int[AbstractLongMatrix.this.axes.length];
			/**
			 * Number of elements in each dimension of the result
			 */
			int[] resultSizes = new int[axes.length];
			/**
			 * Mapping from result axes to axes in original dataset
			 */
			int[] axisPermutation = new int[axes.length];

			{ // Iterator initialization

				int iteratedAxisIndex = 0;
				for (int i = 0; i < AbstractLongMatrix.this.axes.length; i++) {
					int j = 0;
					for (; j < axes.length; j++) {
						if (AbstractLongMatrix.this.axes[i].equals(axes[j])) {
							resultSizes[j] = AbstractLongMatrix.this.sizes[i];
							axisPermutation[j] = i;
							break;
						}
					}
					if (j == axes.length) // i.e. axis number i is not listed in axes parameter
					{
						indicesToIterate[iteratedAxisIndex++] = i;
					}
				}
			}

			@Override
			public boolean hasNext() {
				return coordinate[indicesToIterate[0]] < AbstractLongMatrix.this.sizes[indicesToIterate[0]];
			}

			@Override
			public AbstractLongMatrix next() {
				AbstractLongMatrix toReturn =
						new LongShallowMatrix(axes, resultSizes, AbstractLongMatrix.this, coordinate, axisPermutation);

				coordinate[indicesToIterate[indicesToIterate.length - 1]]++;
				for (int j = indicesToIterate.length - 1; j > 0
						&& coordinate[indicesToIterate[j]] >= AbstractLongMatrix.this.sizes[indicesToIterate[j]]; j--) {
					coordinate[indicesToIterate[j]] = 0;
					coordinate[indicesToIterate[j - 1]]++;
				}

				return toReturn;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException("Remove not yet supported with Matrices");
			}
		};
	}

	/*
	 * @Override
	 * public final String toString()
	 * {
	 * StringBuilder toReturn = new StringBuilder();
	 * if(sizes.length == 0)
	 * {
	 * return ((LongShallowMatrix)this).root.get(((LongShallowMatrix)this).positionInRoot).toString();
	 * }
	 * toReturn.append("[");
	 * for(int i = 0; i < sizes[0]; i++)
	 * {
	 * if(i > 0)
	 * toReturn.append(",");
	 * 
	 * if(sizes.length == 1)
	 * {
	 * toReturn.append(get(i));
	 * }
	 * else
	 * toReturn.append((AbstractLongMatrix)get(i));
	 * }
	 * toReturn.append("]");
	 * return toReturn.toString();
	 * }
	 */

	@Override
	public final int[] getSizes() {
		return Arrays.copyOfRange(sizes, depth, sizes.length);
	}
}
