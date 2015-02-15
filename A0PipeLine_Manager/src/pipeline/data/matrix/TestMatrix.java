/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data.matrix;

import java.util.Iterator;

import pipeline.data.matrix.recursive_iterator.AbstractLongMatrix;
import pipeline.data.matrix.recursive_iterator.LongMatrix;
import pipeline.data.matrix.recursive_iterator.LongMeanAggregator;

public class TestMatrix {

	private static long[][] reference;
	private static final int[] sizes = new int[] { 10, 10, 10 };
	private static final Object[] axes = new Object[] { "z", "y", "x" };

	public static final void main(String[] args) {
		recursiveLongTest();
	}

	@SuppressWarnings("unused")
	private static void referenceLongTest() {
		populateReference();

		int numTrials = 100;
		boolean sumPrinted = false;
		for (int i = 0; i < numTrials; i++) {
			long sum = iterateReference();
			if (!sumPrinted) {
				sumPrinted = true;
				System.out.println(sum);
			}
		}
	}

	static final void recursiveLongTest() {
		LongMatrix m = new LongMatrix(sizes, axes, 2);

		populateLongMatrix(m);

		int numTrials = 0;
		boolean sumPrinted = false;
		for (int i = 0; i < numTrials; i++) {
			long sum = getLongIterator(m);
			if (!sumPrinted) {
				sumPrinted = true;
				System.out.println(sum);
			}
		}

		projectMatrix(m);
	}

	public static final long getLongIterator(ILongMatrix m) {
		long sum = 0;
		Iterator<ILongMatrix> it = m.getMatrixIterator(new Object[] { "y", "x" });
		while (it.hasNext()) {
			ILongMatrix sub = it.next();
			ILongIterator iterator = sub.getIterator();
			while (iterator.hasNext()) {
				sum += iterator.next();
			}
		}
		return sum;
	}

	public static final void populateLongMatrix(ILongMatrix m) {
		long value = 0;
		Iterator<ILongMatrix> it = m.getMatrixIterator(new Object[] { "y", "x" });
		int[] coordinates = new int[2];
		while (it.hasNext()) {
			ILongMatrix sub = it.next();
			for (int i = 0; i < sizes[sizes.length - 2]; i++) {
				coordinates[0] = i;
				for (int j = 0; j < sizes[sizes.length - 1]; j++) {
					coordinates[1] = j;
					sub.put(coordinates, value++);
				}
			}
		}
	}

	public static final void populateReference() {
		int product = 1;
		for (int i = 1; i < sizes.length; i++)
			product *= sizes[i];
		reference = new long[sizes[0]][product];
		long value = 1;
		for (int i = 0; i < reference.length; i++) {
			for (int j = 0; j < reference[i].length; j++) {
				reference[i][j] = value++;
			}
		}
	}

	public static final long iterateReference() {
		long sum = 0;
		for (int i = 0; i < reference.length; i++) {
			for (int j = 0; j < reference[i].length; j++) {
				sum += reference[i][j];
			}
		}
		return sum;
	}

	public static final void projectMatrix(AbstractLongMatrix m) {
		AbstractLongMatrix l = m.project(new Object[] { "z", "y" }, LongMeanAggregator.class);
		System.out.println(l.toString());
	}
}
