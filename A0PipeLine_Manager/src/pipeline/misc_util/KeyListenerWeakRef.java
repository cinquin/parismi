/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.ref.WeakReference;

// From http://www.objectdefinitions.com/odblog/2007/swing-garbage-collection-problems-with-the-observer-pattern/
public class KeyListenerWeakRef implements KeyListener {

	private WeakReference<KeyListener> delegateWeakRef;

	public KeyListenerWeakRef(KeyListener actionListener) {
		this.delegateWeakRef = new WeakReference<>(actionListener);
	}

	public KeyListener getWeakRef() {
		return delegateWeakRef.get();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		KeyListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.keyTyped(e);
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		KeyListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.keyPressed(e);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		KeyListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.keyReleased(e);
		}
	}

}
