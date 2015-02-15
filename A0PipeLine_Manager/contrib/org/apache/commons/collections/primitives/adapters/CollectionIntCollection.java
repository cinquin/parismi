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

import org.apache.commons.collections.primitives.IntCollection;

/**
 * Adapts a {@link java.lang.Number Number}-valued {@link java.util.Collection Collection} to the {@link IntCollection
 * IntCollection} interface.
 * <p />
 * This implementation delegates most methods to the provided {@link Collection Collection} implementation in the
 * "obvious" way.
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
final public class CollectionIntCollection extends AbstractCollectionIntCollection implements Serializable {
	/**
	  * 
	  */
	private static final long serialVersionUID = 1L;

	/**
	 * Create an {@link IntCollection IntCollection} wrapping
	 * the specified {@link Collection Collection}. When
	 * the given <i>collection</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param collection
	 *            the (possibly <code>null</code>) {@link Collection} to wrap
	 * @return an {@link IntCollection IntCollection} wrapping the given
	 *         <i>collection</i>, or <code>null</code> when <i>collection</i> is <code>null</code>.
	 */
	public static IntCollection wrap(Collection collection) {
		if (null == collection) {
			return null;
		} else if (collection instanceof Serializable) {
			return new CollectionIntCollection(collection);
		} else {
			return new NonSerializableCollectionIntCollection(collection);
		}
	}

	/**
	 * Creates an {@link IntCollection IntCollection} wrapping
	 * the specified {@link Collection Collection}.
	 * 
	 * @see #wrap
	 */
	public CollectionIntCollection(Collection collection) {
		_collection = collection;
	}

	@Override
	protected Collection getCollection() {
		return _collection;
	}

	private Collection _collection = null;
}
