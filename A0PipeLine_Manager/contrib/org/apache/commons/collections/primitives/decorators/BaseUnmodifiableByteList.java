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

import org.apache.commons.collections.primitives.ByteCollection;
import org.apache.commons.collections.primitives.ByteIterator;
import org.apache.commons.collections.primitives.ByteList;
import org.apache.commons.collections.primitives.ByteListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseUnmodifiableByteList extends BaseProxyByteList {

	@Override
	public final void add(int index, byte element) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final boolean addAll(int index, ByteCollection collection) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final byte removeElementAt(int index) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final byte set(int index, byte element) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final boolean add(byte element) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final boolean addAll(ByteCollection c) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final void clear() {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final boolean removeAll(ByteCollection c) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final boolean removeElement(byte element) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final boolean retainAll(ByteCollection c) {
		throw new UnsupportedOperationException("This ByteList is not modifiable.");
	}

	@Override
	public final ByteList subList(int fromIndex, int toIndex) {
		return UnmodifiableByteList.wrap(getProxiedList().subList(fromIndex, toIndex));
	}

	@Override
	public final ByteIterator iterator() {
		return UnmodifiableByteIterator.wrap(getProxiedList().iterator());
	}

	@Override
	public ByteListIterator listIterator() {
		return UnmodifiableByteListIterator.wrap(getProxiedList().listIterator());
	}

	@Override
	public ByteListIterator listIterator(int index) {
		return UnmodifiableByteListIterator.wrap(getProxiedList().listIterator(index));
	}

}
