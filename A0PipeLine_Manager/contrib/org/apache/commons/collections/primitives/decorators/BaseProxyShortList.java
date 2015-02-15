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

import org.apache.commons.collections.primitives.ShortCollection;
import org.apache.commons.collections.primitives.ShortList;
import org.apache.commons.collections.primitives.ShortListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseProxyShortList extends BaseProxyShortCollection implements ShortList {
	protected abstract ShortList getProxiedList();

	@Override
	protected final ShortCollection getProxiedCollection() {
		return getProxiedList();
	}

	BaseProxyShortList() {
	}

	@Override
	public void add(int index, short element) {
		getProxiedList().add(index, element);
	}

	@Override
	public boolean addAll(int index, ShortCollection collection) {
		return getProxiedList().addAll(index, collection);
	}

	@Override
	public short get(int index) {
		return getProxiedList().get(index);
	}

	@Override
	public int indexOf(short element) {
		return getProxiedList().indexOf(element);
	}

	@Override
	public int lastIndexOf(short element) {
		return getProxiedList().lastIndexOf(element);
	}

	@Override
	public ShortListIterator listIterator() {
		return getProxiedList().listIterator();
	}

	@Override
	public ShortListIterator listIterator(int index) {
		return getProxiedList().listIterator(index);
	}

	@Override
	public short removeElementAt(int index) {
		return getProxiedList().removeElementAt(index);
	}

	@Override
	public short set(int index, short element) {
		return getProxiedList().set(index, element);
	}

	@Override
	public ShortList subList(int fromIndex, int toIndex) {
		return getProxiedList().subList(fromIndex, toIndex);
	}

}
