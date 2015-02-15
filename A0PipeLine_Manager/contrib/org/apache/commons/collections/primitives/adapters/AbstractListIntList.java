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

import org.apache.commons.collections.primitives.IntCollection;
import org.apache.commons.collections.primitives.IntIterator;
import org.apache.commons.collections.primitives.IntList;
import org.apache.commons.collections.primitives.IntListIterator;

/**
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractListIntList extends AbstractCollectionIntCollection implements IntList {

	@Override
	public void add(int index, int element) {
		getList().add(index, new Integer(element));
	}

	@Override
	public boolean addAll(int index, IntCollection collection) {
		return getList().addAll(index, IntCollectionCollection.wrap(collection));
	}

	@Override
	public int get(int index) {
		return ((Number) getList().get(index)).intValue();
	}

	@Override
	public int indexOf(int element) {
		return getList().indexOf(new Integer(element));
	}

	@Override
	public int lastIndexOf(int element) {
		return getList().lastIndexOf(new Integer(element));
	}

	/**
	 * {@link ListIteratorIntListIterator#wrap wraps} the {@link IntList IntList} returned by my underlying
	 * {@link IntListIterator IntListIterator},
	 * if any.
	 */
	@Override
	public IntListIterator listIterator() {
		return ListIteratorIntListIterator.wrap(getList().listIterator());
	}

	/**
	 * {@link ListIteratorIntListIterator#wrap wraps} the {@link IntList IntList} returned by my underlying
	 * {@link IntListIterator IntListIterator},
	 * if any.
	 */
	@Override
	public IntListIterator listIterator(int index) {
		return ListIteratorIntListIterator.wrap(getList().listIterator(index));
	}

	@Override
	public int removeElementAt(int index) {
		return ((Number) getList().remove(index)).intValue();
	}

	@Override
	public int set(int index, int element) {
		return ((Number) getList().set(index, new Integer(element))).intValue();
	}

	@Override
	public IntList subList(int fromIndex, int toIndex) {
		return ListIntList.wrap(getList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IntList) {
			IntList that = (IntList) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				IntIterator thisiter = intIterator();
				IntIterator thatiter = that.intIterator();
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
