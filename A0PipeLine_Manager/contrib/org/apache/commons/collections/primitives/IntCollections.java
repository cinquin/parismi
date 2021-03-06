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

import org.apache.commons.collections.primitives.decorators.UnmodifiableIntIterator;
import org.apache.commons.collections.primitives.decorators.UnmodifiableIntList;
import org.apache.commons.collections.primitives.decorators.UnmodifiableIntListIterator;

/**
 * This class consists exclusively of static methods that operate on or
 * return IntCollections.
 * <p>
 * The methods of this class all throw a NullPointerException if the provided collection is null.
 * 
 * @version $Revision: 480460 $ $Date: 2006-11-29 00:14:21 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
final class IntCollections {

	/**
	 * Returns an unmodifiable IntList containing only the specified element.
	 * 
	 * @param value
	 *            the single value
	 * @return an unmodifiable IntList containing only the specified element.
	 */
	private static IntList singletonIntList(int value) {
		// TODO: a specialized implementation of IntList may be more performant
		IntList list = new ArrayIntList(1);
		list.add(value);
		return UnmodifiableIntList.wrap(list);
	}

	/**
	 * Returns an unmodifiable IntIterator containing only the specified element.
	 * 
	 * @param value
	 *            the single value
	 * @return an unmodifiable IntIterator containing only the specified element.
	 */
	public static IntIterator singletonIntIterator(int value) {
		return singletonIntList(value).intIterator();
	}

	/**
	 * Returns an unmodifiable IntListIterator containing only the specified element.
	 * 
	 * @param value
	 *            the single value
	 * @return an unmodifiable IntListIterator containing only the specified element.
	 */
	public static IntListIterator singletonIntListIterator(int value) {
		return singletonIntList(value).listIterator();
	}

	/**
	 * Returns an unmodifiable version of the given non-null IntList.
	 * 
	 * @param list
	 *            the non-null IntList to wrap in an unmodifiable decorator
	 * @return an unmodifiable version of the given non-null IntList
	 * @throws NullPointerException
	 *             if the given IntList is null
	 * @see org.apache.commons.collections.primitives.decorators.UnmodifiableIntList#wrap
	 */
	private static IntList unmodifiableIntList(IntList list) throws NullPointerException {
		if (null == list) {
			throw new NullPointerException();
		}
		return UnmodifiableIntList.wrap(list);
	}

	/**
	 * Returns an unmodifiable version of the given non-null IntIterator.
	 * 
	 * @param iter
	 *            the non-null IntIterator to wrap in an unmodifiable decorator
	 * @return an unmodifiable version of the given non-null IntIterator
	 * @throws NullPointerException
	 *             if the given IntIterator is null
	 * @see org.apache.commons.collections.primitives.decorators.UnmodifiableIntIterator#wrap
	 */
	private static IntIterator unmodifiableIntIterator(IntIterator iter) {
		if (null == iter) {
			throw new NullPointerException();
		}
		return UnmodifiableIntIterator.wrap(iter);
	}

	/**
	 * Returns an unmodifiable version of the given non-null IntListIterator.
	 * 
	 * @param iter
	 *            the non-null IntListIterator to wrap in an unmodifiable decorator
	 * @return an unmodifiable version of the given non-null IntListIterator
	 * @throws NullPointerException
	 *             if the given IntListIterator is null
	 * @see org.apache.commons.collections.primitives.decorators.UnmodifiableIntListIterator#wrap
	 */
	private static IntListIterator unmodifiableIntListIterator(IntListIterator iter) {
		if (null == iter) {
			throw new NullPointerException();
		}
		return UnmodifiableIntListIterator.wrap(iter);
	}

	/**
	 * Returns an unmodifiable, empty IntList.
	 * 
	 * @return an unmodifiable, empty IntList.
	 * @see #EMPTY_INT_LIST
	 */
	public static IntList getEmptyIntList() {
		return EMPTY_INT_LIST;
	}

	/**
	 * Returns an unmodifiable, empty IntIterator
	 * 
	 * @return an unmodifiable, empty IntIterator.
	 * @see #EMPTY_INT_ITERATOR
	 */
	public static IntIterator getEmptyIntIterator() {
		return EMPTY_INT_ITERATOR;
	}

	/**
	 * Returns an unmodifiable, empty IntListIterator
	 * 
	 * @return an unmodifiable, empty IntListIterator.
	 * @see #EMPTY_INT_LIST_ITERATOR
	 */
	public static IntListIterator getEmptyIntListIterator() {
		return EMPTY_INT_LIST_ITERATOR;
	}

	/**
	 * An unmodifiable, empty IntList
	 * 
	 * @see #getEmptyIntList
	 */
	private static final IntList EMPTY_INT_LIST = unmodifiableIntList(new ArrayIntList(0));

	/**
	 * An unmodifiable, empty IntIterator
	 * 
	 * @see #getEmptyIntIterator
	 */
	private static final IntIterator EMPTY_INT_ITERATOR = unmodifiableIntIterator(EMPTY_INT_LIST.intIterator());

	/**
	 * An unmodifiable, empty IntListIterator
	 * 
	 * @see #getEmptyIntListIterator
	 */
	private static final IntListIterator EMPTY_INT_LIST_ITERATOR = unmodifiableIntListIterator(EMPTY_INT_LIST
			.listIterator());
}
