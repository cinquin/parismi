/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;

// From http://www.objectdefinitions.com/odblog/2007/swing-garbage-collection-problems-with-the-observer-pattern/
public class MouseListenerWeakRef implements MouseListener {

	private WeakReference<MouseListener> delegateWeakRef;

	public MouseListenerWeakRef(MouseListener actionListener) {
		this.delegateWeakRef = new WeakReference<>(actionListener);
	}

	public MouseListener getWeakRef() {
		return delegateWeakRef.get();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		MouseListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.mouseClicked(e);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		MouseListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.mouseEntered(e);
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		MouseListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.mouseExited(e);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		MouseListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.mousePressed(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		MouseListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.mouseReleased(e);
		}
	}

}
