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
import pipeline.parameters.PipelineParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;

/**
 * Runs in batch a pipeline that is given as parameter (cannot be persisted for now, so user much drag every time an
 * open pipeline
 * window into the first parameter).
 *
 */
public class BatchRun extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Run in batch a pipeline that is given as a parameter (cannot be persisted for now, so user much drag every time an open pipeline "
				+ "window into the first parameter).";
	}

	private PipelineParameter pipelineParameter = new PipelineParameter("Drag pipeline \"Drag me\"",
			"Drag pipeline \"Drag me\" into here", "", true, null);

	@Override
	public void setParameters(AbstractParameter[] param) {
		pipelineParameter = (PipelineParameter) param[0];
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { pipelineParameter, null };
		return paramArray;
	}

	@Override
	public String operationName() {
		return "BatchRun";
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

		pipelineParameter.getPipeline().batch(true, false);
	}

}
