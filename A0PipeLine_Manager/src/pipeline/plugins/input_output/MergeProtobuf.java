/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.ProgressSubrange;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;

public class MergeProtobuf extends MergeFiles {

	@Override
	public String getToolTip() {
		return "Concatenate .proto files whose path is given a path that can contain wildcards, and "
				+ "create a new mergeField ID to distinguish cells. Note that SeedID is not changed and "
				+ "therefore potentially not unique after merge.";
	}

	@Override
	protected void readInputFileHook(IPluginIO input) {
		List<String> names = new ArrayList<>();
		names.addAll(Arrays.asList(((MultiListParameter) getParameter("fieldsToMerge")).getChoices()));
		((PluginIOCells) input).getQuantifiedPropertyNames().stream().filter(s -> !names.contains(s)).forEach(
				names::add);
		((MultiListParameter) getParameter("fieldsToMerge")).setChoices(names.toArray(new String[] {}));
		getParameter("fieldsToMerge").fireValueChanged(false, true, false);
	}

	@ParameterInfo(userDisplayName = "Fields to merge", changeTriggersLiveUpdates = false, changeTriggersUpdate = false)
	@ParameterType(parameterType = "MultiList")
	private int[] fieldsToMerge;

	@ParameterInfo(userDisplayName = "Fill missing fields with 0", changeTriggersLiveUpdates = false,
			changeTriggersUpdate = false, noErrorIfMissingOnReload = true)
	private boolean fillMissingWithZero;

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Cells";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Cells", desc0);
		return result;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Cells" };
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {

		PluginIOCells seeds = new PluginIOCells();
		ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
		view.setData(seeds);
		pluginOutputs.put("Seeds", seeds);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		ProgressSubrange prog = new ProgressSubrange(r, 2);
		List<IPluginIO> inputCells = openInputFiles(prog);
		prog.nextStep();

		if (inputCells.size() == 0) {
			throw new PluginRuntimeException("No selected files to merge in MergeProtobuf",
					new IllegalArgumentException(), true);
		}

		PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Seeds");
		inputCells.get(0).copyInto(outputCells);
		outputCells.clear();

		String[] inputNames = ((MultiListParameter) getParameter("fieldsToMerge")).getChoices();
		int[] selectedFields = checkSelection(fieldsToMerge, inputNames.length);

		if (selectedFields.length == 0) {
			throw new PluginRuntimeException("No selected fields to merge in MergeProtobuf",
					new IllegalArgumentException(), true);
		}

		outputCells.getQuantifiedPropertyNames().clear();

		for (int i : selectedFields) {
			outputCells.getQuantifiedPropertyNames().add(inputNames[i]);
		}

		outputCells.addQuantifiedPropertyName("Merge_ID");
		int mergeID = 0;
		int missingValues = 0;
		List<String> missing = new LinkedList<>();

		prog.setMin(0);
		prog.setMax(inputCells.size());

		for (IPluginIO cells_ : inputCells) {
			PluginIOCells cells = (PluginIOCells) cells_;
			for (ClickedPoint p : cells.getPoints()) {
				ClickedPoint pCloned = (ClickedPoint) p.clone();
				pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
				pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();
				pCloned.quantifiedProperties.clear();
				for (int i : selectedFields) {
					String fieldName = inputNames[i];
					if (!p.hasQuantifiedProperty(fieldName)) {
						if (!fillMissingWithZero) {
							String message = "Missing property " + fieldName + " in file " + mergeID;
							throw new PluginRuntimeException(message, new IllegalArgumentException(), true);
						}
						if (!missing.contains(fieldName))
							missing.add(fieldName);
						pCloned.quantifiedProperties.add(0f);
						missingValues++;
					} else {
						pCloned.quantifiedProperties.add(p.getQuantifiedProperty(inputNames[i]));
					}
				}
				pCloned.quantifiedProperties.add((float) mergeID);
				outputCells.addDontFireValueChanged(pCloned);
			}
			prog.setValue(mergeID);
			mergeID++;
		}

		outputCells.fireValueChanged(false, false);

		if (missingValues > 0) {
			Utils.displayMessage("There were " + missingValues + " missing values replaced by 0, corresponding to "
					+ Utils.printStringArray(missing), true, LogLevel.WARNING);
		}

	}

	@Override
	public String operationName() {
		return "Merge cells";
	}

	@Override
	public String version() {
		return "1.0";
	}

}
