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

import org.apache.commons.collections.primitives.CharCollection;
import org.apache.commons.collections.primitives.CharIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
abstract class BaseProxyCharCollection implements CharCollection {
	protected abstract CharCollection getProxiedCollection();

	BaseProxyCharCollection() {
	}

	@Override
	public boolean add(char element) {
		return getProxiedCollection().add(element);
	}

	@Override
	public boolean addAll(CharCollection c) {
		return getProxiedCollection().addAll(c);
	}

	@Override
	public void clear() {
		getProxiedCollection().clear();
	}

	@Override
	public boolean contains(char element) {
		return getProxiedCollection().contains(element);
	}

	@Override
	public boolean containsAll(CharCollection c) {
		return getProxiedCollection().containsAll(c);
	}

	@Override
	public boolean isEmpty() {
		return getProxiedCollection().isEmpty();
	}

	@Override
	public CharIterator iterator() {
		return getProxiedCollection().iterator();
	}

	@Override
	public boolean removeAll(CharCollection c) {
		return getProxiedCollection().removeAll(c);
	}

	@Override
	public boolean removeElement(char element) {
		return getProxiedCollection().removeElement(element);
	}

	@Override
	public boolean retainAll(CharCollection c) {
		return getProxiedCollection().retainAll(c);
	}

	@Override
	public int size() {
		return getProxiedCollection().size();
	}

	@Override
	public char[] toArray() {
		return getProxiedCollection().toArray();
	}

	@Override
	public char[] toArray(char[] a) {
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
