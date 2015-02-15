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
import org.apache.commons.collections.primitives.ByteList;
import org.apache.commons.collections.primitives.ByteListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseProxyByteList extends BaseProxyByteCollection implements ByteList {
	protected abstract ByteList getProxiedList();

	@Override
	protected final ByteCollection getProxiedCollection() {
		return getProxiedList();
	}

	BaseProxyByteList() {
	}

	@Override
	public void add(int index, byte element) {
		getProxiedList().add(index, element);
	}

	@Override
	public boolean addAll(int index, ByteCollection collection) {
		return getProxiedList().addAll(index, collection);
	}

	@Override
	public byte get(int index) {
		return getProxiedList().get(index);
	}

	@Override
	public int indexOf(byte element) {
		return getProxiedList().indexOf(element);
	}

	@Override
	public int lastIndexOf(byte element) {
		return getProxiedList().lastIndexOf(element);
	}

	@Override
	public ByteListIterator listIterator() {
		return getProxiedList().listIterator();
	}

	@Override
	public ByteListIterator listIterator(int index) {
		return getProxiedList().listIterator(index);
	}

	@Override
	public byte removeElementAt(int index) {
		return getProxiedList().removeElementAt(index);
	}

	@Override
	public byte set(int index, byte element) {
		return getProxiedList().set(index, element);
	}

	@Override
	public ByteList subList(int fromIndex, int toIndex) {
		return getProxiedList().subList(fromIndex, toIndex);
	}

}
