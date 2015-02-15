/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import ij.process.ImageProcessor;

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;

/**
 * Class for plugins that naturally deal with two-dimensional data. The plugin shell will take care
 * of splitting 3-, 4-, or 5-dimensional datasets into 2-dimensional datasets before passing them to this
 * sort of plugin.
 *
 */

public abstract class TwoDPlugin extends BasePipelinePlugin {
	/**
	 * Run plugin on the 2D dataset in ImageProcessor, storing output in the 2D structure in output.
	 * The contents of input should not be modified.
	 * If previewType is not null, the plugin can explore its contents to determine how to cut
	 * runtime (by processing a subset of the input dataset and/or by computing
	 * a quick approximation of the exact output).
	 * The plugin should check at regular intervals if Thread.interrupted() is true, in which case it
	 * should return without finishing the computation, leaving a partially-computed output. Interruption
	 * can occur in response to user cancellation, or user update of parameter values.
	 * 
	 * @param input
	 *            2D ImageProcessor to use as read-only input
	 * @param output
	 *            2D ImageProcessor to update (pixel storage has already have been allocated, unless requested otherwise
	 *            by the plugin flags)
	 * @param previewType
	 *            If not null, contains instructions to generate a quick preview.
	 * @throws InterruptedException
	 */
	// XXX Change ImageProcessor to PluginIO
	public abstract void runSlice(ImageProcessor input, ImageProcessor output, PreviewType previewType)
			throws InterruptedException;

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

	public void postRun() {
	}
}
