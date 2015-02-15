/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;

import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;

// From http://www.objectdefinitions.com/odblog/2007/swing-garbage-collection-problems-with-the-observer-pattern/
public class ParameterListenerWeakRef implements ParameterListener {

	public WeakReference<ParameterListener> actionListenerDelegate;

	public ParameterListenerWeakRef(ParameterListener actionListener) {
		this.actionListenerDelegate = new WeakReference<>(actionListener);
	}

	@Override
	public String getParameterName() {
		ParameterListener delegate = actionListenerDelegate.get();
		if (delegate != null) {
			return delegate.getParameterName();
		} else
			return null;
	}

	@Override
	public void setParameterName(String name) {
		ParameterListener delegate = actionListenerDelegate.get();
		if (delegate != null) {
			delegate.setParameterName(name);
		} else
			throw new RuntimeException("Listener to longer accessible");
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		ParameterListener delegate = actionListenerDelegate.get();
		if (delegate != null) {
			delegate.buttonPressed(commandName, parameter, event);
		}
	}

	public ParameterListener getDelegate() {
		if (actionListenerDelegate != null)
			return actionListenerDelegate.get();
		else
			return null;
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		ParameterListener delegate = actionListenerDelegate.get();
		if (delegate != null) {
			delegate.parameterValueChanged(stillChanging, parameterWhoseValueChanged, false);
		}
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		ParameterListener delegate = actionListenerDelegate.get();
		if (delegate != null) {
			delegate.parameterPropertiesChanged(parameterWhosePropertiesChanged);
		}
	}

	@Override
	public boolean alwaysNotify() {
		ParameterListener delegate = actionListenerDelegate.get();
		if (delegate != null) {
			return delegate.alwaysNotify();
		}
		return false;
	}

}
