/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.gui_control;

import java.awt.image.IndexColorModel;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;

/**
 * Apply fire lookup table to a window (probably does not work with composites as this point).
 *
 */
public class FireLookupTable extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Switch image window to fire lookup table";
	}

	@Override
	public String operationName() {
		return "FireLookupTable";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	private static int[] r = { 0, 0, 1, 25, 49, 73, 98, 122, 146, 162, 173, 184, 195, 207, 217, 229, 240, 252, 255,
			255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255 };
	private static int[] g = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 14, 35, 57, 79, 101, 117, 133, 147, 161, 175,
			190, 205, 219, 234, 248, 255, 255, 255, 255 };
	private static int[] b = { 0, 61, 96, 130, 165, 192, 220, 227, 210, 181, 151, 122, 93, 64, 35, 5, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 35, 98, 160, 223, 255 };

	private byte[] reds = new byte[256];
	private byte[] greens = new byte[256];
	private byte[] blues = new byte[256];

	{
		for (int i = 0; i < reds.length; i++) {
			int scaledIndex = (int) (i / (reds.length - 1f) * (r.length - 1f));
			reds[i] = (byte) r[scaledIndex];
			greens[i] = (byte) g[scaledIndex];
			blues[i] = (byte) b[scaledIndex];
		}
	}

	@Override
	public void run(ProgressReporter pr, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		for (IPluginIO source : getInputs().values()) {
			if (source instanceof IPluginIOImage) {
				IPluginIOImage image = (IPluginIOImage) source;
				if (image.getImp() != null) {
					image.getImp().imp.getProcessor().setColorModel(
							new IndexColorModel(8, reds.length, reds, greens, blues));
				} else {
					Utils.log("Source " + source + " has no associated view; cannot set imp", LogLevel.WARNING);
				}
			} else {
				throw new RuntimeException("Attempting to set LUT of source " + source + ", but it is not an image");
			}
		}

	}

}
