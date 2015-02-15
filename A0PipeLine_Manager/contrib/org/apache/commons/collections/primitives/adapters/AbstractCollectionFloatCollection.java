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

import java.util.Collection;

import org.apache.commons.collections.primitives.FloatCollection;
import org.apache.commons.collections.primitives.FloatIterator;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractCollectionFloatCollection implements FloatCollection {
	protected AbstractCollectionFloatCollection() {
	}

	@Override
	public boolean add(float element) {
		return getCollection().add(new Float(element));
	}

	@Override
	public boolean addAll(FloatCollection c) {
		return getCollection().addAll(FloatCollectionCollection.wrap(c));
	}

	@Override
	public void clear() {
		getCollection().clear();
	}

	@Override
	public boolean contains(float element) {
		return getCollection().contains(new Float(element));
	}

	@Override
	public boolean containsAll(FloatCollection c) {
		return getCollection().containsAll(FloatCollectionCollection.wrap(c));
	}

	@Override
	public String toString() {
		return getCollection().toString();
	}

	@Override
	public boolean isEmpty() {
		return getCollection().isEmpty();
	}

	/**
	 * {@link IteratorFloatIterator#wrap wraps} the {@link java.util.Iterator Iterator} returned by my underlying
	 * {@link Collection Collection},
	 * if any.
	 */
	@Override
	public FloatIterator iterator() {
		return IteratorFloatIterator.wrap(getCollection().iterator());
	}

	@Override
	public boolean removeElement(float element) {
		return getCollection().remove(new Float(element));
	}

	@Override
	public boolean removeAll(FloatCollection c) {
		return getCollection().removeAll(FloatCollectionCollection.wrap(c));
	}

	@Override
	public boolean retainAll(FloatCollection c) {
		return getCollection().retainAll(FloatCollectionCollection.wrap(c));
	}

	@Override
	public int size() {
		return getCollection().size();
	}

	@Override
	public float[] toArray() {
		Object[] src = getCollection().toArray();
		float[] dest = new float[src.length];
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Number) (src[i])).floatValue();
		}
		return dest;
	}

	@Override
	public float[] toArray(float[] dest) {
		Object[] src = getCollection().toArray();
		if (dest.length < src.length) {
			dest = new float[src.length];
		}
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Number) (src[i])).floatValue();
		}
		return dest;
	}

	protected abstract Collection getCollection();

}
