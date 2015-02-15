/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections.primitives;

/**
 * Abstract base class for {@link IntCollection}s.
 * <p />
 * Read-only subclasses must override {@link #intIterator} and {@link #size}. Mutable subclasses should also override
 * {@link #add} and {@link IntIterator#remove IntIterator.remove}. All other methods have at least some base
 * implementation derived from these. Subclasses may choose to override these methods to provide a more efficient
 * implementation.
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480460 $ $Date: 2006-11-29 00:14:21 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
public abstract class AbstractIntCollection implements IntCollection {
	@Override
	public abstract IntIterator intIterator();

	@Override
	public abstract int size();

	AbstractIntCollection() {
	}

	/** Unsupported in this base implementation. */
	@Override
	public boolean add(int element) {
		throw new UnsupportedOperationException("add(int) is not supported.");
	}

	@Override
	public boolean addAll(IntCollection c) {
		boolean modified = false;
		for (IntIterator iter = c.intIterator(); iter.hasNext();) {
			modified |= add(iter.next());
		}
		return modified;
	}

	@Override
	public void clear() {
		for (IntIterator iter = intIterator(); iter.hasNext();) {
			iter.next();
			iter.remove();
		}
	}

	@Override
	public boolean contains(int element) {
		for (IntIterator iter = intIterator(); iter.hasNext();) {
			if (iter.next() == element) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(IntCollection c) {
		for (IntIterator iter = c.intIterator(); iter.hasNext();) {
			if (!contains(iter.next())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean isEmpty() {
		return (0 == size());
	}

	@Override
	public boolean removeElement(int element) {
		for (IntIterator iter = intIterator(); iter.hasNext();) {
			if (iter.next() == element) {
				iter.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean removeAll(IntCollection c) {
		boolean modified = false;
		for (IntIterator iter = c.intIterator(); iter.hasNext();) {
			modified |= removeElement(iter.next());
		}
		return modified;
	}

	@Override
	public boolean retainAll(IntCollection c) {
		boolean modified = false;
		for (IntIterator iter = intIterator(); iter.hasNext();) {
			if (!c.contains(iter.next())) {
				iter.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public int[] toIntArray() {
		int[] array = new int[size()];
		int i = 0;
		for (IntIterator iter = intIterator(); iter.hasNext();) {
			array[i] = iter.next();
			i++;
		}
		return array;
	}

	@Override
	public int[] toIntArray(int[] a) {
		if (a.length < size()) {
			return toIntArray();
		} else {
			int i = 0;
			for (IntIterator iter = intIterator(); iter.hasNext();) {
				a[i] = iter.next();
				i++;
			}
			return a;
		}
	}
}
