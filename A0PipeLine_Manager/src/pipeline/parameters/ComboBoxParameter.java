/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

public class ComboBoxParameter extends AbstractParameter {
	private static final long serialVersionUID = -7807142611052267551L;

	private String[] choices;

	public void setChoices(String[] newChoices) {
		if (currentChoiceIndex >= newChoices.length) {
			currentChoiceIndex = newChoices.length - 1;
			if (currentChoiceIndex > -1)
				currentChoice = newChoices[currentChoiceIndex];
		}
		this.choices = newChoices;
	}

	private String currentChoice;
	private int currentChoiceIndex;

	private static int indexOf(String[] a, String b) {
		if (a == null)
			return -1;
		for (int i = 0; i < a.length; i++) {
			if (b.equals(a[i]))
				return i;
		}
		return -1;
	}

	public ComboBoxParameter(String name, String explanation, String[] choices, String initialChoice, boolean editable,
			ParameterListener listener) {
		super(listener, null);
		this.choices = choices;
		this.currentChoice = initialChoice;
		this.userDisplayName = name;
		this.explanation = explanation;
		this.currentChoiceIndex = indexOf(choices, initialChoice);
		this.editable = editable;
	}

	public String[] getChoices() {
		return choices;
	}

	public int getSelectionIndex() {
		return currentChoiceIndex;
	}

	public void setSelectionIndex(int selection) {
		currentChoiceIndex = selection;
		if (currentChoiceIndex > -1)
			currentChoice = choices[currentChoiceIndex];
		else
			currentChoice = null;
		setFieldsValue(currentChoice);
	}

	public String getSelection() {
		if ((choices != null) && currentChoiceIndex > -1)
			return choices[currentChoiceIndex];
		else
			return null;
	}

	@Override
	public Object getValue() {
		Object[] array = { currentChoice, new Integer(currentChoiceIndex) };
		return array;
	}

	@Override
	public Object getSimpleValue() {
		return currentChoiceIndex;
	}

	@Override
	public boolean valueEquals(Object value) {
		if (value == null)
			return false;
		return currentChoice.equals(((Object[]) value)[0])
				&& new Integer(currentChoiceIndex).equals(((Object[]) value)[1]);
	}

	public String getStringValue() {
		return currentChoice;
	}

	@Override
	public void setValue(Object o) {
		currentChoice = (String) ((Object[]) o)[0];
		currentChoiceIndex = (Integer) ((Object[]) o)[1];
		if (currentChoice == null)
			currentChoice = choices[currentChoiceIndex];
		if (currentChoiceIndex >= choices.length)
			throw new IllegalArgumentException("Index " + currentChoiceIndex + " out of bounds");
		setFieldsValue(getSimpleValue());
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
		return Integer.toString(currentChoiceIndex);
	}

	public void setItemName(int index, String s) {
		if (index == 0 && choices.length == 0) {
			choices = new String[1];
		}
		choices[index] = s;
		if (currentChoiceIndex == index)
			currentChoice = s;
	}

}
