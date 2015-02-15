/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.flow_control;

import pipeline.PreviewType;
import pipeline.misc_util.ProgressReporter;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;

/**
 * Pauses the pipeline. For now the pipeline detects this plugin and just stops execution, but in the future
 * this plugin will just pause until some condition such as user input has been met.
 *
 */
public class Pause extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Pause the pipeline; user resumes by starting from following step";
	}

	@Override
	public String operationName() {
		return "Pause";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		// This plugin is recognized by scheduler, which pauses when it encounters it
	}

}
