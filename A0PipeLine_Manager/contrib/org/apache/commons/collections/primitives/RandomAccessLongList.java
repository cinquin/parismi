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
package org.apache.commons.collections.primitives;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * Abstract base class for {@link LongList}s backed
 * by random access structures like arrays.
 * <p />
 * Read-only subclasses must override {@link #get} and {@link #size}. Mutable subclasses should also override
 * {@link #set}. Variably-sized subclasses should also override {@link #add(long)} and {@link #removeElementAt}. All
 * other methods have at least some base implementation derived from these. Subclasses may choose to override these
 * methods to provide a more efficient implementation.
 * 
 * @since Commons Primitives 0.1
 * @version $Revision: 480460 $ $Date: 2006-11-29 00:14:21 -0800 (Wed, 29 Nov 2006) $
 * 
 * @author Rodney Waldhoff
 */
public abstract class RandomAccessLongList extends AbstractLongCollection implements LongList {

	// constructors
	// -------------------------------------------------------------------------

	/** Constructs an empty list. */
	RandomAccessLongList() {
	}

	// fully abstract methods
	// -------------------------------------------------------------------------

	@Override
	public abstract long get(int index);

	@Override
	public abstract int size();

	// unsupported in base
	// -------------------------------------------------------------------------

	/**
	 * Unsupported in this implementation.
	 * 
	 * @throws UnsupportedOperationException
	 *             since this method is not supported
	 */
	@Override
	public long removeElementAt(int index) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported in this implementation.
	 * 
	 * @throws UnsupportedOperationException
	 *             since this method is not supported
	 */
	@Override
	public long set(int index, long element) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unsupported in this implementation.
	 * 
	 * @throws UnsupportedOperationException
	 *             since this method is not supported
	 */
	@Override
	public void add(int index, long element) {
		throw new UnsupportedOperationException();
	}

	// -------------------------------------------------------------------------

	// javadocs here are inherited

	@Override
	public boolean add(long element) {
		add(size(), element);
		return true;
	}

	@Override
	public boolean addAll(int index, LongCollection collection) {
		boolean modified = false;
		for (LongIterator iter = collection.iterator(); iter.hasNext();) {
			add(index++, iter.next());
			modified = true;
		}
		return modified;
	}

	@Override
	public int indexOf(long element) {
		int i = 0;
		for (LongIterator iter = iterator(); iter.hasNext();) {
			if (iter.next() == element) {
				return i;
			} else {
				i++;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(long element) {
		for (LongListIterator iter = listIterator(size()); iter.hasPrevious();) {
			if (iter.previous() == element) {
				return iter.nextIndex();
			}
		}
		return -1;
	}

	@Override
	public LongIterator iterator() {
		return listIterator();
	}

	@Override
	public LongListIterator listIterator() {
		return listIterator(0);
	}

	@Override
	public LongListIterator listIterator(int index) {
		return new RandomAccessLongListIterator(this, index);
	}

	@Override
	public LongList subList(int fromIndex, int toIndex) {
		return new RandomAccessLongSubList(this, fromIndex, toIndex);
	}

	@Override
	public boolean equals(Object that) {
		if (this == that) {
			return true;
		} else if (that instanceof LongList) {
			LongList thatList = (LongList) that;
			if (size() != thatList.size()) {
				return false;
			}
			for (LongIterator thatIter = thatList.iterator(), thisIter = iterator(); thisIter.hasNext();) {
				if (thisIter.next() != thatIter.next()) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = 1;
		for (LongIterator iter = iterator(); iter.hasNext();) {
			long val = iter.next();
			hash = 31 * hash + ((int) (val ^ (val >>> 32)));
		}
		return hash;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		for (LongIterator iter = iterator(); iter.hasNext();) {
			buf.append(iter.next());
			if (iter.hasNext()) {
				buf.append(", ");
			}
		}
		buf.append("]");
		return buf.toString();
	}

	// protected utilities
	// -------------------------------------------------------------------------

	/** Get my count of structural modifications. */
	int getModCount() {
		return _modCount;
	}

	/** Increment my count of structural modifications. */
	void incrModCount() {
		_modCount++;
	}

	// attributes
	// -------------------------------------------------------------------------

	private int _modCount = 0;

	// inner classes
	// -------------------------------------------------------------------------

	private static class ComodChecker {
		ComodChecker(RandomAccessLongList source) {
			_source = source;
			resyncModCount();
		}

		RandomAccessLongList getList() {
			return _source;
		}

		void assertNotComodified() throws ConcurrentModificationException {
			if (_expectedModCount != getList().getModCount()) {
				throw new ConcurrentModificationException();
			}
		}

		void resyncModCount() {
			_expectedModCount = getList().getModCount();
		}

		private RandomAccessLongList _source = null;
		private int _expectedModCount = -1;
	}

	protected static class RandomAccessLongListIterator extends ComodChecker implements LongListIterator {
		RandomAccessLongListIterator(RandomAccessLongList list, int index) {
			super(list);
			if (index < 0 || index > getList().size()) {
				throw new IndexOutOfBoundsException("Index " + index + " not in [0," + getList().size() + ")");
			} else {
				_nextIndex = index;
				resyncModCount();
			}
		}

		@Override
		public boolean hasNext() {
			assertNotComodified();
			return _nextIndex < getList().size();
		}

		@Override
		public boolean hasPrevious() {
			assertNotComodified();
			return _nextIndex > 0;
		}

		@Override
		public int nextIndex() {
			assertNotComodified();
			return _nextIndex;
		}

		@Override
		public int previousIndex() {
			assertNotComodified();
			return _nextIndex - 1;
		}

		@Override
		public long next() {
			assertNotComodified();
			if (!hasNext()) {
				throw new NoSuchElementException();
			} else {
				long val = getList().get(_nextIndex);
				_lastReturnedIndex = _nextIndex;
				_nextIndex++;
				return val;
			}
		}

		@Override
		public long previous() {
			assertNotComodified();
			if (!hasPrevious()) {
				throw new NoSuchElementException();
			} else {
				long val = getList().get(_nextIndex - 1);
				_lastReturnedIndex = _nextIndex - 1;
				_nextIndex--;
				return val;
			}
		}

		@Override
		public void add(long value) {
			assertNotComodified();
			getList().add(_nextIndex, value);
			_nextIndex++;
			_lastReturnedIndex = -1;
			resyncModCount();
		}

		@Override
		public void remove() {
			assertNotComodified();
			if (_lastReturnedIndex == -1) {
				throw new IllegalStateException();
			}
			if (_lastReturnedIndex == _nextIndex) {
				// remove() following previous()
				getList().removeElementAt(_lastReturnedIndex);
			} else {
				// remove() following next()
				getList().removeElementAt(_lastReturnedIndex);
				_nextIndex--;
			}
			_lastReturnedIndex = -1;
			resyncModCount();
		}

		@Override
		public void set(long value) {
			assertNotComodified();
			if (-1 == _lastReturnedIndex) {
				throw new IllegalStateException();
			} else {
				getList().set(_lastReturnedIndex, value);
				resyncModCount();
			}
		}

		private int _nextIndex = 0;
		private int _lastReturnedIndex = -1;
	}

	protected static class RandomAccessLongSubList extends RandomAccessLongList implements LongList {
		RandomAccessLongSubList(RandomAccessLongList list, int fromIndex, int toIndex) {
			if (fromIndex < 0 || toIndex > list.size()) {
				throw new IndexOutOfBoundsException();
			} else if (fromIndex > toIndex) {
				throw new IllegalArgumentException();
			} else {
				_list = list;
				_offset = fromIndex;
				_limit = toIndex - fromIndex;
				_comod = new ComodChecker(list);
				_comod.resyncModCount();
			}
		}

		@Override
		public long get(int index) {
			checkRange(index);
			_comod.assertNotComodified();
			return _list.get(toUnderlyingIndex(index));
		}

		@Override
		public long removeElementAt(int index) {
			checkRange(index);
			_comod.assertNotComodified();
			long val = _list.removeElementAt(toUnderlyingIndex(index));
			_limit--;
			_comod.resyncModCount();
			incrModCount();
			return val;
		}

		@Override
		public long set(int index, long element) {
			checkRange(index);
			_comod.assertNotComodified();
			long val = _list.set(toUnderlyingIndex(index), element);
			incrModCount();
			_comod.resyncModCount();
			return val;
		}

		@Override
		public void add(int index, long element) {
			checkRangeIncludingEndpoint(index);
			_comod.assertNotComodified();
			_list.add(toUnderlyingIndex(index), element);
			_limit++;
			_comod.resyncModCount();
			incrModCount();
		}

		@Override
		public int size() {
			_comod.assertNotComodified();
			return _limit;
		}

		private void checkRange(int index) {
			if (index < 0 || index >= size()) {
				throw new IndexOutOfBoundsException("index " + index + " not in [0," + size() + ")");
			}
		}

		private void checkRangeIncludingEndpoint(int index) {
			if (index < 0 || index > size()) {
				throw new IndexOutOfBoundsException("index " + index + " not in [0," + size() + "]");
			}
		}

		private int toUnderlyingIndex(int index) {
			return (index + _offset);
		}

		private int _offset = 0;
		private int _limit = 0;
		private RandomAccessLongList _list = null;
		private ComodChecker _comod = null;

	}
}
