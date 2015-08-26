/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.gui_control;

import ij.plugin.frame.Channels;
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
 * Iterates through all inputs and tries to convert them to composite display mode.
 *
 */
public class MakeComposite extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Convert image displays to composite display mode" + "";
	}

	@Override
	public String operationName() {
		return "MakeComposite";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	@SuppressWarnings("unused")
	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		for (IPluginIO source : getInputs().values()) {
			if (source instanceof IPluginIOImage) {
				IPluginIOImage image = (IPluginIOImage) source;
				if (image.getImp() != null) {
					final boolean reopenChannels;
					Channels channels = (Channels) Channels.getInstance();
					if (channels != null) {
						reopenChannels = true;
						channels.close();
					} else {
						reopenChannels = false;
					}
					image.getImp().toComposite();
					if (reopenChannels) {
						new Channels();
					}
				} else {
					Utils.log("Source " + source + " has no associated view; cannot convert to composite",
							LogLevel.WARNING);
				}
			} else {
				Utils.log("Attempting to make composite of source " + source + ", but it is not an image",
						LogLevel.ERROR);
			}
		}
	}

}
