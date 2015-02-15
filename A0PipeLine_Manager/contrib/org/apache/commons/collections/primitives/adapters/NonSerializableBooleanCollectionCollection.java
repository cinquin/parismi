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

import org.apache.commons.collections.primitives.BooleanCollection;

/**
 * @since Commons Primitives 1.1
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 */
final class NonSerializableBooleanCollectionCollection extends AbstractBooleanCollectionCollection {

	/**
	 * Creates a {@link java.util.Collection Collection} wrapping the specified
	 * {@link org.apache.commons.collections.primitives.BooleanCollection
	 * BooleanCollection}.
	 */
	public NonSerializableBooleanCollectionCollection(BooleanCollection collection) {
		_collection = collection;
	}

	@Override
	protected BooleanCollection getBooleanCollection() {
		return _collection;
	}

	private BooleanCollection _collection = null;
}
