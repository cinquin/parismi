package processing_utilities.skeleton;

import processing_utilities.floyd_warshall.IFWNode;

/**
 * AnalyzeSkeleton_ plugin for ImageJ(C).
 * Copyright (C) 2008,2009 Ignacio Arganda-Carreras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * 
 */
/**
 * This class represents a 3D point or position on a 3D image. Therefore, the
 * coordinates are integer.
 */
public class SkeletonPoint implements IFWNode {

	/** x- coordinate */
	public int x = 0;
	/** y- coordinate */
	public int y = 0;
	/** z- coordinate */
	public int z = 0;

	/**
	 * Create point from integer coordinates.
	 * 
	 * @param x
	 *            x- coordinate
	 * @param y
	 *            y- coordinate
	 * @param z
	 *            z- coordinate
	 */
	public SkeletonPoint(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public SkeletonPoint(SkeletonPoint p) {
		x = p.x;
		y = p.y;
		z = p.z;
	}

	/**
	 * Convert point to string.
	 */
	@Override
	public String toString() {
		return new String("(" + this.x + ", " + this.y + ", " + this.z + ")");
	}

	/**
	 * Override equals method to compare points.
	 * 
	 * @param o
	 *            input object
	 * @return true if the input object is equal to this SkeletonPoint
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		final SkeletonPoint p = (SkeletonPoint) o;
		boolean result = p.x == this.x && p.y == this.y && p.z == this.z;
		if (result && this.index != p.index) {
			throw new IllegalArgumentException("Comparing two identical points with difference indices");
		}
		return result;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 71 * hash + this.x;
		hash = 71 * hash + this.y;
		hash = 71 * hash + this.z;
		return hash;
	}

	public int index = -1;

	@Override
	public int getIndex() {
		return index;
	}

	public int owner = 0;

	public double distanceTo(SkeletonPoint p) {
		return Math.sqrt((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) + (z - p.z) * (z - p.z));
	}

}// end class point
