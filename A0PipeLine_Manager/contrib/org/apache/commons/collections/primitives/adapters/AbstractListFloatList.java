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

import org.apache.commons.collections.primitives.FloatCollection;
import org.apache.commons.collections.primitives.FloatIterator;
import org.apache.commons.collections.primitives.FloatList;
import org.apache.commons.collections.primitives.FloatListIterator;

/**
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractListFloatList extends AbstractCollectionFloatCollection implements FloatList {

	@Override
	public void add(int index, float element) {
		getList().add(index, new Float(element));
	}

	@Override
	public boolean addAll(int index, FloatCollection collection) {
		return getList().addAll(index, FloatCollectionCollection.wrap(collection));
	}

	@Override
	public float get(int index) {
		return ((Number) getList().get(index)).floatValue();
	}

	@Override
	public int indexOf(float element) {
		return getList().indexOf(new Float(element));
	}

	@Override
	public int lastIndexOf(float element) {
		return getList().lastIndexOf(new Float(element));
	}

	/**
	 * {@link ListIteratorFloatListIterator#wrap wraps} the {@link FloatList FloatList} returned by my underlying
	 * {@link FloatListIterator FloatListIterator},
	 * if any.
	 */
	@Override
	public FloatListIterator listIterator() {
		return ListIteratorFloatListIterator.wrap(getList().listIterator());
	}

	/**
	 * {@link ListIteratorFloatListIterator#wrap wraps} the {@link FloatList FloatList} returned by my underlying
	 * {@link FloatListIterator FloatListIterator},
	 * if any.
	 */
	@Override
	public FloatListIterator listIterator(int index) {
		return ListIteratorFloatListIterator.wrap(getList().listIterator(index));
	}

	@Override
	public float removeElementAt(int index) {
		return ((Number) getList().remove(index)).floatValue();
	}

	@Override
	public float set(int index, float element) {
		return ((Number) getList().set(index, new Float(element))).floatValue();
	}

	@Override
	public FloatList subList(int fromIndex, int toIndex) {
		return ListFloatList.wrap(getList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FloatList) {
			FloatList that = (FloatList) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				FloatIterator thisiter = iterator();
				FloatIterator thatiter = that.iterator();
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
