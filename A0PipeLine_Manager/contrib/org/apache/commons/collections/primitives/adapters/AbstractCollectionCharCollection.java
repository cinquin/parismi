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

import org.apache.commons.collections.primitives.CharCollection;
import org.apache.commons.collections.primitives.CharIterator;

/**
 * @since Commons Primitives 0.1
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractCollectionCharCollection implements CharCollection {
	protected AbstractCollectionCharCollection() {
	}

	@Override
	public boolean add(char element) {
		return getCollection().add(new Character(element));
	}

	@Override
	public boolean addAll(CharCollection c) {
		return getCollection().addAll(CharCollectionCollection.wrap(c));
	}

	@Override
	public void clear() {
		getCollection().clear();
	}

	@Override
	public boolean contains(char element) {
		return getCollection().contains(new Character(element));
	}

	@Override
	public boolean containsAll(CharCollection c) {
		return getCollection().containsAll(CharCollectionCollection.wrap(c));
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
	 * {@link IteratorCharIterator#wrap wraps} the {@link java.util.Iterator Iterator} returned by my underlying
	 * {@link Collection Collection},
	 * if any.
	 */
	@Override
	public CharIterator iterator() {
		return IteratorCharIterator.wrap(getCollection().iterator());
	}

	@Override
	public boolean removeElement(char element) {
		return getCollection().remove(new Character(element));
	}

	@Override
	public boolean removeAll(CharCollection c) {
		return getCollection().removeAll(CharCollectionCollection.wrap(c));
	}

	@Override
	public boolean retainAll(CharCollection c) {
		return getCollection().retainAll(CharCollectionCollection.wrap(c));
	}

	@Override
	public int size() {
		return getCollection().size();
	}

	@Override
	public char[] toArray() {
		Object[] src = getCollection().toArray();
		char[] dest = new char[src.length];
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Character) (src[i])).charValue();
		}
		return dest;
	}

	@Override
	public char[] toArray(char[] dest) {
		Object[] src = getCollection().toArray();
		if (dest.length < src.length) {
			dest = new char[src.length];
		}
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Character) (src[i])).charValue();
		}
		return dest;
	}

	protected abstract Collection getCollection();

}
