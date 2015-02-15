/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.ref.WeakReference;

// From http://www.objectdefinitions.com/odblog/2007/swing-garbage-collection-problems-with-the-observer-pattern/
public class WindowListenerWeakRef implements WindowListener {

	private WeakReference<WindowListener> delegateWeakRef;

	public WindowListenerWeakRef(WindowListener actionListener) {
		this.delegateWeakRef = new WeakReference<>(actionListener);
	}

	public WindowListener getWeakRef() {
		return delegateWeakRef.get();
	}

	@Override
	public void windowOpened(WindowEvent e) {
		WindowListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.windowOpened(e);
		}
	}

	@Override
	public void windowClosing(WindowEvent e) {
		WindowListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.windowClosing(e);
		}
	}

	@Override
	public void windowClosed(WindowEvent e) {
		WindowListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.windowClosed(e);
		}
	}

	@Override
	public void windowIconified(WindowEvent e) {
		WindowListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.windowIconified(e);
		}
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		WindowListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.windowDeiconified(e);
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {
		WindowListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.windowActivated(e);
		}
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		WindowListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.windowDeactivated(e);
		}
	}

}
