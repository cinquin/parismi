/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeyMissingException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * This plugin is used to select a subset of the cells detected by the SVMCellDetector plugin.
 * Code implements algorithm developed by Sam Hallman and Charless Fowlkes.
 */

public class SVMSuppress extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Select subset of cells by imposing a minimal distance between cells and a minimal detection confidence";
	}

	@ParameterInfo(userDisplayName = "Alpha",
			description = "Scaling factor for detection window size used to suppress neighbors", floatValue = 0.5f,
			permissibleFloatRange = { 0, 2 })
	private float alpha;

	@ParameterInfo(userDisplayName = "Minimum confidence",
			description = "Cells whose detection confidence is lower than this minimum will be ignored",
			floatValue = 0f, permissibleFloatRange = { -10, 1 })
	private float minConfidence;

	@ParameterInfo(userDisplayName = "Minimal distance (µm)", aliases = "Optional minimal distance",
			description = "Minimal distance between cells (µm)", floatValue = 0f, permissibleFloatRange = { 0, 100 })
	private float minDistance;

	private static final byte SUPPRESSED = 1;
	private static final byte SELECTED = -1;
	private static final byte NOT_VISITED = 0;

	public float
			fillTree(List<ClickedPoint> points, List<ClickedPoint> pointsAboveThreshold, KDTree<ClickedPoint> tree) {

		float maxRadius = -Float.MAX_VALUE;

		try {
			for (ClickedPoint p : points) {
				if (p.getConfidence() > minConfidence) {
					p.status = NOT_VISITED;
					if (p.hsz * 0.5 * p.xyCalibration > maxRadius)
						maxRadius = p.hsz * 0.5f * p.xyCalibration;
					double[] key = new double[] { p.getx(), p.gety(), p.getz() };
					ClickedPoint samePoint = tree.search(key);
					boolean insert = true;
					if (samePoint != null) {
						if (samePoint.getConfidence() < p.getConfidence()) {
							try {
								tree.delete(key);
							} catch (KeyMissingException e) {
								throw new IllegalStateException(e);
							}
							pointsAboveThreshold.remove(samePoint);
						} else
							insert = false;
					}
					if (insert)
						try {
							pointsAboveThreshold.add(p);
							tree.insert(key, p);
						} catch (KeyDuplicateException e) {
							throw new IllegalStateException(e);
						}
				}
			}
		} catch (KeySizeException e) {
			throw new IllegalStateException(e);
		}
		return maxRadius;
	}

	@Override
	public void run(ProgressReporter progress, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		try {
			if (progress.isIndeterminate())
				progressSetIndeterminateThreadSafe(progress, false);
			indeterminateProgress = false;
			if (progress.getValue() != 0)
				progressSetValueThreadSafe(progress, 0);

			long time0 = System.currentTimeMillis();

			PluginIOCells detectedCells = (PluginIOCells) pluginInputs.get("Detected cells");
			PluginIOCells detectedCells2 = (PluginIOCells) pluginInputs.get("Detected cells 2");

			final KDTree<ClickedPoint> tree = new KDTree<>(3);

			float maxRadius = -Float.MAX_VALUE;

			final ArrayList<ClickedPoint> pointsAboveThreshold = new ArrayList<>(5000);

			maxRadius = fillTree(detectedCells.getPoints(), pointsAboveThreshold, tree);
			if (detectedCells2 != null) {
				float r2 = fillTree(detectedCells2.getPoints(), pointsAboveThreshold, tree);
				maxRadius = Math.max(maxRadius, r2);
				if (detectedCells.getPoints().get(0).listNamesOfQuantifiedProperties.indexOf("geodesicDistance") != detectedCells2
						.getPoints().get(0).listNamesOfQuantifiedProperties.indexOf("geodesicDistance")
						|| detectedCells.getPoints().get(0).listNamesOfQuantifiedProperties.size() != detectedCells2
								.getPoints().get(0).listNamesOfQuantifiedProperties.size())
					throw new IllegalArgumentException("Structure mismatch between detected cells 1 and 2");
			}
			Utils.log("Time creating KDTree: " + (System.currentTimeMillis() - time0), LogLevel.VERBOSE_DEBUG);
			time0 = System.currentTimeMillis();

			Collections.sort(pointsAboveThreshold);
			Utils.log("Time sorting: " + (System.currentTimeMillis() - time0), LogLevel.VERBOSE_DEBUG);
			time0 = System.currentTimeMillis();

			Calibration calib = detectedCells.getCalibration();
			final float zFactor = (float) (calib != null ? Math.pow(calib.pixelDepth / calib.pixelHeight, 2) : 1);

			if (zFactor == 1) {
				Utils.log("Warning: z factor is 1", LogLevel.WARNING);
			} else {
				if (detectedCells2 != null) {
					if (detectedCells2.getCalibration() == null) {
						Utils.log("Warning: detected cells 2 have no calibration", LogLevel.WARNING);
					} else {
						if (Math.abs(Math.pow(calib.pixelDepth / calib.pixelHeight, 2) - zFactor) > 0.01)
							throw new IllegalArgumentException("z factor is not the same for detected cells 1 and 2");
					}
				}
			}

			@SuppressWarnings("unused")
			final boolean geodesicDistanceComputed =
					pointsAboveThreshold.get(0).listNamesOfQuantifiedProperties.contains("geodesicDistance");

			int nPoints = pointsAboveThreshold.size();
			final double minDistanceSq = minDistance * minDistance;
			int index = 0;
			for (ClickedPoint p : pointsAboveThreshold) {
				progressSetValueThreadSafe(progress, (100 * index++) / nPoints);
				if (Thread.interrupted()) {
					throw new InterruptedException();
				}

				if (p.status == SUPPRESSED)
					continue;

				p.status = SELECTED;

				final float x = p.getx();
				final float y = p.gety();
				final float z = (float) p.getz();
				final double radius = p.hsz * 0.5 * p.xyCalibration;

				final double totalRadius = Math.max(alpha * (radius + maxRadius), 2 * minDistance);

				// TODO Is it worth parallelizing this loop?
				for (ClickedPoint neighbor : tree.range(new double[] { x - totalRadius, y - totalRadius,
						z - totalRadius }, new double[] { x + totalRadius, y + totalRadius, z + totalRadius })) {

					double neighborRadius = neighbor.hsz * 0.5 * p.xyCalibration;
					double distanceSq =
							(x - neighbor.getx()) * (x - neighbor.getx()) + (y - neighbor.gety())
									* (y - neighbor.gety()) + (z - neighbor.getz()) * (z - neighbor.getz());

					if (distanceSq < alpha * alpha * (radius + neighborRadius) * (radius + neighborRadius)
							|| distanceSq < minDistanceSq) {
						if (neighbor.status == NOT_VISITED)
							neighbor.status = SUPPRESSED;
					}
				}
			}

			Utils.log("Time iterating to suppress points: " + (System.currentTimeMillis() - time0),
					LogLevel.VERBOSE_DEBUG);
			time0 = System.currentTimeMillis();

			PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Seeds");
			outputCells.clear();

			outputCells.getQuantifiedPropertyNames().clear();
			outputCells.getQuantifiedPropertyNames().addAll(detectedCells.getQuantifiedPropertyNames());

			outputCells.getUserCellDescriptions().clear();
			outputCells.getUserCellDescriptions().addAll(detectedCells.getUserCellDescriptions());

			outputCells.setCalibration(detectedCells.getCalibration());

			for (ClickedPoint p : pointsAboveThreshold) {
				assert (p.getConfidence() > minConfidence);
				assert (p.status != NOT_VISITED);
				if (p.status == SUPPRESSED)
					continue;
				ClickedPoint pCloned = (ClickedPoint) p.clone();
				pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
				pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();
				outputCells.addDontFireValueChanged(pCloned);
			}

			// Double check distances for debugging purposes
			Utils.log("Low value squared is " + minDistanceSq, LogLevel.DEBUG);

			Utils.log("Time cloning result points: " + (System.currentTimeMillis() - time0), LogLevel.VERBOSE_DEBUG);
			time0 = System.currentTimeMillis();

			outputCells.fireValueChanged(false, false);

		} catch (KeySizeException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String operationName() {
		return "SVMSuppress";
	}

	@Override
	public int getFlags() {
		return PLUGIN_CREATES_OUTPUT_ITSELF;
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, false, false);
		desc0.name = "Detected cells";
		result.put("Detected cells", desc0);
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
	public String[] getInputLabels() {
		return new String[] { "Detected cells", "Detected cells 2" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds" };
	}

}
