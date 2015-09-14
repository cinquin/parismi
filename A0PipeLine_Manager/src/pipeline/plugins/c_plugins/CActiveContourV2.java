/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import ij.ImagePlus;
import ij.measure.Calibration;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.ExternalCallToLibScalingParams;
import pipeline.plugins.MouseEventPlugin;

public class CActiveContourV2 extends ExternalCallToLibScalingParams implements MouseEventPlugin {

	@ParameterInfo(userDisplayName = "Largest cell radius (pixels)", description = "Window size", floatValue = 20,
			permissibleFloatRange = { 1, 50 })
	protected int windowHalfSize;

	@ParameterInfo(userDisplayName = "Runtime", description = "Time active contours run for", floatValue = 200,
			permissibleFloatRange = { 1, 50 })
	protected int runTime;

	@ParameterInfo(userDisplayName = "Time step", description = "Time step", floatValue = 0.1f,
			permissibleFloatRange = { 1, 50 })
	protected float timeStep;

	@ParameterInfo(userDisplayName = "Sussman interval", description = "Reinitialization time", floatValue = 200,
			permissibleFloatRange = { 1, 50 })
	protected int sussmanInterval;

	@ParameterInfo(userDisplayName = "Intensity interaction",
			description = "Strength with which image slows down active contour growth", floatValue = 1,
			permissibleFloatRange = { 1, 50 })
	protected float intensityInteraction;

	@ParameterInfo(userDisplayName = "Gradient interaction",
			description = "Strength with which image gradient slows down active contour growth", floatValue = 1,
			permissibleFloatRange = { 1, 50 })
	protected float gradientInteraction;

	@ParameterInfo(userDisplayName = "Curvature interaction",
			description = "Strength with which image curvature slows down active contour growth", floatValue = 1,
			permissibleFloatRange = { 1, 50 })
	protected float curvatureInteraction;

	@ParameterInfo(userDisplayName = "Seed size", description = "?", floatValue = 1, permissibleFloatRange = { 1, 50 })
	protected int seedSize;

	@ParameterInfo(userDisplayName = "Use cell specific params",
			description = "Look for overriding parameters in seeds and use them", stringValue = "FALSE")
	protected boolean useCellSpecificParams;

	@ParameterInfo(
			userDisplayName = "Narrow band threshold",
			noErrorIfMissingOnReload = true,
			floatValue = 99999999,
			description = "Active contour updates only occur in regions where abs(Phi) is lower than this threshold; lower this value to speed up active contours, but watch out for funny behavior.",
			stringValue = "FALSE")
	protected float narrowBandThreshold;

	@Override
	// For now, don't use an extra input argument as an argument to the program call
	public void loadExtraInputArgsForEstablishment(List<String> args) {
	}

	@Override
	// For this particular plugin, we only want to pass the list of seeds on the very first call
	public void extraArgsForRunLoop(List<String> args, boolean firstLoop) {
		if (firstLoop) {
			args.add("Seeds");
		}
	}

	@Override
	public String operationName() {
		return "C helper for running active contours";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String getCommandName() {
		return "active_contours";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Preexisting segmentation image", "Preexisting segmentation protobuf", "Seeds", "y_eig",
				"x_eig", "z_eig" };
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
		return new String[] { "Seeds" };
	}

	private void sendSeedsToGetMoreWork() {
		ArrayList<String> arguments = new ArrayList<>(10);
		String doLoopParameter;
		if (pipelineCallback.keepCProgramAlive(ourRow)) {
			doLoopParameter = "1 ";
		} else {
			doLoopParameter = "0 ";
		}
		arguments.add(doLoopParameter);// Keep external utility looping, waiting for more work
		arguments.add("0");// Output whole stack
		arguments.add("Seeds");
		arguments.addAll(Arrays.asList(getExtraParams()));

		Utils.log("adding/removing seed with arguments " + arguments.toString(), LogLevel.DEBUG);
		link.run(arguments.toArray(new String[0]), true, true, progressReporter, null);
		pluginInputs.remove("Seeds");

		PluginIOCells outputSeeds = (PluginIOCells) pluginOutputs.get("Seeds");
		if (outputSeeds != null) {
			if (outputSeeds.getCalibration() == null) {
				IPluginIOImage preprocessed = getImageInput();
				outputSeeds.setCalibration((Calibration) preprocessed.getCalibration().clone());
			}
		}
	}

