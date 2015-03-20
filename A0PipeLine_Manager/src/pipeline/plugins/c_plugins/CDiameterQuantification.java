/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.plugins.ExternalCallToLibrary;

public class CDiameterQuantification extends ExternalCallToLibrary {

	private static class MethodListener extends ParameterListenerAdapter {
	}

	private ParameterListener methodListener0 = new MethodListener();

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { methodListener0, methodListener0 };// We do NOT want a weak listener because we
		// need to stay around after the plugin has first been run from the pipeline
	}

	private SplitParameter split;
	private IntParameter rayLengthParam;
	private FloatParameter zScaleParameter;

	private void initializeParams() {
		param1 = new IntParameter("Number of rays", "", 50, 0, 100, true, true, null);
		rayLengthParam = new IntParameter("Maximum ray length", "", 1000, 0, 1000, true, true, null);
		zScaleParameter = new FloatParameter("z scale parameter", "", 1f, 1f, 5f, true, true, true, null);
		split = new SplitParameter(new AbstractParameter[] { rayLengthParam, zScaleParameter });
		param2 = split;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		if (param == null)
			return;
		param1 = param[0];
		param2 = param[1];
		rayLengthParam = (IntParameter) ((SplitParameter) param2).getParameterValue()[0];
		zScaleParameter = (FloatParameter) ((SplitParameter) param2).getParameterValue()[1];
	}

	@Override
	public String getTIFFType() {
		return "32";
	}

	@Override
	public String operationName() {
		return "C helper for finding local diameter in a segmentation";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds", "HistogramToIgnore" };
	}

	public CDiameterQuantification() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "CDiameterQuantification";
	}

	@Override
	public void postRunUpdates() {
		((PluginIOCells) pluginOutputs.get("Seeds")).restoreFromProtobuf();
		PluginIOCells cells = ((PluginIOCells) pluginOutputs.get("Seeds"));
		if (cells.getCalibration() != null && cells.getCalibration().pixelHeight != 0) {
			float calibration = (float) cells.getCalibration().pixelHeight;
			for (ClickedPoint cell : cells) {
				cell.setQuantifiedProperty("rachisDiameter", cell.getQuantifiedProperty("rachisDiameter") * calibration);
			}
		}
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds", desc0);

		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));

		return result;
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {

		Calibration c = getImageInput().getCalibration();
		if (c == null) {
			Utils.log("Cannot guess rescaling factor for image " + input + " because it is not calibrated",
					LogLevel.WARNING);
		} else {
			zScaleParameter.setValue(new float[] { (float) (c.pixelDepth / c.pixelHeight),
					((float[]) zScaleParameter.getValue())[1], ((float[]) zScaleParameter.getValue())[2] });
			zScaleParameter.fireValueChanged(false, true, true);
		}

		Utils.log("Creating seeds", LogLevel.DEBUG);
		initializeOutputs();
		PluginIOCells seeds = new PluginIOCells();
		ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
		view.setData(seeds);
		pluginOutputs.put("Seeds", seeds);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
	}
}
