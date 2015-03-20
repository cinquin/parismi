/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.io.Serializable;
import java.util.Arrays;

import pipeline.misc_util.Utils;

// simple class for one-column editable JTable
public class TableParameter extends AbstractParameter {
	private static final long serialVersionUID = -4579907800837898220L;
	private String[] elements;
	private int[] currentChoices;

	public boolean hasBeenEdited = false;

	public boolean displayHorizontally = false;

	private int[] getValidSelectedItems() {
		if (currentChoices == null)
			return new int[0];
		int[] result = new int[currentChoices.length];
		int index = 0;
		for (int currentChoice : currentChoices) {
			if (currentChoice < elements.length) {
				result[index] = currentChoice;
				index++;
			}
		}
		if (index < result.length) {
			// we need to return a smaller array
			int[] newResult = new int[index];
			for (int i = 0; i < newResult.length; i++) {
				newResult[i] = result[i];
			}
			return newResult;
		} else
			return result;
	}

	public String[] getSelectionString() {
		int[] validItems = getValidSelectedItems();
		String[] selectedItems = new String[validItems.length];
		for (int i = 0; i < validItems.length; i++) {
			selectedItems[i] = elements[validItems[i]];
		}
		return selectedItems;
	}

	static int indexOf(String[] a, String b) {
		for (int i = 0; i < a.length; i++) {
			if (b.equals(a[i]))
				return i;
		}
		return -1;
	}

	public int[] getSelection() {
		return currentChoices;
	}

	public void setSelection(int[] s) {
		currentChoices = s;
	}

	public TableParameter(String name, String explanation, String[] elements, ParameterListener listener) {
		super(listener, null);
		this.elements = elements;
		this.userDisplayName = name;
		this.explanation = explanation;
	}

	public String[] getElements() {
		return elements;
	}

	public void setElements(String[] elements) {
		this.elements = elements;
	}

	public String[] getElements(int[] indexes) {
		String[] results = new String[indexes.length];
		for (int i = 0; i < indexes.length; i++) {
			results[i] = elements[indexes[i]];
		}
		return results;
	}

	@Override
	public Object getValue() {
		return elements;
	}

	@Override
	public void setValue(Object value) {
		elements = (String[]) value;
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { false };
		return array;
	}

	@Override
	public String toString() {
		if (dontPrintOutValueToExternalPrograms)
			return "";
		return Utils.printStringArray(getSelectionString());
	}

	public interface EntryPostProcessor extends Serializable {
		Object postProcess(Object input);
	}

	private EntryPostProcessor postProcessor;

	public EntryPostProcessor getPostProcessor() {
		return postProcessor;
	}

	public void setPostProcessor(EntryPostProcessor postProcessor) {
		this.postProcessor = postProcessor;
	}

	private boolean enforceUniqueEntries;

	public boolean isEnforceUniqueEntries() {
		return enforceUniqueEntries;
	}

	public void setEnforceUniqueEntries(boolean enforceUniqueEntries) {
		this.enforceUniqueEntries = enforceUniqueEntries;
	}

	// FIXME Need to take selection into account
	@Override
	public Object getSimpleValue() {
		return elements;
	}

	// FIXME Need to take selection into account
	@Override
	public boolean valueEquals(Object o) {
		return Arrays.equals(elements, (String[]) o);
	}
}
