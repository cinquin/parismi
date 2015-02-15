/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import ij.IJ;
import ij.io.FileSaver;

import java.io.File;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.FourDPlugin;

/**
 * Save using ImageJ (does NOT create a BigTIFF; output easier to handle for some programs)
 *
 */
public class SaveUsingImageJ extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Save using ImageJ (does NOT create a BigTIFF; output easier to handle for some programs)";
	}

	@Override
	public String operationName() {
		return "SaveUsingImageJ";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	@ParameterInfo(userDisplayName = "Save 32-bit TIFF to", changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false)
	private File output;

	@Override
	public void run(ProgressReporter pr, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		for (IPluginIO source : getInputs().values()) {
			if (source instanceof IPluginIOImage) {
				IPluginIOImage image = (IPluginIOImage) source;
				if (image.getImp() != null) {
					// IJ.run(image.getImp().imp, "8-bit", "number=256");
					IJ.run(image.getImp().imp, "RGB Color", "");
					FileSaver fileSaver = new FileSaver(image.getImp().imp);
					if (image.getDimensions().depth > 1)
						fileSaver.saveAsTiffStack(output.getAbsolutePath());
					else
						fileSaver.saveAsTiff(output.getAbsolutePath());
					Utils.log("Saved to " + output.getAbsolutePath(), LogLevel.DEBUG);
				} else {
					throw new PluginRuntimeException("Source " + source + " has no associated view; cannot save", null,
							true);
				}
			} else {
				throw new PluginRuntimeException("Attempting to save source " + source + ", but it is not an image",
						null, true);
			}
		}
	}

}
