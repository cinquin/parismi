/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.flow_control;

import pipeline.PreviewType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;

/**
 * Pauses pipeline execution for a specified amount of time.
 * TODO Create a single plugin that encompasses Pause and Wait
 *
 */
public class Wait extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Wait for a specified amount of time before execution resumes";
	}

	@Override
	public String operationName() {
		return "Wait";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	@ParameterInfo(userDisplayName = "Seconds to wait", description = "SecondsToWait", floatValue = 1,
			permissibleFloatRange = { 1, 3600 })
	protected int seconds;

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		Thread.sleep(seconds * 1000);
	}

}
