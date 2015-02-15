/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import ij.ImagePlus;
import ij.measure.Calibration;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithBufferedImagePlus;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.image_with_toolbar.PluginIOHyperstackWithToolbar;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.tiff_read_write.TIFFFileAccessor;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.TableParameter;

/**
 * This plugin acts as a shell for plugins that do not want to handle
 * all the complexity of the source data. Depending on the plugin it
 * is managing, the shell plugin calls it on single slices of the
 * source dataset (for managed plugins that implement {@link pipeline.plugins.TwoDPlugin}),
 * or on single channels of the source dataset (for managed plugins that
 * implement {@link ThreeDPlugin}). The time dimension is not handled
 * at this point, but should be in the future.
 *
 */
@PluginInfo(displayToUser = false)
public class PluginShell extends FourDPlugin implements IPluginShell, MouseEventPlugin {

	@Override
	public void setPlugin(PipelinePlugin f) {
		managedPlugin = f;
		managedPlugin.setUpdateTriggering(false);
	}

	private void checkLocalInstanceAllocated() {
		if (localInstanceOnlyforParameters == null) {
			try {
				localInstanceOnlyforParameters = managedPlugin.getClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			localInstanceOnlyforParameters.setpipeLineListener(pipelineCallback);
			localInstanceOnlyforParameters.setRow(ourRow);
		}
	}

	@Override
	public ParameterListener[] getParameterListeners() {
		checkLocalInstanceAllocated();
		return localInstanceOnlyforParameters.getParameterListeners();
	}

	@Override
	public void setParameters(final AbstractParameter[] param) {
		checkLocalInstanceAllocated();
		localInstanceOnlyforParameters.setParameters(param);
	}

	@Override
	public void setRow(int r) {
		ourRow = r;
		if (localInstanceOnlyforParameters != null) {
			localInstanceOnlyforParameters.setRow(ourRow);
		}
		if (managedPlugin != null)
			managedPlugin.setRow(ourRow);
	}

	private PipelinePlugin localInstanceOnlyforParameters;

	@Override
	public AbstractParameter[] getParameters() {
		checkLocalInstanceAllocated();
		return localInstanceOnlyforParameters.getParameters();
	}

	public boolean dontParallelize = true;
	public PipelinePlugin managedPlugin = null;

	@Override
	public String operationName() {
		return managedPlugin.operationName();
	}

	@Override
	public String version() {
		return managedPlugin.version();
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getFlags() {
		return (managedPlugin.getFlags() & (Integer.MAX_VALUE - ONLY_2D - ONLY_3D) | DISPOSE_WHEN_NSLICES_CHANGES | ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL);
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		managedPlugin.setParameters(localInstanceOnlyforParameters.getParameters());

		if (((managedPlugin.getFlags() & NO_IMP_OUTPUT) > 0))
			return null;
		if (((managedPlugin.getFlags() & PLUGIN_CREATES_OUTPUT_ITSELF) > 0)) {
			List<PluginIOView> result = managedPlugin.createOutput(outputName, impForDisplay, null);
			setOutputs(managedPlugin.getOutputs());
			return result;
		}

		List<PluginIOView> imagesToShow = new ArrayList<>(5);

		boolean needToCallPluginCreateDestination = false;

		Map<String, InputOutputDescription> outputDescriptions = managedPlugin.getOutputDescriptions();
		for (Entry<String, InputOutputDescription> keyValue : outputDescriptions.entrySet()) {
			InputOutputDescription desc = keyValue.getValue();
			String keyName = keyValue.getKey();
			Utils.log("Shell creating destination " + desc.name, LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);

			if (desc.pluginWillAllocateOutputItself) {
				if (!(managedPlugin.createOutput(desc, imagesToShow)))
					needToCallPluginCreateDestination = true;
				continue;
			}

			IPluginIO matchingInput;
			IPluginIOImage matchingInputImage = null;
			if ((desc.dimensions == InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS)
					|| (desc.dimensions == InputOutputDescription.Z_PROJECTS)
					|| (desc.dimensions == InputOutputDescription.CUSTOM)) {
				matchingInput = managedPlugin.getInputs().get(desc.matchingInputOrOutput);
				if (matchingInput == null) {
					if (desc.useDefaultIfMatchingAbsent)
						matchingInput = managedPlugin.getInput();
				}
				if (desc.dimensions != InputOutputDescription.CUSTOM) // Allow null matching input if custom dimensions
					// (for example, video grabbing plugin does not need an input to compute dimensions from)
					if (matchingInput == null)
						throw new RuntimeException("Cannot create output " + desc.name
								+ " because no matching input among "
								+ Utils.printStringArray(managedPlugin.getInputs().keySet().toArray(new String[] {})));

				if (!(matchingInput instanceof IPluginIOImage)) {

				} else
					matchingInputImage = (IPluginIOImage) matchingInput;
			}

			String name = desc.name == null ? keyName : desc.name;
			IPluginIOImage createdOutput = null;
			if (desc.dimensions == InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS) {
				if (desc.bestStorageMethod == InputOutputDescription.STORE_IN_TIFF_FILE) {
					File f = null;
					try {
						f = File.createTempFile("storage_for_output_" + name, ".tiff");
						Calibration cal =
								(Calibration) (matchingInputImage != null ? matchingInputImage.getCalibration().clone()
										: null);
						createdOutput = new TIFFFileAccessor(f, name, PixelType.FLOAT_TYPE, cal, true);
						Utils.log("Created TIFFFileAccessor to be used as output: " + name, LogLevel.DEBUG);

						managedPlugin.setOutput(name, createdOutput, false);
					} catch (IOException e) {
						Utils.log("Error creating backing file for output " + name, LogLevel.ERROR);
						Utils.printStack(e);
						throw new RuntimeException("Error creating backing file for output " + name);
					}
				} else {
					PixelType pType = null;
					if (desc.preferredPixelType != null) {
						pType = desc.preferredPixelType;
					} else {
						PixelType inputType = matchingInputImage.getPixelType();
						for (PixelType t : desc.acceptablePixelTypes) {
							if (t.equals(inputType)) {
								pType = t;
								break;
							}
						}
						if (pType == null)
							pType = desc.preferredPixelType;
					}
					createdOutput = matchingInputImage.duplicateStructure(pType, -1, 0, desc.dontAllocatePixels);
					if (desc.name != null)
						createdOutput.setName(desc.name);
					// 0 because we don't know yet how many channels the user selected to run the plugin on; therefore
					// we
					// don't know how many channels to create in the output
				}
				managedPlugin.setOutput(name, createdOutput, false);
			} else if (desc.dimensions == InputOutputDescription.Z_PROJECTS) {
				createdOutput =
						matchingInputImage.duplicateStructure(desc.preferredPixelType, 1, 0, desc.dontAllocatePixels);
				((IPluginIOHyperstack) createdOutput).setDepth(1);
				managedPlugin.setOutput(name, createdOutput, false);
			} else if (desc.dimensions == InputOutputDescription.CUSTOM) {
				if (desc.dontExpandChannelsBasedOnUserSelectionInDefaultSource)
					createdOutput =
							new PluginIOHyperstack(desc.name, ((SpecialDimPlugin) managedPlugin)
									.getOutputWidth(matchingInputImage), ((SpecialDimPlugin) managedPlugin)
									.getOutputHeight(matchingInputImage), ((SpecialDimPlugin) managedPlugin)
									.getOutputDepth(matchingInputImage), ((SpecialDimPlugin) managedPlugin)
									.getOutputNChannels(matchingInputImage), ((SpecialDimPlugin) managedPlugin)
									.getOutputNTimePoints(matchingInputImage), desc.preferredPixelType, false);// TODO
																												// get
																												// pixel
																												// type
																												// from
																												// SpecialDimPlugin
																												// instead
				else
					createdOutput =
							new PluginIOHyperstack(desc.name, ((SpecialDimPlugin) managedPlugin)
									.getOutputWidth(matchingInputImage), ((SpecialDimPlugin) managedPlugin)
									.getOutputHeight(matchingInputImage), ((SpecialDimPlugin) managedPlugin)
									.getOutputDepth(matchingInputImage), 0, ((SpecialDimPlugin) managedPlugin)
									.getOutputNTimePoints(matchingInputImage), desc.preferredPixelType, false);
				if (createdOutput.getPixelType() == null)
					createdOutput.setPixelType(PixelType.FLOAT_TYPE);
				managedPlugin.setOutput(name, createdOutput, false);
			} else
				throw new RuntimeException("Unrecognized dimension requirement " + desc.dimensions
						+ " will creating destination " + desc.name);

			if (matchingInputImage != null) {
				createdOutput.setCalibration(matchingInputImage.getCalibration());
				(createdOutput).setImageAcquisitionMetadata(matchingInputImage.getImageAcquisitionMetadata());
			}
			createdOutput.setDescription(desc);
			if ((desc.showAViewerByDefault) && (!Utils.headless)) {
				PluginIOHyperstackViewWithImagePlus display = null;
				if (impForDisplay != null) {
					(createdOutput).setImp(impForDisplay);
					display = impForDisplay;
				} else {
					if (desc.bufferViewer)
						display = new PluginIOHyperstackViewWithBufferedImagePlus(createdOutput.getName());
					else
						display = new PluginIOHyperstackWithToolbar(createdOutput.getName());// PluginIOHyperstackViewWithImagePlus
					(createdOutput).setImp(display);
				}
				display.addImage((PluginIOHyperstack) createdOutput);
				if (!desc.dontAutoRange)
					display.shouldUpdateRange = true;
				imagesToShow.add(display);
			}
		}

		if (needToCallPluginCreateDestination) {
			imagesToShow.addAll(managedPlugin.createOutput(outputName, impForDisplay, null));
		}

		return imagesToShow;

	}

	@Override
	public void setOutputs(Map<String, IPluginIO> destinations) {
		this.pluginOutputs = destinations;
		if (destinations == null) {
			Utils.log(
					"Setting null destination in PluginShell; this should not happen unless the plugin has no imp output",
					LogLevel.DEBUG);
		}
	}

	@Override
	public void getInputs(Map<String, IPluginIO> sources) {
		if (managedPlugin != null)
			managedPlugin.getInputs(sources);
		// Is the stuff below still necessary??
		this.pluginInputs = sources;
		if (localInstanceOnlyforParameters != null) {
			localInstanceOnlyforParameters.getInputs(sources);
		} else
			Utils.log("couldn't set source of local instance", LogLevel.DEBUG);
	}

	private void runThroughSlices(final IPluginIOStack inputStack, final IPluginIOStack output,
			final ProgressReporter progress, final PreviewType previewType) throws InterruptedException {
		String threadName = Thread.currentThread().getName();
		Thread.currentThread().setName("RunThrough2DSlices");

		int maxNThreads;
		if ((managedPlugin.getFlags() & DONT_PARALLELIZE) != 0)
			maxNThreads = 1;
		else
			maxNThreads = Integer.MAX_VALUE;
		if (maxNThreads > inputStack.getDepth())
			maxNThreads = Math.max(1, inputStack.getDepth());

		ParFor parFor = new ParFor(0, inputStack.getDepth() - 1, progress, BasePipelinePlugin.threadPool, true);
		progress.setIndeterminate(false);
		parFor.setName("2D worker");
		final int nThreads = parFor.getNThreads();
		final PipelinePlugin[] plugin2DInstances = new PipelinePlugin[nThreads];

		if (ourRow == -1)
			throw new IllegalStateException("Our row is -1");

		if ((managedPlugin.getFlags() & PARALLELIZE_WITH_NEW_INSTANCES) != 0) {
			for (int i = 0; i < nThreads; i++) {
				if (plugin2DInstances[i] == null)
					try {
						plugin2DInstances[i] = managedPlugin.getClass().newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				if (plugin2DInstances[i] == null)
					throw new IllegalStateException("Null plugin instance in Shell2D");
				if (localInstanceOnlyforParameters == null)
					throw new IllegalStateException("Null local instance in Shell2D");
				plugin2DInstances[i].setParameters(localInstanceOnlyforParameters.getParameters());
				plugin2DInstances[i].setRow(ourRow);
			}
		} else {
			managedPlugin.setParameters(localInstanceOnlyforParameters.getParameters());
			managedPlugin.setRow(ourRow);
			for (int i = 0; i < nThreads; i++) {
				plugin2DInstances[i] = managedPlugin;
			}
		}

		inputStack.computePixelArray();
		output.computePixelArray();

		for (int ithread = 0; ithread < nThreads; ithread++)
			parFor.addLoopWorker((slice, threadIndex) -> {
				((TwoDPlugin) plugin2DInstances[threadIndex]).runSlice(inputStack.getPixelsAsProcessor(slice), output
						.getPixelsAsProcessor(slice), previewType);
				return null;
			});

		parFor.run(true);
		((TwoDPlugin) plugin2DInstances[0]).postRun();
		Thread.currentThread().setName(threadName);
	}

	// outputChannels is ignored for now
	public void run3D(final ProgressReporter progress, final String[] inputChannels, final String[] outputChannels,
			final PreviewType previewType, final AbstractParameter changedParam, final boolean stayInCoreLoop)
			throws InterruptedException { // Run the plugin channel by channel
		dontParallelize = (managedPlugin.getFlags() & DONT_PARALLELIZE) != 0;

		int maxNThreads;
		if (dontParallelize)
			maxNThreads = 1;
		else
			maxNThreads = Integer.MAX_VALUE;
		if (maxNThreads > inputChannels.length)
			maxNThreads = Math.max(1, inputChannels.length);
		// If there are no input channels, just run 1 instance of the plugin

		if (((managedPlugin.getFlags() & PipelinePlugin.ONLY_1_INPUT_CHANNEL) > 0) && inputChannels.length > 1) {
			throw new IllegalArgumentException("Plugin " + managedPlugin.operationName()
					+ " cannot run on more than 1 channel");
		}

		final int nChannels = inputChannels.length == 0 ? 1 : inputChannels.length;

		Utils.log("run loop forShell3D", LogLevel.VERBOSE_DEBUG);

		// For now parallelize the slices within each channel
		// It might be worth also implementing parallelization by channel (for example when there are few or even just 1
		// slice per channel
		// and the computation per slice is very slow)

		Utils.log("run3d input channels are " + Utils.printStringArray(inputChannels), LogLevel.DEBUG);
		Utils.log("run3d output channels are " + Utils.printStringArray(outputChannels), LogLevel.DEBUG);

		IPluginIOStack[] inputStackArray;
		IPluginIOStack[] outputStackArray;

		IPluginIO defaultInput = managedPlugin.getInput();

		if (defaultInput instanceof IPluginIOHyperstack)
			inputStackArray = ((IPluginIOHyperstack) defaultInput).getChannels(inputChannels);
		else {
			Utils.log("Default input in run3D is not a stack or a hyperstack; it is " + defaultInput, LogLevel.DEBUG);
			// We are allowing this as a convenience for plugins that want to pretend to be 3Dplugins even though they
			// do not have a hyperstack as input. This could be for example because they extend a 3D plugin
			inputStackArray = new IPluginIOStack[] { null };
		}

		IPluginIO defaultOutput = managedPlugin.getOutput();
		if (defaultOutput instanceof IPluginIOStack)
			outputStackArray = new IPluginIOStack[] { (IPluginIOStack) defaultOutput };
		else if (defaultOutput instanceof IPluginIOHyperstack)
			outputStackArray = ((IPluginIOHyperstack) defaultOutput).getChannels(outputChannels);
		else if (defaultOutput == null)
			outputStackArray = new IPluginIOStack[] { null };
		else
			throw new RuntimeException("Default output in run3D is not a stack or a hyperstack; it is " + defaultOutput);

		progressSetIndeterminateThreadSafe(progress, true);
		progressSetValueThreadSafe(progress, 0);

		ParFor parFor = new ParFor(0, nChannels - 1, progress, BasePipelinePlugin.threadPool, true, maxNThreads);
		parFor.setName("3D worker");
		final int nThreads = parFor.getNThreads();
		final PipelinePlugin[] plugin3DInstances = new PipelinePlugin[nThreads];

		if ((managedPlugin.getFlags() & PARALLELIZE_WITH_NEW_INSTANCES) != 0) {
			for (int i = 0; i < nThreads; i++) {
				if (plugin3DInstances[i] == null)
					try {
						plugin3DInstances[i] = managedPlugin.getClass().newInstance();
					} catch (InstantiationException | IllegalAccessException e) {
						throw new RuntimeException(e);
					}

				plugin3DInstances[i].setpipeLineListener(pipelineCallback);
				plugin3DInstances[i].setUpdateTriggering(false);
				plugin3DInstances[i].setParameters(localInstanceOnlyforParameters.getParameters());
				plugin3DInstances[i].setRow(ourRow);
			}
		} else {
			managedPlugin.setParameters(localInstanceOnlyforParameters.getParameters());
			managedPlugin.setRow(ourRow);
			for (int i = 0; i < nThreads; i++) {
				plugin3DInstances[i] = managedPlugin; // TODO Fix parameter setting
			}
		}

		final boolean is2Dplugin = (managedPlugin instanceof TwoDPlugin);
		final IPluginIOStack[] finalOutputStackArray = outputStackArray;
		final IPluginIOStack[] finalInputStackArray = inputStackArray;

		for (int i = 0; i < nThreads; i++) {
			parFor.addLoopWorker((channelNumber, threadID) -> {
				IPluginIOStack inputStack = null, outputStack = null;

				if (channelNumber < inputChannels.length) {
					// The only reason this would not be true is if there is no input channel
					inputStack = finalInputStackArray[channelNumber];
					if (channelNumber < finalOutputStackArray.length)
						outputStack = finalOutputStackArray[channelNumber];
				}

				if (is2Dplugin) {
					// Need to loop through the slices and call the 2D plugin on each
					runThroughSlices(inputStack, outputStack, progress, previewType);
				} else {
					Utils.log(getOutput() + "", LogLevel.DEBUG);

					final PluginIOHyperstackViewWithImagePlus view =
							getOutput() != null ? ((IPluginIOHyperstack) getOutput()).getImp() : null;
					final ImagePlus outputImp =
							getOutput() instanceof IPluginIOHyperstack
									? ((IPluginIOHyperstack) getOutput()).getImp() != null
											? ((IPluginIOHyperstack) getOutput()).getImp().imp : null : null;
					Object semaphore = null;

					long timeLastRun;

					do {
						timeLastRun = System.currentTimeMillis();
						((ThreeDPlugin) plugin3DInstances[threadID]).runChannel(inputStack, outputStack, progress,
								previewType, true);

						if (stayInCoreLoop) {
							if (!changedParam.isStillChanging()) {
								changedParam.setTimeLastResponseToChange(System.currentTimeMillis());
								semaphore = changedParam.getSemaphore();
								synchronized (semaphore) {
									semaphore.notifyAll();
								}
							} else { // Parameter still changing
								if (outputImp != null) {
									view.redrawAndWait();
									// Experimental addition; remove it causes flicker or slowdowns
									// TODO Remove redundant redrawing
									view.displayedImages.get(0).getImagePlusDisplay().getWindow().repaint();
								}
								changedParam.setTimeLastResponseToChange(System.currentTimeMillis());
								semaphore = changedParam.getSemaphore();
								synchronized (semaphore) {
									semaphore.notifyAll();
								}

								synchronized (semaphore) {
									while (changedParam.isStillChanging()
											&& (timeLastRun >= changedParam.getTimeLastChange())) {

										semaphore = changedParam.getSemaphore();
										semaphore.wait();
									}
								}
							} // end if parameter still changing
						} // end if stayInCoreLoop
					} while (stayInCoreLoop && changedParam.isStillChanging());
				}
				// FIXME Ugly hack: the plugins can create more pluginOutputs, but they will get lost if they're not
				// added to the master instance

				getOutputs().putAll(plugin3DInstances[threadID].getOutputs());

				return null;
			});
		}

		parFor.run(true);

		progressSetValueThreadSafe(progress, 100);
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		if (localInstanceOnlyforParameters != null) {
			localInstanceOnlyforParameters.setRow(ourRow);
		}

		String[] outSelection = outChannels.getSelectionString();

		managedPlugin.getOutputs().values().stream().filter(
				output -> (output.getDescription() != null)
						&& (!output.getDescription().dontExpandChannelsBasedOnUserSelectionInDefaultSource)
						&& (output instanceof IPluginIOHyperstack)).map(output -> (IPluginIOHyperstack) output)
				.forEach(output -> {
					for (String channelName : outSelection) {
						if (!output.getChannels().containsKey(channelName))
							output.addChannel(channelName);
					}
				});

		run3D(r, inChannels.getSelectionString(), outSelection, previewType, parameterWhoseValueChanged, stayInCoreLoop);// newOutSelection.toArray(new
																															// String[]
																															// {})
	}

	@Override
	public int mouseClicked(PluginIO clickedPoints, boolean inputHasChanged, MouseEvent generatingEvent)
			throws InterruptedException {
		if (!(managedPlugin instanceof MouseEventPlugin)) {
			throw new RuntimeException("Plugin doesn't know how to respond to mouse clicks in row " + ourRow);
		}
		managedPlugin.setParameters(localInstanceOnlyforParameters.getParameters());
		return ((MouseEventPlugin) managedPlugin).mouseClicked(clickedPoints, false, null);
	}

	@Override
	public void processClicks() {
		if (!(managedPlugin instanceof MouseEventPlugin)) {
			throw new RuntimeException("Plugin doesn't know how to respond to mouse clicks in row " + ourRow);
		}
		managedPlugin.setParameters(localInstanceOnlyforParameters.getParameters());
		((MouseEventPlugin) managedPlugin).processClicks();
	}

	@Override
	public void impReplacement(ImagePlus imp, ImagePlus newImp) {
		if (managedPlugin != null)
			managedPlugin.impReplacement(imp, newImp);
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		if (managedPlugin != null)
			return managedPlugin.getInputDescriptions();
		return null;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		if (managedPlugin != null)
			return managedPlugin.getOutputDescriptions();
		return null;
	}

	@Override
	public void setInput(IPluginIO source) {
		if (managedPlugin != null)
			managedPlugin.setInput(source);
	}

	@Override
	public void setInput(String name, IPluginIO source, boolean setAsDefault) {
		if (managedPlugin != null)
			managedPlugin.setInput(name, source, setAsDefault);
	}

	@Override
	public Map<String, IPluginIO> getInputs() {
		return managedPlugin.getInputs();
	}

	@Override
	public final void clearInputs() {
		managedPlugin.clearInputs();
	}

	@Override
	public final void clearOutputs() {
		managedPlugin.clearOutputs();
	}

	@Override
	public IPluginIO getInput() {
		return managedPlugin.getInput();
	}

	@Override
	public IPluginIOImage getImageInput() {
		return managedPlugin.getImageInput();
	}

	@Override
	public Map<String, IPluginIO> getOutputs() {
		return managedPlugin.getOutputs();
	}

	@Override
	public IPluginIO getOutput() {
		return managedPlugin.getOutput();
	}

	@Override
	public void cleanup() {
		if (managedPlugin != null)
			try {
				managedPlugin.cleanup();
			} catch (Exception e) {
				Utils.printStack(e);
			}
		if (localInstanceOnlyforParameters != null) {
			localInstanceOnlyforParameters.cleanup();
		}
	}
}
