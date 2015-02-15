/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import ij.CompositeImage;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.image.ColorModel;

import pipeline.GUI_utils.image_with_toolbar.PluginIOHyperstackWithToolbar;
import pipeline.data.IPluginIOHyperstack;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class PluginIOHyperstackViewWithBufferedImagePlus extends PluginIOHyperstackWithToolbar {

	public PluginIOHyperstackViewWithBufferedImagePlus(String s) {
		super(s);
	}

	@Override
	protected final void updateImpAndDrawLater() {
		// Copied from computeStack() in parent; we should find a way of unifying this code

		if (displayedImages == null)
			return;
		IPluginIOHyperstack[] hyperstackArray = displayedImages.toArray(new IPluginIOHyperstack[] {});
		if (hyperstackArray.length == 0)
			return;

		ImageStack imageStack = imp.getImageStack();

		int sliceCounter = 1;
		for (int z = 0; z < depth; z++) {
			for (IPluginIOHyperstack element : hyperstackArray) {

				for (int c = 0; c < element.getnChannels(); c++) {
					Object pixels = element.getPixels(z, c, 0);
					System.arraycopy(pixels, 0, imageStack.getPixels(sliceCounter++), 0, height * width);

				}
			}
		}

		super.updateImpAndDrawLater();
	}

	/**
	 * Create an ImageJ ImageStack to display the contents, and call
	 * {@link pipeline.GUI_utils.image_with_toolbar.PluginIOHyperstackWithToolbar#createImagePlus} to display the
	 * [hyper]stack.
	 */
	@Override
	synchronized protected void computeStack() {
		if (displayedImages == null)
			return;
		IPluginIOHyperstack[] hyperstackArray = displayedImages.toArray(new IPluginIOHyperstack[] {});
		if (hyperstackArray.length == 0)
			return;
		width = hyperstackArray[0].getWidth();
		height = hyperstackArray[0].getHeight();
		depth = hyperstackArray[0].getDepth();
		ImageStack imageStack = new ImageStack(width, height);
		ColorModel colorModel = null;
		double min = 0, max = 0;
		if ((imp != null) && (imp.getStack() != null) && (imp.getProcessor() != null)) {
			colorModel = imp.getProcessor().getCurrentColorModel();// imageStack.setColorModel(imp.getProcessor().getCurrentColorModel());
			imageStack.setColorModel(colorModel);
			min = imp.getProcessor().getMin();
			max = imp.getProcessor().getMax();
		}
		nChannels = 0;
		String metadata = null;
		for (int z = 0; z < depth; z++) {
			for (IPluginIOHyperstack element : hyperstackArray) {
				if (z == 0) {
					nChannels += element.getnChannels();
					Object hyperstackMetadata = element.getImageAcquisitionMetadata();
					if (hyperstackMetadata instanceof String) {
						String s = (String) hyperstackMetadata;
						if (!"".equals(s)) {
							if (metadata == null) {
								metadata = s;
							} else {
								metadata += s;
							}
						}
					}
				}
				for (int c = 0; c < element.getnChannels(); c++) {
					Object pixels = element.getPixels(z, c, 0);
					ImageProcessor sliceToAdd;
					if (pixels instanceof float[]) {
						float[] newPixels = new float[width * height];
						System.arraycopy(pixels, 0, newPixels, 0, width * height);
						sliceToAdd = new FloatProcessor(width, height, newPixels, null);
					} else if (pixels instanceof byte[]) {
						byte[] newPixels = new byte[width * height];
						System.arraycopy(pixels, 0, newPixels, 0, width * height);
						sliceToAdd = new ByteProcessor(width, height, newPixels, null);
					} else if (pixels instanceof short[]) {
						short[] newPixels = new short[width * height];
						System.arraycopy(pixels, 0, newPixels, 0, width * height);
						sliceToAdd = new ShortProcessor(width, height, newPixels, null);
					} else
						throw new RuntimeException("Unknow pixel type " + pixels);
					if (colorModel != null)
						sliceToAdd.setColorModel(colorModel);
					if ((min != 0) || (max != 0))
						sliceToAdd.setMinAndMax(min, max);
					imageStack.addSlice("", sliceToAdd);
				}
			}
		}

		if ((lastNChannels != nChannels) && (imp != null)) {
			closeImpDontNotifyListeners(imp);
			if (orthogonalViews != null) {
				listeners.remove(orthogonalViews);
				orthogonalViews.dispose();
				orthogonalViews = null;
			}
			imp = null;
		}
		lastNChannels = nChannels;

		if (imageStack.getSize() > 0) {
			if (imp instanceof CompositeImage) {
				// FIXME CompositeImages don't update properly if their stack is changed. This looks like an ImageJ bug.
				Utils.log("Not updating CompositeImage stack to bypass ImageJ bug", LogLevel.WARNING);
				// imp.setStack(imageStack);
			} else {
				createImagePlus(name, imageStack, metadata);
			}
			// keep orthogonal view creation for later because orthogonal view might try to access data too early
		}
		if (imp != null) {
			if (!(imp instanceof CompositeImage))
				imp.setDimensions(nChannels, depth, 1);
			if (!(imp instanceof CompositeImage))
				imp.setCalibration(hyperstackArray[0].getCalibration());
			if (!(imp instanceof CompositeImage))
				imp.setOpenAsHyperStack(true);
			// imp.updateAndDraw();
		} else
			Utils.log("Empty stack for " + name, LogLevel.ERROR);
	}
}
