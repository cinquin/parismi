/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * This class is used as a workaround for the lack of a unified series collection in jFreeChart.
 *
 */

@SuppressWarnings("unchecked")
public class XYSeriesCollectionGenericManipulator extends XYSeriesCollection implements
		FreeChartSeriesCollectionManipulator {

	private static final long serialVersionUID = 1L;

	@Override
	public void addSeries(Object series) {
		super.addSeries((XYSeries) series);
	}

}
