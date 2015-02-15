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

import org.apache.commons.collections.primitives.LongCollection;
import org.apache.commons.collections.primitives.LongList;
import org.apache.commons.collections.primitives.LongListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseProxyLongList extends BaseProxyLongCollection implements LongList {
	protected abstract LongList getProxiedList();

	@Override
	protected final LongCollection getProxiedCollection() {
		return getProxiedList();
	}

	BaseProxyLongList() {
	}

	@Override
	public void add(int index, long element) {
		getProxiedList().add(index, element);
	}

	@Override
	public boolean addAll(int index, LongCollection collection) {
		return getProxiedList().addAll(index, collection);
	}

	@Override
	public long get(int index) {
		return getProxiedList().get(index);
	}

	@Override
	public int indexOf(long element) {
		return getProxiedList().indexOf(element);
	}

	@Override
	public int lastIndexOf(long element) {
		return getProxiedList().lastIndexOf(element);
	}

	@Override
	public LongListIterator listIterator() {
		return getProxiedList().listIterator();
	}

	@Override
	public LongListIterator listIterator(int index) {
		return getProxiedList().listIterator(index);
	}

	@Override
	public long removeElementAt(int index) {
		return getProxiedList().removeElementAt(index);
	}

	@Override
	public long set(int index, long element) {
		return getProxiedList().set(index, element);
	}

	@Override
	public LongList subList(int fromIndex, int toIndex) {
		return getProxiedList().subList(fromIndex, toIndex);
	}

}
