/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.awt.event.ActionEvent;

public interface ParameterListener {
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet);

	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged);

	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event);

	public boolean alwaysNotify();

	public String getParameterName();

	public void setParameterName(String name);
}
