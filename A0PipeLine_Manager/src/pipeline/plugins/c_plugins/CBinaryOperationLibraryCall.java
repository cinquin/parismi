/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.ExternalCallToLibrary;

public class CBinaryOperationLibraryCall extends ExternalCallToLibrary {

	private String method = "";

	private class MethodListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			Utils.log("Change callback with new combo box choice "
					+ (((ComboBoxParameter) param1).getSelectionIndex() - 1), LogLevel.DEBUG);
			if (!(((ComboBoxParameter) param1).getSelection().equals(method))) {
				method = ((ComboBoxParameter) param1).getSelection();
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener methodListener0 = new MethodListener();
	private ParameterListener methodListener1 = new ParameterListenerWeakRef(methodListener0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { methodListener0, null };// We do NOT want a weak listener because we need to
		// stay around after the plugin has first been run from the pipeline
	}

	private void initializeParams() {
		param1 =
				new ComboBoxParameterPrintValueAsString("Operation", "", new String[] { "Multiply" }, "Multiply",
						false, methodListener1);
		param2 = null;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		param1 = param[0];
		param2 = null;
	}

	@Override
	public String operationName() {
		return "C helper for binary operations";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public CBinaryOperationLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "binaryOperation";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Input2" };
	}
}
