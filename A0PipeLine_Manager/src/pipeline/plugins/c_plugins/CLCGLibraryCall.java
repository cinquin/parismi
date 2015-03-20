/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import ij.ImagePlus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ExternalCallToLibrary;

public class CLCGLibraryCall extends ExternalCallToLibrary implements AuxiliaryInputOutputPlugin {
	private int sigma1;
	private int sigma2;

	@Override
	// for this particular plugin, we only want to pass the list of seeds on the very first call
	public void extraArgsForRunLoop(List<String> args, boolean firstLoop) {
		args.add("0");
	}

	private class SigmaListenerA extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((int[]) param1.getValue())[0] != sigma1) {
				sigma1 = ((int[]) param1.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener sigmaListenerA0 = new SigmaListenerA();
	private ParameterListener sigmaListenerA1 = new ParameterListenerWeakRef(sigmaListenerA0);

	private class SigmaListenerB extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((int[]) param2.getValue())[0] != sigma2) {
				sigma2 = ((int[]) param2.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}

		}
	}

	private ParameterListener sigmaListenerB0 = new SigmaListenerB();
	private ParameterListener sigmaListenerB1 = new ParameterListenerWeakRef(sigmaListenerB0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { sigmaListenerA1, sigmaListenerB1 };
	}

	@Override
	public AbstractParameter[] getParameters() {
		return new AbstractParameter[] { param1, param2 };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		param1 = param[0];
		param2 = param[1];
		sigma1 = ((int[]) param1.getValue())[0];
		sigma2 = ((int[]) param1.getValue())[0];
	}

	@Override
	public String operationName() {
		return "C helper for LCG";
	}

	@Override
	public String version() {
		return "1.0";
	}

	private void initializeParams() {
		param1 = new IntParameter("Param 1", "", 9, 0, 30, true, true, sigmaListenerA1);
		param2 = new IntParameter("Param 2", "", 9, 0, 30, true, true, sigmaListenerB1);
	}

	public CLCGLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "LCG";
	}

	boolean anAuxOutputHasClosed() {
		boolean result = false;
		Map<String, IPluginIO> auxOutputs = pluginOutputs;
		for (IPluginIO output : auxOutputs.values()) {
			if (output instanceof ImagePlus) {
				if (((ImagePlus) output).getWindow() != null && ((ImagePlus) output).getWindow().isClosed()) {
					result = true;
					break;
				}
			}
		}
		return result;
	}

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output,
			final pipeline.misc_util.ProgressReporter p, final PreviewType previewType, boolean inputHasChanged)
			throws InterruptedException {
		setInput(input);
		if ((changedInput) || anAuxOutputHasClosed()) {
			// TODO how should we handle this? recreateAuxOutputs(input);
			changedInput = false;
		}
		super.runChannel(input, output, p, previewType, inputHasChanged);
	}

	// TODO Don't know if fullseed.tif should be in there or just passed as a regular input
	@Override
	public String[] getInputLabels() {
		return new String[] { "preprocessed.tif" };// "LCG_storage.tif",
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "r.tif" };
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));
		result.put("preprocessed.tif", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));

		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, "Default source",
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		result.put("r.tif",
				new InputOutputDescription("r.tif", null, new PixelType[] { PixelType.FLOAT_TYPE },
						InputOutputDescription.KEEP_IN_RAM, InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS,
						true, false));
		InputOutputDescription forProtobuf =
				new InputOutputDescription("test_protobuf", null, null, 0, 0, false, false);
		forProtobuf.pluginWillAllocateOutputItself = true;
		result.put("test_protobuf", forProtobuf);
		return result;
	}

	@Override
	public boolean createOutput(InputOutputDescription desc, List<PluginIOView> views) {
		if ("test_protobuf".equals(desc.name)) {
			setOutput("test_protobuf", new PluginIOCells((List<ClickedPoint>) null), false);
			return true;
		}
		return false;
	}

	private int width;
	private int height;
	private int nSlices;
	private boolean changedInput = true;

	@Override
	// We're overriding this to create auxiliary outputs of same dimensions as the input
	// This should go away when we generalize output management and make all outputs equal
	// This is just a clunky fix for now
	public void setInput(IPluginIO source) {
		super.setInput(source);
		if (width != ((IPluginIOImage) source).getDimensions().width) {
			changedInput = true;
			width = ((IPluginIOImage) source).getDimensions().width;
		}
		if (height != ((IPluginIOImage) source).getDimensions().height) {
			changedInput = true;
			height = ((IPluginIOImage) source).getDimensions().height;
		}

		if ((nSlices != ((IPluginIOImage) source).getDimensions().depth)) {// ||(nChannels!=this.nChannels)
			changedInput = true;
			nSlices = ((IPluginIOImage) source).getDimensions().depth;
			Utils.log("" + nSlices, LogLevel.DEBUG);
		}

	}

}
