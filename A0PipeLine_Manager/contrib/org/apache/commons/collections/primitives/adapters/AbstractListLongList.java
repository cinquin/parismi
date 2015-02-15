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

import org.apache.commons.collections.primitives.LongCollection;
import org.apache.commons.collections.primitives.LongIterator;
import org.apache.commons.collections.primitives.LongList;
import org.apache.commons.collections.primitives.LongListIterator;

/**
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractListLongList extends AbstractCollectionLongCollection implements LongList {

	@Override
	public void add(int index, long element) {
		getList().add(index, new Long(element));
	}

	@Override
	public boolean addAll(int index, LongCollection collection) {
		return getList().addAll(index, LongCollectionCollection.wrap(collection));
	}

	@Override
	public long get(int index) {
		return ((Number) getList().get(index)).longValue();
	}

	@Override
	public int indexOf(long element) {
		return getList().indexOf(new Long(element));
	}

	@Override
	public int lastIndexOf(long element) {
		return getList().lastIndexOf(new Long(element));
	}

	/**
	 * {@link ListIteratorLongListIterator#wrap wraps} the {@link LongList LongList} returned by my underlying
	 * {@link LongListIterator LongListIterator},
	 * if any.
	 */
	@Override
	public LongListIterator listIterator() {
		return ListIteratorLongListIterator.wrap(getList().listIterator());
	}

	/**
	 * {@link ListIteratorLongListIterator#wrap wraps} the {@link LongList LongList} returned by my underlying
	 * {@link LongListIterator LongListIterator},
	 * if any.
	 */
	@Override
	public LongListIterator listIterator(int index) {
		return ListIteratorLongListIterator.wrap(getList().listIterator(index));
	}

	@Override
	public long removeElementAt(int index) {
		return ((Number) getList().remove(index)).longValue();
	}

	@Override
	public long set(int index, long element) {
		return ((Number) getList().set(index, new Long(element))).longValue();
	}

	@Override
	public LongList subList(int fromIndex, int toIndex) {
		return ListLongList.wrap(getList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LongList) {
			LongList that = (LongList) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				LongIterator thisiter = iterator();
				LongIterator thatiter = that.iterator();
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
