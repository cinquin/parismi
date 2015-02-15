/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.c_plugins;

import ij.measure.Calibration;

import java.util.HashMap;
import java.util.Map;

import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.plugins.ExternalCallToLibrary;
import pipeline.plugins.SpecialDimPlugin;

public class CRescaleZLibraryCall extends ExternalCallToLibrary implements SpecialDimPlugin {

	private float sigma;

	private class SigmaListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (((float[]) ratioParam.getValue())[0] != sigma) {
				sigma = ((float[]) ratioParam.getValue())[0];
				pipelineCallback.clearDestinations(ourRow);
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private boolean guessFactorFromImage = true;
	private ParameterListener guessListener0 = new guessFactorFromImageListener();
	private ParameterListener guessListener1 = new ParameterListenerWeakRef(guessListener0);

	private BooleanParameter guessParameter =
			new BooleanParameter(
					"Guess factor from image",
					"If image has calibration information, guess what factor to rescale by so pixels have equal dimensions on the z axis as on the xy axis",
					true, true, guessListener1);

	private class guessFactorFromImageListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			boolean guess = (Boolean) ((Object[]) (guessParameter).getValue())[0];
			if (guessFactorFromImage != guess) {
				guessFactorFromImage = guess;
				ratioParam.setEditable(!guess);
				if (guess) {
					pipelineCallback.clearDestinations(ourRow);
					pipelineCallback.parameterValueChanged(ourRow, null, false);
				}
			}
		}
	}

	private ParameterListener sigmaListener0 = new SigmaListener();
	private ParameterListener sigmaListener1 = new ParameterListenerWeakRef(sigmaListener0);
	private FloatParameter ratioParam = new FloatParameter("Factor", "Ratio of z to xy resolution in acquired image.",
			1.0f, 0.0f, 5.0f, false, true, true, sigmaListener1);

	private int method = 2;

	private class MethodListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (!(((ComboBoxParameter) param2).getSelectionIndex() == method)) {
				method = ((ComboBoxParameter) param2).getSelectionIndex();
				pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener methodListener0 = new MethodListener();
	private ParameterListener methodListener1 = new ParameterListenerWeakRef(methodListener0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] {
				new SplitParameterListener(new ParameterListener[] { guessListener1, sigmaListener1 }), methodListener1 };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		SplitParameter splitParam = (SplitParameter) param[0];
		param1 = splitParam;
		guessParameter = (BooleanParameter) ((Object[]) splitParam.getValue())[0];
		ratioParam = (FloatParameter) ((Object[]) splitParam.getValue())[1];
		param2 = param[1];

		guessFactorFromImage = (Boolean) ((Object[]) (guessParameter).getValue())[0];
		sigma = ((float[]) ratioParam.getValue())[0];
		method = ((ComboBoxParameter) param2).getSelectionIndex();
	}

	@Override
	public String operationName() {
		return "C helper for rescaling Z axis";
	}

	@Override
	public String version() {
		return "1.0";
	}

	void initializeParams() {
		guessParameter =
				new BooleanParameter(
						"Guess factor from image",
						"If image has calibration information, guess what factor to rescale by so pixels have equal dimensions on the z axis as on the xy axis",
						true, true, guessListener1);
		guessParameter.dontPrintOutValueToExternalPrograms = true;
		ratioParam =
				new FloatParameter("Factor", "Ratio of z to xy resolution in acquired image.", 1.0f, 0.0f, 5.0f, false,
						true, true, sigmaListener1);

		param1 = new SplitParameter(new Object[] { guessParameter, ratioParam });

		param2 =
				new ComboBoxParameter("Projection method", "", new String[] { "", "Nearest neighbor",
						"Linear interpolation" }, "Linear interpolation", false, methodListener1);
	}

	public CRescaleZLibraryCall() {
		initializeParams();
	}

	@Override
	public String getCommandName() {
		return "rescaleZ";
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		return ((IPluginIOImage) input).getDimensions().height;
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		return ((IPluginIOImage) input).getDimensions().width;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		if (guessFactorFromImage) {
			Calibration c = ((IPluginIOHyperstack) input).getCalibration();
			if (c == null) {
				throw new PluginRuntimeException("Cannot guess rescaling factor for image " + input
						+ " because it is not calibrated", true);
			}
			sigma = (float) (c.pixelDepth / c.pixelHeight);
			int result = (int) Math.floor(((IPluginIOHyperstack) input).getDepth() * sigma);

			ratioParam.setValue(new float[] { sigma, ((float[]) ratioParam.getValue())[1],
					((float[]) ratioParam.getValue())[2] });
			return result;
		} else {
			if (sigma <= 0)
				throw new IllegalArgumentException("rescaling factor " + sigma + " illegal");
			int result = (int) Math.floor(((IPluginIOHyperstack) input).getDepth() * sigma);
			return result;
		}
	}

	@Override
	public void postRunUpdates() {
		Calibration c = ((IPluginIOImage) getOutput()).getCalibration();
		Calibration newCalibration = new Calibration();
		newCalibration.pixelHeight = c.pixelHeight;
		newCalibration.pixelWidth = c.pixelWidth;
		newCalibration.pixelDepth = c.pixelWidth;
		newCalibration.setUnit(c.getUnit());
		newCalibration.setXUnit(c.getXUnit());
		newCalibration.setYUnit(c.getYUnit());
		newCalibration.setZUnit(c.getZUnit());
		((IPluginIOImage) getOutput()).setCalibration(newCalibration);
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getnChannels();
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, "Default source",
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.CUSTOM, true, false));

		return result;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;// TODO Update this when we handle non-float images
	}
}
