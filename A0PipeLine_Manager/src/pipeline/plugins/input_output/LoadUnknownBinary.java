/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOUnknownBinary;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.SplitParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.PluginInfo;
import pipeline.plugins.ThreeDPlugin;

/**
 * Reads an input file as binary and stores it in a PluginIO of type UnknownBinary.
 *
 */
@PluginInfo(obsolete = true, suggestedReplacement = "LoadProtobufBinary")
public class LoadUnknownBinary extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Restore binary blob from file";
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Load directory",
			"Directory to read file from", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("File to load", "File to load", "xxxxxxxxx", true, null);

	private AbstractParameter splitDirectoryAndFile = null;

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory });
		}
		AbstractParameter[] paramArray = { splitDirectoryAndFile, null };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		Object[] splitParameters = (Object[]) param[0].getValue();
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];
	}

	@Override
	public String operationName() {
		return "LoadUnknownBinary";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_INPUT;
	}

	@Override
	public void runChannel(IPluginIOStack input, IPluginIOStack output, ProgressReporter r, PreviewType previewType,
			boolean inputHasChanged) {

		@SuppressWarnings("null")
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + "/"
						+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));
		File directory = new File((String) workingDirectory.getValue());
		if (!(directory.exists() && directory.isDirectory())) {
			throw new PluginRuntimeException("Directory " + fileNameString + " does not exist or is not a directory",
					true);
		}

		byte[] bytes = null;
		try (FileInputStream fileInput = new FileInputStream(fileNameString)) {
			long length = new File(fileNameString).length();
			if (length > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Protobuf file too large");
			bytes = new byte[(int) length];
			int read = fileInput.read(bytes);
			if (read < length) {
				throw new RuntimeException();
			}
		} catch (IOException e) {
			Utils.printStack(e);
		}

		PluginIOUnknownBinary binary = (PluginIOUnknownBinary) pluginOutputs.get("Binary");
		binary.setProperty("Protobuf", bytes);

		initializeOutputs();
		pluginOutputs.put("Binary", binary);
	}

	@Override
	public String[] getInputLabels() {
		return new String[] {};
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Binary";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Binary", desc0);

		return result;
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
		Utils.log("Creating binary", LogLevel.DEBUG);
		initializeOutputs();
		PluginIOUnknownBinary binary = new PluginIOUnknownBinary();
		pluginOutputs.put("Binary", binary);
		return null;
	}

}
