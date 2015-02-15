/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import org.jfree.data.xy.XYSeries;

import pipeline.data.ClickedPoint;

public class XYSeriesE extends XYSeries implements PluginIOCellsListeningSeries {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9039019686411146637L;

	public XYSeriesE(Comparable<?> key) {
		super(key);
	}

	/**
	 * When this series is bound to a PluginIOListOf5Dpoints, name of the ClickedPoint field
	 * that should correspond to the x axis for this XYSeries. This makes it possible to
	 * automatically update this XYSeries by reflection.
	 */
	String nameForXSeries;
	/**
	 * When this series is bound to a PluginIOListOf5Dpoints, name of the ClickedPoint field
	 * that should correspond to the y axis for this XYSeries. This makes it possible to
	 * automatically update this XYSeries by reflection.
	 */
	String nameForYSeries;

	public String displayNameForXSeries;
	public String displayNameForYSeries;

	public XYSeries trendLine = new XYSeries("Smoothed");

	float smoothingScale = 0.0f;

	void recomputeTrendLine() {
	}

	public void setSmoothingScale(float scale, boolean recompute) {
		smoothingScale = scale;
		if (recompute)
			recomputeTrendLine();
	}

	boolean automaticallyUpdateTrendline = true;

	public void setAutomaticTrendlineUpdate(boolean update) {
		automaticallyUpdateTrendline = update;
	}

	boolean flipAxes = false;

	public void setFlipAxes(boolean b) {
		flipAxes = b;
		recomputeTrendLine();
	}

	public double[] getXValues() {
		int size = getItemCount();
		double[] xValues = new double[size];
		for (int i = 0; i < size; i++) {
			xValues[i] = getX(i).doubleValue();
		}
		return xValues;
	}

	@Override
	public void add(ClickedPoint p) {

	}

	@Override
	public void add(ClickedPoint[] p) {

	}

}
