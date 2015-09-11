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

import pipeline.A0PipeLine_Manager;
import pipeline.PreviewType;
import pipeline.data.PluginIO;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;

import com.thoughtworks.xstream.XStream;

/**
 * Deserializes an XML object from a file.
 *
 */
public class LoadXMLObject extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Restore plugin output from XML file" + "";
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
		return "LoadXMLObject";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT + NO_INPUT;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		@SuppressWarnings("null")
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + "/"
						+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));
		@SuppressWarnings("null")
		File directory = new File(FileNameUtils.removeIncrementationMarks((String) workingDirectory.getValue()));
		if (!(directory.exists() && directory.isDirectory())) {
			throw new PluginRuntimeException("Directory " + directory.getAbsolutePath()
					+ " does not exist or is not a directory", true);
		}

		int dot = fileNameString.lastIndexOf('.');
		String extension = fileNameString.substring(dot + 1);

		if ("proto".equals(extension)) {
			throw new PluginRuntimeException("Use LoadProtobufBinary plugin for .proto files", true);
		}

		try (FileInputStream input = new FileInputStream(fileNameString)) {
			XStream xstream = new XStream(A0PipeLine_Manager.reflectionProvider);
			initializeOutputs();
			pluginOutputs.put("Default destination", (PluginIO) xstream.fromXML(input));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
