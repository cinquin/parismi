/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.util.HashMap;
import java.util.Map;

import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.SpecialDimPlugin;

public class CRayTraceFiller extends ExternalCallToLibrary implements SpecialDimPlugin {

	@Override
	public String operationName() {
		return "C helper for ray trace filling";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public CRayTraceFiller() {
	}

	@Override
	public String getCommandName() {
		return "rayTraceFiller";
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

	@Override
	public int getOutputDepth(IPluginIO input) {
		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		return seeds.getDepth();
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		return seeds.getHeight();
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
		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		return seeds.getWidth();
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;
	}

}
