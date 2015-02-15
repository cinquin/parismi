/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import ij.measure.Calibration;
import pipeline.PreviewType;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

/**
 * Display segmentation by filling in cells with pixel value derived from a field
 *
 */
public class DisplaySolidSegmentation extends DisplaySeedFieldValue {

	@Override
	public String getToolTip() {
		return "From a set of cells create an image by filling in cell segmentations with pixel value derived from "
				+ "user-defined field";
	}

	@Override
	public String operationName() {
		return "DisplaySolidSegmentation";
	}

	@SuppressWarnings("static-method")
	boolean usePerimeter() {
		return false;
	}

	@Override
	public void runChannel(IPluginIOStack input, IPluginIOStack output, ProgressReporter r, PreviewType previewType,
			boolean inputHasChanged) {

		if (fieldNameParam.getSelection() == null)
			throw new IllegalStateException("No field selection has been made");

		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");

		BeanTableModel<?> tableModel = seeds.getBeanTableModel();
		int columnIndex = Utils.indexOf(getFieldNames(tableModel), fieldNameParam.getSelection());

		if (seeds.getCalibration() != null) {
			output.setCalibration((Calibration) seeds.getCalibration().clone());
		} else
			Utils.displayMessage("Warning: missing calibration in DisplaySolidSegmentation", true, LogLevel.WARNING);

		boolean noPixels = false;

		output.clearPixels();

		for (int i = 0; i < tableModel.getRowCount(); i++) {
			float f = tableModel.getFloatValueAt(i, columnIndex);

			if (f < minThreshold || f > maxThreshold)
				continue;

			ClickedPoint p = seeds.get(i);

			int[] xCoord = usePerimeter() ? p.imagePerimsegCoordsX : p.imageFullSegCoordsX;
			int[] yCoord = usePerimeter() ? p.imagePerimsegCoordsY : p.imageFullSegCoordsY;
			int[] zCoord = usePerimeter() ? p.imagePerimsegCoordsZ : p.imageFullSegCoordsZ;
			if (xCoord == null)
				throw new IllegalArgumentException("Missing segmentation");

			if (xCoord.length == 0) {
				noPixels = true;
			}
			for (int j = 0; j < xCoord.length; j++) {
				output.setPixelValue(xCoord[j], yCoord[j], zCoord[j], f);
			}

		}

		if (noPixels)
			Utils.displayMessage("Warning: there was at least 1 cell without a segmentation", true, LogLevel.WARNING);

	}

	@Override
	public String version() {
		return "1.0";
	}
}
