/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.ThreeDPlugin;

/**
 * Creates an empty 32-bit image of same dimensions as input image.
 */

public class EmptyFloatImage extends ThreeDPlugin {

	@Override
	public String getToolTip() {
		return "Create an empty 32-bit image of same dimensions as input";
	}

	@Override
	public String operationName() {
		return "Empty image";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL;
	}

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, ProgressReporter p,
			PreviewType previewType, boolean inputHasChanged) {

		Utils.log("Empty image created", LogLevel.DEBUG);

		progressSetIndeterminateThreadSafe(p, false);
		progressSetValueThreadSafe(p, 100);
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination",
				new InputOutputDescription(null, null, null, InputOutputDescription.KEEP_IN_RAM,
						InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}

}
