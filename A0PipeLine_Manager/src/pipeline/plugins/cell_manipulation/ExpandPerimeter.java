/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections.primitives.ArrayIntList;

import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class ExpandPerimeter extends CellTransform {

	@Override
	public String getToolTip() {
		return "From a segmentation, expand permiter outwards; used for membrane quantification using "
				+ "active contours that did not always quite reach the edge";
	}

	private AtomicInteger missingSegmentation = new AtomicInteger(0);

	@Override
	protected void preRun(PluginIOCells inputCells, IPluginIOHyperstack inputImage) {
		missingSegmentation.set(0);
	}

	@Override
	protected void postRun(PluginIOCells outputCells) {
		if (missingSegmentation.get() > 0) {
			Utils.displayMessage("Warning: " + missingSegmentation.get() + " had a missing segmentation"
					+ " and could not be recentered.", true, LogLevel.WARNING);
		}
	}

	private class Point {
		public int x, y, z;

		public Point(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public int hashCode() {
			int hash = 23;
			hash = hash * 31 + x;
			hash = hash * 31 + y;
			hash = hash * 31 + z;
			return hash;
		}

		@Override
		public boolean equals(Object p) {
			if (!(p instanceof Point))
				return false;
			Point q = (Point) p;
			return x == q.x && y == q.y && z == q.z;
		}
	}

	@ParameterInfo(userDisplayName = "Radius in microns", floatValue = 0.2f, noErrorIfMissingOnReload = false)
	private float boxRadius;

	@ParameterInfo(userDisplayName = "Only expand outwards", stringValue = "FALSE", noErrorIfMissingOnReload = true)
	private boolean onlyExpandOutwards;

	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) {

		ClickedPoint pCloned = (ClickedPoint) point.clone();

		if (point.imageFullSegCoordsX == null || point.imageFullSegCoordsX.length == 0) {
			missingSegmentation.getAndIncrement();
			return pCloned;
		}

		Set<Point> innerPoints = new HashSet<>();

		int nInnerPoints = point.imageFullSegCoordsX.length;

		if (onlyExpandOutwards)
			for (int i = 0; i < nInnerPoints; i++) {
				float x = point.imageFullSegCoordsX[i];
				float y = point.imageFullSegCoordsY[i];
				float z = point.imageFullSegCoordsZ[i];
				innerPoints.add(new Point((int) x, (int) y, (int) z));
			}

		int nPoints = point.imagePerimsegCoordsX.length;

		int xyRad = (int) (boxRadius / point.xyCalibration);
		int zRad = (int) (boxRadius / point.zCalibration);

		int width = ((PluginIOCells) allInputPoints).getWidth();
		int height = ((PluginIOCells) allInputPoints).getHeight();
		int depth = ((PluginIOCells) allInputPoints).getDepth();

		ArrayIntList xCoord = new ArrayIntList(500);
		ArrayIntList yCoord = new ArrayIntList(500);
		ArrayIntList zCoord = new ArrayIntList(500);

		for (int ii = 0; ii < nPoints; ii++) {
			int x = point.imagePerimsegCoordsX[ii];
			int y = point.imagePerimsegCoordsY[ii];
			int z = point.imagePerimsegCoordsZ[ii];

			innerPoints.add(new Point(x, y, z));

			xCoord.add(x);
			yCoord.add(y);
			zCoord.add(z);

			int z0 = Math.min(z, zRad);
			int z1 = Math.min(depth - 1 - z, zRad);

			int y0 = Math.min(y, xyRad);
			int y1 = Math.min(height - 1 - y, xyRad);

			int x0 = Math.min(x, xyRad);
			int x1 = Math.min(width - 1 - x, xyRad);

			for (int k = -z0; k <= z1; k++) {
				for (int j = -y0; j <= y1; j++) {
					for (int i = -x0; i <= x1; i++) {

						Point candidate = new Point(x + i, y + j, z + k);
						if (innerPoints.contains(candidate))
							continue;
						xCoord.add(x + i);
						yCoord.add(y + j);
						zCoord.add(z + k);
						// add point to innerPoints so it doesn't get added more than once to the new perimeter
						innerPoints.add(candidate);
					}
				}
			}
		}

		pCloned.imagePerimsegCoordsX = xCoord.getIntArrayFast();
		pCloned.imagePerimsegCoordsY = yCoord.getIntArrayFast();
		pCloned.imagePerimsegCoordsZ = zCoord.getIntArrayFast();

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
