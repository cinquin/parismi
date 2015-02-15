/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.plugins.ThreeDPlugin;

public class FillHoles2D extends ThreeDPlugin {

	private volatile int returnValue = NO_ERROR; // TODO NOT THREADSAFE

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			final PreviewType previewType, final boolean inputHasChanged) throws InterruptedException {

		nCpus = Runtime.getRuntime().availableProcessors();
		if (input.getWidth() < nCpus)
			nCpus = input.getWidth();

		threads = new Thread[nCpus];

		final FillHoles[] plugins = new FillHoles[nCpus];
		for (int i = 0; i < plugins.length; i++) {
			plugins[i] = new FillHoles();
		}

		input.computePixelArray();
		output.computePixelArray();

		final AtomicInteger thread_registry = new AtomicInteger(0);

		for (int ithread = 0; ithread < nCpus; ithread++) {
			threads[ithread] = new Thread() {
				@Override
				public void run() {
					int result = 0;
					int threadId = thread_registry.getAndIncrement();
					while (true) {
						int slice = slice_registry.getAndIncrement();
						if (slice >= input.getDepth())
							break;
						plugins[threadId].fixSlice = slice;
						try {
							result =
									plugins[threadId].runChannel(input, output, p, previewType, inputHasChanged, 0,
											input.getWidth() - 1, 0, input.getHeight() - 1);
						} catch (InterruptedException e) {
							result = THREAD_INTERRUPTED;
						}
						if (result != NO_ERROR)
							break;
					}
					if (result != NO_ERROR)
						returnValue = result;
				}
			};
		}

		slice_registry.set(0);
		startAndJoin(threads);

		if (returnValue != NO_ERROR)
			throw new RuntimeException();
	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY + PARALLELIZE_WITH_NEW_INSTANCES + ONLY_FLOAT_INPUT;
	}

	@Override
	public String operationName() {
		return "Fill 3D Holes";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE }, InputOutputDescription.NOT_SPECIFIED, InputOutputDescription.NOT_SPECIFIED,
				false, false));
		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.BYTE_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}

}
