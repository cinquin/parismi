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
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.PluginInfo;

/**
 * Saves input to a multi-channel BigTIFF file that is openable by the pipeline or with the LOCI Bioformat importer
 * plugin.
 *
 */
@PluginInfo(displayToUser = false, obsolete = true, suggestedReplacement = "SaveHyperstackToTIFFV2")
public class SaveHyperstackToTIFF extends FourDPlugin implements AuxiliaryInputOutputPlugin {

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
		return "SaveHyperstackToTIFF";
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
			boolean stayInCoreLoop) throws InterruptedException {

		@SuppressWarnings("unchecked")
		IPluginIOList<IPluginIOImage> imageList = (IPluginIOList<IPluginIOImage>) pluginInputs.get("outputImages");
		IPluginIO prefix = pluginInputs.get("File name");
		String fileNamePrefix = prefix != null ? ((PluginIOString) prefix).getString() : "";

		@SuppressWarnings("null")
		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + Utils.fileNameSeparator
						+ fileNamePrefix + FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));

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
			if (imageList != null) {
				for (IPluginIOImage image : imageList) {
					image.asFile(new File(directory + "/" + image.getName()), true);
				}
			} else
				getImageInput().asFile(new File(fileNameString), true);
		} catch (IOException e1) {
			throw new RuntimeException("IO error while saving channel to TIFF", e1);
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

}
