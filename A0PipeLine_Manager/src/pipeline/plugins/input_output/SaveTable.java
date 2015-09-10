/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import pipeline.A0PipeLine_Manager.TableSelectionDemo.TableNotComputed;
import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

public class SaveTable extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Save pipeline table as XML";
	}

	public static boolean disableSaving = false;

	@SuppressWarnings("null")
	@Override
	public void run(final ProgressReporter r, final MultiListParameter inChannels, final TableParameter outChannels,
			final PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		if (disableSaving) {
			Utils.log("Table saving is disabled; not running SaveTable plugin", LogLevel.INFO);
			return;
		}

		IPluginIO prefix = pluginInputs.get("File name");
		String fileNamePrefix = prefix != null ? ((PluginIOString) prefix).getString() + "_" : "";

		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + "/" + fileNamePrefix
						+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));
		File directory = new File(FileNameUtils.removeIncrementationMarks((String) workingDirectory.getValue()));
		if (!(directory.exists() && directory.isDirectory())) {
			throw new PluginRuntimeException("File " + FileNameUtils.compactPath(fileNameString)
					+ " does not exist or is not a directory", true);
		}

		if (!fileNameString.contains(".xml"))
			fileNameString += ".xml";

		try (PrintWriter out = new PrintWriter(fileNameString)) {
			try {
				out.println(pipelineCallback.getTableString(ourRow));
			} catch (TableNotComputed e) {
				Utils.log("Table not computed", LogLevel.DEBUG);
				// ignore; this is probably the first time we run
			}
		} catch (FileNotFoundException e) {
			throw new PluginRuntimeException("Could not write table to file "
					+ FileNameUtils.compactPath(fileNameString), e, true);
		}

		return;
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT + CUSTOM;
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory", "Directory to save table in",
			"", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save table to file", "Save to file", "xxxxxxxxx", true,
			null);

	private AbstractParameter splitDirectoryAndFile = null;

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory });
		}
		AbstractParameter[] paramArray = { null, splitDirectoryAndFile };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		Object[] splitParameters = (Object[]) param[1].getValue();
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];

	}

	@Override
	public String operationName() {
		return "SaveTable";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "File name" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] {};
	}
}
