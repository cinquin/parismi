/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import pipeline.data.PluginIO;

public class Pipe implements IPipe {

	private transient PluginIO pluginIO;

	public PipelinePlugin getIntputPlugin() {
		return intputPlugin;
	}

	@Override
	public void setIntputPlugin(PipelinePlugin intputPlugin) {
		this.intputPlugin = intputPlugin;
	}

	@Override
	public void setPluginIO(PluginIO pluginIO) {
		this.pluginIO = pluginIO;
	}

	@Override
	public void setOutputPlugin(PipelinePlugin outputPlugin) {
		this.outputPlugin = outputPlugin;
	}

	@Override
	public void setInputName(String inputName) {
		this.inputName = inputName;
	}

	@Override
	public void setOutputName(String outputName) {
		this.outputName = outputName;
	}

	private PipelinePlugin intputPlugin, outputPlugin;
	private String inputName, outputName;

	@Override
	public PipelinePlugin getInputPlugin() {
		return intputPlugin;
	}

	@Override
	public PipelinePlugin getOutputPlugin() {
		return outputPlugin;
	}

	@Override
	public String getInputName() {
		return inputName;
	}

	@Override
	public String getOutputName() {
		return outputName;
	}

	@Override
	public PluginIO getPluginIO() {
		return pluginIO;
	}

}
