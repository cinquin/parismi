/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.util.HashMap;
import java.util.Map;

import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.plugins.ExternalCallToLibScalingParams;

public class CPrincipalCurvatureLibraryCall extends ExternalCallToLibScalingParams {

	@Override
	public String operationName() {
		return "C helper for principal curvature";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public CPrincipalCurvatureLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "principal_curvature";
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] {};
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}
}
