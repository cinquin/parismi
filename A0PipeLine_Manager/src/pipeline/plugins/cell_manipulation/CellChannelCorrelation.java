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
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ILoopWorker;
import pipeline.misc_util.parfor.ParFor;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;
import processing_utilities.stepByStepProjection.AverageProjector;
import processing_utilities.stepByStepProjection.IProjector;

/**
 * FIXME For now, this plugin should NOT be run on more than 1 channel at a time.
 */
public class CellChannelCorrelation extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Compute offset that maximizes the average correlation across cells between the two images "
				+ "given as input; store corresponding correlation as a new field in the cells, and display "
				+ "a version of the auxiliary shifted with optimal offset";
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
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		Utils.log("Creating seeds", LogLevel.DEBUG);
		initializeOutputs();
		PluginIOCells seeds = new PluginIOCells();
		ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
		view.setData(seeds);
		pluginOutputs.put("Seeds", seeds);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);

		PluginIOHyperstack createdOutput =
				new PluginIOHyperstack("CellChannelCorrelation", getImageInput().getDimensions().width, getImageInput()
						.getDimensions().height, getImageInput().getDimensions().depth, 1, 1, getImageInput()
						.getPixelType(), false);

		createdOutput.setCalibration(getImageInput().getCalibration());
		setOutput("Realigned", createdOutput, true);

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
			views.add(display);
		}

		return views;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds", desc0);
		InputOutputDescription desc =
				new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
						InputOutputDescription.KEEP_IN_RAM, InputOutputDescription.CUSTOM, true, false);
		desc.useDefaultIfMatchingAbsent = true;
		result.put("Default destination", desc);
		return result;
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF + ONLY_1_INPUT_CHANNEL;
	}

	@ParameterInfo(userDisplayName = "Store field", stringValue = "correlation",
			description = "Name of field to store results into")
	private String fieldName;

	@ParameterInfo(userDisplayName = "Rank field", stringValue = "correlationRank",
			description = "Name of field to store rank into", noErrorIfMissingOnReload = true)
	private String correlationRankFieldName;

	@ParameterInfo(userDisplayName = "Maximum offset", floatValue = 10f, permissibleFloatRange = { 1f, 20f })
	private int maxOffset;

	@ParameterInfo(userDisplayName = "Ball diameter", floatValue = 3f, permissibleFloatRange = { 1f, 50f })
	private float diameter;

	@ParameterInfo(
			userDisplayName = "Use segmentation",
			description = "If active contour has been run, quantify pixels contained within resulting segmentation; if not, use ball centered on seed",
			booleanValue = true)
	private boolean useSegmentation;

	@ParameterInfo(userDisplayName = "Use embedded diameter",
			description = "Use localDiameter field provided by each seed", booleanValue = false)
	private boolean useEmbeddedDiameter;

	@ParameterInfo(userDisplayName = "Only quantify perimeter", stringValue = "FALSE", noErrorIfMissingOnReload = true)
	private boolean usePerimSeg = false;

	@ParameterInfo(userDisplayName = "Ignore pixels of value 0", stringValue = "FALSE", noErrorIfMissingOnReload = true)
	private boolean ignoreZero = false;

	@ParameterInfo(userDisplayName = "Aux image min field", stringValue = "xx",
			description = "Field for minimal aux image value to consider", noErrorIfMissingOnReload = true)
	private String localThreshold;

	@ParameterInfo(userDisplayName = "Output ks", booleanValue = false, description = "",
			noErrorIfMissingOnReload = true)
	private boolean outputKs;

	@Override
	public String operationName() {
		return "Cell Channel Correlation";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds", "Image 2", "Image2Shift" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds" };
	}

	private AtomicInteger cellsWithoutPixels = new AtomicInteger();

	interface ImageFunctionI {
		public double apply(ClickedPoint cell, int index, IPluginIOStack image1, IPluginIOStack image2, int x, int y,
				int z, int x2, int y2, int z2);
	}

	private abstract class ImageFunction implements ImageFunctionI {
	}

	// From http://stackoverflow.com/questions/1519736/random-shuffling-of-an-array
	// Implementing Fisher–Yates shuffle
	private static void shuffleArray(int[] ar) {
		Random rnd = new Random();
		for (int i = ar.length - 1; i >= 0; i--) {
			int index = rnd.nextInt(i + 1);
			// Simple swap
			int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

	private double[] applyFunction(final PluginIOCells cells, final IPluginIOStack image1, final IPluginIOStack image2,
			final String image2minThresholdFieldName, final ImageFunctionI function, final PluginIOCells outputCells,
			final boolean storeResultInField, final String storeFieldName, final boolean fieldIsNew,
			final boolean randomize, final boolean clonePoint) throws InterruptedException {
		final double[] result = new double[cells.size()];

		int parForMaxIndex = cells.getPoints().size() - 1;
		ParFor parFor = new ParFor(0, parForMaxIndex, null, threadPool, true);

		final int radius = (int) (diameter / 2);

		final int width = image1.getWidth();
		final int height = image1.getHeight();
		final int radiusSq = radius * radius;
		final int depth = image1.getDepth();

		cellsWithoutPixels.set(0);

		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker(new ILoopWorker() {
				int localRadius = radius;
				int localRadiusSq = radiusSq;
				List<ClickedPoint> pointList = cells.getPoints();

				@Override
				public final Object run(int loopIndex, int threadIndex) {

					IProjector projector;
					projector = new AverageProjector();

					ClickedPoint p = pointList.get(loopIndex);
					int[] pixelRandomization = new int[p.imageFullSegCoordsX.length];
					for (int i = 0; i < pixelRandomization.length; i++) {
						pixelRandomization[i] = i;
					}

					if (randomize)
						shuffleArray(pixelRandomization);

					ClickedPoint pCloned = null;
					if (outputCells != null) {
						if (!clonePoint)
							pCloned = p;
						else {
							pCloned = (ClickedPoint) p.clone();
							pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
							pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();
							if (fieldIsNew)
								pCloned.getQuantifiedProperties().add(0f);
						}
					}

					if (useEmbeddedDiameter) {
						if (!p.hasQuantifiedProperty("localDiameter")) {
							throw new IllegalArgumentException("localDiameter field is missing");
						}
						localRadius = (int) (p.getQuantifiedProperty("localDiameter") * 0.5);
						localRadiusSq = localRadius * localRadius;
					}

					boolean noPixels = false;
					if (!useSegmentation) {
						int x = (int) p.x;
						// Utils.log("x is "+x,LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
						int y = (int) p.y;
						int z = (int) p.z;

						int z0 = Math.min(z, localRadius);
						int z1 = Math.min(depth - 1 - z, localRadius);

						int y0 = Math.min(y, localRadius);
						int y1 = Math.min(height - 1 - (y), localRadius);

						int x0 = Math.min(x, localRadius);
						int x1 = Math.min(width - 1 - x, localRadius);

						for (int k = -z0; k <= z1; k++) {
							int kSq = k * k;
							for (int j = -y0; j <= y1; j++) {
								int jSq = j * j;
								for (int i = -x0; i <= x1; i++) {
									int iSq = i * i;
									if (kSq + jSq + iSq > localRadiusSq)
										continue;
									float f = 0;
									if (ignoreZero && f == 0)
										continue;
									projector.add(f);
									throw new RuntimeException("Unimplemented");
								}
							}
						}

					} else { // use segmentation
						int[] xCoord = usePerimSeg ? p.imagePerimsegCoordsX : p.imageFullSegCoordsX;
						int[] yCoord = usePerimSeg ? p.imagePerimsegCoordsY : p.imageFullSegCoordsY;
						int[] zCoord = usePerimSeg ? p.imagePerimsegCoordsZ : p.imageFullSegCoordsZ;

						if (xCoord == null)
							throw new IllegalArgumentException("Missing segmentation");

						if (xCoord.length == 0) {
							noPixels = true;
						}
						float minThreshold = -Float.MAX_VALUE;

						if (!"".equals(localThreshold)) {
							minThreshold = p.getQuantifiedProperty(localThreshold);
						}
						double f;
						for (int i = 0; i < xCoord.length; i++) {
							if (image2.getPixelValue(xCoord[pixelRandomization[i]], yCoord[pixelRandomization[i]],
									zCoord[pixelRandomization[i]]) < minThreshold)
								continue;
							f =
									function.apply(p, loopIndex, image1, image2, xCoord[i], yCoord[i], zCoord[i],
											xCoord[pixelRandomization[i]], yCoord[pixelRandomization[i]],
											zCoord[pixelRandomization[i]]);
							// if (ignoreZero && f==0)
							// continue;
							projector.add(f);
						}
					}
					double localResult = projector.project();
					if (Double.isNaN(localResult))
						localResult = 0;
					result[loopIndex] = localResult;
					if (storeResultInField) {
						if (noPixels) {
							cellsWithoutPixels.getAndIncrement();
							if (pCloned != null)
								pCloned.setQuantifiedProperty(storeFieldName, 0);

						} else if (pCloned != null)
							pCloned.setQuantifiedProperty(storeFieldName, (float) localResult);
					}
					return pCloned;
				}
			});

		Stream<Object> s = parFor.run(true).stream();
		if (outputCells != null && clonePoint)
			s.forEach(p -> outputCells.addDontFireValueChanged((ClickedPoint) p));

		return result;
	}

	private double getCorrelation(final int xOffset, final int yOffset, PluginIOCells inputCells,
			PluginIOCells outputCells, IPluginIOStack image1, IPluginIOStack image2, int nRandomize)
			throws InterruptedException {

		final int width = image1.getWidth();
		final int height = image1.getHeight();

		ImageFunction identity = new ImageFunction() {
			@Override
			public final double apply(ClickedPoint cell, int loopIndex, IPluginIOStack image1, IPluginIOStack image2,
					int x, int y, int z, int x2, int y2, int z2) {
				return image1.getFloat(x, y, z);
			}
		};

		final double[] averageImage1 =
				applyFunction(inputCells, image1, image2, localThreshold, identity, null, false, null, false, false,
						false);
		final double[] averageImage2 =
				applyFunction(inputCells, image2, image2, localThreshold, identity, null, false, null, false, false,
						false);

		ImageFunction variance1Func = new ImageFunction() {
			@Override
			public final double apply(ClickedPoint cell, int loopIndex, IPluginIOStack image1, IPluginIOStack image2,
					int x, int y, int z, int x2, int y2, int z2) {
				return (float) Math.pow(image1.getFloat(x, y, z) - averageImage1[loopIndex], 2);
			}
		};

		ImageFunction variance2Func = new ImageFunction() {
			@Override
			public final double apply(ClickedPoint cell, int loopIndex, IPluginIOStack image1, IPluginIOStack image2,
					int x, int y, int z, int x2, int y2, int z2) {
				return (float) Math.pow(image1.getFloat(x, y, z) - averageImage2[loopIndex], 2);
			}
		};

		ImageFunction sumSquaresFunc = new ImageFunction() {
			@Override
			public final double apply(ClickedPoint cell, int loopIndex, IPluginIOStack image1, IPluginIOStack image2,
					int x, int y, int z, int x2, int y2, int z2) {
				return (float) Math.pow(image1.getFloat(x, y, z), 2);
			}
		};

		final double[] variance1 =
				applyFunction(inputCells, image1, image2, localThreshold, variance1Func, null, false, null, false,
						false, false);
		final double[] variance2 =
				applyFunction(inputCells, image2, image2, localThreshold, variance2Func, null, false, null, false,
						false, false);

		final double[] sumSquares1 =
				applyFunction(inputCells, image1, image2, localThreshold, sumSquaresFunc, null, false, null, false,
						false, false);
		final double[] sumSquares2 =
				applyFunction(inputCells, image2, image1, localThreshold, sumSquaresFunc, null, false, null, false,
						false, false);

		ImageFunction crossFunc = new ImageFunction() {
			@Override
			public final double apply(ClickedPoint cell, int loopIndex, IPluginIOStack image1, IPluginIOStack image2,
					int x, int y, int z, int x2, int y2, int z2) {
				return (float) ((image1.getFloat(x, y, z) - averageImage1[loopIndex])
						* (image2.getFloat(Math.min(Math.max(x2 + xOffset, 0), width - 1), Math.min(Math.max(y2
								+ yOffset, 0), height - 1), z2) - averageImage2[loopIndex]) / Math
						.sqrt(variance1[loopIndex] * variance2[loopIndex]));
			}
		};

		ImageFunction crossFuncNoAverageSub = new ImageFunction() {
			@Override
			public final double apply(ClickedPoint cell, int loopIndex, IPluginIOStack image1, IPluginIOStack image2,
					int x, int y, int z, int x2, int y2, int z2) {
				return (float) (image1.getFloat(x, y, z)
						* image2.getFloat(Math.min(Math.max(x2 + xOffset, 0), width - 1), Math.min(Math.max(y2
								+ yOffset, 0), height - 1), z2) / Math.sqrt(sumSquares1[loopIndex]
						* sumSquares2[loopIndex]));
			}
		};

		ImageFunction crossFuncNoAverageSubVar1 = new ImageFunction() {
			@Override
			public final double apply(ClickedPoint cell, int loopIndex, IPluginIOStack image1, IPluginIOStack image2,
					int x, int y, int z, int x2, int y2, int z2) {
				return image1.getFloat(x, y, z)
						* image2.getFloat(Math.min(Math.max(x2 + xOffset, 0), width - 1), Math.min(Math.max(y2
								+ yOffset, 0), height - 1), z2) / sumSquares1[loopIndex];
			}
		};

		ImageFunction crossFuncNoAverageSubVar2 = new ImageFunction() {
			@Override
			public final double apply(ClickedPoint cell, int loopIndex, IPluginIOStack image1, IPluginIOStack image2,
					int x, int y, int z, int x2, int y2, int z2) {
				return image1.getFloat(x, y, z)
						* image2.getFloat(Math.min(Math.max(x2 + xOffset, 0), width - 1), Math.min(Math.max(y2
								+ yOffset, 0), height - 1), z2) / sumSquares2[loopIndex];
			}
		};

		boolean fieldIsNew = (outputCells != null) && !outputCells.addQuantifiedPropertyName(fieldName);

		double[] correlation =
				applyFunction(inputCells, image1, image2, localThreshold, crossFunc, outputCells, true, fieldName,
						fieldIsNew, false, true);

		if (outputKs && outputCells != null) {
			fieldIsNew = !outputCells.addQuantifiedPropertyName("k1");
			applyFunction(outputCells, image1, image2, localThreshold, crossFuncNoAverageSubVar1, outputCells, true,
					"k1", fieldIsNew, false, false);

			fieldIsNew = !outputCells.addQuantifiedPropertyName("k2");
			applyFunction(outputCells, image1, image2, localThreshold, crossFuncNoAverageSubVar2, outputCells, true,
					"k2", fieldIsNew, false, false);

			fieldIsNew = !outputCells.addQuantifiedPropertyName("r");
			applyFunction(outputCells, image1, image2, localThreshold, crossFuncNoAverageSub, outputCells, true, "r",
					fieldIsNew, false, false);

		}

		if (nRandomize > 0 && outputCells != null) {

			List<double[]> randomizedCorrelations = new ArrayList<>();
			for (int n = 0; n < nRandomize; n++) {
				double[] randomCorrelation =
						applyFunction(inputCells, image1, image2, localThreshold, crossFunc, null, false, null, false,
								true, false);
				randomizedCorrelations.add(randomCorrelation);
			}

			final boolean rankFieldIsNew = !outputCells.addQuantifiedPropertyName(correlationRankFieldName);

			// Compute rank
			for (int i = 0; i < outputCells.size(); i++) {
				ClickedPoint cell = getCellID(outputCells, inputCells.get(i).getSeedId());
				if (rankFieldIsNew)
					cell.getQuantifiedProperties().add(0f);
				float rank = 0;
				for (double[] randomizedCorrelation : randomizedCorrelations) {
					if (correlation[i] > randomizedCorrelation[i])
						rank++;
				}
				cell.setQuantifiedProperty(correlationRankFieldName, rank / nRandomize);
			}
		}

		double result = 0;
		for (double i : correlation) {
			result += i;
		}
		return (result / correlation.length);
	}

	static ClickedPoint getCellID(PluginIOCells cells, float seedId) {
		for (ClickedPoint p : cells) {
			if (p.getSeedId() == seedId)
				return p;
		}
		return null;
	}

	@Override
	public void runChannel(final IPluginIOStack input, IPluginIOStack output, ProgressReporter r,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		IPluginIOStack image2 =
				((IPluginIOHyperstack) getInputs().get("Image 2")).getChannels().values().iterator().next();

		IPluginIOStack image2Shift = null;
		if (getInputs().get("Image2Shift") != null)
			image2Shift =
					((IPluginIOHyperstack) getInputs().get("Image2Shift")).getChannels().values().iterator().next();
		if (image2Shift == null)
			image2Shift = image2;

		final PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");
		final PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Seeds");
		outputCells.clear();
		inputCells.copyInto(outputCells);

		final int width = input.getWidth();
		final int height = input.getHeight();
		final int depth = input.getDepth();

		if (image2.getWidth() != width || image2.getHeight() != height || image2.getDepth() != depth)
			throw new IllegalArgumentException("Non-matching dimensions in input images");

		input.computePixelArray();
		image2.computePixelArray();

		if ("xx".equals(localThreshold))
			localThreshold = "";

		int nRandomize = 0;

		double maxCorr = 0;
		int bestxOffset = 0, bestyOffset = 0;
		r.setIndeterminate(false);
		for (int xOffset = -maxOffset; xOffset <= maxOffset; xOffset++) {
			r.setValue(100 * (xOffset + maxOffset) / (2 * maxOffset + nRandomize));
			if (Thread.interrupted())
				throw new RuntimeException("Interrupted");
			for (int yOffset = -maxOffset; yOffset <= maxOffset; yOffset++) {
				double corr = getCorrelation(xOffset, yOffset, inputCells, null, input, image2, 0);
				if (corr > maxCorr) {
					maxCorr = corr;
					bestxOffset = xOffset;
					bestyOffset = yOffset;
				}
			}
		}

		Utils.log("Best offset: " + bestxOffset + ", " + bestyOffset, LogLevel.INFO);

		if (Math.abs(bestxOffset) == maxOffset || Math.abs(bestyOffset) == maxOffset) {
			Utils.log("Best offset reached boundary of search domain ", LogLevel.ERROR);
		}

		getCorrelation(bestxOffset, bestyOffset, inputCells, outputCells, input, image2, nRandomize);

		outputCells.fireValueChanged(false, false);

		for (int x = 0; x < width; x++) {
			int xOffset = x - bestxOffset;
			if (xOffset < 0 || xOffset >= width)
				continue;
			for (int y = 0; y < height; y++) {
				int yOffset = y - bestyOffset;
				if (yOffset < 0 || yOffset >= height)
					continue;
				for (int z = 0; z < input.getDepth(); z++) {
					output.setPixelValue(xOffset, yOffset, z, image2Shift.getPixelValue(x, y, z));
				}
			}
		}

		if (cellsWithoutPixels.get() > 0) {
			Utils.displayMessage(cellsWithoutPixels.get() + " cells had an empty segmentation; value set to 0", true,
					LogLevel.WARNING);
		}

	}

}
