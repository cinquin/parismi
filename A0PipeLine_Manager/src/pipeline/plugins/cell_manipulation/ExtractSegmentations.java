/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOList;
import pipeline.data.PluginIOListOfQ;
import pipeline.data.PluginIOStack;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ILoopWorker;
import pipeline.misc_util.parfor.ParFor;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;

/**
 * Start from an image and its segmentation, and extract one new image for each element of the segmentation.
 * FIXME For now, this plugin should NOT be run on more than 1 channel at a time.
 */
public class ExtractSegmentations extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "From a set of cells and the matching image, extract an independent image for each cell";
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
		initializeOutputs();
		PluginIOList<IPluginIOImage> outputImages = new PluginIOListOfQ<>();
		pluginOutputs.put("outputImages", outputImages);
		ArrayList<PluginIOView> views = new ArrayList<>();
		return views;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "outputImages";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("outputImages", desc0);

		return result;
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF + ONLY_1_INPUT_CHANNEL;
	}

	@ParameterInfo(userDisplayName = "File name prefix", changeTriggersLiveUpdates = false,
			changeTriggersUpdate = false)
	private String fileNamePrefix;

	@Override
	public String operationName() {
		return "ExtractSegmentations";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "outputImages" };
	}

	private AtomicInteger noSegmentation = new AtomicInteger();

	@Override
	public void runChannel(final IPluginIOStack input, IPluginIOStack output, ProgressReporter r,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		final PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");
		@SuppressWarnings("unchecked")
		final PluginIOList<IPluginIOImage> outputImages =
				(PluginIOList<IPluginIOImage>) pluginOutputs.get("outputImages");
		outputImages.clear();

		input.computePixelArray();

		noSegmentation.set(0);

		ParFor parFor = new ParFor(0, inputCells.getPoints().size() - 1, r, threadPool, true);

		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker(new ILoopWorker() {

				int max(int[] a) {
					int result = -Integer.MAX_VALUE;
					for (int i : a) {
						if (i > result)
							result = i;
					}
					return result;
				}

				int min(int[] a) {
					int result = Integer.MAX_VALUE;
					for (int i : a) {
						if (i < result)
							result = i;
					}
					return result;
				}

				List<ClickedPoint> pointList = inputCells.getPoints();

				@Override
				public final Object run(int loopIndex, int threadIndex) throws InterruptedException {

					boolean noPixels = false;
					ClickedPoint p = pointList.get(loopIndex);

					int[] xCoord = p.imageFullSegCoordsX;
					int[] yCoord = p.imageFullSegCoordsY;
					int[] zCoord = p.imageFullSegCoordsZ;

					if (xCoord == null)
						throw new IllegalArgumentException("Missing segmentation");

					if (xCoord.length == 0) {
						noPixels = true;
					}

					int minX = min(xCoord);
					int minY = min(yCoord);
					int minZ = min(zCoord);

					PluginIOStack cellStack =
							new PluginIOStack(fileNamePrefix + ((int) p.getSeedId()) + ".tif", max(xCoord) - minX + 1,
									max(yCoord) - minY + 1, max(zCoord) - minZ + 1, 1, PixelType.FLOAT_TYPE);

					cellStack.setCalibration((Calibration) input.getCalibration().clone());

					for (int i = 0; i < xCoord.length; i++) {
						cellStack.setPixelValue(xCoord[i] - minX, yCoord[i] - minY, zCoord[i] - minZ, input.getFloat(
								xCoord[i], yCoord[i], zCoord[i]));
					}

					if (noPixels) {
						noSegmentation.incrementAndGet();
					}

					synchronized (outputImages) {
						outputImages.add(cellStack);
					}

					return null;
				}
			});

		parFor.run(true);

		if (noSegmentation.get() > 0) {
			Utils.displayMessage(noSegmentation.get() + " cells had an empty segmentation", true, LogLevel.WARNING);
		}

	}

}
