/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/

package pipeline.plugins.cell_manipulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.PluginRuntimeException;

public class NormalizeGeodesicDistance extends CellTransform {

	@Override
	public String getToolTip() {
		return "Normalize geodesic distance so that the second crescent is at position 1.0";
	}

	@Override
	public String operationName() {
		return "Normalize GD";
	}

	@ParameterInfo(userDisplayName = "Crescent label name", stringValue = "_anno_crescent")
	private String crescentLabelName;
	
	private float sndCrescent;
	
	@Override
	protected void preRun(pipeline.data.PluginIOCells inputCells, IPluginIOHyperstack inputImage) {
		List<Float> distances = new ArrayList<>();
		
		for (ClickedPoint cp: inputCells) {
			if (cp.getQuantifiedProperty(crescentLabelName) > 0) {
				distances.add(cp.getQuantifiedProperty("geodesicDistance"));
			}
		}
		if (distances.size() < 2) {
			throw new PluginRuntimeException("Too few crescents for geodesic distance normalization",
					true);
		}
		Collections.sort(distances);
		sndCrescent = distances.get(1);
	}
	
	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) throws InterruptedException {
		ClickedPoint pCloned = (ClickedPoint) point.clone();
		pCloned.setQuantifiedPropertyNames(point.getQuantifiedPropertyNames());
		pCloned.setQuantifiedProperty("geodesicDistance", point.getQuantifiedProperty("geodesicDistance") / sndCrescent);
		return pCloned;
	}
}
