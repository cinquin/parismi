/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

public class FloatParameter extends AbstractParameter {

	private static final long serialVersionUID = 3435483457892965942L;
	private float floatValue;
	private float minimum, maximum;
	private boolean editableMax = true, editableMin = true;

	public boolean useExponentialFormat = false;

	public FloatParameter(String name, String explanation, float initialValue, float minimum, float maximum,
			boolean editable, boolean editableMax, boolean editableMin, ParameterListener listener) {
		super(listener, null);
		this.floatValue = initialValue;
		this.minimum = Math.min(minimum, initialValue);
		this.maximum = Math.max(maximum, initialValue);
		this.editable = editable;
		this.editableMax = editableMax;
		this.editableMin = editableMin;
		this.userDisplayName = name;
		this.explanation = explanation;
	}

	@Override
	public Object getValue() {
		float[] array = { floatValue, minimum, maximum };
		return array;
	}

	@Override
	public Object getSimpleValue() {
		return floatValue;
	}

	public float getFloatValue() {
		return floatValue;
	}

	@Override
	public String[] getParamNameDescription() {
		String[] strings = { userDisplayName, explanation };
		return strings;
	}

	@Override
	public void setValue(Object o) {
		floatValue = ((float[]) o)[0];
		minimum = ((float[]) o)[1];
		maximum = ((float[]) o)[2];
		if (Math.abs(floatValue) > 1.0E8)
			floatValue = 0;
		if (Math.abs(maximum) > 1.0E8)
			maximum = 0.0001f;
		if (Math.abs(minimum) > 1.0E8)
			minimum = 0.0001f;
		if (floatValue > maximum)
			maximum = floatValue;
		if (floatValue < minimum)
			minimum = floatValue;
		setFieldsValue(floatValue);
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { editable, editableMin, editableMax };
		return array;
	}

	@Override
	public String toString() {
		if (dontPrintOutValueToExternalPrograms)
			return "";
		initializeFormatter();
		return nf.format(floatValue);
	}

	@Override
	public boolean valueEquals(Object value) {
		if (value == null)
			return false;
		return new Float(floatValue).equals(((float[]) value)[0]);
	}

	public void setFloatValue(float f, boolean notifyGUIListeners, boolean notifyParameterListeners) {
		floatValue = f;
		validateRange();
		setFieldsValue(floatValue);
		fireValueChanged(false, notifyGUIListeners, notifyParameterListeners);
	}

	public void validateRange() {
		this.minimum = Math.min(minimum, floatValue);
		this.maximum = Math.max(maximum, floatValue);
	}

}
