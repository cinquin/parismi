/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIONumber;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;

public class NormalizeImage extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String operationName() {
		return "Normalize image";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return 0;
	}
	
	@ParameterInfo(userDisplayName = "Read 5th and 95th percentiles from output of CellBallQuantify plugin",
			changeTriggersUpdate = false, changeTriggersLiveUpdates = false,
			stringValue = "TRUE")
	private boolean readPercentiles;

	@ParameterInfo(userDisplayName = "High percentile", changeTriggersUpdate = true, changeTriggersLiveUpdates = true,
			floatValue = 95, permissibleFloatRange = { 0, 100 }, noErrorIfMissingOnReload = true)
	private float highPercentile;
	
	@ParameterInfo(userDisplayName = "Low percentile", changeTriggersUpdate = true, changeTriggersLiveUpdates = true,
			floatValue = 5, permissibleFloatRange = { 0, 100 }, noErrorIfMissingOnReload = true)
	private float lowPercentile;

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

		final float offset;
		final float scalingFactor;

		if (readPercentiles) {
			PluginIONumber f = (PluginIONumber) getInput("Fifth percentile");
			PluginIONumber fn = (PluginIONumber) getInput("Ninety-fifth percentile");
			offset = f.number.floatValue();
			if (f.number.floatValue() + offset <= 0) {
				throw new PluginRuntimeException("Ninety-fith percentile is not greater than fifth percentile: " +
						fn.number + " vs " + offset, true);
			}
			scalingFactor = 1f / (fn.number.floatValue() - offset);
			Utils.log("Offset: " + offset + "; scaling factor: " + scalingFactor, LogLevel.INFO);
		} else {
			if (lowPercentile >= highPercentile) {
				throw new PluginRuntimeException("Low percentile must be lower than high percentile but found " +
						lowPercentile + " and " + highPercentile, true);
			}
			if (highPercentile > 100) {
				throw new PluginRuntimeException("High percentile is greater than 100: " + highPercentile, true);
			}
			if (lowPercentile < 0) {
				throw new PluginRuntimeException("Low percentile is lower than 0: " + highPercentile, true);
			}
			
			long nPixels = input.getDepth() * input.getHeight() * input.getWidth();

			if (nPixels > Integer.MAX_VALUE) {
				throw new RuntimeException("Too many pixels");
			}

			float[] pixels = new float[(int) nPixels];

			int index = 0;

			for (int z = 0; z < input.getDepth(); z++) {
				for (int y = 0; y < input.getHeight(); y++) {
					for (int x = 0; x < input.getWidth(); x++) {
						pixels[index++] = input.getFloat(x, y, z);
					}
				}
			}

			Arrays.sort(pixels);
			int highPercIndex = (int) (((nPixels - 1d) * highPercentile) / 100);
			int lowPercIndex = (int) (((nPixels - 1d) * lowPercentile) / 100);

			offset = pixels[lowPercIndex];
			if (offset == pixels[highPercIndex]) {
				throw new PluginRuntimeException("Ninety-fith percentile is not greater than fifth percentile: " +
						pixels[highPercIndex] + " vs " + offset, true);
			}
			scalingFactor = 1f / (pixels[highPercIndex] - offset);
		}

		for (int z = 0; z < input.getDepth(); z++) {
			for (int y = 0; y < input.getHeight(); y++) {
				for (int x = 0; x < input.getWidth(); x++) {
					output.setPixelValue(x, y, z, (Math.max(0, input.getFloat(x, y, z) - offset)) * scalingFactor);
				}
			}
		}
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] {};
	}

	@Override
	public String[] getInputLabels() {
		return new String[] {"Fifth percentile", "Ninety-fifth percentile" };
	}

}
