/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2013 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.misc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ij.measure.Calibration;
import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;

// The output of this plugin is a PluginIOCells, which is a quick hack to reuse the plotting functionality
// associated with that object. We need a PluginIO subclass that corresponds to a profile plot (and/or list of xy
// values),
// and an associated viewer.

public class PlotZProfile extends FourDPlugin {

	@ParameterInfo(userDisplayName = "Projection method", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false, stringChoices = {"Median", "Average", "Median non-0", "Average non-0" },
			stringValue = "Median non-0", editable = false)
	@ParameterType(parameterType = "ComboBox", printValueAsString = true)
	private String projectionMethod;

	@ParameterInfo(userDisplayName = "Left of box to project to a single value", changeTriggersUpdate = true,
			changeTriggersLiveUpdates = true)
	private int x0;

	@ParameterInfo(userDisplayName = "Right of box to project to a single value", changeTriggersUpdate = true,
			changeTriggersLiveUpdates = true)
	private int x1;

	@ParameterInfo(userDisplayName = "Top of box to project to a single value", changeTriggersUpdate = true,
			changeTriggersLiveUpdates = true)
	private int y0;

	@ParameterInfo(userDisplayName = "Bottom of box to project to a single value", changeTriggersUpdate = true,
			changeTriggersLiveUpdates = true)
	private int y1;

	@ParameterInfo(userDisplayName = "Save text file to", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false)
	private File saveTo;

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		final IPluginIOHyperstack hsInput = (IPluginIOHyperstack) getImageInput();

		final int boxWidth = x1 - x0 + 1;
		final int boxHeight = y1 - y0 + 1;

		StringBuilder result = new StringBuilder();

		result.append("z\t");
		final Calibration cal = hsInput.getCalibration();
		for (int i = 0; i < ((IPluginIOHyperstack) getImageInput()).getDepth(); i++) {
			result.append(cal == null ? i : (cal.pixelDepth * i)).append("\t");
		}
		result.append("\n");

		for (IPluginIOStack input: ((IPluginIOHyperstack) getImageInput()).getChannels().values()) {
			result.append(input.getName());

			for (int z = 0; z < input.getDepth(); z++) {
				float[] pixels = new float[boxWidth * boxHeight];

				int index = 0;
				for (int x = x0; x <= x1; x++) {
					for (int y = y0; y <= y1; y++) {
						pixels[index++] = input.getFloat(x, y, z);
					}
				}

				switch (projectionMethod) {
					case "Median":
						Arrays.sort(pixels);
						result.append("\t").append(pixels[pixels.length / 2]);
						break;
					case "Median non-0":
						Arrays.sort(pixels);
						int firstNon0 = 0;
						while (pixels[firstNon0] <= 0 && firstNon0 < pixels.length - 1) {
							firstNon0++;
						}
						result.append("\t").append(pixels[firstNon0 + (pixels.length - 1 - firstNon0) / 2]);
						break;
					case "Average non-0":
					case "Average":
						double sum = 0;
						int n = 0;
						for (float f : pixels) {
							if (projectionMethod.equals("Average") || f > 0) {
								sum += f;
								n++;
							}
						}
						result.append("\t").append((float) (sum / n));
						break;
					default:
						throw new IllegalStateException("Unrecognized projection method " + projectionMethod);
				}//End projection
			}//End channel loop
			result.append("\n");
		}//End channel loop

		@SuppressWarnings("null")
		String fileNameString = FileNameUtils.removeIncrementationMarks(saveTo.getAbsolutePath());
		File file1 = new File(fileNameString);

		try (FileWriter outFile = new FileWriter(file1); PrintWriter out = new PrintWriter(outFile)) {
			out.append(result);
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
		return null;
	}

	@Override
	public String operationName() {
		return "PlotZProfile";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.SHORT_TYPE, PixelType.BYTE_TYPE},
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

}