/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline;

/**
 * Interface implemented by objects that want to increment a file name when the user presses the "Open next" button.
 * Parameters containing file names implement this.
 *
 */

public interface FileNameIncrementable {

	public void incrementFileName();

	public void prefixFileName(String prefix);
}
