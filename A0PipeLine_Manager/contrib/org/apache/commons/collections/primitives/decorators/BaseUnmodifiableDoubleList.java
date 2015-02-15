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
package org.apache.commons.collections.primitives.decorators;

import org.apache.commons.collections.primitives.DoubleCollection;
import org.apache.commons.collections.primitives.DoubleIterator;
import org.apache.commons.collections.primitives.DoubleList;
import org.apache.commons.collections.primitives.DoubleListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseUnmodifiableDoubleList extends BaseProxyDoubleList {

	@Override
	public final void add(int index, double element) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final boolean addAll(int index, DoubleCollection collection) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final double removeElementAt(int index) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final double set(int index, double element) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final boolean add(double element) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final boolean addAll(DoubleCollection c) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final void clear() {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final boolean removeAll(DoubleCollection c) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final boolean removeElement(double element) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final boolean retainAll(DoubleCollection c) {
		throw new UnsupportedOperationException("This DoubleList is not modifiable.");
	}

	@Override
	public final DoubleList subList(int fromIndex, int toIndex) {
		return UnmodifiableDoubleList.wrap(getProxiedList().subList(fromIndex, toIndex));
	}

	@Override
	public final DoubleIterator iterator() {
		return UnmodifiableDoubleIterator.wrap(getProxiedList().iterator());
	}

	@Override
	public DoubleListIterator listIterator() {
		return UnmodifiableDoubleListIterator.wrap(getProxiedList().listIterator());
	}

	@Override
	public DoubleListIterator listIterator(int index) {
		return UnmodifiableDoubleListIterator.wrap(getProxiedList().listIterator(index));
	}

}
