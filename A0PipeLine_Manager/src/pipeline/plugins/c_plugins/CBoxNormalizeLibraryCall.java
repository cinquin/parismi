/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.plugins.ExternalCallToLibScalingParams;

public class CBoxNormalizeLibraryCall extends ExternalCallToLibScalingParams {

	@ParameterInfo(userDisplayName = "Window size", description = "Diameter of the sliding box used for normalization",
			floatValue = 1, permissibleFloatRange = { 1, 20 }, aliases = { "Box size" })
	protected int windowSize;

	@Override
	public String operationName() {
		return "C helper for box normalization";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public CBoxNormalizeLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "box_normalize";
	}

}
