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

import org.apache.commons.collections.primitives.ByteIterator;

/**
 * Adapts an {@link ByteIterator ByteIterator} to the {@link java.util.Iterator Iterator} interface.
 * <p />
 * This implementation delegates most methods to the provided {@link ByteIterator ByteIterator} implementation in the
 * "obvious" way.
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
public class ByteIteratorIterator implements Iterator {

	/**
	 * Create an {@link Iterator Iterator} wrapping
	 * the specified {@link ByteIterator ByteIterator}. When
	 * the given <i>iterator</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param iterator
	 *            the (possibly <code>null</code>) {@link ByteIterator ByteIterator} to wrap
	 * @return an {@link Iterator Iterator} wrapping the given
	 *         <i>iterator</i>, or <code>null</code> when <i>iterator</i> is <code>null</code>.
	 */
	public static Iterator wrap(ByteIterator iterator) {
		return null == iterator ? null : new ByteIteratorIterator(iterator);
	}

	/**
	 * Creates an {@link Iterator Iterator} wrapping
	 * the specified {@link ByteIterator ByteIterator}.
	 * 
	 * @see #wrap
	 */
	public ByteIteratorIterator(ByteIterator iterator) {
		_iterator = iterator;
	}

	@Override
	public boolean hasNext() {
		return _iterator.hasNext();
	}

	@Override
	public Object next() {
		return new Byte(_iterator.next());
	}

	@Override
	public void remove() {
		_iterator.remove();
	}

	private ByteIterator _iterator = null;

}
