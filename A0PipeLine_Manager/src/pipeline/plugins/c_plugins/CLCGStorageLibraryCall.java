/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import java.util.HashMap;
import java.util.Map;

import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.plugins.ExternalCallToLibrary;

public class CLCGStorageLibraryCall extends ExternalCallToLibrary {

	@Override
	public String getTIFFType() {
		return "32";// TODO CHANGE THIS TO 64 WHEN WE CAN USE THE BIOFORMAT IMPORTER TO OPEN BIGTIFF VIRTUAL STACKS
	}

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { rangeListener1, null };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		param1 = param[0];
		minThreshold = ((int[]) param1.getValue())[0];
		maxThreshold = ((int[]) param1.getValue())[1];
	}

	@Override
	public String operationName() {
		return "C helper for LCG storage";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@SuppressWarnings("unused")
	private float minThreshold;
	@SuppressWarnings("unused")
	private float maxThreshold;

	private class RangeListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			minThreshold = ((int[]) param1.getValue())[0];
			maxThreshold = ((int[]) param1.getValue())[1];
			if (!stillChanging) {
			}
			if (pipelineCallback != null)
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}
	}

	private ParameterListener rangeListener0 = new RangeListener();
	private ParameterListener rangeListener1 = new ParameterListenerWeakRef(rangeListener0);

	void initializeParams() {
		param1 =
				new IntRangeParameter("Diameter range", "Range of diameters to convolve over", 9, 10, 0, 20, true,
						true, rangeListener1, null);

	}

	public CLCGStorageLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "LCG_storage";
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, "Default source",
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.STORE_IN_TIFF_FILE,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, false, false));

		return result;
	}

}
