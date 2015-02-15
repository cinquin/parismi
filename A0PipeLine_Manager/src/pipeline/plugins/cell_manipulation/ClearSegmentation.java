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

public class ClearSegmentation extends CellTransform {

	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) {
		ClickedPoint result = (ClickedPoint) point.clone();
		result.imageFullSegCoordsX = null;
		result.imageFullSegCoordsY = null;
		result.imageFullSegCoordsZ = null;

		result.imagePerimsegCoordsX = null;
		result.imagePerimsegCoordsY = null;
		result.imagePerimsegCoordsZ = null;

		return result;
	}

	@Override
	public String operationName() {
		return "ClearSegmentation";
	}

	@Override
	public String getToolTip() {
		return "Strip active contour segmentation from cells, so plugin can be run from scratch";
	}

}
