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

import org.apache.commons.collections.primitives.CharCollection;
import org.apache.commons.collections.primitives.CharIterator;
import org.apache.commons.collections.primitives.CharList;
import org.apache.commons.collections.primitives.CharListIterator;

/**
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
abstract class AbstractListCharList extends AbstractCollectionCharCollection implements CharList {

	@Override
	public void add(int index, char element) {
		getList().add(index, new Character(element));
	}

	@Override
	public boolean addAll(int index, CharCollection collection) {
		return getList().addAll(index, CharCollectionCollection.wrap(collection));
	}

	@Override
	public char get(int index) {
		return (Character) getList().get(index);
	}

	@Override
	public int indexOf(char element) {
		return getList().indexOf(new Character(element));
	}

	@Override
	public int lastIndexOf(char element) {
		return getList().lastIndexOf(new Character(element));
	}

	/**
	 * {@link ListIteratorCharListIterator#wrap wraps} the {@link CharList CharList} returned by my underlying
	 * {@link CharListIterator CharListIterator},
	 * if any.
	 */
	@Override
	public CharListIterator listIterator() {
		return ListIteratorCharListIterator.wrap(getList().listIterator());
	}

	/**
	 * {@link ListIteratorCharListIterator#wrap wraps} the {@link CharList CharList} returned by my underlying
	 * {@link CharListIterator CharListIterator},
	 * if any.
	 */
	@Override
	public CharListIterator listIterator(int index) {
		return ListIteratorCharListIterator.wrap(getList().listIterator(index));
	}

	@Override
	public char removeElementAt(int index) {
		return (Character) getList().remove(index);
	}

	@Override
	public char set(int index, char element) {
		return (Character) getList().set(index, new Character(element));
	}

	@Override
	public CharList subList(int fromIndex, int toIndex) {
		return ListCharList.wrap(getList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CharList) {
			CharList that = (CharList) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				CharIterator thisiter = iterator();
				CharIterator thatiter = that.iterator();
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
