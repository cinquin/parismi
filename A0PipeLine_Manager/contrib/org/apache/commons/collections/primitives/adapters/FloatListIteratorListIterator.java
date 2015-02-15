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
 * Adapts an {@link FloatListIterator FloatListIterator} to the {@link ListIterator ListIterator} interface.
 * <p />
 * This implementation delegates most methods to the provided {@link FloatListIterator FloatListIterator} implementation
 * in the "obvious" way.
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
public class FloatListIteratorListIterator implements ListIterator {

	/**
	 * Create a {@link ListIterator ListIterator} wrapping
	 * the specified {@link FloatListIterator FloatListIterator}. When
	 * the given <i>iterator</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param iterator
	 *            the (possibly <code>null</code>) {@link FloatListIterator FloatListIterator} to wrap
	 * @return a {@link ListIterator ListIterator} wrapping the given
	 *         <i>iterator</i>, or <code>null</code> when <i>iterator</i> is <code>null</code>.
	 */
	public static ListIterator wrap(FloatListIterator iterator) {
		return null == iterator ? null : new FloatListIteratorListIterator(iterator);
	}

	/**
	 * Creates an {@link ListIterator ListIterator} wrapping
	 * the specified {@link FloatListIterator FloatListIterator}.
	 * 
	 * @see #wrap
	 */
	public FloatListIteratorListIterator(FloatListIterator iterator) {
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
	public Object next() {
		return new Float(_iterator.next());
	}

	@Override
	public Object previous() {
		return new Float(_iterator.previous());
	}

	@Override
	public void add(Object obj) {
		_iterator.add(((Number) obj).floatValue());
	}

	@Override
	public void set(Object obj) {
		_iterator.set(((Number) obj).floatValue());
	}

	@Override
	public void remove() {
		_iterator.remove();
	}

	private FloatListIterator _iterator = null;

}
