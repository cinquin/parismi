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

import java.util.Iterator;

import org.apache.commons.collections.primitives.IntIterator;

/**
 * Adapts a {@link java.lang.Number Number}-valued {@link Iterator Iterator} to the {@link IntIterator IntIterator}
 * interface.
 * <p />
 * This implementation delegates most methods to the provided {@link Iterator Iterator} implementation in the "obvious"
 * way.
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
public class IteratorIntIterator implements IntIterator {

	/**
	 * Create an {@link IntIterator IntIterator} wrapping
	 * the specified {@link Iterator Iterator}. When
	 * the given <i>iterator</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param iterator
	 *            the (possibly <code>null</code>) {@link Iterator Iterator} to wrap
	 * @return an {@link IntIterator IntIterator} wrapping the given
	 *         <i>iterator</i>, or <code>null</code> when <i>iterator</i> is <code>null</code>.
	 */
	public static IntIterator wrap(Iterator iterator) {
		return null == iterator ? null : new IteratorIntIterator(iterator);
	}

	/**
	 * Creates an {@link IntIterator IntIterator} wrapping
	 * the specified {@link Iterator Iterator}.
	 * 
	 * @see #wrap
	 */
	public IteratorIntIterator(Iterator iterator) {
		_iterator = iterator;
	}

	@Override
	public boolean hasNext() {
		return _iterator.hasNext();
	}

	@Override
	public int next() {
		return ((Number) (_iterator.next())).intValue();
	}

	@Override
	public void remove() {
		_iterator.remove();
	}

	private Iterator _iterator = null;

}
