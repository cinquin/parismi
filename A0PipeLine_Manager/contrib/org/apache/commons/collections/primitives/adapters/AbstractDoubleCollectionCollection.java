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
package org.apache.commons.collections.primitives.adapters;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections.primitives.DoubleCollection;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractDoubleCollectionCollection implements Collection {

	@Override
	public boolean add(Object element) {
		return getDoubleCollection().add(((Number) element).doubleValue());
	}

	@Override
	public boolean addAll(Collection c) {
		return getDoubleCollection().addAll(CollectionDoubleCollection.wrap(c));
	}

	@Override
	public void clear() {
		getDoubleCollection().clear();
	}

	@Override
	public boolean contains(Object element) {
		return getDoubleCollection().contains(((Number) element).doubleValue());
	}

	@Override
	public boolean containsAll(Collection c) {
		return getDoubleCollection().containsAll(CollectionDoubleCollection.wrap(c));
	}

	@Override
	public String toString() {
		return getDoubleCollection().toString();
	}

	@Override
	public boolean isEmpty() {
		return getDoubleCollection().isEmpty();
	}

	/**
	 * {@link DoubleIteratorIterator#wrap wraps} the {@link org.apache.commons.collections.primitives.DoubleIterator
	 * DoubleIterator} returned by my underlying {@link DoubleCollection DoubleCollection},
	 * if any.
	 */
	@Override
	public Iterator iterator() {
		return DoubleIteratorIterator.wrap(getDoubleCollection().iterator());
	}

	@Override
	public boolean remove(Object element) {
		return getDoubleCollection().removeElement(((Number) element).doubleValue());
	}

	@Override
	public boolean removeAll(Collection c) {
		return getDoubleCollection().removeAll(CollectionDoubleCollection.wrap(c));
	}

	@Override
	public boolean retainAll(Collection c) {
		return getDoubleCollection().retainAll(CollectionDoubleCollection.wrap(c));
	}

	@Override
	public int size() {
		return getDoubleCollection().size();
	}

	@Override
	public Object[] toArray() {
		double[] a = getDoubleCollection().toArray();
		Object[] A = new Object[a.length];
		for (int i = 0; i < a.length; i++) {
			A[i] = new Double(a[i]);
		}
		return A;
	}

	@Override
	public Object[] toArray(Object[] A) {
		double[] a = getDoubleCollection().toArray();
		if (A.length < a.length) {
			A = (Object[]) (Array.newInstance(A.getClass().getComponentType(), a.length));
		}
		for (int i = 0; i < a.length; i++) {
			A[i] = new Double(a[i]);
		}
		if (A.length > a.length) {
			A[a.length] = null;
		}

		return A;
	}

	protected abstract DoubleCollection getDoubleCollection();
}
