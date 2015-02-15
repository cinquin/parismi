/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import ij.measure.Calibration;

import java.io.File;
import java.io.IOException;

import processing_utilities.pcurves.PrincipalCurve.PrincipalCurveClass;

public class PluginIOPrincipalCurve extends PluginIO implements IDimensions, PluginIOCalibrable {

	private static final long serialVersionUID = 1L;

	private PrincipalCurveClass pCurve;

	public PluginIOPrincipalCurve(PrincipalCurveClass curve) {
		pCurve = curve;
	}

	public PrincipalCurveClass getPCurve() {
		return pCurve;
	}

	public void setPCurve(PrincipalCurveClass p) {
		pCurve = p;
	}

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	private int width, height, depth;

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getDepth() {
		return depth;
	}

	@Override
	public void setDepth(int depth) {
		this.depth = depth;
	}

	private Calibration calibration;

	@Override
	public Calibration getCalibration() {
		return calibration;
	}

	@Override
	public void setCalibration(Calibration calibration) {
		this.calibration = calibration;
	}

}
