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

import org.apache.commons.collections.primitives.ByteCollection;
import org.apache.commons.collections.primitives.ByteList;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractByteListList extends AbstractByteCollectionCollection implements List {

	@Override
	public void add(int index, Object element) {
		getByteList().add(index, ((Number) element).byteValue());
	}

	@Override
	public boolean addAll(int index, Collection c) {
		return getByteList().addAll(index, CollectionByteCollection.wrap(c));
	}

	@Override
	public Object get(int index) {
		return new Byte(getByteList().get(index));
	}

	@Override
	public int indexOf(Object element) {
		return getByteList().indexOf(((Number) element).byteValue());
	}

	@Override
	public int lastIndexOf(Object element) {
		return getByteList().lastIndexOf(((Number) element).byteValue());
	}

	/**
	 * {@link ByteListIteratorListIterator#wrap wraps} the
	 * {@link org.apache.commons.collections.primitives.ByteListIterator ByteListIterator} returned by my underlying
	 * {@link ByteList ByteList},
	 * if any.
	 */
	@Override
	public ListIterator listIterator() {
		return ByteListIteratorListIterator.wrap(getByteList().listIterator());
	}

	/**
	 * {@link ByteListIteratorListIterator#wrap wraps} the
	 * {@link org.apache.commons.collections.primitives.ByteListIterator ByteListIterator} returned by my underlying
	 * {@link ByteList ByteList},
	 * if any.
	 */
	@Override
	public ListIterator listIterator(int index) {
		return ByteListIteratorListIterator.wrap(getByteList().listIterator(index));
	}

	@Override
	public Object remove(int index) {
		return new Byte(getByteList().removeElementAt(index));
	}

	@Override
	public Object set(int index, Object element) {
		return new Byte(getByteList().set(index, ((Number) element).byteValue()));
	}

	@Override
	public List subList(int fromIndex, int toIndex) {
		return ByteListList.wrap(getByteList().subList(fromIndex, toIndex));
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
		return getByteList().hashCode();
	}

	@Override
	protected final ByteCollection getByteCollection() {
		return getByteList();
	}

	protected abstract ByteList getByteList();

}
