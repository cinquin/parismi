/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix.recursive_iterator;

import pipeline.data.matrix.ILongIterator;

public final class LongMatrix extends AbstractLongMatrix {
	private AbstractLongMatrix[] data;
	int numDimensionsToStoreInArray;

	/***
	 * Constructs a multidimensional matrix with long values. Attempts to store up to the last 2 dimensions in a
	 * flattened array at the bottom-most level.
	 * 
	 * @param sizes
	 *            Represents the length of each dimension, respective to the axes.
	 * @param axes
	 *            Represents a label for each dimension in the matrix.
	 */
	public LongMatrix(int[] sizes, Object[] axes) {
		this(sizes, axes, sizes.length == 1 ? 1 : 2, null);
	}

	public LongMatrix(int[] sizes, Object[] axes, Class<? extends AbstractLongAggregator> aggregator) {
		this(sizes, axes, sizes.length == 2 ? 1 : 2, aggregator);
	}

	/***
	 * Constructs a multidimensional matrix with long values.
	 * 
	 * @param sizes
	 *            Represents the length of each dimension, respective to the axes.
	 * @param axes
	 *            Represents a label for each dimension in the matrix.
	 * @param numDimensionsToStoreInArray
	 *            Multidimensional matrices are represented in a recursive hierarchy, and only the matrix at the
	 *            bottom-most level stores actual data. This value determines the number of dimensions should be
	 *            flattened into an array at the bottom matrix.
	 */
	public LongMatrix(int[] sizes, Object[] axes, int numDimensionsToStoreInArray) {
		this(0, sizes, axes, numDimensionsToStoreInArray, getBottomDimensionTotalSizes(sizes,
				numDimensionsToStoreInArray), null);
	}

	public LongMatrix(int[] sizes, Object[] axes, int numDimensionsToStoreInArray,
			Class<? extends AbstractLongAggregator> aggregator) {
		this(0, sizes, axes, numDimensionsToStoreInArray, getBottomDimensionTotalSizes(sizes,
				numDimensionsToStoreInArray), aggregator);
	}

	private LongMatrix(int depth, int[] sizes, Object[] axes, int numDimensionsToStoreInArray,
			int[] bottomDimensionTotalSizes, Class<? extends AbstractLongAggregator> aggregator) {
		this.axes = axes;
		this.depth = depth;
		this.sizes = sizes;
		this.numDimensionsToStoreInArray = numDimensionsToStoreInArray;

		data = new AbstractLongMatrix[sizes[depth]];
		if (depth + 1 >= sizes.length - numDimensionsToStoreInArray) {
			for (int i = 0; i < sizes[depth]; i++) {
				data[i] = new BottomMatrix(depth + 1, sizes, axes, bottomDimensionTotalSizes, aggregator);
			}
		} else {
			for (int i = 0; i < sizes[depth]; i++) {
				data[i] =
						new LongMatrix(depth + 1, sizes, axes, numDimensionsToStoreInArray, bottomDimensionTotalSizes,
								aggregator);
			}
		}
	}

	@Override
	public final AbstractLongMatrix getMatrix(int index) {
		return data[index];
	}

	@Override
	public final long get_aux(int coordDepth, int[] coordinates) {
		return data[coordinates[coordDepth]].get_aux(coordDepth + 1, coordinates);
	}

	@Override
	public final void put(int[] coordinates, long value) {
		data[coordinates[depth]].put(coordinates, value);
	}

	@Override
	public final ILongIterator getIterator() {
		return new ILongIterator() {
			ILongIterator subIterator = LongMatrix.this.getMatrix(0).getIterator();
			int subMatrixIndex = 0;

			@Override
			public final boolean hasNext() {
				return subIterator.hasNext();
			}

			@Override
			public final long next() {
				try {
					return subIterator.next();
				} finally {
					if (!subIterator.hasNext()) {
						subMatrixIndex++;
						if (subMatrixIndex < LongMatrix.this.sizes[depth])
							subIterator = LongMatrix.this.getMatrix(subMatrixIndex).getIterator();
					}
				}
			}
		};
	}

