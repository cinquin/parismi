/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import pipeline.PreviewType;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOListMemberQ;
import pipeline.data.IPluginIOListOfQ;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;

public class ThresholdToLabelCells extends ThresholdCells {

	@Override
	public String getToolTip() {
		return "Label cells that fall within a subrange of user-defined field";
	}

	@Override
	public String operationName() {
		return "Threshold to label";
	}

	@Override
	public String version() {
		return "1.0";
	}

	private TextParameter quantifiedFieldNameParam = new TextParameter("Store field", "Name of output field", "color",
			true, null, null);
	private FloatParameter colorParam = new FloatParameter("Color", "Value to store in output field", 65, 1, 255, true,
			true, true, null);
	private BooleanParameter othersAsNaN = new BooleanParameter("Others as NaN",
			"Fill in NaN for cells outside of range", false, false, null);

	private SplitParameter split = new SplitParameter(new Object[] { fieldNameParam, quantifiedFieldNameParam,
			colorParam, othersAsNaN });

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { rangeParameter, split };
		return paramArray;
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		rangeParameter = param[0];
		split = (SplitParameter) param[1];
		fieldNameParam = (ComboBoxParameterPrintValueAsString) ((Object[]) split.getValue())[0];
		minThreshold = ((float[]) rangeParameter.getValue())[0];
		maxThreshold = ((float[]) rangeParameter.getValue())[1];
		fieldName = fieldNameParam.getStringValue();

		quantifiedFieldNameParam = (TextParameter) ((Object[]) split.getValue())[1];
		colorParam = (FloatParameter) ((Object[]) split.getValue())[2];

		if (((Object[]) split.getValue()).length > 3) {
			othersAsNaN = (BooleanParameter) ((Object[]) split.getValue())[3];
		}
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		IPluginIOList<?> cells = (IPluginIOList<?>) pluginInputs.get("Seeds");
		BeanTableModel<?> tableModel = cells.getBeanTableModel();
		IPluginIOListOfQ<?> outputCells = (IPluginIOListOfQ<?>) pluginOutputs.get("Cells");
		cells.copyInto(outputCells);
		outputCells.clear();

		float[] values = (float[]) rangeParameter.getValue();
		float lowValue = values[0];
		float highValue = values[1];

		if (fieldNameParam.getSelection() == null)
			throw new IllegalStateException("No field selection has been made");

		int columnIndex = Utils.indexOf(getFieldNames(tableModel), fieldNameParam.getSelection());

		float color = colorParam.getFloatValue();
		String quantifiedPropertyName = quantifiedFieldNameParam.getStringValue();

		outputCells.addQuantifiedPropertyName(quantifiedPropertyName);

		boolean useNaN = othersAsNaN.getBooleanValue();

		for (int i = 0; i < tableModel.getRowCount(); i++) {
			float f = tableModel.getFloatValueAt(i, columnIndex);

			IPluginIOListMemberQ<?> pCloned = (IPluginIOListMemberQ<?>) cells.get(i).clone();
			if ((f >= lowValue) && (f <= highValue)) {
				pCloned.setQuantifiedProperty(quantifiedPropertyName, color);
			} else {
				if (useNaN)
					pCloned.setQuantifiedProperty(quantifiedPropertyName, Float.NaN);
				else
					pCloned.setQuantifiedProperty(quantifiedPropertyName, 0);
			}
			pCloned.linkToList(outputCells);
			outputCells.addDontFireValueChanged(pCloned);

		}

		outputCells.fireValueChanged(false, false);
	}
}
