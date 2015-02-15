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

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.collections.primitives.ShortCollection;

/**
 * Adapts an {@link ShortCollection ShortCollection} to the {@link java.util.Collection Collection} interface.
 * <p />
 * This implementation delegates most methods to the provided {@link ShortCollection ShortCollection} implementation in
 * the "obvious" way.
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
final public class ShortCollectionCollection extends AbstractShortCollectionCollection implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a {@link Collection Collection} wrapping
	 * the specified {@link ShortCollection ShortCollection}. When
	 * the given <i>collection</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param collection
	 *            the (possibly <code>null</code>) {@link ShortCollection ShortCollection} to wrap
	 * @return a {@link Collection Collection} wrapping the given
	 *         <i>collection</i>, or <code>null</code> when <i>collection</i> is <code>null</code>.
	 */
	@SuppressWarnings("all")
	public static Collection wrap(ShortCollection collection) {
		if (null == collection) {
			return null;
		} else if (collection instanceof Serializable) {
			return new ShortCollectionCollection(collection);
		} else {
			return new NonSerializableShortCollectionCollection(collection);
		}
	}

	/**
	 * Creates a {@link Collection Collection} wrapping
	 * the specified {@link ShortCollection ShortCollection}.
	 * 
	 * @see #wrap
	 */
	private ShortCollectionCollection(ShortCollection collection) {
		_collection = collection;
	}

	@Override
	protected ShortCollection getShortCollection() {
		return _collection;
	}

	private ShortCollection _collection = null;
}
