/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.IntrospectionParameters.ParameterType;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

/**
 * Given cells and an image as input, group cells by value along specified axis, and add
 * each cell to the center of accumulator image also specified as input, in channel
 * corresponding to axis chunk.
 *
 */
public class CellImageAccumulator extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public @NonNull String operationName() {
		return "CellImageAccumulator";
	}
	
	@ParameterInfo(userDisplayName = "Group by field")
	@ParameterType(parameterType = "ComboBox")
	private String fieldName = "";

	@ParameterInfo(userDisplayName = "Group by field stride", floatValue = 1)
	private float stride;

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
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		Map<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.SHORT_TYPE, PixelType.BYTE_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));

		result.put("Accumulator image", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE},
				InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));

		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, false, false);
		desc0.name = "Seeds";
		result.put("Seeds", desc0);
		return result;
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		return null;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		Map<String, InputOutputDescription> result = new HashMap<>();
		return result;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds", "Accumulator image", "Sample count image" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] {};
	}

	private void processCells(final IPluginIOList<ClickedPoint> inputCells,
			final IPluginIOHyperstack inputImage, 
			int inputChannel, final IPluginIOHyperstack accumulatorImage, 
			final IPluginIOHyperstack sampleCountImage, ProgressReporter r) {

		int width = accumulatorImage.getWidth();
		int height = accumulatorImage.getHeight();
		int depth = accumulatorImage.getDepth();
		
		r.setMin(0);
		r.setMax(inputCells.size());
		int index = 0;

		for (ClickedPoint inputCell: inputCells) {
			r.setValueThreadSafe(index++);
			float f = inputCell.getQuantifiedProperty(fieldName);
			int channel = (int) (f / stride);
			if (channel >= accumulatorImage.getnChannels()) {
				Utils.log("Warning: ignoring cell at " + f + ", outside of range", LogLevel.WARNING);
				continue;
			}
			int xCenter = (int) inputCell.x;
			int yCenter = (int) inputCell.y;
			int zCenter = (int) inputCell.z;
			for (int x = - width / 2; x < width / 2; x++) {
				for (int y = - height / 2; y < height / 2; y++) {
					for (int z = - depth / 2; z < depth / 2; z++) {
						if (x + xCenter < 0 || x + xCenter >= inputImage.getWidth() ||
								y + yCenter < 0 || y + yCenter >= inputImage.getHeight() ||
								z + zCenter < 0 || z + zCenter >= inputImage.getDepth())
							continue;
						float val = accumulatorImage.getPixelValue(x + width / 2, y + height / 2, z + depth / 2, channel, 0);
						accumulatorImage.setPixelValue(x + width / 2, y + height / 2, z + depth / 2,
								val + inputImage.getPixelValue(x + xCenter, y + yCenter, z + zCenter, inputChannel, 0), channel, 0);
						sampleCountImage.setPixelValue(x + width / 2, y + height / 2, z + depth / 2,
								1f + sampleCountImage.getPixelValue(x + width / 2, y + height / 2, z + depth / 2, channel, 0), channel, 0);

					}
				}
			}
		}
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		@SuppressWarnings("unchecked")
		final IPluginIOList<ClickedPoint> inputCells = (IPluginIOList<ClickedPoint>) pluginInputs.get("Seeds");

		if (inChannels.getSelection().length != 1) {
			throw new PluginRuntimeException(inChannels.getSelection().length + " channels " +
					"selected but expecting 1 to be selected", true);
		}
		
		int inputChannel = inChannels.getSelection()[0];
		
		final IPluginIOHyperstack inputImage = (IPluginIOHyperstack) pluginInputs.get("Default source");
		final IPluginIOHyperstack accumulatorImage = (IPluginIOHyperstack) pluginInputs.get("Accumulator image");
		final IPluginIOHyperstack sampleCountImage = (IPluginIOHyperstack) pluginInputs.get("Sample count image");

		processCells(inputCells, inputImage, inputChannel, accumulatorImage, sampleCountImage, r);

	}

}