	@Override
	protected final void stopAggregating() {
		for (int i = 0; i < data.length; i++) {
			data[i].stopAggregating();
		}
	}

	/**
	 * Matrix at the bottom level of the axes hierarchy, that stores the objects corresponding to the matrix in an array
	 * that can
	 * be 1- or 2-dimensional. This special class is used for performance and memory reasons.
	 * 
	 * @author tyler
	 *
	 */
	public final class BottomMatrix extends AbstractLongMatrix {
		long[] data1;
		int[] totalSizes;
		int rootIndex;
		AbstractLongAggregator aggregator;

		/**
		 * 
		 * @param depth
		 * @param sizes
		 * @param axes
		 * @param totalSizes
		 * @param aggregator
		 *            May be null
		 */
		public BottomMatrix(int depth, int[] sizes, Object[] axes, int[] totalSizes,
				Class<? extends AbstractLongAggregator> aggregator) {
			if ((totalSizes.length > 1) || (totalSizes.length == 0))
				throw new IllegalArgumentException("Dimensionality of BottomMatrix array must be 1 or 2");
			this.depth = depth;
			this.sizes = sizes;
			this.axes = axes;
			this.totalSizes = totalSizes;
			this.data1 = new long[totalSizes.length == 0 ? sizes[depth] : (totalSizes[0] * sizes[depth])];
			if (aggregator != null) {
				try {
					this.aggregator =
							aggregator.getConstructor(new Class[] { AbstractLongMatrix.class }).newInstance(this);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * 
		 * @param depth
		 *            Position in the list of dimensions.
		 * @param sizes
		 * @param axes
		 * @param rootIndex
		 *            Index of first dimension, if it has already been set (and we thus have a 1D view of an underlying
		 *            2D array).
		 * @param data
		 * @param aggregator
		 */
		public BottomMatrix(int depth, int[] sizes, Object[] axes, int rootIndex, long[] data,
				Class<? extends AbstractLongAggregator> aggregator) {
			this.depth = depth;
			this.sizes = sizes;
			this.axes = axes;
			this.rootIndex = rootIndex;
			this.data1 = data;
			try {
				this.aggregator = aggregator.getConstructor(new Class[] { AbstractLongMatrix.class }).newInstance(this);
			} catch (Exception e) {
				e.printStackTrace();
			}

			for (int product = sizes[sizes.length - 1], i = depth + 2; i < sizes.length; product *=
					sizes[sizes.length + depth - i], i++)
				totalSizes[sizes.length - (i + 1)] = product;
		}

		@Override
		public final AbstractLongMatrix getMatrix(int index) {
			return new BottomMatrix(depth + 1, sizes, axes, index, data1, null);
		}

		@Override
		public final long get_aux(int coordDepth, int[] coordinates) {
			return data1[computeDataIndex(coordDepth, coordinates)];
		}

		@Override
		public final void put(int[] coordinates, long value) {
			if (aggregator == null)
				data1[computeDataIndex(depth, coordinates)] = value;
			else
				aggregator.put(coordinates, value);
		}

		@Override
		public final void putIgnoreAggregate(int[] coordinates, long value) {
			data1[computeDataIndex(depth, coordinates)] = value;
		}

		private final int computeDataIndex(int coordDepth, int[] coordinates) {
			if (totalSizes.length == 0) // Just a 1D array
				return coordinates[coordinates.length - 1];
			if (totalSizes.length == 1) // 2D array
				return totalSizes[0] * coordinates[coordinates.length - 2] + coordinates[coordinates.length - 1];
			int index = rootIndex;
			for (int i = 0; i < coordinates.length - coordDepth - 1; i++) {
				index += coordinates[coordDepth + i] * totalSizes[i];
			}
			index += coordinates[coordinates.length - 1];
			return index;
		}

		@Override
		public final ILongIterator getIterator() {
			return new ILongIterator() {
				int index = 0;

				@Override
				public final boolean hasNext() {
					return index < BottomMatrix.this.data1.length;
				}

				@Override
				public final long next() {
					return BottomMatrix.this.data1[index++];
				}
			};
		}

		@Override
		protected final void stopAggregating() {
			aggregator.computeAll();
			aggregator = null;
		}
	}
}
