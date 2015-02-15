/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import processing_utilities.skeleton.SkeletonPoint;

public class Point3D {
	public int x, y, z;

	public Point3D(SkeletonPoint p) {
		this.x = p.x;
		this.y = p.y;
		this.z = p.z;
	}

	public Point3D(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 71 * hash + this.x;
		hash = 71 * hash + this.y;
		hash = 71 * hash + this.z;
		return hash;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		final Point3D p = (Point3D) o;
		boolean result = (p.x == this.x) && (p.y == this.y) && (p.z == this.z);
		return result;
	}

	public double distanceTo(Point3D p) {
		return Math.sqrt((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) + (z - p.z) * (z - p.z));
	}
}
