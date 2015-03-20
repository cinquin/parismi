/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map.Entry;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage;
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

/**
 * Saves input as an XML object by serializing it. Don't use on images as it will be very inefficient.
 *
 */
public class SaveObjectsToXML extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Save plugin output as an XML file";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] {"File name"};
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory",
			"Directory to save XML object to", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);

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
		return "SaveObjectsToXML";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT + STUFF_ALL_INPUTS;
	}

	private static int nSavableInputs(Iterator<Entry<String, IPluginIO>> it) {
		int result = 0;
		while (it.hasNext()) {
			Entry<String, IPluginIO> entry = it.next();
			if ("File name".equals(entry.getKey()))
				continue;
			if (entry.getValue() instanceof PluginIOImage)
				continue;
			result++;
		}
		return result;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		IPluginIO prefix = pluginInputs.get("File name");
		String fileNamePrefix = prefix != null ? ((PluginIOString) prefix).getString() : "";

		Iterator<Entry<String, IPluginIO>> it = pluginInputs.entrySet().iterator();

		boolean oneSavedEntry = false;

		boolean needTypeName = nSavableInputs(pluginInputs.entrySet().iterator()) > 1;

		while (it.hasNext()) {
			Entry<String, IPluginIO> source = it.next();
			if (source.getKey().equals("File name"))
				continue;// Don't save file name to disk
			String fileNameString =
					FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + "/" + fileNamePrefix
							+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue()))
							+ (needTypeName ? source.getKey() : "");
			File directory = new File(FileNameUtils.removeIncrementationMarks((String) workingDirectory.getValue()));
			if (!(directory.exists() && directory.isDirectory())) {
				throw new PluginRuntimeException("Directory "
						+ FileNameUtils.compactPath(workingDirectory.getValue().toString())
						+ " does not exist or is not a directory", true);
			}

			if (source.getValue() == null) {
				throw new PluginRuntimeException("Trying to save null value", true);
			}
			if (source.getValue() instanceof PluginIOCells) {
				oneSavedEntry = true;
				// Save as binary, not as XML
				if (!fileNameString.contains(".proto"))
					fileNameString += ".proto";
				try (FileOutputStream fos = new FileOutputStream(fileNameString)) {
					fos.write(source.getValue().asProtobufBytes());
				} catch (IOException e) {
					throw new PluginRuntimeException("Could not write protobuf file", e, true);
				}
			} else if (source.getValue() instanceof PluginIOImage) {
				Utils.log("Not saving images as XML; use specialized plugin", LogLevel.INFO);
			} else
				try (PrintWriter out = new PrintWriter(fileNameString)) {
					oneSavedEntry = true;
					out.println(Utils.objectToXMLString(source.getValue()).toCharArray());
				} catch (FileNotFoundException e) {
					throw new PluginRuntimeException("Error writing to file "
							+ FileNameUtils.compactPath(fileNameString), e, true);
				}

		}

		if (!oneSavedEntry) {
			throw new PluginRuntimeException("No entries to be saved", true);
		}

	}

}
