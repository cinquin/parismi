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
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.plugins.ThreeDPlugin;

public class NormalizeImage extends ThreeDPlugin {

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

	@ParameterInfo(userDisplayName = "Percentile", changeTriggersUpdate = false, changeTriggersLiveUpdates = false,
			floatValue = 95, permissibleFloatRange = { 0, 100 })
	private float percentile;

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

		long nPixels = input.getDepth() * input.getHeight() * input.getWidth();

		if (nPixels > Integer.MAX_VALUE)
			throw new RuntimeException("Too many pixels");

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
		int percIndex = (int) ((nPixels * percentile) / 100);
		if (percIndex == nPixels)
			percIndex--;

		float norm = pixels[percIndex];

		if (norm == 0) {
			throw new PluginRuntimeException("Percentile is 0", true);
		}

		index = 0;
		for (int z = 0; z < input.getDepth(); z++) {
			for (int y = 0; y < input.getHeight(); y++) {
				for (int x = 0; x < input.getWidth(); x++) {
					output.setPixelValue(x, y, z, input.getFloat(x, y, z) / norm);
				}
			}
		}

	}

}
