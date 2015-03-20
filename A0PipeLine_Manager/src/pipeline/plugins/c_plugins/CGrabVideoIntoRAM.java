/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.tiff_read_write.TIFFFileAccessor;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ActionParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.PluginInfo;
import pipeline.plugins.SpecialDimPlugin;

/**
 * Problems with channel selection in input/output
 *
 */
@PluginInfo(displayToUser = false, obsolete = true)
public class CGrabVideoIntoRAM extends ExternalCallToLibrary implements SpecialDimPlugin {
	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF;
	}

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { null,
				new SplitParameterListener(new ParameterListener[] { null, null, interruptListener }) };
	}

	@Override
	public String operationName() {
		return "C helper for grabbing frames from DCAM-compliant FireWire camera";
	}

	@Override
	public String version() {
		return "1.0";
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory",
			"Directory to save measurements in", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);

	private AbstractParameter splitDirectoryAndFile = null;

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory, interruptButton });
		}
		AbstractParameter[] paramArray = { videoParameters, splitDirectoryAndFile };
		return paramArray;
	}

	private SplitParameter videoParameters;
	private IntParameter x0, y0, x1, y1, zRange;
	private IntParameter millisecondsBetweenFrames;
	private IntParameter exposureInMilliseconds;
	private FloatParameter gain;
	private FloatParameter videoThreshold;
	private ComboBoxParameter thresholdingMethod;
	private ActionParameter interruptButton;

	ActionParameter saveResultsParameter;
	ComboBoxParameter actionTypeParameter;

	private static class InterruptListener extends ParameterListenerAdapter {
		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			try {
				if (accessor != null)
					accessor.closeFileEarly();
			} catch (Exception e) {
				Utils.printStack(e);
			}
		}

	}

	private ParameterListener interruptListener0 = new InterruptListener();
	private ParameterListener interruptListener = new ParameterListenerWeakRef(interruptListener0);

	@Override
	public void setParameters(AbstractParameter[] param) {
		videoParameters = (SplitParameter) param[0];
		param1 = videoParameters;
		Object[] splitParameters = (Object[]) (videoParameters).getValue();
		x0 = (IntParameter) splitParameters[0];
		x1 = (IntParameter) splitParameters[1];
		y0 = (IntParameter) splitParameters[2];
		y1 = (IntParameter) splitParameters[3];
		zRange = (IntParameter) splitParameters[4];
		millisecondsBetweenFrames = (IntParameter) splitParameters[5];
		exposureInMilliseconds = (IntParameter) splitParameters[6];
		gain = (FloatParameter) splitParameters[7];
		videoThreshold = (FloatParameter) splitParameters[8];
		thresholdingMethod = (ComboBoxParameter) splitParameters[9];

		splitParameters = (Object[]) param[1].getValue();
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];
		interruptButton = (ActionParameter) splitParameters[2];
	}

	private void initializeParams() {
		x0 = new IntParameter("x0", "For ROI", 0, 0, 1280, true, true, null);
		x1 = new IntParameter("x1", "For ROI", 1280, 0, 1280, true, true, null);
		y0 = new IntParameter("y0", "For ROI", 0, 0, 960, true, true, null);
		y1 = new IntParameter("y1", "For ROI", 960, 0, 960, true, true, null);
		zRange =
				new IntParameter("Number of frames to save", "Number of frames to save", 500, 0, 500, true, true, null);
		millisecondsBetweenFrames =
				new IntParameter("Milliseconds between frames", "Milliseconds between frames", 500, 0, 500, true, true,
						null);
		exposureInMilliseconds = new IntParameter("Exposure (ms)", "", 30, 0, 1000, true, true, null);
		gain = new FloatParameter("Gain", "", 1f, 0f, 10f, true, true, true, null);
		videoThreshold =
				new FloatParameter("Keep frames above threshold",
						"Movement threshold computed by the C plugin to decide which frames are worth keeping", 0f, 0f,
						10f, true, true, true, null);
		thresholdingMethod =
				new ComboBoxParameter("Thresholding method", "", new String[] { "Method 0", "Method 1", "Method 2",
						"Method 3", "Method 4" }, "Method 0", false, null);

		videoParameters =
				new SplitParameter(new Object[] { x0, x1, y0, y1, zRange, millisecondsBetweenFrames,
						exposureInMilliseconds, gain, videoThreshold, thresholdingMethod });
		param1 = videoParameters;

		fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);
		workingDirectory =
				new DirectoryParameter("Save directory", "Directory to save measurements in", "", true, null);
		interruptButton =
				new ActionParameter("Interrupt", "Interrupt current video acquisition and saves data already acquired",
						true, interruptListener);
		splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory, interruptButton });
	}

	public CGrabVideoIntoRAM() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "grab_video";
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		return x1.getintValue() - x0.getintValue() + 1;
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		return y1.getintValue() - y0.getintValue() + 1;
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		return zRange.getintValue();
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return 1;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.BYTE_TYPE;
	}

	private TIFFFileAccessor tiffOutput;
	private File f;

	private @NonNull File getFile() {
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + Utils.fileNameSeparator
						+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));

		File directory = new File(fileNameString).getParentFile();
		if (!(directory.exists() && directory.isDirectory())) {
			if (directory.isFile()) {
				throw new PluginRuntimeException("Cannot save to " + directory + " because it is a file", true);
			}
			if (!directory.mkdirs()) {
				throw new PluginRuntimeException("Directory " + directory + " does not exist and could not be created", true);
			}
		}
		return new File(fileNameString);
	}

	/**
	 * FOR NOW, WE'RE ASSUMING ONLY 1 C PLUGIN WILL RUN AT A TIME FOR VIDEO ACQUISITION
	 */
	private static TIFFFileAccessor accessor;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output,
			final pipeline.misc_util.ProgressReporter p, final PreviewType previewType, boolean inputHasChanged)
			throws InterruptedException {
		f = getFile();

		tiffOutput = new TIFFFileAccessor(f, "Video", PixelType.BYTE_TYPE, null, true);
		tiffOutput.setDimensions(getOutputWidth(null), getOutputHeight(null), getOutputDepth(null), 1, 1);
		accessor = tiffOutput;

		super.runChannel(input, output, p, previewType, inputHasChanged);
	}

	@Override
	public void loadExtraOutputArgs(List<String> args, String firstOutputName) {
		args.add(getFile().getAbsolutePath() + ".metadata");
	}

	@Override
	public void postRunUpdates() {
		try {
			if (getOutput() instanceof TIFFFileAccessor)
				((TIFFFileAccessor) getOutput()).close();
		} catch (IOException e) {
			Utils.printStack(e);
		}
		super.postRunUpdates();
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {

		PluginIOHyperstack createdOutput =
				new PluginIOHyperstack("Test video", getOutputWidth(null), getOutputHeight(null), getOutputDepth(null),
						1, 1, PixelType.BYTE_TYPE, false);

		int nChannelsToAdd = 1;

		for (int i = 0; i < nChannelsToAdd; i++) {
			createdOutput.addChannel("choice1");
		}

		setOutput("Default destination", createdOutput, true);

		ArrayList<PluginIOView> imagesToShow = new ArrayList<>();

		PluginIOHyperstackViewWithImagePlus display = null;
		if (!Utils.headless) {
			if (impForDisplay != null) {
				createdOutput.setImp(impForDisplay);
				display = impForDisplay;
			} else {
				display = new PluginIOHyperstackViewWithImagePlus(createdOutput.getName());
				createdOutput.setImp(display);
			}

			display.addImage(createdOutput);
			display.shouldUpdateRange = true;
			imagesToShow.add(display);
		}

		return imagesToShow;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}
}
