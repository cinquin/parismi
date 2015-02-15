/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix.recursive_iterator;

import java.util.Iterator;

import pipeline.data.matrix.ILongIterator;
import pipeline.data.matrix.ILongMatrix;

public abstract class AbstractLongMatrix implements ILongMatrix {
	/**
	 * All axes used to define this matrix, only one of which (whose index is given by {@link #depth}) corresponds to
	 * this object.
	 */
	protected Object[] axes;
	protected int[] sizes;

	/**
	 * Index in the dimension hierarchy given by {@link #axes} this object corresponds to.
	 */
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

	public static final int[] getBottomDimensionTotalSizes(int[] sizes, int numDimensionsToStoreInArray) {
		int[] totalSizes = new int[numDimensionsToStoreInArray - 1];
		for (int i = 0, product = sizes[sizes.length - 1]; i < totalSizes.length; i++, product *=
				sizes[sizes.length - 1 - i]) {
			totalSizes[i] = product;
		}
		return totalSizes;
	}

	@Override
	public final long get(int[] coordinates) {
		return get_aux(0, coordinates);
	}

	/**
	 * Is this method really necessary??
	 * 
	 * @param coordDepth
	 *            Relative index to consider in the coordinate array??
	 * @param coordinates
	 * @return
	 */
	protected abstract long get_aux(int coordDepth, int[] coordinates);

	@Override
	public abstract void put(int[] coordinates, long value);

	public void putIgnoreAggregate(int[] coordinates, long value) {
		put(coordinates, value);
	}

	@Override
	public abstract ILongIterator getIterator();

	@Override
	public final Iterator<ILongMatrix> getMatrixIterator(final Object[] axes1) {
		// FIXME IS THIS CORRECT?
		// IMPLEMENTATION IN permutation PACKAGE MIGHT BE BETTER???
		if (axes1[0].equals(AbstractLongMatrix.this.axes[depth])) {
			return new Iterator<ILongMatrix>() {
				boolean hasNext = true;

				@Override
				public final boolean hasNext() {
					return hasNext;
				}

				@Override
				public final AbstractLongMatrix next() {
					hasNext = false;
					return AbstractLongMatrix.this;
				}

				@Override
				public final void remove() {
					throw new UnsupportedOperationException("Remove not yet supported with Matrices");
				}
			};
		} else {
			return new Iterator<ILongMatrix>() {
				Iterator<ILongMatrix> subIterator = AbstractLongMatrix.this.getMatrix(0).getMatrixIterator(axes1);
				int subMatrixIndex = 0;

				@Override
				public final boolean hasNext() {
					return subIterator.hasNext();
				}

				@Override
				public final ILongMatrix next() {
					ILongMatrix toReturn = subIterator.next();

					if (!subIterator.hasNext()) {
						subMatrixIndex++;
						if (subMatrixIndex < AbstractLongMatrix.this.sizes[depth])
							subIterator = AbstractLongMatrix.this.getMatrix(subMatrixIndex).getMatrixIterator(axes1);
					}

					return toReturn;
				}

				@Override
				public final void remove() {
					// TODO Auto-generated method stub
					throw new UnsupportedOperationException("Remove not yet supported with Matrices");
				}
			};
		}
	}

	/*
	 * @Override
	 * public final String toString()
	 * {
	 * StringBuilder toReturn = new StringBuilder();
	 * toReturn.append("[");
	 * for(int i = 0; i < sizes[depth]; i++)
	 * {
	 * if(i > 0)
	 * toReturn.append(",");
	 * 
	 * if(sizes.length - depth == 1)
	 * toReturn.append(get(new int[] { i }));
	 * else
	 * toReturn.append(getMatrix(i));
	 * }
	 * toReturn.append("]");
	 * return toReturn.toString();
	 * }
	 */

	public final AbstractLongMatrix project(Object[] axes1, Class<? extends AbstractLongAggregator> aggregator) {
		int[] axesIndices = new int[axes1.length];
		int[] sizes1 = new int[axes1.length];
		for (int i = 0; i < axes1.length; i++) {
			for (int j = 0; j < this.axes.length; j++) {
				if (axes1[i].equals(this.axes[j])) {
					axesIndices[i] = j;
					sizes1[i] = this.sizes[j];
					break;
				}
			}
		}

		LongMatrix projection = new LongMatrix(sizes1, axes1, aggregator);

		int[] coordinates = new int[this.axes.length];

		int[] projectionCoordinates = new int[axes1.length];

		ILongIterator iterator = this.getIterator();

		while (iterator.hasNext()) {
			for (int i = 0; i < axes1.length; i++)
				projectionCoordinates[i] = coordinates[axesIndices[i]];
			projection.put(projectionCoordinates, iterator.next());

			coordinates[coordinates.length - 1]++;
			for (int i = coordinates.length - 1; coordinates[i] == this.sizes[i] && i > 0; coordinates[i] = 0, i--, coordinates[i]++)
				;
		}

		projection.stopAggregating();

		return projection;
	}

	protected abstract void stopAggregating();

	@Override
	public final int[] getSizes() {
		return sizes;// Arrays.copyOfRange(sizes, depth, sizes.length);
	}

	public final Object[] getAxes() {
		return axes;// Arrays.copyOfRange(axes, depth, axes.length);
	}
}
