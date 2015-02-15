/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdom.Document;
import org.jdom.Element;

import pipeline.ParseImageMetadata;
import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.PluginInfo;

/**
 * Uses ImageJ built in functionality to save a TIFF from input image.
 * Adds user notes to a new "PipelineMetadata->UserNotes" node in XML metadata that is used to set image
 * metadata (any other properties are discarded for now).
 * 
 * It's probably best to use SaveImageToTIFF, which now supports multiple channels in a same TIFF
 * (but saves to BigTIFF format, which ImageJ does not read natively).
 *
 */
@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "SaveHyperstackToTIFFV2")
public class AnnotateAndSave extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Obsolete; don't use";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Aux 1", "Aux 2", "Aux 3", "Aux 4", "Aux 5", "File name" };
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		IPluginIO prefix = pluginInputs.get("File name");
		String fileNamePrefix = prefix != null ? ((PluginIOString) prefix).getString() : "";

		for (Entry<String, IPluginIO> io : pluginInputs.entrySet()) {

			if (!(io.getValue() instanceof IPluginIOImage))
				continue;
			ImagePlus imp =
					((IPluginIOImage) io.getValue()).getImp() != null ? ((IPluginIOImage) io.getValue()).getImp().imp
							: null;

			String sourceName = io.getKey();

			FileSaver saver = new FileSaver(imp);

			String fileNameString =
					FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + Utils.fileNameSeparator
							+ fileNamePrefix + FileNameUtils.removeIncrementationMarks((String) fileName.getValue())
							+ sourceName + ".tiff");
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

			// replace UserNotes node in the metadata

			Document doc = new Document();
			Element root = new Element("PipelineMetadata");
			doc.addContent(root);
			root.addContent(new Element("UserNotes").setText((String) userNotes.getValue()));

			ParseImageMetadata.setPipelineProcessingMetadata(imp, "UserNotes", root);

			String saveTitle = imp.getTitle();
			// for some reason stacks need to be saved differently
			if (imp.getStackSize() > 1)
				IJ.saveAs(imp, "tif", fileNameString);
			else
				saver.saveAsTiff(fileNameString);

			imp.setTitle(saveTitle);// because the save operation renames the imp
		}

	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT + CUSTOM + STUFF_ALL_INPUTS;
	}

	private class workingDirectoryListener implements ParameterListener {
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
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private class saveToFileListener implements ParameterListener {
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
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private class userNotesListener implements ParameterListener {
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
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private AbstractParameter userNotes = new TextParameter("User notes to store in file metadata",
			"User notes to add to image metadata (will replace any previous user notes)", "", true,
			new userNotesListener(), null);
	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory", "Directory to save image in",
			"", true, new workingDirectoryListener());
	private AbstractParameter fileName = new FileNameParameter("Save image to file", "Save to file", "xxxxxxxxx", true,
			new saveToFileListener());

	private AbstractParameter splitDirectoryAndFile = null;

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] {
				new userNotesListener(),
				new SplitParameterListener(new ParameterListener[] { new saveToFileListener(),
						new workingDirectoryListener() }) };
	}

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory });
		}
		AbstractParameter[] paramArray = { userNotes, splitDirectoryAndFile };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		userNotes = param[0];
		Object[] splitParameters = (Object[]) param[1].getValue();
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];

	}

	// VARIOUS NAMES TO UPDATE SO THE PLUGIN MANAGER KNOWS WHAT YOUR PLUGIN IS CALLED, ITS VERSION, AND NAMES FOR ITS
	// PARAMETERS
	@Override
	public String operationName() {
		return "AnnotateAndSave";
	}

	@Override
	public String version() {
		return "1.0";
	}

	// CREATE A DESTINATION IMAGE. IF YOUR PLUGIN OUTPUTS AN IMAGE WITH THE SAME NUMBER OF SLICES, CHANNELS,
	// PIXEL-DEPTH, ETC. AS THE ORIGINAL IMAGE YOU CAN KEEP
	// THIS AS SUCH (IT DUPLICATES THE SOURCE IMAGE)
	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {

		return null;
	}

}
