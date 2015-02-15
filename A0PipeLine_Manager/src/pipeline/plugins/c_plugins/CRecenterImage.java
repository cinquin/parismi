/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.plugins.ExternalCallToLibScalingParams;

public class CRecenterImage extends ExternalCallToLibScalingParams {

	@Override
	public String getCommandName() {
		return "recenterImage";
	}

	@ParameterInfo(userDisplayName = "Threshold",
			description = "Only pixel values above threshold are used when calculating the center of mass",
			floatValue = 1, permissibleFloatRange = { 0, 10 })
	protected float sigma;

	@Override
	public String operationName() {
		return "C helper for recentering";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public CRecenterImage() {
		initializeParams();
	}

}
