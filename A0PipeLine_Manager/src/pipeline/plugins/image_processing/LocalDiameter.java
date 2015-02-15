/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PixelIterator;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntParameter;
import pipeline.plugins.ThreeDPlugin;

/**
 * Fit balls in non-0 pixel region, and output diameter of largest ball that will fit in non-0 pixels.
 */

public class LocalDiameter extends ThreeDPlugin {

	private IntParameter diamParameter = new IntParameter("Diameter", "Maximum diameter to try to fit", 10, 0, 20,
			true, true, null);

	@Override
	public AbstractParameter[] getParameters() {
		return new AbstractParameter[] { diamParameter, null };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		diamParameter = (IntParameter) param[0];
	}

	@Override
	public String operationName() {
		return "LocalDiameter";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + ONLY_FLOAT_INPUT + ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL;
	}

	private boolean cancelled;

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter progress,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		final int maxRadius = diamParameter.getintValue();
		cancelled = false;

		progressSetIndeterminateThreadSafe(progress, true);
		progressSetValueThreadSafe(progress, 0);
		indeterminateProgress = true;

		nCpus = Runtime.getRuntime().availableProcessors();
		if (input.getDepth() < nCpus)
			nCpus = input.getDepth();

		input.computePixelArray();
		output.computePixelArray();

		slice_registry.set(0);
		threads = newThreadArray();

		final int width = input.getWidth();
		final int height = input.getHeight();
		final int depth = input.getDepth();

		for (int ithread = 0; ithread < nCpus; ithread++) {
			threads[ithread] = new Thread("Local Diameter worker thread") {
				@Override
				public void run() {
					try {

						for (int z = slice_registry.getAndIncrement(); z < depth; z = slice_registry.getAndIncrement()) {

							for (int x = 0; x < width; x++) {
								for (int y = 0; y < height; y++) {
									int radius = 0;
									if (input.getFloat(x, y, z) != 0) {
										boolean done = false;
										for (radius = 1; radius < maxRadius; radius++) {
											PixelIterator ballIterator = input.getBallIterator(x, y, z, radius);
											if (!ballIterator.hasNext())
												done = true;
											while (ballIterator.hasNext()) {
												float pixelValue = ballIterator.nextFloatValue();
												if (pixelValue == 0) {
													done = true;
													break;
												}
											}
											if (done)
												break;
										}
									}
									output.setPixelValue(x, y, z, radius);
								}
							}

							int our_progress = ((int) (100.0 * ((z)) / (depth)));
							if (our_progress > progress.getValue())
								progressSetValueThreadSafe(progress, our_progress); // not perfect but at least does not
																					// required synchronization
							if (indeterminateProgress) {
								progressSetIndeterminateThreadSafe(progress, false);
								indeterminateProgress = false;
							}
							if (Thread.interrupted()) {
								cancelled = true;
							}
							if (cancelled)
								return;
						}
					} catch (Exception e) {
						Utils.printStack(e);
					}
				}
			};
		}
		startAndJoin(threads);
		if (cancelled)
			throw new InterruptedException();
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
