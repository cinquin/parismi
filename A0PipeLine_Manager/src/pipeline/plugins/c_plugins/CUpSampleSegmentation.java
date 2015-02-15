/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.CPluginIntrospectionAdapter;
import pipeline.plugins.PipelinePlugin;

public class CUpSampleSegmentation extends CPluginIntrospectionAdapter {

	@Override
	public String operationName() {
		return "C helper for upsampling protobuf segmentation";
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
	public String version() {
		return "1.0";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String getCommandName() {
		return "upsample";
	}

	@Override
	public void postRunUpdates() {
		PluginIOCells output = ((PluginIOCells) pluginOutputs.get("Seeds"));
		PluginIOCells input = ((PluginIOCells) pluginInputs.get("Seeds"));
		Calibration c = input.getCalibration();
		if (c != null) {
			Calibration newCalibration = (Calibration) c.clone();
			newCalibration.pixelDepth = c.pixelDepth * (((float) output.getDepth()) / input.getDepth());
			newCalibration.pixelWidth = c.pixelWidth * (((float) output.getWidth()) / input.getWidth());
			newCalibration.pixelHeight = c.pixelHeight * (((float) output.getHeight()) / input.getHeight());

			output.setCalibration(newCalibration);
		}
		output.restoreFromProtobuf();
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds", desc0);
		return result;
	}

	@Override
	public int getFlags() {
		return PipelinePlugin.PLUGIN_CREATES_OUTPUT_ITSELF;
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
		Utils.log("Creating seeds", LogLevel.DEBUG);
		initializeOutputs();
		PluginIOCells seeds = new PluginIOCells();
		ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
		view.setData(seeds);
		pluginOutputs.put("Seeds", seeds);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
	}

}
