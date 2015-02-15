/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import ij.ImageListener;
import ij.ImagePlus;

import java.lang.ref.WeakReference;

// From http://www.objectdefinitions.com/odblog/2007/swing-garbage-collection-problems-with-the-observer-pattern/
public class ImageListenerWeakRef implements ImageListener {

	private WeakReference<ImageListener> delegateWeakRef;

	public ImageListenerWeakRef(ImageListener actionListener, boolean register) {
		this.delegateWeakRef = new WeakReference<>(actionListener);
		if (register)
			ImagePlus.addImageListener(this);
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		// Utils.log("Image closed "+imp,LogLevel.DEBUG);
		ImageListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.imageClosed(imp);
		}
	}

	@Override
	public void imageOpened(ImagePlus imp) {
		ImageListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.imageOpened(imp);
		}
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		ImageListener delegate = delegateWeakRef.get();
		if (delegate != null) {
			delegate.imageUpdated(imp);
		}
	}

}
