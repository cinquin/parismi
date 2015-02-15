/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.GUI_utils.PluginIOView;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.PluginInfo;

/**
 * Finds seeds by looking for local maxima in an LCG image. Stores the results as metadata within the output TIFF.
 * 
 * @author michael chiang (external C program)
 *
 */
@PluginInfo(displayToUser = false, obsolete = true)
public class CFindSeedsLibraryCall extends ExternalCallToLibrary {

	private float sigma;

	@Override
	public boolean createOutput(InputOutputDescription desc, List<PluginIOView> views) {
		if ("Seeds".equals(desc.name)) {
			initializeOutputs();
			pluginOutputs.put("Seeds", new PluginIOCells());
			return true;
		}
		return false;
	}

	@Override
	public boolean hasDisplayableOutput() {
		return false;
	}

	private class SigmaListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((int[]) param1.getValue())[0] != sigma) {
				sigma = ((int[]) param1.getValue())[0];
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener sigmaListener0 = new SigmaListener();
	private ParameterListener sigmaListener1 = new ParameterListenerWeakRef(sigmaListener0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { sigmaListener1, null };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		param1 = param[0];
		param2 = null;
		sigma = ((int[]) param1.getValue())[0]; // getValue() returns an array of Objects; first object in array is the
												// value of the parameter
	}

	@Override
	public String operationName() {
		return "C helper for finding seeds";
	}

	@Override
	public String version() {
		return "1.0";
	}

	void initializeParams() {
		param1 =
				new IntParameter("Sigma", "Sets the minimal distance between potential seeds.", 1, 0, 20, true, true,
						sigmaListener1);
	}

	public CFindSeedsLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "find_seeds";
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "r.tif" };
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));

		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds", desc0);

		return result;
	}
}
