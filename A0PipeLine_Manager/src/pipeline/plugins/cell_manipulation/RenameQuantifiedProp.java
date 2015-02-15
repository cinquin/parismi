/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.Utils;

public class RenameQuantifiedProp extends CellTransform {

	@ParameterInfo(userDisplayName = "Original name", stringValue = "original")
	private String oldName;

	@ParameterInfo(userDisplayName = "New name", stringValue = "new")
	private String newName;

	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) {
		return (ClickedPoint) point.clone();
	}

	@Override
	protected void preRun(PluginIOCells inputCells, IPluginIOHyperstack inputImage) {
		if (inputCells.hasQuantifiedProperty(newName))
			throw new IllegalArgumentException("Property " + newName + " already exists");
	}

	@Override
	protected void postRun(PluginIOCells outputCells) {
		int index = outputCells.getQuantifiedPropertyNames().lastIndexOf(oldName);
		if (index == -1) {
			String message =
					"Could not find property " + oldName + " in "
							+ Utils.printStringArray(outputCells.getQuantifiedPropertyNames());
			throw new PluginRuntimeException(new IllegalArgumentException(message), true);
		}
		outputCells.getQuantifiedPropertyNames().set(index, newName);
		outputCells.fireValueChanged(false, false);
	}

	@Override
	public String operationName() {
		return "Rename quantified property";
	}

}
