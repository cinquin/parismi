/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;

public class PluginIOString extends PluginIO {

	private static final long serialVersionUID = 1L;

	public PluginIOString(String s) {
		string = s;
	}

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	private String string;

	public String getString() {
		return string;
	}

	public void setString(String s) {
		string = s;
	}

	@Override
	public String toString() {
		return string;
	}
}
