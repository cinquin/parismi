/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.principal_curves;

import ij.measure.Calibration;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOPrincipalCurve;
import pipeline.misc_util.IntrospectionParameters;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import processing_utilities.pcurves.LinearAlgebra.LineSegment;
import processing_utilities.pcurves.LinearAlgebra.Sample;
import processing_utilities.pcurves.LinearAlgebra.SampleDD;
import processing_utilities.pcurves.LinearAlgebra.Vektor;
import processing_utilities.pcurves.LinearAlgebra.VektorDD;
import processing_utilities.pcurves.PrincipalCurve.PrincipalCurveAlgorithm;
import processing_utilities.pcurves.PrincipalCurve.PrincipalCurveClass;
import processing_utilities.pcurves.PrincipalCurve.PrincipalCurveParameters;
import processing_utilities.pcurves.PrincipalCurve.SetOfCurves;

/**
 * Starts from a set of points, computes a principal curve, and assigns each point a distance along that principal
 * curve. That
 * distance is stored in quantified property "geodesicDistance" in the corresponding ClickedPoint objects.
 * {@link processing_utilities.pcurves.PrincipalCurve.BackboneFinder} can be called directly from the pipeline
 * stand-alone jar.
 */
public class GeodesicDistanceByPrincipalCurve extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public void getInputs(Map<String, IPluginIO> inputs) {
		super.getInputs(inputs);
		IPluginIOList<?> cells = (IPluginIOList<?>) pluginInputs.get("Seeds");

		if (cells == null)
			return;

		BeanTableModel<?> tableModel = cells.getBeanTableModel();

		String[] fieldNames = new String[tableModel.getColumnCount()];

		for (int i = 0; i < fieldNames.length; i++) {
			fieldNames[i] = tableModel.getColumnName(i);
		}

		String currentXChoiceName = backboneLabelParam.getSelection();
		backboneLabelParam.setChoices(fieldNames);
		backboneLabelParam.setSelectionIndex(Utils.indexOf(fieldNames, currentXChoiceName));

	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");

		if (inputCells.getCalibration() == null) {
			throw new PluginRuntimeException("Geodesic distance plugin needs input cells to be calibrated", true);
		}

		String labelName = backboneLabelParam.getSelection();
		if (labelName != null) {

		}

		PluginIOCells outputCells = (PluginIOCells) inputCells.clone();
		initializeOutputs();

		pluginOutputs.put("Seeds", outputCells);
		outputCells.addQuantifiedPropertyName("geodesicDistance");
		outputCells.addQuantifiedPropertyName("orthogonalDistance");

		// Load seed coordinates
		SampleDD sample2 = new SampleDD();
		double[] seedCoord = new double[3];
		for (ClickedPoint p : outputCells.getPoints()) {
			float x = p.getx();
			float y = p.gety();
			float z = (float) p.getz();
			boolean useAsBackbone = labelName == null || p.getQuantifiedProperty(labelName) > 0;
			if (useAsBackbone) {
				seedCoord[0] = x;
				seedCoord[1] = y;
				seedCoord[2] = z;
				VektorDD tempVektor = new VektorDD(seedCoord);
				sample2.AddPoint(tempVektor);
			}
		}

		// Run principal curves
		PrincipalCurveParameters parameters = new PrincipalCurveParameters();// Provides a default set of parameters

		parameters.penaltyCoefficient = penaltyCoefficient;
		parameters.relativeLengthPenaltyCoefficient = relativeLengthPenaltyCoefficient;
		parameters.terminatingConditionCoefficient = terminatingConditionCoefficient;
		parameters.terminatingConditionMaxLength = terminatingConditionMaxLength;
		parameters.relativeChangeInCriterionThreshold = relativeChangeInCriterionThreshold;

		Utils.log("Parameters: " + penaltyCoefficient + ", " + relativeLengthPenaltyCoefficient + ", "
				+ terminatingConditionCoefficient + ", " + terminatingConditionMaxLength + ", "
				+ relativeChangeInCriterionThreshold + " " + randomSeed + "Flip distal end:" + flipDistalEnd,
				LogLevel.DEBUG);

		PrincipalCurveClass principalCurve = new PrincipalCurveClass(sample2, parameters);
		PrincipalCurveAlgorithm algorithmThread = new PrincipalCurveAlgorithm(principalCurve, parameters);
		algorithmThread.start(randomSeed);

