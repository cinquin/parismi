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
import java.util.List;

import org.apache.commons.collections.primitives.ShortCollection;
import org.apache.commons.collections.primitives.ShortIterator;
import org.apache.commons.collections.primitives.ShortList;
import org.apache.commons.collections.primitives.ShortListIterator;

/**
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractListShortList extends AbstractCollectionShortCollection implements ShortList {

	@Override
	public void add(int index, short element) {
		getList().add(index, new Short(element));
	}

	@Override
	public boolean addAll(int index, ShortCollection collection) {
		return getList().addAll(index, ShortCollectionCollection.wrap(collection));
	}

	@Override
	public short get(int index) {
		return ((Number) getList().get(index)).shortValue();
	}

	@Override
	public int indexOf(short element) {
		return getList().indexOf(new Short(element));
	}

	@Override
	public int lastIndexOf(short element) {
		return getList().lastIndexOf(new Short(element));
	}

	/**
	 * {@link ListIteratorShortListIterator#wrap wraps} the {@link ShortList ShortList} returned by my underlying
	 * {@link ShortListIterator ShortListIterator},
	 * if any.
	 */
	@Override
	public ShortListIterator listIterator() {
		return ListIteratorShortListIterator.wrap(getList().listIterator());
	}

	/**
	 * {@link ListIteratorShortListIterator#wrap wraps} the {@link ShortList ShortList} returned by my underlying
	 * {@link ShortListIterator ShortListIterator},
	 * if any.
	 */
	@Override
	public ShortListIterator listIterator(int index) {
		return ListIteratorShortListIterator.wrap(getList().listIterator(index));
	}

	@Override
	public short removeElementAt(int index) {
		return ((Number) getList().remove(index)).shortValue();
	}

	@Override
	public short set(int index, short element) {
		return ((Number) getList().set(index, new Short(element))).shortValue();
	}

	@Override
	public ShortList subList(int fromIndex, int toIndex) {
		return ListShortList.wrap(getList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ShortList) {
			ShortList that = (ShortList) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				ShortIterator thisiter = iterator();
				ShortIterator thatiter = that.iterator();
				while (thisiter.hasNext()) {
					if (thisiter.next() != thatiter.next()) {
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
		return getList().hashCode();
	}

	@Override
	final protected Collection getCollection() {
		return getList();
	}

	abstract protected List getList();
}
