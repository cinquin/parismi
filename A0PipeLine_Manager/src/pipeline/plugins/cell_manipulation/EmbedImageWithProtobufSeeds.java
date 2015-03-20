/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;

/**
 * Start from an image and a list of seeds in that image, and embed small windows around each seed into the
 * PluginIOCells structure that lists the seeds. Assume that xz dimensions of all the windows are the same.
 * Output can be displayed with {@link DisplayEmbeddedImages} plugin.
 * FIXME There is currently no appropriately-named field in the current protobuf structure. Store an xzy array into
 * the field image_fullseg_coords_x even though it is used in a different way when the structure contains segmentation
 * rather than image data. This will need to be changed when the protobuf structure is cleaned up.
 */
public class EmbedImageWithProtobufSeeds extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Start from an image and a list of seeds in that image, and embed small windows around each seed into the "
				+ "PluginIOCells structure that lists the seeds. Assume that xz dimensions of all the windows are the same. "
				+ "Output can be displayed using the DisplayEmbeddedImages plugin.";
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
		pluginOutputs.put("Seeds", seeds);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
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
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF;
	}

	private IntParameter xRadiusParam = new IntParameter("x radius", "Pixel x radius of square centered on seed", 3, 1,
			50, true, true, null);
	private IntParameter yRadiusParam = new IntParameter("y radius", "Pixel y radius of square centered on seed", 3, 1,
			50, true, true, null);

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { xRadiusParam, yRadiusParam };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		xRadiusParam = (IntParameter) param[0];
		yRadiusParam = (IntParameter) param[1];
	}

	@Override
	public String operationName() {
		return "EmbedImageWithProtobufSeeds";
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
		return new String[] { "Seeds" };
	}

	private transient AtomicInteger sliceRegistry = new AtomicInteger();
	private volatile boolean abort = false;

	private static final int numberOfThreadsToUse = 4;
	private transient Runnable[] tasks = new Runnable[numberOfThreadsToUse];
	private transient Future<?>[] futures = new Future[numberOfThreadsToUse];

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter progress,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		final PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");
		final PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Seeds");
		outputCells.clear();
		inputCells.copyInto(outputCells);

		final int xRadius = xRadiusParam.getintValue();
		final int xDiameter = 2 * xRadius + 1;

		final int yRadius = yRadiusParam.getintValue();
		final int yDiameter = 2 * yRadius + 1;

		outputCells.setWidth(xDiameter);
		outputCells.setHeight(yDiameter);
		outputCells.setDepth(-1);// Depth does not make sense at this level, because it will probably change between
		// cells as the depth is really the range of times (time is stuffed into the z dimension)

		final int width = input.getWidth();
		final int height = input.getHeight();
		final int depth = input.getDepth();

		input.computePixelArray();

		// FIXME Change to BallIterator, from which following code was copied

		sliceRegistry.set(0);

		abort = false;
		final List<ClickedPoint> listCells = inputCells.getPoints();
		final int nCells = listCells.size();
		final float progressMultiplyingFactor = 100.0f / nCells;

		for (int i = 0; i < numberOfThreadsToUse; i++) {
			tasks[i] =
					() -> {
						try {
							int sliceModulo10 = 0;
							for (int cellIndex = sliceRegistry.getAndIncrement(); cellIndex < nCells; cellIndex =
									sliceRegistry.getAndIncrement()) {
								ClickedPoint p = listCells.get(cellIndex);
								ClickedPoint pCloned = (ClickedPoint) p.clone();
								pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
								pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();

								int zThickness =
										(int) (p.hasQuantifiedProperty("zThickness") ? p
												.getQuantifiedProperty("zThickness") + 1 : 1);

								int x = (int) p.x;
								int y = (int) p.y;
								int z = (int) p.z;

								int z0 = 0;
								int z1 = Math.min(depth - 1 - z, zThickness - 1);

								int y0 = Math.min(y, yRadius);
								int y1 = Math.min(height - 1 - (y), yRadius);

								int x0 = Math.min(x, xRadius);
								int x1 = Math.min(width - 1 - x, xRadius);

								int[] pixelValues = new int[xDiameter * yDiameter * zThickness];

								for (int k = -z0; k <= z1; k++) {
									for (int j = -y0; j <= y1; j++) {
										for (int i1 = -x0; i1 <= x1; i1++) {
											pixelValues[k * (xDiameter * yDiameter) + (j + yRadius) * xDiameter
													+ (i1 + xRadius)] = (int) input.getFloat(x + i1, y + j, z + k);
										}
									}
								}

								pCloned.imageFullSegCoordsX = pixelValues;

								synchronized (outputCells) {
									outputCells.addDontFireValueChanged(pCloned);
								}

								if (abort)
									return;
								if (sliceModulo10++ == 10) {
									if (Thread.interrupted()) {
										abort = true;
										return;
									}
									int ourProgress = (int) (cellIndex * progressMultiplyingFactor);
									if (ourProgress > progress.getValue())
										progressSetValueThreadSafe(progress, ourProgress); // not perfect but at least
																							// does not require
																							// synchronization
									sliceModulo10 = 0;
								}

							}
						} catch (Exception e) {
							abort = true;
							throw new RuntimeException(e);
						}
					};
			futures[i] = threadPool.submit(tasks[i], 0);
		}

		for (int i = 0; i < numberOfThreadsToUse; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException e) {
				abort = true;
				Utils.printStack(e, LogLevel.DEBUG);
				i = 0;// wait for all the tasks to complete (should be quick since we set the abort flag)
			} catch (ExecutionException e) {
				throw new RuntimeException(e.getCause());
			}
		}

		outputCells.fireValueChanged(false, false);

		if (abort)
			throw new InterruptedException();

	}

}
