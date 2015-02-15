/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

public class IntParameter extends AbstractParameter {
	private static final long serialVersionUID = -658459062071758465L;

	private int intValue, minimum, maximum;
	private boolean editableMax, editableMin;

	public IntParameter(String name, String explanation, int initialValue, int minimum, int maximum,
			boolean editableMax, boolean editableMin, ParameterListener listener) {
		super(listener, null);
		this.intValue = initialValue;
		this.minimum = Math.min(minimum, initialValue);
		this.maximum = Math.max(maximum, initialValue);
		this.editableMax = editableMax;
		this.editableMin = editableMin;
		this.userDisplayName = name;
		this.explanation = explanation;
	}

	@Override
	public Object getValue() {
		int[] array = { intValue, minimum, maximum };
		return array;
	}

	@Override
	public boolean valueEquals(Object value) {
		if (value == null)
			return false;
		return new Integer(intValue).equals(((int[]) value)[0]);
	}

	public int getintValue() {
		return intValue;
	}

	@Override
	public Object getSimpleValue() {
		return intValue;
	}

	@Override
	public void setValue(Object o) {
		intValue = ((int[]) o)[0];
		minimum = ((int[]) o)[1];
		maximum = ((int[]) o)[2];
		if (minimum > maximum)
			minimum = maximum;
		if (minimum > intValue)
			minimum = intValue;
		if (maximum < intValue)
			maximum = intValue;
		setFieldsValue(intValue);
	}

	public void setIntValue(int v) {
		intValue = v;
		if (minimum > intValue)
			minimum = intValue;
		if (maximum < intValue)
			maximum = intValue;
		setFieldsValue(intValue);
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { editableMin, editableMax };
		return array;
	}

	@Override
	public String toString() {
		if (dontPrintOutValueToExternalPrograms)
			return "";
		initializeFormatter();
		return nf.format(intValue);

	}

}
