/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline;

/**
 * Provides a function that helps a row determine how it should update its relative and absolute references to other
 * rows
 * when the table is updated by row insertion, deletion, or moving.
 */

public interface RowTransform {

	public int computeNewReference(boolean isAbsolute, int myRowPosition, int currentReference);

}
