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

import org.apache.commons.collections.primitives.LongListIterator;

/**
 * 
 * @since Commons Primitives 1.0
 * @version $Revision: 480463 $ $Date: 2006-11-29 00:15:23 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
public final class UnmodifiableLongListIterator extends ProxyLongListIterator {
	private UnmodifiableLongListIterator(LongListIterator iterator) {
		this.proxied = iterator;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This LongListIterator is not modifiable.");
	}

	@Override
	public void add(long value) {
		throw new UnsupportedOperationException("This LongListIterator is not modifiable.");
	}

	@Override
	public void set(long value) {
		throw new UnsupportedOperationException("This LongListIterator is not modifiable.");
	}

	@Override
	protected LongListIterator getListIterator() {
		return proxied;
	}

	public static LongListIterator wrap(LongListIterator iterator) {
		if (null == iterator) {
			return null;
		} else if (iterator instanceof UnmodifiableLongListIterator) {
			return iterator;
		} else {
			return new UnmodifiableLongListIterator(iterator);
		}
	}

	private LongListIterator proxied = null;
}
