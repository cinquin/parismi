/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.util.ArrayList;
import java.util.List;

/**
 * Parameterized class to store an arbitrarily-long list of ranges whose coordinates can be of any type (points in 3D
 * space
 * whose coordinates can be integer, float, etc).
 *
 * @param <TypeOfPosition>
 */
public class RangesOfPositions<TypeOfPosition> {
	private class pairOfPositions {
		public pairOfPositions(TypeOfPosition start, TypeOfPosition end) {
			this.start = start;
			this.end = end;
		}

		@SuppressWarnings("unused")
		public TypeOfPosition start, end;
	}

	private List<pairOfPositions> listOfRanges;

	public RangesOfPositions() {
		listOfRanges = new ArrayList<>();
	}

	public void clear() {
		listOfRanges.clear();
	}

	public void addRange(TypeOfPosition start, TypeOfPosition end) {
		listOfRanges.add(new pairOfPositions(start, end));
	}

	public void removeRange(TypeOfPosition start, TypeOfPosition end) {
		// TODO Not yet implemented
	}

}
