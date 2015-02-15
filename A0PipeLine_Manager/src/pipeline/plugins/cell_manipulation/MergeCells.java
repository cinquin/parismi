/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.concurrent.ConcurrentHashMap;

import pipeline.PreviewType;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;

/**
 * Cells in primary source take precedence.
 *
 */
public class MergeCells extends CellTransform {

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds", "Seeds2" };
	}

	@Override
	public String getToolTip() {
		return "Cells with same ID are chosen from the primay source";
	}

	@Override
	public String operationName() {
		return "Merge cells";
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		@SuppressWarnings("unchecked")
		final IPluginIOList<ClickedPoint> inputCells = (IPluginIOList<ClickedPoint>) pluginInputs.get("Seeds");
		@SuppressWarnings("unchecked")
		final IPluginIOList<ClickedPoint> inputCells2 = (IPluginIOList<ClickedPoint>) pluginInputs.get("Seeds2");

		preRun((PluginIOCells) inputCells, null);

		@SuppressWarnings("unchecked")
		IPluginIOList<ClickedPoint> outputCells = (IPluginIOList<ClickedPoint>) pluginOutputs.get("Cells");
		inputCells.copyInto(outputCells);
		outputCells.clear();

		ParFor parFor = new ParFor("MergeCells", 0, inputCells.size() - 1, r, true);
		final ConcurrentHashMap<Integer, ClickedPoint> map =
				new ConcurrentHashMap<>(inputCells.size(), 0.75f, parFor.getNThreads());

		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker((loopIndex, threadIndex) -> {
				ClickedPoint pCloned = (ClickedPoint) inputCells.get(loopIndex).clone();
				map.put((int) pCloned.getSeedId(), pCloned);
				return pCloned;
			});

		for (Object p : parFor.run(true)) {
			outputCells.addDontFireValueChanged((ClickedPoint) p);
		}

		parFor = new ParFor(0, inputCells2.size() - 1, r, threadPool, true);
		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker((loopIndex, threadIndex) -> {
				ClickedPoint p = inputCells2.get(loopIndex);
				if (map.containsKey((int) p.getSeedId()))
					return null;
				ClickedPoint pCloned = (ClickedPoint) p.clone();
				map.put((int) pCloned.getSeedId(), pCloned);
				return pCloned;
			});

		parFor.run(true).stream().filter(p -> p != null).forEach(
				p -> outputCells.addDontFireValueChanged((ClickedPoint) p));

		outputCells.fireValueChanged(false, false);

		postRun((PluginIOCells) outputCells);
	}

	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) {
		throw new IllegalStateException("Should not be called");
	}
}
