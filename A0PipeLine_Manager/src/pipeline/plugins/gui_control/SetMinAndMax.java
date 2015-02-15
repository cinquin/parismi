/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.gui_control;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;

/**
 * Set minimum and max in display, to make for meaningful visual intensity comparisons across images.
 *
 */
public class SetMinAndMax extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Set minimum and max in display, to make for meaningful visual intensity comparisons across images";
	}

	@Override
	public String operationName() {
		return "SetMinAndMax";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	@ParameterInfo(userDisplayName = "Min", floatValue = 1, permissibleFloatRange = { 0, 100000 })
	public float min;

	@ParameterInfo(userDisplayName = "Max", floatValue = 66000, permissibleFloatRange = { 0, 100000 })
	public float max;

	@Override
	public void run(ProgressReporter pr, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		for (IPluginIO source : getInputs().values()) {
			if (source instanceof IPluginIOImage) {
				IPluginIOImage image = (IPluginIOImage) source;
				if (image.getImp() != null) {
					image.getImp().imp.getProcessor().setMinAndMax(min, max);
					image.getImp().imp.updateAndDraw();
				} else {
					Utils.log("Source " + source + " has no associated view; cannot set imp", LogLevel.WARNING);
				}
			} else {
				throw new RuntimeException("Attempting to set minmax of source " + source + ", but it is not an image");
			}
		}

	}

}
