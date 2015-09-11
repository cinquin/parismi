/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.misc;

import static processing_utilities.projection.RayFunction.MAX_METHOD;
import static processing_utilities.projection.RayFunction.projectSlice;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.XYScatterPlotView;
import pipeline.GUI_utils.XYSeriesCollectionGenericManipulator;
import pipeline.GUI_utils.XYSeriesReflection;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.plugins.PluginInfo;
import pipeline.plugins.ThreeDPlugin;
import pipeline.plugins.image_processing.skeleton.AnalyzeSkeleton;
import processing_utilities.projection.RayFunction;
import processing_utilities.straightening.LocalStraightener;

// The output of this plugin is a PluginIOCells, which is a quick hack to reuse the plotting functionality
// associated with that object. We need a PluginIO subclass that corresponds to a profile plot (and/or list of xy
// values),
// and an associated viewer.
@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "Plot3DProfileV2")
public class Plot3DProfile extends ThreeDPlugin {

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack uselessOutput, ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) {

		Roi roi = null;
		if (selectedOptions[useRoiForPlot]) {
			if (input.getRoi() != null)
				roi = (Roi) input.getRoi().clone();// Roi OBJECTS ARE NOT THREAD-SAFE!!!!
			if ((!(roi instanceof PolygonRoi)) || (roi instanceof PointRoi))
				roi = null;
			if (roi == null) {
				Utils.log("No ROI selection (of the right kind at least) in Plot3DProfile", LogLevel.ERROR);
			}
		}
		int lineWidth = -1;
		if (roi != null)
			lineWidth = Math.round(roi.getStrokeWidth());

		FloatProcessor fp = null;
		RayFunction rayFunc = null;
		ImageProcessor straightenedSlice = null;
		// project the z axis

		for (int i = 0; i < input.getDepth(); i++) {
			if (roi != null)
				straightenedSlice =
						LocalStraightener.localStraightenLine(input.getPixelsAsProcessor(i), (PolygonRoi) roi,
								lineWidth);
			else
				straightenedSlice = input.getPixelsAsProcessor(i);

			if (!selectedOptions[dontIgnorexyBorders]) {
				straightenedSlice.setRoi(1, 1, straightenedSlice.getWidth() - 2, straightenedSlice.getHeight() - 2);
				straightenedSlice = straightenedSlice.crop();
			}

			if (rayFunc == null) {
				fp = new FloatProcessor(straightenedSlice.getWidth(), straightenedSlice.getHeight());
				rayFunc =
						RayFunction.getRayFunction(method, (float[]) fp.getPixels(), input.getDepth(), pixelMinFilter,
								pixelMaxFilter);

			}
			projectSlice(straightenedSlice.getPixels(), rayFunc, input.getPixelType());
		}
		// DO NOT POSTPROCESS BECAUSE THAT WOULD MESS UP THE LAST PROJECTION (BELOW) rayFunc.postProcess();

		// now project the y axis

		FloatProcessor fp2 = new FloatProcessor(fp.getWidth(), 1, rayFunc.projectProjection(fp.getWidth()), null);
		float[] resultPixels = (float[]) fp2.getPixels();
		if (pixelFilterKind > 1) {
			for (int i = 0; i < resultPixels.length; i++) {
				resultPixels[i] = resultPixels[i] / pixelMaxFilter;
			}
		}
		@NonNull String name = input.getName();
		String truncatedChannelName = name.substring(0, Utils.min(10, name.length() - 1));

		PluginIOCells plot = (PluginIOCells) pluginOutputs.get("Plot of " + input.getName());// new PluginIOCells()
		if (plot == null)
			throw new IllegalStateException("Plot \"Plot of " + input.getName() + "\"has not been created for channel "
					+ input.getName());

		@SuppressWarnings("null")
		String fileNameString =
				FileNameUtils.removeIncrementationMarks((String) (workingDirectory.getValue())) + "/"
						+ truncatedChannelName + "_"
						+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue());
		@SuppressWarnings("null")
		File directory = new File(FileNameUtils.removeIncrementationMarks((String) workingDirectory.getValue()));
		if (!(directory.exists() && directory.isDirectory())) {
			Utils.displayMessage("File " + fileNameString + " does not exist or is not a directory", true,
					LogLevel.ERROR);
			throw new RuntimeException();
		}
		File file1 = new File(fileNameString);

		plot.clear();

		try (FileWriter outFile = new FileWriter(file1); PrintWriter out = new PrintWriter(outFile)) {
			Calibration cal = input.getCalibration();
			float xCal = cal != null ? (float) cal.pixelWidth : 1.0f;
			float[] pixels = (float[]) fp2.getPixels();
			float[] xarray = new float[pixels.length];
			for (int i = 0; i < fp2.getWidth(); i++) {
				xarray[i] = i * xCal;
				plot.add(new ClickedPoint(i * xCal, pixels[i]));
				out.println(i * xCal + "	" + pixels[i]);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	// from ImageJ source: hui/ProfilePlot.java
	@SuppressWarnings("unused")
	private static double[] getWideLineProfile(ImageProcessor ip, Roi ro, int lineWidth) {
		// Roi roi = (Roi)ro.clone();
		ImageProcessor ip2 = LocalStraightener.localStraightenLine(ip, (PolygonRoi) ro, lineWidth);
		int width = ip2.getWidth();
		int height = ip2.getHeight();
		double[] profile = new double[width];
		double[] aLine;
		ip2.setInterpolate(false);
		for (int y = 0; y < height; y++) {
			aLine = ip2.getLine(0, y, width - 1, y);
			for (int i = 0; i < width; i++)
				profile[i] += aLine[i];
		}
		for (int i = 0; i < width; i++)
			profile[i] /= height;
		return profile;
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF;
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
		@NonNull String @NonNull[] sourceChannels = getInput().listOfSubObjects();
		ArrayList<PluginIOView> plotViewList = new ArrayList<>();
		XYScatterPlotView<XYSeriesCollectionGenericManipulator, XYSeriesReflection> plotView =
				new XYScatterPlotView<>(0);
		plotViewList.add(plotView);
		for (String sourceChannel : sourceChannels) {
			initializeOutputs();
			PluginIOCells series = new PluginIOCells(sourceChannel);
			plotView.addSeries(sourceChannel, series.getJFreeChartXYSeries("x", "y", -1, -1, null, null));
			pluginOutputs.put("Plot of " + sourceChannel, series);
			Utils.log("Created Plot of " + sourceChannel, LogLevel.DEBUG);
		}
		return plotViewList;
	}

	private float pixelMinFilter = -Float.MAX_VALUE;
	private float pixelMaxFilter = Float.MAX_VALUE;

	private int pixelFilterKind = -1;

	private class PixelFilterListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (!(((ComboBoxParameter) filterParameter).getSelectionIndex() == pixelFilterKind)) {
				pixelFilterKind = ((ComboBoxParameter) filterParameter).getSelectionIndex();
				updatePixelMinAndMax();
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener pixelFilterListener0 = new PixelFilterListener();
	private ParameterListener pixelFilterListener1 = new ParameterListenerWeakRef(pixelFilterListener0);

	private boolean[] selectedOptions = { false, false };
	private static final int useRoiForPlot = 0;
	private static final int dontIgnorexyBorders = 1;

	private class OptionsListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			boolean[] newSelections = optionsParameter.getSelectedChoices();
			if (!Arrays.equals(selectedOptions, newSelections)) {
				selectedOptions = newSelections;
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener optionsListener0 = new OptionsListener();
	private ParameterListener optionsListener1 = new ParameterListenerWeakRef(optionsListener0);

	private void updatePixelMinAndMax() {
		switch (pixelFilterKind) {
			case 0:
			case 1:
				pixelMinFilter = -Float.MAX_VALUE;
				pixelMaxFilter = Float.MAX_VALUE;
				break;
			case skeletonJunction:
				pixelMinFilter = pixelMaxFilter = AnalyzeSkeleton.JUNCTION;
				break;
			case skeletonEndPoint:
				pixelMinFilter = pixelMaxFilter = AnalyzeSkeleton.END_POINT;
				break;
			default:
				throw new IllegalStateException("Unknown pixel filter kind " + pixelFilterKind);
		}
	}

	private class MethodListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (!(((ComboBoxParameter) methodParameter).getSelectionIndex() - 1 == method)) {
				method = ((ComboBoxParameter) methodParameter).getSelectionIndex() - 1;
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener methodListener0 = new MethodListener();
	private ParameterListener methodListener1 = new ParameterListenerWeakRef(methodListener0);

	private class WorkingDirectoryListener extends ParameterListenerAdapter {
	}

	private ParameterListener workingDirectoryListener0 = new WorkingDirectoryListener();
	private ParameterListener workingDirectoryListener1 = new ParameterListenerWeakRef(workingDirectoryListener0);

	private class SaveToFileListener extends ParameterListenerAdapter {
	}

	private ParameterListener saveToFileListener0 = new SaveToFileListener();
	private ParameterListener saveToFileListener1 = new ParameterListenerWeakRef(saveToFileListener0);

	private int method = MAX_METHOD;
	private String[] choices = RayFunction.METHODS;
	private final static int skeletonJunction = 2;
	private final static int skeletonEndPoint = 3;
	private String[] filterChoices = { "", "None", "Skeleton junction", "Skeleton end point" };
	private AbstractParameter filterParameter = new ComboBoxParameter("Filter pixels", "", filterChoices, "None",
			false, pixelFilterListener1);
	private AbstractParameter methodParameter = new ComboBoxParameter("Profiling method", "", choices, "Average",
			false, methodListener1);

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory", "Directory to save plot in",
			"", true, workingDirectoryListener1);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true,
			saveToFileListener1);

	private MultiListParameter optionsParameter = new MultiListParameter("Options", "", new @NonNull String[] { "Use ROI",
			"Include xy borders" }, new int[] {}, optionsListener1);

	private AbstractParameter splitOptionsAndMethod = null;

	private AbstractParameter splitDirectoryAndFile = null;
	private AbstractParameter splitMethodAndPixelFilter = null;

	private ParameterListener splitC0 = new SplitParameterListener(new ParameterListener[] { optionsListener1,
			methodListener1 });
	private ParameterListener splitC1 = new ParameterListenerWeakRef(splitC0);

	private ParameterListener splitA0 = new SplitParameterListener(new ParameterListener[] { splitC1,
			pixelFilterListener1 });
	private ParameterListener splitA1 = new ParameterListenerWeakRef(splitA0);
	private ParameterListener splitB0 = new SplitParameterListener(new ParameterListener[] { saveToFileListener1,
			workingDirectoryListener1 });
	private ParameterListener splitB1 = new ParameterListenerWeakRef(splitB0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { splitA1, splitB1 };
	}

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory });
		}
		if (splitOptionsAndMethod == null) {
			splitOptionsAndMethod = new SplitParameter(new Object[] { optionsParameter, methodParameter });
		}
		if (splitMethodAndPixelFilter == null) {
			splitMethodAndPixelFilter = new SplitParameter(new Object[] { splitOptionsAndMethod, filterParameter });
		}
		AbstractParameter[] paramArray = { splitMethodAndPixelFilter, splitDirectoryAndFile };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		Object[] splitParameters = (Object[]) param[0].getValue();
		Object[] resplitParameters = (Object[]) ((SplitParameter) splitParameters[0]).getValue();
		optionsParameter = (MultiListParameter) resplitParameters[0];
		methodParameter = (ComboBoxParameter) resplitParameters[1];
		filterParameter = (ComboBoxParameter) splitParameters[1];
		splitParameters = (Object[]) param[1].getValue();
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];

		method = ((ComboBoxParameter) methodParameter).getSelectionIndex() - 1;// for a ComboBoxParameter,
																				// getSelectionIndex returns the index
																				// of the selection in the list of
																				// possible choices
		pixelFilterKind = ((ComboBoxParameter) filterParameter).getSelectionIndex();
		selectedOptions = optionsParameter.getSelectedChoices();
		updatePixelMinAndMax();
	}

	@Override
	public String operationName() {
		return "Plot3DProfile";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}
}
