/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOListMemberQ;
import pipeline.data.IQuantifiable;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

public class CellsSlidingWindowAverage extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "From a set of cells, compute a sliding average of values in user-chosen field y"
				+ " over the axis defined by user-chosen field x.";
	}

	@SuppressWarnings("unused")
	private boolean rangeSet;

	private String xFieldName = "";
	private String yFieldName = "";
	private float windowLength = 1;

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
			if (!(xFieldNameParam.getValue().equals(xFieldName))) {
				xFieldName = xFieldNameParam.toString();
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

	private class YFieldListener implements ParameterListener {
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
			if (!(yFieldNameParam.getValue().equals(yFieldName))) {
				yFieldName = yFieldNameParam.toString();
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private ParameterListener yFieldListener0 = new YFieldListener();
	private ParameterListener yFieldListener1 = new ParameterListenerWeakRef(yFieldListener0);

	private class WindowLengthListener implements ParameterListener {
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
			if (windowParameter.getFloatValue() != windowLength) {
				windowLength = windowParameter.getFloatValue();
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}

		@Override
		public boolean alwaysNotify() {
			return false;
		}
	}

	private ParameterListener windowLengthListener0 = new WindowLengthListener();
	private ParameterListener windowLengthListener1 = new ParameterListenerWeakRef(windowLengthListener0);

	private FloatParameter windowParameter = new FloatParameter("Length of sliding x window", "", windowLength, 0f,
			100f, true, true, true, windowLengthListener1);
	private ComboBoxParameterPrintValueAsString xFieldNameParam = new ComboBoxParameterPrintValueAsString(
			"Field to use as x", "", new String[] {}, xFieldName, true, xFieldListener1);
	private ComboBoxParameterPrintValueAsString yFieldNameParam = new ComboBoxParameterPrintValueAsString(
			"Field to locally average", "", new String[] {}, yFieldName, true, yFieldListener1);

	private SplitParameter fieldNames = new SplitParameter(new Object[] { xFieldNameParam, yFieldNameParam });

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { null, windowLengthListener1 };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { fieldNames, windowParameter };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		fieldNames = (SplitParameter) param[0];
		xFieldNameParam = (ComboBoxParameterPrintValueAsString) fieldNames.getParameterValue()[0];
		yFieldNameParam = (ComboBoxParameterPrintValueAsString) fieldNames.getParameterValue()[1];
		windowParameter = (FloatParameter) param[1];
		windowLength = windowParameter.getFloatValue();
		xFieldName = xFieldNameParam.getStringValue();
		yFieldName = yFieldNameParam.getStringValue();
	}

	protected static String[] getFieldNames(BeanTableModel<?> tableModel) {
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

		String currentXChoiceName = xFieldNameParam.getSelection();
		String currentYChoiceName = yFieldNameParam.getSelection();
		xFieldNameParam.setChoices(fieldNames);
		yFieldNameParam.setChoices(fieldNames);
		xFieldNameParam.setSelectionIndex(Utils.indexOf(fieldNames, currentXChoiceName));
		yFieldNameParam.setSelectionIndex(Utils.indexOf(fieldNames, currentYChoiceName));
		if ((fieldNames.length > 0) && xFieldNameParam.getSelectionIndex() == -1) {
			xFieldNameParam.setSelectionIndex(0);
			xFieldNameParam.fireValueChanged(false, true, false);
		}
		if ((fieldNames.length > 0) && yFieldNameParam.getSelectionIndex() == -1) {
			yFieldNameParam.setSelectionIndex(0);
			yFieldNameParam.fireValueChanged(false, true, false);
		}
	}

	@Override
	public String operationName() {
		return "Sliding window averaging";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY;
	}

	protected float minThreshold;
	protected float maxThreshold;

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

	private static float getCPField(ClickedPoint p, Field f, String name) {
		if (f != null)
			try {
				return f.getFloat(p);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		return p.getQuantifiedProperty(name);
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		@SuppressWarnings("unchecked")
		IPluginIOList<ClickedPoint> cells = (IPluginIOList<ClickedPoint>) pluginInputs.get("Seeds");
		@SuppressWarnings("unchecked")
		IPluginIOList<? extends IQuantifiable> outputCells =
				(IPluginIOList<? extends IQuantifiable>) pluginOutputs.get("Cells");
		outputCells.clear();
		cells.copyInto(outputCells);

		String outputFieldName = "LA_" + yFieldName;
		((PluginIOCells) outputCells).addQuantifiedPropertyName(outputFieldName);

		if (xFieldNameParam.getSelection() == null)
			throw new IllegalStateException("No x field selection has been made");
		if (yFieldNameParam.getSelection() == null)
			throw new IllegalStateException("No y field selection has been made");

		final int xColumnIndex =
				Utils.indexOf(cells.get(0).getQuantifiedPropertyNames().toArray(new String[] {}), xFieldNameParam
						.getSelection());

		Field f = null;

		if (xColumnIndex == -1) {
			String nameForXSeries = xFieldNameParam.getSelection();
			try {
				try {
					f = ClickedPoint.class.getDeclaredField(nameForXSeries);
				} catch (NoSuchFieldException e) {
					String alternativeName = nameForXSeries.substring(0, 1).toLowerCase() + nameForXSeries.substring(1);
					f = ClickedPoint.class.getDeclaredField(alternativeName);
				}
				f.setAccessible(true);
			} catch (Exception e) {
				throw new IllegalStateException("Could not find field " + xFieldNameParam.getSelection(), e);
			}
		}

		final Field f2 = f;

		Comparator<ClickedPoint> xComparator =
				(o1, o2) -> Float.compare(getCPField(o1, f2, xFieldName), getCPField(o2, f2, xFieldName));

		List<ClickedPoint> sortedList = new ArrayList<>();
		sortedList.addAll(cells);
		Collections.sort(sortedList, xComparator);

		int x_behind = 0;
		int x_forward = 0;

		for (ClickedPoint p : sortedList) {
			float currentX = getCPField(p, f, xFieldName);
			while (currentX - getCPField(sortedList.get(x_behind), f, xFieldName) > windowLength * 0.5)
				x_behind++;

			while ((getCPField(sortedList.get(x_forward), f, xFieldName) - currentX < windowLength * 0.5)
					&& (x_forward < sortedList.size() - 2))
				x_forward++;

			double averagedY = 0;
			int counter = 0;

			for (int x = x_behind; x <= x_forward; x++) {
				averagedY += sortedList.get(x).getQuantifiedProperty(yFieldName);
				counter++;
			}
			averagedY /= counter;

			@SuppressWarnings("unchecked")
			IPluginIOListMemberQ<ClickedPoint> pCloned = (IPluginIOListMemberQ<ClickedPoint>) p.clone();
			pCloned.linkToList(outputCells);

			pCloned.setQuantifiedProperty(outputFieldName, (float) averagedY);

			outputCells.addDontFireValueChanged(pCloned);
		}

		outputCells.fireValueChanged(false, false);
	}

}