	@Override
	public int mouseClicked(PluginIO clickedPoints, boolean inputHasChanged, MouseEvent generatingEvent)
			throws InterruptedException {
		Utils.log("mouseclicked link is " + link + " and plugin " + this + " at time " + System.currentTimeMillis(),
				LogLevel.DEBUG);
		if (!(clickedPoints instanceof PluginIOCells)) {
			throw new RuntimeException("Expected a list of clicked points at input to MouseClicked but got "
					+ clickedPoints);
		}

		reestablishLink(MouseEventPlugin.PROGRAM_LAUNCHED_IN_RESPONSE_TO_CLICK);

		pluginInputs.put("Preexisting segmentation protobuf", pluginOutputs.get("Seeds"));
		pluginInputs.put("Seeds", clickedPoints);

		if (pluginInputs.get("Preexisting segmentation") != null) {

		}

		Utils.log("Calling get more work at time " + System.currentTimeMillis(), LogLevel.DEBUG);
		sendSeedsToGetMoreWork();
		Utils.log("Active contour library call returned at time " + System.currentTimeMillis(), LogLevel.DEBUG);
		ImagePlus imp = ((IPluginIOImage) getOutput()).getImp().imp;

		if (imp != null) {
			int currentSlice = imp.getCurrentSlice() / imp.getNChannels();
			if (imp.getChannel() < imp.getNChannels())
				currentSlice++;
			imp.setPosition(imp.getChannel(), currentSlice + 1, imp.getFrame());
			imp.setPosition(imp.getChannel(), currentSlice, imp.getFrame());
		} else
			Utils.log("Null imp in active contours", LogLevel.DEBUG);

		Utils.log("Active contour output image updated at time " + System.currentTimeMillis(), LogLevel.DEBUG);
		return 0;
	}

	@Override
	public List<String> getInputArgs() {
		ArrayList<String> inputArgs = new ArrayList<>(6);

		inputArgs.add("Default source");
		loadExtraInputArgsForEstablishment(inputArgs);
		padWithDummyInputFileNames(inputArgs);

		if (getInputs().containsKey("Preexisting segmentation protobuf"))
			inputArgs.add("Preexisting segmentation protobuf");
		else
			inputArgs.add("0");
		for (int i = 0; i < numberProtobufInputFiles - 1; i++) {
			inputArgs.add("0");
		}

		return inputArgs;
	}

	private ProgressReporter progressReporter;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) throws InterruptedException {
		progressReporter = p;

		Utils.log("runchannel call link is " + link + " and plugin " + this, LogLevel.DEBUG);

		// If there is protobuf data associated with our output, clear it and the contents of the image so that we don't
		// end up with a duplicate set of segmentations
		if (output.getProperty("Protobuf") != null) {
			output.setProperty("Protobuf", null);
			output.clearPixels();
		}

		super.runChannel(input, output, p, previewType, inputHasChanged);
		pluginInputs.clear();
		Utils.log("runchannel return link is " + link, LogLevel.DEBUG);
	}

	@Override
	public void postRunUpdates() {
		super.postRunUpdates();
	}

	@Override
	public void postGetMoreWorkUpdates() {
		((PluginIOCells) getOutput("Seeds")).restoreFromProtobuf();
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		// TODO THIS WILL NEED UPDATING; WHAT INPUT SHOULD THE DEFAULT OUTPUT BE MATCHED WITH TO GET DIMENSIONS?
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));

		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds", desc0);

		return result;
	}

	@Override
	public boolean createOutput(InputOutputDescription desc, List<PluginIOView> views) {
		if ("Seeds".equals(desc.name)) {
			Utils.log("Creating Seeds output for CActiveContours", LogLevel.DEBUG);
			initializeOutputs();
			PluginIOCells seeds = new PluginIOCells();
			ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
			view.setData(seeds);
			pluginOutputs.put("Seeds", seeds);
			return true;
		}
		return false;
	}

	@Override
	public void processClicks() {
		// Nothing to do: we process clicks as we get them
	}

}