		// output principal curve coordinates (i.e., the backbone) // TODO: Display backbone. Need some way to visualize
		// 3D line (z-projection or volume rendering)
		SetOfCurves savePrincipalCurve = principalCurve.ConvertToCurves();
		for (int i = 0; i < savePrincipalCurve.GetNumOfCurves(); i++) {
			for (int j = 0; j < savePrincipalCurve.GetCurveAt(i).getSize(); j++) {
				// get control points of backbone
				/*
				 * Vektor temp = savePrincipalCurve.GetCurveAt(i).GetPointAt(j);
				 * double y = temp.GetCoords(0);
				 * double x = temp.GetCoords(1);
				 * double z = temp.GetCoords(2);
				 */

				// line segments composing backbone
				if (j < savePrincipalCurve.GetCurveAt(i).GetNumOfLineSegments()) {
					@SuppressWarnings("unused")
					LineSegment line = savePrincipalCurve.GetCurveAt(i).GetLineSegmentAt(j);
					// line.GetPointAtParameter(parameter);
				}

				// Debugging output
				// String tempString = "x=" + x + ", y=" +y+ ", z="+z;
				// Utils.log(tempString,LogLevel.DEBUG);
			}
		}

		// output seed distances along the backbone
		Sample tempSample = principalCurve.GetProjectionIndices();
		double[] orthogonalDistances = new double[outputCells.size()];
		double[] geodesicDistances = new double[outputCells.size()];

		float geodesicMax = 0;

		int index = 0;
		principalCurve.initializeForGeodesicDistance();

		int nCellsFirst20 = 0;
		for (ClickedPoint p : outputCells) {
			Vektor projectionCoord =
					principalCurve.getGeodesicDistances(new VektorDD(new double[] { p.getx(), p.gety(), p.getz() }));
			float geodesicDistance = (float) projectionCoord.GetCoords(0);
			p.setQuantifiedProperty("geodesicDistance", geodesicDistance);
			if (geodesicDistance > geodesicMax)
				geodesicMax = geodesicDistance;
			if (geodesicDistance < 20)
				nCellsFirst20++;
			double orthogonalDistance = projectionCoord.GetCoords(1);
			orthogonalDistances[index] = orthogonalDistance;
			index++;
			p.setQuantifiedProperty("orthogonalDistance", (float) orthogonalDistance);

		}

		if (tryToFindDistalEnd) {
			int nBins = 15;

			HistogramDataset dataset = new HistogramDataset();
			dataset.setType(HistogramType.RELATIVE_FREQUENCY);
			dataset.addSeries("Histogram", orthogonalDistances, nBins);

			float[] histogram = new float[nBins];
			for (int i = 0; i < nBins; i++) {
				histogram[i] = dataset.getY(0, i).floatValue();
			}
			int peakIndex = findPeakIndex(histogram);
			if (peakIndex == -1)
				Utils.log("Could not find peak in histogram of orthogonal distances", LogLevel.ERROR);
			else {
				float orthogonalThreshold = dataset.getX(0, findPeakIndex(histogram)).floatValue();

				float[] orthogonalThresholded = new float[tempSample.getSize()];
				float[] geodesicThresholded = new float[tempSample.getSize()];

				int nAfterThresholding = 0;
				for (int i = 0; i < tempSample.getSize(); i++) {
					if (orthogonalDistances[i] <= orthogonalThreshold) {
						orthogonalThresholded[nAfterThresholding] = (float) orthogonalDistances[i];
						geodesicThresholded[nAfterThresholding] = (float) geodesicDistances[i];
						nAfterThresholding++;
					}
				}

				float slope =
						(float) ((sumProduct(geodesicThresholded, orthogonalThresholded, nAfterThresholding) - (sum(
								geodesicThresholded, nAfterThresholding) * sum(orthogonalThresholded,
								nAfterThresholding))
								/ nAfterThresholding) / (sumSquared(geodesicThresholded, nAfterThresholding) - Math
								.pow(sum(geodesicThresholded, nAfterThresholding), 0.5)
								/ nAfterThresholding));

				boolean doFlip = (slope < 0) || (nCellsFirst20 < 20);
				if (flipDistalEnd)
					doFlip = !doFlip;
				if (doFlip) {
					if (slope < 0)
						Utils.log("Reversing geodesic distance because of negative slope", LogLevel.INFO);
					if (nCellsFirst20 < 20)
						Utils.log("Reversing geodesic distance because <20 cells in first 20 microns", LogLevel.INFO);
					for (int i = 0; i < tempSample.getSize(); i++) {
						ClickedPoint p = outputCells.getPoints().get(i);
						p.setQuantifiedProperty("geodesicDistance", geodesicMax
								- p.getQuantifiedProperty("geodesicDistance"));
					}
				}
			}
		}
		PluginIOPrincipalCurve pCurve = ((PluginIOPrincipalCurve) pluginOutputs.get("pCurve"));
		if ((pCurve) == null) {
			pCurve = new PluginIOPrincipalCurve(principalCurve);
			pluginOutputs.put("pCurve", pCurve);
		}
		pCurve.setPCurve(principalCurve);
		pCurve.setWidth(inputCells.getWidth());
		pCurve.setHeight(inputCells.getHeight());
		pCurve.setDepth(inputCells.getDepth());
		pCurve.setCalibration((Calibration) inputCells.getCalibration().clone());

