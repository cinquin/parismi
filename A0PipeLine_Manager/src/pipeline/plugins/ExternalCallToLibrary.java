/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import java.util.ArrayList;
import java.util.List;

import pipeline.data.IPluginIOImage;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOUnknownBinary;
import pipeline.external_plugin_interfaces.JNACallToNativeLibrary;

public abstract class ExternalCallToLibrary extends ExternalCall {

	protected final static int numberProtobufInputFiles = 3;

	@Override
	public void createNewLink() {
		link = new JNACallToNativeLibrary(input, output, getFirstArg(), pluginInputs, pluginOutputs);
	}

	@Override
	public int getFlags() {
		return ONLY_FLOAT_INPUT + ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL + SAME_AS_FLOAT + DONT_PARALLELIZE;
		// by default, parallelization is expected to be implemented by the library
	}

	@Override
	public void loadExtraInputArgsForEstablishment(List<String> args) {
		String[] labels = getInputLabels();
		for (String label : labels) {
			if (getInput(label) instanceof IPluginIOImage)
				args.add(label);
		}
	}

	@Override
	public void loadExtraOutputArgs(List<String> args, String firstOutputName) {
		String[] labels = getOutputLabels();
		for (String label : labels) {
			if (getOutput(label) instanceof IPluginIOImage)
				args.add(label);
		}
	}

	void loadOutputProtobufNames(List<String> args) {
		String[] labels = getOutputLabels();
		for (String label : labels) {
			if (getOutput(label) instanceof PluginIOCells || getOutput(label) instanceof PluginIOUnknownBinary)
				args.add(label);
		}
	}

	@Override
	public String getFirstArg() {
		return "segpipeline_1";
	}

	@Override
	public List<String> getInputArgs() {
		List<String> inputArgs = new ArrayList<>(6);
		if (getInputs().containsKey("Default source"))
			inputArgs.add("Default source");
		loadExtraInputArgsForEstablishment(inputArgs);
		padWithDummyInputFileNames(inputArgs);

		int numberProtobuf = getNonImageInputs(inputArgs);
		if (numberProtobuf > numberProtobufInputFiles) {
			throw new IllegalStateException("Found " + numberProtobuf
					+ " non image pluginInputs to pass as input in plugin " + this + " but the maximum is "
					+ numberProtobufInputFiles);
		}
		for (int i = 0; i < numberProtobufInputFiles - numberProtobuf; i++) { // pad to a total of
																				// numberProtobufInputFiles protobuf
																				// input files
			inputArgs.add("0");
		}
		return inputArgs;
	}

	@Override
	public List<String> getOutputArgs() {
		List<String> outputArgs = new ArrayList<>(6);
		if (getOutputs().containsKey("Default destination"))
			outputArgs.add("Default destination");
		loadExtraOutputArgs(outputArgs, "Default destination");
		padWithDummyOutputFileNames(outputArgs);

		loadOutputProtobufNames(outputArgs);

		return outputArgs;
	}

}
