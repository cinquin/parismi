/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

public class FileAndDirectoryNameParameter extends SplitParameter {

	private static final long serialVersionUID = -8884738469215443949L;

	public FileAndDirectoryNameParameter(Object[] objects) {
		super(objects);
	}

	@Override
	public Object getSimpleValue() {
		return (String) ((AbstractParameter) parameters[0]).getSimpleValue()
				+ (String) ((AbstractParameter) parameters[1]).getSimpleValue();
	}
}
