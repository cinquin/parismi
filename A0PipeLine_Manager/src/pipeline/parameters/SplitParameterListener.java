/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.awt.event.ActionEvent;

public class SplitParameterListener implements ParameterListener {
	public ParameterListener[] parameterListeners;

	public SplitParameterListener(ParameterListener[] a) {
		parameterListeners = a;
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		throw new RuntimeException("buttonPressed should never be called in SplitParameterListener");
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		throw new RuntimeException("parameterValueChanged should never be called in SplitParameterListener");
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		throw new RuntimeException("parameterPropertiesChanges should never be called in SplitParameterListener");
	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

	@Override
	public String getParameterName() {
		throw new RuntimeException("getParameterName should never be called in SplitParameterListener");

	}

	@Override
	public void setParameterName(String name) {
		throw new RuntimeException("setParameterName should never be called in SplitParameterListener");

	}

}
