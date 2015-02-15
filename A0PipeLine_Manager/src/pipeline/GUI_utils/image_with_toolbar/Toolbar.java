/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils.image_with_toolbar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pipeline.GUI_utils.PluginIOView;
import pipeline.data.PluginIOListener;
import pipeline.misc_util.Utils;

/**
 * Generic toolbar to be displayed ot the bottom of an image. Extended in particular by {@link ActiveContourToolbar}.
 *
 */
public abstract class Toolbar extends JPanel implements ActionListener, ItemListener {

	public abstract void publicProcessKeyEvent(KeyEvent e);

	private static final long serialVersionUID = 1L;

	/**
	 * Called by window owner when a click in the image has occurred.
	 * 
	 * @param e
	 *            The event containing the details of the click.
	 * @return The click group assigned to the click corresponding to event e. This is used for example
	 *         to merge segmentations.
	 */
	public abstract int getClickGroup(MouseEvent e);

	/**
	 * Reads the current status of the toolbar.
	 * 
	 * @return Number that represents what action to perform with clicks (for example, add, delete, merge segmentation).
	 */
	public abstract int getCurrentModifier();

	private transient List<PluginIOListener> listeners = new ArrayList<>();

	public void addListener(PluginIOListener listener) {
		listeners.add(listener);
	}

	public void removeListener(PluginIOListener listener) {
		listeners.remove(listener);
	}

	/**
	 * True if toolbar is in a state where clicks should not be intercepted, and used to
	 * browse the image as one normally would with the ImageJ interface
	 */
	public boolean browse = true;

	JToggleButton browseButton;

	transient PluginIOView parentView;

	@Override
	public void actionPerformed(ActionEvent e) {
		// If buttons require immediate action, rather than the selected button being read later,
		// this is the place to do it.
		for (PluginIOListener listener : listeners) {
			try {
				listener.pluginIOViewEvent(parentView, false, e);
			} catch (Exception e1) {
				Utils.printStack(e1);
			}
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (browseButton != null) {
			browse = browseButton.isSelected();
		}
		actionPerformed(null);
	}
}
