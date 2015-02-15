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

import org.apache.commons.collections.primitives.DoubleCollection;
import org.apache.commons.collections.primitives.DoubleIterator;
import org.apache.commons.collections.primitives.DoubleList;
import org.apache.commons.collections.primitives.DoubleListIterator;

/**
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractListDoubleList extends AbstractCollectionDoubleCollection implements DoubleList {

	@Override
	public void add(int index, double element) {
		getList().add(index, new Double(element));
	}

	@Override
	public boolean addAll(int index, DoubleCollection collection) {
		return getList().addAll(index, DoubleCollectionCollection.wrap(collection));
	}

	@Override
	public double get(int index) {
		return ((Number) getList().get(index)).doubleValue();
	}

	@Override
	public int indexOf(double element) {
		return getList().indexOf(new Double(element));
	}

	@Override
	public int lastIndexOf(double element) {
		return getList().lastIndexOf(new Double(element));
	}

	/**
	 * {@link ListIteratorDoubleListIterator#wrap wraps} the {@link DoubleList DoubleList} returned by my underlying
	 * {@link DoubleListIterator DoubleListIterator},
	 * if any.
	 */
	@Override
	public DoubleListIterator listIterator() {
		return ListIteratorDoubleListIterator.wrap(getList().listIterator());
	}

	/**
	 * {@link ListIteratorDoubleListIterator#wrap wraps} the {@link DoubleList DoubleList} returned by my underlying
	 * {@link DoubleListIterator DoubleListIterator},
	 * if any.
	 */
	@Override
	public DoubleListIterator listIterator(int index) {
		return ListIteratorDoubleListIterator.wrap(getList().listIterator(index));
	}

	@Override
	public double removeElementAt(int index) {
		return ((Number) getList().remove(index)).doubleValue();
	}

	@Override
	public double set(int index, double element) {
		return ((Number) getList().set(index, new Double(element))).doubleValue();
	}

	@Override
	public DoubleList subList(int fromIndex, int toIndex) {
		return ListDoubleList.wrap(getList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DoubleList) {
			DoubleList that = (DoubleList) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				DoubleIterator thisiter = iterator();
				DoubleIterator thatiter = that.iterator();
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
