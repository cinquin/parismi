/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import pipeline.RowReferenceHolder;
import pipeline.RowTransform;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class RowOrFileTextReference extends TextParameter implements RowReferenceHolder {
	private static final long serialVersionUID = -256215904403977791L;

	public RowOrFileTextReference(String name, String explanation, String initial_value, boolean editable,
			ParameterListener listener) {
		super(name, explanation, initial_value, editable, listener, null);
	}

	@Override
	public void offsetReference(int yourRow, RowTransform transform) {
		if (value == null)
			return;
		if ((value.indexOf('$') == 0)) { // name starts with a $, meaning an absolute row number
			value = value.substring(1);
			if (!Utils.isParsableToInt(value)) {
				Utils.log(value + " is not parsable to int in offsetReference", LogLevel.ERROR);
				return;
			}
			int rowNumber = Integer.parseInt(value);
			if (rowNumber < 0) {
				throw new PluginRuntimeException("Negative row", true);
			}
			value = "$" + transform.computeNewReference(true, yourRow, rowNumber);

		} else if (Utils.isParsableToInt(value)) { // relative reference
			int relativeRef = Integer.parseInt(value);
			value = "" + transform.computeNewReference(false, yourRow, relativeRef);
		}
	}

	@Override
	public boolean isCompactDisplay() {
		return true;
	}

}
