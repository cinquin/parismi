/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

public class BooleanParameter extends AbstractParameter {
	private static final long serialVersionUID = 4359555945275324531L;

	private boolean currentChoice;

	public BooleanParameter(String name, String explanation, boolean initialValue, boolean editable,
			ParameterListener listener) {
		super(listener, null);
		this.currentChoice = initialValue;
		this.userDisplayName = name;
		this.explanation = explanation;
		this.editable = editable;
	}

	@Override
	public Object getValue() {
		Object[] array = { currentChoice };
		return array;
	}

	@Override
	public Object getSimpleValue() {
		return currentChoice;
	}

	@Override
	public boolean valueEquals(Object value) {
		if (value == null)
			return false;
		return ((Boolean) ((Object[]) value)[0]) == currentChoice;
	}

	public boolean getBooleanValue() {
		return currentChoice;
	}

	@Override
	public void setValue(Object o) {
		currentChoice = (Boolean) o;
		setFieldsValue(currentChoice);
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { editable };
		return array;
	}

	@Override
	public String toString() {
		if (dontPrintOutValueToExternalPrograms)
			return "";
		return ((Boolean) currentChoice).toString();
	}

}
