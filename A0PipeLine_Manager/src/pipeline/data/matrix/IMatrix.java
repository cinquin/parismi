/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix;

import java.util.Iterator;

public interface IMatrix {
	public Object get(int index);

	public Object get(int[] coordinates);

	public void put(int[] coordinates, Object value);

	public Iterator<IMatrix> getIterator(final Object[] axes);

	public int[] getSizes();
}
