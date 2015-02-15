/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.SpecialDimPlugin;
import pipeline.plugins.TwoDPlugin;

public class xyRescale extends TwoDPlugin implements SpecialDimPlugin {

	@Override
	public String operationName() {
		return "xy Rescale";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL + SPECIAL_DIMENSIONS;
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		return (int) (((IPluginIOHyperstack) input).getHeight() * ratioParam.getFloatValue());
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		return (int) (((IPluginIOHyperstack) input).getWidth() * ratioParam.getFloatValue());
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getDepth();
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getnChannels();
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null, new PixelType[] {
				PixelType.FLOAT_TYPE, PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.CUSTOM, true, false));
		return result;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return input.getPixelType();
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getnTimePoints();
	}

	private class RatioListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((float[]) ratioParam.getValue())[0] != rescaleFactor) {
				rescaleFactor = ((float[]) ratioParam.getValue())[0];
				pipelineCallback.clearDestinations(ourRow);
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private float rescaleFactor = 1f;

	private ParameterListener ratioListener0 = new RatioListener();
	private ParameterListener ratioListener1 = new ParameterListenerWeakRef(ratioListener0);

	private FloatParameter ratioParam = new FloatParameter("Factor", "xy rescaling factor", rescaleFactor, 0.0f, 5.0f,
			true, true, true, ratioListener1);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { ratioListener1, null };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		ratioParam = (FloatParameter) param[0];
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { ratioParam, null };
		return paramArray;
	}

	@Override
	public void runSlice(ImageProcessor input, ImageProcessor output, PreviewType previewType) {

		int newWidth = output.getWidth();
		int newHeight = output.getHeight();

		rescaleFactor = ratioParam.getFloatValue();

		for (int x = 0; x < newWidth; x++)
			for (int y = 0; y < newHeight; y++)
				output.setf(x, y, (float) input.getInterpolatedValue(x / rescaleFactor, y / rescaleFactor));
	}

	@Override
	public void postRun() {
		Calibration calib = (Calibration) getImageInput().getCalibration().clone();
		calib.pixelHeight = calib.pixelHeight / ratioParam.getFloatValue();
		calib.pixelWidth = calib.pixelWidth / ratioParam.getFloatValue();
		((IPluginIOImage) getOutput()).setCalibration(calib);
	}

}
