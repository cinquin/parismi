/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import pipeline.data.IPluginIO;

/**
 * Abstract class for GUI display of plugin input or output. So far ImagePlus, PluginIOListOf5DPoints and PluginIOCells
 * can be displayed.
 *
 */
public abstract class PluginIOView {

	public abstract void show();

	public abstract void close();

	public abstract void setData(IPluginIO data);
}
