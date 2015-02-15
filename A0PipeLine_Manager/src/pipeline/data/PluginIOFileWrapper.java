/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;

/**
 * Represent a file as a PluginIO.
 *
 */
public class PluginIOFileWrapper extends PluginIO {

	public PluginIOFileWrapper(File f) {
		file = f;
	}

	private static final long serialVersionUID = 1L;
	private File file;

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) {
		return file;
	}

}
