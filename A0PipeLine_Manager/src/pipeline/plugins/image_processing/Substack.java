/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.IPluginIOSubstack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOSubstack;
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
 * Create substack view of subset of z slices from original; changes to substack affect original.
 * FIXME Does NOT work with input hyperstacks.
 *
 */
public class Substack extends FourDPlugin implements SpecialDimPlugin {

	@Override
	public String getToolTip() {
		return "Create substack view of subset of z slices from original; changes to substack "
				+ "affect original. Does NOT work on input hyperstacks";
	}

	@Override
	public String operationName() {
		return "Substack";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return DONT_ALLOCATE_OUTPUT_PIXELS;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	private int inputWidth, inputHeight;

	@ParameterInfo(userDisplayName = "Start index", floatValue = 0, noErrorIfMissingOnReload = false)
	private int startIndex;

	@ParameterInfo(userDisplayName = "Stop index", floatValue = 1, noErrorIfMissingOnReload = false)
	private int stopIndex;

	private PixelType pType = null;

	private boolean dimensionsComputed = false;

	@Override
	public void clearInputs() {
		super.clearInputs();
		dimensionsComputed = false;
	}

	@Override
	public void setInput(IPluginIO source) {
		super.setInput(source);
		dimensionsComputed = false;
	}

	private void computeOutputDimensions() {
		if (dimensionsComputed) {
			Utils.log("Skipping output dimension computing", LogLevel.VERBOSE_DEBUG);
			return;
		} else {
			Utils.log("Computing ouput dimensions", LogLevel.VERBOSE_DEBUG);
		}

		inputWidth = getImageInput().getDimensions().width;
		inputHeight = getImageInput().getDimensions().height;
		pType = getImageInput().getPixelType();
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		return (stopIndex - startIndex + 1);
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		computeOutputDimensions();
		return inputHeight;
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return 1;
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		computeOutputDimensions();
		return inputWidth;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {
		((IPluginIOSubstack) getOutput()).setStartIndex(startIndex);
		((IPluginIOSubstack) getOutput()).setStopIndex(stopIndex);
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
		Utils.log("Output depth " + getOutputDepth(getInput()), LogLevel.VERBOSE_DEBUG);

		IPluginIOHyperstack input = (IPluginIOHyperstack) getImageInput();
		if (!(input instanceof IPluginIOStack)) // choose just one channel
			input = input.getChannels().values().iterator().next();

		IPluginIOHyperstack createdOutput = PluginIOSubstack.getSubstack((IPluginIOStack) input, startIndex, stopIndex);

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
		createdOutput.setCalibration((Calibration) ((IPluginIOImage) getInput()).getCalibration().clone());

		return imagesToShow;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		computeOutputDimensions();
		return pType;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}
}
