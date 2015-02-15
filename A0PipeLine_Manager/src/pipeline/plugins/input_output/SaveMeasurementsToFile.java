/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import pipeline.PreviewType;
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
import pipeline.parameters.TextParameter;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.PluginInfo;

@PluginInfo(displayToUser = false, obsolete = true)
public class SaveMeasurementsToFile extends FourDPlugin {

	@Override
	public String getToolTip() {
		return "Obsolete; do not use";
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			final PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + Utils.fileNameSeparator
						+ FileNameUtils.removeIncrementationMarks((String) fileName.getValue()));
		File directory = new File((String) workingDirectory.getValue());
		if (!(directory.exists() && directory.isDirectory())) {
			Utils.displayMessage("Directory " + fileNameString + " does not exist or is not a directory", true,
					LogLevel.ERROR);
			throw new RuntimeException();
		}

		try (FileWriter outFile = new FileWriter(fileNameString); PrintWriter out = new PrintWriter(outFile)) {
			out.print(userNotes.getValue());
		} catch (IOException e) {
			Utils.printStack(e);
		}

	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT + CUSTOM;
	}

	public final static int skeletonJunction = 2;
	public final static int skeletonEndPoint = 3;
	String[] filterChoices = { "", "None", "Skeleton junction", "Skeleton end point" };

	private AbstractParameter userNotes = new TextParameter("User measurements to store",
			"User measurements to save to text file", "", true, null, null);
	private AbstractParameter workingDirectory = new DirectoryParameter("Save directory",
			"Directory to save measurements in", "", true, null);
	private AbstractParameter fileName = new FileNameParameter("Save to file", "Save to file", "xxxxxxxxx", true, null);

	private AbstractParameter splitDirectoryAndFile = null;

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

	@Override
	public String operationName() {
		return "AnnotateAndSave";
	}

	@Override
	public String version() {
		return "1.0";
	}

}
