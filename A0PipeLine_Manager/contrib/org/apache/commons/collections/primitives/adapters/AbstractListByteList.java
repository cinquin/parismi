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

import org.apache.commons.collections.primitives.ByteCollection;
import org.apache.commons.collections.primitives.ByteIterator;
import org.apache.commons.collections.primitives.ByteList;
import org.apache.commons.collections.primitives.ByteListIterator;

/**
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
abstract class AbstractListByteList extends AbstractCollectionByteCollection implements ByteList {

	@Override
	public void add(int index, byte element) {
		getList().add(index, new Byte(element));
	}

	@Override
	public boolean addAll(int index, ByteCollection collection) {
		return getList().addAll(index, ByteCollectionCollection.wrap(collection));
	}

	@Override
	public byte get(int index) {
		return ((Number) getList().get(index)).byteValue();
	}

	@Override
	public int indexOf(byte element) {
		return getList().indexOf(new Byte(element));
	}

	@Override
	public int lastIndexOf(byte element) {
		return getList().lastIndexOf(new Byte(element));
	}

	/**
	 * {@link ListIteratorByteListIterator#wrap wraps} the {@link ByteList ByteList} returned by my underlying
	 * {@link ByteListIterator ByteListIterator},
	 * if any.
	 */
	@Override
	public ByteListIterator listIterator() {
		return ListIteratorByteListIterator.wrap(getList().listIterator());
	}

	/**
	 * {@link ListIteratorByteListIterator#wrap wraps} the {@link ByteList ByteList} returned by my underlying
	 * {@link ByteListIterator ByteListIterator},
	 * if any.
	 */
	@Override
	public ByteListIterator listIterator(int index) {
		return ListIteratorByteListIterator.wrap(getList().listIterator(index));
	}

	@Override
	public byte removeElementAt(int index) {
		return ((Number) getList().remove(index)).byteValue();
	}

	@Override
	public byte set(int index, byte element) {
		return ((Number) getList().set(index, new Byte(element))).byteValue();
	}

	@Override
	public ByteList subList(int fromIndex, int toIndex) {
		return ListByteList.wrap(getList().subList(fromIndex, toIndex));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ByteList) {
			ByteList that = (ByteList) obj;
			if (this == that) {
				return true;
			} else if (this.size() != that.size()) {
				return false;
			} else {
				ByteIterator thisiter = iterator();
				ByteIterator thatiter = that.iterator();
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
