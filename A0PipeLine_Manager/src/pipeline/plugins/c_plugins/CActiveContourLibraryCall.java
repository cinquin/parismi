/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import ij.ImagePlus;
import ij.measure.Calibration;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ActionParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.plugins.ExternalCallToLibScalingParams;
import pipeline.plugins.MouseEventPlugin;
import pipeline.plugins.PluginInfo;

@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "CActiveContourV2")
public class CActiveContourLibraryCall extends ExternalCallToLibScalingParams implements MouseEventPlugin,
		ParameterListener {
	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
	}

	/**
	 * {@inheritDoc} It is important to remove listeners so the instances of this plugin can die when required. If not
	 * they will build up,
	 * and might all respond to parameter callbacks when they should not.
	 */
	@Override
	public void cleanup() {
		super.cleanup();
		param1.removeListener(this);
		param2.removeListener(this);
	}

	private BooleanParameter accumulateResultsParameter;

	private FloatParameter emptyParam, timeStep, stepsBeforeReinit, intensityInteraction, gradientInteraction,
			curvatureConstraint, initialSeedRadius, d2, tMax3;

	private void initializeNewParams() {
		emptyParam = new FloatParameter("Unused", "Unused", 0, 0f, 0, true, true, true, genericListener1);
		timeStep =
				new FloatParameter("Time step", "Time step in active contour simulation", 0.1f, 0, 1, true, true, true,
						genericListener1);
		stepsBeforeReinit =
				new FloatParameter("Steps before reinit", "Number of time steps before re-initialization", 5, 1, 10,
						true, true, true, genericListener1);
		intensityInteraction =
				new FloatParameter("Intensity interaction",
						"Weight to place on pixel intensity interaction with active contour", 1, 0, 10, true, true,
						true, genericListener1);
		gradientInteraction =
				new FloatParameter("Gradient interaction",
						"Weight to place on pixel gradient interaction with active contour", 10, 0, 20, true, true,
						true, genericListener1);
		curvatureConstraint =
				new FloatParameter("Curvature constraint", "Weight to place on curvature constraints", 0.1f, 0, 1,
						true, true, true, genericListener1);
		initialSeedRadius =
				new FloatParameter("Initial radius", "Radius of initializing seed", 3, 1, 10, true, true, true,
						genericListener1);
	}

	private void initializeNewNewNewParams() {
		d2 =
				new FloatParameter("Gradient interaction for step 2",
						"Gradient interaction term for second run of active contour", 1, 1, 10, true, true, true,
						genericListener1);
		tMax3 =
				new FloatParameter("Runtime for step 2", "Runtime for second run of active contour", 1, 1, 10, true,
						true, true, genericListener1);
	}

	@Override
	protected void initializeParams() {
		super.initializeParams();
		contourRunTimeParameter =
				new FloatParameter("Active contour runtime", "How long to simulate active contours", contourRunTime,
						0f, 200f, true, true, true, contourRunTimeListener1);
		contourFreeExpansionTimeParameter =
				new FloatParameter("Free expansion time",
						"How long the active contour should expand, ignoring image features, after is has first run",
						contourFreeExpansionTime, 0f, 200f, true, true, true, contourFreeExpansionTimeListener1);
		windowHalfSizeParameter =
				new IntParameter(
						"Cell radius lower bound (pixels)",
						"Should be greater than cell radius (in pixels), but not too big to keep computation time down. Try increasing if active contours behave funny (they might be getting too close to the edges, creating boundary condition problems).",
						windowHalfSize, 0, 50, true, true, windowHalfSizeListener1);
		outputUpdateMethodParameter =
				new ComboBoxParameter("Output update method", "", new String[] { "", "Write full segmentation" },
						"Write full segmentation", false, outputUpdateMethodListener1);
		activeContourTypeParameter =
				new ComboBoxParameter("Active contour type", "", new String[] { "", "Default", "Ignore curvature",
						"Drosophila wing disc", "User-defined fields", "2-step active contour" }, "Default", false,
						activeContourTypeListener1);
		accumulateResultsParameter =
				new BooleanParameter("Accumulate results",
						"If set, new active contours are added to the ones previously computed", accumulateResults,
						true, accumulateResultsListener1);
		accumulateResultsParameter.dontPrintOutValueToExternalPrograms = true;
		saveResultsParameter =
				new ActionParameter("Save modifications (NOT IMPLEMENTED)",
						"Makes the modifications performed by clicking to add/remove/fuse contours permanent", true,
						saveButtonListener1);
		actionTypeParameter =
				new ComboBoxParameter("Click action", "", new String[] { "Create active contour",
						"Delete active contour", "Fuse active contours" }, "Create active contour", false,
						actionTypeListener1);
		actionTypeParameter.dontPrintOutValueToExternalPrograms = true;

		initializeNewParams();
		initializeNewNewNewParams();

		param1 =
				new SplitParameter(new Object[] { activeContourTypeParameter, accumulateResultsParameter,
						saveResultsParameter, actionTypeParameter });// new SplitParameter(new Object[]
																		// {activeContourTypeParameter,outputUpdateMethodParameter});
		param2 =
				new SplitParameter(new Object[] { windowHalfSizeParameter, contourRunTimeParameter,
						contourFreeExpansionTimeParameter, emptyParam, timeStep, stepsBeforeReinit,
						intensityInteraction, gradientInteraction, curvatureConstraint, initialSeedRadius, d2, tMax3,
						getParameter("xStretch"), getParameter("yStretch"), getParameter("zStretch"),
						getParameter("guessStretch") });
	}

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { splitA1, splitB1 };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		initializeParams();
		super.setParameters(((SplitParameter) param[1]).getParameterValue());

		Object[] splitParameters = (Object[]) param[0].getValue();
		activeContourTypeParameter = (ComboBoxParameter) splitParameters[0];
		accumulateResultsParameter = (BooleanParameter) splitParameters[1];
		Boolean newAccumulateResults = (Boolean) ((Object[]) accumulateResultsParameter.getValue())[0];
		if (newAccumulateResults != accumulateResults) {
			Utils.log("restarting because of a change in accumulate results", LogLevel.DEBUG);
			if (link != null)
				link.terminate(false);
			link = null;// program needs to be restarted with new arguments, because arguments depend on the value of
						// accumulateResults
		}
		accumulateResults = newAccumulateResults;

		saveResultsParameter = (ActionParameter) splitParameters[2];
		saveResultsParameter.addPluginListener(this);
		actionTypeParameter = (ComboBoxParameter) splitParameters[3];
		actionTypeParameter.addPluginListener(this);
		actionTypeParameter.dontPrintOutValueToExternalPrograms = true; // won't be necessary soon

		//actionType = (byte) actionTypeParameter.getSelectionIndex();
		splitParameters = (Object[]) param[1].getValue();

		contourRunTimeParameter = (FloatParameter) splitParameters[1];
		contourFreeExpansionTimeParameter = (FloatParameter) splitParameters[2];
		windowHalfSizeParameter = (IntParameter) splitParameters[0];

		if (splitParameters[3] != null) {
			emptyParam = (FloatParameter) splitParameters[3];
			timeStep = (FloatParameter) splitParameters[4];
			stepsBeforeReinit = (FloatParameter) splitParameters[5];
			intensityInteraction = (FloatParameter) splitParameters[6];
			gradientInteraction = (FloatParameter) splitParameters[7];
			curvatureConstraint = (FloatParameter) splitParameters[8];
			initialSeedRadius = (FloatParameter) splitParameters[9];
		} else
			initializeNewParams();

		if ((splitParameters.length > 10) && splitParameters[10] != null
				&& ("d2".equals(((AbstractParameter) splitParameters[10]).getUserDisplayName()))) {
			d2 = (FloatParameter) splitParameters[10];
			tMax3 = (FloatParameter) splitParameters[11];
		} else
			initializeNewNewNewParams();

		activeContourType = (Integer) (((Object[]) activeContourTypeParameter.getValue())[1]);
		outputUpdateMethod = (Integer) (((Object[]) outputUpdateMethodParameter.getValue())[1]);

		contourRunTime = ((float[]) contourRunTimeParameter.getValue())[0];
		contourFreeExpansionTime = ((float[]) contourFreeExpansionTimeParameter.getValue())[0];
		windowHalfSize = ((int[]) windowHalfSizeParameter.getValue())[0];

		param1 =
				new SplitParameter(new Object[] { activeContourTypeParameter, accumulateResultsParameter,
						saveResultsParameter, actionTypeParameter });
		param2 =
				new SplitParameter(new Object[] { windowHalfSizeParameter, contourRunTimeParameter,
						contourFreeExpansionTimeParameter, emptyParam, timeStep, stepsBeforeReinit,
						intensityInteraction, gradientInteraction, curvatureConstraint, initialSeedRadius, d2, tMax3,
						getParameter("xStretch"), getParameter("yStretch"), getParameter("zStretch"),
						getParameter("guessStretch") });
	}

	private ActionParameter saveResultsParameter;
	private ComboBoxParameter actionTypeParameter;

	private class ActionTypeListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			//actionType = (byte) actionTypeParameter.getSelectionIndex();
		}

	}

	private ParameterListener actionTypeListener0 = new ActionTypeListener();
	private ParameterListener actionTypeListener1 = new ParameterListenerWeakRef(actionTypeListener0);

	private String savedModificationsPath = null;

	private class SaveButtonListener extends ParameterListenerAdapter {
		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			// We might have been called as the dummy instance created by the shell that's not
			// tied to any real data; in that case, just return
			if (pluginOutputs == null)
				return;
			Utils.log("here", LogLevel.DEBUG);
			if (pluginOutputs.size() == 0)
				return;
			if (savedModificationsPath == null) {
				File f = null;
				try {
					f = File.createTempFile("savedEditedSegmentation", ".tiff");
					savedModificationsPath = f.getPath();
				} catch (IOException e) {
					Utils.printStack(e);
				}
			}
			try {
				ProcessBuilder pb =
						new ProcessBuilder("cp", getOutput().asFile(null, true).getAbsolutePath(),
								savedModificationsPath);// getStoredOutputFileName()
				pb.redirectErrorStream(true);
				Process process = pb.start();
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					Utils.printStack(e);
				}
			} catch (IOException | InterruptedException e) {
				Utils.printStack(e);
			}

		}

	}

	private ParameterListener saveButtonListener0 = new SaveButtonListener();
	private ParameterListener saveButtonListener1 = new ParameterListenerWeakRef(saveButtonListener0);

	private boolean dontTriggerPipelineUpdates = false;

	private float contourRunTime = 100f;
	private FloatParameter contourRunTimeParameter;

	private class ContourRunTimeListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (dontTriggerPipelineUpdates)
				return;
			if (((float[]) contourRunTimeParameter.getValue())[0] != contourRunTime) {
				contourRunTime = ((float[]) contourRunTimeParameter.getValue())[0];
				if (pipelineCallback != null)
					pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener contourRunTimeListener0 = new ContourRunTimeListener();
	private ParameterListener contourRunTimeListener1 = new ParameterListenerWeakRef(contourRunTimeListener0);

	private FloatParameter contourFreeExpansionTimeParameter;
	private float contourFreeExpansionTime = 0.0f;

	private class ContourFreeExpansionTimeListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (dontTriggerPipelineUpdates)
				return;
			if (((float[]) contourFreeExpansionTimeParameter.getValue())[0] != contourFreeExpansionTime) {
				contourFreeExpansionTime = ((float[]) contourFreeExpansionTimeParameter.getValue())[0];
				if (pipelineCallback != null)
					pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener contourFreeExpansionTimeListener0 = new ContourFreeExpansionTimeListener();
	private ParameterListener contourFreeExpansionTimeListener1 = new ParameterListenerWeakRef(
			contourFreeExpansionTimeListener0);

	private boolean accumulateResults = true;

	private class AccumulateResultsListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			Boolean newAccumulateResults = (Boolean) ((Object[]) accumulateResultsParameter.getValue())[0];
			if (newAccumulateResults != accumulateResults) {
				if (link != null)
					link.terminate(false);
				link = null;// program needs to be restarted with new arguments, because arguments depend on the value
							// of accumulateResults
			}
			accumulateResults = newAccumulateResults;
		}
	}

	private ParameterListener accumulateResultsListener0 = new AccumulateResultsListener();
	private ParameterListener accumulateResultsListener1 = new ParameterListenerWeakRef(accumulateResultsListener0);

	private class GenericListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (dontTriggerPipelineUpdates)
				return;
			if (pipelineCallback != null)
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}
	}

	private ParameterListener genericListener0 = new GenericListener();
	private ParameterListener genericListener1 = new ParameterListenerWeakRef(genericListener0);

	private int windowHalfSize = 20;
	private IntParameter windowHalfSizeParameter;

	private class WindowHalfSizeListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (dontTriggerPipelineUpdates)
				return;
			if (((int[]) windowHalfSizeParameter.getValue())[0] != windowHalfSize) {
				windowHalfSize = ((int[]) windowHalfSizeParameter.getValue())[0];
				if (pipelineCallback != null)
					pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener windowHalfSizeListener0 = new WindowHalfSizeListener();
	private ParameterListener windowHalfSizeListener1 = new ParameterListenerWeakRef(windowHalfSizeListener0);

	private int outputUpdateMethod = 2;
	private ComboBoxParameter outputUpdateMethodParameter;

	private class OutputUpdateMethodListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (dontTriggerPipelineUpdates)
				return;
			if (!((outputUpdateMethodParameter).getSelectionIndex() - 1 == outputUpdateMethod)) {
				outputUpdateMethod = (outputUpdateMethodParameter).getSelectionIndex() - 1;
				if (pipelineCallback != null)
					pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener outputUpdateMethodListener0 = new OutputUpdateMethodListener();
	private ParameterListener outputUpdateMethodListener1 = new ParameterListenerWeakRef(outputUpdateMethodListener0);

	private int activeContourType = 2;
	private ComboBoxParameter activeContourTypeParameter;

	private class ActiveContourTypeListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (!((activeContourTypeParameter).getSelectionIndex() - 1 == activeContourType)) {
				activeContourType = (activeContourTypeParameter).getSelectionIndex() - 1;
				// pipelineCallback.parameterValueChanged(ourRow);
			}
		}
	}

	private ParameterListener activeContourTypeListener0 = new ActiveContourTypeListener();
	private ParameterListener activeContourTypeListener1 = new ParameterListenerWeakRef(activeContourTypeListener0);

	private ParameterListener splitA0 = new SplitParameterListener(new ParameterListener[] {
			activeContourTypeListener1, accumulateResultsListener1, saveButtonListener1, actionTypeListener1 });
	private ParameterListener splitA1 = new ParameterListenerWeakRef(splitA0);
	private ParameterListener splitB0 = new SplitParameterListener(new ParameterListener[] { windowHalfSizeListener1,
			contourRunTimeListener1, contourFreeExpansionTimeListener1 });
	private ParameterListener splitB1 = new ParameterListenerWeakRef(splitB0);

	@Override
	// for now, don't use an extra input argument as an argument to the program call
	public void loadExtraInputArgsForEstablishment(List<String> args) {
	}

	@Override
	// for this particular plugin, we only want to pass the list of seeds on the very first call
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
		return "active_contours_deprecated";
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
		arguments.add(doLoopParameter);// keep C program looping, waiting for stdin input
		arguments.add(Integer.toString(-outputUpdateMethod));// output whole stack
		arguments.add("Seeds");
		arguments.add(Integer.toString(activeContourTypeParameter.getSelectionIndex()));
		arguments.add(Integer.toString(windowHalfSizeParameter.getintValue()));

		boolean setRuntimeFromSeeds = false;
		IPluginIO clickedPoints = pluginInputs.get("Seeds");
		if ((clickedPoints != null) && (clickedPoints instanceof PluginIOCells)) {
			ClickedPoint firstPoint = ((PluginIOCells) clickedPoints).getPoints().get(0);
			if (firstPoint != null) {
				if (firstPoint.contourRuntime > 0) {
					setRuntimeFromSeeds = true;
					arguments.add(Float.toString(firstPoint.contourRuntime));
				}
			}
		}

		if (!setRuntimeFromSeeds)
			arguments.add(Float.toString(contourRunTimeParameter.getFloatValue()));

		arguments.add(Float.toString(contourFreeExpansionTimeParameter.getFloatValue()));
		arguments.add(Float.toString(emptyParam.getFloatValue()));
		arguments.add(Float.toString(timeStep.getFloatValue()));
		arguments.add(Float.toString(stepsBeforeReinit.getFloatValue()));
		arguments.add(Float.toString(intensityInteraction.getFloatValue()));
		arguments.add(Float.toString(gradientInteraction.getFloatValue()));
		arguments.add(Float.toString(curvatureConstraint.getFloatValue()));
		arguments.add(Float.toString(initialSeedRadius.getFloatValue()));
		arguments.add(Float.toString(d2.getFloatValue()));
		arguments.add(Float.toString(tMax3.getFloatValue()));
		arguments.add(Float.toString(xStretch));
		arguments.add(Float.toString(yStretch));
		arguments.add(Float.toString(zStretch));

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

		/*
		if (pluginInputs.get("Preexisting segmentation") != null) {
		}*/

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
		// If there is protobuf data associated with our output, clear it and the contents of the image so that we don't
		// end up with a duplicate set of segmentations

		progressReporter = p;
		Utils.log("runchannel call link is " + link + " and plugin " + this, LogLevel.DEBUG);

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
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		(saveButtonListener1).buttonPressed(commandName, null, null);
	}

	@SuppressWarnings("null")
	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		(actionTypeListener1).parameterValueChanged(false, actionTypeParameter, false);
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

	@Override
	public boolean alwaysNotify() {
		return false;
	}

	@Override
	public String getParameterName() {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void setParameterName(String name) {
		throw new RuntimeException("Unimplemented");
	}

}
