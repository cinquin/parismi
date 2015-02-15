/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import java.awt.event.MouseEvent;

import pipeline.data.PluginIO;

/**
 * Interface implemented by plugins that are able to respond to mouse events in a window
 * that's not necessarily theirs (e.g. click in final segmentation results after active contours
 * are run, to add a new seed).
 *
 */
public interface MouseEventPlugin {

	/**
	 * Notifies plugin of a click, or series of clicks.
	 * 
	 * @param clickedPoints
	 *            coalesced list of points that have been clicked since the last time MouseClicked was called.
	 * @param inputHasChanged
	 *            true if input has potentially changed since last time the plugin was called
	 * @param generatingEvent
	 *            Optional: the AWT event that corresponds to the click; may be null if the caller decides not
	 *            to pass along that information (for example if the "click" was not user-generated).
	 * @return Same value as returned by a plugin called in the regular way (e.g. runChannel): 0 if no error.
	 *         THREAD_INTERRUPTED is thread was interrupted. error if some other error occurred.
	 * @throws InterruptedException
	 */
	public int mouseClicked(PluginIO clickedPoints, boolean inputHasChanged, MouseEvent generatingEvent)
			throws InterruptedException;

	/**
	 * Signals that any clicks that have not been already processed should now be processed. Used for example
	 * to flush buffered clicks for active contours.
	 */
	public void processClicks();

	public static final int PROGRAM_LAUNCHED_IN_RESPONSE_TO_CLICK = 1;
	public static final int PROGRAM_LAUNCHED_IN_RESPONSE_TO_PIPELINE = 2;
}
