/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.plugins.ExternalCallToLibScalingParams;
import pipeline.plugins.PluginInfo;

@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "Blur3D")
public class CBlurLibraryCall extends ExternalCallToLibScalingParams {

	@Override
	public String getCommandName() {
		return "blur";
	}

	@ParameterInfo(userDisplayName = "Sigma", description = "Sets the extent of the blurring", floatValue = 1,
			permissibleFloatRange = { 1, 20 })
	protected float sigma;

	@Override
	public String operationName() {
		return "C helper for blurring";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public CBlurLibraryCall() {
		initializeParams();
	}

}
