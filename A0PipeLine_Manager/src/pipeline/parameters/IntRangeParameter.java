/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

public class IntRangeParameter extends AbstractParameter {
	private static final long serialVersionUID = 5994950788927536231L;

	private int rangeLowerBound, rangeUpperBound, minimum, maximum;
	private boolean editableMax, editableMin;

	public IntRangeParameter(String name, String explanation, int initialLowValue, int initialHighValue, int minimum,
			int maximum, boolean editableMax, boolean editableMin, ParameterListener listener, Object creatorReference) {
		super(listener, null);
		this.rangeLowerBound = initialLowValue;
		this.rangeUpperBound = initialHighValue;
		this.minimum = minimum;
		this.maximum = maximum;
		this.editableMax = editableMax;
		this.editableMin = editableMin;
		this.userDisplayName = name;
		this.explanation = explanation;
		this.creatorReference = creatorReference;
	}

	@Override
	public Object getValue() {
		int[] array = { rangeLowerBound, rangeUpperBound, minimum, maximum };
		return array;
	}

	@Override
	public boolean valueEquals(Object value) {
		if (value == null)
			return false;
		return new Integer(rangeLowerBound).equals(((Object[]) value)[0])
				&& new Integer(rangeUpperBound).equals(((Object[]) value)[1]);
	}

	public int[] getintValue() {
		return new int[] { rangeLowerBound, rangeUpperBound };
	}

	public int getHighInt() {
		return rangeUpperBound;
	}

	public int getLowInt() {
		return rangeLowerBound;
	}

	public void updateBounds(int minimum, int maximum) {
		if (rangeLowerBound < minimum)
			rangeLowerBound = minimum;
		if (rangeUpperBound < minimum)
			rangeUpperBound = minimum;
		if (rangeLowerBound > maximum)
			rangeLowerBound = maximum;
		if (rangeUpperBound > maximum)
			rangeUpperBound = maximum;
		this.minimum = minimum;
		this.maximum = maximum;
	}

	@Override
	public void setValue(Object o) {
		timeLastChange = System.currentTimeMillis();
		rangeLowerBound = ((int[]) o)[0];
		rangeUpperBound = ((int[]) o)[1];
		if (rangeUpperBound < rangeLowerBound) {
			int temp = rangeLowerBound;
			rangeLowerBound = rangeUpperBound;
			rangeUpperBound = temp;
		}
		minimum = ((int[]) o)[2];
		maximum = ((int[]) o)[3];
		if (rangeUpperBound > maximum)
			maximum = rangeUpperBound;
		if (rangeLowerBound < minimum)
			minimum = rangeLowerBound;
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
		return (nf.format(rangeLowerBound) + "	" + nf.format(rangeUpperBound));
	}

	public void setValueFireIfAppropriate(int[] o, boolean stillChanging, boolean notifyGUIListeners,
			boolean notifyParameterListeners) {
		boolean fireUpdate =
				(o[0] != rangeLowerBound) || ((o[1] != rangeUpperBound)) || ((o[2] != minimum)) || ((o[3] != maximum));
		setValue(o);
		if (fireUpdate)
			fireValueChanged(stillChanging, notifyGUIListeners, notifyParameterListeners);
	}

}
