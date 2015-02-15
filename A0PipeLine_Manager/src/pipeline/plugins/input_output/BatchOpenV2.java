/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

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
import pipeline.data.IPluginIOImage;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.IntrospectionParameters.DropHandler;
import pipeline.misc_util.IntrospectionParameters.DropHandlerType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.ImageOpenFailed;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.ResettablePlugin;

/**
 * Allows the user to choose a directory, and iterates through the files in that directory every time {@ref reset} is
 * called on that plugin.
 *
 */
public class BatchOpenV2 extends FourDPlugin implements AuxiliaryInputOutputPlugin, ResettablePlugin {

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

	@ParameterType(printValueAsString = true)
	@DropHandler(type = DropHandlerType.KEEP_DIRECTORY)
	@ParameterInfo(userDisplayName = "Directory", directoryOnly = true, changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false)
	private String directory;

	@DropHandler(type = DropHandlerType.KEEP_EXTENSION)
	@ParameterInfo(userDisplayName = "File name filter", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false, compactDisplay = true)
	private String filter;

	@ParameterInfo(userDisplayName = "Filter is regexp", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false)
	private boolean filterIsRegexp;

	@ParameterInfo(userDisplayName = "Recursive search", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false)
	private boolean recursiveSearch;

	@ParameterInfo(userDisplayName = "Found files", changeTriggersLiveUpdates = false, changeTriggersUpdate = false,
			noErrorIfMissingOnReload = true)
	@ParameterType(parameterType = "MultiList")
	protected int[] selectedFiles;

	@Override
	public void setParameters(AbstractParameter[] param) {
		for (String paramName : parametersListenedTo)
			if (getParameter(paramName) != null) {
				getParameter(paramName).removeListener(listener);
			}
		super.setParameters(param);
		for (String paramName : parametersListenedTo)
			getParameter(paramName).addPluginListener(listener);

		updateFileList();
		if ((files != null) && (files.size() > 0))
			pipelineCallback.setInputPath(ourRow, files.get(fileIndex));
	}

	private String[] parametersListenedTo = new String[] { "directory", "filter", "filterIsRegexp", "recursiveSearch" };

	private ParameterListener listener = new ParameterListenerAdapter() {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			try {
				updateFileList();
			} catch (PluginRuntimeException e) {
				// Don't want to print errors when directory name is in process of being typed
				// and therefore does not exist
				if (!stillChanging)
					Utils.printStack(e);
			}
		}

		@Override
		public boolean alwaysNotify() {
			return true;
		}
	};

	{
		for (String paramName : parametersListenedTo)
			getParameter(paramName).addPluginListener(listener);

	}

	private void updateFileList() {
		fileIndex = 0;
		files = new ArrayList<>();
		String directoryName = FileNameUtils.expandPath(FileNameUtils.removeIncrementationMarks(directory));
		if ((directoryName == null) || (directoryName.equals("")))
			return;
		File directory = new File(directoryName);
		if (!directory.exists()) {
			throw new PluginRuntimeException("Directory " + FileNameUtils.compactPath(directoryName)
					+ " does not exist or is unreadable", true);
		}
		if (!directory.isDirectory()) {
			throw new PluginRuntimeException(FileNameUtils.compactPath(directoryName) + " is not a directory", true);
		}
		if (filter == null)
			filter = "";
		this.currentFilter = new String(filter);
		List<File> filesToWalkThrough = new LinkedList<>(Arrays.asList(directory.listFiles()));
		while (filesToWalkThrough.size() > 0) {
			File file = filesToWalkThrough.remove(0);

			// ignore files whose name starts with .
			if (file.getName().indexOf(".") == 0)
				continue;

			if (file.isDirectory() && file.listFiles() != null) {
				if (recursiveSearch) {
					filesToWalkThrough.addAll(Arrays.asList(file.listFiles()));
				}
				continue;
			}

			String name = file.getName();
			if (name.equals("")) {
				continue;
			}
			if (filterIsRegexp) {
				if (name.matches(filter))
					files.add(file.getAbsolutePath());
			} else {
				if (name.contains(filter))
					files.add(file.getAbsolutePath());
			}
		}
		Collections.sort(files, (o1, o2) -> o1.compareTo(o2));

		String[] compactedFileNames = new String[files.size()];
		int i = 0;
		for (String s : files) {
			compactedFileNames[i] = FileNameUtils.compactPath(s);
			i++;
		}
		((MultiListParameter) getParameter("selectedFiles")).setChoices(compactedFileNames);
		getParameter("selectedFiles").setEditable(false);
		getParameter("selectedFiles").fireValueChanged(false, true, false);

		if (!files.isEmpty())
			pipelineCallback.setInputPath(ourRow, files.get(fileIndex));
		else
			pipelineCallback.setInputPath(ourRow, null);
	}

	@Override
	public String operationName() {
		return "BatchOpenV2";
	}

	@Override
	public String version() {
		return "2.0";
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
	private transient ArrayList<String> files = null;

	public void prepareForBatchRun() {
		updateFileList();
		pipelineCallback.setInputPath(ourRow, files.get(fileIndex));
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		if ((files == null) || files.size() == 0) {
			// read the list from the directory
			updateFileList();
		}

		if (files.size() > 0) {
			r.setIndeterminate(false);
			r.setValue((fileIndex * 100) / files.size());
		}

		IPluginIOImage loadedImage = getImageInput();
		String path = files.get(fileIndex);

		if (loadedImage != null)
			pluginOutputs.put("Loaded image", loadedImage);
		else if (path.endsWith(".proto")) {
			pluginOutputs.put("Seeds", PluginIOCells.readFromFile(new File(path)));
		} else
			throw new PluginRuntimeException("Unrecognized file type for " + path, null, true);

		String dirPath = new File(FileNameUtils.expandPath(FileNameUtils.removeIncrementationMarks(directory))).getAbsolutePath();
		Utils.log("directory: " + directory, LogLevel.VERBOSE_DEBUG);
                Utils.log("dirPath: " + dirPath, LogLevel.VERBOSE_DEBUG);
                Utils.log("path: " + path, LogLevel.VERBOSE_DEBUG);
		path = path.substring(dirPath.length() + 1);
                Utils.log("path: " + path, LogLevel.VERBOSE_DEBUG);

		while (path.startsWith("/"))
			path = path.substring(1);
                Utils.log("path: " + path, LogLevel.VERBOSE_DEBUG);
		int periodPosition = path.lastIndexOf('.');
		if (periodPosition > -1) {
			path = path.substring(0, periodPosition);
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
		if (files == null)
			return;// Plugin was never run; nothing to reset
		String filterFromParameter = filter;
		if (filterFromParameter == null)
			filterFromParameter = "";
		if (currentFilter != null && !currentFilter.equals(filterFromParameter)) {
			updateFileList();
			fileIndex = -1;
		}
		fileIndex++;
		if (fileIndex >= files.size()) {
			pipelineCallback.setInputPath(ourRow, null);
			throw new Utils.ImageOpenFailed("Reached end of file list at " + fileIndex);
		} else
			pipelineCallback.setInputPath(ourRow, files.get(fileIndex));
		((MultiListParameter) getParameter("selectedFiles")).setSelection(new int[] { fileIndex });
		((MultiListParameter) getParameter("selectedFiles")).fireValueChanged(false, true, false);
	}
}
