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

import org.apache.commons.collections.primitives.IntCollection;
import org.apache.commons.collections.primitives.IntIterator;
import org.apache.commons.collections.primitives.IntList;
import org.apache.commons.collections.primitives.IntListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseUnmodifiableIntList extends BaseProxyIntList {

	@Override
	public final void add(int index, int element) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final boolean addAll(int index, IntCollection collection) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final int removeElementAt(int index) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final int set(int index, int element) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final boolean add(int element) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final boolean addAll(IntCollection c) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final void clear() {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final boolean removeAll(IntCollection c) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final boolean removeElement(int element) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final boolean retainAll(IntCollection c) {
		throw new UnsupportedOperationException("This IntList is not modifiable.");
	}

	@Override
	public final IntList subList(int fromIndex, int toIndex) {
		return UnmodifiableIntList.wrap(getProxiedList().subList(fromIndex, toIndex));
	}

	@Override
	public final IntIterator intIterator() {
		return UnmodifiableIntIterator.wrap(getProxiedList().intIterator());
	}

	@Override
	public IntListIterator listIterator() {
		return UnmodifiableIntListIterator.wrap(getProxiedList().listIterator());
	}

	@Override
	public IntListIterator listIterator(int index) {
		return UnmodifiableIntListIterator.wrap(getProxiedList().listIterator(index));
	}

}
