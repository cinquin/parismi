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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections.primitives.CharCollection;
import org.apache.commons.collections.primitives.CharList;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractCharListList extends AbstractCharCollectionCollection implements List {

	@Override
	public void add(int index, Object element) {
		getCharList().add(index, ((Character) element).charValue());
	}

	@Override
	public boolean addAll(int index, Collection c) {
		return getCharList().addAll(index, CollectionCharCollection.wrap(c));
	}

	@Override
	public Object get(int index) {
		return new Character(getCharList().get(index));
	}

	@Override
	public int indexOf(Object element) {
		return getCharList().indexOf(((Character) element).charValue());
	}

	@Override
	public int lastIndexOf(Object element) {
		return getCharList().lastIndexOf(((Character) element).charValue());
	}

	/**
	 * {@link CharListIteratorListIterator#wrap wraps} the
	 * {@link org.apache.commons.collections.primitives.CharListIterator CharListIterator} returned by my underlying
	 * {@link CharList CharList},
	 * if any.
	 */
	@Override
	public ListIterator listIterator() {
		return CharListIteratorListIterator.wrap(getCharList().listIterator());
	}

	/**
	 * {@link CharListIteratorListIterator#wrap wraps} the
	 * {@link org.apache.commons.collections.primitives.CharListIterator CharListIterator} returned by my underlying
	 * {@link CharList CharList},
	 * if any.
	 */
	@Override
	public ListIterator listIterator(int index) {
		return CharListIteratorListIterator.wrap(getCharList().listIterator(index));
	}

	@Override
	public Object remove(int index) {
		return new Character(getCharList().removeElementAt(index));
	}

	@Override
	public Object set(int index, Object element) {
		return new Character(getCharList().set(index, ((Character) element).charValue()));
	}

	@Override
	public List subList(int fromIndex, int toIndex) {
		return CharListList.wrap(getCharList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof List) {
			List that = (List) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				Iterator thisiter = iterator();
				Iterator thatiter = that.iterator();
				while (thisiter.hasNext()) {
					Object thiselt = thisiter.next();
					Object thatelt = thatiter.next();
					if (null == thiselt ? null != thatelt : !(thiselt.equals(thatelt))) {
						return false;
					}
				}
				return true;
			}
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getCharList().hashCode();
	}

	@Override
	protected final CharCollection getCharCollection() {
		return getCharList();
	}

	protected abstract CharList getCharList();

}
