/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import pipeline.misc_util.Utils;

/**
 * Interface implemented by plugins that should be reset when their input changes. This includes for example
 * plugins that record mouse clicks, because one does not want to carry over the mouse click records from one
 * image to another.
 *
 */
public interface ResettablePlugin {
	/**
	 * Called when the input image of the plugin has changed, so the plugin resets any parameters
	 * that should not be carried over from one image to another (e.g. records of mouse clicks performed by the user).
	 */
	void reset() throws Utils.ImageOpenFailed;
}
