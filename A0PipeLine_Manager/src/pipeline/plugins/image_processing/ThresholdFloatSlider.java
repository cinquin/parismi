/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.TwoDPlugin;

public class ThresholdFloatSlider extends TwoDPlugin {

	private boolean range_set;

	private class RangeListener extends ParameterListenerAdapter {

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			float[] values = (float[]) intRangeParam.getValue();
			float lowValue = values[0];
			float highValue = values[1];
			float lowLimit = values[2];
			float highLimit = values[3];

			switch (commandName) {
				case "Reset Min":
					lowLimit = (int) Utils.getMin((IPluginIOHyperstack) getImageInput());
					break;
				case "Reset Max":
					highLimit = (int) Utils.getMax((IPluginIOHyperstack) getImageInput());
					break;
				case "Reset Range":
					lowLimit = (int) Utils.getMin((IPluginIOHyperstack) getImageInput());
					highLimit = (int) Utils.getMax((IPluginIOHyperstack) getImageInput());
					break;
				default:
					throw new IllegalStateException("Unknown command name " + commandName);
			}
			if (lowValue < lowLimit)
				lowValue = lowLimit;
			if (lowValue > highLimit)
				highLimit = lowValue;
			if (highValue < lowLimit)
				lowLimit = highValue;
			if (highValue > highLimit)
				highValue = highLimit;

			float[] objAray = { lowValue, highValue, lowLimit, highLimit };
			intRangeParam.setValue(objAray);
			intRangeParam.fireValueChanged(false, true, true);
			Utils.log("Reset range to " + lowLimit + "--" + highLimit, LogLevel.DEBUG);

		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			range_set = true;
			float oldMin = minThreshold;
			float oldMax = maxThreshold;
			minThreshold = ((float[]) intRangeParam.getValue())[0];
			maxThreshold = ((float[]) intRangeParam.getValue())[1];
			if ((pipelineCallback != null) && ((minThreshold != oldMin) || (maxThreshold != oldMax)))
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}
	}

	private ParameterListener rangeListener0 = new RangeListener();
	private ParameterListener rangeListener1 = new ParameterListenerWeakRef(rangeListener0);

	private AbstractParameter intRangeParam = new FloatRangeParameter("Range to keep",
			"Anything outside this range will be set set to the minimum/maximum of the range.", 0, 50000, 0, 50000,
			true, true, rangeListener1, null);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { rangeListener1, getParameterListenersAsSplit() };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { intRangeParam, getParametersAsSplit() };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		super.setParameters(param);
		intRangeParam = param[0];
		minThreshold = ((float[]) intRangeParam.getValue())[0];
		maxThreshold = ((float[]) intRangeParam.getValue())[1];
		String s = "updated min and max to " + minThreshold + " " + maxThreshold;
		Utils.log(s, LogLevel.VERBOSE_DEBUG);
	}

	@Override
	public void setInput(IPluginIO source) {
		super.setInput(source);
		float theMin, theMax;
		theMin = (int) Utils.getMin((IPluginIOStack) getImageInput());
		theMax = (int) Utils.getMax((IPluginIOStack) getImageInput());
		// Utils.log("Max is "+theMax,LogLevel.VERBOSE_DEBUG);
		if ((!range_set) && (!(intRangeParam).hasBeenSet)) {
			float[] objArray = (float[]) intRangeParam.getValue();
			objArray[2] = theMin;
			objArray[3] = theMax;
			intRangeParam.setValue(objArray);
		}
	}

	@Override
	public String operationName() {
		return "Threshold Float Slider";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return 0;
	}

	private float minThreshold, maxThreshold;

	@ParameterInfo(booleanValue = false, userDisplayName = "Clip pixels outside range to 0",
			noErrorIfMissingOnReload = true)
	private boolean clipTo0;

	@Override
	public void runSlice(ImageProcessor ip, ImageProcessor dest, PreviewType previewType) {

		int width = ip.getWidth();
		Rectangle roi = ip.getRoi();

		float minValue = clipTo0 ? 0 : minThreshold;
		float maxValue = clipTo0 ? 0 : maxThreshold;

		if (ip instanceof FloatProcessor) {
			float[] float_pixels = (float[]) ip.getPixels();
			// float [] output_pixels=(float[]) dest.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++)
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
					dest.putPixelValue(x, y,
							(operation(float_pixels[p], minThreshold, maxThreshold, minValue, maxValue)));

		} else if (ip instanceof ByteProcessor) {
			byte[] byte_pixels = (byte[]) ip.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++)
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)

					dest.putPixelValue(x, y, ((byte) operation(byte_pixels[p] & 0xff, minThreshold, maxThreshold,
							minValue, maxValue)) & 0xff);
		} else if (ip instanceof ShortProcessor) {
			short[] short_pixels = (short[]) ip.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++)
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
					dest.putPixelValue(x, y, ((short) operation(short_pixels[p] & 0xffff, minThreshold, maxThreshold,
							minValue, maxValue)) & 0xffff);
		}
	}

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
		result.put("Default destination", new InputOutputDescription(null, null, new PixelType[] {
				PixelType.FLOAT_TYPE, PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}

	@SuppressWarnings("static-method")
	float operation(float pixelValue, float lowThreshold, float highThreshold, float minValue, float maxValue) {
		if (pixelValue < lowThreshold) {
			return minValue;
		} else if (pixelValue > highThreshold) {
			return maxValue;
		} else
			return pixelValue;
	}

}
