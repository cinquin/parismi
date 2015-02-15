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
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;

/**
 * Translate seeds by user-specified offsets.
 *
 */
public class TranslateCells extends CellTransform {

	@Override
	public String getToolTip() {
		return "Translate seeds by user-specified offsets";
	}

	@Override
	public String operationName() {
		return "Translate cells";
	}

	@ParameterInfo(userDisplayName = "x offset", floatValue = 0, permissibleFloatRange = { 0, 100 },
			noErrorIfMissingOnReload = false)
	private int xOffset;

	@ParameterInfo(userDisplayName = "y offset", floatValue = 0, permissibleFloatRange = { 0, 100 },
			noErrorIfMissingOnReload = false)
	private int yOffset;

	@ParameterInfo(userDisplayName = "z offset", floatValue = 0, permissibleFloatRange = { 0, 100 },
			noErrorIfMissingOnReload = false)
	private int zOffset;

	@ParameterInfo(
			userDisplayName = "Randomize offsets",
			description = "If true, offsets are chosen at random from a uniform distribution between -r and +r where r is value given above",
			booleanValue = false, noErrorIfMissingOnReload = true)
	private boolean randomizeOffsets;

	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) {
		ClickedPoint pCloned = (ClickedPoint) point.clone();

		if (randomizeOffsets) {
			pCloned.x += xOffset * 2 * (Math.random() - 0.5);
			pCloned.y += yOffset * 2 * (Math.random() - 0.5);
			pCloned.z += zOffset * 2 * (Math.random() - 0.5);
		} else {
			pCloned.x += xOffset;
			pCloned.y += yOffset;
			pCloned.z += zOffset;
		}

		return pCloned;
	}

}
