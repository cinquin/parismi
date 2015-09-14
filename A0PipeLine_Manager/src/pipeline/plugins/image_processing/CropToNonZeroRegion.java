/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.ProgressSubrange;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.SpecialDimPlugin;

/**
 * Crops a stack or hyperstack to the smallest rectangular region that contains all non-zero pixels in all slices and
 * all channels.
 * TODO The pipeline should have the option to recreate the destination if the source has changed. At the moment the
 * output dimension
 * is stuck to what was computed when the plugin was first run.
 * TODO At the moment two runs are done on the pixels (one to determine the size of the output, one to copy the
 * contents). This could
 * be optimized.
 * FIXME This won't work properly with multiple time points if each time point does not end up with the same number of
 * non-0 slices
 *
 */
public class CropToNonZeroRegion extends FourDPlugin implements SpecialDimPlugin, AuxiliaryInputOutputPlugin {

	@Override
	public String[] getInputLabels() {
		return new String[] { "Aux 1", "Aux 2", "Aux 3", "Aux 4", "Aux 5" };
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

	@Override
	public String operationName() {
		return "CropToNonZeroRegion";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return DONT_ALLOCATE_OUTPUT_PIXELS;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@SuppressWarnings("unused")
	private int inputWidth, inputHeight, inputDepth;

	private int gminX = Integer.MAX_VALUE;
	private int gmaxX = 0;
	private int gminY = Integer.MAX_VALUE;
	private int gmaxY = 0;
	private int gminZ = Integer.MAX_VALUE;
	private int gmaxZ = 0;

	private PixelType pType = null;

	private boolean dimensionsComputed = false;

	@Override
	public void clearInputs() {
		super.clearInputs();
		dimensionsComputed = false;
	}

	@Override
	public void setInput(IPluginIO source) {
		super.setInput(source);
		dimensionsComputed = false;
	}

	private void computeOutputDimensions() {
		if (dimensionsComputed) {
			Utils.log("Skipping output dimension computing", LogLevel.DEBUG);
			return;
		} else {
			Utils.log("Computing ouput dimensions", LogLevel.DEBUG);
		}
		final AtomicInteger globalMinX = new AtomicInteger(Integer.MAX_VALUE);
		final AtomicInteger globalMinY = new AtomicInteger(Integer.MAX_VALUE);
		final AtomicInteger globalMinZ = new AtomicInteger(Integer.MAX_VALUE);
		final AtomicInteger globalMaxX = new AtomicInteger(0);
		final AtomicInteger globalMaxY = new AtomicInteger(0);
		final AtomicInteger globalMaxZ = new AtomicInteger(0);

		// TODO Parallelize this computation
		ArrayList<Future<?>> futures = new ArrayList<>(10);

		for (IPluginIO io : pluginInputs.values()) {
			if (io instanceof IPluginIOHyperstack) {
				Utils.log("Found a hyperstack", LogLevel.DEBUG);
				final IPluginIOHyperstack hst = (IPluginIOHyperstack) io;
				final int width = hst.getWidth();
				final int height = hst.getHeight();
				inputWidth = width;
				inputHeight = height;
				inputDepth = hst.getDepth();
				for (final IPluginIOStack channel : hst.getChannels().values()) {

					Runnable task = () -> {
						int minX = Integer.MAX_VALUE;
						int maxX = 0;
						int minY = Integer.MAX_VALUE;
						int maxY = 0;
						int minZ = Integer.MAX_VALUE;
						int maxZ = 0;

						Utils.log("Found a channel", LogLevel.DEBUG);
						for (int z = 0; z < hst.getDepth(); z++) {
							if (channel.getPixels(z) == null) {
								Utils.log("null input slice", LogLevel.WARNING);
							}
							Object pixelsAsObject = channel.getPixels(z);
							if (pixelsAsObject instanceof float[]) {
								pType = PixelType.FLOAT_TYPE;
								float[] pixels = (float[]) pixelsAsObject;
								for (int x = 0; x < width; x++) {
									for (int y = 0; y < height; y++) {
										if (pixels[y * width + x] != 0) {
											if (x < minX)
												minX = x;
											if (y < minY)
												minY = y;
											if (z < minZ)
												minZ = z;
											if (x > maxX)
												maxX = x;
											if (y > maxY)
												maxY = y;
											if (z > maxZ)
												maxZ = z;
										}
									}
								}
							} else if (pixelsAsObject instanceof short[]) {
								pType = PixelType.SHORT_TYPE;
								short[] pixels = (short[]) pixelsAsObject;
								for (int x = 0; x < width; x++) {
									for (int y = 0; y < height; y++) {
										if (pixels[y * width + x] != 0) {
											if (x < minX)
												minX = x;
											if (y < minY)
												minY = y;
											if (z < minZ)
												minZ = z;
											if (x > maxX)
												maxX = x;
											if (y > maxY)
												maxY = y;
											if (z > maxZ)
												maxZ = z;
										}
									}
								}
							} else if (pixelsAsObject instanceof byte[]) {
								pType = PixelType.BYTE_TYPE;
								byte[] pixels = (byte[]) pixelsAsObject;
								for (int x = 0; x < width; x++) {
									for (int y = 0; y < height; y++) {
										if (pixels[y * width + x] != 0) {
											if (x < minX)
												minX = x;
											if (y < minY)
												minY = y;
											if (z < minZ)
												minZ = z;
											if (x > maxX)
												maxX = x;
											if (y > maxY)
												maxY = y;
											if (z > maxZ)
												maxZ = z;
										}
									}
								}
							}
						}
						synchronized (globalMinX) {
							if (minX < globalMinX.get())
								globalMinX.set(minX);
							if (minY < globalMinY.get())
								globalMinY.set(minY);
							if (minZ < globalMinZ.get())
								globalMinZ.set(minZ);

							if (maxX > globalMaxX.get())
								globalMaxX.set(maxX);
							if (maxY > globalMaxY.get())
								globalMaxY.set(maxY);
							if (maxZ > globalMaxZ.get())
								globalMaxZ.set(maxZ);

						}
					};
					futures.add(threadPool.submit(task));
				}
			}
		}

		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				Utils.printStack(e);
			}
		}

		gminX = globalMinX.get();
		gminY = globalMinY.get();
		gminZ = globalMinZ.get();

		gmaxX = globalMaxX.get();
		gmaxY = globalMaxY.get();
		gmaxZ = globalMaxZ.get();

		dimensionsComputed = true;
		
		Utils.log("Done computing dimensions", LogLevel.DEBUG);
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		computeOutputDimensions();
		return (gmaxZ - gminZ + 1);
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		computeOutputDimensions();
		return (gmaxY - gminY + 1);
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		int sumChannels = 0;
		for (IPluginIO io : pluginInputs.values()) {
			if (io instanceof IPluginIOHyperstack) {
				sumChannels += ((IPluginIOHyperstack) io).getnChannels();
			}
		}
		return sumChannels;
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		computeOutputDimensions();
		return (gmaxX - gminX + 1);
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		IPluginIOHyperstack destination = (IPluginIOHyperstack) getOutput();
		Iterator<IPluginIOStack> destinationIterator = destination.getChannels().values().iterator();

		ProgressSubrange p2 = new ProgressSubrange(r, 1);

		List<ParFor> parFors = new ArrayList<>();

		int nToDo = 0;

		for (IPluginIO io : pluginInputs.values()) {
			if (io instanceof IPluginIOHyperstack) {
				final IPluginIOHyperstack hst = (IPluginIOHyperstack) io;
				final int width = hst.getWidth();
				final int outputWidth = gmaxX - gminX + 1;
				final int outputHeight = gmaxY - gminY + 1;
				for (final IPluginIOStack channel : hst.getChannels().values()) {
					final IPluginIOStack destinationChannel =
							(!destinationIterator.hasNext()) ? destination.addChannel(null) : destinationIterator
									.next();

					int localNToDo = (gmaxZ - gminZ + 1) * hst.getnTimePoints();
					nToDo += localNToDo;
					ParFor parFor = new ParFor("Crop to non-zero", 0, localNToDo - 1, p2, true);
					parFors.add(parFor);
					for (int i = 0; i < parFor.getNThreads(); i++)
						parFor.addLoopWorker((z, threadIndex) -> {

							Object pixelsAsObject = channel.getPixels(z);
							if (pixelsAsObject instanceof float[]) {
								float[] pixels = (float[]) pixelsAsObject;
								float[] destinationPixels = (float[]) destinationChannel.getPixels(z + gminZ);
								for (int x = 0; x < outputWidth; x++) {
									for (int y = 0; y < outputHeight; y++) {
										destinationPixels[y * outputWidth + x] =
												pixels[(y + gminY) * width + (x + gminX)];
									}
								}
							} else if (pixelsAsObject instanceof short[]) {
								short[] pixels = (short[]) pixelsAsObject;
								if (destinationChannel.getPixels(z + gminZ) instanceof short[]) {
									short[] destinationPixels = (short[]) destinationChannel.getPixels(z + gminZ);
									for (int x = 0; x < outputWidth; x++) {
										for (int y = 0; y < outputHeight; y++) {
											destinationPixels[y * outputWidth + x] =
													pixels[(y + gminY) * width + (x + gminX)];
										}
									}
								} else if (destinationChannel.getPixels(z + gminZ) instanceof float[]) {
									float[] destinationPixels = (float[]) destinationChannel.getPixels(z + gminZ);
									for (int x = 0; x < outputWidth; x++) {
										for (int y = 0; y < outputHeight; y++) {
											destinationPixels[y * outputWidth + x] =
													pixels[(y + gminY) * width + (x + gminX)];
										}
									}
								} else
									throw new RuntimeException("Unhandled destination pixel type "
											+ destinationChannel.getPixels(z + gminZ));
							} else if (pixelsAsObject instanceof byte[]) {
								byte[] pixels = (byte[]) pixelsAsObject;
								byte[] destinationPixels = (byte[]) destinationChannel.getPixels(z + gminZ);
								for (int x = 0; x < outputWidth; x++) {
									for (int y = 0; y < outputHeight; y++) {
										destinationPixels[y * outputWidth + x] =
												pixels[(y + gminY) * width + (x + gminX)];
									}
								}
							} else
								throw new IllegalArgumentException("Unknown pixel type " + pixelsAsObject);
							return null;
						});
					parFor.run(false);
					// futures.add(threadPool.submit(task));

				}
			}
		}

		p2.setMax(nToDo);

		for (ParFor parFor : parFors) {
			parFor.waitForCompletion();
		}

	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		Utils.log("Output depth " + getOutputDepth(getInput()), LogLevel.DEBUG);
		PluginIOHyperstack createdOutput =
				new PluginIOHyperstack("Cropped", getOutputWidth(getInput()), getOutputHeight(getInput()),
						getOutputDepth(getInput()), ((IPluginIOImage) getInput()).getDimensions().nChannels,
						((IPluginIOImage) getInput()).getDimensions().nTimePoints, ((IPluginIOImage) getInput())
								.getPixelType(), false);

		createdOutput.setImageAcquisitionMetadata(((IPluginIOImage) getInput()).getImageAcquisitionMetadata());

		int nChannelsToAdd = getOutputNChannels(null) - ((IPluginIOImage) getInput()).getDimensions().nChannels;

		for (int i = 0; i < nChannelsToAdd; i++) {
			createdOutput.addChannel("Aux channel " + i);
		}

		setOutput("Default destination", createdOutput, true);

		List<PluginIOView> imagesToShow = new ArrayList<>();

		PluginIOHyperstackViewWithImagePlus display = null;
		if (!Utils.headless) {
			if (impForDisplay != null) {
				createdOutput.setImp(impForDisplay);
				display = impForDisplay;
			} else {
				display = new PluginIOHyperstackViewWithImagePlus(createdOutput.getName());
				createdOutput.setImp(display);
			}

			display.addImage(createdOutput);
			display.shouldUpdateRange = true;
			imagesToShow.add(display);
		}
		createdOutput.setCalibration((Calibration) ((IPluginIOImage) getInput()).getCalibration().clone());

		return imagesToShow;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		computeOutputDimensions();
		return pType;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		// TODO Auto-generated method stub
		return ((PluginIOHyperstack) input).getnTimePoints();
	}
}
