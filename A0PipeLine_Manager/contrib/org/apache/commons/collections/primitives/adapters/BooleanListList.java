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

import org.apache.commons.collections.primitives.BooleanList;

/**
 * Adapts an {@link BooleanList BooleanList} to the {@link List List} interface.
 * <p />
 * This implementation delegates most methods to the provided {@link BooleanList BooleanList} implementation in the
 * "obvious" way.
 *
 * @since Commons Primitives 1.1
 * @version $Revision: 480462 $ $Date: 2006-11-29 00:15:00 -0800 (Wed, 29 Nov 2006) $
 * @author Rodney Waldhoff
 */
@SuppressWarnings("all")
final public class BooleanListList extends AbstractBooleanListList implements Serializable {

	/**
	  * 
	  */
	private static final long serialVersionUID = 1L;

	/**
	 * Create a {@link List List} wrapping
	 * the specified {@link BooleanList BooleanList}. When
	 * the given <i>list</i> is <code>null</code>,
	 * returns <code>null</code>.
	 * 
	 * @param list
	 *            the (possibly <code>null</code>) {@link BooleanList BooleanList} to wrap
	 * @return a {@link List List} wrapping the given
	 *         <i>list</i>, or <code>null</code> when <i>list</i> is <code>null</code>.
	 */
	public static List wrap(BooleanList list) {
		if (null == list) {
			return null;
		} else if (list instanceof Serializable) {
			return new BooleanListList(list);
		} else {
			return new NonSerializableBooleanListList(list);
		}
	}

	/**
	 * Creates a {@link List List} wrapping
	 * the specified {@link BooleanList BooleanList}.
	 * 
	 * @see #wrap
	 */
	public BooleanListList(BooleanList list) {
		_list = list;
	}

	@Override
	protected BooleanList getBooleanList() {
		return _list;
	}

	private BooleanList _list = null;
}
