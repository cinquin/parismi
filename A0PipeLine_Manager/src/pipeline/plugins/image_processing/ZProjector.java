/**
 * This plugin performs a z-projection of the input stack. Type of
 * output image is same as type of input image. Maximum, average, and stdev
 * projections are supported.
 * 
 * Original version:
 * 
 * @author Patrick Kelly <phkelly@ucsd.edu>
 * 
 *         Parismi additions:
 */
/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import static processing_utilities.projection.RayFunction.MAX_METHOD;
import static processing_utilities.projection.RayFunction.MEDIAN_METHOD;
import static processing_utilities.projection.RayFunction.MEDIAN_METHOD_NON_0;
import static processing_utilities.projection.RayFunction.METHODS;
import static processing_utilities.projection.RayFunction.SD_METHOD;
import static processing_utilities.projection.RayFunction.SUM_METHOD;
import static processing_utilities.projection.RayFunction.projectSlice;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Timer;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.image_with_toolbar.ActiveContourToolbar;
import pipeline.GUI_utils.image_with_toolbar.ImageCanvasWithAnnotations;
import pipeline.GUI_utils.image_with_toolbar.StackWindowWithToolbar;
import pipeline.data.IPluginIOStack;
import pipeline.data.ImageAccessor;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.SliceAccessor;
import pipeline.data.video.MissingMagicNumber;
import pipeline.data.video.VideoFrameInfo;
import pipeline.misc_util.Pair;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ActionParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.IntRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.plugins.ThreeDPlugin;
import processing_utilities.projection.RayFunction;

