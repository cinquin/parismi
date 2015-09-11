/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOList;
import pipeline.data.InputOutputDescription;
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
import pipeline.misc_util.drag_and_drop.DropProcessorIgnore;
import pipeline.misc_util.drag_and_drop.DropProcessorKeepDirectory;
import pipeline.misc_util.drag_and_drop.DropProcessorKeepFileName;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

/**
 * Saves input to a multi-channel BigTIFF file that is openable by the pipeline or with the LOCI Bioformat importer
 * plugin.
 *
 */
public class SaveHyperstackToTIFFV2 extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Save image to multi-channel BigTIFF, which is openable by pipeline or with LOCI Bioformat importer "
				+ "(the latter loses scaling information)";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "File name", "outputImages" };
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

	@ParameterInfo(userDisplayName = "Move instead of copying", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false)
	boolean MoveInsteadOfCopying;

	@ParameterType(printValueAsString = true)
	@DropHandler(type = DropHandlerType.KEEP_DIRECTORY)
	@ParameterInfo(userDisplayName = "Directory", directoryOnly = true, changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false, fileNameIncrementable = true)
	private String directory;

	@DropHandler(type = DropHandlerType.KEEP_FILENAME)
	@ParameterInfo(userDisplayName = "File name", changeTriggersUpdate = false, changeTriggersLiveUpdates = false,
			fileNameIncrementable = true, compactDisplay = true)
	private String fileName;

	@ParameterInfo(userDisplayName = "Prefix", changeTriggersUpdate = false, changeTriggersLiveUpdates = false,
			compactDisplay = true)
	private String userPrefix;

	@Override
	public String operationName() {
		return "SaveHyperstackToTIFFV2";
	}

	@Override
	public String version() {
		return "2.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		@SuppressWarnings("unchecked")
		IPluginIOList<IPluginIOImage> imageList = (IPluginIOList<IPluginIOImage>) pluginInputs.get("outputImages");
		IPluginIO prefix = pluginInputs.get("File name");
		String fileNamePrefix = prefix != null ? ((PluginIOString) prefix).getString() : "";

		// If fileNamePrefix contains directories, preprend userPrefix to the file name,
		// not to the name of the first directory in the path.
		if (fileNamePrefix.contains("/")) {
			int lastIndex = fileNamePrefix.lastIndexOf("/");
			fileNamePrefix =
					fileNamePrefix.substring(0, lastIndex + 1) + userPrefix
							+ fileNamePrefix.substring(lastIndex + 1, fileNamePrefix.length());
		} else {
			fileNamePrefix = userPrefix + fileNamePrefix;
		}

		@SuppressWarnings("null")
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(directory + Utils.fileNameSeparator + fileNamePrefix
						+ FileNameUtils.removeIncrementationMarks(fileName));

		File directory = new File(fileNameString).getParentFile();
		if (!(directory.exists() && directory.isDirectory())) {
			if (directory.isFile()) {
				throw new PluginRuntimeException("Cannot save to " + directory + " because it is a file", true);
			}
			if (!directory.mkdirs()) {
				throw new PluginRuntimeException("Directory " + directory + " does not exist and could not be created",
						true);

			}
		}

		if (!fileNameString.endsWith(".tif") && !fileNameString.endsWith(".tiff"))
			fileNameString = fileNameString + ".tiff";

		try {
			if (imageList != null) {
				for (IPluginIOImage image : imageList) {
					image.asFile(new File(directory + "/" + image.getName()), true);
				}
			} else
				getImageInput().asFile(new File(fileNameString), true);
		} catch (IOException e1) {
			throw new PluginRuntimeException("IO error while saving channel to TIFF", e1, true);
		}

	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.CUSTOM, true, false));
		return result;
	}

	@Override
	//Only for backward compatibility
	public void setParameters(AbstractParameter[] params) {
		super.setParameters(params);
		((TextParameter) getParameter("fileName")).setDropProcessor(new DropProcessorKeepFileName());
		((TextParameter) getParameter("directory")).setDropProcessor(new DropProcessorKeepDirectory());
		((TextParameter) getParameter("userPrefix")).setDropProcessor(new DropProcessorIgnore());
	}
}
