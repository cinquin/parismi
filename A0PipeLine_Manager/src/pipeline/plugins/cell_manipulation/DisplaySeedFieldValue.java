/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import ij.measure.Calibration;

import java.awt.event.ActionEvent;
import java.util.Map;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import pipeline.PreviewType;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.ParameterListener;

public class DisplaySeedFieldValue extends DisplayEmbeddedImages {

	@Override
	public String getToolTip() {
		return "From a set of cells create an image with a seed at the center of each cell, "
				+ "colored following a user-chosen field.";
	}

	@Override
	public String operationName() {
		return "Display field value at seed center";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		if (getImageInput() != null) {
			return getImageInput().getDimensions().depth;
		}

		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		int depth = seeds.getDepth();
		if (depth == 0) {
			for (ClickedPoint p : seeds) {
				if (p.z > depth)
					depth = (int) p.z;
			}
			depth++;
		}
		return depth;
	}

	// FIXME The following copied from ThresholdCells. Should find a way to merge the code.

	@SuppressWarnings("unused")
	private boolean rangeSet;

	float minThreshold;
	float maxThreshold;

	private class RangeListener implements ParameterListener {
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
		public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		}

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			float[] values = (float[]) rangeParameter.getValue();
			float lowValue = values[0];
			float highValue = values[1];
			float lowLimit = values[2];
			float highLimit = values[3];

			switch (commandName) {
				case "Reset Min":
					lowLimit = (float) Utils.enhanceMin(getMin());
					break;
				case "Reset Max":
					highLimit = (float) Utils.enhanceMax(getMax());
					break;
				case "Reset Range":
					lowLimit = (float) Utils.enhanceMin(getMin());
					highLimit = (float) Utils.enhanceMax(getMax());
					break;
				default:
					throw new IllegalStateException("Unknown command name " + commandName);
			}
			if (lowValue < lowLimit)
				lowValue = lowLimit;
			if (lowValue > highLimit)
				lowValue = highLimit;
			if (highValue < lowLimit)
				highValue = lowLimit;
			if (highValue > highLimit)
				highValue = highLimit;

			float[] objAray = { lowValue, highValue, lowLimit, highLimit };
			rangeParameter.setValue(objAray);
			rangeParameter.fireValueChanged(false, true, true);
			Utils.log("Reset range to " + lowLimit + "--" + highLimit, LogLevel.DEBUG);

		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			rangeSet = true;
			float oldMin = minThreshold;
			float oldMax = maxThreshold;
			minThreshold = ((float[]) rangeParameter.getValue())[0];
			maxThreshold = ((float[]) rangeParameter.getValue())[1];
			if ((pipelineCallback != null) && ((minThreshold != oldMin) || (maxThreshold != oldMax)))
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private String fieldName = "";

	private class FieldListener implements ParameterListener {
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
			if (!(fieldNameParam.getValue().equals(fieldName))) {
				fieldName = fieldNameParam.toString();
				rangeSet = false;
				updateHistogram();
				updateRange(true);
			}
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private ParameterListener fieldListener0 = new FieldListener();
	private ParameterListener fieldListener1 = new ParameterListenerWeakRef(fieldListener0);

	private ParameterListener rangeListener0 = new RangeListener();
	private ParameterListener rangeListener1 = new ParameterListenerWeakRef(rangeListener0);

	private AbstractParameter rangeParameter = new FloatRangeParameter("Range to keep",
			"Anything within this range will be set to 255, anything out to 0.", 0, 50000, 0, 50000, true, true,
			rangeListener1, null);
	ComboBoxParameterPrintValueAsString fieldNameParam = new ComboBoxParameterPrintValueAsString("Field to use", "",
			new String[] {}, fieldName, true, fieldListener1);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { rangeListener1, fieldListener1 };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { rangeParameter, fieldNameParam };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		rangeParameter = param[0];
		fieldNameParam = (ComboBoxParameterPrintValueAsString) param[1];
		minThreshold = ((float[]) rangeParameter.getValue())[0];
		maxThreshold = ((float[]) rangeParameter.getValue())[1];
		fieldName = fieldNameParam.getStringValue();
	}

