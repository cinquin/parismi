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

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseProxyByteCollection implements ByteCollection {
	protected abstract ByteCollection getProxiedCollection();

	BaseProxyByteCollection() {
	}

	@Override
	public boolean add(byte element) {
		return getProxiedCollection().add(element);
	}

	@Override
	public boolean addAll(ByteCollection c) {
		return getProxiedCollection().addAll(c);
	}

	@Override
	public void clear() {
		getProxiedCollection().clear();
	}

	@Override
	public boolean contains(byte element) {
		return getProxiedCollection().contains(element);
	}

	@Override
	public boolean containsAll(ByteCollection c) {
		return getProxiedCollection().containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return getProxiedCollection().isEmpty();
	}

	@Override
	public ByteIterator iterator() {
		return getProxiedCollection().iterator();
	}

	@Override
	public boolean removeAll(ByteCollection c) {
		return getProxiedCollection().removeAll(c);
	}

	@Override
	public boolean removeElement(byte element) {
		return getProxiedCollection().removeElement(element);
	}

	@Override
	public boolean retainAll(ByteCollection c) {
		return getProxiedCollection().retainAll(c);
	}

	@Override
	public int size() {
		return getProxiedCollection().size();
	}

	@Override
	public byte[] toArray() {
		return getProxiedCollection().toArray();
	}

	@Override
	public byte[] toArray(byte[] a) {
		return getProxiedCollection().toArray(a);
	}

	// TODO: Add note about possible contract violations here.

	@Override
	public boolean equals(Object obj) {
		return getProxiedCollection().equals(obj);
	}

	@Override
	public int hashCode() {
		return getProxiedCollection().hashCode();
	}

	@Override
	public String toString() {
		return getProxiedCollection().toString();
	}

}
