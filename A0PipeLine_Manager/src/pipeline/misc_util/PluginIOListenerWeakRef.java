/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.awt.AWTEvent;
import java.lang.ref.WeakReference;

import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.PluginIOListener;

// From http://www.objectdefinitions.com/odblog/2007/swing-garbage-collection-problems-with-the-observer-pattern/
public class PluginIOListenerWeakRef implements PluginIOListener {

	public WeakReference<PluginIOListener> actionListenerDelegate;

	public PluginIOListenerWeakRef(PluginIOListener actionListener) {
		this.actionListenerDelegate = new WeakReference<>(actionListener);
	}

	@Override
	public void pluginIOValueChanged(boolean stillChanging, IPluginIO pluginIOWhoseValueChanged) {
		PluginIOListener delegate = actionListenerDelegate.get();
		if (delegate != null) {
			delegate.pluginIOValueChanged(stillChanging, pluginIOWhoseValueChanged);
		}
	}

	@Override
	public void pluginIOViewEvent(PluginIOView trigger, boolean stillChanging, AWTEvent event) {
		PluginIOListener delegate = actionListenerDelegate.get();
		if (delegate != null) {
			delegate.pluginIOViewEvent(trigger, stillChanging, null);
		}
	}

}
