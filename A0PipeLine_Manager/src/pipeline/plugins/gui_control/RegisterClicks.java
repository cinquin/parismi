/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.gui_control;

// adapted from fiji
/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOFileWrapper;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ActionParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.MouseEventPlugin;
import pipeline.plugins.PluginInfo;
import pipeline.plugins.ResettablePlugin;

/**
 * This plugin attaches to a view of its input and notifies the plugin associated with the output when clicks are made.
 * TODO For now the deletion clicks (clicks performed with the shift key) are not recorded.
 *
 */
//
@PluginInfo(obsolete = true)
public class RegisterClicks extends FourDPlugin implements MouseEventPlugin, ResettablePlugin,
		AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Attach to an open image display window and send click events to target plugin";
	}

	private static boolean coalesceUpdates = true;

	private Thread workerThread = null;

	/**
	 * If bufferClicks is true, hold the clicks in a buffer to send to the target plugin later. This is used for example
	 * to
	 * seed a number of active contours, and start them at the same time to maximize the usefulness of collision
	 * detection.
	 */
	private boolean bufferClicks = false;

	@Override
	public String operationName() {
		return "RegisterClicks";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return CUSTOM + DONT_ALLOCATE_OUTPUT_PIXELS + NO_IMP_OUTPUT;
	}

	@Override
	public void cleanup() {
		if (workerThread != null)
			workerThread.interrupt();
		list = Collections.synchronizedList(new LinkedList<ClickedPoint>());
	}

	/**
	 * List of clicked points that have not yet been processed by the worker thread
	 */
	private transient List<ClickedPoint> list = Collections.synchronizedList(new LinkedList<ClickedPoint>());

	/**
	 * Used to sort clicks into groups, and hold them in the list until they're ready to be passed
	 * all together to the destination plugin. A new group is started each time the user double-clicks
	 * while holding the right modifier key (option/alt).
	 */

	private transient volatile int clickGroup = 1;

	/**
	 * List of all the points that have been clicked; the list can be reset by the user.
	 * This is useful to reproduce the segmentation of an image when it has had manual input from the user.
	 */
	private ArrayList<ClickedPoint> accumulatedList;

	private volatile transient boolean flushButtonPressed = false;

	private transient PluginIOHyperstackViewWithImagePlus imageView = null;

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			final PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		PluginIOHyperstackViewWithImagePlus newView = ((IPluginIOImage) getInput()).getImp();
		if (newView != imageView) {
			if (imageView != null) {
				imageView.removeListener(this);
			}
			newView.addListener(this);
			imageView = newView;
		}

		if (list == null) {
			list = Collections.synchronizedList(new LinkedList<ClickedPoint>());
		}

		bufferedClicksAsText.setValue("");
		bufferedClicksAsText.fireValueChanged(false, true, true);

		if (accumulatedList == null) {
			accumulatedList = new ArrayList<>();
			accumulatedClicksAsText.privateStorage = accumulatedList;
		}

		if ((workerThread == null) || (!workerThread.isAlive())) {
			workerThread = new Thread("Register click worker thread from row " + ourRow) {
				@Override
				public void run() {
					ArrayList<ClickedPoint> pointsToPass;
					boolean done = false;
					boolean nothingToDo = false;
					while (!done) {
						synchronized (list) {
							while (nothingToDo || list.isEmpty() || (bufferClicks && !flushButtonPressed)) {
								try {
									list.wait();
									// Utils.log("woken up at time "+System.currentTimeMillis(),LogLevel.DEBUG);
									nothingToDo = false;
								} catch (InterruptedException e) {
									Utils.printStack(e);
									done = true;
									break;
								}
							}
							// Utils.log("Done waiting "+bufferClicks+" "+flushButtonPressed,LogLevel.VERBOSE_DEBUG);
							if (done)
								return;
							boolean respondingToFlush = flushButtonPressed;
							flushButtonPressed = false;

							// Extract points from the list that should not be held because
							// we're waiting for the user to complete a group (for example
							// a group of cells to be merged); these points should have a
							// clickGroup of 0 and therefore always be smaller than the
							// current value of clickGroup (which is initialized at 1).
							// Keep points that should be held in the list until the user
							// says it's time to pass them.

							ArrayList<ClickedPoint> pointsToKeep = new ArrayList<>();
							pointsToPass = new ArrayList<>();

							for (ClickedPoint aList : list) {
								if (respondingToFlush || (aList.clickGroup < clickGroup)) {
									pointsToPass.add(aList);
								} else {
									pointsToKeep.add(aList);
								}
							}

							if (pointsToPass.size() > 0) {
								list.clear();
								list.addAll(pointsToKeep);

								bufferedClicksAsText.setValue("");
								bufferedClicksAsText.fireValueChanged(false, true, true);
							} else {
								nothingToDo = true;
							}
						}

						if (pointsToPass.size() > 0) {
							try {
								int rowToCall = pipelineCallback.getOwnerOfOurOutput(ourRow);
								pipelineCallback.passClickToRow(rowToCall, new PluginIOCells(pointsToPass), false,
										coalesceUpdates);// don't attempt to interrupt row update
							} catch (Exception e) {
								Utils.printStack(e);
							}
						}

					}
				}
			};
			workerThread.start();
		}
	}

	private class BufferedClicksAsTextListener extends ParameterListenerAdapter {
	}

	private ParameterListener bufferedClicksAsTextListener0 = new BufferedClicksAsTextListener();
	private ParameterListener bufferedClicksAsTextListener1 = new ParameterListenerWeakRef(
			bufferedClicksAsTextListener0);

	private class AccumulatedClicksAsTextListener extends ParameterListenerAdapter {
	}

	private ParameterListener accumulatedClicksAsTextListener0 = new AccumulatedClicksAsTextListener();
	private ParameterListener accumulatedClicksAsTextListener1 = new ParameterListenerWeakRef(
			accumulatedClicksAsTextListener0);

	private class BufferClicksListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			bufferClicks = (Boolean) ((Object[]) bufferClicksParameter.getValue())[0];
			if (!bufferClicks) {
				synchronized (list) {
					list.notify();
				}
			}
		}
	}

	private ParameterListener bufferClicksListener0 = new BufferClicksListener();
	private ParameterListener bufferClicksListener1 = new ParameterListenerWeakRef(bufferClicksListener0);

	private class FlushBufferedClicksListener extends ParameterListenerAdapter {

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			Utils.log("Flush at time " + System.currentTimeMillis(), LogLevel.DEBUG);
			if (!workerThread.isAlive())
				Utils.log("WORKER THREAD DEAD", LogLevel.ERROR);
			flushButtonPressed = true;
			synchronized (list) {
				list.notifyAll();
			}
		}
	}

	private ParameterListener flushBufferedClicksListener0 = new FlushBufferedClicksListener();
	private ParameterListener flushBufferedClicksListener1 = new ParameterListenerWeakRef(flushBufferedClicksListener0);

	private class ClearAccumulateListListener extends ParameterListenerAdapter {

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			reset();

		}
	}

	private ParameterListener clearAccumulateListListener0 = new ClearAccumulateListListener();
	private ParameterListener clearAccumulateListListener1 = new ParameterListenerWeakRef(clearAccumulateListListener0);

	private AbstractParameter bufferClicksParameter =
			new BooleanParameter(
					"Buffer clicks",
					"If checked, accumulate clicks without passing them to the target plugin. When \"Flush Clicks\" is pressed, send them all together to the target plugin",
					false, true, bufferClicksListener1);
	private AbstractParameter flushBufferedClicksButton = new ActionParameter("Flush buffered clicks",
			"Sends all the clicks that are being held in the buffer to the destination plugin in one go", true,
			flushBufferedClicksListener1);
	private AbstractParameter bufferedClicksAsText = new TextParameter("Clicks in buffer",
			"Clicks waiting to be sent to target plugin when \"Flush buffered clicks\" is pressed.", "", false,
			bufferedClicksAsTextListener1, null);

	private AbstractParameter clearAccumulatedListButton = new ActionParameter("Clear accumulated list",
			"Clears the list of all the clicks that have been recorded", true, clearAccumulateListListener1);
	private AbstractParameter accumulatedClicksAsText = new TextParameter("Recorded clicks",
			"Clicks that have been recorded and will be sent to the target plugin whenever the present plugin is run.",
			"", false, accumulatedClicksAsTextListener1, null);
	private AbstractParameter recordClicks = new BooleanParameter("Record clicks",
			"If checked, all clicks are recorded and show in the accumulated list", false, true, null);

	private AbstractParameter splitDirectoryAndFile = null;
	private AbstractParameter splitBufferClicksAndbufferedClicksAsText = null;

	private ParameterListener splitA0 = new SplitParameterListener(new ParameterListener[] { bufferClicksListener1,
			flushBufferedClicksListener1, bufferedClicksAsTextListener1 });
	private ParameterListener splitA1 = new ParameterListenerWeakRef(splitA0);
	private ParameterListener splitB0 = new SplitParameterListener(new ParameterListener[] {
			accumulatedClicksAsTextListener1, clearAccumulateListListener1, null });
	private ParameterListener splitB1 = new ParameterListenerWeakRef(splitB0);

	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { splitA1, splitB1 };
	}

	@Override
	public AbstractParameter[] getParameters() {

		if (splitBufferClicksAndbufferedClicksAsText == null) {
			splitBufferClicksAndbufferedClicksAsText =
					new SplitParameter(new Object[] { bufferClicksParameter, flushBufferedClicksButton,
							bufferedClicksAsText });
		}
		if (splitDirectoryAndFile == null) {
			splitDirectoryAndFile =
					new SplitParameter(
							new Object[] { accumulatedClicksAsText, clearAccumulatedListButton, recordClicks });
		}
		AbstractParameter[] paramArray = { splitBufferClicksAndbufferedClicksAsText, splitDirectoryAndFile };
		return paramArray;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setParameters(AbstractParameter[] param) {
		splitBufferClicksAndbufferedClicksAsText = param[0];
		Object[] splitParameters = (Object[]) (splitBufferClicksAndbufferedClicksAsText).getValue();
		bufferClicksParameter = (AbstractParameter) splitParameters[0];
		bufferClicks = (Boolean) ((Object[]) bufferClicksParameter.getValue())[0];
		flushBufferedClicksButton = (AbstractParameter) splitParameters[1];
		bufferedClicksAsText = (AbstractParameter) splitParameters[2];

		splitDirectoryAndFile = param[1];
		splitParameters = (Object[]) (splitDirectoryAndFile).getValue();
		accumulatedClicksAsText = (AbstractParameter) splitParameters[0];
		clearAccumulatedListButton = (AbstractParameter) splitParameters[1];

		if (splitParameters.length > 2) {
			recordClicks = (AbstractParameter) splitParameters[2];
		} else
			recordClicks = null;

		accumulatedList = (ArrayList<ClickedPoint>) accumulatedClicksAsText.privateStorage;
		if (accumulatedList == null) {
			// accumulatedList=Collections.synchronizedList(new ArrayList<ClickedPoint>());
			accumulatedList = new ArrayList<>();
			accumulatedClicksAsText.privateStorage = accumulatedList;
		}
	}

	/**
	 * Path to temporary file that stores all the clicks that have been received, including coordinates and modifiers
	 */
	private transient String filePathForClickAccumulation = null;

	/**
	 * Path to temporary file that stores all the clicks that have been received without modifiers so it can be
	 * read directly by C plugins.
	 */
	private transient String filePathForClickAccumulationWithoutModifiers = null;

	/**
	 * Print a String representation of a point (coordinates and modifiers) to a buffered writer.
	 * 
	 * @param p
	 *            point
	 * @param writer
	 * @param outputModifiers
	 *            If true, output x y z modifiers, if not output y x z (NOTE y first)
	 *            z starts at 0
	 * @throws IOException
	 */
	private static void printPoint(ClickedPoint p, BufferedWriter writer, boolean outputModifiers) throws IOException {
		if (outputModifiers)
			writer.write(p.x + "\t" + p.y + "\t" + (p.z - 1) + "\t" + p.modifiers + "\n");
		else
			writer.write(p.y + "\t" + p.x + "\t" + (p.z - 1) + "\n");
	}

	/**
	 * 
	 * @param p
	 *            point
	 * @return String representation of point
	 */
	private static String printPoint(ClickedPoint p) {
		return (p.x + "\t" + p.y + "\t" + p.z + "\t" + p.modifiers + "\n");
	}

	private static String printPointList(List<ClickedPoint> list) {
		StringBuffer s = new StringBuffer();
		synchronized (list) {
			for (ClickedPoint p : list) {
				s.append(printPoint(p));
			}
		}
		return s.toString();
	}

	/**
	 * Print a String representation of a list of points (coordinates and modifiers) to a buffered writer.
	 * The first line contains the number of points present in the file.
	 * 
	 * @param list
	 *            List of points
	 * @param writer
	 * @throws IOException
	 */
	private static void printPointList(List<ClickedPoint> list, BufferedWriter writer, boolean outputModifiers)
			throws IOException {
		writer.write(list.size() + "\n");
		for (ClickedPoint p : list) {
			printPoint(p, writer, outputModifiers);
		}
	}

	private Thread t;

	@Override
	public String[] getInputLabels() {
		return new String[0];
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "pointCoordinates.txt", "pointCoordinatesWithModifiers.txt" };
	}

	@Override
	public void reset() {
		filePathForClickAccumulation = null;
		accumulatedList.clear();
		accumulatedClicksAsText.setValue("");
		accumulatedClicksAsText.fireValueChanged(false, true, true);
		if (filePathForClickAccumulationWithoutModifiers != null) {
			try (FileWriter outFile = new FileWriter(filePathForClickAccumulationWithoutModifiers, false)) {
				outFile.write("");
			} catch (IOException e1) {
				Utils.printStack(e1);
				return;
			}
			filePathForClickAccumulationWithoutModifiers = null;
		}
	}


	@Override
	public int mouseClicked(final PluginIO clickedPoints, boolean inputHasChanged, MouseEvent generatingEvent) {
		t = new Thread("Mouse click processing thread for RegisterClicks") {
			@Override
			public void run() {

				// Utils.log("Adding click at time "+System.currentTimeMillis(),LogLevel.DEBUG);
				synchronized (list) {
					list.addAll(((PluginIOCells) clickedPoints).getPoints());
					list.notify();
				}
				// Utils.log("Done adding click at time "+System.currentTimeMillis(),LogLevel.DEBUG);

				if ((recordClicks == null) || ((Boolean) ((Object[]) recordClicks.getValue())[0]))
					synchronized (accumulatedList) {
						accumulatedList.addAll(((PluginIOCells) clickedPoints).getPoints());
						bufferedClicksAsText.setValue(printPointList(list));
						bufferedClicksAsText.fireValueChanged(false, true, true);
						accumulatedClicksAsText.setValue(printPointList(accumulatedList));
						accumulatedClicksAsText.fireValueChanged(false, true, true);

						if (filePathForClickAccumulation == null) {
							File f;
							File fWithoutModifiers;
							try {
								f = File.createTempFile(getOutputLabels()[0], ".txt");
								fWithoutModifiers = File.createTempFile(getOutputLabels()[1], ".txt");
							} catch (IOException e1) {
								Utils.printStack(e1);
								return;
							}
							filePathForClickAccumulation = f.getPath();
							filePathForClickAccumulationWithoutModifiers = fWithoutModifiers.getPath();
						}

						Map<String, IPluginIO> auxOutputs = getOutputs();
						auxOutputs.clear();
						auxOutputs.put(getOutputLabels()[1], new PluginIOFileWrapper(new File(
								filePathForClickAccumulation)));
						auxOutputs.put(getOutputLabels()[0], new PluginIOFileWrapper(new File(
								filePathForClickAccumulationWithoutModifiers)));

						File file1 = new File(filePathForClickAccumulation);
						File file2 = new File(filePathForClickAccumulationWithoutModifiers);
						try (FileWriter outFile = new FileWriter(file1, false); FileWriter outFile2 =
								new FileWriter(file2, false); BufferedWriter writer = new BufferedWriter(outFile); BufferedWriter writer2 =
								new BufferedWriter(outFile2)) {

							printPointList(accumulatedList, writer, true);
							printPointList(accumulatedList, writer2, false);
						} catch (IOException e1) {
							Utils.printStack(e1);
						}
					}
			}
		};
		if (!workerThread.isAlive())
			Utils.log("WORKER THREAD DEAD", LogLevel.ERROR);
		// Utils.log("Starting click thread at time "+System.currentTimeMillis(),LogLevel.DEBUG);
		t.start();
		// Utils.log("Started click thread at time"+System.currentTimeMillis(),LogLevel.DEBUG);
		return 0;
	}

	@Override
	public void processClicks() {
		// flush the buffer, and process any clicks held back becaues of clickGroup
		flushBufferedClicksListener0.buttonPressed(null, null, null);
	}

}
