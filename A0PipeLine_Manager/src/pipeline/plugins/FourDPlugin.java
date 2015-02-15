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
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;

/**
 * Class for plugins that naturally deal with four-dimensional data. The plugin shell will take care
 * of splitting 5-dimensional datasets into 4-dimensional datasets before passing them to this
 * sort of plugin (NB: THIS HAS NOT BEEN IMPLEMENTED YET).
 *
 */
public abstract class FourDPlugin extends BasePipelinePlugin {
	/**
	 * Run plugin on the input specified by the inherited variable source, and store the output
	 * in the inherited variable stored in destination.
	 * The contents of input should not be modified.
	 * If previewType is not null, the plugin can explore its contents to determine how to cut
	 * runtime (by processing a subset of the input dataset and/or by computing a quick
	 * approximation of the exact output).
	 * The plugin should check at regular intervals if Thread.interrupted() is true, in which case it
	 * should return without finishing the computation, leaving a partially-computed output. Interruption
	 * can occur in response to user cancellation, or user update of parameter values.
	 * 
	 * @param r
	 *            Progress bar that should be updated at regular intervals to give GUI feedback of remaining computation
	 *            time
	 * @param inChannels
	 *            List of input channels that have been selected by the user to be processed; ignore the other ones
	 * @param outChannels
	 *            List of output channels in the destination ImagePlus channels that the user wants the results stored
	 *            into.
	 * @param previewType
	 *            If not null, contains instructions to generate a quick preview.
	 * @param inputHasChanged
	 *            true if input has potentially changed since last time the plugin was called
	 * @param parameterWhoseValueChanged
	 * @param stayInCoreLoop
	 *            True if plugins responding to the change should stay in their core loop and
	 *            update when the parameter changes again; this is used to increase responsiveness to GUI input
	 * @throws InterruptedException
	 */
	public abstract void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException;

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		return result;
	}
}
