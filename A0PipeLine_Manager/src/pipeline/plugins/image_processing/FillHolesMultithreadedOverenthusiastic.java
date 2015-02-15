
package pipeline.plugins.image_processing;

/* Copyright 2006, 2007 Mark Longair */
// Plugin by Mark Longair, downloaded from http://homepages.inf.ed.ac.uk/s9808248/imagej/find-connected-regions/
// Minor modifications by Olivier Cinquin
// Covered by the GPL

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.plugins.ThreeDPlugin;

public class FillHolesMultithreadedOverenthusiastic extends ThreeDPlugin {

	private volatile int returnValue = NO_ERROR; // TODO NOT THREADSAFE

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			final PreviewType previewType, final boolean inputHasChanged) throws InterruptedException {

		nCpus = Runtime.getRuntime().availableProcessors();
		if (input.getWidth() < nCpus)
			nCpus = input.getWidth();

		threads = new Thread[nCpus];

		final FillHoles plugin = new FillHoles();
		final int blockWidth = input.getWidth() / nCpus;

		input.computePixelArray();
		output.computePixelArray();

		for (int ithread = 0; ithread < nCpus; ithread++) {
			threads[ithread] = new Thread() {
				@Override
				public void run() {
					int block = slice_registry.getAndIncrement();
					int startX = block * blockWidth;
					int stopX = (block == nCpus) ? input.getWidth() - 1 : startX + blockWidth;
					int result;
					try {
						result =
								plugin.runChannel(input, output, p, previewType, inputHasChanged, startX, stopX, 0,
										input.getHeight() - 1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						result = THREAD_INTERRUPTED;
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
