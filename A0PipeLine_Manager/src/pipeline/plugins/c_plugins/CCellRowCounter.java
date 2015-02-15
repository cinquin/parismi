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

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.ComboBoxParameter;
import pipeline.plugins.CPluginIntrospectionAdapter;
import pipeline.plugins.PipelinePlugin;

public class CCellRowCounter extends CPluginIntrospectionAdapter {

	@Override
	public String operationName() {
		return "C helper for counting cell rows using Dijkstra distance";
	}

	@ParameterInfo(userDisplayName = "Cell row 1 labels", stringValue = "", stringChoices = { "" },
			description = "Name of field acting as label for cells in row 1")
	@ParameterType(parameterType = "ComboBox", printValueAsString = true)
	private String row1FieldName;

	@ParameterInfo(userDisplayName = "Store field", stringValue = "cell_row",
			description = "Name of field to store assigned cell row into")
	private String outputFieldName;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) throws InterruptedException {
		((ComboBoxParameter) getParameter("row1FieldName")).setChoices(((PluginIOCells) getInput())
				.getQuantifiedPropertyNames().toArray(new String[] {}));
		super.runChannel(input, output, p, previewType, inputHasChanged);
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

	@Override
	public String getCommandName() {
		return "cell_row_counter";
	}

	@Override
	public void postRunUpdates() {
		((PluginIOCells) pluginOutputs.get("Seeds")).restoreFromProtobuf();
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
