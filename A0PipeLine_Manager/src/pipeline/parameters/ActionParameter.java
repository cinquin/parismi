/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

/**
 * Provides a callback mechanism to notify a plugin when a button is pressed; does not store any value, unlike other
 * parameters.
 *
 */
public class ActionParameter extends AbstractParameter {
	private static final long serialVersionUID = -8763595982878085979L;

	private boolean clickable;

	public ActionParameter(String name, String explanation, boolean clickable, ParameterListener listener) {
		super(listener, null);
		this.userDisplayName = name;
		this.explanation = explanation;
		this.clickable = clickable;
	}

	@Override
	public Object getValue() {
		Object[] array = {};
		return array;
	}

	@Override
	public void setValue(Object o) {

	}

	@Override
	public boolean[] editable() {
		boolean[] array = { clickable };
		return array;
	}

	@Override
	public String toString() {
		return "";
	}

	@Override
	public boolean valueEquals(Object value) {
		return false;
	}

}
