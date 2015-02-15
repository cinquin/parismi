/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.ExternalCallToLibrary;

public class CDilateLibraryCall extends ExternalCallToLibrary {

	private int sigma;

	private class SigmaListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((int[]) param1.getValue())[0] != sigma) {
				sigma = ((int[]) param1.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}

		}
	}

	private ParameterListener sigmaListener0 = new SigmaListener();
	private ParameterListener sigmaListener1 = new ParameterListenerWeakRef(sigmaListener0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { sigmaListener1, null };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		param1 = param[0];
		param2 = null;
		sigma = ((int[]) param1.getValue())[0]; // getValue() returns an array of Objects; first object in array is the
												// value of the parameter
	}

	@Override
	public String operationName() {
		return "C helper for dilation";
	}

	@Override
	public String version() {
		return "1.0";
	}

	void initializeParams() {
		param1 = new IntParameter("Box size", "", 1, 0, 20, true, true, sigmaListener1);
	}

	public CDilateLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "dilate";
	}

}
