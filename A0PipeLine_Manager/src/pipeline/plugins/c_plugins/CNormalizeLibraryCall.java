/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import pipeline.plugins.ExternalCallToLibrary;

public class CNormalizeLibraryCall extends ExternalCallToLibrary {

	@Override
	public String operationName() {
		return "C helper for normalizing";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String getCommandName() {
		return "normalize";
	}

}
