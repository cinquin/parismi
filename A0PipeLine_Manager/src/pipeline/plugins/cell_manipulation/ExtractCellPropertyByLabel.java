/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOListOfQ;
import pipeline.data.IQuantifiable;
import pipeline.data.InputOutputDescription;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

public class ExtractCellPropertyByLabel extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "From a set of cells create a text file with labels as columns (TODO clarify) ";
	}

	protected static String[] getFieldNames(BeanTableModel<?> tableModel) {
		String[] fieldNames = new String[tableModel.getColumnCount()];

		for (int i = 0; i < fieldNames.length; i++) {
			fieldNames[i] = tableModel.getColumnName(i);
		}

		return fieldNames;
	}

	@ParameterInfo(userDisplayName = "Field name")
	@ParameterType(parameterType = "ComboBox")
	private String fieldName = "";

	@ParameterInfo(userDisplayName = "Export file")
	private File exportFile;

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

		ComboBoxParameterPrintValueAsString fieldNameParam =
				(ComboBoxParameterPrintValueAsString) getParameter("fieldName");
		String currentChoiceName = (String) fieldNameParam.getSimpleValue();
		fieldNameParam.setChoices(fieldNames);
		fieldNameParam.setSelectionIndex(Utils.indexOf(fieldNames, currentChoiceName));
		if ((fieldNames.length > 0) && fieldNameParam.getSelectionIndex() == -1) {
			fieldNameParam.setSelectionIndex(0);
			fieldNameParam.fireValueChanged(false, true, false);
		}
	}

	@Override
	public String operationName() {
		return "Export quantified properties as text file with labels as columns";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, false, false);
		desc0.name = "Seeds";
		result.put("Seeds", desc0);
		return result;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		exportFile = (File) getParameter("exportFile").getSimpleValue();

		IPluginIOListOfQ<?> cells = (IPluginIOListOfQ<?>) pluginInputs.get("Seeds");
		if ("".equals(fieldName))
			throw new IllegalStateException("No field selection has been made");

		Map<String, List<Float>> reorganizedProp = new HashMap<>();

		for (String prop : cells.get(0).getQuantifiedPropertyNames()) {
			if (prop.indexOf("_anno_") != 0)
				continue;
			reorganizedProp.put(prop, new ArrayList<Float>());
		}

		int maxSize = 0;

		for (IQuantifiable i : cells) {
			for (String annotation : reorganizedProp.keySet()) {
				if (i.getQuantifiedProperty(annotation) > 0) {
					reorganizedProp.get(annotation).add(i.getQuantifiedProperty(fieldName));
					int size = reorganizedProp.get(annotation).size();
					if (size > maxSize)
						maxSize = size;
				}
			}
		}

		try (FileWriter outFile = new FileWriter(exportFile); PrintWriter out = new PrintWriter(outFile)) {

			boolean first = true;
			for (String annotation : reorganizedProp.keySet()) {
				if (first)
					first = false;
				else
					out.write("\t");
				out.write(annotation);
			}

			out.write("\n");

			for (int line = 0; line < maxSize; line++) {
				first = true;
				for (String annotation : reorganizedProp.keySet()) {
					if (first)
						first = false;
					else
						out.write("\t");
					List<Float> list = reorganizedProp.get(annotation);
					if (list.size() > line) {
						out.write("" + list.get(line));
					}
				}
				out.write("\n");
			}

			out.print("");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

}
