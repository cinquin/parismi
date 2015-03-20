/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ncsa.util.ReaderTokenizer;
import pipeline.A0PipeLine_Manager;
import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.external_plugin_interfaces.RemoteMachine;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;

/**
 * Detects cell centers (for example to use as seeds for running active contours), using an input image and training
 * data.
 * The output is a list of points, which are assigned a measure of confidence. This is just a wrapper that calls an
 * executable
 * provided by Sam Hallman and Charless Fowlkes.
 */

public class SVMCellDetector extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Detect cell centers (for example to use as seeds for running active contours), "
				+ "using an input image and training data";
	}

	@ParameterType(printValueAsString = false)
	@ParameterInfo(userDisplayName = "Load directory", directoryOnly = true, changeTriggersUpdate = false,
			changeTriggersLiveUpdates = false)
	private File workingDirectory;

	@ParameterInfo(userDisplayName = "Training data", changeTriggersUpdate = false, aliases = { "File to load" },
			changeTriggersLiveUpdates = false)
	private String fileName;

	@ParameterInfo(userDisplayName = "Smallest scale",
			description = "Smallest factor by which to rescale the image before detection", floatValue = 0.7f,
			noErrorIfMissingOnReload = true)
	private float scale0;

	@ParameterInfo(userDisplayName = "Largest scale",
			description = "Largest factor by which to rescale the image before detection", floatValue = 1.5f,
			noErrorIfMissingOnReload = true)
	private float scale1;

	@ParameterInfo(userDisplayName = "Number of scales", description = "Decrease for faster runtime", floatValue = 14,
			noErrorIfMissingOnReload = true)
	private int nScales;

	@Override
	public String[] getInputLabels() {
		return new String[] {};
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Cells" };
	}

	@Override
	public String operationName() {
		return "SVM cell detector";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF;
	}

	transient private Process process = null;

	public static void readTextFileIntoCells(File file, IPluginIOStack inputImage, PluginIOCells cells) {
		cells.clear();
		cells.addQuantifiedPropertyName("Confidence");

		final int nColumns = 6;
		final int xColumn = 0;
		final int yColumn = 1;
		final int widthColumn = 2;
		final int heightColumn = 3;
		final int zColumn = 4;
		final int confidenceColumn = 5;
		// From http://www.java2s.com/Code/Java/File-Input-Output/ReadingNumbersfromaTextFile.htm
		// XXX Code duplicated in RegisterVideoLabelTimes plugin. Create a parsing class.

		try (Reader r = new BufferedReader(new FileReader(file.getAbsoluteFile()))) {

			ReaderTokenizer stok = new ReaderTokenizer(r);
			stok.parseNumbers();
			int currentColumn = 0;
			boolean allocateNewPoint = true;
			// Skip first 4 tokens (column headers);
			/*
			 * for (int i=0;i<4;i++){
			 * stok.nextToken();
			 * }
			 */
			stok.nextToken();

			ClickedPoint point = null;

			int seedId = 0;
			while (stok.ttype != ReaderTokenizer.TT_EOF) {
				if (allocateNewPoint) {
					point = new ClickedPoint();
					point.setSeedId(seedId);
					seedId++;
					point.listNamesOfQuantifiedProperties = cells.getQuantifiedPropertyNames();
					if (cells.getUserCellDescriptions().size() >= point.userCellDescriptions.size())
						point.userCellDescriptions = cells.getUserCellDescriptions();
					else
						cells.setUserCellDescriptions(point.userCellDescriptions);
					if (inputImage != null && inputImage.getCalibration() != null) {
						// Assume same calibration for x and y
						point.xyCalibration = (float) inputImage.getCalibration().pixelWidth;
						point.zCalibration = (float) inputImage.getCalibration().pixelDepth;
					}
					for (int i = 0; i < cells.getQuantifiedPropertyNames().size(); i++) {
						point.quantifiedProperties.add(0f);
					}
					allocateNewPoint = false;
				}

				if (stok.ttype == ReaderTokenizer.TT_NUMBER) {
					float f = (float) stok.nval;
					switch (currentColumn) {
						case xColumn:
							point.setx(f);
							if (f < 0) {
								Utils.log("Read negative x coordinate", LogLevel.ERROR);
							}
							break;
						case yColumn:
							point.sety(f);
							if (f < 0) {
								Utils.log("Read negative y coordinate", LogLevel.ERROR);
							}
							break;
						case zColumn:
							point.setz(f);
							if (f < 0) {
								Utils.log("Read negative z coordinate", LogLevel.ERROR);
							}
							break;
						case confidenceColumn:
							point.setConfidence(f);
							break;
						case widthColumn:
							point.hsz = f;
							break;
						case heightColumn:
							// Ignore; it's the same as width at this point
							break;
						default:
							throw new IllegalStateException("Unhandled column " + currentColumn);
					}
				} else
					throw new PluginRuntimeException("Unexpected read from detected cell file " + stok.sval
							+ " of type " + stok.ttype, true);
				stok.nextToken();
				currentColumn++;
				if (currentColumn == nColumns) {
					currentColumn = 0;
					assert (point != null);
					point.setx(point.x + Math.max(0, (point.hsz - 1) / 2));
					point.sety(point.y + Math.max(0, (point.hsz - 1) / 2));
					cells.add(point);
					allocateNewPoint = true;
				}
			}
		} catch (FileNotFoundException e) {
			throw new PluginRuntimeException("Could not find file " + file.getAbsolutePath()
					+ " from which to read cells", e, true);
		} catch (IOException e) {
			throw new PluginRuntimeException("Could not access file " + file.getAbsolutePath() + " to read cells", e,
					true);
		}
	}

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, ProgressReporter p,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		RemoteMachine machine = pipelineCallback.getRemoteMachine();

		boolean local =
				(machine == null) || (machine.getMachineName() == null) || ("".equals(machine.getMachineName()));

		String tempDirectory = System.getProperty("user.home") + "/parismiTempFiles/";
		File tempDirectoryFile = new File(tempDirectory);
		if (!local)
			try {
				if (!Files.createDirectories(Paths.get(tempDirectory)).toFile().setWritable(true, false))
					throw new RuntimeException("Permissions problem");
			} catch (IOException e2) {
				throw new PluginRuntimeException("Could not create or set permissions on ~/parismiTempFiles/", e2, true);
			}

		File inputImageFile = null, textListFile = null;
		try {

			try {
				inputImageFile = local ? null : File.createTempFile("inputImage", ".tiff", tempDirectoryFile);
				inputImageFile = input.asFile(inputImageFile, false);
				inputImageFile.setReadable(true, false);
			} catch (IOException e) {
				throw new PluginRuntimeException("Could not create temporary file from input", e, true);
			}

			// String filePrefix=local?"":machine.getGuestPrefix();

			List<String> remoteExecutionArgs = new ArrayList<>(15);
			List<String> nameAndArgs = new ArrayList<>(15);

			String trainingDataFileName = FileNameUtils.removeIncrementationMarks(fileName);
			String trainingDataFullPath =
					FileNameUtils.expandPath(FileNameUtils.removeIncrementationMarks(workingDirectory.getAbsolutePath()
							+ "/" + trainingDataFileName));

			try {
				if (local) {
					textListFile = File.createTempFile("list_of_cells", ".txt");
				} else {
					textListFile = File.createTempFile("list_of_cells", ".txt", tempDirectoryFile);
					if (!textListFile.setWritable(true, false))
						throw new RuntimeException("Permissions problem");
					if (!textListFile.setReadable(true, false))
						throw new RuntimeException("Permissions problem");
				}
			} catch (IOException e) {
				throw new PluginRuntimeException(
						"Could not create and set permissions on temporary file for list of cells", e, true);
			}

			if (!local) {
				remoteExecutionArgs.add("ssh");
				remoteExecutionArgs.add("-n");
				remoteExecutionArgs.add("-o");
				remoteExecutionArgs.add("UserKnownHostsFile=/dev/null");
				remoteExecutionArgs.add("-o");
				remoteExecutionArgs.add("StrictHostKeyChecking=no");
				remoteExecutionArgs.add("-i");
				remoteExecutionArgs.add("/virtual_machine_key_rsa");
				remoteExecutionArgs.add(machine.getUserName() + "@" + machine.getMachineName());
				nameAndArgs.addAll(remoteExecutionArgs);
				nameAndArgs.add(A0PipeLine_Manager.getBaseDir() + "/matlab_interface/matlab_wrapper");
				nameAndArgs.add(A0PipeLine_Manager.getBaseDir() + "/matlab_interface/run_detect.sh");
				nameAndArgs.add("/Applications/MATLAB/MATLAB_Compiler_Runtime/v81/");
			} else {
				nameAndArgs.add(A0PipeLine_Manager.getBaseDir() + "/matlab_interface/matlab_wrapper");
				nameAndArgs.add(A0PipeLine_Manager.getBaseDir() + "/matlab_interface/run_detect.sh");
				nameAndArgs.add("/Applications/MATLAB_R2009b.app/");
			}

			nameAndArgs.add("-s");
			nameAndArgs.add("" + scale0);
			nameAndArgs.add("" + scale1);
			nameAndArgs.add("" + nScales);
			nameAndArgs.add(trainingDataFullPath);
			nameAndArgs.add(inputImageFile.getAbsolutePath());
			nameAndArgs.add(textListFile.getAbsolutePath());

			List<String> killArgs = new ArrayList<>(3);
			killArgs.add("pkill");
			killArgs.add("-9");
			killArgs.add("-f");
			killArgs.add(textListFile.getAbsolutePath());
			ProcessBuilder killProcess = new ProcessBuilder(killArgs);
			ProcessBuilder remoteKillProcess = null;
			if (!remoteExecutionArgs.isEmpty()) {
				List<String> remoteKillArgs = new ArrayList<String>(remoteExecutionArgs);
				remoteKillArgs.addAll(killArgs);
				remoteKillProcess = new ProcessBuilder(remoteKillArgs);
			}

			Utils.log("Calling external process with arguments " + Utils.printStringArray(nameAndArgs), LogLevel.DEBUG);

			ProcessBuilder pb = new ProcessBuilder(nameAndArgs);
			pb.redirectErrorStream(true);
			boolean interrupted = false;
			try {
				process = pb.start();
				process.waitFor();
			} catch (IOException e) {
				throw new PluginRuntimeException("Could not run external process to detect cells", e, true);
			} catch (InterruptedException e) {
				interrupted = true;
				try {
					if (remoteKillProcess != null) {
						remoteKillProcess.start().waitFor();
					}
					process.destroy();// Probably redundant
					killProcess.start();
				} catch (IOException e1) {
					Utils.log("Could not kill external process", LogLevel.WARNING);
				}
			}

			// Scanner scanner=new Scanner(process.getInputStream());
			int logLevel = process.exitValue() == 0 && !interrupted ? LogLevel.DEBUG : LogLevel.ERROR;
			Utils.log("Executable output: ", logLevel);
			BufferedReader br = new BufferedReader(new InputStreamReader((process.getInputStream())));
			String line = null;
			try {
				while ((line = br.readLine()) != null)
					Utils.log(line, logLevel);
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
			// Utils.log("Executable output: "+scanner.useDelimiter("\\A").next(),LogLevel.DEBUG);
			// scanner.close();

			if (interrupted)
				throw new InterruptedException();

			if (process.exitValue() != 0) {
				throw new RuntimeException("External Matlab program failed with exit value " + process.exitValue());
			}

			// Parse the output file to create a bunch of points
			PluginIOCells cells = (PluginIOCells) pluginOutputs.get("Detected cells");
			readTextFileIntoCells(textListFile, input, cells);

			cells.setCalibration(input.getCalibration());
			cells.setWidth(input.getWidth());
			cells.setHeight(input.getHeight());
			cells.setDepth(input.getDepth());

		} finally {
			if (inputImageFile != null) {
				try {
					Files.deleteIfExists(Paths.get(inputImageFile.getAbsolutePath()));
				} catch (IOException e) {
					throw new PluginRuntimeException(e, true);
				}
			}
			if (textListFile != null) {
				try {
					Files.deleteIfExists(Paths.get(textListFile.getAbsolutePath()));
				} catch (IOException e) {
					throw new PluginRuntimeException(e, true);
				}
			}

		}

	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.SHORT_TYPE, PixelType.BYTE_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
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
		pluginOutputs.put("Detected cells", seeds);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Detected cells";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Detected cells", desc0);

		return result;
	}

}
