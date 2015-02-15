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

import java.util.Collection;

import org.apache.commons.collections.primitives.BooleanCollection;
import org.apache.commons.collections.primitives.BooleanIterator;

/**
 * @since Commons Primitives 1.1
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 */
@SuppressWarnings("all")
abstract class AbstractCollectionBooleanCollection implements BooleanCollection {

	protected AbstractCollectionBooleanCollection() {
	}

	@Override
	public boolean add(boolean element) {
		return getCollection().add(new Boolean(element));
	}

	@Override
	public boolean addAll(BooleanCollection c) {
		return getCollection().addAll(BooleanCollectionCollection.wrap(c));
	}

	@Override
	public void clear() {
		getCollection().clear();
	}

	@Override
	public boolean contains(boolean element) {
		return getCollection().contains(new Boolean(element));
	}

	@Override
	public boolean containsAll(BooleanCollection c) {
		return getCollection().containsAll(BooleanCollectionCollection.wrap(c));
	}

	@Override
	public String toString() {
		return getCollection().toString();
	}

	@Override
	public boolean isEmpty() {
		return getCollection().isEmpty();
	}

	/**
	 * {@link IteratorBooleanIterator#wrap wraps} the {@link java.util.Iterator
	 * Iterator} returned by my underlying {@link java.util.Collection
	 * Collection}, if any.
	 */
	@Override
	public BooleanIterator iterator() {
		return IteratorBooleanIterator.wrap(getCollection().iterator());
	}

	@Override
	public boolean removeElement(boolean element) {
		return getCollection().remove(new Boolean(element));
	}

	@Override
	public boolean removeAll(BooleanCollection c) {
		return getCollection().removeAll(BooleanCollectionCollection.wrap(c));
	}

	@Override
	public boolean retainAll(BooleanCollection c) {
		return getCollection().retainAll(BooleanCollectionCollection.wrap(c));
	}

	@Override
	public int size() {
		return getCollection().size();
	}

	@Override
	public boolean[] toArray() {
		Object[] src = getCollection().toArray();
		boolean[] dest = new boolean[src.length];
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Boolean) (src[i])).booleanValue();
		}
		return dest;
	}

	@Override
	public boolean[] toArray(boolean[] dest) {
		Object[] src = getCollection().toArray();
		if (dest.length < src.length) {
			dest = new boolean[src.length];
		}
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Boolean) (src[i])).booleanValue();
		}
		return dest;
	}

	protected abstract Collection getCollection();

}
