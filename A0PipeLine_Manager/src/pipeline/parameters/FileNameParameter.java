/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import pipeline.FileNameIncrementable;
import pipeline.misc_util.FileNameUtils;

public class FileNameParameter extends TextParameter implements FileNameIncrementable {
	private static final long serialVersionUID = 154577261571761715L;

	@Override
	public void incrementFileName() {
		String newValue = FileNameUtils.incrementName(value);
		value = newValue;
		fireValueChanged(false, true, false);
	}

	public FileNameParameter(String name, String explanation, String initial_value, boolean editable,
			ParameterListener listener) {
		super(name, explanation, initial_value, editable, listener, null);
	}

	@Override
	public void prefixFileName(String prefix) {
		value = prefix + value;
		fireValueChanged(false, true, false);
	}

}
