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
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class CellRecenter extends CellTransform {

	@Override
	public String getToolTip() {
		return "From a segmentation, recenter each seed on segmentation center of mass; if input image "
				+ "is provided, weight center of mass by pixel intensity.";
	}

	private AtomicInteger missingSegmentation = new AtomicInteger(0);

	@Override
	protected void preRun(PluginIOCells inputCells, IPluginIOHyperstack inputImage) {
		missingSegmentation.set(0);
	}

	@Override
	protected void postRun(PluginIOCells outputCells) {
		if (missingSegmentation.get() > 0) {
			Utils.displayMessage("Warning: " + missingSegmentation.get() + " cells had a missing segmentation"
					+ " and could not be recentered.", true, LogLevel.WARNING);
		}
	}

	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) {

		ClickedPoint pCloned = (ClickedPoint) point.clone();

		if (point.imageFullSegCoordsX == null || point.imageFullSegCoordsX.length == 0) {
			missingSegmentation.getAndIncrement();
			return pCloned;
		}

		int nPoints = point.imageFullSegCoordsX.length;

		double xSum = 0, ySum = 0, zSum = 0;
		double pixelSum = 0;

		for (int i = 0; i < nPoints; i++) {
			float x = point.imageFullSegCoordsX[i];
			float y = point.imageFullSegCoordsY[i];
			float z = point.imageFullSegCoordsZ[i];
			float pixelValue = inputImage != null ? inputImage.getPixelValue((int) x, (int) y, (int) z, 1, 0) : 1;
			pixelSum += pixelValue;
			xSum += x * pixelValue;
			ySum += y * pixelValue;
			zSum += z * pixelValue;
		}

		pCloned.x = (int) (xSum / pixelSum);
		pCloned.y = (int) (ySum / pixelSum);
		pCloned.z = (int) (zSum / pixelSum);

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
