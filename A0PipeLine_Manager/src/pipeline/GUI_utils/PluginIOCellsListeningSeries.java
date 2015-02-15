/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import pipeline.data.ClickedPoint;

/**
 * Used by PluginIOCells to describe to listeners changes that were made, to avoid them having to read the
 * whole dataset again.
 *
 */
public interface PluginIOCellsListeningSeries {
	public void clear();

	public void add(ClickedPoint p);

	public void add(ClickedPoint[] p);
}
