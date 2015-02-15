/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ILoopWorker;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;

/**
 * Scale x and y coordinates by a given factor. Used to map cells detected by SVM on a scaled image back to the
 * original image.
 * FIXME For now, this plugin should NOT be run on more than 1 channel at a time.
 */
public class xyRescaleCells extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "From a set of cells, scale x and y coordinates by a given factor. Used to map cells detected by SVM on a scaled image back to the "
				+ "original image";
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
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

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds", desc0);

		return result;
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF + SAME_AS_BINARY;
	}

	private float rescaleFactor = 1;
	private FloatParameter ratioParam = new FloatParameter("Factor", "xy rescaling factor", rescaleFactor, 0.0f, 5.0f,
			true, true, true, null);

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { ratioParam, null };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		ratioParam = (FloatParameter) param[0];
	}

	@Override
	public String operationName() {
		return "xy rescale cells";
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
		return new String[] { "Seeds" };
	}

	@Override
	public void runChannel(final IPluginIOStack input, IPluginIOStack output, ProgressReporter r,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		Utils.log("Running ball quantification", LogLevel.DEBUG);
		final PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");
		final PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Seeds");
		outputCells.clear();
		inputCells.copyInto(outputCells);

		rescaleFactor = ratioParam.getFloatValue();

		Calibration cal = outputCells.getCalibration();
		if (cal != null) {
			cal.pixelHeight /= rescaleFactor;
			cal.pixelWidth /= rescaleFactor;
		}

		ParFor parFor = new ParFor(0, inputCells.getPoints().size() - 1, r, threadPool, true);

		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker(new ILoopWorker() {

				List<ClickedPoint> pointList = inputCells.getPoints();

				@Override
				public final Object run(int loopIndex, int threadIndex) {

					ClickedPoint p = pointList.get(loopIndex);
					ClickedPoint pCloned = (ClickedPoint) p.clone();
					pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
					pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();

					pCloned.x = p.x * rescaleFactor;
					pCloned.y = p.y * rescaleFactor;

					pCloned.xyCalibration /= rescaleFactor;

					return pCloned;
				}
			});

		for (Object p : parFor.run(true)) {
			outputCells.addDontFireValueChanged((ClickedPoint) p);
		}

		outputCells.fireValueChanged(false, false);

	}

}
