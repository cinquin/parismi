/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.io.File;

import pipeline.FileNameIncrementable;
import pipeline.misc_util.FileNameUtils;

public class DirectoryParameter extends TextParameter implements FileNameIncrementable {
	private static final long serialVersionUID = 505709404483835075L;

	public DirectoryParameter(String name, String explanation, String initial_value, boolean editable,
			ParameterListener listener) {
		super(name, explanation, "".equals(initial_value) ? "~/" : "", editable, listener, null);
	}

	@Override
	public void incrementFileName() {
		String newName = FileNameUtils.incrementName(value);
		value = newName;
		fireValueChanged(false, true, false);
	}

	public boolean printValueAsString = true;

	@Override
	public Object getSimpleValue() {
		if (printValueAsString)
			return value;
		else
			return new File(value);
	}

	@Override
	public void prefixFileName(String prefix) {
		value = prefix + value;
		fireValueChanged(false, true, false);
	}

}
