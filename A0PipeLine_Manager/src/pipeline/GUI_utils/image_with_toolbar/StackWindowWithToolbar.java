/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils.image_with_toolbar;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Replaces ImageJ-type windows used to display stacks, to allow displaying of a toolbar at the bottom.
 * For class this to work, the ImageJ ImageWindow paint function has to be modified to paint all the components.
 *
 */
public class StackWindowWithToolbar extends StackWindow implements MouseListener, KeyListener {

	public StackWindowWithToolbar(ImagePlus imp, Toolbar toolbar) {
		this(imp, toolbar, null);
	}

	public StackWindowWithToolbar(ImagePlus imp, Toolbar toolbar, ImageCanvas ic) {
		super(imp, ic);
		this.toolbar = toolbar;
		if (toolbar != null) {
			add(this.toolbar);
			Dimension size = getSize();
			size.height += toolbar.getSize().getHeight() + 50;
			setSize(size);
			pack();
			toolbar.requestFocus(false);
			toolbar.addKeyListener(this);
		}
		this.addMouseListener(this);
		this.getCanvas().addMouseListener(this);
	}

	private static final long serialVersionUID = 1896585540379035573L;

	public Toolbar toolbar;

	/**
	 * Updates the window's toolbar, replacing the previous one.
	 * 
	 * @param toolbar
	 *            may be null
	 */
	public void setToolbar(Toolbar toolbar) {
		if (this.toolbar != null)
			remove(this.toolbar);
		this.toolbar = toolbar;
		if (toolbar == null)
			return;
		add(toolbar);
		toolbar.requestFocus(false);
		toolbar.addKeyListener(this);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (toolbar != null)
			toolbar.requestFocus();
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (toolbar != null)
			toolbar.requestFocus();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// Utils.log("Mouse released",LogLevel.VERBOSE_VERBOSE_DEBUG);
		if (toolbar != null)
			toolbar.requestFocus();
	}

	@Override
	public boolean isFocusTraversable() {
		return false;
	}

	@Override
	// So that ImageJ keyboard shortcuts are not swallowed up by the toolbar
	// For now this only passes zooming shortcuts (and maybe not all of the time)
	// There should be a better solution
			public
			void keyPressed(KeyEvent e) {
		// Utils.log("Key pressed "+e.getKeyChar(),LogLevel.INFO);
		if ((e.getKeyChar() == ('=')) || (e.getKeyChar() == ('-')))
			getCanvas().dispatchEvent(e);
		else if (getCanvas() instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) getCanvas()).keyPressed(e);
		}
	}

	@Override
	// So that ImageJ keyboard shortcuts are not swallowed up by the toolbar
	// For now this only passes zooming shortcuts (and maybe not all of the time)
	// There should be a better solution
			public
			void keyReleased(KeyEvent e) {
		if ((e.getKeyChar() == ('=')) || (e.getKeyChar() == ('-')))
			getCanvas().dispatchEvent(e);
		else if (getCanvas() instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) getCanvas()).keyReleased(e);
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// Utils.log("Key typed "+e.getKeyChar(),LogLevel.INFO);
		char keyTyped = e.getKeyChar();

		if ((keyTyped == ('=')) || (keyTyped == ('-')))
			getCanvas().dispatchEvent(e);
		else if (getCanvas() instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) getCanvas()).keyTyped(e);
		}
	}
}
