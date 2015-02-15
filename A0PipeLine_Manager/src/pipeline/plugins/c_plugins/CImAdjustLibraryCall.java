/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.plugins.ExternalCallToLibScalingParams;

public class CImAdjustLibraryCall extends ExternalCallToLibScalingParams {

	@ParameterInfo(userDisplayName = "minPercentile",
			description = "Percentile below which pixels are all set to the same value", floatValue = 5,
			permissibleFloatRange = { 0, 100 })
	protected float minPercentile;

	@ParameterInfo(userDisplayName = "maxPercentile",
			description = "Percentile above which pixels are all set to the same value", floatValue = 95,
			permissibleFloatRange = { 0, 100 })
	protected float maxPercentile;

	@Override
	public String operationName() {
		return "C helper for C version of Matlab's imadjust";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public CImAdjustLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "imadjust";
	}

}
