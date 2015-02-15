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

import org.apache.commons.collections.primitives.ByteCollection;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractByteCollectionCollection implements Collection {

	@Override
	public boolean add(Object element) {
		return getByteCollection().add(((Number) element).byteValue());
	}

	@Override
	public boolean addAll(Collection c) {
		return getByteCollection().addAll(CollectionByteCollection.wrap(c));
	}

	@Override
	public void clear() {
		getByteCollection().clear();
	}

	@Override
	public boolean contains(Object element) {
		return getByteCollection().contains(((Number) element).byteValue());
	}

	@Override
	public boolean containsAll(Collection c) {
		return getByteCollection().containsAll(CollectionByteCollection.wrap(c));
	}

	@Override
	public String toString() {
		return getByteCollection().toString();
	}

	@Override
	public boolean isEmpty() {
		return getByteCollection().isEmpty();
	}

	/**
	 * {@link ByteIteratorIterator#wrap wraps} the {@link org.apache.commons.collections.primitives.ByteIterator
	 * ByteIterator} returned by my underlying {@link ByteCollection ByteCollection},
	 * if any.
	 */
	@Override
	public Iterator iterator() {
		return ByteIteratorIterator.wrap(getByteCollection().iterator());
	}

	@Override
	public boolean remove(Object element) {
		return getByteCollection().removeElement(((Number) element).byteValue());
	}

	@Override
	public boolean removeAll(Collection c) {
		return getByteCollection().removeAll(CollectionByteCollection.wrap(c));
	}

	@Override
	public boolean retainAll(Collection c) {
		return getByteCollection().retainAll(CollectionByteCollection.wrap(c));
	}

	@Override
	public int size() {
		return getByteCollection().size();
	}

	@Override
	public Object[] toArray() {
		byte[] a = getByteCollection().toArray();
		Object[] A = new Object[a.length];
		for (int i = 0; i < a.length; i++) {
			A[i] = new Byte(a[i]);
		}
		return A;
	}

	@Override
	public Object[] toArray(Object[] A) {
		byte[] a = getByteCollection().toArray();
		if (A.length < a.length) {
			A = (Object[]) (Array.newInstance(A.getClass().getComponentType(), a.length));
		}
		for (int i = 0; i < a.length; i++) {
			A[i] = new Byte(a[i]);
		}
		if (A.length > a.length) {
			A[a.length] = null;
		}

		return A;
	}

	protected abstract ByteCollection getByteCollection();
}
