/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOListMember;
import pipeline.data.InputOutputDescription;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

public class ThresholdCells extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Select subset of cells by selecting subrange of user-defined field";
	}

	@SuppressWarnings("unused")
	private boolean rangeSet;

	private class RangeListener extends ParameterListenerAdapter {
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
	}

	String fieldName = "";

	private class FieldListener extends ParameterListenerAdapter {

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
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener fieldListener0 = new FieldListener();
	private ParameterListener fieldListener1 = new ParameterListenerWeakRef(fieldListener0);

	private ParameterListener rangeListener0 = new RangeListener();
	private ParameterListener rangeListener1 = new ParameterListenerWeakRef(rangeListener0);

	AbstractParameter rangeParameter = new FloatRangeParameter("Range to keep",
			"Anything within this range will be set to 255, anything out to 0.", 0, 50000, 0, 50000, true, true,
			rangeListener1, null);
	ComboBoxParameterPrintValueAsString fieldNameParam = new ComboBoxParameterPrintValueAsString("Threshold on", "",
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

		rangeParameter.fireValueChanged(false, true, true);
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
		updateHistogram();
		updateRange(false);
	}

	@Override
	public String operationName() {
		return "Threshold cells based on content of arbitrary field";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY;
	}

	float minThreshold;
	float maxThreshold;

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, false, false);
		desc0.name = "Seeds";
		result.put("Seeds", desc0);
		return result;
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
		Utils.log("Creating seeds", LogLevel.DEBUG);
		initializeOutputs();
		IPluginIOList<?> out = (IPluginIOList<?>) getInput("Seeds").duplicateStructure();
		PluginIOView view = out.createView();
		// PluginIOView view=new ListOfPointsView(seeds);
		view.setData(out);
		pluginOutputs.put("Cells", out);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Cells";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Cells", desc0);
		return result;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Cells" };
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		IPluginIOList<?> cells = (IPluginIOList<?>) pluginInputs.get("Seeds");
		BeanTableModel<?> tableModel = cells.getBeanTableModel();// new
																	// BeanTableModel<ClickedPoint>(cells.getElementClass(),(List
																	// <?>) cells);
		IPluginIOList<?> outputCells = (IPluginIOList<?>) pluginOutputs.get("Cells");
		cells.copyInto(outputCells);
		outputCells.clear();

		float[] values = (float[]) rangeParameter.getValue();
		float lowValue = values[0];
		float highValue = values[1];

		if (fieldNameParam.getSelection() == null)
			throw new IllegalStateException("No field selection has been made");

		int columnIndex = Utils.indexOf(getFieldNames(tableModel), fieldNameParam.getSelection());

		for (int i = 0; i < tableModel.getRowCount(); i++) {
			float f = tableModel.getFloatValueAt(i, columnIndex);
			if ((f >= lowValue) && (f <= highValue)) {
				IPluginIOListMember<?> pCloned = (IPluginIOListMember<?>) cells.get(i).clone();
				pCloned.linkToList(outputCells);
				outputCells.addDontFireValueChanged(pCloned);

			}
		}

		outputCells.fireValueChanged(false, false);
	}

}
