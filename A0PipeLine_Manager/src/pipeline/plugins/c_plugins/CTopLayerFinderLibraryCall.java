/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.PipelinePlugin;
import pipeline.plugins.SpecialDimPlugin;

public class CTopLayerFinderLibraryCall extends ExternalCallToLibrary implements SpecialDimPlugin {

	@Override
	public String getTIFFType() {
		return "32";
	}

	@Override
	public String operationName() {
		return "C helper for finding cells on top of the gonad";
	}

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

	void initializeParams() {
	}

	public CTopLayerFinderLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "top_layer";
	}

	@Override
	public void postRunUpdates() {
		((PluginIOCells) pluginOutputs.get("Seeds")).restoreFromProtobuf();
	}

	/*
	 * @Override
	 * public String[] getInputLabels() {
	 * return new String [] {"r.tif", "DAPI.tif","Seeds"};
	 * }
	 * 
	 * @Override
	 * public String[] getOutputLabels() {
	 * return new String [] {"Seeds","histLCG","histr","histDAPI"};
	 * }
	 */

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

	@Override
	public int getOutputDepth(IPluginIO input) {
		return ((PluginIOCells) pluginInputs.get("Seeds")).getDepth();
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		return ((PluginIOCells) pluginInputs.get("Seeds")).getHeight();
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
		return ((PluginIOCells) pluginInputs.get("Seeds")).getWidth();
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;// TODO Update this when we handle non-float images
	}
}
