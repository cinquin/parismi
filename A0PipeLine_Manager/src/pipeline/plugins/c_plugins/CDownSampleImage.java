/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import ij.measure.Calibration;

import java.util.HashMap;
import java.util.Map;

import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.plugins.CPluginIntrospectionAdapter;
import pipeline.plugins.SpecialDimPlugin;

public class CDownSampleImage extends CPluginIntrospectionAdapter implements SpecialDimPlugin {

	@Override
	public String operationName() {
		return "C helper for downsampling image";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String getCommandName() {
		return "downsample";
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc =
				new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
						InputOutputDescription.KEEP_IN_RAM, InputOutputDescription.CUSTOM, true, false);
		desc.useDefaultIfMatchingAbsent = true;
		result.put("Default destination", desc);
		return result;
	}

	@ParameterInfo(userDisplayName = "New x dimension", description = "x dimension of the resampled product image",
			floatValue = 1, permissibleFloatRange = { 1, 800 })
	private int dimX;

	@ParameterInfo(userDisplayName = "New y dimension", description = "y dimension of the resampled product image",
			floatValue = 1, permissibleFloatRange = { 1, 800 })
	private int dimY;

	@ParameterInfo(userDisplayName = "New z dimension", description = "z dimension of the resampled product image",
			floatValue = 1, permissibleFloatRange = { 1, 30 })
	private int dimZ;

	@Override
	public int getOutputDepth(IPluginIO input) {
		return dimZ;
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		return dimY;
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return 1;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		return dimX;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;
	}

	@Override
	public void postRunUpdates() {
		IPluginIOImage output = (IPluginIOImage) getOutput();
		IPluginIOImage input = getImageInput();
		Calibration c = input.getCalibration();
		if (c != null) {
			Calibration newCalibration = (Calibration) c.clone();
			newCalibration.pixelDepth =
					c.pixelDepth * (((float) output.getDimensions().depth) / input.getDimensions().depth);
			newCalibration.pixelWidth =
					c.pixelWidth * (((float) output.getDimensions().width) / input.getDimensions().width);
			newCalibration.pixelHeight =
					c.pixelHeight * (((float) output.getDimensions().height) / input.getDimensions().height);

			output.setCalibration(newCalibration);
		}
	}

}