public class ZProjector extends ThreeDPlugin {

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.Z_PROJECTS, true, true));
		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@SuppressWarnings("unused")
	private boolean isVideo;

	@Override
	public void runChannel(IPluginIOStack input, IPluginIOStack output, ProgressReporter r, PreviewType previewType,
			boolean inputHasChanged) throws InterruptedException {
		returnValue = 0;

		int nSlices = input.getDimensions().depth;

		preferredStartSlice = ((int[]) intRangeParam.getValue())[0];
		preferredStopSlice = ((int[]) intRangeParam.getValue())[1];

		if (rangeSet) {
			startSlice = preferredStartSlice;
			if (preferredStartSlice > nSlices) {
				Utils.log("Range exceeds number of slices; adjusting", LogLevel.DEBUG);
				startSlice = nSlices;
			}
			if (startSlice < 1)
				startSlice = 1;

		} else
			startSlice = 1;

		if (rangeSet) {
			stopSlice = preferredStopSlice;
			if (preferredStopSlice > nSlices) {
				Utils.log("Range exceeds number of slices; adjusting", LogLevel.DEBUG);
				stopSlice = nSlices;
			}
		} else
			stopSlice = nSlices;


		doProjection(input, output, r, previewType, inputHasChanged);

		isVideo = false;

		PluginIOHyperstackViewWithImagePlus view = output.getImp();
		if (view != null && (!Utils.headless)) {
			if (view.getZRange() == null) // Should only happen on first run
				// We shouldn't register a new listener every time we get here
				try {
					view.show();

					((ImageCanvasWithAnnotations) view.displayedImages.get(0).getImp().imp.getCanvas())
							.addPrivateKeyListener(new KeyListener() {

								@Override
								public void keyTyped(KeyEvent e) {
									if (' ' == e.getKeyChar()) {
										if (startButton.isStillChanging()) {
											stopButton.buttonPressed(null, false, null);
										} else {
											startButton.buttonPressed(null, false, null);
										}
									} else if ('z' == e.getKeyChar()) {
										stepButton.buttonPressed(null, false, null);

									} else if ('Z' == e.getKeyChar()) {
										stepButton.buttonPressed(null, false, new ActionEvent(this, 0, "Z", System
												.currentTimeMillis(), InputEvent.SHIFT_MASK));
									}
								}

								@Override
								public void keyReleased(KeyEvent e) {
								}

								@Override
								public void keyPressed(KeyEvent e) {
								}
							});
				} catch (Exception e) {
					Utils.printStack(e);
				}
			if (VideoFrameInfo.hasMagicNumber(input.getPixels(startSlice - 1))) {
				try {
					isVideo = true;
					VideoFrameInfo startSliceInfo = new VideoFrameInfo(input.getPixels(startSlice - 1));
					VideoFrameInfo stopSliceInfo = new VideoFrameInfo(input.getPixels(stopSlice - 1));
					Utils.log("Start slice time: " + startSliceInfo, LogLevel.INFO);
					Utils.log("Stop slice time: " + stopSliceInfo, LogLevel.INFO);
					view.setZRange(new Pair<Long, Long>(startSliceInfo.rebasedTimeMs, stopSliceInfo.rebasedTimeMs));
					startSliceInfo.write(output.getPixels(0));
				} catch (MissingMagicNumber e) {
					throw new RuntimeException("Problem with time stamp magic numbers", e);
				}
			} else
				view.setZRange(new Pair<Long, Long>((long) startSlice, (long) stopSlice));
		}
		if (returnValue != NO_ERROR)
			throw new RuntimeException("Error " + returnValue);
	}

	private int returnValue;

	private class MethodListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (!(((ComboBoxParameter) boxParameter).getSelectionIndex() - 1 == method)) {
				method = ((ComboBoxParameter) boxParameter).getSelectionIndex() - 1;
				if (isUpdateTriggering())
					pipelineCallback.parameterValueChanged(ourRow, null, false);
			}
		}
	}

	private ParameterListener methodListener0 = new MethodListener();
	private ParameterListener methodListener1 = new ParameterListenerWeakRef(methodListener0);

	private int frameInterval;

	private class IntervalListener extends ParameterListenerAdapter {
		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			frameInterval = movieFrameInterval.getintValue();
		}
	}

	private ParameterListener intervalListener0 = new IntervalListener();
	private ParameterListener intervalListener1 = new ParameterListenerWeakRef(intervalListener0);

	private boolean waitForUpdatesToComplete = true;

	private class StartListener extends ParameterListenerAdapter {
		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			if (!isUpdateTriggering())
				return;
			if (startButton.isStillChanging()) {
				Utils.log("Was already running; restarting anyway", LogLevel.DEBUG);
				synchronized (startButton.getSemaphore()) {
					startButton.getSemaphore().notifyAll();
				}
			}
			preferredStartSlice = ((int[]) intRangeParam.getValue())[0];
			preferredStopSlice = ((int[]) intRangeParam.getValue())[1];
			final int sliceInterval = preferredStopSlice - preferredStartSlice + 1;
			startButton.setStillChanging(true);
			final int[] currentSettings = (int[]) intRangeParam.getValue();

			pipelineCallback.parameterValueChanged(ourRow, startButton, true);
			frameInterval = movieFrameInterval.getintValue();

			final Timer timer = new Timer(1000, null);

			Thread t = new Thread("Movie timer thread") {
				private boolean notInterrupted = true;
				private boolean doneOneIteration = false;

				@Override
				public void run() {
					try {
						while (startButton.isStillChanging() && (!Thread.interrupted()) && notInterrupted) {
							if (doneOneIteration /* isVideo */) {

								PluginIOHyperstackViewWithImagePlus view =
										pipelineCallback.getAuxiliaryOutputImps(ourRow).get(0);

								boolean stop = true;
								if (view.imp.getWindow() instanceof StackWindowWithToolbar)
									stop =
											((ActiveContourToolbar) ((StackWindowWithToolbar) view.imp.getWindow()).toolbar)
													.getStopVideoOnCells();

								if (stop && view.isShowingCells()) {
									break;
								}
							}
							doneOneIteration = true;
							currentSettings[0] += sliceInterval;
							currentSettings[1] += sliceInterval;
							intRangeParam.setValue(currentSettings);
							long timeWeUpdatedParam = System.currentTimeMillis();
							intRangeParam.setTimeLastChange(timeWeUpdatedParam);
							startButton.setTimeLastChange(timeWeUpdatedParam);

							synchronized (startButton.getSemaphore()) {
								startButton.getSemaphore().notifyAll();
							}

							synchronized (startButton.getSemaphore()) {
								while (notInterrupted && waitForUpdatesToComplete && startButton.isStillChanging()
										&& (startButton.getTimeLastResponseToChange() < timeWeUpdatedParam)) {
									try {
										startButton.getSemaphore().wait();
									} catch (InterruptedException e) {
										notInterrupted = false;
									}
								}
							}

							long timeToSleep = frameInterval - (System.currentTimeMillis() - timeWeUpdatedParam);
							if (timeToSleep > 0) {
								try {
									Thread.sleep(timeToSleep);
									// Utils.log("Slept for "+frameInterval,LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
								} catch (InterruptedException e) {
									Utils.printStack(e, LogLevel.DEBUG);
									break;
								}
							}
						}
					} finally {
						timer.stop();
						intRangeParam.fireValueChanged(true, true, false);
						startButton.setStillChanging(false);
						synchronized (startButton.getSemaphore()) {
							startButton.getSemaphore().notifyAll();
						}
					}
				}
			};

			final Action redrawIntRange = new AbstractAction() {
				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent action) {
					intRangeParam.fireValueChanged(true, true, false);
				}
			};

			timer.addActionListener(redrawIntRange);
			timer.start();
			t.start();

		}

	}

	private ParameterListener startListener0 = new StartListener();
	private ParameterListener startListener1 = new ParameterListenerWeakRef(startListener0);

	private class StopListener extends ParameterListenerAdapter {
		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			startButton.setStillChanging(false);
			synchronized (startButton.getSemaphore()) {
				startButton.getSemaphore().notifyAll();
			}
		}
	}

	private ParameterListener stopListener0 = new StopListener();
	private ParameterListener stopListener1 = new ParameterListenerWeakRef(stopListener0);

	private class StepListener extends ParameterListenerAdapter {
		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			if (!isUpdateTriggering())
				return;
			preferredStartSlice = ((int[]) intRangeParam.getValue())[0];
			preferredStopSlice = ((int[]) intRangeParam.getValue())[1];
			int sliceInterval = preferredStopSlice - preferredStartSlice + 1;

			if ((event != null) && ((event.getModifiers() & ActionEvent.SHIFT_MASK) != 0)) {
				sliceInterval = sliceInterval * -1;
			}

			final int[] currentSettings = (int[]) intRangeParam.getValue();

			currentSettings[0] += sliceInterval;
			currentSettings[1] += sliceInterval;
			intRangeParam.setValue(currentSettings);

			intRangeParam.fireValueChanged(false, true, false);
			pipelineCallback.parameterValueChanged(ourRow, null, false);
		}
	}

	private ParameterListener stepListener0 = new StepListener();
	private ParameterListener stepListener1 = new ParameterListenerWeakRef(stepListener0);

	private class RangeListener extends ParameterListenerAdapter {
		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			int[] currentValues = (int[]) intRangeParam.getValue();
			switch (commandName) {
				case "Reset Min":
					currentValues[2] = 1;
					intRangeParam.updateBounds(currentValues[2], currentValues[3]);
					currentValues[0] = 1;
					break;
				case "Reset Max":
					currentValues[3] = getImageInput().getDimensions().depth;
					intRangeParam.updateBounds(currentValues[2], currentValues[3]);
					currentValues[1] = currentValues[3];
					break;
				case "Reset Range":
					currentValues[2] = 1;
					currentValues[3] = getImageInput().getDimensions().depth;
					intRangeParam.updateBounds(currentValues[2], currentValues[3]);
					currentValues[0] = 1;
					currentValues[1] = currentValues[3];
					break;
				default:
					throw new IllegalStateException("Unknown command name " + commandName);
			}
			intRangeParam.setValue(currentValues);
			intRangeParam.fireValueChanged(false, true, false);

		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			if (!isUpdateTriggering())
				return;
			rangeSet = true;
			preferredStartSlice = ((int[]) intRangeParam.getValue())[0];
			preferredStopSlice = ((int[]) intRangeParam.getValue())[1];

			intRangeParam.setStillChanging(stillChanging);

			if (intRangeParam.isPipelineCalled()) {
				synchronized (intRangeParam.getSemaphore()) {
					intRangeParam.getSemaphore().notifyAll();
				}
			} else {
				intRangeParam.setPipelineCalled(true);
				pipelineCallback.parameterValueChanged(ourRow, intRangeParam, true);
			}

			if (!stillChanging)
				intRangeParam.setPipelineCalled(false);

		}
	}

	private ParameterListener rangeListener0 = new RangeListener();
	private ParameterListener rangeListener1 = new ParameterListenerWeakRef(rangeListener0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] {
				methodListener1,
				new SplitParameterListener(new ParameterListener[] { rangeListener1, intervalListener1, startListener1,
						stopListener1, stepListener1 }) };
	}

	@Override
	public void setParameters(AbstractParameter[] param) {
		boxParameter = param[0];
		if (param[1] instanceof IntRangeParameter) { // for retrocompatibility
			intRangeParam = (IntRangeParameter) param[1];
			splitRangeMovie =
					new SplitParameter(new Object[] { intRangeParam, movieFrameInterval, startButton, stopButton });
		} else {
			SplitParameter split = (SplitParameter) param[1];
			Object[] params = (Object[]) split.getValue();

			intRangeParam = (IntRangeParameter) params[0];
			intRangeParam.addPluginListener(rangeListener1);
			movieFrameInterval = (IntParameter) params[1];
			movieFrameInterval.addPluginListener(intervalListener1);
			startButton = (ActionParameter) params[2];
			startButton.addPluginListener(startListener1);
			stopButton = (ActionParameter) params[3];
			stopButton.addPluginListener(stopListener1);
			stepButton = (ActionParameter) params[4];
			stepButton.addPluginListener(stepListener1);
			splitRangeMovie = split;
		}

		frameInterval = movieFrameInterval.getintValue();

		if (!(((ComboBoxParameter) boxParameter).getSelectionIndex() - 1 == method)) {
			method = ((ComboBoxParameter) boxParameter).getSelectionIndex() - 1;
		}
		preferredStartSlice = ((int[]) intRangeParam.getValue())[0];
		preferredStopSlice = ((int[]) intRangeParam.getValue())[1];
		// TODO there is a conflict between having the slices just be the whole range of the stack we're working on, or
		// the range defined
		// by the parameters; need to think of a way of storing that preference on an individual basis

		rangeSet = true;// TODO Need to do something more refined than this

	}

	private AbstractParameter boxParameter = new ComboBoxParameter("Projection method", "", METHODS, "Max intensity",
			false, methodListener1);
	private IntRangeParameter intRangeParam = new IntRangeParameter("Range to project", "Slice projection range.", 1,
			100, 1, 100, true, true, rangeListener1, null);

	private IntParameter movieFrameInterval = new IntParameter("Interval between frames", "In milliseconds", 100, 0,
			2000, true, true, intervalListener1);
	private ActionParameter startButton = new ActionParameter("Start animation", "", true, startListener1);
	private ActionParameter stopButton = new ActionParameter("Stop animation", "", true, stopListener1);
	private ActionParameter stepButton = new ActionParameter("Animation step", "", true, stepListener1);

	private SplitParameter splitRangeMovie = new SplitParameter(new Object[] { intRangeParam, movieFrameInterval,
			startButton, stopButton, stepButton });

	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { boxParameter, splitRangeMovie };
		return paramArray;
	}

	@Override
	public String operationName() {
		return "Z Projection";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return Z_PROJECTS + ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL + PARALLELIZE_WITH_NEW_INSTANCES;
	}

	private boolean rangeSet = false;
	private int preferredStartSlice;
	private int preferredStopSlice;

	public int startSlice = 1;
	public int stopSlice = 1;

	private int increment = 1;
	private int sliceCount;

	public int method = MAX_METHOD;

	public void setMethod(int projMethod) {
		method = projMethod;
	}

	public void doRGBProjection() { // GO BACK TO THIS
		/*
		 * RGBStackSplitter splitter = new RGBStackSplitter();
		 * splitter.split(source.getStack(), true);
		 * ImagePlus red = new ImagePlus("Red", splitter.red);
		 * ImagePlus green = new ImagePlus("Green", splitter.green);
		 * ImagePlus blue = new ImagePlus("Blue", splitter.blue);
		 * ImagePlus saveImp = source;
		 * source = red;
		 * //color = "(red)"; doProjection();
		 * ImagePlus red2 = projImage;
		 * source = green;
		 * //color = "(green)"; doProjection();
		 * ImagePlus green2 = projImage;
		 * source = blue;
		 * //color = "(blue)"; doProjection();
		 * ImagePlus blue2 = projImage;
		 * int w = red2.getWidth(), h = red2.getHeight(), d = red2.getStackSize();
		 * RGBStackMerge merge = new RGBStackMerge();
		 * ImageStack stack = merge.mergeStacks(w, h, d, red2.getStack(), green2.getStack(), blue2.getStack(), true);
		 * source = saveImp;
		 * projImage = new ImagePlus(makeTitle(), stack);
		 */
	}

	long lastExecutionTime;

	public int numberOfThreadsToUse = 8;

	private transient float[][] outputArrays = new float[numberOfThreadsToUse][];
	private transient RayFunction[] rayFuncArray = new RayFunction[numberOfThreadsToUse];
	private transient Runnable[] tasks = new Runnable[numberOfThreadsToUse];
	private transient Future<?>[] futures = new Future[numberOfThreadsToUse];

	private transient AtomicInteger sliceRegistry = new AtomicInteger();
	private transient AtomicInteger threadId = new AtomicInteger();

	private volatile boolean abort;

	/**
	 * Performs actual projection using specified method.
	 */
	public void doProjection(final IPluginIOStack input, IPluginIOStack output, final ProgressReporter progress,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {
		sliceCount = 0;

		if (progress != null) {
			if (progress.isIndeterminate())
				progressSetIndeterminateThreadSafe(progress, false);
			indeterminateProgress = false;
			if (progress.getValue() != 0)
				progressSetValueThreadSafe(progress, 0);
		}

		final PixelType pType = input.getPixelType();

		for (int slice = startSlice; slice <= stopSlice; slice += increment)
			sliceCount++;
		if (method == MEDIAN_METHOD) {
			doMedianProjection(input, output);
			return;
		}
		if (method == MEDIAN_METHOD_NON_0) {
			doMedianProjection_non_0(input, output);
			return;
		}

		int arrayLength = ((float[]) output.getPixels(0)).length;
		for (int i = 0; i < numberOfThreadsToUse; i++) {
			if ((outputArrays[i] == null) || (outputArrays[i].length != arrayLength)) {
				outputArrays[i] = new float[arrayLength];
			}
			rayFuncArray[i] =
					RayFunction.getRayFunction(method, outputArrays[i], sliceCount, -Float.MAX_VALUE, Float.MAX_VALUE);
			rayFuncArray[i].preProcess();
		}
		RayFunction rayFunc =
				RayFunction.getRayFunction(method, (float[]) output.getPixels(0), sliceCount, -Float.MAX_VALUE,
						Float.MAX_VALUE);

		rayFunc.preProcess();

		threadId.set(0);
		sliceRegistry.set(startSlice);

		abort = false;
		final float progressMultiplyingFactor = 100.0f / ((float) stopSlice - startSlice);

		final boolean isVirtual = input.isVirtual();
		final ImageAccessor imageAccessor = isVirtual ? input.getImageAccessor() : null;

		for (int i = 0; i < numberOfThreadsToUse; i++) {
			tasks[i] =
					() -> {
						try {
							int ourId = threadId.getAndIncrement();
							int sliceModulo10 = 0;

							float[] floatPixels = null;
							byte[] bytePixels = null;
							short[] shortPixels = null;
							SliceAccessor sliceAccessor = null;
							if (isVirtual) {
								if (pType == PixelType.FLOAT_TYPE) {
									floatPixels = new float[input.getWidth() * input.getHeight()];
								} else if (pType == PixelType.BYTE_TYPE) {
									bytePixels = new byte[input.getWidth() * input.getHeight()];
								} else if (pType == PixelType.SHORT_TYPE) {
									shortPixels = new short[input.getWidth() * input.getHeight()];
								}
								sliceAccessor = imageAccessor.getSlicesAccessor();
							}

							for (int n = sliceRegistry.getAndAdd(increment); n <= stopSlice; n =
									sliceRegistry.getAndAdd(increment)) {

								if (isVirtual) {
									if (pType == PixelType.FLOAT_TYPE) {
										sliceAccessor.copyPixelSliceIntoArray(n - 1, ImageAccessor.DONT_CACHE_PIXELS,
												floatPixels);
										projectSlice(floatPixels, rayFuncArray[ourId], pType);
									} else if (pType == PixelType.BYTE_TYPE) {
										sliceAccessor.copyPixelSliceIntoArray(n - 1, ImageAccessor.DONT_CACHE_PIXELS,
												bytePixels);
										projectSlice(bytePixels, rayFuncArray[ourId], pType);
									} else if (pType == PixelType.SHORT_TYPE) {
										sliceAccessor.copyPixelSliceIntoArray(n - 1, ImageAccessor.DONT_CACHE_PIXELS,
												shortPixels);
										projectSlice(shortPixels, rayFuncArray[ourId], pType);
									}
								} else
									projectSlice(input.getPixels(n - 1), rayFuncArray[ourId], pType);

								if (abort)
									return;
								if ((progress != null) && (sliceModulo10++ == 10)) {
									if (Thread.interrupted()) {
										returnValue = THREAD_INTERRUPTED;
										return;
									}
									int ourProgress = (int) ((n - startSlice) * progressMultiplyingFactor);
									if (ourProgress > progress.getValue())
										progressSetValueThreadSafe(progress, ourProgress); // not perfect but at least
																							// does not require
																							// synchronization
									sliceModulo10 = 0;
								}
							}
						} catch (Exception e) {
							abort = true;
							returnValue = 1;
							Utils.printStack(e);
						}
					};
			futures[i] = threadPool.submit(tasks[i], 0);
		}

		for (int i = 0; i < numberOfThreadsToUse; i++) {
			try {
				futures[i].get();
			} catch (InterruptedException e) {
				abort = true;
				Utils.printStack(e);
				i--;// Wait for all the tasks to complete (should be quick since we set the abort flag)
			} catch (ExecutionException e) {
				abort = true;
				returnValue = 1;
				Utils.printStack(e);
			}
		}

		if (abort)
			throw new InterruptedException();

		// Finish up projection.
		if (method == SUM_METHOD) {
		} else if (method == SD_METHOD) {
			for (int i = 0; i < numberOfThreadsToUse; i++) {
				rayFuncArray[i].postProcess();
			}
		} else {
			for (int i = 0; i < numberOfThreadsToUse; i++) {
				rayFuncArray[i].postProcess();
			}
		}

		// Now project the projections

		for (int i = 0; i < numberOfThreadsToUse; i++) {
			projectSlice(outputArrays[i], rayFunc, PixelType.FLOAT_TYPE);
		}

		// Finish up projection.
		if (method == SUM_METHOD) {
		} else if (method == SD_METHOD) {
			rayFunc.postProcess();
		} else {
			rayFunc.postProcess();
		}

	}

	private void doMedianProjection(IPluginIOStack input, IPluginIOStack output) {
		float[] values = new float[sliceCount];
		for (int y = 0; y < input.getHeight(); y++) {
			for (int x = 0; x < input.getWidth(); x++) {
				for (int i = 0; i < sliceCount; i++)
					values[i] = input.getPixelValue(x, y, i);
				output.setPixelValue(x, y, 0, median(values));
			}
		}
	}

	private void doMedianProjection_non_0(IPluginIOStack input, IPluginIOStack output) {
		float[] values = new float[sliceCount];
		for (int y = 0; y < input.getHeight(); y++) {
			for (int x = 0; x < input.getWidth(); x++) {
				int value_count = 0;
				for (int i = 0; i < sliceCount; i++) {
					float v = input.getPixelValue(x, y, i);
					if (v > 0.0) {
						values[value_count++] = v;
					}
				}
				if (value_count > 0) {
					float[] new_array = new float[value_count];
					System.arraycopy(values, 0, new_array, 0, value_count);
					output.setPixelValue(x, y, 0, median(new_array));
				}// 0 was 1
				else {
					output.setPixelValue(x, y, 0, 0.0f);// 0 was 1
				}
			}
		}
	}

	private float median(float[] a) {
		sort(a);
		int length = a.length;
		if ((length & 1) == 0)
			return (a[length / 2 - 1] + a[length / 2]) / 2f; // even
		else
			return a[length / 2]; // odd
	}

	private void sort(float[] a) {
		if (!alreadySorted(a))
			sort(a, 0, a.length - 1);
	}

	private void sort(float[] a, int from, int to) {
		int i = from, j = to;
		float center = a[(from + to) / 2];
		do {
			while (i < to && center > a[i])
				i++;
			while (j > from && center < a[j])
				j--;
			if (i < j) {
				float temp = a[i];
				a[i] = a[j];
				a[j] = temp;
			}
			if (i <= j) {
				i++;
				j--;
			}
		} while (i <= j);
		if (from < j)
			sort(a, from, j);
		if (i < to)
			sort(a, i, to);
	}

	private static boolean alreadySorted(float[] a) {
		for (int i = 1; i < a.length; i++) {
			if (a[i] < a[i - 1])
				return false;
		}
		return true;
	}
}
