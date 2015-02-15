/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix.recursive_iterator;

public final class LongMeanAggregator extends AbstractLongAggregator {

	private LongMatrix putCountMatrix;

	public LongMeanAggregator(AbstractLongMatrix matrix) {
		super(matrix);

		putCountMatrix = new LongMatrix(matrix.getSizes(), matrix.getAxes());
	}

	@Override
	public void computeAll() {
	}

	@Override
	public void put(int[] coordinates, long value) {
		long putCount = putCountMatrix.get(coordinates);

		matrix.putIgnoreAggregate(coordinates, (matrix.get(coordinates) * putCount + value) / ++putCount);

		putCountMatrix.put(coordinates, putCount);
	}
}
