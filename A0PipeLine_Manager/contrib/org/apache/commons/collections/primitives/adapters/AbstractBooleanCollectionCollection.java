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

import org.apache.commons.collections.primitives.BooleanCollection;

/**
 * @since Commons Primitives 1.1
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 */

@SuppressWarnings("all")
abstract class AbstractBooleanCollectionCollection implements Collection {

	@Override
	public boolean add(Object element) {
		return getBooleanCollection().add(((Boolean) element).booleanValue());
	}

	@Override
	public boolean addAll(Collection c) {
		return getBooleanCollection().addAll(CollectionBooleanCollection.wrap(c));
	}

	@Override
	public void clear() {
		getBooleanCollection().clear();
	}

	@Override
	public boolean contains(Object element) {
		return getBooleanCollection().contains(((Boolean) element).booleanValue());
	}

	@Override
	public boolean containsAll(Collection c) {
		return getBooleanCollection().containsAll(CollectionBooleanCollection.wrap(c));
	}

	@Override
	public String toString() {
		return getBooleanCollection().toString();
	}

	@Override
	public boolean isEmpty() {
		return getBooleanCollection().isEmpty();
	}

	/**
	 * {@link BooleanIteratorIterator#wrap wraps} the {@link org.apache.commons.collections.primitives.BooleanIterator
	 * BooleanIterator} returned by my underlying {@link org.apache.commons.collections.primitives.BooleanCollection
	 * BooleanCollection}, if any.
	 */
	@Override
	public Iterator iterator() {
		return BooleanIteratorIterator.wrap(getBooleanCollection().iterator());
	}

	@Override
	public boolean remove(Object element) {
		return getBooleanCollection().removeElement(((Boolean) element).booleanValue());
	}

	@Override
	public boolean removeAll(Collection c) {
		return getBooleanCollection().removeAll(CollectionBooleanCollection.wrap(c));
	}

	@Override
	public boolean retainAll(Collection c) {
		return getBooleanCollection().retainAll(CollectionBooleanCollection.wrap(c));
	}

	@Override
	public int size() {
		return getBooleanCollection().size();
	}

	@Override
	public Object[] toArray() {
		boolean[] a = getBooleanCollection().toArray();
		Object[] A = new Object[a.length];
		for (int i = 0; i < a.length; i++) {
			A[i] = new Boolean(a[i]);
		}
		return A;
	}

	@Override
	public Object[] toArray(Object[] A) {
		boolean[] a = getBooleanCollection().toArray();
		if (A.length < a.length) {
			A = (Object[]) (Array.newInstance(A.getClass().getComponentType(), a.length));
		}
		for (int i = 0; i < a.length; i++) {
			A[i] = new Boolean(a[i]);
		}
		if (A.length > a.length) {
			A[a.length] = null;
		}

		return A;
	}

	protected abstract BooleanCollection getBooleanCollection();
}
