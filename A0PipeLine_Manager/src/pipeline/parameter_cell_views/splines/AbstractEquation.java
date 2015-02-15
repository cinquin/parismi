/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views.splines;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;

abstract class AbstractEquation implements Equation {
	private AbstractEquationData data = new AbstractEquationData();

	protected AbstractEquation() {
		this.data.listeners = new LinkedList<>();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (listener != null && !data.listeners.contains(listener)) {
			data.listeners.add(listener);
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (listener != null) {
			data.listeners.remove(listener);
		}
	}

	protected void firePropertyChange(String propertyName, double oldValue, double newValue) {
		PropertyChangeEvent changeEvent = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
		for (PropertyChangeListener listener : data.listeners) {
			listener.propertyChange(changeEvent);
		}
	}
}
