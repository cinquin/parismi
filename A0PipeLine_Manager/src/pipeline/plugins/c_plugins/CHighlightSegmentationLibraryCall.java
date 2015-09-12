/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.awt.AWTEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOListener;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TextParameter;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.MouseEventPlugin;
import pipeline.plugins.PluginInfo;
import pipeline.plugins.SpecialDimPlugin;

@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "DisplaySolidSegmentation, "
		+ "DisplaySeedFieldValue, or DisplaySegmentationPerimeter")
public class CHighlightSegmentationLibraryCall extends ExternalCallToLibrary implements PluginIOListener,
		SpecialDimPlugin, MouseEventPlugin {

	private @NonNull String colorWithField = "";

	private class MethodListener extends ParameterListenerAdapter {

		@SuppressWarnings("null")
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (stillChanging)
				return;
			if (!(param1.getValue().equals(colorWithField))) {
				colorWithField = (String) param1.getValue();
				PluginIOCells input = (PluginIOCells) pluginInputs.get("Seeds");
				if (input != null) {
					List<ListOfPointsView<?>> views = input.getViews();
					for (ListOfPointsView<?> view : views) {
						view.setFieldForColoring(colorWithField);
					}
				}
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener methodListener0 = new MethodListener();
	private ParameterListener methodListener1 = new ParameterListenerWeakRef(methodListener0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { methodListener0, null };// We do NOT want a weak listener because we need to
		// stay around after the plugin has first been run from the pipeline
	}

	@Override
	public String operationName() {
		return "C helper for highlighting segmentation";
	}

	@Override
	public String version() {
		return "1.0";
	}

	void initializeParams() {
		param1 =
				new TextParameter("Color with", "Name of field to use for coloring", "seed_x", true, methodListener1,
						null);
		param2 = new SplitParameter(new Object[] { fileName, workingDirectory });
	}

	public CHighlightSegmentationLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "proto2image";
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
	public String[] getExtraParams() {
		String choice = (String) param1.getValue();
		return new String[] { choice };// No need for "Seeds"
	}

	@Override
	public void setParameters(AbstractParameter[] params) {
		param1 = params[0];
		param2 = params[1];
		SplitParameter fileParams = (SplitParameter) param2;
		fileName = (AbstractParameter) ((Object[]) fileParams.getValue())[0];
		workingDirectory = (AbstractParameter) ((Object[]) fileParams.getValue())[1];
	}

	@Override
	public void postRunUpdates() {
		// Register the plugin as a listener so we can update the display depending on
		// what seeds and color scheme the user selects
		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		if (seeds == null)
			throw new IllegalStateException("No registered seeds input to highlight");
		seeds.addListener(this);// not weakRefToThis so we stick around

		// See if the user wants us to save the protobuf file

		@SuppressWarnings("null")
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + "/"
						+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));
		File directory = new File((String) workingDirectory.getValue());
		if (!(directory.exists() && directory.isDirectory())) {
			Utils.log("Directory " + workingDirectory.getValue()
					+ " does not exist or is not a directory; not saving seeds", LogLevel.WARNING);
			return;
		}

		try (FileOutputStream fos = new FileOutputStream(fileNameString)) {
			fos.write(seeds.asProtobufBytes());
		} catch (IOException e) {
			Utils.printStack(e);
		}
	}

	@Override
	public void pluginIOValueChanged(boolean stillChanging, IPluginIO pluginIOWhoseValueChanged) {
		// Do nothing: let the pipeline call runChannel when it thinks it's time
		Utils.log("value changed", LogLevel.VERBOSE_VERBOSE_DEBUG);
	}

	@Override
	public void pluginIOViewEvent(final PluginIOView trigger, final boolean stillChanging, AWTEvent event) {
		Utils.log("VIEW EVENT", LogLevel.DEBUG);
		PluginIOCells cells = (PluginIOCells) ((ListOfPointsView<?>) trigger).getSelectedCells();
		String colorSelection = ((ListOfPointsView<?>) trigger).getFieldForColoring();
		if (colorSelection != null) {
			param1.setValue(colorSelection);
			param1.fireValueChanged(false, true, false);
		}
		cells.updatesCanBeCoalesced = true;
		pipelineCallback.passClickToRow(ourRow, cells, false, false);
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory",
			"Directory to save measurements in", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);

	@Override
	public int mouseClicked(PluginIO clickedPoints, boolean inputHasChanged, MouseEvent generatingEvent)
			throws InterruptedException {
		pluginInputs.put("Seeds", clickedPoints);
		runChannel(input, output, null, null, true);
		return 0;
	}

	@Override
	public void processClicks() {
		// Nothing to do: we process clicks as we get them
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;// TODO Update this when we handle non-float images
	}

}
