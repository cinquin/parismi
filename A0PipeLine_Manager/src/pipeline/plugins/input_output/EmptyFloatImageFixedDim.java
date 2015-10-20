/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.SpecialDimPlugin;

/**
 * Creates an empty 32-bit image using either specified dimensions or dimensions of input image.
 */

public class EmptyFloatImageFixedDim extends FourDPlugin implements SpecialDimPlugin {
	
	@ParameterInfo(userDisplayName = "Copy dimensions of input image", booleanValue = false,
			noErrorIfMissingOnReload = true)
	private boolean copyDimensionsOfInput;

	@ParameterInfo(userDisplayName = "Width", noErrorIfMissingOnReload = true)
	private int width;

	@ParameterInfo(userDisplayName = "Height", noErrorIfMissingOnReload = true)
	private int height;

	@ParameterInfo(userDisplayName = "Depth", noErrorIfMissingOnReload = true)
	private int depth;

	@ParameterInfo(userDisplayName = "Number of channels", noErrorIfMissingOnReload = true, floatValue = 1)
	private int nChannels;
	
	@Override
	public String getToolTip() {
		return "Create an empty 32-bit image of same dimensions as input (by default) or manually-specified dimensions";
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
		return SPECIAL_DIMENSIONS;
	}
	
	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		PluginIOHyperstack createdOutput =
				new PluginIOHyperstack("Empty", getOutputWidth(getInput()), getOutputHeight(getInput()),
						getOutputDepth(getInput()), getOutputNChannels(getInput()),
						getOutputNTimePoints(getInput()), PixelType.FLOAT_TYPE, false);

		setOutput("Default destination", createdOutput, true);

		ArrayList<PluginIOView> imagesToShow = new ArrayList<>();

		PluginIOHyperstackViewWithImagePlus display = null;
		if (!Utils.headless) {
			if (impForDisplay != null) {
				createdOutput.setImp(impForDisplay);
				display = impForDisplay;
			} else {
				display = new PluginIOHyperstackViewWithImagePlus(createdOutput.getName());
				createdOutput.setImp(display);
			}

			display.addImage(createdOutput);
			display.shouldUpdateRange = true;
			imagesToShow.add(display);
		}

		return imagesToShow;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination",
				new InputOutputDescription(null, null, null, InputOutputDescription.KEEP_IN_RAM,
						InputOutputDescription.NOT_SPECIFIED, true, false));
		return result;
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		return copyDimensionsOfInput ? ((IPluginIOImage) input).getDimensions().width :
			width;
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		return copyDimensionsOfInput ? ((IPluginIOImage) input).getDimensions().height :
			height;
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		return copyDimensionsOfInput ? ((IPluginIOImage) input).getDimensions().depth :
			depth;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return copyDimensionsOfInput ? ((IPluginIOImage) input).getDimensions().nTimePoints :
			1;
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return copyDimensionsOfInput ? ((IPluginIOImage) input).getDimensions().nChannels :
			nChannels;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) throws InterruptedException {
		return PixelType.FLOAT_TYPE;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {
		Utils.log("Empty image created", LogLevel.DEBUG);
		progressSetIndeterminateThreadSafe(r, false);
		progressSetValueThreadSafe(r, 100);		
	}

}
