/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix;

import java.util.Iterator;

public interface ILongMatrix {
	public long get(int[] coordinates);

	public void put(int[] coordinates, long value);

	public ILongIterator getIterator();

	/**
	 * 
	 * @param axes
	 *            Axes of the matrices that will be returned by the iterator (i.e., axes that are preserved
	 *            by the iteration operation and thus not iterated over). May be of length 0 but not null.
	 * @return An iterator that iterates over all elements in all axes other than those specified in parameter "axes".
	 *         The order in which iteration is performed is not predetermined.
	 */
	public Iterator<ILongMatrix> getMatrixIterator(final Object[] axes);

	public int[] getSizes();
}
