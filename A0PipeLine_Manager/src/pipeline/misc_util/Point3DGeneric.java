/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

/**
 * Parameterized class to store 3D points who coordinates can be of any type (integer, float, etc).
 *
 * @param <TypeOfCoordinate>
 */
class Point3DGeneric<TypeOfCoordinate> {
	public TypeOfCoordinate x, y, z;

}
