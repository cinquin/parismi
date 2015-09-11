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
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.ChannelInfo;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.SplitParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.PluginInfo;
import pipeline.plugins.ThreeDPlugin;

/**
 * Saves input to a TIFF file, grabbing metadata from the pipeline if is present, or else from the input (if possible).
 * If the input metadata indicates that the file is already stored somewhere on disk, either copies the file or moves it
 * depending on setting of the moveInsteadOfCopying parameter.
 *
 */
@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "SaveHyperstackToTIFFV2")
public class SaveImageToTIFF extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Obsolete; do not use";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "File name" };
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

	private AbstractParameter moveInsteadOfCopying =
			new BooleanParameter(
					"Move without copying",
					"Does not copy the file if it has already been stored on disk by another plugin; this could lead to errors if other plugins try to access the stored file subsequently",
					false, true, null);

	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory",
			"Directory to save measurements in", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);

	private AbstractParameter splitDirectoryAndFile = null;

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory });
		}
		AbstractParameter[] paramArray = { moveInsteadOfCopying, splitDirectoryAndFile };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		moveInsteadOfCopying = param[0];
		Object[] splitParameters = (Object[]) param[1].getValue();
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];
	}

	@Override
	public String operationName() {
		return "SaveImageToTIFF";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT + ONLY_FLOAT_INPUT;
	}

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, ProgressReporter p,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		String detectionSuffix = "";

		if ((input.getImageAcquisitionMetadata() instanceof ChannelInfo)) {
			ChannelInfo info = (ChannelInfo) input.getImageAcquisitionMetadata();
			int index = 0;
			List<Integer> wv = info.getDetectionRanges();
			while (index < wv.size()) {
				detectionSuffix += "_" + wv.get(index) + "_nm";
				index += 2;
			}
		}

		IPluginIO prefix = pluginInputs.get("File name");
		String fileNamePrefix = prefix != null ? ((PluginIOString) prefix).getString() : "";

		// FIXME If there is more than 1 channel the last one will overwrite the other ones, because they all have the
		// same file name

		@SuppressWarnings("null")
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + Utils.fileNameSeparator
						+ fileNamePrefix + FileNameUtils.removeIncrementationMarks((String) fileName.getValue())
						+ detectionSuffix + ".tif");

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

		try {
			input.asFile(new File(fileNameString), true);
		} catch (IOException e1) {
			throw new RuntimeException("IO error while saving channel to TIFF", e1);
		}
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.CUSTOM, true, false));
		return result;
	}

}
