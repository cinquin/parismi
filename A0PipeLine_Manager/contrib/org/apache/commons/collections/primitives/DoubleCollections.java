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
package org.apache.commons.collections.primitives;

import org.apache.commons.collections.primitives.decorators.UnmodifiableDoubleIterator;
import org.apache.commons.collections.primitives.decorators.UnmodifiableDoubleList;
import org.apache.commons.collections.primitives.decorators.UnmodifiableDoubleListIterator;

/**
 * This class consists exclusively of static methods that operate on or
 * return DoubleCollections.
 * <p>
 * The methods of this class all throw a NullPoDoubleerException if the provided collection is null.
 * 
 * @version $Revision: 480460 $ $Date: 2006-11-29 00:14:21 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
final class DoubleCollections {

	/**
	 * Returns an unmodifiable DoubleList containing only the specified element.
	 * 
	 * @param value
	 *            the single value
	 * @return an unmodifiable DoubleList containing only the specified element.
	 */
	private static DoubleList singletonDoubleList(double value) {
		// TODO: a specialized implementation of DoubleList may be more performant
		DoubleList list = new ArrayDoubleList(1);
		list.add(value);
		return UnmodifiableDoubleList.wrap(list);
	}

	/**
	 * Returns an unmodifiable DoubleIterator containing only the specified element.
	 * 
	 * @param value
	 *            the single value
	 * @return an unmodifiable DoubleIterator containing only the specified element.
	 */
	public static DoubleIterator singletonDoubleIterator(double value) {
		return singletonDoubleList(value).iterator();
	}

	/**
	 * Returns an unmodifiable DoubleListIterator containing only the specified element.
	 * 
	 * @param value
	 *            the single value
	 * @return an unmodifiable DoubleListIterator containing only the specified element.
	 */
	public static DoubleListIterator singletonDoubleListIterator(double value) {
		return singletonDoubleList(value).listIterator();
	}

	/**
	 * Returns an unmodifiable version of the given non-null DoubleList.
	 * 
	 * @param list
	 *            the non-null DoubleList to wrap in an unmodifiable decorator
	 * @return an unmodifiable version of the given non-null DoubleList
	 * @throws NullPointerException
	 *             if the given DoubleList is null
	 * @see org.apache.commons.collections.primitives.decorators.UnmodifiableDoubleList#wrap
	 */
	private static DoubleList unmodifiableDoubleList(DoubleList list) throws NullPointerException {
		if (null == list) {
			throw new NullPointerException();
		}
		return UnmodifiableDoubleList.wrap(list);
	}

	/**
	 * Returns an unmodifiable version of the given non-null DoubleIterator.
	 * 
	 * @param iter
	 *            the non-null DoubleIterator to wrap in an unmodifiable decorator
	 * @return an unmodifiable version of the given non-null DoubleIterator
	 * @throws NullPointerException
	 *             if the given DoubleIterator is null
	 * @see org.apache.commons.collections.primitives.decorators.UnmodifiableDoubleIterator#wrap
	 */
	private static DoubleIterator unmodifiableDoubleIterator(DoubleIterator iter) {
		if (null == iter) {
			throw new NullPointerException();
		}
		return UnmodifiableDoubleIterator.wrap(iter);
	}

	/**
	 * Returns an unmodifiable version of the given non-null DoubleListIterator.
	 * 
	 * @param iter
	 *            the non-null DoubleListIterator to wrap in an unmodifiable decorator
	 * @return an unmodifiable version of the given non-null DoubleListIterator
	 * @throws NullPointerException
	 *             if the given DoubleListIterator is null
	 * @see org.apache.commons.collections.primitives.decorators.UnmodifiableDoubleListIterator#wrap
	 */
	private static DoubleListIterator unmodifiableDoubleListIterator(DoubleListIterator iter) {
		if (null == iter) {
			throw new NullPointerException();
		}
		return UnmodifiableDoubleListIterator.wrap(iter);
	}

	/**
	 * Returns an unmodifiable, empty DoubleList.
	 * 
	 * @return an unmodifiable, empty DoubleList.
	 * @see #EMPTY_DOUBLE_LIST
	 */
	public static DoubleList getEmptyDoubleList() {
		return EMPTY_DOUBLE_LIST;
	}

	/**
	 * Returns an unmodifiable, empty DoubleIterator
	 * 
	 * @return an unmodifiable, empty DoubleIterator.
	 * @see #EMPTY_DOUBLE_ITERATOR
	 */
	public static DoubleIterator getEmptyDoubleIterator() {
		return EMPTY_DOUBLE_ITERATOR;
	}

	/**
	 * Returns an unmodifiable, empty DoubleListIterator
	 * 
	 * @return an unmodifiable, empty DoubleListIterator.
	 * @see #EMPTY_DOUBLE_LIST_ITERATOR
	 */
	public static DoubleListIterator getEmptyDoubleListIterator() {
		return EMPTY_DOUBLE_LIST_ITERATOR;
	}

	/**
	 * An unmodifiable, empty DoubleList
	 * 
	 * @see #getEmptyDoubleList
	 */
	private static final DoubleList EMPTY_DOUBLE_LIST = unmodifiableDoubleList(new ArrayDoubleList(0));

	/**
	 * An unmodifiable, empty DoubleIterator
	 * 
	 * @see #getEmptyDoubleIterator
	 */
	private static final DoubleIterator EMPTY_DOUBLE_ITERATOR =
			unmodifiableDoubleIterator(EMPTY_DOUBLE_LIST.iterator());

	/**
	 * An unmodifiable, empty DoubleListIterator
	 * 
	 * @see #getEmptyDoubleListIterator
	 */
	private static final DoubleListIterator EMPTY_DOUBLE_LIST_ITERATOR =
			unmodifiableDoubleListIterator(EMPTY_DOUBLE_LIST.listIterator());
}
