/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils.image_with_toolbar;

import ij.ImagePlus;
import ij.gui.ImageWindow;

/**
 * Replaces ImageJ-type windows used to display non-stack images, to allow displaying of a toolbar at the bottom.
 * For class this to work, the ImageJ ImageWindow paint function has to be modified to paint all the components.
 *
 */
class ImageWindowWithToolbar extends ImageWindow {

	public ImageWindowWithToolbar(ImagePlus imp, Toolbar toolbar) {
		super(imp);
		this.toolbar = toolbar;
		add(this.toolbar, 0);
	}

	private static final long serialVersionUID = 1896585540379035573L;

	private Toolbar toolbar;

}
