/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.ImageOpenFailed;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.PluginInfo;
import pipeline.plugins.ResettablePlugin;

/**
 * Allows the user to choose a directory, and iterates through the files in that directory every time {@ref reset} is
 * called on that plugin.
 *
 */
@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "BatchOpenV2")
public class BatchOpen extends FourDPlugin implements AuxiliaryInputOutputPlugin, ResettablePlugin {

	@Override
	public String getToolTip() {
		return "Iterate over files that are contained in a given directory and optionally "
				+ "whose name contains a substring";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] {};
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "File name" };
	}

	private ParameterListener methodListener0 = new MethodListener();
	private ParameterListener methodListener = new ParameterListenerWeakRef(methodListener0);

	private AbstractParameter workingDirectory = new DirectoryParameter("Directory", "Directory to read files from",
			"", true, methodListener);
	private AbstractParameter fileNameFilter = new TextParameter("File name filter",
			"Only files containing this string will be considered", "", true, methodListener, null);

	private AbstractParameter splitDirectoryAndFile = null;

	private BooleanParameter recursiveParameter = new BooleanParameter("Recursive", "Explore folders recursively",
			false, true, methodListener);

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile = new SplitParameter(new Object[] { fileNameFilter, workingDirectory });
		}
		AbstractParameter[] paramArray = { splitDirectoryAndFile, recursiveParameter };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		Object[] splitParameters = (Object[]) param[0].getValue();
		fileNameFilter = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];
		recursiveParameter = (BooleanParameter) param[1];

		updateFileList();
		if ((files != null) && (files.size() > 0))
			pipelineCallback.setInputPath(ourRow, files.get(fileIndex).getAbsolutePath());
	}

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] {
				new SplitParameterListener(new ParameterListener[] { methodListener, methodListener }), methodListener };
	}

	private class MethodListener implements ParameterListener {
		private String parameterName;

		@Override
		public String getParameterName() {
			return parameterName;
		}

		@Override
		public void setParameterName(String name) {
			parameterName = name;
		}

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (!stillChanging)
				updateFileList();
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private void updateFileList() {
		fileIndex = 0;
		files = new ArrayList<>();
		String directoryName = FileNameUtils.removeIncrementationMarks((String) workingDirectory.getValue());
		if ((directoryName == null) || (directoryName.equals("")))
			return;
		File directory = new File(directoryName);
		if (!directory.exists()) {
			// Utils.displayError("Directory "+directoryName+" does not exist or is unreadable");
			throw new RuntimeException("Directory " + directoryName + " does not exist or is unreadable");
		}
		String filter = (String) fileNameFilter.getValue();
		if (filter == null)
			filter = "";
		this.currentFilter = new String(filter);
		List<File> filesToWalkThrough = new LinkedList<>(Arrays.asList(directory.listFiles()));
		while (filesToWalkThrough.size() > 0) {
			File file = filesToWalkThrough.remove(0);

			if (file.getName().indexOf(".") == 0)
				continue;

			if (file.isDirectory() && file.listFiles() != null) {
				if ((Boolean) ((Object[]) recursiveParameter.getValue())[0]) {
					filesToWalkThrough.addAll(Arrays.asList(file.listFiles()));
				}
				continue;
			}

			String name = file.getName();
			if (!(name.equals(".DS_Store")) && ((filter.equals("")) || (name.contains(filter)))) {
				files.add(file);
			}
		}
		Collections.sort(files, (o1, o2) -> o1.getAbsolutePath().compareTo(o2.getAbsolutePath()));
		if (!files.isEmpty())
			pipelineCallback.setInputPath(ourRow, files.get(fileIndex).getAbsolutePath());
		else
			pipelineCallback.setInputPath(ourRow, null);
	}

	@Override
	public String operationName() {
		return "BatchOpen";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return DONT_ALLOCATE_OUTPUT_PIXELS + PLUGIN_CREATES_OUTPUT_ITSELF;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	private transient int fileIndex = 0;
	private transient ArrayList<File> files = null;

	public void prepareForBatchRun() {
		updateFileList();
		pipelineCallback.setInputPath(ourRow, files.get(fileIndex).getAbsolutePath());
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		if ((files == null) || files.size() == 0) {
			// read the list from the directory
			updateFileList();
		}
		
		if (files == null) {
			throw new PluginRuntimeException("Empty file list", true);
		}

		pluginOutputs.put("Loaded image", getImageInput());
		String path = files.get(fileIndex).getPath();
		path = path.substring(FileNameUtils.removeIncrementationMarks((String) workingDirectory.getValue()).length());
		File f = new File(path);
		int periodPosition = f.getName().indexOf('.');
		if (periodPosition > -1) {
			path = f.getParentFile() + "/" + f.getName().substring(0, periodPosition);
		}
		pluginOutputs.put("File name", new PluginIOString(path));
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {

		ArrayList<PluginIOView> imagesToShow = new ArrayList<>();

		return imagesToShow;
	}

	/**
	 * Used to figure out whether file list should be recomputed because the currentFilter has changed
	 */
	private String currentFilter;

	@Override
	public void reset() throws ImageOpenFailed {
		String filterFromParameter = (String) fileNameFilter.getValue();
		if (filterFromParameter == null)
			filterFromParameter = "";
		if (!currentFilter.equals(filterFromParameter)) {
			updateFileList();
			fileIndex = -1;
		}
		fileIndex++;
		if (fileIndex >= files.size()) {
			pipelineCallback.setInputPath(ourRow, null);
			throw new Utils.ImageOpenFailed("Reached end of file list at " + fileIndex);
		} else
			pipelineCallback.setInputPath(ourRow, files.get(fileIndex).getAbsolutePath());
	}
}
