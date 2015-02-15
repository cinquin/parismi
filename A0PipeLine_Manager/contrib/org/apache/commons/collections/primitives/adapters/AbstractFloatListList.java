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

import org.apache.commons.collections.primitives.FloatCollection;
import org.apache.commons.collections.primitives.FloatList;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractFloatListList extends AbstractFloatCollectionCollection implements List {

	@Override
	public void add(int index, Object element) {
		getFloatList().add(index, ((Number) element).floatValue());
	}

	@Override
	public boolean addAll(int index, Collection c) {
		return getFloatList().addAll(index, CollectionFloatCollection.wrap(c));
	}

	@Override
	public Object get(int index) {
		return new Float(getFloatList().get(index));
	}

	@Override
	public int indexOf(Object element) {
		return getFloatList().indexOf(((Number) element).floatValue());
	}

	@Override
	public int lastIndexOf(Object element) {
		return getFloatList().lastIndexOf(((Number) element).floatValue());
	}

	/**
	 * {@link FloatListIteratorListIterator#wrap wraps} the
	 * {@link org.apache.commons.collections.primitives.FloatListIterator FloatListIterator} returned by my underlying
	 * {@link FloatList FloatList},
	 * if any.
	 */
	@Override
	public ListIterator listIterator() {
		return FloatListIteratorListIterator.wrap(getFloatList().listIterator());
	}

	/**
	 * {@link FloatListIteratorListIterator#wrap wraps} the
	 * {@link org.apache.commons.collections.primitives.FloatListIterator FloatListIterator} returned by my underlying
	 * {@link FloatList FloatList},
	 * if any.
	 */
	@Override
	public ListIterator listIterator(int index) {
		return FloatListIteratorListIterator.wrap(getFloatList().listIterator(index));
	}

	@Override
	public Object remove(int index) {
		return new Float(getFloatList().removeElementAt(index));
	}

	@Override
	public Object set(int index, Object element) {
		return new Float(getFloatList().set(index, ((Number) element).floatValue()));
	}

	@Override
	public List subList(int fromIndex, int toIndex) {
		return FloatListList.wrap(getFloatList().subList(fromIndex, toIndex));
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
		return getFloatList().hashCode();
	}

	@Override
	protected final FloatCollection getFloatCollection() {
		return getFloatList();
	}

	protected abstract FloatList getFloatList();

}
