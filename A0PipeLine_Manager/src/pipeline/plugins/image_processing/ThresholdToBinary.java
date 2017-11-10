/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.TwoDPlugin;

public class ThresholdToBinary extends TwoDPlugin {

	private boolean range_set;

	private class RangeListener extends ParameterListenerAdapter {
		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			int[] values = (int[]) intrangeparam.getValue();
			int lowValue = values[0];
			int highValue = values[1];
			int lowLimit = values[2];
			int highLimit = values[3];

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
				lowLimit = lowValue;// =lowLimit;
			if (lowValue > highLimit)
				highLimit = lowValue;// =highLimit;
			if (highValue < lowLimit)
				lowLimit = highValue;// =lowLimit;
			if (highValue > highLimit)
				highLimit = highValue;// =highLimit;

			int[] objAray = { lowValue, highValue, lowLimit, highLimit };
			intrangeparam.setValue(objAray);
			intrangeparam.fireValueChanged(false, true, true);

		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			range_set = true;
			float oldMin = minThreshold;
			float oldMax = maxThreshold;
			minThreshold = ((int[]) intrangeparam.getValue())[0];
			maxThreshold = ((int[]) intrangeparam.getValue())[1];
			if ((pipelineCallback != null) && ((minThreshold != oldMin) || (maxThreshold != oldMax))) {
				pipelineCallback.parameterValueChanged(ourRow, parameterWhoseValueChanged, false);
			}
		}
	}

	private ParameterListener rangeListener0 = new RangeListener();
	private ParameterListener rangeListener1 = new ParameterListenerWeakRef(rangeListener0);

	private AbstractParameter intrangeparam = new IntRangeParameter("Range to keep",
			"Anything within this range will be set to 255, anything out to 0.", 0, 50000, 0, 50000, true, true,
			rangeListener1, null);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { rangeListener1, null };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { intrangeparam, null };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		intrangeparam = param[0];
		minThreshold = ((int[]) intrangeparam.getValue())[0];
		maxThreshold = ((int[]) intrangeparam.getValue())[1];
		intrangeparam.addPluginListener(rangeListener1);
	}

	@Override
	public void setInput(IPluginIO source) {
		super.setInput(source);
		int theMin, theMax;
		theMin = (int) Utils.getMin((IPluginIOStack) getImageInput());
		theMax = (int) Utils.getMax((IPluginIOStack) getImageInput());

		if ((!range_set) && (!(intrangeparam).hasBeenSet)) {
			int[] objArray = (int[]) intrangeparam.getValue();
			objArray[2] = theMin;
			objArray[3] = theMax;
			intrangeparam.setValue(objArray);
		}
	}

	@Override
	public String operationName() {
		return "Threshold to binary";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY;
	}

	private float minThreshold, maxThreshold;

	@Override
	public void runSlice(ImageProcessor ip, ImageProcessor dest, PreviewType previewType) {

		int width = ip.getWidth();
		Rectangle roi = ip.getRoi();

		if (ip instanceof FloatProcessor) {
			float[] fp_pixels = (float[]) ip.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++)
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
					if (fp_pixels[p] < minThreshold) {
						dest.putPixelValue(x, y, 0);
					} else if (fp_pixels[p] > maxThreshold) {
						dest.putPixelValue(x, y, 0);
					} else
						dest.putPixelValue(x, y, 255);

		} else if (ip instanceof ByteProcessor) {
			byte[] fp_pixels = (byte[]) ip.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++) {
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++) {
					if ((fp_pixels[p] & 0xff) < minThreshold) {
						dest.putPixelValue(x, y, 0);
					} else if ((fp_pixels[p] & 0xff) > maxThreshold) {
						dest.putPixelValue(x, y, 0);
					} else
						dest.putPixelValue(x, y, 255);
				}
			}
		} else if (ip instanceof ShortProcessor) {
			// IJ.log("short processor");
			short[] fp_pixels = (short[]) ip.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++)
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
					if ((fp_pixels[p] & 0xffff) < minThreshold) {
						dest.putPixelValue(x, y, 0);
					} else if ((fp_pixels[p] & 0xffff) > maxThreshold) {
						dest.putPixelValue(x, y, 0f);
					} else
						dest.putPixelValue(x, y, 255);
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
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.BYTE_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}

}
