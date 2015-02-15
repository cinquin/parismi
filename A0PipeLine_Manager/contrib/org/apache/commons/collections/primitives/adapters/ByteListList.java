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
package org.apache.commons.collections.primitives.adapters;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.collections.primitives.ByteList;

/**
 * Adapts an {@link ByteList ByteList} to the {@link List List} interface.
 * <p />
 * This implementation delegates most methods to the provided {@link ByteList ByteList} implementation in the "obvious"
 * way.
 *
 * @since Commons Primitives 1.0
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
final public class ByteListList extends AbstractByteListList implements Serializable {

	/**
	  * 
	  */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a {@link List List} wrapping
	 * the specified {@link ByteList ByteList}. When
	 * the given <i>list</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param list
	 *            the (possibly <code>null</code>) {@link ByteList ByteList} to wrap
	 * @return a {@link List List} wrapping the given
	 *         <i>list</i>, or <code>null</code> when <i>list</i> is <code>null</code>.
	 */
	public static List wrap(ByteList list) {
		if (null == list) {
			return null;
		} else if (list instanceof Serializable) {
			return new ByteListList(list);
		} else {
			return new NonSerializableByteListList(list);
		}
	}

	/**
	 * Creates a {@link List List} wrapping
	 * the specified {@link ByteList ByteList}.
	 * 
	 * @see #wrap
	 */
	public ByteListList(ByteList list) {
		_list = list;
	}

	@Override
	protected ByteList getByteList() {
		return _list;
	}

	private ByteList _list = null;
}
