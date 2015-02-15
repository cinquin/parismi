/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

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
import pipeline.data.IPluginIOListMember;
import pipeline.data.IPluginIOListMemberQ;
import pipeline.data.InputOutputDescription;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

/**
 * Sample cells along user-defined axis.
 *
 */
public class SparsifyCells extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "From a set of cells sample a subset along user-defined axis";
	}

	private String fieldName = "";

	private class FieldListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (stillChanging)
				return;
			if (!(fieldNameParam.getValue().equals(fieldName))) {
				fieldName = fieldNameParam.toString();
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener fieldListener0 = new FieldListener();
	private ParameterListener fieldListener1 = new ParameterListenerWeakRef(fieldListener0);

	private ComboBoxParameterPrintValueAsString fieldNameParam = new ComboBoxParameterPrintValueAsString("Use field",
			"", new String[] {}, fieldName, true, fieldListener1);

	@ParameterInfo(userDisplayName = "Minimum interval", floatValue = 5f, permissibleFloatRange = { 0f, 50f })
	private float interval;

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { getListener("interval").get(0), fieldListener1 };
	}

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { getParameter("interval"), fieldNameParam };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		super.setParameters(param);
		fieldNameParam = (ComboBoxParameterPrintValueAsString) param[1];
		fieldName = fieldNameParam.getStringValue();
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

		String currentChoiceName = fieldName;
		fieldNameParam.setChoices(fieldNames);
		fieldNameParam.setSelectionIndex(Utils.indexOf(fieldNames, currentChoiceName));
		if ((fieldNames.length > 0) && fieldNameParam.getSelectionIndex() == -1) {
			fieldNameParam.setSelectionIndex(0);
			fieldNameParam.fireValueChanged(false, true, false);
		}
	}

	@Override
	public String operationName() {
		return "Sparsify cells";
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

		interval = (Float) getParameter("interval").getSimpleValue();

		IPluginIOList<?> cells = (IPluginIOList<?>) pluginInputs.get("Seeds");

		final String fieldName = fieldNameParam.getSelection();
		if (fieldName == null)
			throw new IllegalStateException("No field selection has been made");

		Field f = null;
		try {
			try {
				f = ClickedPoint.class.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
				String alternativeName = fieldName.substring(0, 1).toLowerCase() + fieldName.substring(1);
				f = ClickedPoint.class.getDeclaredField(alternativeName);
			}
			f.setAccessible(true);
		} catch (Exception e) {
			throw new IllegalStateException("Could not find field " + fieldName, e);
		}

		final Field f2 = f;

		Comparator<ClickedPoint> fieldComparator =
				(o1, o2) -> Float.compare(getCPField(o1, f2, fieldName), getCPField(o2, f2, fieldName));

		List<ClickedPoint> sortedList = new ArrayList<>();
		for (IPluginIOListMember<?> cell : cells) {
			sortedList.add((ClickedPoint) cell);
		}
		// List<ClickedPoint> sortedList = cells.stream().map(p -> (ClickedPoint) p).collect(Collectors.toList());
		Collections.sort(sortedList, fieldComparator);

		IPluginIOList<?> outputCells = (IPluginIOList<?>) pluginOutputs.get("Cells");
		cells.copyInto(outputCells);
		outputCells.clear();

		float lastValue = -Float.MAX_VALUE;
		for (ClickedPoint p : sortedList) {
			float current = getCPField(p, f, fieldName);
			if (current - lastValue > interval) {
				lastValue = current;
			} else
				continue;

			@SuppressWarnings("unchecked")
			IPluginIOListMemberQ<ClickedPoint> pCloned = (IPluginIOListMemberQ<ClickedPoint>) p.clone();
			pCloned.linkToList(outputCells);

			outputCells.addDontFireValueChanged(pCloned);
		}

		outputCells.fireValueChanged(false, false);
	}

}
