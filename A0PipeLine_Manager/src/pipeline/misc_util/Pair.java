/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.lang.reflect.Array;
import java.util.Collection;

// From http://stackoverflow.com/questions/156275/what-is-the-equivalent-of-the-c-pairl-r-in-java
public class Pair<A, B> {
	public A fst;
	public B snd;

	public Pair(A first, B second) {
		super();
		this.fst = first;
		this.snd = second;
	}

	@Override
	public int hashCode() {
		int hashFirst = fst != null ? fst.hashCode() : 0;
		int hashSecond = snd != null ? snd.hashCode() : 0;

		return (hashFirst + hashSecond) * hashSecond + hashFirst;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Pair) {
			Pair<?, ?> otherPair = (Pair<?, ?>) other;
			return ((this.fst == otherPair.fst || (this.fst != null && otherPair.fst != null && this.fst
					.equals(otherPair.fst))) && (this.snd == otherPair.snd || (this.snd != null
					&& otherPair.snd != null && this.snd.equals(otherPair.snd))));
		}

		return false;
	}

	@Override
	public String toString() {
		return "(" + fst + ", " + snd + ")";
	}

	public A getFst() {
		return fst;
	}

	public void setFst(A first) {
		this.fst = first;
	}

	public B getSnd() {
		return snd;
	}

	public void setSnd(B second) {
		this.snd = second;
	}

	public A[] firstAsArray(Collection<Pair<A, B>> col, Class<?> clazz) {
		@SuppressWarnings("unchecked")
		A[] result = (A[]) Array.newInstance(clazz, col.size());
		int index = 0;
		for (Pair<A, B> pair : col) {
			result[index] = pair.getFst();
			index++;
		}
		return result;
	}

	public B[] secondAsArray(Collection<Pair<A, B>> col, Class<?> clazz) {
		@SuppressWarnings("unchecked")
		B[] result = (B[]) Array.newInstance(clazz, col.size());
		int index = 0;
		for (Pair<A, B> pair : col) {
			result[index] = pair.getSnd();
			index++;
		}
		return result;
	}
}
