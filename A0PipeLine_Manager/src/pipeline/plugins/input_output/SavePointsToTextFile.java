/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;

import pipeline.PreviewType;
import pipeline.GUI_utils.JXTablePerColumnFiltering;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
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
 * Export PluginIOCells to tab-delimited text file.
 *
 */
public class SavePointsToTextFile extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Export cells to tab-delimited text file";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "File name", "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory",
			"Directory to save measurements in", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);

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
		return "SavePointToTextFileQQQ";
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

		IPluginIO prefix = pluginInputs.get("File name");
		String fileNamePrefix = prefix != null ? ((PluginIOString) prefix).getString() : "";

		@SuppressWarnings("null")
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + Utils.fileNameSeparator
						+ fileNamePrefix + FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));

		File directory = new File(fileNameString).getParentFile();
		if (!(directory.exists() && directory.isDirectory())) {
			if (directory.isFile()) {
				Utils.displayMessage("Cannot save to " + directory + " because it is a file", true, LogLevel.ERROR);
				throw new RuntimeException();
			}
			if (!directory.mkdirs()) {
				Utils.displayMessage("Directory " + directory + " does not exist and could not be created", true,
						LogLevel.ERROR);
				throw new RuntimeException();
			}
		}

		if (!fileNameString.contains(".tsv") && !fileNameString.contains(".txt"))
			fileNameString += ".tsv";

		PluginIOCells cells = (PluginIOCells) pluginInputs.get("Seeds");
		if (cells == null) {
			try {
				IPluginIOImage imageSource = getImageInput();
				cells = imageSource.getImp().getCellsToOverlay();
			} catch (Exception e) {
				Utils.printStack(e);
			}
		}
		if (cells == null) {
			for (Object o : pluginInputs.values()) {
				if (o instanceof PluginIOCells) {
					cells = (PluginIOCells) o;
					break;
				}
			}
		}
		if (cells == null) {
			throw new RuntimeException("Could not find any points to save");
		}
		BeanTableModel<ClickedPoint> tableModel = new BeanTableModel<>(ClickedPoint.class, cells.getPoints());
		JXTablePerColumnFiltering table = new JXTablePerColumnFiltering(tableModel);
		table.saveFilteredRowsToFile(null, true, fileNameString);

	}

}
