/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.tiff_read_write.TIFFFileAccessor;
import pipeline.external_plugin_interfaces.JNACallToNativeLibrary;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ActionParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.IntRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.SpecialDimPlugin;

/**
 * ONLY 1 C PLUGIN CAN RUN AT A TIME FOR MOVEMENT DETECTION, because of static accessor declaration
 *
 */
public class CDetectMovement extends ExternalCallToLibrary implements SpecialDimPlugin {

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.BYTE_TYPE },
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF;
	}

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { null, new SplitParameterListener(new ParameterListener[] { null, null, null }) };
	}

	@Override
	public String operationName() {
		return "C helper for detecting movement";
	}

	@Override
	public String version() {
		return "1.0";
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory",
			"Directory to save measurements in", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);

	private AbstractParameter splitDirectoryAndFile;

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			throw new IllegalStateException();
		}
		AbstractParameter[] paramArray = { videoParameters, splitDirectoryAndFile };
		return paramArray;
	}

	private SplitParameter videoParameters;
	private IntParameter x0, y0, x1, y1;

	private IntRangeParameter zRange;

	private IntParameter threshold1Parameter;
	private IntParameter threshold2Parameter;
	private IntParameter threshold3Parameter;
	private IntParameter threshold4Parameter;
	private ComboBoxParameter thresholdingMethod;

	ActionParameter saveResultsParameter;
	ComboBoxParameter actionTypeParameter;

	@Override
	public void setParameters(AbstractParameter[] param) {
		videoParameters = (SplitParameter) param[0];
		param1 = videoParameters;
		Object[] splitParameters = (Object[]) (videoParameters).getValue();
		x0 = (IntParameter) splitParameters[0];
		x1 = (IntParameter) splitParameters[1];
		y0 = (IntParameter) splitParameters[2];
		y1 = (IntParameter) splitParameters[3];
		zRange = (IntRangeParameter) splitParameters[4];
		threshold1Parameter = (IntParameter) splitParameters[5];
		threshold2Parameter = (IntParameter) splitParameters[6];
		if (splitParameters[7] instanceof IntParameter)
			threshold3Parameter = (IntParameter) splitParameters[7];
		if (splitParameters[8] instanceof IntParameter)
			threshold4Parameter = (IntParameter) splitParameters[8];
		thresholdingMethod = (ComboBoxParameter) splitParameters[9];

		splitParameters = (Object[]) param[1].getValue();
		splitDirectoryAndFile = param[1];
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];
	}

	private static final String[] methodNames = new String[] { "Write all frames to output",
			"Write all difference images to output", "Only write frames in which movement was detected" };

	private static final String[] parameterNames = new String[] { "Channel brightness", "Egg brightness",
			"# frames eggs spend in channel", "Movement detection threshold" };

	private void initializeParams() {
		x0 = new IntParameter("x0", "For ROI", 0, 0, 1280, true, true, null);
		x1 = new IntParameter("x1", "For ROI", 1280, 0, 1280, true, true, null);
		y0 = new IntParameter("y0", "For ROI", 0, 0, 960, true, true, null);
		y1 = new IntParameter("y1", "For ROI", 960, 0, 960, true, true, null);
		zRange = new IntRangeParameter("Slice range to work on", "", 0, 500, 0, 500, true, true, null, null);

		threshold1Parameter = new IntParameter(parameterNames[0], "", 100, 0, 255, true, true, null);
		threshold2Parameter = new IntParameter(parameterNames[1], "", 100, 0, 255, true, true, null);
		threshold3Parameter = new IntParameter(parameterNames[2], "", 7, 0, 20, true, true, null);
		threshold4Parameter =
				new IntParameter(parameterNames[3], "In theory, peak height should be pi*r^2*(parameter2-parameter1)",
						30, 0, 1000, true, true, null);

		thresholdingMethod = new ComboBoxParameter("Thresholding method", "", methodNames, methodNames[2], false, null);

		videoParameters =
				new SplitParameter(new Object[] { x0, x1, y0, y1, zRange, threshold1Parameter, threshold2Parameter,
						threshold3Parameter, threshold4Parameter, thresholdingMethod });
		param1 = videoParameters;

		fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);
		workingDirectory =
				new DirectoryParameter("Save directory", "Directory to save measurements in", "", true, null);
		splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory });
	}

	public CDetectMovement() {
		Utils.log("new grab video", LogLevel.DEBUG);
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "detect_movement";
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
		return zRange.getHighInt() - zRange.getLowInt() + 1;
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return 1;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.BYTE_TYPE;
	}

	private @NonNull File getFile() {
		@SuppressWarnings("null")
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

		File result = new File(fileNameString);

		if (result.exists() && (result.length() > 5000000000L)) {
			throw new PluginRuntimeException(
					"File "
							+ result.getAbsolutePath()
							+ " already exists and is over ~5GB. Not overwriting; please delete file or choose a different name",
					true);
		}

		return result;
	}

	/**
	 * FOR NOW, WE'RE ASSUMING ONLY 1 C PLUGIN WILL RUN AT A TIME FOR VIDEO ACQUISITION
	 */
	@SuppressWarnings("unused")
	private static transient TIFFFileAccessor accessor;
	private static transient IPluginIOStack liveDisplay;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output,
			final pipeline.misc_util.ProgressReporter p, final PreviewType previewType, boolean inputHasChanged)
			throws InterruptedException {
		File f = getFile();

		TIFFFileAccessor tiffOutput = new TIFFFileAccessor(f, "Video", PixelType.BYTE_TYPE, null, true);
		tiffOutput.setDimensions(getOutputWidth(null), getOutputHeight(null), getOutputDepth(null), 1, 1);
		accessor = tiffOutput;

		setOutput("Default destination", tiffOutput, true);

		JNACallToNativeLibrary.SetPixelHook hook = new JNACallToNativeLibrary.SetPixelHook() {
			// Hook to update the live display for the user's benefit, every so many frames (not every frame for
			// performance reasons)
			private byte indexModulo50 = 0;
			// PluginIOStackInterface liveDisplay=(PluginIOStackInterface) getDestination("Live display");
			byte[] liveDisplayBytes = liveDisplay.getPixels(0, (byte) 0);
			PluginIOHyperstackViewWithImagePlus view = liveDisplay.getParentHyperstack().getImp();

			@Override
			public final void run(int index, ByteBuffer buffer) {
				if (indexModulo50++ == 50) {
					buffer.get(liveDisplayBytes);
					view.imp.updateAndDraw();
					indexModulo50 = 0;
				}
			}
		};

		externalCallPixelHook = hook;

		super.runChannel(input, tiffOutput, p, previewType, inputHasChanged);
	}

	@Override
	public void loadExtraOutputArgs(List<String> args, String firstOutputName) {
		args.add(getFile().getAbsolutePath() + ".metadata");
	}

	@Override
	public void postRunUpdates() {
		try {
			if (getOutput() instanceof TIFFFileAccessor)
				((TIFFFileAccessor) getOutput()).closeFileEarly();
		} catch (IOException e) {
			Utils.printStack(e);
		}
		super.postRunUpdates();
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {

		PluginIOHyperstack createdOutput =
				new PluginIOHyperstack("Live display", getOutputWidth(null), getOutputHeight(null), 1, 1, 1,
						PixelType.BYTE_TYPE, false);

		setOutput("Live display", createdOutput, true);
		liveDisplay = createdOutput.getChannels(new String[] { "Ch0" })[0];

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
			display.show();// NEED TO SHOW DISPLAY BEFORE THE END OF THE RUN AS NORMALLY HAPPENS

		}

		return imagesToShow;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}
}