	private float getMin() {
		IPluginIOList<?> cells = (IPluginIOList<?>) pluginInputs.get("Seeds");
		BeanTableModel<?> tableModel = cells.getBeanTableModel();

		int columnIndex = Utils.indexOf(getFieldNames(tableModel), fieldNameParam.getSelection());

		float min = Float.MAX_VALUE;
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			float f = tableModel.getFloatValueAt(i, columnIndex);
			if (f < min)
				min = f;
		}
		return min;
	}

	private float getMax() {
		IPluginIOList<?> cells = (IPluginIOList<?>) pluginInputs.get("Seeds");
		BeanTableModel<?> tableModel = cells.getBeanTableModel();

		int columnIndex = Utils.indexOf(getFieldNames(tableModel), fieldNameParam.getSelection());

		float max = -Float.MAX_VALUE;
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			float f = tableModel.getFloatValueAt(i, columnIndex);
			if (f > max)
				max = f;
		}
		return max;
	}

	private void updateHistogram() {
		IPluginIOList<?> cells = (IPluginIOList<?>) pluginInputs.get("Seeds");
		if (cells == null)
			return;
		BeanTableModel<?> tableModel = cells.getBeanTableModel();

		int columnIndex = Utils.indexOf(getFieldNames(tableModel), fieldNameParam.getSelection());

		double[] valuesForHistogram = new double[tableModel.getRowCount()];
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			valuesForHistogram[i] = tableModel.getFloatValueAt(i, columnIndex);
		}

		HistogramDataset dataset = new HistogramDataset();
		dataset.setType(HistogramType.RELATIVE_FREQUENCY);
		dataset.addSeries("Histogram", valuesForHistogram, 15);

		((FloatRangeParameter) rangeParameter).histogram = dataset;
	}

	private void updateRange(boolean updateSelection) {
		if (fieldNameParam.getSelection() == null)
			return;
		float min = (float) Utils.enhanceMin(getMin());
		float max = (float) Utils.enhanceMax(getMax());
		float[] values = (float[]) rangeParameter.getValue();

		if (updateSelection) {
			values[0] = min;
			values[1] = max;
		}
		values[2] = min;
		values[3] = max;
		rangeParameter.setValue(values);

		rangeParameter.fireValueChanged(false, true, updateSelection);
	}

	static String[] getFieldNames(BeanTableModel<?> tableModel) {
		String[] fieldNames = new String[tableModel.getColumnCount()];

		for (int i = 0; i < fieldNames.length; i++) {
			fieldNames[i] = tableModel.getColumnName(i);
		}

		return fieldNames;
	}

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

		String currentChoiceName = fieldNameParam.getSelection();
		fieldNameParam.setChoices(fieldNames);
		fieldNameParam.setSelectionIndex(Utils.indexOf(fieldNames, currentChoiceName));
		if ((fieldNames.length > 0) && fieldNameParam.getSelectionIndex() == -1) {
			fieldNameParam.setSelectionIndex(0);
			fieldNameParam.fireValueChanged(false, true, false);
		}
		try {
			updateHistogram();
			updateRange(false);
		} catch (Exception e) {
			Utils.printStack(e);
		}
	}

	@Override
	public void runChannel(IPluginIOStack input, IPluginIOStack output, ProgressReporter r, PreviewType previewType,
			boolean inputHasChanged) {

		if (fieldNameParam.getSelection() == null)
			throw new IllegalStateException("No field selection has been made");

		IPluginIOImage output2 = (IPluginIOImage) getOutput();

		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		output2.setCalibration(seeds.getCalibration() == null ? null : (Calibration) seeds.getCalibration().clone());

		String propName = fieldNameParam.getSelection();

		int xyRad = 3;
		int zRad = 1;

		for (ClickedPoint p : seeds.getPoints()) {
			float f = p.getQuantifiedProperty(propName);
			for (int x = (int) Math.max(0, p.x - xyRad); x <= Math.min(output2.getDimensions().width - 1, p.x + xyRad); x++)
				for (int y = (int) Math.max(0, p.y - xyRad); y <= Math.min(output2.getDimensions().height - 1, p.y
						+ xyRad); y++)
					for (int z = (int) Math.max(0, p.z - zRad); z <= Math.min(output2.getDimensions().depth - 1, p.z
							+ zRad); z++)
						output.setPixelValue(x, y, z, f);
		}

	}

}
