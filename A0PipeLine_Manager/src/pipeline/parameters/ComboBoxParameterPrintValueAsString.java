/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

public class ComboBoxParameterPrintValueAsString extends ComboBoxParameter {
	private static final long serialVersionUID = 667763764489392934L;

	public ComboBoxParameterPrintValueAsString(String name, String explanation, String[] choices, String initialChoice,
			boolean editable, ParameterListener listener) {
		super(name, explanation, choices, initialChoice, editable, listener);
	}

	@Override
	public String toString() {
		if (dontPrintOutValueToExternalPrograms)
			return "";
		return getSelection();
	}

	@Override
	public Object getSimpleValue() {
		return getSelection();
	}
}
