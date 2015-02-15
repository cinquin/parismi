/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.MouseEventPlugin;
import pipeline.plugins.SpecialDimPlugin;

public class CLabelCellLibraryCall extends ExternalCallToLibrary implements MouseEventPlugin, SpecialDimPlugin {

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
	public int getOutputWidth(IPluginIO input) {
		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		return seeds.getWidth();
	}

	private void initializeParams() {
		param1 =
				new ComboBoxParameter("Field to store label in (MUST start with userCell)", "",
						new String[] { "userCell label" }, "userCell label", true, null);
		param2 =
				new ComboBoxParameter("Label", "", new String[] { "MR/TZ", "GLD-1 border", "Last PH3", "Distal end",
						"Distal tip cell", "Crescent", "Metaphase plate", "PH3" }, "Crescent", true, null);
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		param1 = param[0];
		param2 = param[1];
	}

	@Override
	// for this particular plugin, we only want to pass the list of seeds on the very first call
			public
			void extraArgsForRunLoop(List<String> args, boolean firstLoop) {
		if (firstLoop) {
			args.add("Seeds2");
		}
	}

	@Override
	public String operationName() {
		return "C helper for labeling cells";
	}

	@Override
	public String version() {
		return "1.0";
	}

	public CLabelCellLibraryCall() {
		Utils.log("constructor without listener", LogLevel.DEBUG);
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "user_labeling";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
		// default destination so that the plugin takes the existing output as a preexisting segmentation
		// Seeds will not actually be read from the arguments in the first call
		// but we need it there so the reference is resolved
	}

	@Override
	public String[] inputsToHideFromExtraArgs() {
		return new String[] { "Seeds" };// these will go through the first getMoreWork call
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds2" };
	}

	@Override
	public int mouseClicked(PluginIO clickedPoints, boolean inputHasChanged, MouseEvent generatingEvent)
			throws InterruptedException {
		Utils.log("mouseclicked link is " + link + " and plugin " + this, LogLevel.VERBOSE_DEBUG);
		if (!(clickedPoints instanceof PluginIOCells)) {
			throw new RuntimeException("Expected a list of clicked points at input to MouseClicked but got "
					+ clickedPoints);
		}

		reestablishLink(MouseEventPlugin.PROGRAM_LAUNCHED_IN_RESPONSE_TO_CLICK);

		for (ClickedPoint point : ((PluginIOCells) clickedPoints).getPoints()) {

			ArrayList<String> arguments = new ArrayList<>(10);
			String doLoopParameter;
			if (pipelineCallback.keepCProgramAlive(ourRow)) {
				doLoopParameter = "1 ";
			} else {
				doLoopParameter = "0 ";
			}
			arguments.add(doLoopParameter);// keep C program looping, waiting for stdin input
			arguments.add(Integer.toString(-1));// output whole stack

			arguments.add("" + point.x);
			arguments.add("" + point.y);
			arguments.add("" + point.z);
			arguments.add(((ComboBoxParameter) param1).getSelection());// field name
			arguments.add(((ComboBoxParameter) param2).getSelection());// label name

			link.run(arguments.toArray(new String[0]), true, true, progressReporter, this);
		}
		return 0;
	}

	private ProgressReporter progressReporter;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) {
		this.progressReporter = p;
		PluginIOCells destinationSeeds = (PluginIOCells) pluginOutputs.get("Seeds2");
		destinationSeeds.setProperty("Protobuf", pluginInputs.get("Seeds").asProtobufBytes());
	}

	@Override
	public void postGetMoreWorkUpdates() {
		((PluginIOCells) getOutput("Seeds2")).restoreFromProtobuf();
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();

		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds2";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds2", desc0);

		return result;
	}

	@Override
	public boolean createOutput(InputOutputDescription desc, List<PluginIOView> views) {
		if ("Seeds2".equals(desc.name)) {
			Utils.log("Creating Seeds output for CActiveContours", LogLevel.DEBUG);
			initializeOutputs();
			PluginIOCells seeds = new PluginIOCells();
			ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
			view.setData(seeds);
			// view.show();
			pluginOutputs.put("Seeds2", seeds);
			return true;
		}
		return false;
	}

	@Override
	public void processClicks() {
		// Nothing to do: we process clicks as we get them
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;// TODO Update this when we handle non-float images
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}
}
