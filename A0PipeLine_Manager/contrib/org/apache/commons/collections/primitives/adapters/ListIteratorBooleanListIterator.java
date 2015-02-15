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

import java.util.ListIterator;

import org.apache.commons.collections.primitives.BooleanListIterator;

/**
 * Adapts a {@link Number}-valued {@link ListIterator ListIterator} to the {@link BooleanListIterator
 * BooleanListIterator} interface.
 * <p />
 * This implementation delegates most methods to the provided {@link BooleanListIterator BooleanListIterator}
 * implementation in the "obvious" way.
 *
 * @since Commons Primitives 1.1
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
public class ListIteratorBooleanListIterator implements BooleanListIterator {

	/**
	 * Create an {@link BooleanListIterator BooleanListIterator} wrapping
	 * the specified {@link ListIterator ListIterator}. When
	 * the given <i>iterator</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param iterator
	 *            the (possibly <code>null</code>) {@link ListIterator ListIterator} to wrap
	 * @return an {@link BooleanListIterator BooleanListIterator} wrapping the given
	 *         <i>iterator</i>, or <code>null</code> when <i>iterator</i> is <code>null</code>.
	 */
	public static BooleanListIterator wrap(ListIterator iterator) {
		return null == iterator ? null : new ListIteratorBooleanListIterator(iterator);
	}

	/**
	 * Creates an {@link BooleanListIterator BooleanListIterator} wrapping
	 * the specified {@link ListIterator ListIterator}.
	 * 
	 * @see #wrap
	 */
	public ListIteratorBooleanListIterator(ListIterator iterator) {
		_iterator = iterator;
	}

	@Override
	public int nextIndex() {
		return _iterator.nextIndex();
	}

	@Override
	public int previousIndex() {
		return _iterator.previousIndex();
	}

	@Override
	public boolean hasNext() {
		return _iterator.hasNext();
	}

	@Override
	public boolean hasPrevious() {
		return _iterator.hasPrevious();
	}

	@Override
	public boolean next() {
		return ((Boolean) _iterator.next()).booleanValue();
	}

	@Override
	public boolean previous() {
		return ((Boolean) _iterator.previous()).booleanValue();
	}

	@Override
	public void add(boolean element) {
		_iterator.add(new Boolean(element));
	}

	@Override
	public void set(boolean element) {
		_iterator.set(new Boolean(element));
	}

	@Override
	public void remove() {
		_iterator.remove();
	}

	private ListIterator _iterator = null;

}
