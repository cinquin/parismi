/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;

/**
 * Used to encapsulate binary data of unknown type to pass to plugins (such as external plugins) that need it.
 *
 */
public class PluginIOUnknownBinary extends PluginIO {

	private static final long serialVersionUID = 1L;

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void restoreFromProtobuf() throws NotRestorableFromProtobuf {
		// Don't do anything because we don't know what the contents are.
		// We need to override the supertype method because if not it will throw an exception.
	}

	@Override
	public byte[] asProtobufBytes() {
		return getProtobuf();
	}
}
