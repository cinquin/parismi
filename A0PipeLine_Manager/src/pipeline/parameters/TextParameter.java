/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import javax.swing.TransferHandler.TransferSupport;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class TextParameter extends AbstractParameter implements DropAcceptingParameter {
	private static final long serialVersionUID = 8066408784611910240L;

	String value;

	@SuppressWarnings("unused")
	private static Object stupid = new DropProcessor() {
		// Pacify xstream by creating anonymous inner class
		// Keep this here or some old pipeline xml files won't reload properly
		@Override
		public Object process(Object o) {
			return null;
		}
	};

	public TextParameter(String name, String explanation, String initial_value, boolean editable,
			ParameterListener listener, Object creatorReference) {
		super(listener, creatorReference);
		this.value = initial_value;
		this.editable = editable;
		this.userDisplayName = name;
		this.explanation = explanation;
	}

	@Override
	public Object getSimpleValue() {
		return value;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public boolean valueEquals(Object value) {
		return this.value.equals(value);
	}

	public String getStringValue() {
		return value;
	}

	@Override
	public void setValue(Object o) {
		value = (String) o;
		setFieldsValue(value);
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { editable, false };
		return array;
	}

	@Override
	public String toString() {
		if (dontPrintOutValueToExternalPrograms)
			return "";
		return value;
	}

	@Override
	public boolean canImport(TransferSupport info) {
		return true;// FIXME Examine info to determine whether import is possible
	}

	@Override
	public boolean importData(TransferSupport support) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean importPreprocessedData(Object o) {
		if (!(o instanceof String)) {
			Utils.log("Textbox can only import String", LogLevel.ERROR);
			return false;
		}
		if (dropProcessor == null)
			dropProcessor = defaultProcessor;
		String processed = (String) dropProcessor.process(o);
		if (processed == null)
			return false;
		value = processed;
		fireValueChanged(false, true, true);
		return true;
	}

	public void setDropProcessor(DropProcessor processor) {
		dropProcessor = processor;
	}

	protected transient DropProcessor dropProcessor = defaultProcessor;

	protected static DropProcessor defaultProcessor = o -> o;

}
