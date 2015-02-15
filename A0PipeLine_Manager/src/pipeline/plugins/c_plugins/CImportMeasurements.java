/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import ij.Prefs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.tiff_read_write.TIFFFileAccessor;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TextParameter;
import pipeline.plugins.ExternalCallToLibrary;

/**
 *
 */
public class CImportMeasurements extends ExternalCallToLibrary {

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.BYTE_TYPE },
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@Override
	public int getFlags() {
		return 0;
	}

	@Override
	public String operationName() {
		return "C helper for parsing tab-delimited text file columns into protobuf structure";
	}

	@Override
	public String version() {
		return "1.0";
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory",
			"Directory to save measurements in", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);

	private AbstractParameter splitDirectoryAndFile;

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			throw new IllegalStateException();
		}
		AbstractParameter[] paramArray = { columnNames, splitDirectoryAndFile };
		return paramArray;
	}

	private SplitParameter columnNames;
	private TextParameter protobufColumn, textColumn;

	@Override
	public void setParameters(AbstractParameter[] param) {
		columnNames = (SplitParameter) param[0];
		param1 = columnNames;
		Object[] splitParameters = (Object[]) (columnNames).getValue();
		protobufColumn = (TextParameter) splitParameters[0];
		textColumn = (TextParameter) splitParameters[1];

		splitParameters = (Object[]) param[1].getValue();
		splitDirectoryAndFile = param[1];
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];
	}

	private void initializeParams() {
		protobufColumn = new TextParameter("Protobuf column", "", "", true, null, null);
		textColumn = new TextParameter("Text column", "", "", true, null, null);

		columnNames = new SplitParameter(new Object[] { protobufColumn, textColumn });
		param1 = columnNames;

		fileName = new FileNameParameter("Text file name", "Save to file", "xxxxxxxxx", true, null);
		workingDirectory =
				new DirectoryParameter("Text file directory", "Directory to save measurements in", "", true, null);
		splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory });
	}

	public CImportMeasurements() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "txt2proto";
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds", desc0);
		return result;
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	private File getFile() {
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + Prefs.separator
						+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));

		File directory = new File(fileNameString).getParentFile();
		if (!(directory.exists() && directory.isDirectory())) {
			if (directory.isFile()) {
				Utils.displayMessage("Cannot save to " + directory + " because it is a file", true, LogLevel.ERROR);
				return null;
			}
			if (!directory.mkdirs()) {
				Utils.displayMessage("Directory " + directory + " does not exist and could not be created", true,
						LogLevel.ERROR);
				return null;
			}
		}

		File result = new File(fileNameString);

		if (result.exists() && (result.length() > 5000000000L)) {
			Utils.displayMessage(
					"File "
							+ result.getAbsolutePath()
							+ " already exists and is over ~5GB. Not overwriting; please delete file or choose a different name",
					true, LogLevel.ERROR);
			return null;
		}

		return result;
	}

	@Override
	public void loadExtraOutputArgs(List<String> args, String firstOutputName) {
		args.add(getFile().getAbsolutePath() + ".metadata");
	}

	@Override
	public void postRunUpdates() {
		try {
			if (getOutput() instanceof TIFFFileAccessor)
				((TIFFFileAccessor) getOutput()).closeFileEarly();
		} catch (IOException e) {
			Utils.printStack(e);
		}
		super.postRunUpdates();
	}

	@Override
	public boolean createOutput(InputOutputDescription desc, List<PluginIOView> views) {
		if ("Seeds".equals(desc.name)) {
			Utils.log("Creating Seeds output for CActiveContours", LogLevel.DEBUG);
			initializeOutputs();
			PluginIOCells seeds = new PluginIOCells();
			ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
			view.setData(seeds);
			// view.show();
			pluginOutputs.put("Seeds", seeds);
			return true;
		}
		return false;
	}
}
