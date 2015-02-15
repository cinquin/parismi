/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import ij.measure.Calibration;
import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.FloatParameter;

public abstract class ExternalCallToLibScalingParams extends CPluginIntrospectionAdapter {

	@ParameterInfo(userDisplayName = "x axis stretch", floatValue = 1, permissibleFloatRange = { 0.1f, 10 },
			aliases = { "x scale factor" }, noErrorIfMissingOnReload = true)
	protected float xStretch;

	@ParameterInfo(userDisplayName = "y axis stretch", floatValue = 1, permissibleFloatRange = { 0.1f, 10 },
			aliases = { "y scale factor" }, noErrorIfMissingOnReload = true)
	protected float yStretch;

	@ParameterInfo(userDisplayName = "z axis stretch", floatValue = 1, permissibleFloatRange = { 0.1f, 10 },
			aliases = { "z scale factor" }, noErrorIfMissingOnReload = true)
	protected float zStretch;

	@ParameterInfo(userDisplayName = "Guess z stretch", stringValue = "TRUE", noErrorIfMissingOnReload = true)
	private boolean guessStretch;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output,
			final pipeline.misc_util.ProgressReporter p, final PreviewType previewType, boolean inputHasChanged)
			throws InterruptedException {
		if (guessStretch) {
			Calibration c = input.getCalibration();
			if (c == null)
				throw new RuntimeException("No calibration in input to guess z stretch from");
			float ratio = (float) (c.pixelDepth / c.pixelWidth);
			if (ratio == 1)
				Utils.displayMessage("Suspicious guessed z stretch = 1; incorrect image calibration?", true,
						LogLevel.WARNING);
			((FloatParameter) getParameter("zStretch")).setFloatValue(ratio, true, false);
			((FloatParameter) getParameter("yStretch")).setFloatValue(1, true, false);
			((FloatParameter) getParameter("xStretch")).setFloatValue(1, true, false);
		}
		super.runChannel(input, output, p, previewType, inputHasChanged);
	}

}
