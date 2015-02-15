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
 * Adapts an {@link IntCollection IntCollection} to the {@link java.util.Collection Collection} interface.
 * <p />
 * This implementation delegates most methods to the provided {@link IntCollection IntCollection} implementation in the
 * "obvious" way.
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
final public class IntCollectionCollection extends AbstractIntCollectionCollection implements Serializable {

	/**
	  * 
	  */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a {@link Collection Collection} wrapping
	 * the specified {@link IntCollection IntCollection}. When
	 * the given <i>collection</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param collection
	 *            the (possibly <code>null</code>) {@link IntCollection IntCollection} to wrap
	 * @return a {@link Collection Collection} wrapping the given
	 *         <i>collection</i>, or <code>null</code> when <i>collection</i> is <code>null</code>.
	 */
	public static Collection wrap(IntCollection collection) {
		if (null == collection) {
			return null;
		} else if (collection instanceof Serializable) {
			return new IntCollectionCollection(collection);
		} else {
			return new NonSerializableIntCollectionCollection(collection);
		}
	}

	/**
	 * Creates a {@link Collection Collection} wrapping
	 * the specified {@link IntCollection IntCollection}.
	 * 
	 * @see #wrap
	 */
	public IntCollectionCollection(IntCollection collection) {
		_collection = collection;
	}

	@Override
	protected IntCollection getIntCollection() {
		return _collection;
	}

	private IntCollection _collection = null;
}
