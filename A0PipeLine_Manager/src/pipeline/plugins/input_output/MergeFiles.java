/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.data.IPluginIO;
import pipeline.data.ImageAccessor;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.tiff_read_write.TIFFFileAccessor;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.FormatException;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

public abstract class MergeFiles extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@ParameterInfo(userDisplayName = "Path with * wildcard", changeTriggersLiveUpdates = false,
			changeTriggersUpdate = false)
	private String path;

	@ParameterInfo(userDisplayName = "Found files", changeTriggersLiveUpdates = false, changeTriggersUpdate = false)
	@ParameterType(parameterType = "MultiList")
	protected int[] selectedFiles;

	protected MergeFiles() {
		super();
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Aux 1", "Aux 2", "Aux 3", "Aux 4", "Aux 5" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "File name" };
	}

	@Override
	public int getFlags() {
		return DONT_ALLOCATE_OUTPUT_PIXELS;
	}

	protected void readInputFileHook(IPluginIO input) {
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		if (getParameter("path") != null) {
			getParameter("path").removeListener(pathListener);
		}
		super.setParameters(param);
		getParameter("path").addPluginListener(pathListener);
		getParameter("path").fireValueChanged(false, true, true);
	}

	private ParameterListener pathListener = new ParameterListenerAdapter() {
		@SuppressWarnings("null")
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			Objects.requireNonNull(path);
			List<@NonNull String> paths = FileNameUtils.getPathExpansions(path, false);
			((MultiListParameter) getParameter("selectedFiles")).setChoices(paths.toArray(new String[] {}));
			((MultiListParameter) getParameter("selectedFiles")).fireValueChanged(false, true, false);
			try {
				if (path.contains(".proto") && paths.size() > 0) // at this point nothing useful to pre-read from TIFFs
					readInputFileHook(openInputFile(FileNameUtils.expandPath(paths.get(0))));
			} catch (IOException | FormatException e) {
				Utils.printStack(e);
			}
		}

		@Override
		public boolean alwaysNotify() {
			return true;
		}
	};

	{
		getParameter("path").addPluginListener(pathListener);
	}

	@SuppressWarnings("null")
	static IPluginIO openInputFile(@NonNull String path) throws IOException, FormatException {
		if (path.endsWith(".tif") || path.endsWith(".tiff")) {
			TIFFFileAccessor tiffReader;
			File file = new File(path);
			tiffReader = new TIFFFileAccessor(file, file.getName());
			tiffReader.setDefaultCachePolicy(ImageAccessor.DONT_CACHE_PIXELS);
			if (file.length() > 3000000000L) {
				Utils.log("File " + file.getAbsolutePath() + " is over ~3GB; opening as a virtual stack.",
						LogLevel.INFO);
				tiffReader.setDefaultCachePolicy(ImageAccessor.DONT_CACHE_PIXELS);
			}
			tiffReader.openForSequentialRead();
			tiffReader.setOriginalSourceFile(file);
			return tiffReader;
		} else if (path.endsWith(".proto")) {
			byte[] bytes = LoadProtobufBinary.readProtobufFile(new File(path));

			PluginIOCells seeds = new PluginIOCells();
			seeds.setProperty("Protobuf", bytes);
			seeds.setDiskLocation(path.substring(path.lastIndexOf("/") + 1));
			// seeds.readFromFile=path.substring(path.lastIndexOf("/")+1);
			return seeds;
		} else {
			throw new PluginRuntimeException(new RuntimeException("Unrecognized file type for" + path), true);
		}

	}

	@SuppressWarnings("null")
	protected List<IPluginIO> openInputFiles(ProgressReporter prog) throws InterruptedException {
		List<IPluginIO> result = new ArrayList<>();
		final List<String> paths = FileNameUtils.getPathExpansions(path, false);
		((MultiListParameter) getParameter("selectedFiles")).setChoices(paths.toArray(new String[] {}));

		final @NonNull String @NonNull [] fileNames = 
				((MultiListParameter) getParameter("selectedFiles")).getSelectionString();

		/*
		 * prog.setMin(0);
		 * prog.setMax(fileNames.length-1);
		 */

		ParFor parFor = new ParFor("Open MergeFile inputs", 0, fileNames.length - 1, prog, true);

		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker((loopIndex, threadIndex) -> {
				IPluginIO pluginIO = null;
				@NonNull String path1 = "";
				try {
					path1 = fileNames[loopIndex];
					pluginIO = openInputFile(FileNameUtils.expandPath(path1));
					readInputFileHook(pluginIO);
				} catch (Exception e) {
					throw (new PluginRuntimeException("Could not open file file " + FileNameUtils.compactPath(path1),
							e, true));
				}

				return pluginIO;
			});

		result.addAll(parFor.run(true).stream().map(o -> (IPluginIO) o).collect(Collectors.toList()));

		Collections.sort(result, (o1, o2) -> o1.getDiskLocation().compareTo(o2.getDiskLocation()));

		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	protected static int[] checkSelection(int[] selection, int resultLength) {
		int[] result;
		if (selection.length > 0) {
			result = selection;
		} else {
			result = new int[resultLength];
			for (int i = 0; i < resultLength; i++) {
				result[i] = i;
			}
		}
		return result;
	}

}
