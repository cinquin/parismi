/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;

class GlassFrame extends JComponent {
	private static final long serialVersionUID = 1L;

	public GlassFrame() {
		super();
	}

	public void setAlpha(int alpha) {
		this.alpha = alpha;
	}

	private int alpha = 200;

	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setColor(new Color(200, 200, 200, alpha));
		g2.fillRect(0, 0, getWidth(), getHeight());
		g2.dispose();
	}

	// See http://weblogs.java.net/blog/alexfromsun/archive/2006/09/a_wellbehaved_g.html
	@Override
	public boolean contains(int x, int y) {
		return false;
	}
}
