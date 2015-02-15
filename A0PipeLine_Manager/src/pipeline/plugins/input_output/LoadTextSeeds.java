/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;
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
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.IntrospectionParameters.DropHandler;
import pipeline.misc_util.IntrospectionParameters.DropHandlerType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.cell_manipulation.SVMCellDetector;

/**
 * Load tab-delimited text file containing detected cells.
 *
 */
public class LoadTextSeeds extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Load tab-delimited text file containing detected cells";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "File name" };
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

	@ParameterType(printValueAsString = true)
	@DropHandler(type = DropHandlerType.KEEP_DIRECTORY)
	@ParameterInfo(userDisplayName = "Directory", directoryOnly = true, changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false, fileNameIncrementable = true)
	private String directory;

	@DropHandler(type = DropHandlerType.KEEP_FILENAME)
	@ParameterInfo(userDisplayName = "File name", changeTriggersUpdate = false, changeTriggersLiveUpdates = false,
			fileNameIncrementable = true, compactDisplay = true)
	private String fileName;

	@Override
	public String operationName() {
		return "Load Text File";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return 0;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		String fileNameString =
				FileNameUtils.removeIncrementationMarks(directory + Utils.fileNameSeparator
						+ FileNameUtils.removeIncrementationMarks(fileName));

		SVMCellDetector.readTextFileIntoCells(new File(fileNameString), null, (PluginIOCells) getOutput("Seeds"));
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
