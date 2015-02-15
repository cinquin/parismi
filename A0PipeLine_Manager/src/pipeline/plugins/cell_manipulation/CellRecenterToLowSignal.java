/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import processing_utilities.stepByStepProjection.IProjector;

public class CellRecenterToLowSignal extends CellBallQuantify {

	@Override
	public String getToolTip() {
		return "From a segmentation, recenter each seed so that ball of specified diameter has smallest "
				+ "quantified value.";
	}

	private AtomicInteger missingSegmentation = new AtomicInteger(0);

	protected void preRun() {
		missingSegmentation.set(0);
	}

	protected void postRun() {
		if (missingSegmentation.get() > 0) {
			Utils.displayMessage("Warning: " + missingSegmentation.get() + " cells had a missing segmentation"
					+ " and could not be recentered.", true, LogLevel.WARNING);
		}
	}

	{
		applyRadiusToSegmentation = true;
	}

	@Override
	protected ClickedPoint transform(ClickedPoint p, IPluginIOList<ClickedPoint> allInputPoints, IPluginIOStack input,
			boolean fieldIsNew, PluginIOCells outputCells) {

		int saveX = (int) p.x, saveY = (int) p.y, saveZ = (int) p.z;
		int bestX = -1, bestY = -1, bestZ = -1;

		int xyRadius = 10;
		int zRadius = 5;

		double min = Double.MAX_VALUE;
		boolean foundPoint = false;

		for (int dx = -xyRadius + 1; dx < xyRadius; dx++) {
			p.x = saveX + dx;
			for (int dy = -xyRadius + 1; dy < xyRadius; dy++) {
				p.y = saveY + dy;
				for (int dz = -zRadius + 1; dz < zRadius; dz++) {
					p.z = saveZ + dz;
					IProjector projector = getProjector(p, allInputPoints, input);
					if (projector == null || projector.getNPoints() < 50)
						continue;
					double result = projector.project();
					if (result < min) {
						min = result;
						foundPoint = true;
						bestX = (int) p.x;
						bestY = (int) p.y;
						bestZ = (int) p.z;
					}
				}
			}
		}

		p.x = bestX;
		p.y = bestY;
		p.z = bestZ;

		ClickedPoint pCloned = (ClickedPoint) p.clone();
		pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
		pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();
		if (fieldIsNew)
			pCloned.getQuantifiedProperties().add(0f);

		if (!foundPoint) {
			cellsWithoutPixels.getAndIncrement();
			pCloned.setQuantifiedProperty(fieldName, 0);
		} else {
			pCloned.setQuantifiedProperty(fieldName, (float) min);
		}

		return pCloned;
	}

	@Override
	public String operationName() {
		return "Cell recenter";
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		Map<String, InputOutputDescription> result = super.getInputDescriptions();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.SHORT_TYPE, PixelType.BYTE_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF + ONLY_1_INPUT_CHANNEL;
	}

}
