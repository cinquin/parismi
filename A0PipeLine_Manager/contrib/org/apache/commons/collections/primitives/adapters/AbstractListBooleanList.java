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

import org.apache.commons.collections.primitives.BooleanCollection;
import org.apache.commons.collections.primitives.BooleanIterator;
import org.apache.commons.collections.primitives.BooleanList;
import org.apache.commons.collections.primitives.BooleanListIterator;

/**
 *
 * @since Commons Primitives 1.1
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
abstract class AbstractListBooleanList extends AbstractCollectionBooleanCollection implements BooleanList {

	@Override
	public void add(int index, boolean element) {
		getList().add(index, new Boolean(element));
	}

	@Override
	public boolean addAll(int index, BooleanCollection collection) {
		return getList().addAll(index, BooleanCollectionCollection.wrap(collection));
	}

	@Override
	public boolean get(int index) {
		return (Boolean) getList().get(index);
	}

	@Override
	public int indexOf(boolean element) {
		return getList().indexOf(new Boolean(element));
	}

	@Override
	public int lastIndexOf(boolean element) {
		return getList().lastIndexOf(new Boolean(element));
	}

	/**
	 * {@link ListIteratorBooleanListIterator#wrap wraps} the {@link BooleanList BooleanList} returned by my underlying
	 * {@link BooleanListIterator BooleanListIterator},
	 * if any.
	 */
	@Override
	public BooleanListIterator listIterator() {
		return ListIteratorBooleanListIterator.wrap(getList().listIterator());
	}

	/**
	 * {@link ListIteratorBooleanListIterator#wrap wraps} the {@link BooleanList BooleanList} returned by my underlying
	 * {@link BooleanListIterator BooleanListIterator},
	 * if any.
	 */
	@Override
	public BooleanListIterator listIterator(int index) {
		return ListIteratorBooleanListIterator.wrap(getList().listIterator(index));
	}

	@Override
	public boolean removeElementAt(int index) {
		return (Boolean) getList().remove(index);
	}

	@Override
	public boolean set(int index, boolean element) {
		return (Boolean) getList().set(index, new Boolean(element));
	}

	@Override
	public BooleanList subList(int fromIndex, int toIndex) {
		return ListBooleanList.wrap(getList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BooleanList) {
			BooleanList that = (BooleanList) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				BooleanIterator thisiter = iterator();
				BooleanIterator thatiter = that.iterator();
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
