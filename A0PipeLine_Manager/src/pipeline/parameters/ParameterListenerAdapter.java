/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.awt.event.ActionEvent;

import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;

public class ParameterListenerAdapter implements ParameterListener {

	public ParameterListenerAdapter() {
	}

	private Object currentValue;
	private boolean changeTriggersUpdate = true, changeTriggersLiveUpdates = true;

	protected void changeAction(AbstractParameter parameterWhoseValueChanged) throws Exception {
	}

	public ParameterListenerAdapter(Object currentValue, boolean changeTriggersUpdate, boolean changeTriggersLiveUpdates) {
		super();

		this.currentValue = currentValue;
		this.changeTriggersUpdate = changeTriggersUpdate;
		this.changeTriggersLiveUpdates = changeTriggersLiveUpdates;
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		if (!changeTriggersUpdate)
			return;
		if (stillChanging && (!changeTriggersLiveUpdates))
			return;
		if (!parameterWhoseValueChanged.valueEquals(currentValue)) {
			currentValue = parameterWhoseValueChanged.getValue();
			if (!keepQuiet)
				try {
					changeAction(parameterWhoseValueChanged);
				} catch (Exception e) {
					Utils.printStack(e);
				}
		}
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
	}

	public ParameterListener getWeakRef() {
		return new ParameterListenerWeakRef(this);
	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

	private String parameterName;

	@Override
	public String getParameterName() {
		return parameterName;
	}

	@Override
	public void setParameterName(String name) {
		parameterName = name;
	}
}
