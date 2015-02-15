/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import pipeline.misc_util.Utils;

// Simple class for two-column uneditable JTable
// TODO Refactor to inherit from TableParameter?
public class TwoColumnTableParameter extends AbstractParameter {
	private static final long serialVersionUID = -5188715349731600795L;
	private Object[] firstColumn, secondColumn;
	public boolean hasBeenEdited = false;

	public TwoColumnTableParameter(String name, String explanation, Object[] column1, Object[] column2,
			ParameterListener listener) {
		super(listener, null);
		this.firstColumn = column1;
		this.secondColumn = column2;
		this.userDisplayName = name;
		this.explanation = explanation;
	}

	public Object[] getElements() {
		return secondColumn;
	}

	public Object[] getFirstColumn() {
		return firstColumn;
	}

	public Object[] getSecondColumn() {
		return secondColumn;
	}

	@Override
	public Object getValue() {
		return secondColumn;
	}

	@Override
	public void setValue(Object value) {
		secondColumn = (String[]) value;
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
		return Utils.printObjectArray(secondColumn);
	}

	@Override
	public boolean valueEquals(Object value) {
		throw new RuntimeException("Unimplemented");
	}

	public interface EntryPostProcessor {
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
}
