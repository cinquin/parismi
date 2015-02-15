/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix.recursive_iterator;

public abstract class AbstractLongAggregator {
	AbstractLongMatrix matrix;

	public AbstractLongAggregator(AbstractLongMatrix matrix) {
		this.matrix = matrix;
	}

	public abstract void computeAll();

	public abstract void put(int[] coordinates, long value);
}
