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

import org.apache.commons.collections.primitives.FloatListIterator;

/**
 * Adapts a {@link Number}-valued {@link ListIterator ListIterator} to the {@link FloatListIterator FloatListIterator}
 * interface.
 * <p />
 * This implementation delegates most methods to the provided {@link FloatListIterator FloatListIterator} implementation
 * in the "obvious" way.
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
public class ListIteratorFloatListIterator implements FloatListIterator {

	/**
	 * Create an {@link FloatListIterator FloatListIterator} wrapping
	 * the specified {@link ListIterator ListIterator}. When
	 * the given <i>iterator</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param iterator
	 *            the (possibly <code>null</code>) {@link ListIterator ListIterator} to wrap
	 * @return an {@link FloatListIterator FloatListIterator} wrapping the given
	 *         <i>iterator</i>, or <code>null</code> when <i>iterator</i> is <code>null</code>.
	 */
	public static FloatListIterator wrap(ListIterator iterator) {
		return null == iterator ? null : new ListIteratorFloatListIterator(iterator);
	}

	/**
	 * Creates an {@link FloatListIterator FloatListIterator} wrapping
	 * the specified {@link ListIterator ListIterator}.
	 * 
	 * @see #wrap
	 */
	public ListIteratorFloatListIterator(ListIterator iterator) {
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
	public float next() {
		return ((Number) _iterator.next()).floatValue();
	}

	@Override
	public float previous() {
		return ((Number) _iterator.previous()).floatValue();
	}

	@Override
	public void add(float element) {
		_iterator.add(new Float(element));
	}

	@Override
	public void set(float element) {
		_iterator.set(new Float(element));
	}

	@Override
	public void remove() {
		_iterator.remove();
	}

	private ListIterator _iterator = null;

}