		if (pointView != null)
			pointView.setData(outputCells);
	}

	private static float sum(float[] array, int size) {
		float result = 0;
		for (int i = 0; i < size; i++) {
			result += array[i];
		}
		return result;
	}

	private static float sumSquared(float[] array, int size) {
		float result = 0;
		for (int i = 0; i < size; i++) {
			result += array[i] * array[i];
		}
		return result;
	}

	private static float sumProduct(float[] array1, float[] array2, int size) {
		float result = 0;
		for (int i = 0; i < size; i++) {
			result += array1[i] * array2[i];
		}
		return result;
	}

	public static float findPeak(float[] array) {
		float baseline = array[0];
		float result = 0;
		float sumAbs = 0;
		for (int i = 1; i < array.length; i++) {
			sumAbs += Math.abs(array[i] - array[i - 1]);
			if (sumAbs > 1.5 * Math.abs(array[i] - baseline)) {
				result = array[i];
				break;
			}
		}
		return result;
	}

	private static int findPeakIndex(float[] array) {
		float baseline = array[0];
		int result = -1;
		float sumAbs = 0;
		for (int i = 1; i < array.length; i++) {
			sumAbs += Math.abs(array[i] - array[i - 1]);
			if (sumAbs > 1.5 * Math.abs(array[i] - baseline)) {
				result = i;
				break;
			}
		}
		return result;
	}

	private ListOfPointsView<ClickedPoint> pointView;

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
		pluginOutputs.put("pCurve", new PluginIOPrincipalCurve(null));
		ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(null);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		pointView = view;
		return views;
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	private class XFieldListener implements ParameterListener {
		private String parameterName;

		@Override
		public String getParameterName() {
			return parameterName;
		}

		@Override
		public void setParameterName(String name) {
			parameterName = name;
		}

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		}

		@Override
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (stillChanging)
				return;
			if (!(backboneLabelParam.getValue().equals(backboneLabel))) {
				backboneLabel = backboneLabelParam.toString();
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private ParameterListener xFieldListener0 = new XFieldListener();
	private ParameterListener xFieldListener1 = new ParameterListenerWeakRef(xFieldListener0);
	private String backboneLabel = "";

	private ComboBoxParameterPrintValueAsString backboneLabelParam =
			new ComboBoxParameterPrintValueAsString(
					"Backbone label",
					"If a valid field name is selected, only cells for which the value of the field is >0 will be considered to define the backbone; all cells will still receive a geodesic distance",
					new String[] {}, backboneLabel, true, xFieldListener1);

	@ParameterInfo(userDisplayName = "Penalty coefficient", floatValue = 0.09f, permissibleFloatRange = { 0f, 1f })
	private float penaltyCoefficient;

	@ParameterInfo(userDisplayName = "Relative length penalty", floatValue = 0.01f, permissibleFloatRange = { 0f, 1f })
	private float relativeLengthPenaltyCoefficient;

	@ParameterInfo(userDisplayName = "Terminating condition", floatValue = 0.5f, permissibleFloatRange = { 0f, 1f })
	private float terminatingConditionCoefficient;

	@ParameterInfo(userDisplayName = "Term. cond. max length", floatValue = 47.16f,
			permissibleFloatRange = { 0f, 100f })
	private float terminatingConditionMaxLength;

	@ParameterInfo(userDisplayName = "Relative change in threshold", floatValue = 0.003f, permissibleFloatRange = { 0f,
			0.01f })
	private float relativeChangeInCriterionThreshold;

	@ParameterInfo(userDisplayName = "Try to find distal end", stringValue = "TRUE", noErrorIfMissingOnReload = true)
	private boolean tryToFindDistalEnd;

	@ParameterInfo(userDisplayName = "Random seed", floatValue = 0, noErrorIfMissingOnReload = true)
	private int randomSeed;

	@ParameterInfo(userDisplayName = "Flip distal end", stringValue = "FALSE", noErrorIfMissingOnReload = true)
	private boolean flipDistalEnd;

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { xFieldListener1, getParameterListenersAsSplit() };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { backboneLabelParam, getParametersAsSplit() };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		if (param.length == 0 || param[0] == null)
			return;
		backboneLabelParam = (ComboBoxParameterPrintValueAsString) param[0];
		backboneLabel = backboneLabelParam.getStringValue();

		AbstractParameter[] otherParams = Arrays.copyOfRange(param, 1, param.length);
		IntrospectionParameters.setParameters(this, otherParams);

	}

	@Override
	public String operationName() {
		return "Geodesic distance by principal curve";
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
		return new String[] { "Seeds", "pCurve" };
	}

}
