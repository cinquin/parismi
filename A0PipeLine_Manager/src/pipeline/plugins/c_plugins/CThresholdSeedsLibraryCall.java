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

import org.jfree.data.xy.XYSeries;

import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.PluginInfo;

@PluginInfo(displayToUser = false, obsolete = true)
public class CThresholdSeedsLibraryCall extends ExternalCallToLibrary {

	@Override
	public void postRunUpdates() {

		((XYSeries) LCGparam.histogram).clear();
		((XYSeries) Rparam.histogram).clear();
		((XYSeries) DAPIparam.histogram).clear();
		Map<String, IPluginIO> auxOutputs = pluginOutputs;
		Utils.readHistogramIntoXYSeries((PluginIOCells) auxOutputs.get("histLCG"), ((XYSeries) LCGparam.histogram));
		Utils.readHistogramIntoXYSeries((PluginIOCells) auxOutputs.get("histr"), ((XYSeries) Rparam.histogram));
		Utils.readHistogramIntoXYSeries((PluginIOCells) auxOutputs.get("histDAPI"), ((XYSeries) DAPIparam.histogram));
	}

	@Override
	public String getTIFFType() {
		return "32";
	}

	private FloatRangeParameter LCGparam, Rparam, DAPIparam;

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { splitA1, splitB1 };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		param1 = param[0];
		param2 = param[1];
		Object[] splitParameters = (Object[]) param[0].getValue();

		LCGparam = (FloatRangeParameter) splitParameters[0];
		minLCG = ((float[]) LCGparam.getValue())[0];
		maxLCG = ((float[]) LCGparam.getValue())[1];

		Rparam = (FloatRangeParameter) splitParameters[1];
		minR = ((float[]) Rparam.getValue())[0];
		maxR = ((float[]) Rparam.getValue())[1];

		splitParameters = (Object[]) param[1].getValue();

		DAPIparam = (FloatRangeParameter) splitParameters[0];
		minDAPI = ((float[]) DAPIparam.getValue())[0];
		maxDAPI = ((float[]) DAPIparam.getValue())[1];

	}

	@Override
	public String operationName() {
		return "C helper for false positive removal";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@SuppressWarnings("unused")
	private float minLCG, maxLCG;
	@SuppressWarnings("unused")
	private float minR, maxR;
	@SuppressWarnings("unused")
	private float minDAPI, maxDAPI;

	private class LCGListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			minLCG = ((float[]) LCGparam.getValue())[0];
			maxLCG = ((float[]) LCGparam.getValue())[1];
			if (!stillChanging) {
			}
			if (pipelineCallback != null)
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}
	}

	private ParameterListener LCGListener0 = new LCGListener();
	private ParameterListener LCGListener1 = new ParameterListenerWeakRef(LCGListener0);

	private class RListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			minR = ((float[]) Rparam.getValue())[0];
			maxR = ((float[]) Rparam.getValue())[1];
			if (!stillChanging) {
			}
			if (pipelineCallback != null)
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}
	}

	private ParameterListener RListener0 = new RListener();
	private ParameterListener RListener1 = new ParameterListenerWeakRef(RListener0);

	private class DAPIListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			minDAPI = ((float[]) DAPIparam.getValue())[0];
			maxDAPI = ((float[]) DAPIparam.getValue())[1];
			if (!stillChanging) {
			}
			if (pipelineCallback != null)
				pipelineCallback.parameterValueChanged(ourRow, null, false);
		}
	}

	private ParameterListener DAPIListener0 = new DAPIListener();
	private ParameterListener DAPIListener1 = new ParameterListenerWeakRef(DAPIListener0);

	private ParameterListener splitA0 =
			new SplitParameterListener(new ParameterListener[] { LCGListener1, RListener1 });
	private ParameterListener splitA1 = new ParameterListenerWeakRef(splitA0);

	private ParameterListener splitB0 = new SplitParameterListener(new ParameterListener[] { DAPIListener1, null });
	private ParameterListener splitB1 = new ParameterListenerWeakRef(splitB0);

	void initializeParams() {
		LCGparam =
				new FloatRangeParameter("LCG thresholds", "Throw out seeds whose LCG falls outside this range", 0, 999,
						0, 999, true, true, LCGListener1, null);
		Rparam =
				new FloatRangeParameter("R thresholds",
						"Throw out seeds whose radius of maximal LCG (?) intensity falls outside this range", 0, 999,
						0, 999, true, true, RListener1, null);
		DAPIparam =
				new FloatRangeParameter("DAPI thresholds",
						"Throw out seeds whose DAPI content falls outside this range", 0, 999, 0, 999, true, true,
						DAPIListener1, null);
		param1 = new SplitParameter(new Object[] { LCGparam, Rparam });
		param2 = new SplitParameter(new Object[] { DAPIparam, null });

	}

	public CThresholdSeedsLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "threshold_seeds";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "r.tif", "DAPI.tif", "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds", "histLCG", "histr", "histDAPI" };
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

		InputOutputDescription desc = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc.name = "histLCG";
		desc.pluginWillAllocateOutputItself = true;
		result.put("histLCG", desc);
		InputOutputDescription desc2 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc2.name = "histDAPI";
		desc2.pluginWillAllocateOutputItself = true;
		result.put("histDAPI", desc2);
		InputOutputDescription desc3 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc3.name = "histr";
		desc3.pluginWillAllocateOutputItself = true;
		result.put("histr", desc3);
		return result;
	}

	@Override
	public boolean createOutput(InputOutputDescription desc, List<PluginIOView> views) {
		Utils.log("In threhsold seed create destination", LogLevel.VERBOSE_DEBUG);
		if (("histLCG".equals(desc.name)) || ("histDAPI".equals(desc.name)) || ("histr".equals(desc.name))) {
			Utils.log("Creating histograms", LogLevel.DEBUG);
			initializeOutputs();
			pluginOutputs.put(desc.name, new PluginIOCells());
			return true;
		} else if ("Seeds".equals(desc.name)) {
			Utils.log("Creating seeds", LogLevel.DEBUG);
			initializeOutputs();
			PluginIOCells seeds = new PluginIOCells();
			pluginOutputs.put("Seeds", seeds);
			return true;
		}
		return false;
	}
}
