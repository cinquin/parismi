/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.util.ArrayList;
import java.util.List;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class MultiListParameter extends AbstractParameter {
	private static final long serialVersionUID = 4948350199914290055L;

	private String[] choices;
	private int[] currentSelection;

	public boolean[] getSelectedChoices() {
		boolean[] result = new boolean[choices.length];
		if (currentSelection != null) {
			for (int aCurrentSelection : currentSelection) {
				result[aCurrentSelection] = true;
			}
		}
		return result;
	}

	public MultiListParameter(String name, String explanation, String[] choices, int[] initialChoices,
			ParameterListener listener) {
		super(listener, null);
		this.choices = choices;
		this.currentSelection = initialChoices;
		this.userDisplayName = name;
		this.explanation = explanation;
	}

	public String[] getChoices() {
		return choices;
	}

	public String[] getSelectionString() {
		ArrayList<String> selectedItems = new ArrayList<>(currentSelection.length);
		for (int currentChoice : currentSelection) {
			if (currentChoice < choices.length)
				selectedItems.add(choices[currentChoice]);
			else
				Utils.log("In getSelectionString: argument out of bounds: " + currentChoice + " length is "
						+ choices.length, LogLevel.DEBUG);
		}
		String[] returnArray = new String[selectedItems.size()];
		selectedItems.toArray(returnArray);
		return returnArray;
	}

	public int[] getSelection() {
		return currentSelection;
	}

	public void setSelection(int[] a) {
		for (int element : a) {
			if (element >= choices.length) {
				throw new IllegalArgumentException("Trying to set selection beyond possible choices "
						+ "in MultiListParameter: index " + element + " but length is " + choices.length);
			}
		}
		currentSelection = a;
	}

	public void trimSelection() {
		// remove items from selection that are past current length
		ArrayList<Integer> newSelection = new ArrayList<>(20);
		for (int currentChoice : currentSelection) {
			if (currentChoice < choices.length) {
				newSelection.add(currentChoice);
			}
		}
		int[] newSelectionArray = new int[newSelection.size()];
		for (int i = 0; i < newSelectionArray.length; i++)
			newSelectionArray[i] = newSelection.get(i);

		currentSelection = newSelectionArray;

	}

	@Override
	public Object getValue() {
		return currentSelection;
	}

	@Override
	public boolean valueEquals(Object value) {
		if (value == null)
			return false;
		int[] val = (int[]) value;
		int index = 0;
		for (int i : val) {
			if (index == currentSelection.length)
				return false;
			if (i != currentSelection[index])
				return false;
			index++;
		}
		return index == currentSelection.length;
	}

	@Override
	public void setValue(Object value) {
		currentSelection = (int[]) value;
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { false };
		return array;
	}

	public void setChoices(String[] newChoices) {
		List<Integer> newSelection = new ArrayList<>();
		for (int aCurrentSelection : currentSelection) {
			int match = -1;
			for (int j = 0; j < newChoices.length; j++) {
				if (aCurrentSelection < choices.length && newChoices[j].equals(choices[aCurrentSelection])) {
					match = j;
					break;
				}
			}
			if (match == -1) {
				match = 0;
				// Output warning?
			}
			if (!newSelection.contains(match))
				newSelection.add(match);
		}
		currentSelection = new int[newSelection.size()];
		for (int i = 0; i < currentSelection.length; i++) {
			currentSelection[i] = newSelection.get(i);
		}
		choices = newChoices;
	}

	@Override
	public Object getSimpleValue() {
		return currentSelection;
	}

	@Override
	public String toString() {
		if (dontPrintOutValueToExternalPrograms)
			return "";
		return Utils.printStringArray(getSelectionString());
	}

}
