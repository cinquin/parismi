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

import org.apache.commons.collections.primitives.ShortCollection;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractShortCollectionCollection implements Collection {

	@Override
	public boolean add(Object element) {
		return getShortCollection().add(((Number) element).shortValue());
	}

	@Override
	public boolean addAll(Collection c) {
		return getShortCollection().addAll(CollectionShortCollection.wrap(c));
	}

	@Override
	public void clear() {
		getShortCollection().clear();
	}

	@Override
	public boolean contains(Object element) {
		return getShortCollection().contains(((Number) element).shortValue());
	}

	@Override
	public boolean containsAll(Collection c) {
		return getShortCollection().containsAll(CollectionShortCollection.wrap(c));
	}

	@Override
	public String toString() {
		return getShortCollection().toString();
	}

	@Override
	public boolean isEmpty() {
		return getShortCollection().isEmpty();
	}

	/**
	 * {@link ShortIteratorIterator#wrap wraps} the {@link org.apache.commons.collections.primitives.ShortIterator
	 * ShortIterator} returned by my underlying {@link ShortCollection ShortCollection},
	 * if any.
	 */
	@Override
	public Iterator iterator() {
		return ShortIteratorIterator.wrap(getShortCollection().iterator());
	}

	@Override
	public boolean remove(Object element) {
		return getShortCollection().removeElement(((Number) element).shortValue());
	}

	@Override
	public boolean removeAll(Collection c) {
		return getShortCollection().removeAll(CollectionShortCollection.wrap(c));
	}

	@Override
	public boolean retainAll(Collection c) {
		return getShortCollection().retainAll(CollectionShortCollection.wrap(c));
	}

	@Override
	public int size() {
		return getShortCollection().size();
	}

	@Override
	public Object[] toArray() {
		short[] a = getShortCollection().toArray();
		Object[] A = new Object[a.length];
		for (int i = 0; i < a.length; i++) {
			A[i] = new Short(a[i]);
		}
		return A;
	}

	@Override
	public Object[] toArray(Object[] A) {
		short[] a = getShortCollection().toArray();
		if (A.length < a.length) {
			A = (Object[]) (Array.newInstance(A.getClass().getComponentType(), a.length));
		}
		for (int i = 0; i < a.length; i++) {
			A[i] = new Short(a[i]);
		}
		if (A.length > a.length) {
			A[a.length] = null;
		}

		return A;
	}

	protected abstract ShortCollection getShortCollection();
}
