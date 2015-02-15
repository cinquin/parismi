/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import pipeline.data.PluginIO;

public interface IPipe {
	PipelinePlugin getInputPlugin();

	PipelinePlugin getOutputPlugin();

	String getInputName();

	String getOutputName();

	PluginIO getPluginIO();

	void setIntputPlugin(PipelinePlugin intputPlugin);

	void setPluginIO(PluginIO pluginIO);

	void setOutputPlugin(PipelinePlugin outputPlugin);

	void setInputName(String inputName);

	void setOutputName(String outputName);
}
