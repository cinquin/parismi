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
import org.apache.commons.collections.primitives.IntList;
import org.apache.commons.collections.primitives.IntListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseProxyIntList extends BaseProxyIntCollection implements IntList {
	protected abstract IntList getProxiedList();

	@Override
	protected final IntCollection getProxiedCollection() {
		return getProxiedList();
	}

	BaseProxyIntList() {
	}

	@Override
	public void add(int index, int element) {
		getProxiedList().add(index, element);
	}

	@Override
	public boolean addAll(int index, IntCollection collection) {
		return getProxiedList().addAll(index, collection);
	}

	@Override
	public int get(int index) {
		return getProxiedList().get(index);
	}

	@Override
	public int indexOf(int element) {
		return getProxiedList().indexOf(element);
	}

	@Override
	public int lastIndexOf(int element) {
		return getProxiedList().lastIndexOf(element);
	}

	@Override
	public IntListIterator listIterator() {
		return getProxiedList().listIterator();
	}

	@Override
	public IntListIterator listIterator(int index) {
		return getProxiedList().listIterator(index);
	}

	@Override
	public int removeElementAt(int index) {
		return getProxiedList().removeElementAt(index);
	}

	@Override
	public int set(int index, int element) {
		return getProxiedList().set(index, element);
	}

	@Override
	public IntList subList(int fromIndex, int toIndex) {
		return getProxiedList().subList(fromIndex, toIndex);
	}

}
