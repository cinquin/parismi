/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;

/**
 * Will be used later to store the input the user provides by clicking in PluginIOImages.
 * Not used for now.
 *
 */
public class PluginIOUserClicks extends PluginIO implements PluginUserInput {
	private static final long serialVersionUID = 1L;

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) {
		throw new RuntimeException("Unimplemented");
	}

}
