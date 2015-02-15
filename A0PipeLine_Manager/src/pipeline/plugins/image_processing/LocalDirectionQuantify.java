/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;
import pipeline.stats.jama.Matrix;
import pipeline.stats.jama.SingularValueDecomposition;

/**
 * 
 */
public class LocalDirectionQuantify extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.SHORT_TYPE, PixelType.BYTE_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
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
		return PLUGIN_CREATES_OUTPUT_ITSELF + ONLY_1_INPUT_CHANNEL;
	}

	private IntParameter directionAveragingLength = new IntParameter("Avering length",
			"Number of local points to fit a line to to get the local orientation of the rachis", 10, 0, 20, true,
			true, null);

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { directionAveragingLength, null };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		directionAveragingLength = (IntParameter) param[0];
	}

	@Override
	public String operationName() {
		return "Local Direction Quantify";
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

	private static void setFit(List<ClickedPoint> list, ClickedPoint resultPoint) {
		float av_x = 0, av_y = 0, av_z = 0;
		int n_points = 0;
		for (ClickedPoint p : list) {
			n_points++;
			av_x += p.x;
			av_y += p.y;
			av_z += p.z;
		}
		av_x = av_x / n_points;
		av_y = av_y / n_points;
		av_z = av_z / n_points;

		Matrix m = new Matrix(n_points, 3);

		int point = 0;
		for (ClickedPoint p : list) {
			m.set(point, 0, p.x - av_x);
			m.set(point, 1, p.y - av_y);
			m.set(point, 2, p.z - av_z);
			point++;
		}

		SingularValueDecomposition sv = new SingularValueDecomposition(m);
		Matrix rightSingular = sv.getV();

		resultPoint.setQuantifiedProperty("vec_x", (float) rightSingular.get(0, 0));
		resultPoint.setQuantifiedProperty("vec_y", (float) rightSingular.get(1, 0));
		resultPoint.setQuantifiedProperty("vec_z", (float) rightSingular.get(2, 0));

	}

	@Override
	public void runChannel(final IPluginIOStack input, IPluginIOStack output, ProgressReporter r,
			PreviewType previewType, boolean inputHasChanged) {

		Utils.log("Running ball quantification", LogLevel.DEBUG);
		final PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");
		final PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Seeds");
		outputCells.clear();
		inputCells.copyInto(outputCells);

		final String fieldName = "vec_x";

		final boolean fieldIsNew = !outputCells.addQuantifiedPropertyName(fieldName);
		outputCells.addQuantifiedPropertyName("vec_y");
		outputCells.addQuantifiedPropertyName("vec_z");

		// FIXME Change to BallIterator, from which following code was copied

		LinkedList<ClickedPoint> localList = new LinkedList<>();

		Iterator<ClickedPoint> middlePointIterator = inputCells.iterator();

		ClickedPoint middlePoint = null;
		for (ClickedPoint endPoint : inputCells.getPoints()) {
			localList.add(endPoint);
			if (localList.size() >= directionAveragingLength.getintValue())
				localList.removeFirst();
			if (localList.size() >= directionAveragingLength.getintValue() / 2)
				middlePoint = middlePointIterator.next();
			else
				continue;
			ClickedPoint pCloned = (ClickedPoint) middlePoint.clone();
			pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
			pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();
			if (fieldIsNew) {
				pCloned.getQuantifiedProperties().add(0f);
				pCloned.getQuantifiedProperties().add(0f);
				pCloned.getQuantifiedProperties().add(0f);
			}

			setFit(localList, pCloned);

			outputCells.addDontFireValueChanged(pCloned);

		}

		outputCells.fireValueChanged(false, false);
	}

}
