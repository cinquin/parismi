/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.SplitParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;

/**
 * Deserializes a PluginIOCells from a protobuf representation in a file.
 *
 */
public class LoadProtobufBinary extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Restore cells stored to binary file";
	}

	private AbstractParameter workingDirectory = new DirectoryParameter("Load directory",
			"Directory to read file from", "~/", true, null);
	private AbstractParameter fileName =
			new FileNameParameter("File to load", "File to load", "file.proto", true, null);

	private AbstractParameter splitDirectoryAndFile = null;

	@Override
	public AbstractParameter[] getParameters() {
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile = new SplitParameter(new Object[] { fileName, workingDirectory });
		}
		AbstractParameter[] paramArray = { splitDirectoryAndFile, null };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		Object[] splitParameters = (Object[]) param[0].getValue();
		fileName = (AbstractParameter) splitParameters[0];
		workingDirectory = (AbstractParameter) splitParameters[1];
	}

	@Override
	public String operationName() {
		return "LoadProtobufBinary";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return 0;
	}

	@SuppressWarnings("null")
	public static byte[] readProtobufFile(File f) {
		byte[] bytes = null;
		try (FileInputStream fileInput = new FileInputStream(f)) {
			long length = f.length();
			if (length > Integer.MAX_VALUE)
				throw new IllegalArgumentException("Protobuf file too large");
			bytes = new byte[(int) length];
			if (fileInput.read(bytes) < length) {
				throw new IOException("Did not read full " + length + " bytes");
			}
			Utils.log("Read " + length + " from protobuf file", LogLevel.VERBOSE_DEBUG);
		} catch (IOException e) {
			throw new PluginRuntimeException("Could not open file " + FileNameUtils.compactPath(f.getAbsolutePath()),
					e, true);
		}
		return bytes;
	}

	@Override
	public void runChannel(IPluginIOStack input, IPluginIOStack output, ProgressReporter r, PreviewType previewType,
			boolean inputHasChanged) {

		IPluginIO prefix = pluginInputs.get("File name");
		String fileNamePrefix = prefix != null ? ((PluginIOString) prefix).getString() : "";

		String fileNameString =
				FileNameUtils.removeIncrementationMarks(workingDirectory.getValue() + "/"
						+ FileNameUtils.removeIncrementationMarks(fileNamePrefix + fileName.getValue()));
		@SuppressWarnings("null")
		File directory =
				new File(FileNameUtils.expandPath(FileNameUtils.removeIncrementationMarks((String) workingDirectory
						.getValue())));
		if (!(directory.exists() && directory.isDirectory())) {
			throw new PluginRuntimeException("Directory " + FileNameUtils.compactPath(directory)
					+ " does not exist or is not a directory", true);
		}

		byte[] bytes = readProtobufFile(new File(fileNameString));
		PluginIOCells seeds = (PluginIOCells) pluginOutputs.get("Seeds");
		seeds.setProperty("Protobuf", bytes);

		/**
		 * If plugin has an image input, copy calibration information into PluginIOCells. This is used
		 * to add back scaling information that was erroneously discarded in older versions of the program.
		 */
		IPluginIO source = getInput();
		if ((source != null) && (source instanceof IPluginIOImage)) {
			IPluginIOImage image = (IPluginIOImage) source;
			seeds.parseOrReallocate();
			seeds.setCalibration(image.getCalibration());
			seeds.setWidth(input.getWidth());
			seeds.setHeight(input.getHeight());
			seeds.setDepth(input.getDepth());
		}

		seeds.fireValueChanged(false, true);
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "File name" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds" };
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
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
		Utils.log("Creating seeds", LogLevel.DEBUG);
		initializeOutputs();
		PluginIOCells seeds = new PluginIOCells();
		ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
		view.setData(seeds);
		pluginOutputs.put("Seeds", seeds);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

}
