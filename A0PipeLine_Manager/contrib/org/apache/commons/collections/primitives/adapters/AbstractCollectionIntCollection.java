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

import org.apache.commons.collections.primitives.IntCollection;
import org.apache.commons.collections.primitives.IntIterator;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractCollectionIntCollection implements IntCollection {
	protected AbstractCollectionIntCollection() {
	}

	@Override
	public boolean add(int element) {
		return getCollection().add(new Integer(element));
	}

	@Override
	public boolean addAll(IntCollection c) {
		return getCollection().addAll(IntCollectionCollection.wrap(c));
	}

	@Override
	public void clear() {
		getCollection().clear();
	}

	@Override
	public boolean contains(int element) {
		return getCollection().contains(new Integer(element));
	}

	@Override
	public boolean containsAll(IntCollection c) {
		return getCollection().containsAll(IntCollectionCollection.wrap(c));
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
	 * {@link IteratorIntIterator#wrap wraps} the {@link java.util.Iterator Iterator} returned by my underlying
	 * {@link Collection Collection},
	 * if any.
	 */
	@Override
	public IntIterator intIterator() {
		return IteratorIntIterator.wrap(getCollection().iterator());
	}

	@Override
	public boolean removeElement(int element) {
		return getCollection().remove(new Integer(element));
	}

	@Override
	public boolean removeAll(IntCollection c) {
		return getCollection().removeAll(IntCollectionCollection.wrap(c));
	}

	@Override
	public boolean retainAll(IntCollection c) {
		return getCollection().retainAll(IntCollectionCollection.wrap(c));
	}

	@Override
	public int size() {
		return getCollection().size();
	}

	@Override
	public int[] toIntArray() {
		Object[] src = getCollection().toArray();
		int[] dest = new int[src.length];
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Number) (src[i])).intValue();
		}
		return dest;
	}

	@Override
	public int[] toIntArray(int[] dest) {
		Object[] src = getCollection().toArray();
		if (dest.length < src.length) {
			dest = new int[src.length];
		}
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Number) (src[i])).intValue();
		}
		return dest;
	}

	protected abstract Collection getCollection();

}
