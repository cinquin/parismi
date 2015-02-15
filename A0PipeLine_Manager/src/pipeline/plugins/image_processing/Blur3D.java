/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

// adapted from fiji
/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

import ij.measure.Calibration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.GUI_utils.ProgressSubrange;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOStack;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ILoopWorker;
import pipeline.misc_util.parfor.ParFor;
import pipeline.plugins.ThreeDPlugin;
import processing_utilities.convolution.convolveOneSliceIn3D;
import processing_utilities.convolution.convolver3D;

/**
 * Smoothes an ImagePlus, either uniformly or by Gaussian blur.
 * The user can specify if for Gaussian smoothing, the dimensions of
 * the pixels should be taken into account when calculating the
 * kernel. The user also specifies the radius / std dev.
 */

public class Blur3D extends ThreeDPlugin {

	@ParameterInfo(userDisplayName = "Sigma", floatValue = 1, permissibleFloatRange = { 0, 10 })
	public float sigma;

	@ParameterInfo(userDisplayName = "Use Gaussian kernel", booleanValue = true, noErrorIfMissingOnReload = true)
	private boolean useGaussian;

	@Override
	public String operationName() {
		return "3D Blur filter";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + PARALLELIZE_WITH_NEW_INSTANCES + ONLY_FLOAT_INPUT + ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL;
	}

	private boolean useCalibration = true;
	private PluginIOStack tempStack;

	private void createTempChannel(IPluginIOStack input) throws InterruptedException {
		tempStack =
				new PluginIOStack("temp stack", input.getWidth(), input.getHeight(), input.getDepth(), 1,
						PixelType.FLOAT_TYPE);
	}

	private float[] H_x, H_y, H_z;

	private float localSigmaCopy = -1;

	private static float checkCalib(float pixelW) {
		if (pixelW == 0) {
			Utils.log("Image calibration was 0; resetting to 1", LogLevel.ERROR);
			pixelW = 1;
		}
		return pixelW;
	}

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		Utils.log("Blurring with sigma=" + sigma, LogLevel.DEBUG);

		if ((tempStack == null) || (!tempStack.sameDimensions(input)))
			createTempChannel(input);

		Calibration calib = input.getCalibration();

		float pixelW = !useCalibration || calib == null ? 1.0f : (float) Math.abs(calib.pixelWidth);

		pixelW = checkCalib(pixelW);

		if (sigma != localSigmaCopy) {
			Utils.log("Creating kernel with " + (new Boolean(useGaussian)).toString() + " " + sigma + " " + pixelW
					+ " ", LogLevel.DEBUG);
			localSigmaCopy = sigma;// we don't want to get confused if the value of sigma is changed while we're
									// creating the kernel

			H_x = createKernel(localSigmaCopy, pixelW, useGaussian);

			pixelW = !useCalibration || calib == null ? 1.0f : (float) Math.abs(calib.pixelHeight);
			pixelW = checkCalib(pixelW);
			H_y = createKernel(localSigmaCopy, pixelW, useGaussian);

			pixelW = !useCalibration || calib == null ? 1.0f : (float) Math.abs(calib.pixelDepth);
			pixelW = checkCalib(pixelW);
			H_z = createKernel(localSigmaCopy, pixelW, useGaussian);

		}

		input.computePixelArray();
		output.computePixelArray();

		final AtomicInteger convolvingDimension = new AtomicInteger(1);

		final ProgressSubrange progress = new ProgressSubrange(p, 3);
		for (; convolvingDimension.get() <= 3; convolvingDimension.incrementAndGet()) {
			ParFor parFor = new ParFor("Blur", 0, input.getDepth() - 1, progress, true);
			for (int i = 0; i < parFor.getNThreads(); i++) {
				parFor.addLoopWorker(new ILoopWorker() {
					convolver3D convolver = new convolveOneSliceIn3D();

					@Override
					public Object run(int slice, int threadIndex) throws InterruptedException {

						switch (convolvingDimension.get()) {
							case 1:
								convolver.convolveX(input, output.getPixels(slice), slice + 1, H_x);
								break;
							case 2:
								convolver.convolveY(output, tempStack.getStackPixelArray()[slice], slice + 1, H_y);
								break;
							case 3:
								convolver.convolveZ(tempStack, output.getPixels(slice), slice + 1, H_z);
								break;
							default:
								throw new IllegalStateException("Unknown convolving dimension " + convolvingDimension);
						}
						return null;
					}
				});
			}
			parFor.run(true);
			progress.nextStep();
		}

	}

	private static float[] createKernel(float sigma, float pixelW, boolean useGaussian) {

		return useGaussian ? createGaussianKernel(sigma, pixelW) : createUniformKernel(sigma, pixelW);
	}

	private static float[] createUniformKernel(float radius, float pixelW) {
		radius = radius / pixelW;
		int diameter = (int) Math.ceil(2 * radius + 1);
		float[] H = new float[diameter];
		for (int i = 0; i < diameter; i++) {
			H[i] = 1f / diameter;
		}
		return H;
	}

	private static float[] createGaussianKernel(float sigma, float pixelW) {
		// the radius of a gaussian should at least be 2.5 sigma
		sigma = sigma / pixelW;
		int diameter = (int) Math.ceil(5 * sigma);
		diameter = (diameter % 2 == 0) ? diameter + 1 : diameter;
		float[] kernel = new float[diameter];
		int radius = diameter / 2;
		float sum = 0f;
		kernel[radius] = gauss(0, sigma);
		sum += kernel[radius];
		for (int x = 1; x <= radius; x++) {
			kernel[radius - x] = kernel[radius + x] = gauss(x, sigma);
			sum += 2 * kernel[radius - x];
		}
		// normalize
		for (int i = 0; i < diameter; i++) {
			kernel[i] /= sum;
		}
		return kernel;
	}

	public static float gauss(float x, float sigma) {
		return (float) Math.exp(-x * x / (2 * sigma * sigma));
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
				InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination",
				new InputOutputDescription(null, null, null, InputOutputDescription.KEEP_IN_RAM,
						InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}

}
