/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline;

/**
 * Interface implemented by objects that hold references to other rows in the table.
 * When rows are moved, inserted, or deleted, those references need to be updated.
 *
 */

public interface RowReferenceHolder {

	public void offsetReference(int yourRow, RowTransform transform);

}
