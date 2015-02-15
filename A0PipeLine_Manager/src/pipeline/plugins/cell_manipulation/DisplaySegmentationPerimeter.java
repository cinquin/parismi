/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

public class DisplaySegmentationPerimeter extends DisplaySolidSegmentation {

	@Override
	protected boolean usePerimeter() {
		return true;
	}

	@Override
	public String getToolTip() {
		return "From a set of cells create an image by filling in cell segmentation"
				+ "perimeters with pixel value derived from " + "user-defined field";
	}

	@Override
	public String operationName() {
		return "DisplaySolidSegmentation";
	}

}
