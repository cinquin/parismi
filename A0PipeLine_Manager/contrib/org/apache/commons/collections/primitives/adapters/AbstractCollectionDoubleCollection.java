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

import org.apache.commons.collections.primitives.DoubleCollection;
import org.apache.commons.collections.primitives.DoubleIterator;

/**
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
abstract class AbstractCollectionDoubleCollection implements DoubleCollection {
	protected AbstractCollectionDoubleCollection() {
	}

	@Override
	public boolean add(double element) {
		return getCollection().add(new Double(element));
	}

	@Override
	public boolean addAll(DoubleCollection c) {
		return getCollection().addAll(DoubleCollectionCollection.wrap(c));
	}

	@Override
	public void clear() {
		getCollection().clear();
	}

	@Override
	public boolean contains(double element) {
		return getCollection().contains(new Double(element));
	}

	@Override
	public boolean containsAll(DoubleCollection c) {
		return getCollection().containsAll(DoubleCollectionCollection.wrap(c));
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
	 * {@link IteratorDoubleIterator#wrap wraps} the {@link java.util.Iterator Iterator} returned by my underlying
	 * {@link Collection Collection},
	 * if any.
	 */
	@Override
	public DoubleIterator iterator() {
		return IteratorDoubleIterator.wrap(getCollection().iterator());
	}

	@Override
	public boolean removeElement(double element) {
		return getCollection().remove(new Double(element));
	}

	@Override
	public boolean removeAll(DoubleCollection c) {
		return getCollection().removeAll(DoubleCollectionCollection.wrap(c));
	}

	@Override
	public boolean retainAll(DoubleCollection c) {
		return getCollection().retainAll(DoubleCollectionCollection.wrap(c));
	}

	@Override
	public int size() {
		return getCollection().size();
	}

	@Override
	public double[] toArray() {
		Object[] src = getCollection().toArray();
		double[] dest = new double[src.length];
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Number) (src[i])).doubleValue();
		}
		return dest;
	}

	@Override
	public double[] toArray(double[] dest) {
		Object[] src = getCollection().toArray();
		if (dest.length < src.length) {
			dest = new double[src.length];
		}
		for (int i = 0; i < src.length; i++) {
			dest[i] = ((Number) (src[i])).doubleValue();
		}
		return dest;
	}

	protected abstract Collection getCollection();

}
