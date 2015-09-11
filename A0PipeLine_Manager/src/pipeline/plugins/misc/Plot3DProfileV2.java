/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.misc;

import ij.measure.Calibration;

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
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.ThreeDPlugin;

// The output of this plugin is a PluginIOCells, which is a quick hack to reuse the plotting functionality
// associated with that object. We need a PluginIO subclass that corresponds to a profile plot (and/or list of xy
// values),
// and an associated viewer.

public class Plot3DProfileV2 extends ThreeDPlugin {

	@ParameterInfo(userDisplayName = "Projection method", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false, stringChoices = { "Median non-0", "Average non-0" },
			stringValue = "Median non-0", editable = false)
	@ParameterType(parameterType = "ComboBox", printValueAsString = true)
	private String projectionMethod;

	@ParameterInfo(userDisplayName = "Save text file to", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false)
	private File saveTo;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack uselessOutput, ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) {

		float[][] pixelsAlongXAxis = new float[input.getWidth()][];

		for (int i = 0; i < input.getWidth(); i++) {
			int index = 0;
			float[] pixels = new float[input.getHeight() * input.getDepth()];
			pixelsAlongXAxis[i] = pixels;
			for (int j = 0; j < input.getHeight(); j++) {
				for (int k = 0; k < input.getDepth(); k++) {
					pixels[index++] = input.getFloat(i, j, k);
				}
			}
		}

		float[] result = new float[input.getWidth()];

		int index = 0;
		for (float[] pixels : pixelsAlongXAxis) {
			switch (projectionMethod) {
				case "Median non-0":
					Arrays.sort(pixels);
					int firstNon0 = 0;
					while (pixels[firstNon0] <= 0 && firstNon0 < pixels.length - 1) {
						firstNon0++;
					}
					result[index] = pixels[firstNon0 + (pixels.length - 1 - firstNon0) / 2];
					break;
				case "Average non-0":
					double sum = 0;
					int n = 0;
					for (float f : pixels) {
						if (f > 0) {
							sum += f;
							n++;
						}
					}
					result[index] = (float) (sum / n);
					break;
				default:
					throw new IllegalStateException("Unrecognized projection method " + projectionMethod);
			}
			index++;
		}

		PluginIOCells plot = (PluginIOCells) pluginOutputs.get("Plot of " + input.getName());// new PluginIOCells()
		if (plot == null)
			throw new IllegalStateException("Plot \"Plot of " + input.getName() + "\"has not been created for channel "
					+ input.getName());

		@SuppressWarnings("null")
		String fileNameString = FileNameUtils.removeIncrementationMarks(saveTo.getAbsolutePath());
		File file1 = new File(fileNameString);

		plot.clear();

		try (FileWriter outFile = new FileWriter(file1); PrintWriter out = new PrintWriter(outFile)) {

			Calibration cal = input.getCalibration();
			float xCal = cal != null ? (float) cal.pixelWidth : 1.0f;

			float[] xarray = new float[result.length];
			for (int i = 0; i < result.length; i++) {
				xarray[i] = i * xCal;
				plot.add(new ClickedPoint(i * xCal, result[i]));
				out.println(i * xCal + "	" + result[i]);
			}

		} catch (IOException e) {
			throw new PluginRuntimeException("Could not write to file " + fileNameString, e, true);
		}
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
		for (@NonNull String sourceChannel : sourceChannels) {
			initializeOutputs();
			PluginIOCells series = new PluginIOCells(sourceChannel);
			plotView.addSeries(sourceChannel, series.getJFreeChartXYSeries("x", "y", -1, -1, null, null));
			pluginOutputs.put("Plot of " + sourceChannel, series);
			Utils.log("Created Plot of " + sourceChannel, LogLevel.DEBUG);
		}
		return plotViewList;
	}

	@Override
	public String operationName() {
		return "Plot3DProfile V2";
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
