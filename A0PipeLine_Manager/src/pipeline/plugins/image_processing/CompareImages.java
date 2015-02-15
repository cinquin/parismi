/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

/**
 * Throw exception if input image dimensions differ or if pixel values are not within tolerance.
 *
 */
public class CompareImages extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@ParameterInfo(userDisplayName = "Relative tolerance", floatValue = 0.1f, permissibleFloatRange = { 0.0001f, 0.2f })
	private float relativeTolerance;

	@ParameterInfo(userDisplayName = "Absolute tolerance", floatValue = 0.1f, permissibleFloatRange = { 0.0001f, 0.2f })
	private float absoluteTolerance;

	@Override
	public String[] getInputLabels() {
		return new String[] { "Aux 1" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] {};
	}

	@Override
	public String operationName() {
		return "CompareImages";
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
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	public class DimensionMismatchException extends PluginRuntimeException {
		private static final long serialVersionUID = -4594939512721080584L;

		public DimensionMismatchException(String message, boolean displayUserDialog) {
			super(message, displayUserDialog);
		}
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		Iterator<IPluginIO> inputs = getInputs().values().iterator();

		IPluginIOHyperstack input0 = (IPluginIOHyperstack) inputs.next();
		IPluginIOHyperstack input1 = (IPluginIOHyperstack) inputs.next();

		if (!input0.getDimensions().equals(input1.getDimensions()))
			throw new DimensionMismatchException("Dimension mismatch between inputs", true);
		List<String> channelNames =
				input0.getChannels().values().stream().map(IPluginIOStack::getName).collect(Collectors.toList());

		final double epsilon = 1.0E-10;

		for (String channelName : channelNames) {
			IPluginIOStack channel0 = input0.getChannels().get(channelName);
			IPluginIOStack channel1 = input0.getChannels().get(channelName);

			for (int z = 0; z < channel0.getDepth(); z++) {
				Object pixels0 = channel0.getPixels(z);
				Object pixels1 = channel1.getPixels(z);

				if (pixels0 instanceof byte[]) {
					byte[] cast0 = (byte[]) pixels0;
					byte[] cast1 = (byte[]) pixels1;

					for (int p = 0; p < cast0.length; p++) {
						if (Math.abs(cast0[p] - cast1[p]) > 1) {
							throw new PluginRuntimeException("Unequal pixel values " + cast0[p] + " and " + cast1[p]
									+ " on channel " + channelName + " slice " + z + " offset " + p + " in array", true);
						}
					}
				} else if (pixels0 instanceof short[]) {
					short[] cast0 = (short[]) pixels0;
					short[] cast1 = (short[]) pixels1;

					for (int p = 0; p < cast0.length; p++) {
						if (Math.abs(cast0[p] - cast1[p]) > 1) {
							throw new PluginRuntimeException("Unequal pixel values " + cast0[p] + " and " + cast1[p]
									+ " on channel " + channelName + " slice " + z + " offset " + p + " in array", true);
						}
					}
				} else if (pixels0 instanceof float[]) {
					float[] cast0 = (float[]) pixels0;
					float[] cast1 = (float[]) pixels1;

					for (int p = 0; p < cast0.length; p++) {
						if (Math.abs(cast0[p] - cast1[p]) > absoluteTolerance
								&& Math.abs(cast0[p] - cast1[p]) / (Math.abs(cast0[p]) + epsilon) > relativeTolerance) {
							throw new PluginRuntimeException("Unequal pixel values " + cast0[p] + " and " + cast1[p]
									+ " on channel " + channelName + " slice " + z + " offset " + p + " in array", true);
						}
					}
				}
			}
		}
	}

}
