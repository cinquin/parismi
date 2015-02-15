/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;

/**
 * Class for plugins that naturally deal with three-dimensional data. The plugin shell will take care
 * of splitting 4- or 5-dimensional datasets into 3-dimensional datasets before passing them to this
 * sort of plugin.
 *
 */
public abstract class ThreeDPlugin extends BasePipelinePlugin {
	/**
	 * Run plugin on the 3D dataset in input, storing output in the 3D structure in output.
	 * The contents of input should not be modified.
	 * If previewType is not null, the plugin can explore its contents to determine how to cut
	 * runtime (by processing a subset of the input dataset and/or by computing a quick
	 * approximation of the exact output).
	 * The plugin should check at regular intervals if Thread.interrupted() is true, in which case it
	 * should return without finishing the computation, leaving a partially-computed output. Interruption
	 * can occur in response to user cancellation, or user update of parameter values.
	 * 
	 * @param input
	 *            3D dataset to use as read-only input
	 * @param output
	 *            3D dataset to update (pixel storage has already have been allocated, unless requested otherwise by the
	 *            plugin flags)
	 * @param r
	 *            Progress bar that should be updated at regular intervals to give GUI feedback of remaining computation
	 *            time
	 * @param previewType
	 *            If not null, contains instructions to generate a quick preview.
	 * @param inputHasChanged
	 *            TODO
	 */
	public abstract void runChannel(IPluginIOStack input, IPluginIOStack output, ProgressReporter r,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException;

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
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
