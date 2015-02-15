/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.io.File;

public class FakeFileForPipelineRef extends File {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Object reference;

	public FakeFileForPipelineRef(String pathname, Object reference) {
		super(pathname);
		this.reference = reference;
	}

}
