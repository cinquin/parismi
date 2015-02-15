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

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.plugins.ThreeDPlugin;

public class Shift extends ThreeDPlugin {

	private class XYOffsetListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			float oldXOffset = xOffset;
			float oldYOffset = yOffset;
			float oldZOffset = zOffset;
			xOffset = ((int[]) xOffsetParam.getValue())[0];
			yOffset = ((int[]) yOffsetParam.getValue())[0];
			zOffset = ((int[]) zOffsetParam.getValue())[0];
			if ((pipelineCallback != null)
					&& ((xOffset != oldXOffset) || (yOffset != oldYOffset) || (zOffset != oldZOffset)))
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}

	}

	private ParameterListener offsetListener0 = new XYOffsetListener();
	private ParameterListener offsetListener1 = new ParameterListenerWeakRef(offsetListener0);

	private AbstractParameter xOffsetParam = new IntParameter("X offset",
			"Number of pixels by which to shift the image horizontally", 0, -15, 15, true, true, offsetListener1);
	private AbstractParameter yOffsetParam = new IntParameter("Y offset",
			"Number of pixels by which to shift the image vertically", 0, -15, 15, true, true, offsetListener1);
	private AbstractParameter zOffsetParam = new IntParameter("Z offset",
			"Number of pixels by which to shift the image vertically", 0, -15, 15, true, true, offsetListener1);

	private int xOffset, yOffset, zOffset;

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { offsetListener1, null };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray =
				{ xOffsetParam, new SplitParameter(new Object[] { yOffsetParam, zOffsetParam }) };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		xOffsetParam = param[0];
		if (param[1] instanceof IntParameter) {
			yOffsetParam = param[1];
			zOffsetParam =
					new IntParameter("Z offset", "Number of pixels by which to shift the image vertically", 0, -15, 15,
							true, true, offsetListener1);
		} else if (param[1] instanceof SplitParameter) {
			SplitParameter p = (SplitParameter) param[1];
			yOffsetParam = p.getParameterValue()[0];
			zOffsetParam = p.getParameterValue()[1];
		} else {
			throw new IllegalArgumentException("Wrong parameters to shift plugin");
		}

		xOffset = ((int[]) xOffsetParam.getValue())[0];
		yOffset = ((int[]) yOffsetParam.getValue())[0];
		zOffset = ((int[]) zOffsetParam.getValue())[0];
	}

	@Override
	public String operationName() {
		return "Shift";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return 0;
	}

	private static boolean inBounds(int x, int upperBound) {
		return ((x >= 0) && (x < upperBound));
	}

	public int run_slice(ImageProcessor ip, ImageProcessor dest, PreviewType previewType) {

		int width = ip.getWidth();
		int height = ip.getHeight();

		xOffset = ((int[]) xOffsetParam.getValue())[0];
		yOffset = ((int[]) yOffsetParam.getValue())[0];
		zOffset = ((int[]) zOffsetParam.getValue())[0];

		if (ip instanceof FloatProcessor) {
			float[] pixels = (float[]) ip.getPixels();
			@SuppressWarnings("unused")
			int p = 0;
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++, p++) {
					if (inBounds(x - xOffset, width) && inBounds(y - yOffset, height)) {
						dest.putPixelValue(x, y, pixels[(y - yOffset) * width + (x - xOffset)]);
					} else
						dest.putPixelValue(x, y, 0);
				}

		} else if (ip instanceof ByteProcessor) {
			byte[] pixels = (byte[]) ip.getPixels();
			@SuppressWarnings("unused")
			int p = 0;
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++, p++) {
					if (inBounds(x - xOffset, width) && inBounds(y - yOffset, height)) {
						dest.putPixelValue(x, y, pixels[(y - yOffset) * width + (x - xOffset)]);
					} else
						dest.putPixelValue(x, y, 0);
				}
		} else if (ip instanceof ShortProcessor) {
			short[] pixels = (short[]) ip.getPixels();
			@SuppressWarnings("unused")
			int p = 0;
			for (int y = 0; y < height; y++)
				for (int x = 0; x < width; x++, p++) {
					if (inBounds(x - xOffset, width) && inBounds(y - yOffset, height)) {
						dest.putPixelValue(x, y, pixels[(y - yOffset) * width + (x - xOffset)]);
					} else
						dest.putPixelValue(x, y, 0);
				}
		}

		return 0;
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

	@Override
	public void runChannel(IPluginIOStack input, IPluginIOStack output, ProgressReporter r, PreviewType previewType,
			boolean inputHasChanged) {

		int width = output.getWidth();
		int height = output.getHeight();

		PixelType pType = input.getPixelType();

		for (int slice = 0; slice < output.getDepth(); slice++) {
			int inputSlice = slice - zOffset;
			if ((inputSlice < 0) || (inputSlice >= input.getDepth())) {
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						output.setPixelValue(x, y, slice, 0);
					}
				}
				continue;
			}
			if (pType == PixelType.FLOAT_TYPE) {
				float[] pixels = (float[]) input.getPixels(inputSlice);
				@SuppressWarnings("unused")
				int p = 0;
				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++, p++) {
						if (inBounds(x - xOffset, width) && inBounds(y - yOffset, height)) {
							output.setPixelValue(x, y, slice, pixels[(y - yOffset) * width + (x - xOffset)]);
						} else
							output.setPixelValue(x, y, slice, 0);
					}

			} else if (pType == PixelType.BYTE_TYPE) {
				byte[] pixels = (byte[]) input.getPixels(inputSlice);
				@SuppressWarnings("unused")
				int p = 0;
				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++, p++) {
						if (inBounds(x - xOffset, width) && inBounds(y - yOffset, height)) {
							output.setPixelValue(x, y, slice, pixels[(y - yOffset) * width + (x - xOffset)]);
						} else
							output.setPixelValue(x, y, slice, 0);
					}
			} else if (pType == PixelType.SHORT_TYPE) {
				short[] pixels = (short[]) input.getPixels(inputSlice);
				@SuppressWarnings("unused")
				int p = 0;
				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++, p++) {
						if (inBounds(x - xOffset, width) && inBounds(y - yOffset, height)) {
							output.setPixelValue(x, y, slice, pixels[(y - yOffset) * width + (x - xOffset)]);
						} else
							output.setPixelValue(x, y, slice, 0);
					}
			} else
				throw new IllegalArgumentException("Unrecognized pixel type " + pType);
		}
	}

}
