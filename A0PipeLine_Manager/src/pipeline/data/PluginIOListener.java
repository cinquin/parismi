/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.awt.AWTEvent;

import pipeline.GUI_utils.PluginIOView;

public interface PluginIOListener {
	public void pluginIOValueChanged(boolean stillChanging, IPluginIO pluginIOWhoseValueChanged);

	public void pluginIOViewEvent(PluginIOView trigger, boolean stillChanging, AWTEvent event);

}
