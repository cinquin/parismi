/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.util.List;

/**
 * This interface is used as a workaround for the lack of a unified series collection in jFreeChart.
 *
 */
public interface FreeChartSeriesCollectionManipulator {
	public List<Object> getSeries();

	public void removeAllSeries();

	public void addSeries(Object series);
}
