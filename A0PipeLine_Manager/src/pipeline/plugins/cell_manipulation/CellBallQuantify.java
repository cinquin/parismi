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
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;
import processing_utilities.stepByStepProjection.AverageProjector;
import processing_utilities.stepByStepProjection.IProjector;
import processing_utilities.stepByStepProjection.MaxProjector;
import processing_utilities.stepByStepProjection.MinProjector;
import processing_utilities.stepByStepProjection.PercentileProjector;
import processing_utilities.stepByStepProjection.SumProjector;

/**
 * Quantify total intensity within a set of cells defined by their center and by a diameter.
 * FIXME For now, this plugin should NOT be run on more than 1 channel at a time.
 * FIXME Not dealing with z well (should take into account different z and xy resolutions when considering diameter).
 */
public class CellBallQuantify extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Quantify total intensity within a set of cells defined by their center and by a diameter,"
				+ " or by a pre-existing segmentation";
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		Map<String, InputOutputDescription> result = new HashMap<>();
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
		return PLUGIN_CREATES_OUTPUT_ITSELF + ONLY_1_INPUT_CHANNEL;
	}

	@ParameterInfo(userDisplayName = "Store field", stringValue = "DAPI content",
			description = "Name of field to store results into")
	String fieldName;

	@ParameterInfo(userDisplayName = "Diameter", floatValue = 3f, permissibleFloatRange = { 1f, 50f })
	private float diameter;

	@ParameterInfo(userDisplayName = "Diameter specified in pixels", booleanValue = true,
			noErrorIfMissingOnReload = true)
	private boolean diameterInPixels;

	@ParameterInfo(
			userDisplayName = "Use segmentation",
			description = "If active contour has been run, quantify pixels contained within resulting segmentation; if not, use ball centered on seed",
			booleanValue = false)
	private boolean useSegmentation;

	@ParameterInfo(userDisplayName = "Use embedded diameter",
			description = "Use localDiameter field provided by each seed", booleanValue = false)
	private boolean useEmbeddedDiameter;

	@ParameterInfo(userDisplayName = "Quantification method", stringChoices = { "Sum intensities", "Average intensity",
			"Max intensity", "Percentile intensity" }, stringValue = "Sum intensities")
	@ParameterType(parameterType = "ComboBox", printValueAsString = true)
	private String method;

	@ParameterInfo(userDisplayName = "Percentile", floatValue = 95, permissibleFloatRange = { 0, 100 },
			noErrorIfMissingOnReload = true)
	private float percentile;

	@ParameterInfo(userDisplayName = "Only quantify perimeter", stringValue = "FALSE", noErrorIfMissingOnReload = true)
	private boolean usePerimSeg = false;

	@ParameterInfo(userDisplayName = "Ignore pixels of value 0", stringValue = "FALSE", noErrorIfMissingOnReload = true)
	private boolean ignoreZero = false;

	@ParameterInfo(userDisplayName = "Disk with same Z as seed", stringValue = "FALSE", noErrorIfMissingOnReload = true)
	private boolean diskOnly = false;

	@Override
	public String operationName() {
		return "Cell Ball Quantify";
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

	AtomicInteger cellsWithoutPixels = new AtomicInteger();

	IProjector getProjector() {
		IProjector projector;
		switch (method) {
			case "Sum intensities":
				projector = new SumProjector();
				break;
			case "Average intensity":
				projector = new AverageProjector();
				break;
			case "Max intensity":
				projector = new MaxProjector();
				break;
			case "Min intensity":
				projector = new MinProjector();
				break;
			case "Percentile intensity":
				projector = new PercentileProjector(percentile);
				break;
			default:
				throw new IllegalStateException("Unknown quantification method " + method);
		}
		return projector;
	}

	/**
	 * Use pre-existing segmentation mask, but within that mask only consider pixels that are
	 * within specified radius of center.
	 * Set to true by CellRecenterToLowSignal
	 */
	boolean applyRadiusToSegmentation = false;

	IProjector getProjector(ClickedPoint p, IPluginIOList<ClickedPoint> allInputPoints, IPluginIOStack input) {

		IProjector projector = getProjector();

		double radius = diameter / 2;
		double radiusSq = radius * radius;
		if (useEmbeddedDiameter) {
			if (!p.hasQuantifiedProperty("localDiameter")) {
				throw new IllegalArgumentException("localDiameter field is missing");
			}
			radius = p.getQuantifiedProperty("localDiameter") * 0.5;
			radiusSq = radius * radius;
		}

		final int width = input.getWidth();
		final int height = input.getHeight();
		final int depth = input.getDepth();

		double xCenter = p.x;
		double yCenter = p.y;
		double zCenter = p.z;

		int xCenterInt = (int) xCenter;
		int yCenterInt = (int) yCenter;
		int zCenterInt = (int) zCenter;

		double xyCalib = diameterInPixels || p.xyCalibration == 0 ? 1 : p.xyCalibration;
		double zCalib = diameterInPixels || p.zCalibration == 0 ? 1 : p.zCalibration;

		boolean noPixels = false;
		if (!useSegmentation) {
			int x0, x1, y0, y1, z0, z1;

			z0 = (int) (diskOnly ? 0 : Math.min(zCenter, radius / xyCalib));
			z1 = (int) (diskOnly ? 0 : Math.min(depth - 1 - zCenter, radius / xyCalib));

			y0 = (int) Math.min(yCenter, radius / xyCalib);
			y1 = (int) Math.min(height - 1 - (yCenter), radius / xyCalib);

			x0 = (int) Math.min(xCenter, radius / zCalib);
			x1 = (int) Math.min(width - 1 - xCenter, radius / zCalib);

			for (int k = -z0; k <= z1; k++) {
				double kSq = k * k * zCalib * zCalib;
				for (int j = -y0; j <= y1; j++) {
					double jSq = j * j * xyCalib;
					for (int i = -x0; i <= x1; i++) {
						double iSq = i * i * xyCalib;
						if (kSq + jSq + iSq > radiusSq)
							continue;
						float f = input.getFloat(xCenterInt + i, yCenterInt + j, zCenterInt + k);
						if (ignoreZero && f == 0)
							continue;
						projector.add(f);
						// pixelValues.add(f);
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
			float f;

			for (int i = 0; i < xCoord.length; i++) {
				if (applyRadiusToSegmentation) {
					double distanceSq =
							Math.pow((xCoord[i] - xCenter) * xyCalib, 2) + Math.pow((yCoord[i] - yCenter) * xyCalib, 2)
									+ Math.pow((zCoord[i] - zCenter) * zCalib, 2);
					if (distanceSq > radiusSq)
						continue;
				}
				if (diskOnly && zCoord[i] != zCenter)
					continue;
				f = input.getFloat(xCoord[i], yCoord[i], zCoord[i]);
				if (ignoreZero && f == 0)
					continue;
				projector.add(f);
			}
		}

		if (noPixels)
			return null;
		else
			return projector;
	}

	ClickedPoint transform(ClickedPoint p, IPluginIOList<ClickedPoint> allInputPoints, IPluginIOStack input,
			boolean fieldIsNew, PluginIOCells outputCells) {

		IProjector projector = getProjector(p, allInputPoints, input);

		ClickedPoint pCloned = (ClickedPoint) p.clone();
		pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
		pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();
		if (fieldIsNew)
			pCloned.getQuantifiedProperties().add(0f);

		if (projector == null) {
			cellsWithoutPixels.getAndIncrement();
			pCloned.setQuantifiedProperty(fieldName, 0);
		} else {
			pCloned.setQuantifiedProperty(fieldName, (float) projector.project());
		}

		return pCloned;
	}

	@Override
	public void runChannel(final IPluginIOStack input, IPluginIOStack output, ProgressReporter r,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		Utils.log("Running ball quantification", LogLevel.DEBUG);

		final PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");
		final PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Seeds");
		outputCells.clear();
		inputCells.copyInto(outputCells);

		final boolean fieldIsNew = !outputCells.addQuantifiedPropertyName(fieldName);

		if (usePerimSeg && !useSegmentation) {
			throw new PluginRuntimeException(new IllegalArgumentException(
					"Perimeter segmentation only works with embedded segmentation"), true);
		}

		input.computePixelArray();

		// FIXME Change to BallIterator, from which following code was copied
		int parForMaxIndex = inputCells.getPoints().size() - 1;
		ParFor parFor = new ParFor(0, parForMaxIndex, r, threadPool, true);

		cellsWithoutPixels.set(0);

		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker((loopIndex, threadIndex) -> transform(inputCells.get(loopIndex), inputCells, input,
					fieldIsNew, outputCells));

		for (Object p : parFor.run(true)) {
			if (p == null) {
				throw new IllegalStateException("Null point");
			}
			outputCells.addDontFireValueChanged((ClickedPoint) p);
		}

		outputCells.fireValueChanged(false, false);

		if (cellsWithoutPixels.get() > 0) {
			Utils.displayMessage(cellsWithoutPixels.get() + " cells had an empty segmentation; value set to 0", true,
					LogLevel.WARNING);
		}

	}

}
