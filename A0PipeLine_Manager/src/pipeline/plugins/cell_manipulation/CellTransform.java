/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

/**
 * For plugins of the form Cells -> Cells. Descendants should implement the "transform" method.
 *
 */
public abstract class CellTransform extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, false, false);
		desc0.name = "Seeds";
		result.put("Seeds", desc0);
		return result;
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		Utils.log("Creating seeds", LogLevel.DEBUG);
		initializeOutputs();
		IPluginIOList<?> out = (IPluginIOList<?>) getInput("Seeds").duplicateStructure();
		PluginIOView view = out.createView();
		view.setData(out);
		pluginOutputs.put("Cells", out);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Cells";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Cells", desc0);
		return result;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Cells" };
	}

	protected abstract ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) throws InterruptedException;

	protected void preRun(PluginIOCells inputCells, IPluginIOHyperstack inputImage) {
	}

	protected void postRun(PluginIOCells outputCells) {
	}

	protected void transformCells(final IPluginIOList<ClickedPoint> inputCells,
			final IPluginIOList<ClickedPoint> outputCells, final IPluginIOHyperstack inputImage, ProgressReporter r)
			throws InterruptedException {

		ParFor parFor = new ParFor(0, inputCells.size() - 1, r, threadPool, true);
		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker((loopIndex, threadIndex) -> transform(inputCells.get(loopIndex), inputCells,
					inputImage, loopIndex));

		List<String> namesQuantifiedProp = ((PluginIOCells) outputCells).getQuantifiedPropertyNames();
		List<String> namesUserCellDescs = outputCells.getUserCellDescriptions();

		for (Object p : parFor.run(true)) {
			ClickedPoint p2 = (ClickedPoint) p;
			p2.listNamesOfQuantifiedProperties = namesQuantifiedProp;
			p2.userCellDescriptions = namesUserCellDescs;
			outputCells.addDontFireValueChanged(p2);
		}
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		@SuppressWarnings("unchecked")
		final IPluginIOList<ClickedPoint> inputCells = (IPluginIOList<ClickedPoint>) pluginInputs.get("Seeds");

		final IPluginIOHyperstack inputImage = (IPluginIOHyperstack) pluginInputs.get("Default source");
		preRun((PluginIOCells) inputCells, inputImage);

		@SuppressWarnings("unchecked")
		IPluginIOList<ClickedPoint> outputCells = (IPluginIOList<ClickedPoint>) pluginOutputs.get("Cells");
		inputCells.copyInto(outputCells);
		outputCells.clear();

		transformCells(inputCells, outputCells, inputImage, r);

		outputCells.fireValueChanged(false, false);

		postRun((PluginIOCells) outputCells);
	}

}
