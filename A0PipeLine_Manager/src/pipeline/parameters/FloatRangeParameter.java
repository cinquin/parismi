/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import org.jfree.data.xy.XYSeries;

public class FloatRangeParameter extends AbstractParameter {
	private static final long serialVersionUID = 2684359025790759554L;

	private float lowValue, highValue, minimum, maximum;
	private boolean editableMax, editableMin;
	public Object histogram = new org.jfree.data.xy.XYSeries("Histogram");

	public FloatRangeParameter(String name, String explanation, float initialLowValue, float initialHighValue,
			float minimum, float maximum, boolean editableMax, boolean editableMin, ParameterListener listener,
			Object creatorReference) {
		super(listener, null);
		this.lowValue = initialLowValue;
		this.highValue = initialHighValue;
		this.minimum = minimum;
		this.maximum = maximum;
		this.editableMax = editableMax;
		this.editableMin = editableMin;
		this.userDisplayName = name;
		this.explanation = explanation;
		this.creatorReference = creatorReference;

		for (int i = 1; i <= 50; i++) {
			((XYSeries) histogram).add(i, 10 * Math.exp(i / 5.0));
		}

	}

	public float lowValue() {
		return lowValue;
	}

	public float highValue() {
		return highValue;
	}

	@Override
	/**
	 * @return float []: selected minimum, selected maximum, minimum of selectable range, maximum of selectable range
	 */
	public Object getValue() {
		float[] array = { lowValue, highValue, minimum, maximum };
		return array;
	}

	@Override
	public boolean valueEquals(Object value) {
		if (value == null)
			return false;
		return new Float(lowValue).equals(((Object[]) value)[0]) && new Float(highValue).equals(((Object[]) value)[1]);
	}

	@Override
	public void setValue(Object o) {
		lowValue = ((float[]) o)[0];
		highValue = ((float[]) o)[1];
		if (highValue < lowValue) {
			float temp = lowValue;
			lowValue = highValue;
			highValue = temp;
		}
		minimum = ((float[]) o)[2];
		maximum = ((float[]) o)[3];
		if (highValue > maximum)
			maximum = highValue;
		if (lowValue < minimum)
			minimum = lowValue;
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
		return (nf.format(lowValue) + "	" + nf.format(highValue));
	}

	public void updateBounds(float minimum, float maximum) {
		if (lowValue < minimum)
			lowValue = minimum;
		if (highValue < minimum)
			highValue = minimum;
		if (lowValue > maximum)
			lowValue = maximum;
		if (highValue > maximum)
			highValue = maximum;
		this.minimum = minimum;
		this.maximum = maximum;
	}

	public void setValueFireIfAppropriate(float[] o, boolean stillChanging, boolean notifyGUIListeners,
			boolean notifyParameterListeners) {
		boolean fireUpdate = (o[0] != lowValue) || ((o[1] != highValue)) || ((o[2] != minimum)) || ((o[3] != maximum));
		setValue(o);
		if (fireUpdate)
			fireValueChanged(stillChanging, notifyGUIListeners, notifyParameterListeners);
	}

}
