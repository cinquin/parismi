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
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.TextParameter;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.PluginInfo;

@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "CellBallQuantify")
public class CCellQuantificationLibraryCall extends ExternalCallToLibrary {

	private String method = "";

	private class MethodListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			Utils.log("Change callback with new combo box choice "
					+ (((ComboBoxParameter) param1).getSelectionIndex() - 1), LogLevel.DEBUG);
			if (!(((ComboBoxParameter) param1).getSelection().equals(method))) {
				method = ((ComboBoxParameter) param1).getSelection();
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener methodListener0 = new MethodListener();
	private ParameterListener methodListener1 = new ParameterListenerWeakRef(methodListener0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { methodListener0, methodListener0 };// We do NOT want a weak listener because we
		// need to stay around after the plugin has first been run from the pipeline
	}

	private void initializeParams() {
		param1 =
				new ComboBoxParameterPrintValueAsString("FULL_OR_PERIM", "", new String[] { "image_fullseg_coords",
						"image_perimseg_coords" }, "image_fullseg_coords", false, methodListener1);
		param2 = new TextParameter("Store field", "Name of field", "DAPI content", true, methodListener1, null);
	}

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
		return new String[] { "Seeds", "HistogramToIgnore" };
	}

	public CCellQuantificationLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "quantify";
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

		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));

		return result;
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
