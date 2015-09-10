/*******************************************************************************
 * Parismi v0.1 Copyright (c) 2009-2015 Cinquin Lab. All rights reserved. This code is made available under a dual
 * license: the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TooManyListenersException;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.ButtonGroup;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.InputSource;

import com.beust.jcommander.JCommander;
import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.Menus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.util.StringSorter;
import pipeline.GUI_utils.ColumnHeaderToolTips;
import pipeline.GUI_utils.JTableWithStripes;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.MultiRenderer;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.ProgressRenderer;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.ImageAccessor;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.tiff_read_write.TIFFFileAccessor;
import pipeline.external_plugin_interfaces.LinkToExternalProgram;
import pipeline.external_plugin_interfaces.RemoteMachine;
import pipeline.misc_util.DefaultExceptionHandler;
import pipeline.misc_util.DylibInfo;
import pipeline.misc_util.FakeFileForPipelineRef;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressBarWrapper;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.SwingMemory;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.ImageOpenFailed;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.drag_and_drop.DnDUtils;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameter_cell_views.OneColumnJTable;
import pipeline.parameter_cell_views.TextBox;
import pipeline.parameter_cell_views.TwoColumnJTable;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.DropAcceptingParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.IntRangeParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.PipelineParameter;
import pipeline.parameters.RowOrFileTextReference;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;
import pipeline.parameters.TwoColumnTableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.BasePipelinePlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.IPluginShell;
import pipeline.plugins.MouseEventPlugin;
import pipeline.plugins.PipelinePlugin;
import pipeline.plugins.PluginHolder;
import pipeline.plugins.PluginInfo;
import pipeline.plugins.ResettablePlugin;
import pipeline.plugins.ThreeDPlugin;
import pipeline.plugins.TwoDPlugin;
import pipeline.plugins.cell_manipulation.SVMSuppress;
import pipeline.plugins.flow_control.Pause;
import pipeline.plugins.input_output.BatchOpen;
import pipeline.plugins.input_output.BatchOpenV2;
import pipeline.plugins.input_output.LazyCopy;
import pipeline.plugins.input_output.LazyCopy.DimensionMismatchException;
import pipeline.plugins.input_output.SaveTable;

/**
 * This is the main class, which implements most of the pipeline task scheduling, and provides the glue between the
 * pipeline, the actual image processing plugins (found in {@link pipeline.plugins}), their parameters (found in
 * {@link pipeline.parameters}), and the GUI for representation and manipulation of those parameters (implemented by the
 * main table, with renderers in the {@link pipeline.parameter_cell_views} package).
 * 
 * TODO This class is in need of overhauling to change the GUI to a graph-based representation, and to cleanly
 * separate it from the pipeline logic (which should be further modularized).
 * 
 * @see pipeline.plugins
 * @see pipeline.parameters.AbstractParameter
 * @see pipeline.parameters
 * @see pipeline.parameter_cell_views
 * 
 */
public class A0PipeLine_Manager implements PlugIn {
	private static final int COLUMN_NUMBER = 0;
	private static final int INPUT_NAME_FIELD = 1;
	private static final int WORK_ON_CHANNEL_FIELD = 2;
	private static final int SHOW_IMAGE = 3;
	private static final int KEEP_C_PLUGIN_ALIVE = 4;
	private static final int USE_STEP = 5;
	private static final int RESET_RANGE = 6;
	private static final int PLUGIN_NAME_FIELD = 7;
	private static final int PLUGIN_PARAM_1_FIELD = 8;
	private static final int PLUGIN_PARAM_2_FIELD = 9;
	private static final int OUTPUT_NAME_FIELD = 10;
	private static final int OUT_CHANNELS_FIELD = 11;
	private static final int IS_UPDATING = 12;
	private static final int UPDATE_QUEUED = 13;
	private static final int PERCENT_DONE = 14;
	private static final int AUXILIARY_INPUTS = 15;
	private static final int AUXILIARY_OUTPUTS = 16;
	private static final int WORKER_THREAD = 17;
	private static final int QUEUED_WORKER_THREAD = 18;
	private static final int OUTPUT_LOCKS = 19;
	private static final int PLUGIN_INSTANCE = 20;
	private static final int Z_PROJ = 21;
	private static final int ROW_HEIGHT = 22;
	private static final int PLUGIN_INPUTS = 23;
	private static final int PLUGIN_OUTPUTS = 24;
	private static final int OUTPUT_XML = 25;
	@SuppressWarnings("unused")
	private static final int INPUT_XML = 26;
	private static final int AUXILIARY_OUTPUT_IMPS = 27;
	public static final int COMPUTING_ERROR = 28;
	private static final int LINK_TO_EXTERNAL_PROGRAM = 29;
	private static final int COMPUTED_INPUTS = 30;
	private static final int COMPUTED_OUTPUTS = 31;
	private static final int LAST_TIME_RUN = 32;
	private static final int IMP_FOR_DISPLAY = 33;
	private static final int TABLE_WIDTH = 34;
	private static final int LAST_VISIBLE_COLUMN = 19;

	private static final int PLUGIN_EXPERT_LEVEL_DISPLAY = 1;

	private static String pattern3 = Pattern.quote("pipeline.misc__util.IntrospectionParameters_-3");
	private static String pattern4 = Matcher.quoteReplacement("pipeline.misc__util.IntrospectionParameters_-4");

	private static boolean stopOnError = true;

	private TableSelectionDemo table;

	private JPopupMenu ImageListMenu;
	private JPopupMenu pluginListMenu;

	private int imageListMenuXCellIndex;
	private int imageListMenuYCellIndex;

	public static int setup(String arg, ImagePlus img) {
		Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
		return 0;
	}

	private String[] arguments;

	// Structure of the table and its model were adapted from
	// http://java.sun.com/docs/books/tutorial/uiswing/examples/components/TableRenderDemoProject/src/components/TableRenderDemo.java
	public class TableSelectionDemo extends JPanel implements ActionListener {

		private String workingDirectory;
		private static final long serialVersionUID = 1L;
		private JTable table1;
		private JScrollPane sp;
		private JCheckBox updatePipelineButton;
		private JCheckBox cancelUponChangeButton;
		private JCheckBox updateCurrentStepButton;
		private JCheckBox globalCancelUponChangeButton;
		private JCheckBox suppressLog, suppressWarningPopups;
		private JComboBox<String> logLevel;
		private JCheckBox keepWindowOnTopButton;

		private ButtonGroup controlGroupButton;
		private JButton toFrontButton, deleteButton, newButton, updateImageButton, cancelUpdatesButton, runButton,
				openNextButton, saveTableButton, loadTableButton;
		private JTextArea remoteMachine;
		private int mousePressedRow;

		private ConcurrentHashMap<String, PluginHolder> plugins = new ConcurrentHashMap<>();
		private String[] pluginNames, longPluginNames;

		void progressSetIndeterminateThreadSafe(final ProgressBarWrapper p, final boolean indeterminate, int row) {
			ProgressRenderer.progressSetIndeterminateAndAnimate(p, indeterminate, new PluginCallBack(), row);
		}

		void progressSetValueThreadSafe(final ProgressReporter p, final int value) {
			SwingUtilities.invokeLater(() -> p.setValue(value));
		}

		public void updateAllImpReferences(Map<String, IPluginIO> map, ImagePlus oldImp, ImagePlus newImp) {
			if (map == null)
				return;
			map.values().stream().filter(input -> input instanceof IPluginIOImage).forEach(input -> {
				PluginIOHyperstackViewWithImagePlus impWithMetadata = ((IPluginIOImage) input).getImp();
				if (impWithMetadata.imp == oldImp) {
					impWithMetadata.imp = newImp;
				}
			});
		}

		public void updateAllImpReferences(ImagePlus oldImp, ImagePlus newImp) {
			Object[] data = ((MyTableModel) table1.getModel()).data;
			for (Object element : data) {
				Object[] row = (Object[]) element;
				PipelinePlugin plugin = (PipelinePlugin) row[PLUGIN_INSTANCE];
				if (plugin == null)
					continue;
				updateAllImpReferences(plugin.getInputs(), oldImp, newImp);
				updateAllImpReferences(plugin.getOutputs(), oldImp, newImp);
			}
		}

		/**
		 * Select numberToAdd more elements in the list of output channels for the plugin at row tableRow. This is
		 * called when there are more selected input channels than selected output channels, if the plugin specifies it
		 * has one output channel per input channel.
		 * 
		 * @param table11
		 * @param tableRow
		 * @param numberToAdd
		 */
		public void expandOutputChannelSelection(JTable table11, int tableRow, final int numberToAdd) {
			Object[] data = ((MyTableModel) table11.getModel()).data;
			Object[] theRow = (Object[]) data[tableRow];
			int[] outputChannelSelection = ((TableParameter) theRow[OUT_CHANNELS_FIELD]).getSelection();

			int[] newSelection = new int[outputChannelSelection.length + numberToAdd];
			int numberAdded = 0;
			int newIndex = 0;
			int oldIndex = 0;
			while (numberAdded < numberToAdd) {
				if ((oldIndex < outputChannelSelection.length) && (outputChannelSelection[oldIndex] == newIndex)) {
					// index is already part of the selection; don't do anything other than copy it
					oldIndex++;
				} else {
					numberAdded++;

				}
				newSelection[newIndex] = newIndex;
				newIndex++;
			}
			while (oldIndex < outputChannelSelection.length) {
				// loop to add the selected channels that we haven't been through yet
				newSelection[newIndex] = outputChannelSelection[oldIndex];
				newIndex++;
				oldIndex++;
			}

			Utils.log("Updated output selection to a total of " + newSelection.length + " elements: "
					+ Utils.printIntArray(newSelection), LogLevel.DEBUG);
			((TableParameter) theRow[OUT_CHANNELS_FIELD]).setSelection(newSelection);
			((MyTableModel) table11.getModel()).fireTableCellUpdated(tableRow, OUT_CHANNELS_FIELD);

		}

		/**
		 * Returns any ImagePlus that can be found in a Map of PluginIOs. This is used when a plugin specifies its
		 * output should be displayed in the same ImagePlus as that of another plugin.
		 * 
		 * @param map
		 * @return
		 */
		private PluginIOHyperstackViewWithImagePlus findAnImp(Map<String, IPluginIO> map) {
			return map.values().stream().filter(output1 -> output1 instanceof IPluginIOImage).map(
					output1 -> (IPluginIOImage) output1).map(output1 -> output1.getImp()).filter(imp -> imp != null)
					.findAny().get();
		}

		/**
		 * Returns all ImagePluses that can be found in a plugin's set of outputs.
		 * 
		 * @param hashMap
		 * @return
		 */
		private PluginIOHyperstackViewWithImagePlus[] getAllImpsToReRange(Map<String, IPluginIO> hashMap) {
			if (hashMap == null)
				return new PluginIOHyperstackViewWithImagePlus[] {};
			return hashMap.values().stream().filter(output1 -> output1 instanceof IPluginIOImage).map(
					output1 -> (IPluginIOImage) output1).map(output1 -> output1.getImp()).filter(
					imp -> imp != null && imp.shouldUpdateRange).toArray(PluginIOHyperstackViewWithImagePlus[]::new);
		}

		/**
		 * Update the ImagePlus in the destImp field based on what the user set in the String OUTPUT_NAME_FIELD
		 * 
		 * @param tableRow
		 * @param nTries
		 */
		@SuppressWarnings("null")
		private void updateDestinationFieldAtRow(int tableRow, int nTries) {

			Object[] data = ((MyTableModel) table1.getModel()).data;
			Object[] theRow = (Object[]) data[tableRow];
			PipelinePlugin plugin = (PipelinePlugin) theRow[PLUGIN_INSTANCE];
			if ((plugin.getFlags() & PipelinePlugin.NO_IMP_OUTPUT) > 0)
				return;

			Map<String, IPluginIO> outputLinks = new HashMap<>();

			if (!theRow[OUTPUT_NAME_FIELD].toString().equals("")) {
				// The user specified an output name
				workoutInputOrOutputFromString(theRow[OUTPUT_NAME_FIELD].toString(), tableRow, false, false,
						"This name should not be used", outputLinks);
				theRow[IMP_FOR_DISPLAY] = findAnImp(outputLinks);
				if ((theRow[IMP_FOR_DISPLAY] != null)
						&& ((PluginIOHyperstackViewWithImagePlus) theRow[IMP_FOR_DISPLAY]).imp != null)
					Utils.log("Row " + tableRow
							+ " is using for display an imp pre-existing destination whose title is "
							+ ((PluginIOHyperstackViewWithImagePlus) theRow[IMP_FOR_DISPLAY]).imp.getTitle(),
							Utils.LogLevel.DEBUG);
			}
			// Check if we know what the destination should be; if not, work it out

			plugin.shouldClearOutputs();

			if ((plugin.getOutputs() == null) || plugin.getOutputs().isEmpty()) {

				Utils.log("Creating destination at row " + tableRow, LogLevel.DEBUG);

				HashMap<String, ImagePlus> imageList = computeImagePlusList();

				@NonNull String outputName;
				if (!theRow[OUTPUT_NAME_FIELD].toString().equals("")) {
					// We got here because the user specified a name but no such image existed
					outputName = theRow[OUTPUT_NAME_FIELD].toString();
				} else {
					String sourceName = plugin.getInput() == null ? "null" : plugin.getInput().getName();
					outputName = plugin.operationName() + "_" + Utils.chopOffStringBeginning(sourceName, 15);
					while (imageList.get(outputName) != null) {
						outputName = outputName + "#";
					}
				}

				List<PluginIOView> imagesToShow = null;
				// The plugin knows best what kind of output to create; let it handle that
				try {
					imagesToShow =
							plugin.createOutput(outputName,
									(PluginIOHyperstackViewWithImagePlus) theRow[IMP_FOR_DISPLAY], outputLinks);
				} catch (Exception e) {
					plugin.clearOutputs();
					// We clear pluginOutputs because if at least one destination was successfully created
					// we won't attempt to create the other missing pluginOutputs on the next run
					throw (new RuntimeException(e));
				}

				if (theRow[AUXILIARY_OUTPUT_IMPS] == null)
					theRow[AUXILIARY_OUTPUT_IMPS] = new ArrayList<PluginIOView>();

				@SuppressWarnings("unchecked")
				List<PluginIOView> rowImageList = ((List<PluginIOView>) theRow[AUXILIARY_OUTPUT_IMPS]);

				if ((imagesToShow != null) && (!Utils.headless)) {
					imagesToShow.stream().filter(imageToShow -> !rowImageList.contains(imageToShow)).forEach(
							rowImageList::add);
				}
			}

			TableParameter outFieldParameter = ((TableParameter) theRow[OUT_CHANNELS_FIELD]);
			int[] outputChannelSelection = outFieldParameter.getSelection();
			for (int i = 0; i < outputChannelSelection.length; i++) {
				if (outputChannelSelection[i] >= outFieldParameter.getElements().length) {
					Utils.log("deleting output selection because it's out of range", LogLevel.DEBUG);
					outFieldParameter.setSelection(new int[0]);
					outputChannelSelection = null;
					break;
				}
			}

			MultiListParameter inFieldParameter = ((MultiListParameter) theRow[WORK_ON_CHANNEL_FIELD]);
			int[] inputChannelSelection = inFieldParameter.getSelection();

			if ((outputChannelSelection == null) || (outputChannelSelection.length < inputChannelSelection.length)) {
				int l;
				if (outputChannelSelection == null)
					l = 0;
				else
					l = outputChannelSelection.length;
				expandOutputChannelSelection(table1, tableRow, inputChannelSelection.length - l);
				outputChannelSelection = outFieldParameter.getSelection();
				((MyTableModel) table1.getModel()).fireTableCellUpdated(tableRow, OUT_CHANNELS_FIELD);
			}
		}

		/**
		 * Given a string and a starting position within the table, figures out what row is pointed to by the string.
		 * The reference can be absolute (if the string is of the form $integer), or relative (if the string is directly
		 * parsable to an integer).
		 * 
		 * @param userInput
		 * @param tableRow
		 * @return The absolute index in the table of the row pointed to by the string in userInput.
		 */
		private int resolveRowReference(String userInput, int tableRow) {
			int sourceRow = -1;

			if (userInput == null) {
				Utils.log("Reference not set for update at row " + tableRow, LogLevel.INFO);
				throw new RuntimeException("Reference not set");
			}
			if (userInput.equals("")) {
				Utils.log("Source not set for update at row " + tableRow, LogLevel.INFO);
				throw new RuntimeException("Reference not set");
			}

			Object[] data = ((MyTableModel) table1.getModel()).data;
			if ((userInput.indexOf('$') == 0)) { // Name starts with a $, meaning an absolute row number
				userInput = userInput.substring(1);
				if (!Utils.isParsableToInt(userInput)) {
					Utils.log(userInput + " is not parsable to int", LogLevel.ERROR);
					throw new RuntimeException("Reference not set");
				}
				sourceRow = Integer.parseInt(userInput) - 1;// Remove 1 because table numbering starts from 1
				if (sourceRow < 0) {
					Utils.displayMessage("Negative source row.", true, LogLevel.ERROR);
					throw new RuntimeException("Reference not set");
				}

				// If the current step is inactive, try to find the closest prior step that is active

				while (((Object[]) data[sourceRow])[USE_STEP].equals(Boolean.FALSE)) {
					sourceRow--;
					if ((sourceRow < 0)) {
						Utils.displayMessage("Absolute reference invalid because of inactive steps at row " + tableRow,
								true, LogLevel.ERROR);
						throw new RuntimeException("Absolute reference invalid because of inactive steps at row "
								+ tableRow);
					}
				}

			} else if (Utils.isParsableToInt(userInput)) { // Relative reference
				// Skip over the inactive steps when figuring out the relative reference

				int relativeRef = Integer.parseInt(userInput);
				int signedIncrement = relativeRef > 0 ? 1 : -1;
				int currentRow = tableRow;
				while (relativeRef != 0) {
					currentRow += signedIncrement;
					if ((currentRow < 0) || currentRow >= data.length) {
						throw new RuntimeException("Relative reference invalid at row " + tableRow);
					}
					while (((Object[]) data[currentRow])[USE_STEP].equals(Boolean.FALSE)) {
						currentRow += signedIncrement;
						if ((currentRow < 0) || currentRow >= data.length) {
							throw new RuntimeException("Relative reference invalid at row " + tableRow);
						}
					}
					relativeRef += -1 * signedIncrement;
				}

				sourceRow = currentRow;
				if (sourceRow < 0) {
					Utils.log("Negative source row", LogLevel.ERROR);
					throw new RuntimeException("Reference not set");
				}
			} else {
				throw new RuntimeException("Reference " + userInput + " invalid at row " + tableRow);
			}

			if (sourceRow > -1) { // Need to retrieve the input from sourceRow
				if (sourceRow >= ((MyTableModel) table1.getModel()).data.length) {
					Utils.log("Row " + sourceRow + " requested, but table only has "
							+ ((MyTableModel) table1.getModel()).data.length + "rows", LogLevel.ERROR);
					throw new RuntimeException("Cannot find reference");
				}
				return sourceRow;
			}
			throw new InternalError("shouldn't get here");
		}

		/**
		 * 
		 * @param userInput
		 * @param tableRow
		 * @param convertImpToPluginIO
		 *            IGNORED for now?
		 * @param stuffAllPluginIOs
		 *            If true, take all destination PluginIOs from source row and use irrespective of name
		 * @param forceNameTo
		 * @param tempHashMap
		 *            non-null HashMap that will be cleared and filled with new entries
		 */
		@SuppressWarnings("null")
		private void workoutInputOrOutputFromString(String userInput, int tableRow, boolean convertImpToPluginIO,
				boolean stuffAllPluginIOs, String forceNameTo, final Map<String, IPluginIO> tempHashMap) {

			int sourceRow = -1;
			// Check if we know what the source should be; if not, work it out
			// If it is the destination of a relative rows, set it in sourceRow, which will then become > 1

			if ((userInput == null) || userInput.equals("")) {
				Utils.log("Source not set for update at row " + tableRow, LogLevel.INFO);
				tempHashMap.clear();
				return;
			}

			Object[] data = ((MyTableModel) table1.getModel()).data;
			if ((userInput.indexOf('$') == 0)) { // Name starts with a $, meaning an absolute row number
				tempHashMap.clear();
				userInput = userInput.substring(1);
				if (!Utils.isParsableToInt(userInput)) {
					Utils.log(userInput + " is not parsable to int", LogLevel.ERROR);
					tempHashMap.clear();
					throw new RuntimeException("source not set");
				}
				sourceRow = Integer.parseInt(userInput) - 1;// Remove 1 because table numbering starts from 1
				if (sourceRow < 0) {
					Utils.displayMessage("Negative source row.", true, LogLevel.ERROR);
					throw new RuntimeException("source not set");
				}

				// If the current step is inactive, try to find the closest prior step that is active

				while (((Object[]) data[sourceRow])[USE_STEP].equals(Boolean.FALSE)) {
					sourceRow--;
					if ((sourceRow < 0)) {
						Utils.displayMessage("Absolute reference invalid because of inactive steps at row " + tableRow,
								true, LogLevel.ERROR);
						throw new RuntimeException("Absolute reference invalid because of inactive steps at row "
								+ tableRow);
					}
				}

			} else if (Utils.isParsableToInt(userInput)) { // Relative reference
				tempHashMap.clear();
				// Skip over the inactive steps when figuring out the relative reference

				int relativeRef = Integer.parseInt(userInput);
				int signedIncrement = relativeRef > 0 ? 1 : -1;

				int currentRow = tableRow;
				while (relativeRef != 0) {
					currentRow += signedIncrement;
					if ((currentRow < 0) || currentRow >= data.length) {
						throw new RuntimeException("Relative reference invalid at row " + tableRow);
					}
					while (((Object[]) data[currentRow])[USE_STEP].equals(Boolean.FALSE)) {
						currentRow += signedIncrement;
						if ((currentRow < 0) || currentRow >= data.length) {
							throw new RuntimeException("Relative reference invalid at row " + tableRow);
						}
					}
					relativeRef += -1 * signedIncrement;
				}

				sourceRow = currentRow;

				if (sourceRow < 0) {
					Utils.log("negative source row", LogLevel.ERROR);
					tempHashMap.clear();
					throw new RuntimeException("source not set");
				}
			} else {
				// This might be the name of an ImagePlus

				@NonNull String cleanedUpName = FileNameUtils.removeIncrementationMarks(userInput);

				HashMap<String, ImagePlus> impList = computeImagePlusList();
				ImagePlus imp = impList.get(cleanedUpName);

				if (imp == null) {
					// Check if cleanUpName was a full path name
					File f = new File(cleanedUpName);
					imp = impList.get(f.getName());
				}

				if (imp == null) {
					// The name was not that of an open ImagePlus; check if it corresponds to a file on disk

					File file = new File(cleanedUpName);
					if (!file.exists()) {
						Utils.displayMessage("Cannot find a window for image name "
								+ FileNameUtils.compactPath(cleanedUpName)
								+ " and it also does not correspond to a file on disk", true, LogLevel.ERROR);
						tempHashMap.clear();
						return;
					}

					// Check if this file is already in the inputs of the plugin
					// If it is, do nothing and return the same hashmap

					for (Map.Entry<String, IPluginIO> input : tempHashMap.entrySet()) {
						if (input.getValue() instanceof ImageAccessor) {
							ImageAccessor inputAccessor = (ImageAccessor) input.getValue();
							if ((((inputAccessor.getBackingFile() != null)) && (cleanedUpName.equals((inputAccessor)
									.getBackingFile().getPath())))
									|| (((inputAccessor.getOriginalSourceFile() != null)) && (cleanedUpName
											.equals((inputAccessor).getOriginalSourceFile().getPath())))) {
								tempHashMap.clear();
								tempHashMap.put(input.getKey(), input.getValue());
								return;
							}
						}
					}
					tempHashMap.clear();

					int dot = cleanedUpName.lastIndexOf('.');
					String extension = dot == -1 ? "" : cleanedUpName.substring(dot + 1);
					String originalCleanedUpName = cleanedUpName;

					File compressedFile = null;

					if ("gz".equals(extension)) {
						try (FileInputStream fis = new FileInputStream(file); InputStream is =
								new BufferedInputStream(new GZIPInputStream(fis))) {
							compressedFile = file;
							extension = file.getName().substring(file.getName().indexOf('.') + 1);
							extension = extension.substring(0, extension.indexOf('.'));
							File tempUncompressedFile =
									File.createTempFile(new File(cleanedUpName.substring(0, dot)).getName(),
											'.' + extension);
							Utils.log("Uncompressing file to " + tempUncompressedFile.getAbsolutePath(),
									LogLevel.VERBOSE_VERBOSE_DEBUG);

							try (FileOutputStream fos = new FileOutputStream(tempUncompressedFile); OutputStream os =
									new BufferedOutputStream(fos)) {
								final byte[] buffer = new byte[10000000];
								int bytesRead;
								/*@SuppressWarnings("unused")
								int n = 0;
								@SuppressWarnings("unused")
								final File f = file;*/
								SwingUtilities.invokeLater(() -> {
									// decompressionProgress.setIndeterminate(false);
									// decompressionProgress.setMinimum(0);
									// decompressionProgress.setMaximum((int) f.length());
									});
								int totalBytesRead = 0;
								int nIncrements10M = 0;
								while ((bytesRead = is.read(buffer)) != -1) {
									totalBytesRead += bytesRead;
									if (totalBytesRead > nIncrements10M * 2000000) {
										nIncrements10M++;
										@SuppressWarnings("unused")
										final int read = totalBytesRead;
										SwingUtilities.invokeLater(() -> {
											// decompressionProgress.setValue(read);
											});
									}
									os.write(buffer, 0, bytesRead);
									//n++;
								}
								SwingUtilities.invokeLater(() -> {
									// decompressionProgress.setValue(decompressionProgress.getMaximum());
								});

								file = tempUncompressedFile;
								originalCleanedUpName = cleanedUpName;
								cleanedUpName = tempUncompressedFile.getAbsolutePath();
							}
						} catch (Exception e) {
							// decompressionProgress.setValue(decompressionProgress.getMaximum());
							throw new RuntimeException("Error uncompressing file " + cleanedUpName, e);
						}
					}

					switch (extension) {
						case "proto":
							PluginIOCells seeds = new PluginIOCells(new File(cleanedUpName));
							tempHashMap.put("Seeds", seeds);
							break;
						case "tif":
						case "tiff":
						case "lsm":
							// File does not exist, so create a new accessor and put it in the hashmap
							TIFFFileAccessor tiffReader;
							try {
								tiffReader =
										new TIFFFileAccessor(file, FileNameUtils.compactPath(originalCleanedUpName));
								if (((MyTableModel) table1.getModel()).openUsingVirtualStacks)
									tiffReader.setDefaultCachePolicy(ImageAccessor.DONT_CACHE_PIXELS);
								else {
									if (file.length() > 30000000000L) {
										Utils.displayMessage("File " + file.getAbsolutePath()
												+ " is over ~30GB; forcing opening as a virtual stack.", true,
												LogLevel.INFO);
										tiffReader.setDefaultCachePolicy(ImageAccessor.DONT_CACHE_PIXELS);
									}
								}
								tiffReader.openForSequentialRead();
								tiffReader.setOriginalSourceFile(compressedFile);
							} catch (Exception e) {
								throw (new PluginRuntimeException("Could not open TIFF file "
										+ FileNameUtils.compactPath(cleanedUpName) + " to use for row " + tableRow, e,
										true));
							}
							tempHashMap.put(forceNameTo, tiffReader);
							break;
						default:
							throw new PluginRuntimeException("Could not open file "
									+ FileNameUtils.compactPath(cleanedUpName)
									+ (extension.equals("") ? " because of absent extension "
											: " because of unrecognized extension " + extension), true);
					}
				} else {
					// We have found an ImagePlus
					// Check if this imp is already in the hashmap we were passed
					// If it is, do nothing and return the same hashmap
					for (Map.Entry<String, IPluginIO> input : tempHashMap.entrySet()) {
						if (input.getValue() instanceof IPluginIOHyperstack) {
							PluginIOHyperstack hyperstack = (PluginIOHyperstack) input.getValue();
							if (((hyperstack.getImp() != null) && hyperstack.getImagePlusDisplay() == imp)) {
								tempHashMap.clear();
								tempHashMap.put(input.getKey(), hyperstack);
								return;
							}
						}
					}

					tempHashMap.clear();
					tempHashMap.put(forceNameTo, new PluginIOHyperstack(imp));
				}
			}

			if (sourceRow > -1) { // Need to retrieve the source PluginIO from sourcRow
				if (sourceRow >= ((MyTableModel) table1.getModel()).data.length) {
					Utils.log("Row " + sourceRow + " requested, but table only has "
							+ ((MyTableModel) table1.getModel()).data.length + "rows", LogLevel.ERROR);
					throw new RuntimeException("cannot find image");
				}

				if (((MyTableModel) table1.getModel()).data[sourceRow][PLUGIN_INSTANCE] == null)
					throw new RuntimeException("Trying to read the source of a null plugin");
				if (stuffAllPluginIOs) {
					tempHashMap
							.putAll(((PipelinePlugin) ((MyTableModel) table1.getModel()).data[sourceRow][PLUGIN_INSTANCE])
									.getOutputs());
				} else {
					IPluginIO dest =
							((PipelinePlugin) ((MyTableModel) table1.getModel()).data[sourceRow][PLUGIN_INSTANCE])
									.getOutput();
					if (dest != null)
						tempHashMap.put(forceNameTo, dest);
				}
			}
		}

		private Map<String, IPluginIO> getRowOutputs(int row) {
			Object[] data = ((MyTableModel) table1.getModel()).data;
			Object[] theRow = (Object[]) data[row];
			PipelinePlugin plugin = (PipelinePlugin) theRow[PLUGIN_INSTANCE];
			if (plugin == null)
				return Collections.emptyMap();
			return (plugin.getOutputs());
		}

		private Map<String, IPluginIO> getRowInputs(int row) {
			Object[] data = ((MyTableModel) table1.getModel()).data;
			Object[] theRow = (Object[]) data[row];
			PipelinePlugin plugin = (PipelinePlugin) theRow[PLUGIN_INSTANCE];
			if (plugin == null)
				return Collections.emptyMap();
			return (plugin.getInputs());
		}

		/**
		 * Updates the sourceImp field of row tableRow based on what the user set in the INPUT_NAME_FIELD. Returns the
		 * index of a row that has the input of row tableRow as its destination (NB: there could be more than one; which
		 * one is returned in that case?), or -1 if there is no such row. Also updates the auxiliary inputs
		 * (COMPUTED_INPUTS column) based on the references to other rows contained in column AUXILIARY_INPUTS.
		 * 
		 * @param tableRow
		 *            Index of the row in the table
		 */
		private void updateSourceFieldAtRow(int tableRow) {
			Object[] data = ((MyTableModel) table1.getModel()).data;
			Object[] row = (Object[]) data[tableRow];

			PipelinePlugin plugin = (PipelinePlugin) row[PLUGIN_INSTANCE];
			if (plugin == null) {
				Utils.log("Null plugin instance; cannot set pluginInputs", LogLevel.DEBUG);
				return;
			}

			workoutInputOrOutputFromString(row[INPUT_NAME_FIELD].toString(), tableRow, true,
					(plugin.getFlags() & PipelinePlugin.STUFF_ALL_INPUTS) > 0, "Default source", plugin.getInputs());
			Map<String, IPluginIO> inputs = plugin.getInputs();

			// Work out auxiliary inputs
			Map<String, IPluginIO> targetRowOutputs = null;
			String lastReferenceName = null;
			if (row[AUXILIARY_INPUTS] != null) {
				TwoColumnTableParameter auxInputs = (TwoColumnTableParameter) row[AUXILIARY_INPUTS];
				Object[] referenceNames = auxInputs.getFirstColumn();
				Object[] rowReferences = auxInputs.getSecondColumn();

				for (int i = 0; i < referenceNames.length; i++) {
					lastReferenceName = (String) referenceNames[i];
					if (rowReferences[i] == null || rowReferences[i].equals("")) {
						Utils.log("No entry for input " + referenceNames[i], LogLevel.DEBUG);
						continue;
					}

					int j = -1;
					try {
						j = resolveRowReference((String) rowReferences[i], tableRow);
						targetRowOutputs = getRowOutputs(j);
					} catch (Exception e) {
						// We will get here if the string was not a reference to another row but the name of an image
						targetRowOutputs = new HashMap<>();
						workoutInputOrOutputFromString((String) rowReferences[i], tableRow, true, false,
								(String) referenceNames[i], targetRowOutputs);
					}
					if ((targetRowOutputs == null) || (!targetRowOutputs.containsKey(referenceNames[i]))) {
						// Add default output from target row, or all outputs from targetRow if none is labeled as
						// default
						// XXX This might not be desirable behavior
						if ((targetRowOutputs != null) && (targetRowOutputs.containsKey("Default destination")))
							inputs.put((String) referenceNames[i], targetRowOutputs.get("Default destination"));
						else if (targetRowOutputs != null) {
							int numberAdded = 0;
							for (Entry<String, IPluginIO> inputSet : targetRowOutputs.entrySet()) {
								if (plugin instanceof SVMSuppress) {
									if ((numberAdded > 0) || !(inputSet.getValue() instanceof PluginIOCells)) {
										Utils.log("Detected SVMSuppress; skipping input " + inputSet.getKey()
												+ " because not cells", LogLevel.WARNING);
										continue;
									}
								}
								Utils.log("Adding output " + inputSet.getKey() + " of row " + j + " as input "
										+ referenceNames[i] + " of row " + tableRow + "; this is a guess",
										LogLevel.DEBUG);
								inputs.put((String) referenceNames[i], inputSet.getValue());
								if (numberAdded > 0) {
									Utils.log("There are more than 1 references matching " + referenceNames[i]
											+ "; ONLY THE LAST ONE WILL BE PRESERVED", LogLevel.WARNING);
								}
								numberAdded++;
							}
							if ((numberAdded == 0) && (j != tableRow)) {
								// Don't give a warning if the row referred to itself, because in that case
								// the first time it runs there can be no output
								Utils.displayMessage(
										"Warning: did not find any outputs from row " + (j + 1) + " to use in row "
												+ (tableRow + 1) + "; you might need to run row " + (j + 1) + " first.", true,
										LogLevel.WARNING);
							}
						}
					} else {
						Utils.log(">>>Found " + referenceNames[i] + ": " + targetRowOutputs.get(referenceNames[i]),
								LogLevel.DEBUG);
						inputs.put((String) referenceNames[i], targetRowOutputs.get(referenceNames[i]));
						Utils.log("Put into HashMap " + inputs, LogLevel.DEBUG);
					}
				}
			}

			{
				try {
					plugin.getInputs(inputs);
				} catch (ClassCastException e) {
					// See if other inputs are acceptable to the plugin
					// Dirty hack to get by until we implement graph structure of the pipeline
					inputs.clear();
					for (IPluginIO pluginIO : targetRowOutputs.values()) {
						try {
							inputs.put(lastReferenceName, pluginIO);
							plugin.getInputs(inputs);
							break;
						} catch (ClassCastException e2) {
							// Ignore; keep looping
							inputs.clear();
						}
					}
				}
			}

			if ("".equals(row[INPUT_NAME_FIELD].toString()))
				return; // No "default" source, and therefore no channel choices to update

			updateChannelChoices(tableRow, INPUT_NAME_FIELD, false);

			// update

			if (!((TableParameter) row[OUT_CHANNELS_FIELD]).hasBeenEdited) {
				// The user did not specify a channel for the output; we'll find a name
				// Loop through the input channels and assign an output channel name for each
				String[] inputNames = null;
				// For now, display as channel names those of the input (if they exist) that the plugin considers its
				// "default" input
				IPluginIO defaultPluginInput = plugin.getInput();
				if ((defaultPluginInput instanceof IPluginIOHyperstack)) {
					// If there is an imp used with the input, extract channel names from it
					// If not, don't do anything
					// We're requiring for simplicity that names have already been assigned
					// TODO Assign generic names earlier if there are no names already
					if (((IPluginIOHyperstack) defaultPluginInput).getChannels() == null)
						inputNames = new String[] { "Only channel" };
					else
						inputNames =
								((IPluginIOHyperstack) defaultPluginInput).getChannels().keySet().toArray(
										new String[] {});
					String[] outputNames = new String[inputNames.length];
					for (int i = 0; i < inputNames.length; i++) {
						outputNames[i] = Utils.chopOffString(row[PLUGIN_NAME_FIELD] + " " + inputNames[i], 30);
						while (Utils.indexOf(outputNames, outputNames[i]) < i) {
							outputNames[i] += "#";
						}
					}
					((TableParameter) row[OUT_CHANNELS_FIELD]).setValue(outputNames);
				}
			}

		}

		private Element addProcessingStepToXML(Document doc, Element s, int index) {
			Element documentRoot = doc.hasRootElement() ? doc.getRootElement() : null;
			Element processingSteps = null;
			if (documentRoot == null) {
				documentRoot = new Element("PipelineMetadata");
				doc.addContent(documentRoot);
			} else {
				processingSteps = documentRoot.getChild("ProcessingSteps");
			}
			if (processingSteps == null) {
				processingSteps = new Element("ProcessingSteps");
				documentRoot.addContent(processingSteps);
			}
			Element stepAtIndex = processingSteps.getChild("Step" + index);
			if (stepAtIndex != null) {
				Utils.log("replacing content " + index, LogLevel.DEBUG);
				if (!processingSteps.removeContent(stepAtIndex))
					Utils.log("Problem removing processing step", LogLevel.WARNING);
			}

			processingSteps.addContent(s.setName("Step" + index));
			return documentRoot;
		}

		private int getPreviousActiveRow(int tableRow) {
			Object[] data = ((MyTableModel) table1.getModel()).data;
			int r = tableRow - 1;
			while (r >= 0) {
				if (((Object[]) data[r])[USE_STEP].equals(Boolean.TRUE)) {
					return r;
				}
				r--;
			}
			return -1;
		}

		@SuppressWarnings("unused")
		private void closeAllViews(List<PluginIOView> views) {
			views.stream().filter(view -> view != null).forEach(PluginIOView::close);
			views.clear();
		}

		/**
		 * Call the plugin at row tableRow. Propagate the changes down the table if necessary. If an update is already
		 * underway, either cancel it (if the trigger row is the same as the row to update, and global settings say to
		 * cancel upon update), or flag it for re-updating when it's done. If we're processing the step as a result of
		 * the user clicking in a window, clickedPoints contains the list of points.
		 * 
		 * @param tableRow
		 *            Row index in the table
		 * @param triggerRow
		 *            Row that initially triggered updating of the table
		 * @param clickedPoints
		 *            If not null, list of points clicked by the user that triggered this call
		 * @param allowInterruptionOfUpdateAlreadyUnderway
		 * @param changedParameter
		 *            Parameter whose changed caused calling of this method
		 * @param stayInCoreLoop
		 *            If true, plugin called by pipeline should not return and keep performing live updates as long as
		 *            changedParameter is still changing.
		 */
		private void processStep(int tableRow, int triggerRow, PluginIO clickedPoints,
				boolean allowInterruptionOfUpdateAlreadyUnderway, AbstractParameter changedParameter,
				boolean stayInCoreLoop) throws InterruptedException {

			Utils.log("Process step called on row " + tableRow + ", triggered by row " + triggerRow, LogLevel.DEBUG);

			boolean shouldClearUpdateQueued = false;
			boolean wasInterrupted = false;
			Object[] data = ((MyTableModel) table1.getModel()).data;
			if ((tableRow < 0) || (tableRow >= data.length)) {
				throw new IllegalArgumentException();
			}

			Object[] pluginTableRow = (Object[]) data[tableRow];
			if (((AtomicBoolean) pluginTableRow[IS_UPDATING]).get()) {
				// The step is already being updated
				if ((tableRow == triggerRow) && ((MyTableModel) table1.getModel()).cancelUponChange
						&& allowInterruptionOfUpdateAlreadyUnderway) {
					// Signal to the thread working on the row that it should give up
					Thread thread = (Thread) pluginTableRow[WORKER_THREAD];
					// Make a copy in case the other thread sets this back not null between the time we test for null
					// and the time we send the interrupt
					if (thread != null) {
						thread.interrupt();
						Utils.log("Interrupting other thread", LogLevel.DEBUG);
					} else {
						Utils.log("Thread finished processing row before it could be interrupted", LogLevel.DEBUG);
					}
				}
				// See if anyone else if waiting. If so, just return as the thread waiting will use the latest
				// parameters; if not, wait for update to complete
				synchronized (pluginTableRow[UPDATE_QUEUED]) {
					if (((AtomicBoolean) pluginTableRow[UPDATE_QUEUED]).get()
							&& ((clickedPoints == null) || (clickedPoints.updatesCanBeCoalesced))) {
						return;// No need to worry about this row or the successive ones, because the thread
						// that has the update queued hold will take care of everything.
						// If clickedPoints is not null, we cannot just return because we would lose the list of clicked
						// points

					} else {
						while (((AtomicBoolean) pluginTableRow[UPDATE_QUEUED]).get()) {
							try {
								Utils.log("Waiting for row " + tableRow + " to be done with updates", LogLevel.DEBUG);
								Thread.currentThread().setName("Pipeline scheduler [blocked]");
								pluginTableRow[UPDATE_QUEUED].wait();
							} catch (InterruptedException e) {
								Utils.log("Interrupted while waiting for row " + tableRow + " to be done updating",
										LogLevel.DEBUG);
								throw e;
							}
						}
						((AtomicBoolean) pluginTableRow[UPDATE_QUEUED]).set(true);
						((MyTableModel) table1.getModel()).fireTableCellUpdated(tableRow, UPDATE_QUEUED);
						pluginTableRow[QUEUED_WORKER_THREAD] = Thread.currentThread();
						((MyTableModel) table1.getModel()).fireTableCellUpdated(tableRow, QUEUED_WORKER_THREAD);
						shouldClearUpdateQueued = true;
					}
				}
			}
			Thread.currentThread().setName("Pipeline scheduler");

			PipelinePlugin plugin = null;
			synchronized (pluginTableRow[IS_UPDATING]) {
				try {
					while (((AtomicBoolean) pluginTableRow[IS_UPDATING]).get()) {
						Utils.log("waiting for update to row " + tableRow + " to complete", LogLevel.DEBUG);
						pluginTableRow[IS_UPDATING].wait();
						Utils.log("Done waiting for row " + tableRow + " to complete", LogLevel.DEBUG);
					}
					((AtomicBoolean) pluginTableRow[IS_UPDATING]).set(true);
					((MyTableModel) table1.getModel()).fireTableCellUpdated(tableRow, IS_UPDATING);

					pluginTableRow[WORKER_THREAD] = Thread.currentThread();

					if (shouldClearUpdateQueued) {
						synchronized (pluginTableRow[UPDATE_QUEUED]) {
							((MyTableModel) table1.getModel()).fireTableCellUpdated(tableRow, UPDATE_QUEUED);
							pluginTableRow[QUEUED_WORKER_THREAD] = null;
							((MyTableModel) table1.getModel()).fireTableCellUpdated(tableRow, QUEUED_WORKER_THREAD);
							((AtomicBoolean) pluginTableRow[UPDATE_QUEUED]).set(false);
							pluginTableRow[UPDATE_QUEUED].notify();
						}
					}

					synchronized (pluginTableRow[OUTPUT_LOCKS]) {
						while (((AtomicInteger) pluginTableRow[OUTPUT_LOCKS]).get() > 0) {
							// Another plugin is using [one of] our output[s] as an input and does not want its input to
							// change while it is working on it. We need to wait for that
							// plugin to finish; we have already set theRow[IS_UPDATING] to true, so no further plugins
							// should attempt to use [one of] our output[s] as input until we finish
							// FIXME This might lead to deadlocks
							Utils.log("Row " + tableRow + " waiting for output lock to be released", LogLevel.DEBUG);
							pluginTableRow[OUTPUT_LOCKS].wait();
						}
						Utils.log("Done waiting for output lock to be released at row " + tableRow, LogLevel.DEBUG);
					}
					// The output should not be locked again before we finish because anyone trying to lock it
					// will check that theRow[IS_UPDATING] is false first, and we have already set theRow[IS_UPDATING]
					// to true

					plugin = (PipelinePlugin) pluginTableRow[PLUGIN_INSTANCE];

					if (pluginTableRow[USE_STEP].equals(Boolean.FALSE)) {
						// Save metadata anyway in case we want to activate the step after reusing the metadata

						String sourceName = pluginTableRow[INPUT_NAME_FIELD].toString();
						Element p = new Element("Step" + tableRow);
						p.addContent(new Element("PluginDescription").setText(""));
						p.addContent(new Element("SourceFile").setText(sourceName));
						p.addContent(new Element("PluginVersion").setText("ND"));
						p.addContent(new Element("PluginHash").setText("ND"));

						for (int i = 0; i < pluginTableRow.length; i++) {
							if (!((pluginTableRow[i] instanceof ImagePlus)
									|| (pluginTableRow[i] instanceof ProgressRenderer)
									|| (pluginTableRow[i] instanceof PipelinePlugin)
									|| (pluginTableRow[i] instanceof Thread) || (pluginTableRow[i] instanceof Document)))
								p.addContent(new Element("Column").setAttribute("Index", "" + i).setText(
										Utils.objectToXMLString(pluginTableRow[i])));
						}

						int previousActiveRow = getPreviousActiveRow(tableRow);
						Document doc = null;
						if (previousActiveRow > -1) {
							doc = (Document) ((Object[]) data[tableRow - 1])[OUTPUT_XML];
						} else
							doc = plugin.getInput().getMetadata();

						Element processing = addProcessingStepToXML((Document) doc.clone(), p, tableRow);

						if (pluginTableRow[OUTPUT_XML] == null)
							pluginTableRow[OUTPUT_XML] = new Document();
						ParseImageMetadata.setPipelineProcessingMetadata((Document) pluginTableRow[OUTPUT_XML],
								"ProcessingSteps", processing);

						if (((MyTableModel) table1.getModel()).updatePipeline)
							processStep(tableRow + 1, triggerRow, null, true, changedParameter, stayInCoreLoop);
						pluginTableRow[WORKER_THREAD] = null;
					}

					int pluginID = Utils.indexOf(pluginNames, (String) pluginTableRow[PLUGIN_NAME_FIELD]);
					if (plugins.get(pluginTableRow[PLUGIN_NAME_FIELD]) == null)
						throw new IllegalStateException("Could not find plugin " + pluginTableRow[PLUGIN_NAME_FIELD]);
					if (plugins.get(pluginTableRow[PLUGIN_NAME_FIELD]).obsolete)
						Utils.log("Plugin " + ((String) pluginTableRow[PLUGIN_NAME_FIELD]) + " is obsolete",
								LogLevel.WARNING);
					if (pluginTableRow[PLUGIN_INSTANCE] == null) {
						if (pluginID > -1) { // Should this ever happen?
							pluginTableRow[PLUGIN_INSTANCE] =
									((PipelinePlugin) pluginObjects[pluginID]).getClass().newInstance();
							((PipelinePlugin) pluginTableRow[PLUGIN_INSTANCE])
									.setpipeLineListener(new PluginCallBack());
							((PipelinePlugin) pluginTableRow[PLUGIN_INSTANCE]).setRow(tableRow);
						} else {
							throw new Exception("Cannot find plugin " + pluginTableRow[PLUGIN_NAME_FIELD]
									+ " for update at row " + tableRow + ", triggered from row triggerRow");
						}
					}

					try {

						progressSetIndeterminateThreadSafe((ProgressRenderer) pluginTableRow[PERCENT_DONE], true,
								tableRow);
						if ((plugin.getFlags() & PipelinePlugin.NO_INPUT) == 0)
							updateSourceFieldAtRow(tableRow);
						// Resolve the input so we have a valid source_imp to pass to the plugin

						// Check if any inputs are marked as requiring locking for current plugin to work on them
						// If they are, wait for them to be available and lock them
						Map<String, InputOutputDescription> inputDescriptions = plugin.getInputDescriptions();
						// First register our thread in all our inputs so we can get into a state where we have a lock
						// on all of them at the same time
						for (Entry<String, IPluginIO> inputSet : plugin.getInputs().entrySet()) {
							InputOutputDescription inputDescription = inputDescriptions.get(inputSet.getKey());
							if ((inputDescription != null) && (inputDescription.pluginNeedsInputLocked)) {
								synchronized (inputSet.getValue().getLockingThreads()) {
									inputSet.getValue().getLockingThreads().add(Thread.currentThread());
								}
							}
						}

						// Now wait for all of our inputs to be available
						if (plugin.getInputs() != null)
							for (Map.Entry<String, IPluginIO> inputSet : plugin.getInputs().entrySet()) {
								InputOutputDescription inputDescription = inputDescriptions.get(inputSet.getKey());
								if ((inputDescription != null) && (inputDescription.pluginNeedsInputLocked)) {
									IPluginIO input = inputSet.getValue();
									synchronized (input.getIsUpdating()) {
										while (input.getIsUpdating().get()) {
											Utils.log("Row " + tableRow + " waiting for input " + input.getName()
													+ " to be unlocked", LogLevel.DEBUG);
											try {
												input.getIsUpdating().wait();
											} catch (InterruptedException e) {
												Utils.log("Interrupted while row " + tableRow + " waiting for input "
														+ input.getName() + " to be unlocked", LogLevel.INFO);
												throw e;
											}
											Utils.log("Row " + tableRow + " DONE waiting for input " + input.getName()
													+ " to be unlocked", LogLevel.DEBUG);
										}
									}
								}
							}
						// TODO Unlock all the stuff later on

						// Check that the plugin inputs are of a compatible type with what the plugin can handle
						// If not, try to convert them
						if (plugin.getInputs() != null)
							for (Map.Entry<String, IPluginIO> inputSet : plugin.getInputs().entrySet()) {
								InputOutputDescription inputDescription = inputDescriptions.get(inputSet.getKey());
								if ((inputDescription != null) && (inputSet.getValue() instanceof IPluginIOImage)) {
									IPluginIOImage imageInput = (IPluginIOImage) inputSet.getValue();
									if (!PluginIOImage.indexOf(inputDescription.acceptablePixelTypes, imageInput
											.getPixelType())) {
										// Attempt to convert output
										// TODO Need better synchronization
										PixelType[] canConvert = imageInput.canConvertTo();
										boolean converted = false;
										for (PixelType element : canConvert) {
											if (PluginIOImage.indexOf(inputDescription.acceptablePixelTypes, element)) {
												Utils.log("Converting input " + imageInput.getName() + " from "
														+ imageInput.getPixelType() + " to " + element, LogLevel.DEBUG);
												imageInput.convertTo(element);
												converted = true;
											}
										}
										if (!converted)
											throw new RuntimeException("Row " + tableRow + ": incompatible pixel type "
													+ imageInput.getPixelType() + " in input " + imageInput.getName()
													+ "; empty intersection between convertible set "
													+ Utils.printPixelTypes(canConvert)
													+ " and acceptable input types "
													+ Utils.printPixelTypes(inputDescription.acceptablePixelTypes));
									}
								}
							}

						if (plugin.getInputs() != null)
							Utils.log("Working on images " + plugin.getInputs().toString(), LogLevel.DEBUG);

						updateDestinationFieldAtRow(tableRow, 1);

						((ProgressRenderer) pluginTableRow[PERCENT_DONE]).setPlugin(plugin);

						PreviewType previewType = null;

						boolean someInputHasChanged = false;
						if (plugin.getInputs() != null)
							for (IPluginIO input : plugin.getInputs().values()) {
								if (input == null) {
									Utils.log("Null input", LogLevel.WARNING);
									continue;
								}
								if (input.getLastTimeModified() > ((Long) pluginTableRow[LAST_TIME_RUN])) {
									someInputHasChanged = true;
									// Utils.log("Changed input: " + input.getName(), LogLevel.VERBOSE_DEBUG);
									break;
								} else {
									// Utils.log("Unchanged input: " + input.getName(), LogLevel.VERBOSE_DEBUG);
								}
							}

						Utils.log("Starting " + plugin.operationName(), LogLevel.INFO);

						wasInterrupted = false;
						try {
							if (clickedPoints != null) {
								// Check that the plugin can handle those clicked points
								if (!(plugin instanceof MouseEventPlugin)) {
									throw new RuntimeException("Plugin " + plugin
											+ " doesn't know how to respond to mouse clicks at row " + tableRow);
								}
								((MouseEventPlugin) plugin).mouseClicked(clickedPoints, someInputHasChanged, null);
							} else {
								Thread.currentThread().setName(plugin.operationName());
								((FourDPlugin) plugin).run(((ProgressBarWrapper) pluginTableRow[PERCENT_DONE]),
										((MultiListParameter) pluginTableRow[WORK_ON_CHANNEL_FIELD]),
										(TableParameter) pluginTableRow[OUT_CHANNELS_FIELD], previewType,
										someInputHasChanged, changedParameter, stayInCoreLoop);
							}
							Utils.log("Done running " + plugin.operationName(), LogLevel.INFO);
						} catch (OutOfMemoryError e) {
							Utils.displayMessage("Out of memory", true, LogLevel.ERROR);
							plugin.clearOutputs();
							throw e;
						} catch (Exception e) {
							if (Utils.causedByInterruption(e)) {
								Utils.log(plugin.operationName() + " interrupted", LogLevel.INFO);
								wasInterrupted = true;
								Utils.printStack(e, LogLevel.DEBUG);
							} else
								throw e;
						}

						pluginTableRow[COMPUTING_ERROR] = wasInterrupted;

						List<?> imagesToShow = (List<?>) pluginTableRow[AUXILIARY_OUTPUT_IMPS];

						if ((imagesToShow != null) && ((Boolean) table1.getValueAt(tableRow, SHOW_IMAGE))) {
							for (Object anImagesToShow : imagesToShow) {
								try {
									((PluginIOView) anImagesToShow).show();
								} catch (Exception e) {
									Utils.printStack(e);
								}
								Utils.log("Showed output", LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
							}
						}

						boolean b = false;
						if ((Boolean) pluginTableRow[RESET_RANGE]) {
							PluginIOHyperstackViewWithImagePlus[] imps = getAllImpsToReRange(plugin.getOutputs());
							for (PluginIOHyperstackViewWithImagePlus imp : imps) {
								if (imp.imp != null)
									if (imp.imp.getNSlices() > 1 && b)
										Utils.updateRangeInStack(imp.imp);
									else {
										// Utils.updateRangeInRegularImp(imp.imp);
										imp.imp.getProcessor().setAutoThreshold(ImageProcessor.ISODATA,
												ImageProcessor.NO_LUT_UPDATE);
										imp.imp.updateAndDraw();
									}
							}
						} else
							Utils.log("Not updating range", LogLevel.DEBUG);

					} finally {
						Element inputDescription = new Element("Sources");

						if (plugin.getInputs() != null)
							for (Map.Entry<String, IPluginIO> inputSet : plugin.getInputs().entrySet()) {
								if ((inputSet == null) || inputSet.getValue() == null)
									continue;
								Element source = new Element("Source").setText(inputSet.getKey());
								source.addContent(new Element("Derivation").setText(""
										+ inputSet.getValue().getDerivation()));
								inputDescription.addContent(source);
							}

						Element p = new Element("Step" + tableRow);
						p.addContent(new Element("PluginDescription").setText(plugin.operationName()));
						p.addContent(inputDescription);
						p.addContent(new Element("PluginVersion").setText(plugin.version()));
						p.addContent(new Element("PluginHash").setText(pluginHash[pluginID]));
						p.addContent(new Element("LastModificationTime").setText("" + System.currentTimeMillis()));
						// theRow[LAST_TIME_RUN] must be set AFTER LastModificationTime for inputHasChanged to be
						// determined right
						pluginTableRow[LAST_TIME_RUN] = System.currentTimeMillis();

						for (int i = 0; i < pluginTableRow.length; i++) {
							try {
								if (!((pluginTableRow[i] instanceof List<?>)
										|| (pluginTableRow[i] instanceof PluginIOHyperstackViewWithImagePlus)
										|| (pluginTableRow[i] instanceof PluginIOView)
										|| (pluginTableRow[i] instanceof HashMap<?, ?>)
										|| (pluginTableRow[i] instanceof ImagePlus)
										|| (pluginTableRow[i] instanceof ProgressRenderer)
										|| (pluginTableRow[i] instanceof PipelinePlugin)
										|| (pluginTableRow[i] instanceof Thread)
										|| (pluginTableRow[i] instanceof Document) || (pluginTableRow[i] instanceof LinkToExternalProgram)))
									p.addContent(new Element("Column").setAttribute("Index", "" + i).setText(
											Utils.objectToXMLString(pluginTableRow[i])));
							} catch (Exception e) {
								Utils.log("Eror while generating table XML", LogLevel.ERROR);
								Utils.printStack(e);
							}
						}

						int previousActiveRow = getPreviousActiveRow(tableRow);
						Document doc = null;
						if (tableRow == 0)
							doc = new Document(); // If this is the first row, don't read metadata from the destination
													// imp so that the processing steps don't get mixed up with those
													// from the file the input comes from

						else if (previousActiveRow > -1) {
							doc = (Document) ((Object[]) data[tableRow - 1])[OUTPUT_XML];
						} else
							doc = plugin.getInput().getMetadata();

						if (doc == null)
							doc = new Document();
						if (clickedPoints == null) {
							// Only add this step to the metadata if it doesn't result from a GUI click
							// The RegisterClick step already stores the list of clicks
							Element processing = addProcessingStepToXML((Document) doc.clone(), p, tableRow);
							pluginTableRow[OUTPUT_XML] = new Document().addContent(processing.detach());// processing;
						}

						if (plugin instanceof SaveTable) {
							// Run again so table gets save properly, now XML has been generated
							((FourDPlugin) plugin).run(((ProgressBarWrapper) pluginTableRow[PERCENT_DONE]),
									((MultiListParameter) pluginTableRow[WORK_ON_CHANNEL_FIELD]),
									(TableParameter) pluginTableRow[OUT_CHANNELS_FIELD], null, true, null, false);
						}
					}

				} catch (Exception e) {
					pluginTableRow[COMPUTING_ERROR] = true;
					String pluginName = "";
					try {
						pluginName = plugin.operationName() + ": ";
						Utils.log("Error in plugin " + pluginName, LogLevel.ERROR);
					} catch (Throwable t) {
						System.err.println("Problem logging exception");
					}
					scrollTableToRow(tableRow - 1);
					tableFrame.toFront();
					Utils.printStack(e, LogLevel.ERROR);
					if (e instanceof PluginRuntimeException && ((PluginRuntimeException) e).getDisplayUserDialog()
							&& !Utils.causedByInterruption(e)) {
						Utils.displayMessage(pluginName + e.getMessage(), false, LogLevel.ERROR);
					}
				} finally {
					progressSetIndeterminateThreadSafe((ProgressRenderer) pluginTableRow[PERCENT_DONE], false, tableRow);
					if (!((Boolean) pluginTableRow[COMPUTING_ERROR]) && !(plugin instanceof BatchOpenV2)) {
						progressSetValueThreadSafe((ProgressRenderer) pluginTableRow[PERCENT_DONE], 100);
					}

					synchronized (pluginTableRow[IS_UPDATING]) {
						((AtomicBoolean) pluginTableRow[IS_UPDATING]).set(false);
						pluginTableRow[WORKER_THREAD] = null;
						pluginTableRow[IS_UPDATING].notifyAll();
					}

					((MyTableModel) table1.getModel()).fireTableCellUpdated(tableRow, IS_UPDATING);
					((MyTableModel) table1.getModel()).fireTableCellUpdated(tableRow, WORKER_THREAD);
				}

			}

			if (Utils.headless && ((Boolean) pluginTableRow[COMPUTING_ERROR])) {
				String message = "Stopping command line run because of error. ";
				message += "Arguments were: " + Utils.printStringArray(arguments);
				Utils.log(message, LogLevel.ERROR);
				// TODO throw an exception that will be caught later instead of exiting
				System.exit(1);
			}

			if ((((MyTableModel) table1.getModel()).updatePipeline) && (!(plugin instanceof Pause && !Utils.headless))
					&& (!wasInterrupted) && !(((Boolean) pluginTableRow[COMPUTING_ERROR]) && stopOnError)
					&& tableRow + 1 < data.length)
				processStep(tableRow + 1, triggerRow, null, true, changedParameter, stayInCoreLoop);

			if (wasInterrupted)
				throw new InterruptedException();
			if ((Boolean) pluginTableRow[COMPUTING_ERROR])
				throw new PluginRuntimeException("Pipeline run finishing with error", false);
		}

		private HashMap<String, ImagePlus> computeImagePlusList() {
			HashMap<String, ImagePlus> result = new HashMap<>();
			int[] ImageIDList = WindowManager.getIDList();
			if (ImageIDList == null) {
				return result;
			}
			for (int element : ImageIDList) {
				ImagePlus imp = WindowManager.getImage(element);
				@SuppressWarnings("null")
				String name = imp != null ? FileNameUtils.removeIncrementationMarks(imp.getTitle()) : "";
				result.put(name, WindowManager.getImage(element));
			}
			return result;
		}

		/**
		 * Updates the list of channel names displayed by the table for the user to choose from in the source and
		 * destination images. Called when a user has selected a new file from a popup menu to use as an input or an
		 * output to a plugin.
		 * 
		 * @param row
		 *            Row index in the table
		 * @param column
		 *            Column index; should be INPUT_NAME_FIELD for an update of the source channel choices, or
		 *            OUTPUT_NAME_FIELD for an update of the destination channel choices (the latter is currently not
		 *            implemented)
		 * @param recomputeSourceField
		 *            True if the reference to the image whose channel choices we're updating should be refreshed
		 */
		private void updateChannelChoices(int row, int column, boolean recomputeSourceField) {
			if (column == INPUT_NAME_FIELD) {
				try {
					if (recomputeSourceField) {
						try {
							updateSourceFieldAtRow(row);
						} catch (Exception e) {
							Utils.printStack(e);
						}
					}
					Object[] theRow = ((MyTableModel) table1.getModel()).data[row];
					if (theRow[PLUGIN_INSTANCE] == null)
						return;
					IPluginIO source = ((PipelinePlugin) theRow[PLUGIN_INSTANCE]).getInput();
					@SuppressWarnings("null")
					@NonNull String @NonNull[] channelNames = source == null ? new String[] {} :
						source.listOfSubObjects();
					((MultiListParameter) theRow[WORK_ON_CHANNEL_FIELD]).setChoices(channelNames);
					((MultiListParameter) theRow[WORK_ON_CHANNEL_FIELD]).trimSelection();
					if ((((MultiListParameter) theRow[WORK_ON_CHANNEL_FIELD]).getSelection().length == 0)
							&& (channelNames.length > 0)) {
						((MultiListParameter) theRow[WORK_ON_CHANNEL_FIELD]).setSelection(new int[] { 0 });
					}
					if (((TableParameter) theRow[OUT_CHANNELS_FIELD]).getElements().length < channelNames.length) {
						String[] newElements = new String[channelNames.length];
						System.arraycopy(channelNames, 0, newElements, 0, newElements.length);
						((TableParameter) theRow[OUT_CHANNELS_FIELD]).setValue(newElements);
					}
				} catch (Exception e) {
					Utils.log("Could not update info about source", LogLevel.ERROR);
					Utils.printStack(e);
				}
			} else if (column == OUTPUT_NAME_FIELD) {

			}

			((MyTableModel) table1.getModel()).fireTableRowsUpdated(row, row);
		}

		/**
		 * Display a popup menu as a response to a user right-click in the input or output fields of a plugin
		 * 
		 * @param row
		 * @param column
		 * @param comp
		 * @param xPosition
		 * @param yPosition
		 */
		private void displayImageListMenu(final int row, final int column, final Component comp, int xPosition,
				int yPosition) {
			final Object[] data = ((MyTableModel) table1.getModel()).data;

			int[] ImageIDList = WindowManager.getIDList();
			if (ImageIDList == null)
				ImageIDList = new int[0];
			String[] ImageTitleList = new String[ImageIDList.length];
			ImagePlus[] ImagePlusList = new ImagePlus[ImageIDList.length];
			ImageListMenu = new JPopupMenu("Image popup");
			ActionListener actionListener = actionEvent -> {
				((TextParameter) ((Object[]) data[row])[column]).setValue(actionEvent.getActionCommand());
				table1.tableChanged(new TableModelEvent(table1.getModel(), row, column));
				((TextParameter) ((Object[]) data[row])[column]).fireValueChanged(false, true, true);
				// table1.setRowHeight(1);
				// scrollTableToRow(row);
				// table1.invalidate();
					Utils.log("Updating input for row " + row, LogLevel.VERBOSE_VERBOSE_DEBUG);
					updateChannelChoices(row, column, true);
					PipelinePlugin plugin = (PipelinePlugin) ((Object[]) data[row])[PLUGIN_INSTANCE];
					if (plugin != null) {
						plugin.clearInputs();
						plugin.clearOutputs();
					}
				};

			JMenuItem selectFileItem = new JMenuItem("Select file...");
			selectFileItem.addActionListener(actionEvent -> {
				// The user wants to select a file to open from a menu
					File file = FileNameUtils.chooseFile("Choose an image to open", FileDialog.LOAD);
					if (file == null)
						return;
					@SuppressWarnings("null")
					final String path = FileNameUtils.compactPath(file.getAbsolutePath());
					Runnable r = () -> {
						((TextParameter) ((Object[]) data[row])[column]).setValue(path);
						table1.tableChanged(new TableModelEvent(table1.getModel(), row, column));
						((TextParameter) ((Object[]) data[row])[column]).fireValueChanged(false, true, true);
						Utils.log("Updating input for row " + row, LogLevel.VERBOSE_VERBOSE_DEBUG);
						PipelinePlugin plugin = (PipelinePlugin) ((Object[]) data[row])[PLUGIN_INSTANCE];
						if (plugin != null) {
							plugin.clearInputs();
							plugin.clearOutputs();
						}

						updateChannelChoices(row, column, true);
					};
					BasePipelinePlugin.threadPool.submit(r);
				});
			ImageListMenu.add(selectFileItem);
			for (int i = 0; i < ImageIDList.length; i++) {
				ImagePlusList[i] = WindowManager.getImage(ImageIDList[i]);
				ImageTitleList[i] = ImagePlusList[i] != null ? ImagePlusList[i].getTitle() : "";
				JMenuItem menuItem = new JMenuItem(ImageTitleList[i]);
				menuItem.addActionListener(actionListener);
				ImageListMenu.add(menuItem);
			}

			JMenuItem menuItem = new JMenuItem("Use output from step at relative position");
			menuItem.addActionListener(actionListener);
			menuItem.setEnabled(false);
			ImageListMenu.add(menuItem);

			// Add relative references to the menu
			for (int i = 0; i < data.length; i++) {
				if (i != imageListMenuXCellIndex) {
					menuItem = new JMenuItem("" + (i - imageListMenuXCellIndex));
					menuItem.addActionListener(actionListener);
					ImageListMenu.add(menuItem);
				}
			}

			menuItem = new JMenuItem("Use output from step at absolute position");
			menuItem.addActionListener(actionListener);
			menuItem.setEnabled(false);
			ImageListMenu.add(menuItem);

			// Add absolute references to the menu
			for (int i = 0; i < data.length; i++) {
				if (i != imageListMenuXCellIndex) {
					menuItem = new JMenuItem("$" + (i + 1));
					menuItem.addActionListener(actionListener);
					ImageListMenu.add(menuItem);
				}
			}

			ImageListMenu.show(comp, xPosition, yPosition);
		}

		/**
		 * Display a popup menu as a response to a user right-click in plugin name field
		 * 
		 * @param row
		 * @param column
		 * @param comp
		 * @param xPosition
		 * @param yPosition
		 */
		private void displayPluginListMenu(final int row, final int column, final Component comp, int xPosition,
				int yPosition) {

			ActionListener actionListener = actionEvent -> {
				String pluginName = actionEvent.getActionCommand();
				table1.setValueAt(pluginName, row, column);
				table1.setRowHeight(1);
				udpateRowPlugin(row, column);
				table1.setRowSelectionInterval(row, row);
				scrollTableToRow(row);
			};

			// Get list of package names
			Map<String, List<String>> subMenus = new TreeMap<>();
			for (String longPluginName : longPluginNames) {
				// Get package name
				if (longPluginName == null)
					longPluginName = "PluginUnloadable.PluginUnloadable";
				String[] components = longPluginName.split("\\.");
				String scndToLast = components[components.length - 2];
				List<String> list = subMenus.get(scndToLast);
				if (list == null) {
					list = new ArrayList<>();
					subMenus.put(scndToLast, list);
				}
				list.add(components[components.length - 1]);
			}

			pluginListMenu = new JPopupMenu("Plugin popup");

			for (String subMenuName : new TreeSet<>(subMenus.keySet())) {
				JMenu subMenu = new JMenu(subMenuName);
				java.util.Collections.sort(subMenus.get(subMenuName));
				for (String pluginName : subMenus.get(subMenuName)) {
					if (!plugins.get(pluginName).display)
						continue;
					JMenuItem menuItem = new JMenuItem(pluginName);
					menuItem.addActionListener(actionListener);
					String pluginToolTip = plugins.get(pluginName).toolTip;
					if (!"".equals(pluginToolTip))
						menuItem.setToolTipText(Utils.encodeHTML(WordUtils.wrap(pluginToolTip, 50, null, true))
								.replace("\n", "<br>\n"));
					subMenu.add(menuItem);
				}
				if (subMenu.getSubElements().length > 0)
					pluginListMenu.add(subMenu);
			}

			ToolTipManager.sharedInstance().setDismissDelay(60000);

			pluginListMenu.show(comp, xPosition, yPosition);
		}

		/**
		 * Display a directory menu as a response to a user right-click in parameter field
		 */
		private void displayFileDirectoryDialog(FileNameParameter file, DirectoryParameter directory, int row,
				int column) {
			FileDialog dialog = new FileDialog(new Frame(), "Choose file", FileDialog.LOAD);
			dialog.setVisible(true);

			if (dialog.getFile() != null) {
				file.setValue(dialog.getFile());
				file.fireValueChanged(false, true, true);
			}
			if (dialog.getDirectory() != null) {
				directory.setValue(dialog.getDirectory());
				directory.fireValueChanged(false, true, true);
			}
		}

		private class PluginCallBack implements PipelineCallback {

			@Override
			public void parameterValueChanged(int row, AbstractParameter changedParameter, boolean stayInCoreLoop) {
				try {
					if (Utils.headless) {
						Utils.log("Ignoring parameter change triggered as follows", LogLevel.DEBUG);
						Exception e = new Exception();
						Utils.printStack(e, LogLevel.WARNING);
					}
					checkUpdateAtRow(row, changedParameter, stayInCoreLoop);
				} catch (Exception e) {
					Utils.log("Exception during callback for update to row " + row, LogLevel.ERROR);
					Utils.printStack(e);
				}
			}

			@Override
			public void passClickToRow(final int row, final PluginIO clickedPoints,
					final boolean allowInterruptionOfUpdateAlreadyUnderway, boolean blockUntilCompleted) {
				if (blockUntilCompleted)
					try {
						processStep(row, row, clickedPoints, allowInterruptionOfUpdateAlreadyUnderway, null, false);
					} catch (InterruptedException e) {
						Utils.printStack(e);
					}
				else {
					Runnable r =
							() -> {
								try {
									processStep(row, row, clickedPoints, allowInterruptionOfUpdateAlreadyUnderway,
											null, false);
								} catch (InterruptedException e) {
									Utils.printStack(e);
								}
							};
					BasePipelinePlugin.threadPool.submit(r);
				}
			}

			@Override
			public void redrawLine(int row) {
				((MyTableModel) table1.getModel()).fireTableRowsUpdated(row, row);
			}

			@Override
			public void redrawProgressRenderer(int row) {
				((MyTableModel) table1.getModel()).fireTableChanged(new TableModelEvent(table1.getModel(), row, row,
						PERCENT_DONE));
			}

			@Override
			public void updateWorkOnChannelField(int row) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				Object[] theRow = (Object[]) data[row];
				@NonNull String @NonNull[] channelNames = 
						((PipelinePlugin) theRow[PLUGIN_INSTANCE]).getInput().listOfSubObjects();
				((MultiListParameter) theRow[WORK_ON_CHANNEL_FIELD]).setChoices(channelNames);
				((MultiListParameter) theRow[WORK_ON_CHANNEL_FIELD]).trimSelection();
			}

			@SuppressWarnings("unchecked")
			@Override
			public List<PluginIOHyperstackViewWithImagePlus> getAuxiliaryOutputImps(int row) {
				// FIXME Shouldn't this method work with PluginIOViews rather than PluginIOHyperstackViewWithImagePlus?
				Object[] data = ((MyTableModel) table1.getModel()).data;
				Object[] theRow = (Object[]) data[row];
				if (theRow[AUXILIARY_OUTPUT_IMPS] == null)
					theRow[AUXILIARY_OUTPUT_IMPS] = new ArrayList<PluginIOHyperstackViewWithImagePlus>();
				return (List<PluginIOHyperstackViewWithImagePlus>) theRow[AUXILIARY_OUTPUT_IMPS];
			}

			@Override
			public String getTableString(int lastRow) throws TableNotComputed {
				return getTableAsString(lastRow);
			}

			@Override
			public LinkToExternalProgram getExternalProgram(int row) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				Object[] theRow = (Object[]) data[row];
				return (LinkToExternalProgram) theRow[LINK_TO_EXTERNAL_PROGRAM];
			}

			@Override
			public void setExternalProgram(int row, LinkToExternalProgram l) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				Object[] theRow = (Object[]) data[row];
				theRow[LINK_TO_EXTERNAL_PROGRAM] = l;
			}

			@Override
			public int getOwnerOfOurOutput(int row) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				Object[] theRow = (Object[]) data[row];

				int ownerRow =
						resolveRowReference((String) ((AbstractParameter) theRow[OUTPUT_NAME_FIELD]).getValue(), row);
				return ownerRow;
			}

			@Override
			public boolean keepCProgramAlive(int row) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				Object[] theRow = (Object[]) data[row];

				return (Boolean) theRow[KEEP_C_PLUGIN_ALIVE];
			}

			@Override
			public void clearDestinations(int row) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				Object[] theRow = (Object[]) data[row];
				Map<String, IPluginIO> destinations = ((PipelinePlugin) theRow[PLUGIN_INSTANCE]).getOutputs();

				destinations.values().stream().filter(output -> output instanceof IPluginIOImage).map(
						output -> (IPluginIOImage) output).map(IPluginIOImage::getImp).filter(imp -> imp != null)
						.forEach(PluginIOHyperstackViewWithImagePlus::close);

				((PipelinePlugin) theRow[PLUGIN_INSTANCE]).clearOutputs();
			}

			@SuppressWarnings("unchecked")
			@Override
			public void clearView(PluginIOView view) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				for (int r = 0; r < table1.getModel().getRowCount(); r++) {
					List<PluginIOView> views = (List<PluginIOView>) ((Object[]) data[r])[AUXILIARY_OUTPUT_IMPS];
					if (views != null)
						views.remove(view);
				}
			}

			@SuppressWarnings("unchecked")
			@Override
			public void clearAllViews(int row) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				List<PluginIOView> views = (List<PluginIOView>) ((Object[]) data[row])[AUXILIARY_OUTPUT_IMPS];
				if (views != null)
					views.clear();
			}

			@Override
			public void setInputPath(int row, String path) {
				Object[] data = ((MyTableModel) table1.getModel()).data;
				String s = path != null ? FileNameUtils.compactPath(path) : "";
				((TextParameter) ((Object[]) data[row])[INPUT_NAME_FIELD]).setValue(s);
				((MyTableModel) table1.getModel()).fireTableCellUpdated(row, INPUT_NAME_FIELD);
			}

			@Override
			public RemoteMachine getRemoteMachine() {
				return null;
			}

		}

		/**
		 * Called when the plugin name has been changed at row index x. Terminates current updates, tells the current
		 * plugin to clean up, and creates a new instance of the plugin name found at column index y.
		 * 
		 * @param x
		 * @param y
		 */
		private synchronized final void udpateRowPlugin(int x, int y) {

			Object[] data = ((MyTableModel) table1.getModel()).data;
			Object[] theRow = (Object[]) data[x];
			PipelinePlugin p = (PipelinePlugin) theRow[PLUGIN_INSTANCE];
			String pluginName = (String) table1.getValueAt(x, y);
			int pluginID = Utils.indexOf(pluginNames, pluginName);
			if (pluginObjects[pluginID] == null) {
				Utils.log("Null plugin class " + pluginID, LogLevel.ERROR);
			}

			Thread t = (Thread) theRow[QUEUED_WORKER_THREAD];
			if (t != null)
				t.interrupt();
			t = (Thread) theRow[WORKER_THREAD];
			if (t != null)
				t.interrupt();

			if (p != null) {
				try {
					cleanupPluginAtRow(x);
				} catch (Exception e) {
					Utils.printStack(e);
				}
			}

			theRow[AUXILIARY_INPUTS] = null;
			theRow[AUXILIARY_OUTPUTS] = null;

			if (pluginObjects[pluginID] instanceof AuxiliaryInputOutputPlugin) {
				AuxiliaryInputOutputPlugin newPlugin = (AuxiliaryInputOutputPlugin) pluginObjects[pluginID];
				String [] inputValues = new String[newPlugin.getInputLabels().length];
				for (int i = 0; i < newPlugin.getInputLabels().length; i++) {
					inputValues[i] = "";
				}
				theRow[AUXILIARY_INPUTS] =
						new TwoColumnTableParameter(
								"Auxiliary inputs",
								"Auxiliary inputs; fill in corresponding rows; relative reference by default, $ for absolute reference",
								newPlugin.getInputLabels(), inputValues, null);
				theRow[AUXILIARY_OUTPUTS] =
						new TableParameter(
								"Auxiliary inputs",
								"Auxiliary inputs; fill in corresponding rows; relative reference by default, $ for absolute reference",
								newPlugin.getOutputLabels(), null);
			}
			theRow[PLUGIN_OUTPUTS] = null;
			theRow[PLUGIN_INPUTS] = null;

			// TODO Figure out which imps should be closed
			// (some Imps might also display channels from other PluginIOs)

			PipelinePlugin pi = null;
			try {
				pi = ((PipelinePlugin) pluginObjects[pluginID]).getClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			pi.setpipeLineListener(new PluginCallBack());

			pi.setRow(x);
			Utils.log("PluginID: " + pluginID + "; plugin name: " + pluginName + "; list at index: "
					+ pluginNames[pluginID], LogLevel.DEBUG);
			Utils.log("Flags: " + pi.getFlags(), LogLevel.DEBUG);
			if (pi instanceof ThreeDPlugin || pi instanceof TwoDPlugin) {
				// Need to use a plugin wrapper (unless the source is not a stack, but we don't necessary know ahead of
				// time)
				IPluginShell sh = null;
				try {
					sh = (IPluginShell) ((PipelinePlugin) pluginObjects[shell2Dindex]).getClass().newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				((PipelinePlugin) sh).setpipeLineListener(new PluginCallBack());
				((PipelinePlugin) sh).setRow(x);
				sh.setPlugin(pi);// pi is the plugin that will be called on every slice
				theRow[PLUGIN_INSTANCE] = sh;
			} else
				theRow[PLUGIN_INSTANCE] = pi;

			if (theRow[PLUGIN_INSTANCE] == null)
				throw new IllegalStateException(
						"Null plugin instance after plugin supposedly created; this should not happen");

			if (pluginName.equals("CActiveContourV2")) {
				theRow[KEEP_C_PLUGIN_ALIVE] = true;
			}

			theRow[PLUGIN_PARAM_1_FIELD] = ((PipelinePlugin) theRow[PLUGIN_INSTANCE]).getParameters()[0];
			theRow[PLUGIN_PARAM_2_FIELD] = ((PipelinePlugin) theRow[PLUGIN_INSTANCE]).getParameters()[1];
			((PipelinePlugin) theRow[PLUGIN_INSTANCE]).setRow(x);
		}

		/**
		 * Stop all updates from row startRow down, by interrupting the corresponding worker threads found in the table.
		 * 
		 * @param startRow
		 */
		private void stopAll(int startRow) {
			Thread t;
			Object[][] data = ((MyTableModel) table1.getModel()).data;
			for (int i = startRow; i < data.length; i++) {
				t = (Thread) data[i][WORKER_THREAD];
				if (t != null) {
					Utils.log("Interrupt worker thread for row " + i + " as a consequence of a call on " + startRow,
							LogLevel.DEBUG);
					t.interrupt();
				}
				t = (Thread) data[i][QUEUED_WORKER_THREAD];
				if (t != null) {
					Utils.log("Interrupt queued worker thread for row " + i + " as a consequence of a call on "
							+ startRow, LogLevel.DEBUG);
					t.interrupt();
				}
			}

		}

		/**
		 * Called when a parameter was updated in row "row"; checks if an update of the corresponding image is required
		 * (which depends on whether the user asked for an update to be triggered whenever a parameter changes), and
		 * cancels all updates of downstream steps already under way, if the pipeline is set to be globally reset when a
		 * parameter is changed.
		 * 
		 * @param row
		 * @param stayInCoreLoop
		 * @param changedParameter
		 */
		private void checkUpdateAtRow(int row, AbstractParameter changedParameter, boolean stayInCoreLoop) {
			if (row > -1) {
				if (((MyTableModel) table1.getModel()).updateCurrentStep) {

					if (((MyTableModel) table1.getModel()).globalCancelUponChange) {
						// Stop threads of rows AFTER the current one
						Utils.log("update at row " + row + "; looking for rows further down to interrupt",
								LogLevel.DEBUG);
						stopAll(row + 1);
					}

					Object[] theRow = ((MyTableModel) table1.getModel()).data[row];
					if (theRow[PLUGIN_INSTANCE] != null) {
						new RunPipelineTask(row, changedParameter, stayInCoreLoop).start();
					}

				}
			} else
				Utils.log("trying to update row -1", LogLevel.ERROR);
		}

		private int lastRowOver = -1;

		// From http://kalanir.blogspot.com/2010/01/how-to-write-custom-class-loader-to.html
		private class TableDemoLoader extends ClassLoader {

			public TableDemoLoader() {
				super(TableDemoLoader.class.getClassLoader());
			}

			@Override
			public Class<?> loadClass(String className) throws ClassNotFoundException {
				return findClass(className);
			}

			@Override
			public Class<?> findClass(String className) {
				return TableSelectionDemo.class;
			}

		}

		private TableDemoLoader tableDemoLoader = new TableDemoLoader();

		public TableSelectionDemo() {
			super();
			Utils.setPrependTime(true);
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;

			table1 = new JTableWithStripes(new MyTableModel()) {
				private static final long serialVersionUID = -4771266607962392023L;
				
				@Override
				//Adapted from http://stackoverflow.com/questions/27102546/show-tooltips-in-jtable-only-when-column-is-cut-off
				public String getToolTipText(MouseEvent e) {
					Point p = e.getPoint();
					int col = columnAtPoint(p);
					int row = rowAtPoint(p);
					if (row == -1 || col == -1) {
						return super.getToolTipText(e);
					}
					if (col == PLUGIN_NAME_FIELD) {
						String pluginName = (String) table1.getValueAt(row, col);
						if (pluginName == null)
							return super.getToolTipText(e);
						PluginHolder holder = plugins.get(pluginName);
						if (holder == null) {
							return super.getToolTipText(e);
						}
						return Utils.encodeHTML(WordUtils.wrap(holder.toolTip, 50, null, true)).
								replace("\n", "<br>\n");
					} else {
						return super.getToolTipText(e);
					}
				}

			};
			
		    ColumnHeaderToolTips tips = new ColumnHeaderToolTips();
			JTableHeader header = table1.getTableHeader();
		    header.addMouseMotionListener(tips);
		    for (int colIndex = 0; colIndex < table1.getColumnCount(); colIndex++) {
		      TableColumn col = table1.getColumnModel().getColumn(colIndex);
		      tips.setToolTip(col, table1.getColumnName(colIndex));
		    }
		    
			table1.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
			table1.setPreferredScrollableViewportSize(new Dimension(900, 250));
			table1.setFillsViewportHeight(true);
			table1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table1.setAutoscrolls(true);
			table1.setShowGrid(true);
			table1.setShowHorizontalLines(true);
			table1.setColumnSelectionAllowed(false);
			table1.setRowSelectionAllowed(true);
			// table1.getSelectionModel().addListSelectionListener(new RowListener());
			// table1.getColumnModel().getSelectionModel().addListSelectionListener(new ColumnListener());
			TableColumn col = table1.getColumnModel().getColumn(PERCENT_DONE);
			col.setCellRenderer(new ProgressRendererWrapper());

			TableColumn col1 = table1.getColumnModel().getColumn(PLUGIN_PARAM_1_FIELD);
			TableColumn col2 = table1.getColumnModel().getColumn(PLUGIN_PARAM_2_FIELD);
			TableColumn col3 = table1.getColumnModel().getColumn(WORK_ON_CHANNEL_FIELD);
			TableColumn col4 = table1.getColumnModel().getColumn(OUT_CHANNELS_FIELD);
			MultiRenderer multiRenderer = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer);
			MultiRenderer multiRenderer2 = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer2);
			MultiRenderer multiRenderer3 = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer3);
			MultiRenderer multiRenderer4 = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer4);
			MultiRenderer multiRenderer5 = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer5);
			MultiRenderer multiRenderer6 = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer6);
			MultiRenderer multiRenderer7 = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer7);
			MultiRenderer multiRenderer8 = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer8);

			multiRenderer.setDefaultEditor(col2.getCellEditor());
			multiRenderer2.setDefaultEditor(col2.getCellEditor());
			multiRenderer3.setDefaultEditor(col2.getCellEditor());
			multiRenderer4.setDefaultEditor(col2.getCellEditor());
			multiRenderer5.setDefaultEditor(col2.getCellEditor());
			multiRenderer6.setDefaultEditor(col2.getCellEditor());
			multiRenderer7.setDefaultEditor(col2.getCellEditor());
			multiRenderer8.setDefaultEditor(col2.getCellEditor());

			col2.setCellRenderer(multiRenderer);
			col2.setCellEditor(multiRenderer2);
			col2.setPreferredWidth(400);
			col1.setCellRenderer(multiRenderer3);
			col1.setCellEditor(multiRenderer4);
			col1.setPreferredWidth(400);
			col3.setCellRenderer(multiRenderer5);
			col3.setCellEditor(multiRenderer6);
			col3.setPreferredWidth(60);
			col4.setCellRenderer(multiRenderer7);
			col4.setCellEditor(multiRenderer8);

			table1.getColumnModel().getColumn(PLUGIN_NAME_FIELD).setPreferredWidth(200);
			table1.getColumnModel().getColumn(INPUT_NAME_FIELD).setPreferredWidth(200);

			table1.getColumnModel().getColumn(INPUT_NAME_FIELD).setCellEditor(new TextBox());
			table1.getColumnModel().getColumn(INPUT_NAME_FIELD).setCellRenderer(new TextBox());
			table1.getColumnModel().getColumn(OUTPUT_NAME_FIELD).setCellEditor(new TextBox());
			table1.getColumnModel().getColumn(OUTPUT_NAME_FIELD).setCellRenderer(new TextBox());

			table1.getColumnModel().getColumn(AUXILIARY_INPUTS).setCellRenderer(new TwoColumnJTable());
			table1.getColumnModel().getColumn(AUXILIARY_OUTPUTS).setCellRenderer(new OneColumnJTable());
			table1.getColumnModel().getColumn(AUXILIARY_INPUTS).setCellEditor(new TwoColumnJTable());
			table1.getColumnModel().getColumn(AUXILIARY_OUTPUTS).setCellEditor(new OneColumnJTable());

			table1.setRowHeight(0, 100);
			table1.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

			table1.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					int c1 = e.getKeyCode();
					if (c1 == KeyEvent.VK_ENTER) {
						updateSelectedRows();
					}
				}
			});

			table1.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent m) {
					lastRowOver = table1.rowAtPoint(m.getPoint());
					if (lastRowOver == -1) {
						if (m.getPoint().y > totalRowHeights(table1))
							lastRowOver = table1.getRowCount() - 1;
						else
							lastRowOver = 0;
					}
					mousePressedRow = table1.getSelectedRow();
				}

				@Override
				public void mouseClicked(MouseEvent e) {
					// Duplicated below
					// TODO This needs to be cleaned up and given a decent structure

					int currentColumn = table1.columnAtPoint(e.getPoint());
					if ((currentColumn == PLUGIN_PARAM_1_FIELD) || (currentColumn == PLUGIN_PARAM_2_FIELD))
						return;

					imageListMenuXCellIndex = table1.rowAtPoint(e.getPoint());
					imageListMenuYCellIndex = table1.columnAtPoint(e.getPoint());

					if (imageListMenuXCellIndex >= 0) {
						if ((imageListMenuYCellIndex == INPUT_NAME_FIELD)
								&& ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)) {
							Utils.log("Creating popup for row " + imageListMenuXCellIndex, LogLevel.VERBOSE_DEBUG);
							displayImageListMenu(imageListMenuXCellIndex, imageListMenuYCellIndex, e.getComponent(), e
									.getX(), e.getY());
						} else if ((imageListMenuYCellIndex == OUTPUT_NAME_FIELD)
								&& ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)) {
							displayImageListMenu(imageListMenuXCellIndex, imageListMenuYCellIndex, e.getComponent(), e
									.getX(), e.getY());
						} else if (imageListMenuYCellIndex == PLUGIN_NAME_FIELD) {
							displayPluginListMenu(imageListMenuXCellIndex, imageListMenuYCellIndex, e.getComponent(), e
									.getX(), e.getY());

						} else if ((imageListMenuYCellIndex == PLUGIN_PARAM_1_FIELD)
								|| (imageListMenuYCellIndex == PLUGIN_PARAM_2_FIELD)) {
							if (((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)) {
								Object param =
										table1.getModel().getValueAt(imageListMenuXCellIndex, imageListMenuYCellIndex);
								if (param instanceof SplitParameter) {
									AbstractParameter[] parameters = ((SplitParameter) param).getParameterValue();
									if ((parameters[0] instanceof FileNameParameter)
											&& (parameters[1] instanceof DirectoryParameter))
										displayFileDirectoryDialog((FileNameParameter) parameters[0],
												(DirectoryParameter) parameters[1], imageListMenuXCellIndex,
												imageListMenuYCellIndex);
								}
							}
						}
					}

					int[] selectedRows = table1.getSelectedRows();
					if (selectedRows.length > 0) {
						updateImageButton.setEnabled(true);
						deleteButton.setEnabled(true);
					} else {
						// updateImageButton.setEnabled(false);
						// deleteButton.setEnabled(false);
					}
				}

			});

			table1.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent m) {
					lastRowOver = -1;
				}
			});

			table1.addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent m)
				// This handles the rearrangement of rows by dragging the mouse
				// TODO this code is a bit of a mess; it would be nicer to use a drag and drop system
				{
					int currentColumn = table1.columnAtPoint(m.getPoint());
					if ((currentColumn == PLUGIN_PARAM_1_FIELD) || (currentColumn == PLUGIN_PARAM_2_FIELD))
						return;
					int currentRow = 0;
					/*int[] selectedRows = table1.getSelectedRows();

					if (selectedRows.length == 1) {
						currentRow = selectedRows[0];
					} else if (selectedRows.length == 2) {
						if (selectedRows[0] != mousePressedRow) {
							currentRow = selectedRows[0];
						}
						if (selectedRows[1] != mousePressedRow) {
							currentRow = selectedRows[1];
						}
					}*/
					currentRow = table1.rowAtPoint(m.getPoint());
					if (currentRow == -1) {
						if (m.getPoint().y > totalRowHeights(table1))
							currentRow = table1.getRowCount() - 1;
						else
							currentRow = 0;
					}

					table1.setRowSelectionInterval(lastRowOver, lastRowOver);
					if (currentRow == lastRowOver) {
						Utils.log("no row change: from " + lastRowOver + "to " + currentRow + "; returning",
								LogLevel.DEBUG);
						return;
					}

					if (mousePressedRow == currentRow)
						return;

					Point shiftedPoint = new Point(m.getPoint());
					int sign = currentRow > lastRowOver ? 1 : -1;
					shiftedPoint.translate(0, sign
							* (-table1.getRowHeight(currentRow) + table1.getRowHeight(lastRowOver)));
					int hypotheticalRowAfterMove = table1.rowAtPoint(shiftedPoint);
					if ((table1.getRowHeight(currentRow) >= table1.getRowHeight(lastRowOver))
							&& (hypotheticalRowAfterMove != currentRow)) {
						// Do not move yet because the row height difference would make us want to move things back next
						// time we're called, causing an infinite sequence of flickering
						return;
					}
					Utils.log("*** moving row " + mousePressedRow + " to " + currentRow, LogLevel.DEBUG);
					((MyTableModel) table1.getModel()).moveRow(lastRowOver, currentRow);// lastRowOver was
																						// mousePressedRow

					mousePressedRow = table1.rowAtPoint(m.getPoint());
					if (mousePressedRow == -1) {
						if (m.getPoint().y > totalRowHeights(table1))
							mousePressedRow = table1.getRowCount() - 1;
						else
							mousePressedRow = 0;
					}

					table1.invalidate();
					lastRowOver = currentRow;
				}

			});

			table1.getColumnModel().getColumn(0).setPreferredWidth(30);
			table1.getColumnModel().getColumn(0).setMaxWidth(30);

			table1.getColumnModel().getColumn(SHOW_IMAGE).setPreferredWidth(30);
			table1.getColumnModel().getColumn(SHOW_IMAGE).setMaxWidth(35);
			table1.getColumnModel().getColumn(KEEP_C_PLUGIN_ALIVE).setPreferredWidth(30);
			table1.getColumnModel().getColumn(KEEP_C_PLUGIN_ALIVE).setMaxWidth(30);
			table1.getColumnModel().getColumn(USE_STEP).setPreferredWidth(25);
			table1.getColumnModel().getColumn(USE_STEP).setMaxWidth(25);
			table1.getColumnModel().getColumn(RESET_RANGE).setPreferredWidth(30);
			table1.getColumnModel().getColumn(RESET_RANGE).setMaxWidth(30);
			table1.getColumnModel().getColumn(OUTPUT_LOCKS).setPreferredWidth(30);

			table1.getColumnModel().getColumn(IS_UPDATING).setPreferredWidth(25);
			table1.getColumnModel().getColumn(IS_UPDATING).setMaxWidth(25);
			table1.getColumnModel().getColumn(UPDATE_QUEUED).setPreferredWidth(25);
			table1.getColumnModel().getColumn(UPDATE_QUEUED).setMaxWidth(25);
			table1.getColumnModel().getColumn(PERCENT_DONE).setPreferredWidth(30);
			table1.getColumnModel().getColumn(PERCENT_DONE).setMaxWidth(30);

			for (int i = PERCENT_DONE + 1; i < LAST_VISIBLE_COLUMN; i++) {
				if ((i == AUXILIARY_INPUTS) || (i == AUXILIARY_OUTPUTS))
					continue;
				if (table1.getColumnModel().getColumn(i) != null)
					table1.getColumnModel().getColumn(i).setMaxWidth(10);
			}
			DataFlavor tempFlavor = null;
			try {
				tempFlavor =
						new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
								+ ";class=pipeline.A0PipeLine_Manager$TableSelectionDemo", "pipeline", tableDemoLoader);
			} catch (ClassNotFoundException e1) {
				throw new RuntimeException(e1);
			}
			final DataFlavor pipelineFlavor = tempFlavor;

			if (!Utils.headless)
				table1.setDropMode(DropMode.ON);
			if (!Utils.headless)
				table1.setTransferHandler(new TransferHandler() {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean canImport(TransferHandler.TransferSupport info) {
						JTable.DropLocation dl = (JTable.DropLocation) info.getDropLocation();
						int row = dl.getRow();
						int column = dl.getColumn();
						if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
							return isFilePathParameter(row, column) || (column == INPUT_NAME_FIELD)
									|| containsDirectoryParameter(row, column)
									|| containsDropAcceptingParameter(row, column, info);
						} else if (info.isDataFlavorSupported(pipelineFlavor)) {
							return isPipelineParameter(row, column);
						}
						info.getDataFlavors();
						return false;
					}

					// DnDUtils dndUtils=new DnDUtils();
					@SuppressWarnings({ "unchecked", "null" })
					@Override
					public boolean importData(TransferSupport support) {
						/*
						 * if (!support.isDrop()) { return false; }
						 */
						if (!canImport(support)) {
							return false;
						}

						JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();

						int row = dl.getRow();
						int column = dl.getColumn();

						if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
							List<File> list;
							try {
								list =
										(List<File>) support.getTransferable().getTransferData(
												DataFlavor.javaFileListFlavor);
							} catch (UnsupportedFlavorException e) {
								return false;
							} catch (IOException e) {
								return false;
							} catch (InvalidDnDOperationException e) {
								// Happens in FreeBSD from Dolphin for some reason
								if (support.isDataFlavorSupported(DnDUtils.getUriListDataFlavor())) {
									String uriList;
									try {
										uriList =
												(String) support.getTransferable().getTransferData(
														DnDUtils.getUriListDataFlavor());
									} catch (UnsupportedFlavorException | IOException e1) {
										Utils.printStack(e1);
										return false;
									}
									list = DnDUtils.textURIListToFileList(uriList);
								} else
									return false;
							}

							if (list.size() == 0)
								return false;
							if (isFilePathParameter(row, column))
								return ((MyTableModel) table1.getModel()).setFileParameter(row, column, list.get(0));
							else if (column == INPUT_NAME_FIELD) {
								((TextParameter) table1.getValueAt(row, column)).setValue(FileNameUtils
										.compactPath(list.get(0).getAbsoluteFile().toString()));
								return true;
							} else if (containsDirectoryParameter(row, column)) {
								return ((MyTableModel) table1.getModel()).setDirectoryParameter(row, column,
										FileNameUtils.compactPath(list.get(0).isDirectory() ? list.get(0)
												.getAbsolutePath() : list.get(0).getParent()));
							} else {
								return importFile(row, column, list.get(0).getAbsoluteFile().toString());
							}
						} else if (isPipelineParameter(row, column)) {
							PipelineParameter p = ((PipelineParameter) table1.getValueAt(row, column));

							TableSelectionDemo pipeline = null;
							try {
								pipeline =
										(TableSelectionDemo) support.getTransferable().getTransferData(pipelineFlavor);
							} catch (UnsupportedFlavorException e) {
								throw new IllegalStateException(e);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
							p.setPipeline(pipeline);
							p.setValue(pipeline.pipelineName);
							return true;
						} else
							throw new RuntimeException("Unrecognized column " + column);
					}
				});

			// c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 0;
			c.weighty = 0.3;
			c.weightx = 1.0;
			sp = new JScrollPane(table1);
			// AquaScrollBarUI j=sp.getVerticalScrollBar().getUI();
			/*
			 * int i =sp.getVerticalScrollBar().getUnitIncrement(1);
			 * sp.putClientProperty("JScrollBar.fastWheelScrolling", Boolean.TRUE); BasicScrollPaneUI hh=
			 * (BasicScrollPaneUI) sp.getUI();
			 */
			// sp.getVerticalScrollBar().setUnitIncrement(10); THIS MAKES UPDATES VERY SLOW, AS UPDATES ARE APPARENTLY
			// NOT COALESCED
			sp.getVerticalScrollBar().setUnitIncrement(30);
			add(sp, c);
			c.weighty = 0.0;

			JPanel workflowControls = new JPanel();

			toFrontButton = new JButton("Bring outputs to front");
			toFrontButton.setActionCommand("Bring outputs to front");
			toFrontButton.addActionListener(this);
			workflowControls.add(toFrontButton);

			JButton makeCompositeButton = new JButton("Make composite");
			makeCompositeButton.setActionCommand("Make composite");
			makeCompositeButton.addActionListener(this);
			workflowControls.add(makeCompositeButton);

			JButton revertToChannelsButton = new JButton("Revert to channels");
			revertToChannelsButton.setActionCommand("Revert to channels");
			revertToChannelsButton.addActionListener(this);
			workflowControls.add(revertToChannelsButton);

			deleteButton = new JButton("Delete step");
			deleteButton.setActionCommand("Delete");
			deleteButton.addActionListener(this);
			workflowControls.add(deleteButton);

			newButton = new JButton("New step");
			newButton.setActionCommand("New");
			newButton.addActionListener(this);
			workflowControls.add(newButton);

			updateImageButton = new JButton("Update step");
			updateImageButton.setActionCommand("Update step");
			updateImageButton.addActionListener(this);
			workflowControls.add(updateImageButton);

			cancelUpdatesButton = new JButton("Stop all updates");
			cancelUpdatesButton.setActionCommand("Stop all updates");
			cancelUpdatesButton.addActionListener(this);
			workflowControls.add(cancelUpdatesButton);

			updateImageButton.setEnabled(true);
			deleteButton.setEnabled(true);

			c.gridx = 0;
			c.gridy = 1;
			add(workflowControls, c);

			JPanel globalOptionPanel = new JPanel();
			c.gridx = 0;
			c.gridy = 2;
			add(globalOptionPanel, c);

			JPanel globalOptionPanel2 = new JPanel();
			c.gridx = 0;
			c.gridy = 3;
			add(globalOptionPanel2, c);

			((MyTableModel) table1.getModel()).updatePipeline = true;
			((MyTableModel) table1.getModel()).updateCurrentStep = true;
			((MyTableModel) table1.getModel()).cancelUponChange = true;
			((MyTableModel) table1.getModel()).globalCancelUponChange = false;

			final JCheckBox stopOnErrorCB = new JCheckBox("Stop on error");
			stopOnErrorCB.addActionListener(e -> stopOnError = (stopOnErrorCB.isSelected()));
			stopOnErrorCB.setSelected(true);
			globalOptionPanel.add(stopOnErrorCB);

			final JCheckBox groupFiles = new JCheckBox("Group files");
			groupFiles.setSelected(true);
			globalOptionPanel2.add(groupFiles);

			final String dropFilesLabelText = "<----      Drop files      ---->";
			final JLabel dropFiles = new JLabel(dropFilesLabelText);
			globalOptionPanel2.add(dropFiles);

			final JLabel dragMe = new JLabel("Drag me");
			globalOptionPanel.add(dragMe);
			if (!Utils.headless)
				dragMe.setTransferHandler(new TransferHandler() {
					private static final long serialVersionUID = 1L;

					@Override
					public int getSourceActions(JComponent c) {
						return TransferHandler.COPY;
					}

					@Override
					public Transferable createTransferable(JComponent c) {
						return new Transferable() {

							@Override
							public boolean isDataFlavorSupported(DataFlavor flavor) {
								return flavor.getClass().equals(TableSelectionDemo.class);
							}

							@Override
							public DataFlavor[] getTransferDataFlavors() {
								return new DataFlavor[] { pipelineFlavor };
							}

							@Override
							public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
									IOException {
								return TableSelectionDemo.this;
							}
						};
					}

					@Override
					public boolean canImport(TransferHandler.TransferSupport info) {
						return false;
					}

					@Override
					public boolean importData(TransferSupport support) {
						return false;
					}
				});

			dragMe.addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					TransferHandler handler = dragMe.getTransferHandler();
					handler.exportAsDrag(dragMe, e, TransferHandler.COPY);
				}
			});

			final JCheckBox vnc = addCheckBox("VNC settings");
			vnc.addActionListener(e -> Utils.setVNCSettings(vnc.isSelected()));
			vnc.setSelected(false);
			globalOptionPanel.add(vnc);

			updateCurrentStepButton = addCheckBox("Update current step upon param change");
			updateCurrentStepButton.setSelected(true);
			globalOptionPanel.add(updateCurrentStepButton);
			updatePipelineButton = addCheckBox("Update pipeline upon param change");
			updatePipelineButton.setSelected(true);
			globalOptionPanel.add(updatePipelineButton);

			JCheckBox openWithVirtualStacks = addCheckBox("Use virtual stacks to open files");
			openWithVirtualStacks.setSelected(false);
			globalOptionPanel.add(openWithVirtualStacks);

			String hostName = "";
			if (!IJ.isMacintosh())
				try {
					/*
					 * java.net.InetAddress addr = java.net.InetAddress.getLocalHost(); hostName=addr.getHostName();
					 * DOES NOT WORK ON FREEBSD
					 */
					Process process = Runtime.getRuntime().exec(new String[] { "hostname", "-s" });
					try (Scanner scanner = new Scanner(process.getInputStream())) {
						hostName = scanner.useDelimiter("\\A").next();
					}
					hostName = hostName.substring(0, hostName.length() - 1) + "l";
				} catch (IOException e1) {
					Utils.printStack(e1);
				}

			remoteMachine = new JTextArea(hostName);
			globalOptionPanel.add(remoteMachine);

			JPanel controls2 = new JPanel();
			c.gridx = 0;
			c.gridy = 4;
			add(controls2, c);

			if (!Utils.headless)
				try {

					TransferHandler transferHandler = new TransferHandler() {

						private static final long serialVersionUID = 1L;

						@Override
						public boolean canImport(TransferHandler.TransferSupport info) {
							return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
						}

						@SuppressWarnings("unchecked")
						@Override
						public boolean importData(TransferSupport support) {
							if (!canImport(support)) {
								return false;
							}

							List<File> lf;
							try {
								lf =
										(List<File>) support.getTransferable().getTransferData(
												DataFlavor.javaFileListFlavor);
							} catch (UnsupportedFlavorException e) {
								return false;
							} catch (IOException e) {
								return false;
							} catch (InvalidDnDOperationException e) {
								// Happens in FreeBSD from Dolphin for some reason
								if (support.isDataFlavorSupported(DnDUtils.getUriListDataFlavor())) {
									String uriList;
									try {
										uriList =
												(String) support.getTransferable().getTransferData(
														DnDUtils.getUriListDataFlavor());
									} catch (UnsupportedFlavorException | IOException e1) {
										Utils.printStack(e1);
										return false;
									}
									lf = DnDUtils.textURIListToFileList(uriList);
								} else
									return false;
							}
							final List<File> files = lf;

							if (files.size() == 0)
								return false;

							Runnable r = new Runnable() {

								@Override
								public void run() {
									try {

										List<PluginIOCells> cells = new ArrayList<>();
										List<PluginIOImage> images = new ArrayList<>();
										List<String> xmlFiles = new ArrayList<>();
										for (File file : files) {
											String cleanedUpName = file.getAbsolutePath();
											int dot = cleanedUpName.lastIndexOf('.');
											String extension = dot == -1 ? "" : cleanedUpName.substring(dot + 1);
											String originalCleanedUpName = cleanedUpName;

											switch (extension) {
												case "proto":
													cells.add(new PluginIOCells(new File(cleanedUpName)));
													break;
												case "tif":
												case "tiff":
												case "lsm":
													// File does not exist, so create a new accessor and put it in the
													// hashmap
													TIFFFileAccessor tiffReader;
													try {
														tiffReader =
																new TIFFFileAccessor(file, FileNameUtils
																		.compactPath(originalCleanedUpName));
														if (((MyTableModel) table1.getModel()).openUsingVirtualStacks)
															tiffReader
																	.setDefaultCachePolicy(ImageAccessor.DONT_CACHE_PIXELS);
														else {
															if (file.length() > 30000000000L) {
																tiffReader
																		.setDefaultCachePolicy(ImageAccessor.DONT_CACHE_PIXELS);
																Utils.displayMessage(
																		"File "
																				+ file.getAbsolutePath()
																				+ " is over ~30GB; needs to be opened with Z projector instead",
																		true, LogLevel.ERROR);
																throw new RuntimeException(
																		"File too big to be handled by LazyCopy");
															}
														}
														tiffReader.openForSequentialRead();
														tiffReader.setOriginalSourceFile(file);
													} catch (Exception e) {
														throw (new PluginRuntimeException("Could not open TIFF file "
																+ FileNameUtils.compactPath(cleanedUpName), e, true));
													}
													images.add(tiffReader);
													break;
												case "xml":
													xmlFiles.add(cleanedUpName);
													break;
												default:
													Utils.displayMessage(
															"Could not open file "
																	+ FileNameUtils.compactPath(cleanedUpName)
																	+ (extension.equals("")
																			? " because of absent extension "
																			: " because of unrecognized extension "
																					+ extension), true, LogLevel.ERROR);
											}

											if (!groupFiles.isSelected() && images.size() > 0) {
												openImages(images, "");
												images.clear();
											}

											if (!groupFiles.isSelected() && cells.size() > 0) {
												openProtos(cells);
												cells.clear();
											}
										}

										if (xmlFiles.size() == 1) {
											loadTable(xmlFiles.get(0));
										} else if (xmlFiles.size() > 1) {
											for (String path : xmlFiles) {
												A0PipeLine_Manager pipeline = new A0PipeLine_Manager();
												final TableSelectionDemo table = pipeline.new TableSelectionDemo();
												table.loadTable(path);
												SwingUtilities.invokeLater(table::createAndShowGUI);
											}
										}

										if (groupFiles.isSelected()) {
											if (images.size() == 0) {
												// .proto files only
												openProtos(cells);
											} else {
												String addToWindowName = "";
												if (cells.size() > 0) {
													addToWindowName = cells.get(0).getName();
												}
												IPluginIOImage createdImage = openImages(images, addToWindowName);
												if (cells.size() > 0) {
													createdImage.getImp().setCellsToOverlay(cells.get(0));
												}
												if (cells.size() > 1) {
													String message = "Multiple .proto files detected; only 1 was used";
													Utils.displayMessage(message, true, LogLevel.WARNING);
												}
											}
										}
									} catch (DimensionMismatchException e) {
										Utils.displayMessage(
												"Input images do not have same dimensions; please unselect Group files option and retry.",
												true, LogLevel.ERROR);
									} catch (Exception e) {
										Utils.displayMessage("Error opening files: " + e.getMessage(), false,
												LogLevel.ERROR);
										Utils.printStack(e, LogLevel.ERROR);
									} finally {
										SwingUtilities.invokeLater(() -> dropFiles.setText(dropFilesLabelText));
									}
								}

								private void openProtos(final List<PluginIOCells> cells) {
									final List<PluginIOCells> cellsCopy = new ArrayList<>(cells);
									SwingUtilities.invokeLater(() -> {
										for (PluginIOCells c0 : cellsCopy)
											new ListOfPointsView<>(c0).show();
									});
								}

								private IPluginIOImage openImages(List<PluginIOImage> images, String addToWindowName)
										throws InterruptedException {
									FourDPlugin lazyCopyPlugin;
									String windowName = new String();
									try {
										lazyCopyPlugin = LazyCopy.class.newInstance();
									} catch (InstantiationException | IllegalAccessException e) {
										throw new RuntimeException(e);
									}
									lazyCopyPlugin.setpipeLineListener(new PluginCallBack());

									Map<String, IPluginIO> inputs = lazyCopyPlugin.getInputs();

									for (PluginIOImage image : images) {
										String name = image.getName();

										if (!windowName.equals("")) {
											windowName += "_" + FileNameUtils.getShortNameFromPath(name, 20);
										} else {
											windowName += FileNameUtils.getShortNameFromPath(name, 40);
										}
										int index = 0;
										while (inputs.containsKey(name)) {
											name += index;
										}
										inputs.put(image.getName(), image);
									}

									if (!"".equals(addToWindowName))
										windowName += "_" + addToWindowName;

									lazyCopyPlugin.createOutput(windowName, null, null);
									lazyCopyPlugin.run(null, null, null, null, true, null, false);
									IPluginIOImage createdImage = ((IPluginIOImage) lazyCopyPlugin.getOutput());
									createdImage.getImp().show();
									createdImage.getImp().toComposite();
									return createdImage;
								}
							};
							dropFiles.setText("<----      PROCESSING      ---->");
							BasePipelinePlugin.threadPool.submit(r);
							return true;
						}
					};

					globalOptionPanel.setTransferHandler(transferHandler);
					globalOptionPanel2.setTransferHandler(transferHandler);
					workflowControls.setTransferHandler(transferHandler);
					controls2.setTransferHandler(transferHandler);

					DropTargetListener dropTargerListener = new DropTargetListener() {

						@Override
						public void dropActionChanged(DropTargetDragEvent dtde) {
						}

						@Override
						public void drop(DropTargetDropEvent dtde) {
						}

						@Override
						public void dragOver(DropTargetDragEvent dtde) {
						}

						@Override
						public void dragExit(DropTargetEvent dte) {
							dropFiles.setText(dropFilesLabelText);
						}

						@Override
						public void dragEnter(DropTargetDragEvent dtde) {
							dropFiles.setText("<----      DROP FILES      ---->");
						}
					};
					globalOptionPanel.getDropTarget().addDropTargetListener(dropTargerListener);
					globalOptionPanel2.getDropTarget().addDropTargetListener(dropTargerListener);
					workflowControls.getDropTarget().addDropTargetListener(dropTargerListener);
					controls2.getDropTarget().addDropTargetListener(dropTargerListener);
				} catch (TooManyListenersException e) {
					Utils.printStack(e);
				}

			suppressLog = addCheckBox("Suppress log");
			suppressLog.setSelected(false);
			globalOptionPanel2.add(suppressLog);

			suppressWarningPopups = addCheckBox("Suppress warning popups");
			suppressWarningPopups.setSelected(false);
			globalOptionPanel2.add(suppressWarningPopups);

			logLevel = new JComboBox<>(Utils.LogLevelNames);
			globalOptionPanel2.add(logLevel);
			logLevel.addActionListener(e -> Utils.logLevelThreshold =
					Utils.indexOf(Utils.LogLevelNames, (String) logLevel.getSelectedItem()));
			logLevel.setSelectedIndex(Utils.LogLevel.INFO);

			keepWindowOnTopButton = new JCheckBox("Keep table on top");
			keepWindowOnTopButton.setActionCommand("Keep table on top");
			keepWindowOnTopButton.addActionListener(this);
			globalOptionPanel2.add(keepWindowOnTopButton);

			controlGroupButton = new ButtonGroup();

			JButton setAllDirectoriesButton = new JButton("Set all directories");
			setAllDirectoriesButton.setActionCommand("Set all directories");
			setAllDirectoriesButton.addActionListener(this);
			controls2.add(setAllDirectoriesButton);

			saveTableButton = new JButton("Save table...");
			saveTableButton.setActionCommand("Save table");
			saveTableButton.addActionListener(this);
			controls2.add(saveTableButton);
			loadTableButton = new JButton("Load table...");
			loadTableButton.setActionCommand("Load table");
			loadTableButton.addActionListener(this);

			if (!Utils.headless)
				loadTableButton.setDropTarget(new DropTarget() {
					private static final long serialVersionUID = 1L;

					@SuppressWarnings("unchecked")
					@Override
					public synchronized void drop(DropTargetDropEvent dtde) {
						loadTableButton.setName("Load table...");
						loadTableButton.invalidate();
						Transferable t = dtde.getTransferable();
						List<File> list = null;
						if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
							try {
								Utils.log("Accept", LogLevel.VERBOSE_DEBUG);
								dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
								list = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
							} catch (UnsupportedFlavorException | IOException e) {
								Utils.printStack(e, LogLevel.DEBUG);
								dtde.rejectDrop();
								return;
							} catch (InvalidDnDOperationException e) {
								// Happens in FreeBSD from Dolphin for some reason
								if (t.isDataFlavorSupported(DnDUtils.getUriListDataFlavor())) {
									String uriList;
									try {
										uriList = (String) t.getTransferData(DnDUtils.getUriListDataFlavor());
									} catch (UnsupportedFlavorException | IOException e1) {
										Utils.printStack(e1, LogLevel.DEBUG);
										return;
									}
									list = DnDUtils.textURIListToFileList(uriList);
								}
							}
						} else {
							Utils.log(t.toString(), LogLevel.DEBUG);
							dtde.rejectDrop();
							return;
						}

						File f = list.get(0);
						int dotPosition = f.getName().lastIndexOf('.');
						String extension = f.getName().substring(dotPosition + 1);

						if (!"xml".equals(extension)) {
							Utils.displayMessage("File " + f.getName() + " does not have a .xml extension", true,
									LogLevel.ERROR);
							// dtde.rejectDrop();
							return;
						}
						try {
							loadTable(f.getAbsolutePath());
							dtde.dropComplete(true);
						} catch (IOException e) {
							Utils.printStack(e);
							dtde.dropComplete(false);
						}
					}

					@Override
					public synchronized void dragEnter(DropTargetDragEvent dtde) {
						super.dragEnter(dtde);
						Transferable t = dtde.getTransferable();
						Utils.log(t.toString(), LogLevel.DEBUG);

						loadTableButton.setName("LOAD TABLE");
						loadTableButton.invalidate();
					}

					@Override
					public synchronized void dragExit(DropTargetEvent dte) {
						super.dragExit(dte);
						loadTableButton.setName("Load table...");
						loadTableButton.invalidate();
					}
				});

			controls2.add(loadTableButton);

			JButton resetButton = new JButton("Reset");
			resetButton.setActionCommand("Reset");
			resetButton.addActionListener(this);
			controls2.add(resetButton);

			openNextButton = new JButton("Open next");
			openNextButton.setActionCommand("Open next");
			openNextButton.addActionListener(this);
			controls2.add(openNextButton);

			JButton openNextAndRunButton = new JButton("Open next and run");
			openNextAndRunButton.setActionCommand("Open next and run");
			openNextAndRunButton.addActionListener(this);
			controls2.add(openNextAndRunButton);

			JButton batchButton = new JButton("Batch run");
			batchButton.setActionCommand("Batch run");
			batchButton.addActionListener(this);
			controls2.add(batchButton);

			runButton = new JButton("Run");
			runButton.setActionCommand("Run");
			runButton.addActionListener(this);
			controlGroupButton.add(runButton);

			controls2.add(runButton);

			final JPanel memoryPanel = new JPanel();
			JButton collectGarbageButton = new JButton("Collect garbage");
			collectGarbageButton.setActionCommand("Collect garbage");
			collectGarbageButton.addActionListener(this);
			memoryPanel.add(collectGarbageButton);
			/*
			 * memoryDisplay=new JTextField(""); memoryDisplay.setEditable(false); memoryPanel.add(memoryDisplay);
			 */

			/*
			 * decompressionProgress=new JProgressBar(); memoryPanel.add(new JLabel("Decompression progress"));
			 * memoryPanel.add(decompressionProgress); c.gridx = 0; c.gridy = 5; add(memoryPanel,c);
			 * 
			 * output = new JTextArea(5, 40); output.setEditable(false); c.gridx = 0; c.gridy = 6; c.weighty=0.7;
			 * c.weightx=1.0;
			 */

			Utils.log("Loading plugins", LogLevel.DEBUG);
			loadPlugins();
		}

		private boolean isFilePathParameter(int row, int column) {
			if ((column == PLUGIN_PARAM_1_FIELD) || (column == PLUGIN_PARAM_2_FIELD)) {
				Object param = table1.getModel().getValueAt(row, column);
				if (param instanceof SplitParameter) {
					AbstractParameter[] parameters = ((SplitParameter) param).getParameterValue();
					if ((parameters[0] instanceof FileNameParameter) && (parameters[1] instanceof DirectoryParameter))
						return true;
				}
			}
			return false;
		}

		private boolean containsDirectoryParameter(int row, int column) {
			if ((column == PLUGIN_PARAM_1_FIELD) || (column == PLUGIN_PARAM_2_FIELD)) {
				Object param = table1.getModel().getValueAt(row, column);
				if (param instanceof SplitParameter) {
					AbstractParameter[] parameters = ((SplitParameter) param).getParameterValue();
					for (AbstractParameter p : parameters) {
						if (p instanceof DirectoryParameter)
							return true;
					}
				}
			}
			return false;
		}

		private boolean containsDropAcceptingParameter(int row, int column, TransferHandler.TransferSupport info) {
			if ((column == PLUGIN_PARAM_1_FIELD) || (column == PLUGIN_PARAM_2_FIELD)) {
				Object param = table1.getModel().getValueAt(row, column);
				if (param instanceof DropAcceptingParameter) {
					return ((DropAcceptingParameter) param).canImport(info);
				}
			}
			return false;
		}

		private boolean importFile(int row, int column, String path) {
			if ((column == PLUGIN_PARAM_1_FIELD) || (column == PLUGIN_PARAM_2_FIELD)) {
				Object param = table1.getModel().getValueAt(row, column);
				if (param instanceof DropAcceptingParameter) {
					return ((DropAcceptingParameter) param).importPreprocessedData(path);
				}
			}
			return false;
		}

		private boolean isPipelineParameter(int row, int column) {
			if ((column == PLUGIN_PARAM_1_FIELD) || (column == PLUGIN_PARAM_2_FIELD)) {
				Object param = table1.getModel().getValueAt(row, column);
				return (param instanceof PipelineParameter);
			}
			return false;
		}

		private int totalRowHeights(JTable table1) {
			int total = 0;
			for (int i = 0; i < table1.getRowCount(); i++) {
				total += table1.getRowHeight(i);
			}
			return total;
		}

		private void loadPlugins() {
			setupPluginsAndMacrosPaths();

			pluginNames = getOurPlugins();
			if (pluginNames == null)
				throw new RuntimeException(
						"No plugins identified, possibly because plugin directory could not be found");
			pluginObjects = new Object[pluginNames.length];
			pluginHash = new String[pluginNames.length];
			longPluginNames = new String[pluginNames.length];
			ParFor parFor = new ParFor(0, pluginNames.length - 1, null, BasePipelinePlugin.threadPool, true);

			for (int i0 = 0; i0 < parFor.getNThreads(); i0++)
				parFor.addLoopWorker((i, threadIndex) -> {
					pluginObjects[i] = loadUserPlugIn(pluginNames[i]);
					if (pluginObjects[i] == null) {
						Utils.log("Could not load plugin " + pluginNames[i], LogLevel.ERROR);
						return null;
					}
					pluginHash[i] = findHash(pluginNames[i]);

					if (pluginNames[i].equals("pipeline.plugins.PluginShell")) {
						shell2Dindex = i;
					}

					String[] components = pluginNames[i].split("\\.");
					String lastName = components[components.length - 1];
					PluginHolder holder = plugins.get(lastName);

					pluginNames[i] = lastName;
					longPluginNames[i] = holder.longName;

					return null;
				});
			try {
				parFor.run(true);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		private int shell2Dindex;

		private String findHash(String name) {
			for (Object[] pluginHash : pluginHashes) {
				if (pluginHash[0] != null) {
					if (pluginHash[1] != null) {
						if (pluginHash[0].equals(name)) {
							return ((String) pluginHash[1]);
						}
					}
				}
			}
			return "";
		}

		private class ProgressRendererWrapper implements TableCellRenderer {
			@Override
			public Component getTableCellRendererComponent(JTable table11, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				return (java.awt.Component) value;
			}
		}

		private ConcurrentLinkedQueue<URL> jarFiles = new ConcurrentLinkedQueue<>();
		private Object[] pluginObjects;
		private Object[][] pluginHashes;
		private String[] pluginHash;
		private String pluginsPath;

		// Copied from ImageJ
		private void setupPluginsAndMacrosPaths() {
			File f;
			pluginsPath = null;
			String homeDir = Prefs.getHomeDir();
			if (homeDir == null)
				homeDir = getBaseDir() + "/plugins";

			if (homeDir.endsWith("plugins"))
				pluginsPath = homeDir + Utils.fileNameSeparator;
			else {
				String property = System.getProperty("plugins.dir");
				if (property != null && (property.endsWith("/") || property.endsWith("\\")))
					property = property.substring(0, property.length() - 1);
				String pluginsDir = property;
				if (pluginsDir == null)
					pluginsDir = homeDir;
				else if (pluginsDir.equals("user.home")) {
					pluginsDir = System.getProperty("user.home");
					if (!(new File(pluginsDir + Utils.fileNameSeparator + "plugins")).isDirectory())
						pluginsDir = pluginsDir + Utils.fileNameSeparator + "ImageJ";
					property = null;
				}
				pluginsPath = pluginsDir + Utils.fileNameSeparator + "plugins" + Utils.fileNameSeparator;
				if (property != null && !(new File(pluginsPath)).isDirectory())
					pluginsPath = pluginsDir + Utils.fileNameSeparator;
			}
			f = pluginsPath != null ? new File(pluginsPath) : null;
			if (f == null || (!f.isDirectory())) {
				pluginsPath = null;
				return;
			}
		}

		@SuppressWarnings({ "unchecked", "unused" })
		private void loadPluginsX() {
			Map<String, Class<? extends PipelinePlugin>> plugins = new HashMap<>();
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			try {
				Enumeration<URL> aa = classLoader.getResources("pipeline");
				File pluginDirectory = new File(classLoader.getResources("pipeline/plugins").nextElement().getFile());
				File[] pluginFiles = pluginDirectory.listFiles();
				for (File classFile : pluginFiles) {
					if (!classFile.isDirectory()) {
						String className = classFile.getName().substring(0, classFile.getName().length() - 6);
						try {
							Class<? extends PipelinePlugin> pluginClass =
									(Class<? extends PipelinePlugin>) Class.forName("pipeline.plugins." + className);
							plugins.put(className, pluginClass);
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private boolean isPlugin(Class<?> clazz) {
			if (clazz.getName().contains("$"))
				return false;

			if ((!PipelinePlugin.class.isAssignableFrom(clazz) && !IPluginShell.class.isAssignableFrom(clazz))
					|| Modifier.isAbstract(clazz.getModifiers()))
				return false;

			Utils.log("Found plugin " + clazz.getName(), LogLevel.VERBOSE_DEBUG);
			PluginInfo pluginInfo = clazz.getAnnotation(PluginInfo.class);
			if (pluginInfo != null) {
				Utils.log("Plugin " + clazz.getName() + " display =" + pluginInfo.displayToUser(),
						LogLevel.VERBOSE_DEBUG);
			}
			return true;
		}

		private String[] getOurPlugins() {
			File f = pluginsPath != null ? new File(pluginsPath) : null;
			if (f == null || (!f.isDirectory()))
				return null;
			/*
			 * final String[] pluginDirEntries = f.list(); if (pluginDirEntries==null) return null;
			 */
			// For now, only look in one specific .jar file
			// TODO Add a system to specify which .jar files to scan
			final String[] pluginDirEntries = new String[] { "A0PipeLine_Manager.jar" };

			final ConcurrentLinkedQueue<String> pluginNames = new ConcurrentLinkedQueue<>();
			pluginHashes = new Object[pluginDirEntries.length][2];

			ParFor parFor = new ParFor(0, pluginDirEntries.length - 1, null, BasePipelinePlugin.threadPool, true);

			for (int i0 = 0; i0 < parFor.getNThreads(); i0++)
				parFor.addLoopWorker((i, threadIndex) -> {
					String name = pluginDirEntries[i];

					if ((name.endsWith(".jar") || name.endsWith(".zip"))) {
						try {
							jarFiles.add(new File(pluginsPath + name).toURI().toURL());
						} catch (MalformedURLException e) {
							throw new RuntimeException(e);
						}
						getPluginsInJar(pluginsPath + Utils.fileNameSeparator + name, pluginNames);
					} else { // Ignore; not a jar or zip file
						// checkSubdirectory(pluginsPath, name, v);
					}
					return null;
				});

			try {
				parFor.run(true);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			String[] result = pluginNames.toArray(new String[0]);
			StringSorter.sort(result);
			return result;
		}

		// We're "leaking" a classloader but that's probably unavoidable since we store classes in a variable outside of
		// the scope of this method
		@SuppressWarnings("resource")
		// Adapted from http://www.rgagnon.com/javadetails/java-0513.html
				private
				void getPluginsInJar(String jarName, ConcurrentLinkedQueue<String> outPluginNames) {
			boolean debug = false;

			final ClassLoader loader =
					URLClassLoader.newInstance(jarFiles.toArray(jarFiles.toArray(new URL[jarFiles.size()])), getClass()
							.getClassLoader());

			JarInputStream jarFile = null;
			try {
				jarFile = new JarInputStream(new FileInputStream(jarName));

				while (true) {
					JarEntry jarEntry = jarFile.getNextJarEntry();
					if (jarEntry == null) {
						break;
					}
					String n = jarEntry.getName();
					if (!n.endsWith(".class") || n.contains("$"))
						continue;
					String className = n.substring(0, n.length() - 6);// Remove ".class"
					className = className.replaceAll("/", "\\.");
					if (!className.contains("pipeline"))
						continue;
					Class<?> jarClass = Class.forName(className, false, loader);
					if (isPlugin(jarClass)) {
						if (debug)
							Utils.log("Found " + className, LogLevel.DEBUG);
						outPluginNames.add(className);

						String[] components = className.split("\\.");
						String lastName = components[components.length - 1];
						String longName = components[components.length - 2] + "." + lastName;

						String toolTip = null;
						try {
							toolTip = ((PipelinePlugin) jarClass.newInstance()).getToolTip();
						} catch (IllegalAccessException | InstantiationException e) {
							Utils.printStack(e, LogLevel.INFO);
						}

						PluginInfo pluginInfo = jarClass.getAnnotation(PluginInfo.class);

						boolean obsolete, display;

						if (pluginInfo != null) {
							obsolete = pluginInfo.obsolete();
							display =
									pluginInfo.displayToUser() && (!obsolete)
											&& (pluginInfo.displayToExpertLevel() < PLUGIN_EXPERT_LEVEL_DISPLAY);
						} else {
							obsolete = false;
							display = true;
						}

						@SuppressWarnings("unchecked")
						PluginHolder holder =
								new PluginHolder((Class<? extends PipelinePlugin>) jarClass, lastName, longName,
										toolTip, display, obsolete);

						if (plugins.containsKey(lastName)) {
							// XXX There is a race condition
							throw new IllegalStateException("Two plugins with the name " + lastName);
						}
						plugins.put(lastName, holder);
					}
				}
			} catch (IOException | ClassNotFoundException | SecurityException e) {
				Utils.log("Error while loading plugins from " + jarName + " in " + new ArrayList<URL>(jarFiles),
						LogLevel.ERROR);
				Utils.printStack(e);
			} finally {
				if (jarFile != null)
					try {
						jarFile.close();
					} catch (IOException e) {
						Utils.printStack(e);
					}
			}
		}

		private Object loadUserPlugIn(String className) {
			ClassLoader loader = getClass().getClassLoader();
			Object plugin = null;
			try {
				plugin = (loader.loadClass(className)).newInstance();
				if (plugin == null) {
					Utils.log("Error in loadUserPlugIn loading plugin " + className, LogLevel.ERROR);
					return null;
				}
				Utils.log("Loaded plugin " + className, LogLevel.DEBUG);
				if (!(plugin instanceof PipelinePlugin))
					Utils.log("Plugin " + className + " is not of the right class", LogLevel.ERROR);
			} catch (ClassNotFoundException e) {
				Utils.log("Plugin or class not found: \"" + className + "\"\n(" + e + ")", LogLevel.ERROR);
				Utils.printStack(e);
			} catch (NoClassDefFoundError e) {
				int dotIndex = className.indexOf('.');
				if (dotIndex >= 0)
					return loadUserPlugIn(className.substring(dotIndex + 1));
				if (className.indexOf('$') != -1)
					Utils.log("Plugin or class not found: \"" + className + "\"\n(" + e + ")", LogLevel.ERROR);
			} catch (InstantiationException e) {
				Utils.log("Unable to load plugin " + className, LogLevel.ERROR);
				Utils.printStack(e);
			} catch (IllegalAccessException e) {
				Utils.log("Unable to load plugin, possibly because it is not public.", LogLevel.ERROR);
				Utils.printStack(e);
			}
			return plugin;
		}

		private void closeAll(Map<String, IPluginIO> hashMap) {
			for (IPluginIO inputOutput : hashMap.values().toArray(new IPluginIO[] {})) {
				if (inputOutput instanceof IPluginIOImage) {
					if (((IPluginIOImage) inputOutput).getImp() != null)
						((IPluginIOImage) inputOutput).getImp().close();
				}
			}
		}

		private String lastExtractedDirectory;

		/**
		 * Examines the content of the RowOrFileTextReference at the specified position in the table. If it contains an
		 * incrementable file name (i.e. if the name contains braces), the name is incremented and an attempt is made to
		 * open the new file using either the directory the previous file was in (if it was already open), or the most
		 * recent directory that was successfully used to open an image from an incremented file name. The previous
		 * image is closed.
		 * 
		 * @param row
		 * @param nameColumn
		 *            Index of the column in the table containing the RowOrFileTextReference with the file name
		 * @param impColumn
		 *            Index of the column where the reference to the opened ImagePlus is to be stored
		 * @param displayErrorMessage
		 * @throws Utils.ImageOpenFailed
		 */
		private void checkForBracesAndUpdate(int row, int nameColumn, int impColumn,
				Map<String, IPluginIO> inputsOrOutputs, boolean displayErrorMessage) throws Utils.ImageOpenFailed {
			Object[][] data = ((MyTableModel) table1.getModel()).data;
			Object[] theRow = data[row];

			RowOrFileTextReference parameter = (RowOrFileTextReference) theRow[nameColumn];
			String str = (String) parameter.getValue();

			String newName = FileNameUtils.incrementName(str);
			if (newName.equals(str)) {// There was not anything to increment
				closeAll(inputsOrOutputs);
				return;
			}

			String extractedDirectory = "";
			if (!Utils.headless) {
				try {
					ImagePlus imp = findAnImp(inputsOrOutputs).imp;
					if ((imp != null) && (imp.getOriginalFileInfo() != null))
						extractedDirectory = imp.getOriginalFileInfo().directory;
					else
						extractedDirectory = lastExtractedDirectory;
				} catch (Exception e) {
					Utils.log("could not get directory for imp ", LogLevel.DEBUG);
					Utils.log("trying to use " + lastExtractedDirectory, LogLevel.DEBUG);
					Utils.printStack(e, LogLevel.DEBUG);
					extractedDirectory = lastExtractedDirectory;// Try to reuse a directory we know of if the imp got
					// closed
				}
			}
			lastExtractedDirectory = extractedDirectory;

			HashMap<String, ImagePlus> impList = computeImagePlusList();
			ImagePlus imp = impList.get(newName);

			if (extractedDirectory == null)
				extractedDirectory = "";

			String extractedDirectoryNoTrailing = new String(extractedDirectory);
			String impDirectoryNoTrailing = "";

			if ((imp != null) && (imp.getOriginalFileInfo() != null)) {
				impDirectoryNoTrailing = new String(imp.getOriginalFileInfo().directory);
				while (extractedDirectoryNoTrailing.lastIndexOf(Utils.fileNameSeparator) == extractedDirectoryNoTrailing
						.length() - 1) {
					extractedDirectoryNoTrailing =
							extractedDirectoryNoTrailing.substring(0, extractedDirectoryNoTrailing.length() - 1);
				}

				while (impDirectoryNoTrailing.lastIndexOf(Utils.fileNameSeparator) == impDirectoryNoTrailing.length() - 1) {
					impDirectoryNoTrailing = impDirectoryNoTrailing.substring(0, impDirectoryNoTrailing.length() - 1);
				}
			}

			boolean headlessFoundFile = false;

			if ((imp != null) && extractedDirectoryNoTrailing.equals(impDirectoryNoTrailing)) {
				Utils.log("Image " + newName + " already open from directory " + extractedDirectory, LogLevel.DEBUG);
				theRow[impColumn] = imp;
			} else {
				if (imp != null) {
					Utils.log(extractedDirectoryNoTrailing, LogLevel.DEBUG);
					Utils.log(impDirectoryNoTrailing, LogLevel.DEBUG);
				}
				ImagePlus replacementImp = null;
				if (((MyTableModel) table1.getModel()).openUsingVirtualStacks) {
					replacementImp = new ImagePlus();
					File file =
							new File(extractedDirectory + Utils.fileNameSeparator
									+ FileNameUtils.removeIncrementationMarks(newName));
					Utils.openVirtualTiff(file, replacementImp, displayErrorMessage);
					// Add metadata to make a note of the fact that all of the channels from that TIFF are already
					// stored on disk

					if (!ParseImageMetadata.parseMetadata(replacementImp).hasRootElement()) {
						// No metadata yet in the imp
						Document doc = new Document();
						replacementImp.setProperty("Info", ParseImageMetadata.addChannelNameToXML(doc,
								new String[] { Utils.DEFAULT_CHANNEL_NAME_WHEN_ONLY_1_CHANNEL }));
					}
					String[] channels = ParseImageMetadata.extractChannelNames(replacementImp);
					for (String channel : channels) {
						ParseImageMetadata.updateChannelInfo(replacementImp, channel, "FileBacking", file
								.getAbsolutePath());
						ParseImageMetadata.updateChannelInfo(replacementImp, channel, "LastStorageTime", ""
								+ System.currentTimeMillis());
					}
					replacementImp.setTitle(FileNameUtils.removeIncrementationMarks(newName));
				} else {
					String fileNameWithoutMarks = FileNameUtils.removeIncrementationMarks(newName);
					String newFileName;
					if (new File(fileNameWithoutMarks).exists())
						newFileName = fileNameWithoutMarks;
					else
						newFileName = extractedDirectory + Utils.fileNameSeparator + fileNameWithoutMarks;
					// If file name does not exist, assume that it's found in the same directory as the previous one

					headlessFoundFile = (new File(newFileName)).exists();
				}

				if (!headlessFoundFile) {
					Utils.log("Could not find file " + extractedDirectory
							+ FileNameUtils.removeIncrementationMarks(newName) + "; stopping", LogLevel.INFO);
					parameter.setValue(newName);
					throw new Utils.ImageOpenFailed("Could not find file " + extractedDirectory
							+ FileNameUtils.removeIncrementationMarks(newName));
				}

			}
			parameter.setValue(newName);
			// TODO Deal with saving changes instead of just closing all imps without asking the user
			closeAll(inputsOrOutputs);
		}

		private Thread batchThread;

		/**
		 * Keep incrementing file names and running the pipeline until file names are not found anymore
		 * 
		 * @param blocking
		 *            TODO
		 */
		public int batch(boolean blocking, final boolean singleRun) {
			if ((batchThread != null) && batchThread.isAlive()) {
				Utils.displayMessage("Batch process already running", true, LogLevel.ERROR);
				return PipelinePlugin.ERROR;
			}

			final AtomicInteger returnValue = new AtomicInteger(PipelinePlugin.NO_ERROR);

			batchThread = new Thread("Batch thread") {
				@Override
				public void run() {
					if (!singleRun)
						cleanUpAllPlugins();

					String doneOrInterrupted = "done";
					int nProcessed = 0;

					final Object[][] data = ((MyTableModel) table1.getModel()).data;

					for (Object[] aData : data) {
						if (aData[PLUGIN_INSTANCE] instanceof BatchOpen
								|| aData[PLUGIN_INSTANCE] instanceof BatchOpenV2) {
							if (aData[PLUGIN_INSTANCE] instanceof BatchOpen)
								((BatchOpen) aData[PLUGIN_INSTANCE]).prepareForBatchRun();
							else
								((BatchOpenV2) aData[PLUGIN_INSTANCE]).prepareForBatchRun();
						}
					}

					try {
						do {
							try {
								processStep(0, 0, null, true, null, false);
							} catch (InterruptedException e) {
								Utils.printStack(e, LogLevel.DEBUG);
								doneOrInterrupted = "interrupted by user";
								returnValue.set(PipelinePlugin.THREAD_INTERRUPTED);
								break;
							} catch (Exception e) {
								Utils.log("Exception during batch run; stopping", LogLevel.ERROR);
								Utils.printStack(e);
								doneOrInterrupted = "interrupted by exception";
								returnValue.set(PipelinePlugin.ERROR);
								break;
							} finally {
								Thread.currentThread().setName("Batch thread");
							}
							if (Thread.interrupted()) {
								doneOrInterrupted = "interrupted by user";
								returnValue.set(PipelinePlugin.THREAD_INTERRUPTED);
								break;
							}

							nProcessed++;
							if (!singleRun)
								openNext(nProcessed < 2);
							if (Thread.interrupted()) {
								doneOrInterrupted = "interrupted by user";
								returnValue.set(PipelinePlugin.THREAD_INTERRUPTED);
								break;
							}
							Utils.log("Processed image " + nProcessed, LogLevel.DEBUG);
						} while (!singleRun && returnValue.get() == PipelinePlugin.NO_ERROR);
					} catch (Utils.ImageOpenFailed e) {
						Utils.printStack(e, LogLevel.DEBUG);
						// Nothing to do; we're expecting this to be raised when we've been through
						// all the images that were present in the working directory
					}
					if (!Utils.headless && !singleRun)
						Utils.displayMessage("Batch run " + doneOrInterrupted + "; ran " + nProcessed + " iterations",
								true, LogLevel.INFO);
					if (returnValue.get() == PipelinePlugin.NO_ERROR)
						reset();
				}
			};
			batchThread.start();
			if (blocking)
				try {
					batchThread.join();
				} catch (InterruptedException e) {
					batchThread.interrupt();
					throw new RuntimeException(e);
				}
			return returnValue.get();
		}

		/**
		 * Make sure all threads get terminated.
		 */
		private void cleanUpAllPlugins() {
			final Object[][] data = ((MyTableModel) table1.getModel()).data;
			for (int row = 0; row < data.length; row++) {
				try {
					cleanupPluginAtRow(row);
				} catch (Exception e) {
					Utils.printStack(e, LogLevel.DEBUG);
				}
			}
		}

		@SuppressWarnings("unchecked")
		private void cleanupPluginAtRow(final int row) throws Utils.ImageOpenFailed {
			final Object[][] data = ((MyTableModel) table1.getModel()).data;
			PipelinePlugin plugin = (PipelinePlugin) data[row][PLUGIN_INSTANCE];
			Utils.ImageOpenFailed e0 = null;
			if (plugin instanceof ResettablePlugin) {
				try {
					((ResettablePlugin) data[row][PLUGIN_INSTANCE]).reset();
				} catch (Utils.ImageOpenFailed e) {
					// Do not throw exception straight away because we want to make sure the plugin does get cleaned up
					e0 = e;
				} catch (Exception e) {
					Utils.printStack(e);
				}
			}
			// Not sure if it makes sense to have both cleanup and reset but ExternalCall does use this system to free
			// up memory
			if (plugin != null) {
				try {
					plugin.cleanup();
				} catch (Exception e) {
					Utils.printStack(e);
				}
			}

			if (data[row][LINK_TO_EXTERNAL_PROGRAM] != null) {
				Utils.log("Terminating link to external program for plugin " + plugin, LogLevel.VERBOSE_DEBUG);

				/*
				 * try { ((LinkToExternalProgram) data[row][LINK_TO_EXTERNAL_PROGRAM]).terminate(false); } catch
				 * (Exception e){ // We do not really care what happens Utils.printStack(e, LogLevel.DEBUG); }
				 */

				data[row][LINK_TO_EXTERNAL_PROGRAM] = null;
			}

			if (data[row][PERCENT_DONE] != null)
				progressSetValueThreadSafe((ProgressRenderer) data[row][PERCENT_DONE], 0);

			List<PluginIOView> auxImps = (List<PluginIOView>) data[row][AUXILIARY_OUTPUT_IMPS];
			// XXX Should the plugin be responsible for this cleanup?
			if (auxImps != null) {
				List<PluginIOView> auxImpsCopy = new ArrayList<>(auxImps);
				for (PluginIOView auxImp : auxImpsCopy) {
					try {
						if (auxImp != null)
							auxImp.close();
					} catch (Exception e) {
						Utils.printStack(e);
					}
				}
				auxImps.clear();
			}

			if (e0 != null)
				throw e0;

			try {
				if (plugin != null) {
					plugin.clearInputs();
					plugin.clearOutputs();
				}
			} catch (Exception e) {
				Utils.printStack(e);
			}

			data[row][COMPUTED_INPUTS] = null;
			data[row][COMPUTED_OUTPUTS] = null;
		}

		/**
		 * Closes all currently-open windows, resets the state of all plugins in the table that implement
		 * ResettablePlugin, and increments the file paths of any parameter that implements FileNameIncrementable.
		 * Currently, incrementable file paths are those containing curly braces { }
		 * 
		 * @param displayErrorMessage
		 *            If true and image opening fails (for example if the file cannot be found or is in an unsupported
		 *            format), a dialog is displayed to the suer
		 */
		private void openNext(boolean displayErrorMessage) throws Utils.ImageOpenFailed {
			// TODO Grab local directory for each step, then close everything
			// Go through all filenames that contain { } and increment the number that's contained in there

			Object[][] data = ((MyTableModel) table1.getModel()).data;

			ImageOpenFailed exception = null;

			for (int row = 0; row < data.length; row++) {

				for (int col = 0; col < data[row].length; col++) {
					if ((col == INPUT_NAME_FIELD) || (col == OUTPUT_NAME_FIELD))
						continue;
					if (data[row][col] instanceof FileNameIncrementable) {
						try {
							((FileNameIncrementable) data[row][col]).incrementFileName();
						} catch (Exception e) {
							Utils.printStack(e);
						}
					}
				}

				// The following line will open the next image, if it is not a relative or absolute reference
				// to the output of another plugin
				try {
					checkForBracesAndUpdate(row, INPUT_NAME_FIELD, PLUGIN_INPUTS, getRowInputs(row),
							displayErrorMessage);
				} catch (ImageOpenFailed e) {
					exception = e;
				} catch (Exception e) {
					Utils.printStack(e);
				}

				try {
					checkForBracesAndUpdate(row, OUTPUT_NAME_FIELD, PLUGIN_OUTPUTS, getRowOutputs(row),
							displayErrorMessage);
				} catch (ImageOpenFailed e) {
					// This should not happen for plugin outputs
					throw new IllegalStateException(e);
				} catch (Exception e) {
					Utils.printStack(e);
				}

				try {
					cleanupPluginAtRow(row);// Need to clean up after opening next images, because we will read
					// the pluginInputs of the plugin
				} catch (ImageOpenFailed e) {
					exception = e;
				} catch (Exception e) {
					Utils.printStack(e);
				}

			}
			System.gc();

			((MyTableModel) table1.getModel()).fireTableDataChanged();

			if (exception != null)
				throw exception;
		}

		private void reset() {
			Object[][] data = ((MyTableModel) table1.getModel()).data;
			for (int row = 0; row < data.length; row++) {
				try {
					Object[] rowObj = data[row];
					if (rowObj[PLUGIN_INSTANCE] instanceof BatchOpen || rowObj[PLUGIN_INSTANCE] instanceof BatchOpenV2) {
						if (rowObj[PLUGIN_INSTANCE] instanceof BatchOpen)
							((BatchOpen) rowObj[PLUGIN_INSTANCE]).prepareForBatchRun();
						else
							((BatchOpenV2) rowObj[PLUGIN_INSTANCE]).prepareForBatchRun();
					}
					cleanupPluginAtRow(row);
				} catch (Exception e) {
					Utils.printStack(e);
				}
			}
			System.gc();
			((MyTableModel) table1.getModel()).fireTableDataChanged();
		}

		public class TableNotComputed extends Exception {
			private static final long serialVersionUID = 1L;
			private int lastRow;

			private TableNotComputed(int lastRow) {
				super();
				this.lastRow = lastRow;
			}
		}

		/**
		 * @param lastRow
		 *            last row to include in the string; if -1, include all the table. Table must have been computed up
		 *            to last row for the call to be successful.
		 * @return The current pipeline table as an XML string.
		 */
		private String getTableAsString(int lastRow) throws TableNotComputed {
			Object[][] data = ((MyTableModel) table1.getModel()).data;
			if (lastRow == -1)
				lastRow = data.length - 1;
			if (lastRow < 0) {
				throw new TableNotComputed(lastRow);
			}
			String output = "";
			Object[] theRow = data[lastRow];
			if (theRow[OUTPUT_XML] != null) {
				StringWriter sw = new StringWriter();
				XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
				try {
					Document d = (Document) theRow[OUTPUT_XML];
					Element workingDirectoryElement = null;
					final String wdElementName = "XXXWorkingDirectory";
					@SuppressWarnings("unchecked")
					List<Element> l = d.getRootElement().getChildren(wdElementName);
					if (l.size() > 1)
						throw new IllegalStateException("More than 1 working directory specified");
					else if (l.size() > 0)
						workingDirectoryElement = l.get(0);
					else {
						workingDirectoryElement = new Element(wdElementName);
						d.getRootElement().addContent(workingDirectoryElement);
					}
					workingDirectoryElement.setText(FileNameUtils.compactPath(System.getProperty("user.dir") + "/"));
					outputter.output(d, sw);
					output = sw.toString();
				} catch (Exception e) {
					Utils.log("Problem printing XML to image metadata in setPipelineProcessingMetadata", LogLevel.ERROR);
					throw new RuntimeException(e);
				}
			} else {
				throw new TableNotComputed(lastRow);
			}
			if (output.equals("")) {
				throw new RuntimeException("Could no retrieve table contents from row " + lastRow);
			}
			return output;
		}

		/**
		 * Prompt the user for a file to save the current pipeline to, as XML.
		 */
		private void saveTable() {
			Object[][] data = ((MyTableModel) table1.getModel()).data;
			int last_row = data.length - 1;
			String output;
			try {
				output = getTableAsString(last_row);
			} catch (TableNotComputed e1) {
				Utils.displayMessage("Output not computed for row " + (e1.lastRow + 1) + "; run before saving table.", true,
						LogLevel.ERROR);
				return;
			} catch (Exception e) {
				Utils.printStack(e);
				Utils.displayMessage("Error while saving table", true, LogLevel.ERROR);
				return;
			}
			FileDialog dialog = new FileDialog(new Frame(), "Save table to", FileDialog.SAVE);

			dialog.setVisible(true);
			String saveTo = dialog.getDirectory();
			if (saveTo == null)
				return;

			saveTo += Utils.fileNameSeparator + dialog.getFile();

			if (!dialog.getFile().contains(".xml"))
				saveTo += ".xml";

			try (PrintWriter out = new PrintWriter(saveTo)) {
				out.println(output);
			} catch (FileNotFoundException e) {
				Utils.printStack(e);
				Utils.displayMessage("Could not save table", true, LogLevel.ERROR);
			}
		}

		/**
		 * Initiate running of the pipeline
		 */
		private void runPipeline() {
			new RunPipelineTask(0, null, false).start();
		}

		/**
		 * Prompt the user for an XML file from which a pipeline is to be loaded.
		 * 
		 * @param modifier
		 *            If non-0, use simple input dialog rather than file browser
		 */
		private void loadTable(int modifier) {
			String fileToReadFrom = null;

			if (modifier == 0) {
				File f = FileNameUtils.chooseFile("Choose table...", FileDialog.LOAD);
				if (f == null)
					return;
				fileToReadFrom = f.getAbsolutePath();
			} else {
				final JOptionPane optionPane = new JOptionPane("Enter full path", JOptionPane.QUESTION_MESSAGE);
				optionPane.addHierarchyListener(e -> {
					Window window = SwingUtilities.getWindowAncestor(optionPane);
					if (window instanceof Dialog) {
						Dialog dialog = (Dialog) window;
						if (!dialog.isResizable()) {
							dialog.setResizable(true);
						}
					}
				});
				optionPane.setPreferredSize(new Dimension(500, 100));

				fileToReadFrom =
						JOptionPane.showInputDialog(optionPane, "Enter full path", "Enter full path",
								JOptionPane.QUESTION_MESSAGE);
				if (fileToReadFrom == null)
					return;
				fileToReadFrom = FileNameUtils.expandPath(fileToReadFrom);
			}
			if (!fileToReadFrom.endsWith(".xml")) {
				Utils.displayMessage(
						"File "
								+ FileNameUtils.compactPath(fileToReadFrom)
								+ " does not have an .xml extension and is thus likely not a pipeline table; please choose another file.",
						true, LogLevel.ERROR);
				return;
			}
			try {
				workingDirectory = null;
				loadTable(fileToReadFrom);
				if (workingDirectory != null) {
					Object[][] data = ((MyTableModel) table1.getModel()).data;

					for (Object[] aData : data) {
						for (int col = 0; col < aData.length; col++) {
							if ((col == INPUT_NAME_FIELD) || (col == OUTPUT_NAME_FIELD)) {
								RowOrFileTextReference r = (RowOrFileTextReference) aData[col];
								String s = r.getStringValue();
								if (s == null)
									continue;
								// Only prefix names that contain a "/" character, to avoid
								// adding prefix to local image names or integer references
								if (s.contains("/"))
									r.setValue(workingDirectory + r.getValue());
								continue;
							}
							if (aData[col] instanceof FileNameIncrementable) {
								try {
									((FileNameIncrementable) aData[col]).prefixFileName(workingDirectory);
								} catch (Exception e) {
									Utils.printStack(e);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				Utils.displayMessage("Could not read table from file " + fileToReadFrom + ": " + e.getMessage(), true,
						LogLevel.ERROR);
			}
		}

		/**
		 * Uncheck all of the "Show" checkboxes
		 */
		private void uncheckTableExceptLast() {
			MyTableModel model = (MyTableModel) table1.getModel();
			int numberOfRows = model.getRowCount();
			for (int currentRow = 0; currentRow < numberOfRows - 1; currentRow++)
				model.setValueAt(false, currentRow, SHOW_IMAGE);
		}

		/**
		 * Reload pipeline from specified XML file.
		 * 
		 * @throws IOException
		 */
		private void loadTable(String fileToReadFrom) throws IOException {
			File f = new File(fileToReadFrom);
			StringBuffer contents = new StringBuffer();

			try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
				String text = null;
				while ((text = reader.readLine()) != null) {
					contents.append(text).append(System.getProperty("line.separator"));
				}
			}

			// Replacement is to ensure backwards compatibility with tables that had unfortunate
			// references to anonymous classes
			String s = contents.toString().replaceAll(pattern3, pattern4);
			Document doc;
			try {
				SAXBuilder builder = new SAXBuilder();
				doc = builder.build(new InputSource(new StringReader(s)));
			} catch (Exception e) {
				Utils.printStack(e);
				Utils.log("Failed to parse " + Utils.chopOffString(s, 20000), LogLevel.ERROR);
				throw new PluginRuntimeException("Failed to parse table from file " + fileToReadFrom, e, true);
			}
			Object[][] newData = {};
			((MyTableModel) table1.getModel()).data = newData;

			reuseSettings(doc);
		}

		/**
		 * Load pipeline metadata from the frontmost ImagePlus window.
		 */
		private void reuseFrontmostImpSettings() {
			ImageWindow frontMostWindow = WindowManager.getCurrentWindow();
			if (frontMostWindow == null) {
				Utils.displayMessage("Please open an image first", true, LogLevel.ERROR);
				return;
			}
			ImagePlus imp = frontMostWindow.getImagePlus();

			Object[][] newData = {};
			((MyTableModel) table1.getModel()).data = newData;

			Document doc = ParseImageMetadata.parseMetadata(imp);
			reuseSettings(doc);
		}

		/**
		 * If Object o is an instance of {@link DirectoryParameter}, set it to point to the directory in s.
		 * 
		 * @param s
		 * @param o
		 */
		private void setDirectory(String s, Object o) {
			if (o instanceof SplitParameter) {
				Object[] subParams = (Object[]) ((SplitParameter) o).getValue();
				if (subParams == null)
					return;
				for (Object subParam : subParams) {
					if (subParam != null)
						setDirectory(s, subParam);
				}
			} else if (o instanceof DirectoryParameter)
				((DirectoryParameter) o).setValue(s);
		}

		/**
		 * Prompt the user for a working directory to use to store files generated by all plugins. This iterates through
		 * the table and calls {@link #setDirectory(String, Object)} on every non-null parameter.
		 */
		private void setAllDirectories() {
			Object[][] data = ((MyTableModel) table1.getModel()).data;

			System.setProperty("apple.awt.fileDialogForDirectories", "true");
			FileDialog dialog = new FileDialog(new Frame(), "Choose a directory to save plot to", FileDialog.LOAD);
			dialog.setVisible(true);
			String path = dialog.getDirectory() + Utils.fileNameSeparator + dialog.getFile();
			System.setProperty("apple.awt.fileDialogForDirectories", "false");

			for (Object[] theRow : data) {
				if (theRow[PLUGIN_PARAM_1_FIELD] != null)
					setDirectory(path, theRow[PLUGIN_PARAM_1_FIELD]);
				if (theRow[PLUGIN_PARAM_2_FIELD] != null)
					setDirectory(path, theRow[PLUGIN_PARAM_2_FIELD]);
			}
			((MyTableModel) table1.getModel()).fireTableDataChanged();
		}

		@SuppressWarnings("unused")
		private boolean registerListener(AbstractParameter param, ParameterListener listener, String paramName) {

			if (paramName.equals(param.getUserDisplayName())) {
				param.addPluginListener(listener);
				return true;
			}
			if (param instanceof SplitParameter) {
				for (AbstractParameter p : ((SplitParameter) param).getParameterValue()) {
					if (registerListener(p, listener, paramName))
						return true;
				}
			}
			return false;
		}

		@SuppressWarnings("unused")
		private void flattenListeners(List<ParameterListener> list, List<ParameterListener> result) {
			for (ParameterListener listener : list) {
				if (listener == null)
					continue;
				if (listener instanceof SplitParameterListener) {
					List<ParameterListener> splitList = new ArrayList<>();
					for (ParameterListener l2 : ((SplitParameterListener) listener).parameterListeners) {
						splitList.add(l2);
					}
					flattenListeners(splitList, result);
				} else {
					result.add(listener);
				}
			}
		}

		@SuppressWarnings("unchecked")
		/**
		 * Reload a pipeline described in the XML document
		 * @param doc
		 */
		private void reuseSettings(Document doc) {

			if (!doc.hasRootElement()) {
				throw new PluginRuntimeException("No metadata contained in image to reuse 1", true);
			}

			Element root = doc.getRootElement();

			Element workingDirectoryElement = root.getChild("WorkingDirectory");
			if (workingDirectoryElement != null) {
				workingDirectory = workingDirectoryElement.getText();
				Utils.log("Working directory: " + workingDirectory, LogLevel.DEBUG);
			}

			Element pSteps = root.getChild("ProcessingSteps");
			if (pSteps == null) {
				throw new PluginRuntimeException("No metadata contained in image to reuse 2", true);
			}

			int childIndex = 0;
			Element step = pSteps.getChild("Step" + childIndex);
			XStream xstream = new XStream(reflectionProvider);
			// Aliases to maintain backward compatibility with XML files generated before name changes
			xstream.alias("pipeline.parameters.intRangeParameter", IntRangeParameter.class);
			xstream.alias("pipeline.parameters.intParameter", IntParameter.class);
			xstream.alias("pipeline.parameters.floatParameter", FloatParameter.class);
			xstream.alias("pipeline.parameters.floatRangeParameter", FloatRangeParameter.class);
			xstream.alias("pipeline.parameters.comboBoxParameter", ComboBoxParameter.class);
			xstream.alias("pipeline.parameters.SplitParameter", SplitParameter.class);
			xstream.aliasField("the_float", FloatParameter.class, "floatValue");
			xstream.aliasField("the_int", IntParameter.class, "intValue");
			xstream.aliasField("value", IntParameter.class, "intValue");
			xstream.aliasField("the_int_low", FloatRangeParameter.class, "lowValue");
			xstream.aliasField("the_int_high", FloatRangeParameter.class, "highValue");
			xstream.aliasField("the_int_low", IntRangeParameter.class, "rangeLowerBound");
			xstream.aliasField("the_int_high", IntRangeParameter.class, "rangeUpperBound");
			xstream.aliasField("name", AbstractParameter.class, "userDisplayName");
			xstream.aliasField("currentChoices", MultiListParameter.class, "currentSelection");

			while (step == null) {// First step in table was not index 0
				childIndex++;
				step = pSteps.getChild("Step" + childIndex);
			}

			String obsoletePluginMessage = "";
			while (step != null) {
				try {
					Object[][] tableData = ((MyTableModel) table1.getModel()).data;
					((MyTableModel) table1.getModel()).newRow(tableData.length - 1, false);
					tableData = ((MyTableModel) table1.getModel()).data;
					int newRowPosition = tableData.length - 1;

					Object[] newRow = new Object[TABLE_WIDTH];

					// Iterate over all the children of step to fill in as many columns as possible of the new row

					List<Element> l = step.getChildren("Column");

					newRow[LAST_TIME_RUN] = (long) 0;// For retrocompatibility with tables that do not include this
					// element

					for (Object next : l) {
						if (next instanceof Element) {
							Element e = (Element) next;
							int index = Utils.parseAndThrowRuntimeException(e.getAttributeValue("Index"));
							if (index >= newRow.length) {
								Utils.log("Index larger than expected while reloading setting; ignoring this object ",
										LogLevel.ERROR);
								Utils.log(e.getText(), LogLevel.ERROR);
								continue;
							}
							try {
								newRow[index] = xstream.fromXML(e.getText());
							} catch (Exception exc) {
								throw new PluginRuntimeException("Problem parsing step " + childIndex
										+ " column index " + index, exc, true);
							}
						}
					}

					if (newRow[AUXILIARY_INPUTS] != null) {
						TwoColumnTableParameter auxInputs = (TwoColumnTableParameter) newRow[AUXILIARY_INPUTS];
						Object[] rowReferences = auxInputs.getSecondColumn();
						for (int i = 0; i < rowReferences.length ; i++) {
							if (rowReferences[i] == null) {
								rowReferences[i] = "";
							}
						}
					}
					
					((AtomicBoolean) newRow[IS_UPDATING]).set(false);
					((AtomicBoolean) newRow[UPDATE_QUEUED]).set(false);
					if (newRow[OUTPUT_LOCKS] == null)
						newRow[OUTPUT_LOCKS] = new AtomicInteger(0);
					else
						((AtomicInteger) newRow[OUTPUT_LOCKS]).set(0);

					int pluginID = Utils.indexOf(pluginNames, (String) newRow[PLUGIN_NAME_FIELD]);

					if (pluginID == -1) {
						pluginID =
								Utils.indexOf(pluginNames, ((String) newRow[PLUGIN_NAME_FIELD]).replaceFirst("QQQ", ""));
					}
					if (pluginID == -1) {
						throw new PluginRuntimeException("Cannot find plugin " + newRow[PLUGIN_NAME_FIELD]
								+ " while restoring settings", true);
					}

					Class<PipelinePlugin> pluginClass = (Class<PipelinePlugin>) (pluginObjects[pluginID]).getClass();
					PluginInfo pluginInfo = pluginClass.getAnnotation(PluginInfo.class);
					if (pluginInfo != null) {
						if (pluginInfo.obsolete()) {
							obsoletePluginMessage += "Plugin " + pluginClass.getName() + " is obsolete";
							if (!"".equals(pluginInfo.suggestedReplacement())) {
								obsoletePluginMessage +=
										"; please consider switching to " + pluginInfo.suggestedReplacement() + "\n";
							} else
								obsoletePluginMessage += "\n";
						}
					}

					PipelinePlugin pluginInstance = pluginClass.newInstance();
					pluginInstance.setpipeLineListener(new PluginCallBack());
					pluginInstance.setRow(newRowPosition);

					IPluginShell sh = null;
					if (pluginInstance instanceof ThreeDPlugin || pluginInstance instanceof TwoDPlugin) {
						// Need to use a plugin wrapper (unless the source is not a stack, but we don't necessary know
						// ahead of time)
						sh = (IPluginShell) ((PipelinePlugin) pluginObjects[shell2Dindex]).getClass().newInstance();
						((PipelinePlugin) sh).setpipeLineListener(new PluginCallBack());
						((PipelinePlugin) sh).setRow(newRowPosition);
						sh.setPlugin(pluginInstance);
					}
					newRow[PLUGIN_INSTANCE] = (sh == null) ? pluginInstance : sh;
					PipelinePlugin plugin = ((PipelinePlugin) newRow[PLUGIN_INSTANCE]);
					newRow[PERCENT_DONE] = new ProgressRenderer(table1, plugin);

					newRow[COLUMN_NUMBER] = newRowPosition;
					tableData[newRowPosition] = newRow;
					plugin.setRow(newRowPosition);

					AbstractParameter[] paramArray = new AbstractParameter[2];
					paramArray[0] = (AbstractParameter) newRow[PLUGIN_PARAM_1_FIELD];
					paramArray[1] = (AbstractParameter) newRow[PLUGIN_PARAM_2_FIELD];

					try {
						plugin.setParameters(paramArray);
					} catch (Exception e) {
						if (Utils.headless) { // If running in headless mode, stop if problem restoring parameters
							throw e;
						} else {
							Utils.displayMessage("Problem restoring parameters", true, LogLevel.ERROR);
							Utils.printStack(e);
						}
					}
					newRow[PLUGIN_PARAM_1_FIELD] = plugin.getParameters()[0];
					newRow[PLUGIN_PARAM_2_FIELD] = plugin.getParameters()[1];
				} catch (Exception e) {
					if (Utils.headless) { // If running in headless mode, stop if problem restoring table
						throw new RuntimeException(e);
					} else {
						Utils.displayMessage("Error while restoring step " + childIndex, true, LogLevel.ERROR);
						Utils.printStack(e);
					}
				} finally {
					childIndex++;
					step = pSteps.getChild("Step" + childIndex);
					Utils.log("read step " + childIndex, LogLevel.DEBUG);
				}
			}
			if (!Utils.headless && !"".equals(obsoletePluginMessage)) {
				Utils.displayMessage(obsoletePluginMessage, true, LogLevel.WARNING);
			}
			((MyTableModel) table1.getModel()).fireTableDataChanged();
		}

		private JCheckBox addCheckBox(String text) {
			JCheckBox checkBox = new JCheckBox(text);
			checkBox.addActionListener(this);
			add(checkBox);
			return checkBox;
		}

		private class RunPipelineTask implements Runnable {
			private int row;
			private AbstractParameter changedParameter;
			private boolean stayInCoreLoop;

			public RunPipelineTask(int row1, AbstractParameter changedParameter, boolean stayInCoreLoop) {
				super();// "Update triggered at row "+row1
				this.row = row1;
				this.changedParameter = changedParameter;
				this.stayInCoreLoop = stayInCoreLoop;
			}

			@Override
			public void run() {
				try {
					processStep(row, row, null, true, changedParameter, stayInCoreLoop);
				} catch (Exception e) {
					Utils.printStack(e, LogLevel.DEBUG);
				} finally {
					Thread.currentThread().setName("");
				}
			}

			public void start() {
				BasePipelinePlugin.threadPool.submit(this);
			}
		}

		private void updateSelectedRows() {
			int[] rowsToUpdate = table1.getSelectedRows();
			Arrays.sort(rowsToUpdate);
			Utils.log("Rows to update: " + Utils.printIntArray(rowsToUpdate), LogLevel.DEBUG);
			for (int i = 0; i < rowsToUpdate.length; i++) {
				Utils.log("updating row " + i + ", which is " + rowsToUpdate[i], LogLevel.DEBUG);
				new RunPipelineTask(rowsToUpdate[i], null, false).start();
			}

		}

		private void scrollTableToRow(final int insertionRow) {
			table1.updateUI();
			SwingUtilities.invokeLater(() -> {
				Rectangle rect =
						table1.getCellRect(insertionRow < table1.getRowCount() - 1 ? insertionRow + 1 : insertionRow,
								1, true);
				table.scrollRectToVisible(rect);

				JViewport viewport = (JViewport) table1.getParent();
				Point pt = viewport.getViewPosition();
				rect.setLocation(rect.x - pt.x, rect.y - pt.y);
				viewport.scrollRectToVisible(rect);
			});
		}

		@Override
		@SuppressWarnings({ "unchecked" })
		public void actionPerformed(ActionEvent event) {
			String command = event.getActionCommand();
			switch (command) {
				case "Run":
					runPipeline();
					break;
				case "Delete":
					int[] rowsToDelete = table1.getSelectedRows();
					Arrays.sort(rowsToDelete);
					for (int i = rowsToDelete.length - 1; i >= 0; i--) {
						Utils.log("deleting row " + i, LogLevel.DEBUG);
						((MyTableModel) table1.getModel()).deleteRow(rowsToDelete[i]);
					}
					// updateImageButton.setEnabled(false);
					// deleteButton.setEnabled(false);
					break;
				case "New":
					int[] selectedRows = table1.getSelectedRows();
					int insertAt;
					if (selectedRows.length == 0) {
						insertAt = ((MyTableModel) table1.getModel()).data.length - 1;
					} else {
						insertAt = selectedRows[0];
					}
					((MyTableModel) table1.getModel()).newRow(insertAt, true);
					scrollTableToRow(insertAt);
					break;
				case "Update step":
					updateSelectedRows();
					break;
				case "Update pipeline upon param change":
					((MyTableModel) table1.getModel()).updatePipeline = updatePipelineButton.isSelected();
					break;
				case "Use virtual stacks to open files":
					((MyTableModel) table1.getModel()).openUsingVirtualStacks = updatePipelineButton.isSelected();
					break;
				case "Update current step upon param change":
					((MyTableModel) table1.getModel()).updateCurrentStep = updateCurrentStepButton.isSelected();
					break;
				case "Restart local update upon param change":
					((MyTableModel) table1.getModel()).cancelUponChange = cancelUponChangeButton.isSelected();
					break;
				case "Restart pipeline upon param change":
					((MyTableModel) table1.getModel()).globalCancelUponChange =
							globalCancelUponChangeButton.isSelected();
					break;
				case "Suppress log":
					Utils.suppressLog = suppressLog.isSelected();
					break;
				case "Suppress warning popups":
					Utils.suppressWarningPopups = suppressWarningPopups.isSelected();
					break;
				case "Stop all updates":
					Runnable cancelTask = () -> {
						Thread t;
						Object[][] data = ((MyTableModel) table1.getModel()).data;
						for (Object[] element : data) {
							if (element[LINK_TO_EXTERNAL_PROGRAM] != null) {
								// Probably redundant since t.interrupt() below should have same effect
							((LinkToExternalProgram) element[LINK_TO_EXTERNAL_PROGRAM]).interrupt();
						}

						t = (Thread) element[WORKER_THREAD];
						if (t != null) {
							t.interrupt();
						}
						t = (Thread) element[QUEUED_WORKER_THREAD];
						if (t != null) {
							t.interrupt();
						}
					}

					for (Object[] element : data) {
						if (element[LINK_TO_EXTERNAL_PROGRAM] != null) {
							((LinkToExternalProgram) element[LINK_TO_EXTERNAL_PROGRAM]).terminateForcibly();
						}
						((AtomicBoolean) element[IS_UPDATING]).set(false);
						((AtomicBoolean) element[UPDATE_QUEUED]).set(false);
						((AtomicInteger) element[OUTPUT_LOCKS]).set(0);
					}
				}	;
					BasePipelinePlugin.threadPool.submit(cancelTask);
					break;
				case "Reuse settings":
					reuseFrontmostImpSettings();
					break;
				case "Reload plugins":
					loadPlugins();
					break;
				case "Set all directories":
					setAllDirectories();
					break;
				case "Collect garbage":
					Utils.log("Calling garbage callector", LogLevel.DEBUG);
					// Runtime.getRuntime().freeMemory();
					SwingMemory.swingGC(null);
					Runtime.getRuntime().gc();
					break;
				case "Keep table on top":
					tableFrame.setAlwaysOnTop(keepWindowOnTopButton.isSelected());
					break;
				case "Bring outputs to front": {
					int[] rowsToBringToFront = table1.getSelectedRows();
					for (int i = rowsToBringToFront.length - 1; i >= 0; i--) {
						PipelinePlugin pluginInstance =
								(PipelinePlugin) ((MyTableModel) table1.getModel()).data[rowsToBringToFront[i]][PLUGIN_INSTANCE];
						List<PluginIOView> auxImps =
								(List<PluginIOView>) ((MyTableModel) table1.getModel()).data[rowsToBringToFront[i]][AUXILIARY_OUTPUT_IMPS];
						boolean foundSomethingToShow = false;
						if (pluginInstance != null) {
							for (IPluginIO input : pluginInstance.getOutputs().values()) {
								if (input instanceof IPluginIOImage) {
									PluginIOHyperstackViewWithImagePlus imp = ((IPluginIOImage) input).getImp();
									if ((imp != null) && (imp.imp != null)) {
										if (imp.imp.getWindow() != null)
											imp.imp.getWindow().toFront();
										foundSomethingToShow = true;
									}
								}
							}
						}
						if (auxImps != null) {
							for (PluginIOView view : auxImps) {
								if (view instanceof PluginIOHyperstackViewWithImagePlus) {
									PluginIOHyperstackViewWithImagePlus impMd =
											(PluginIOHyperstackViewWithImagePlus) view;
									if ((impMd.imp != null) && (impMd.imp.getWindow() != null)) {
										impMd.imp.getWindow().toFront();
										foundSomethingToShow = true;
									}
								} else {
									if (view != null) {
										view.show();
										foundSomethingToShow = true;
									}
								}
							}
						}

						if (!foundSomethingToShow)
							Utils.displayMessage("Output of row " + (rowsToBringToFront[i] + 1) + " not computed yet.",
									true, LogLevel.ERROR);
					}
					break;
				}
				case "Make composite": {
					int[] rowsToBringToFront = table1.getSelectedRows();
					for (int i = rowsToBringToFront.length - 1; i >= 0; i--) {
						PipelinePlugin pluginInstance =
								(PipelinePlugin) ((MyTableModel) table1.getModel()).data[rowsToBringToFront[i]][PLUGIN_INSTANCE];

						if (pluginInstance != null) {
							PluginIOHyperstackViewWithImagePlus destination = findAnImp(pluginInstance.getOutputs());
							if ((destination != null) && (destination.imp != null)) {
								if (destination.imp.getWindow() == null)
									Utils.log("Output has no window yet", LogLevel.ERROR);
								else {
									destination.toComposite();
								}
							}
						}
					}
					break;
				}
				case "Revert to channels": {
					int[] rowsToBringToFront = table1.getSelectedRows();
					for (int i = rowsToBringToFront.length - 1; i >= 0; i--) {
						PipelinePlugin pluginInstance =
								(PipelinePlugin) ((MyTableModel) table1.getModel()).data[rowsToBringToFront[i]][PLUGIN_INSTANCE];

						if (pluginInstance != null) {
							PluginIOHyperstackViewWithImagePlus destination = findAnImp(pluginInstance.getOutputs());
							if ((destination != null) && (destination.imp != null)) {
								if (destination.imp.getWindow() == null)
									Utils.log("Output has no window yet", LogLevel.ERROR);
								else {
									if (!(destination.imp instanceof CompositeImage)) {
										Utils.log("Window is not a composite image", LogLevel.ERROR);
									} else {
										destination.toSeparateChannelMode();
									}
								}
							}
						}

					}
					break;
				}
				case "Open next":
					try {
						openNext(false);
					} catch (ImageOpenFailed e) {
						Utils.displayMessage(e.getMessage(), true, LogLevel.ERROR);
					}
					break;
				case "Reset":
					reset();
					break;
				case "Open next and run":
					try {
						openNext(false);
						runPipeline();
					} catch (ImageOpenFailed e) {
						Utils.displayMessage(e.getMessage(), true, LogLevel.ERROR);
					}
					break;
				case "Batch run":
					batch(false, false);
					break;
				case "Save table":
					saveTable();
					break;
				case "Load table":
					loadTable(event.getModifiers() & ActionEvent.SHIFT_MASK);
					break;
				case "VNC settings":
					// nothing to do
					break;
				default:
					throw new IllegalStateException("Unrecognized command " + command);
			}
		}

		private JFrame tableFrame;

		public class MyTableModel extends AbstractTableModel {

			@SuppressWarnings("null")
			private boolean setFileParameter(int row, int column, File f) {
				if ((column == PLUGIN_PARAM_1_FIELD) || (column == PLUGIN_PARAM_2_FIELD)) {
					Object param = table1.getModel().getValueAt(row, column);
					if (param instanceof SplitParameter) {
						AbstractParameter[] parameters = ((SplitParameter) param).getParameterValue();
						if ((parameters[0] instanceof FileNameParameter)
								&& (parameters[1] instanceof DirectoryParameter)) {
							if (f.isDirectory()) {
								parameters[1].setValue(FileNameUtils.compactPath(f.getAbsolutePath()));
								FileNameParameter fileName = (FileNameParameter) parameters[0];
								if ("".equals(fileName.getValue())) {
									fileName.setValue("Untitled_" + UUID.randomUUID().toString());
								}
							} else {
								parameters[0].setValue(f.getName());
								parameters[1].setValue(FileNameUtils.compactPath(f.getParent()));
							}
							for (AbstractParameter p : parameters)
								p.fireValueChanged(false, true, true);
							((MyTableModel) table1.getModel()).fireTableCellUpdated(row, column);
							return true;
						}
					}
				}
				return false;
			}

			private boolean setDirectoryParameter(int row, int column, String dirPath) {
				if ((column == PLUGIN_PARAM_1_FIELD) || (column == PLUGIN_PARAM_2_FIELD)) {
					Object param = table1.getModel().getValueAt(row, column);
					if (param instanceof SplitParameter) {
						AbstractParameter[] parameters = ((SplitParameter) param).getParameterValue();
						for (AbstractParameter p : parameters) {
							if (p instanceof DirectoryParameter) {
								p.setValue(dirPath);
								p.fireValueChanged(false, true, true);
								((MyTableModel) table1.getModel()).fireTableCellUpdated(row, column);
								return true;
							}
						}
					}
				}
				Utils.log("Could not find directory parameter", LogLevel.DEBUG);
				return false;
			}

			private static final long serialVersionUID = 1L;

			private String[] columnNames = { "#", "Input", "Channels", "Show",
					"C+",// Keep C program alive
					"On", "Autorange", "Filter", "Parameter 1", "Parameter 2", "Output", "Output channels", "Updating",
					"Update queued", "%done", "Aux input", "Aux output", null, // WORKER_THREAD; don't attempt to
					// display it to have an empty column
					// name
					null, // QUEUED_WORKER_THREAD; don't attempt to display it to have an empty column name
					null, // plugin instance; don't attempt to display it to have an empty column name
					null // z projection; don't attempt to display it to have an empty column name
					};

			private MultiListParameter channelsParameter = new MultiListParameter("channels",
					"Choose while channels to process", new @NonNull String @NonNull[] { "Ch0", "Ch1" },
					new int[] { 1, 2 }, null);
			private TableParameter outputChannelParameter = new TableParameter("Output channels",
					"Choose output channel names", new String[] { "Ch0", "Ch1" }, null);

			public Object[][] data =
					{ {
							0,
							new RowOrFileTextReference(
									"",
									"Image that this step uses as an input. Name of an open image, absolute reference to another row starting with a dollar (e.g. $2), or relative reference (e.g. -1 for the previous row)",
									"", true, null),
							channelsParameter,
							Boolean.TRUE,
							Boolean.TRUE,
							Boolean.TRUE,
							Boolean.TRUE,
							"Click for plugin",
							"",
							"",
							new RowOrFileTextReference(
									"",
									"Image that this step uses as an output. Name of an open image, absolute reference to another row starting with a dollar (e.g. $2), or relative reference (e.g. -1 for the previous row)",
									"", true, null),
							outputChannelParameter, new AtomicBoolean(false),
							new AtomicBoolean(false), new ProgressRenderer(table1, null), null, null, null, null,
							new AtomicInteger(0), // outputlocks 19
							null, null, null, null, // plugin inputs
							null, null, null, null, Boolean.FALSE, null, null, null, (long) 0, null // imp for display
					} };
			private boolean updatePipeline, updateCurrentStep, cancelUponChange, globalCancelUponChange,
					openUsingVirtualStacks;

			public void externalClick(MouseEvent e, int ourColumn, int ourRow) {
				// Duplicated from earlier
				// TODO This needs to be cleaned up and given a decent structure

				Point p = e.getPoint();
				p.x += table1.getColumnModel().getColumn(0).getWidth();
				int currentColumn = table1.columnAtPoint(p);
				if ((currentColumn == PLUGIN_PARAM_1_FIELD) || (currentColumn == PLUGIN_PARAM_2_FIELD))
					return;

				imageListMenuXCellIndex = ourRow;
				imageListMenuYCellIndex = ourColumn;// table.columnAtPoint(p);
				if (imageListMenuXCellIndex >= 0) {
					if ((imageListMenuYCellIndex == INPUT_NAME_FIELD)
							&& ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)) {
						displayImageListMenu(imageListMenuXCellIndex, imageListMenuYCellIndex, e.getComponent(), e
								.getX(), e.getY());
					} else if ((imageListMenuYCellIndex == OUTPUT_NAME_FIELD)
							&& ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)) {
						displayImageListMenu(imageListMenuXCellIndex, imageListMenuYCellIndex, e.getComponent(), e
								.getX(), e.getY());
					} else if (imageListMenuYCellIndex == PLUGIN_NAME_FIELD) {
						displayPluginListMenu(imageListMenuXCellIndex, imageListMenuYCellIndex, e.getComponent(), e
								.getX(), e.getY());
					} else if ((imageListMenuYCellIndex == PLUGIN_PARAM_1_FIELD)
							|| (imageListMenuYCellIndex == PLUGIN_PARAM_2_FIELD)) {
						if (((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0)) {
							Object param =
									table1.getModel().getValueAt(imageListMenuXCellIndex, imageListMenuYCellIndex);
							if (param instanceof SplitParameter) {
								AbstractParameter[] parameters = ((SplitParameter) param).getParameterValue();
								if ((parameters[0] instanceof FileNameParameter)
										&& (parameters[1] instanceof DirectoryParameter))
									displayFileDirectoryDialog((FileNameParameter) parameters[0],
											(DirectoryParameter) parameters[1], imageListMenuXCellIndex,
											imageListMenuYCellIndex);
							}
						}
					}
				}

			}

			/**
			 * Called when the user has finished editing a text box in the table. Not using the standard listener system
			 * because it's easier to respond specifically to this specific GUI event.
			 * 
			 * @param column
			 * @param row
			 */
			public void editingFinished(int column, int row) {
				// Check if column corresponds to input name, and if so clear pluginInputs and pluginOutputs of
				// corresponding plugin
				if (column == INPUT_NAME_FIELD) {
					if (data[row][PLUGIN_INSTANCE] != null) {
						PipelinePlugin plugin = (PipelinePlugin) data[row][PLUGIN_INSTANCE];
						plugin.clearInputs();
						plugin.clearOutputs();
						// Utils.log("Cleared pluginInputs and pluginOutputs of plugin at row "+row+" because of source name change",LogLevel.VERBOSE_DEBUG);
					}
				}
			}

			@Override
			public void fireTableCellUpdated(final int row, final int column) {
				final MyTableModel theTable = this;
				if (!SwingUtilities.isEventDispatchThread()) {
					SwingUtilities.invokeLater(() -> theTable.fireTableCellUpdated(row, column));
				} else
					super.fireTableCellUpdated(row, column);
			}

			@Override
			public void fireTableRowsUpdated(final int row1, final int row2) {
				final MyTableModel theTable = this;
				if (!SwingUtilities.isEventDispatchThread()) {
					SwingUtilities.invokeLater(() -> theTable.fireTableCellUpdated(row1, row2));
				} else
					super.fireTableCellUpdated(row2, row2);
			}

			@Override
			public void fireTableDataChanged() {
				final MyTableModel theTable = this;
				if (!SwingUtilities.isEventDispatchThread()) {
					SwingUtilities.invokeLater(theTable::fireTableDataChanged);
				} else {
					super.fireTableDataChanged();
				}
			}

			private void adjustTableRowHeights() {
				/*
				 * for (int i=0;i<data.length;i++){ if (data[i][ROW_HEIGHT]!=null){ table.setRowHeight(i,((Integer)
				 * data[i][ROW_HEIGHT]).intValue()); //IJ.log("row "+i+" set at height "+((Integer)
				 * data[i][ROW_HEIGHT]).intValue()); } else {} //IJ.log("row "+i+" has not been assigned a height"); }
				 */
			}

			private void adjustPluginRowInfo() {
				for (int i = 0; i < data.length; i++) {
					if (data[i][PLUGIN_INSTANCE] != null) {
						((PipelinePlugin) data[i][PLUGIN_INSTANCE]).setRow(i);
					}
				}
			}

			private void moveRow(final int from, final int to) {
				if (from < 0) {
					Utils.log("negative from in move row", LogLevel.ERROR);
					return;
				}
				if (to < 0) {
					Utils.log("negative to in move row", LogLevel.ERROR);
					return;
				}

				final int top = Utils.min(from, to);
				final int bottom = Utils.max(from, to);
				final boolean goingUp = (to == bottom);// Says whether the rows in-between from and to will move up or
				// down

				RowTransform transform = (isAbsolute, myRowPosition, currentReference) -> {
					int actualReference = isAbsolute ? currentReference : myRowPosition + currentReference;
					// Compute the absolute position of a relative reference

						if ((myRowPosition < top) || (myRowPosition >= bottom)) { // myRowPosition won't move
							if ((actualReference < top) || (actualReference >= bottom)) { // actualReference won't move
								// either
								return currentReference;
							} else { // myRowPosition doesn't move, but actualReference does
								if (myRowPosition < top) {
									if (goingUp)
										return currentReference - 1;
									else
										return currentReference + 1;
								} else {
									if (goingUp)
										return currentReference + 1;
									else
										return currentReference - 1;
								}
							}
						} else { // myRowPosition will move up or down
							if ((actualReference < top) || (actualReference > bottom)) { // actualReference stays in the
								// same place
								if (isAbsolute) {
									return currentReference;
								} else {
									if (goingUp) { //
										if (actualReference < top) // Getting closer to reference
											return currentReference - 1;
										else
											return currentReference + 1; // Getting further away from reference
									} else {
										if (actualReference < top)
											return currentReference + 1;
										else
											return currentReference - 1;
									}
								}
							} else {// Both myRowPosition and reference are moving together
								// An absolute reference will change, but a relative one won't
								if (isAbsolute) {
									if (goingUp)
										return currentReference - 1;
									else
										return currentReference + 1;
								} else {
									return currentReference;
								}
							}
						}
					};

				transformRowReferences(transform);

				int[] rowHeights = new int[data.length];

				int m = (from > to) ? from : to;

				for (int i = m + 1; i < data.length; i++) {
					rowHeights[i] = table1.getRowHeight(i);
				}

				m = (from > to) ? to : from;
				for (int i = 0; i < m; i++) {
					rowHeights[i] = table1.getRowHeight(i);
				}

				Object[] saverow = data[from];

				if (from > to) {
					int saveheight = table1.getRowHeight(from);
					for (int i = from; i >= to + 1; i--) {
						data[i] = data[i - 1];
						rowHeights[i] = table1.getRowHeight(i - 1);
						if (table1.getRowHeight(i - 1) < 1)
							Utils.log("row " + (i - 1) + " has height " + table1.getRowHeight(i - 1), LogLevel.DEBUG);
					}
					data[to] = saverow;
					rowHeights[to] = saveheight;
				} else if (to > from) {
					int saveheight = table1.getRowHeight(from);
					for (int i = from; i <= to - 1; i++) {
						data[i] = data[i + 1];
						rowHeights[i] = table1.getRowHeight(i + 1);
						if (table1.getRowHeight(i + 1) < 1)
							Utils.log("row " + (i + 1) + " has height " + table1.getRowHeight(i + 1), LogLevel.DEBUG);
					}
					data[to] = saverow;
					rowHeights[to] = saveheight;
				}
				fireTableDataChanged();
				for (int i = 0; i < data.length; i++) {
					table1.setRowHeight(i, rowHeights[i]);
				}
				adjustPluginRowInfo();
			}

			private void deleteRow(final int r) {
				RowTransform transform = (isAbsolute, myRowPosition, currentReference) -> {
					int actualReference = isAbsolute ? currentReference : myRowPosition + currentReference;
					// Compute the absolute position of a relative reference
						if (myRowPosition <= r) {
							if (actualReference <= r) {
								return currentReference;
							} else {
								return currentReference - 1;
							}
						} else { // Row myRowPosition will get bumped up
							if (actualReference > r) { // Both row and the reference will get bumped up
								if (isAbsolute) {
									return currentReference - 1;
								} else {
									return currentReference;
								}
							} else {// Reference is before deleted row; an absolute reference won't change,
								// but a relative one will
								if (isAbsolute) {
									return currentReference;
								} else {
									return currentReference + 1;
								}
							}
						}
					};

				transformRowReferences(transform);

				Object[][] newarray = new Object[data.length - 1][];
				System.arraycopy(data, 0, newarray, 0, r);
				System.arraycopy(data, r + 1, newarray, r, data.length - 1 - r);
				data = newarray;
				fireTableDataChanged();
				adjustTableRowHeights();
				adjustPluginRowInfo();
			}

			private void transformRowReferences(RowTransform transform) {
				for (int i = 0; i < data.length; i++) {
					Object[] row = data[i];
					for (Object element : row) {
						if (element instanceof RowReferenceHolder) {
							((RowReferenceHolder) element).offsetReference(i, transform);
						}
					}
				}
			}

			private void newRow(final int position, boolean updateReferences) {
				if (updateReferences) {
					RowTransform transform = (isAbsolute, myRowPosition, currentReference) -> {
						int actualReference = isAbsolute ? currentReference : myRowPosition + currentReference;
						// Compute the absolute position of a relative reference
							if (myRowPosition <= position) {
								if (actualReference <= position) {
									return currentReference;
								} else {
									return currentReference + 1;
								}
							} else { // Row myRowPosition will get bumped down
								if (actualReference > position) { // Both row and the reference will get bumped down
									if (isAbsolute) {
										return currentReference + 1;
									} else {
										return currentReference;
									}
								} else {// Reference is before inserted row; an absolute reference won't change,
									// but a relative one will
									if (isAbsolute) {
										return currentReference;
									} else {
										return currentReference - 1;
									}
								}
							}
						};
					transformRowReferences(transform);
				}

				if (position >= data.length) {
					throw new RuntimeException("Trying to insert too far down in the table");
				}
				Object[][] newarray = new Object[data.length + 1][];
				// Move everything down from row position

				System.arraycopy(data, 0, newarray, 0, Utils.min(data.length, position + 1));
				System.arraycopy(data, position + 2 - 1, newarray, position + 2, data.length + 1 - (position + 2));

				// Initialize things at row position
				TableParameter outputChannelParameter1 =
						new TableParameter("Output channels", "Choose output channel names", new String[] { "Ch0",
								"Ch1" }, null);

				newarray[position + 1] = new Object[TABLE_WIDTH];
				newarray[position + 1][INPUT_NAME_FIELD] =
						new RowOrFileTextReference(
								"",
								"Image that this step uses as an input. Name of an open image, absolute reference to another row starting with a dollar (e.g. $2), or relative reference (e.g. -1 for the previous row)",
								"", true, null);
				newarray[position + 1][SHOW_IMAGE] = Boolean.TRUE;
				newarray[position + 1][KEEP_C_PLUGIN_ALIVE] = Boolean.FALSE;
				newarray[position + 1][USE_STEP] = Boolean.TRUE;
				newarray[position + 1][RESET_RANGE] = Boolean.TRUE;
				newarray[position + 1][PLUGIN_NAME_FIELD] = "Click for plugin";
				newarray[position + 1][PLUGIN_PARAM_1_FIELD] = "";
				newarray[position + 1][PLUGIN_PARAM_2_FIELD] = "";
				newarray[position + 1][OUTPUT_NAME_FIELD] =
						new RowOrFileTextReference(
								"",
								"Image that this step uses as an input. Name of an open image, absolute reference to another row starting with a dollar (e.g. $2), or relative reference (e.g. -1 for the previous row)",
								"", true, null);
				newarray[position + 1][OUT_CHANNELS_FIELD] = outputChannelParameter1;
				newarray[position + 1][IS_UPDATING] = new AtomicBoolean(false);
				newarray[position + 1][UPDATE_QUEUED] = new AtomicBoolean(false);
				newarray[position + 1][OUTPUT_LOCKS] = new AtomicInteger(0);
				newarray[position + 1][WORKER_THREAD] = null;
				newarray[position + 1][QUEUED_WORKER_THREAD] = null;
				newarray[position + 1][PLUGIN_INSTANCE] = null;
				newarray[position + 1][Z_PROJ] = null;
				newarray[position + 1][PERCENT_DONE] = new ProgressRenderer(table1, null);
				newarray[position + 1][ROW_HEIGHT] = 20;
				newarray[position + 1][PLUGIN_OUTPUTS] = null;
				newarray[position + 1][PLUGIN_INPUTS] = null;
				newarray[position + 1][COMPUTING_ERROR] = Boolean.FALSE;
				newarray[position + 1][LAST_TIME_RUN] = (long) 0;

				MultiListParameter newChannelsParameter =
						new MultiListParameter("channels", "Choose while channels to process",
								new @NonNull String @NonNull[] { "Ch0", "Ch1" }, new int[] { 1, 2 }, null);
				newarray[position + 1][WORK_ON_CHANNEL_FIELD] = newChannelsParameter;

				((ProgressRenderer) newarray[position + 1][PERCENT_DONE]).setIndeterminate(false);
				((ProgressRenderer) newarray[position + 1][PERCENT_DONE]).setValue(0);
				data = newarray;

				fireTableDataChanged();

				adjustTableRowHeights();
				adjustPluginRowInfo();
				table1.setRowSelectionInterval(position + 1, position + 1);
			}

			@Override
			public int getColumnCount() {
				return columnNames.length;
			}

			@Override
			public int getRowCount() {
				return data.length;
			}

			@Override
			public String getColumnName(int col) {
				return columnNames[col];
			}

			@Override
			public Object getValueAt(int row, int col) {
				if (col == 0)
					return (row + 1);
				else
					return data[row][col];
			}

			/*
			 * JTable uses this method to determine the default renderer/ editor for each cell. If we didn't implement
			 * this method, then the last column would contain text ("true"/"false"), rather than a check box.
			 */
			@Override
			public Class<?> getColumnClass(int c) {
				if (getValueAt(0, c) != null)
					return getValueAt(0, c).getClass();
				else
					return "".getClass();
			}

			@Override
			public boolean isCellEditable(int row, int col) {
				return (col > 0 && col != PLUGIN_NAME_FIELD);// Do not want the row numbers to be editable
			}

			@Override
			public void setValueAt(Object value, int row, int col) {
				if (col > 0)
					data[row][col] = value; // Do not update the row number
				fireTableCellUpdated(row, col);

				if (col == USE_STEP) { // Update the name of the source channels for the processing step
					// after the one that's just been activated or deactivated
					try {
						// Update input channel names for row+1
						if (row + 1 < data.length) {
							Utils.log("updating channels", LogLevel.DEBUG);
							updateSourceFieldAtRow(row + 1);
						}
					} catch (Exception e) {
						Utils.printStack(e);
					}
					fireTableRowsUpdated(row, row + 1);
				}
			}

			public void dropFile(File file, int ourColumn, int ourRow) {
				// TODO Auto-generated method stub

			}
		}

		/**
		 * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching
		 * thread.
		 */

		private String pipelineName;

		private void createAndShowGUI() {
			if (!Utils.headless) {
				// Disable boldface controls.
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				// Create and set up the window.

				pipelineWindowCounter++;
				final JFrame frame = new JFrame("Image processing workflow " + pipelineWindowCounter);
				frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				JRootPane root = frame.getRootPane();
				pipelineName = "Workflow" + pipelineWindowCounter;
				root.putClientProperty("Window.documentFile", new FakeFileForPipelineRef(pipelineName, this));
				try {
					new File(pipelineName).createNewFile();
				} catch (IOException e2) {
					Utils.printStack(e2, LogLevel.WARNING);
				}

				// Create and set up the content pane.
				TableSelectionDemo newContentPane = this;
				newContentPane.setOpaque(true);
				frame.setContentPane(newContentPane);

				// Display the window.
				frame.pack();
				frame.setVisible(true);

				frame.addWindowListener(new WindowListener() {

					@Override
					public void windowOpened(WindowEvent e) {
					}

					@Override
					public void windowIconified(WindowEvent e) {
					}

					@Override
					public void windowDeiconified(WindowEvent e) {
					}

					@Override
					public void windowDeactivated(WindowEvent e) {
					}

					@Override
					public void windowClosing(WindowEvent e) {
					}

					@Override
					public void windowClosed(WindowEvent e) {
						cleanUpAllPlugins();
						frame.setMenuBar(null); // If not the menubar keeps a reference to the frame, preventing it from
						// being garbage collected until the menubar is updated
						KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
					}

					@Override
					public void windowActivated(WindowEvent e) {
						if (IJ.isMacOSX())
							frame.setMenuBar(Menus.getMenuBar());
					}
				});

				tableFrame = frame;

				Utils.log("CREATED GUI", LogLevel.DEBUG);
			} else {
				Utils.log("Running in headless mode", LogLevel.DEBUG);
			}

		}

	}

	public interface RusageLibrary extends Library {
		public void get_rusage_wrapper(NativeLong[] stats);
	}

	// Copied from http://weblogs.java.net/blog/alexfromsun/archive/2006/02/debugging_swing.html
	public static final class CheckThreadViolationRepaintManager extends RepaintManager {
		// It is recommended to pass the complete check
		private boolean completeCheck = true;
		private WeakReference<JComponent> lastComponent;

		public CheckThreadViolationRepaintManager(boolean completeCheck1) {
			this.completeCheck = completeCheck1;
		}

		public CheckThreadViolationRepaintManager() {
			this(true);
		}

		public boolean isCompleteCheck() {
			return completeCheck;
		}

		public void setCompleteCheck(boolean completeCheck1) {
			this.completeCheck = completeCheck1;
		}

		@Override
		public synchronized void addInvalidComponent(JComponent component) {
			checkThreadViolations(component);
			super.addInvalidComponent(component);
		}

		@Override
		public void addDirtyRegion(JComponent component, int x, int y, int w, int h) {
			checkThreadViolations(component);
			super.addDirtyRegion(component, x, y, w, h);
		}

		private void checkThreadViolations(JComponent c) {
			if (!SwingUtilities.isEventDispatchThread() && (completeCheck || c.isShowing())) {
				boolean repaint = false;
				boolean fromSwing = false;
				boolean imageUpdate = false;
				StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
				for (StackTraceElement st : stackTrace) {
					if (repaint && st.getClassName().startsWith("javax.swing.") &&
					// For details see
					// https://swinghelper.dev.java.net/issues/show_bug.cgi?id=1
							!st.getClassName().startsWith("javax.swing.SwingWorker")) {
						fromSwing = true;
					}
					if (repaint && "imageUpdate".equals(st.getMethodName())) {
						imageUpdate = true;
					}
					if ("repaint".equals(st.getMethodName())) {
						repaint = true;
						fromSwing = false;
					}
				}
				if (imageUpdate) {
					// Assuming it is java.awt.image.ImageObserver.imageUpdate(...)
					// image was asynchronously updated, that's ok
					return;
				}
				if (repaint && !fromSwing) {
					// No problems here, since repaint() is thread safe
					return;
				}
				// Ignore the last processed component
				if (lastComponent != null && c == lastComponent.get()) {
					return;
				}
				lastComponent = new WeakReference<>(c);
				violationFound(c, stackTrace);
			}
		}

		void violationFound(JComponent c, StackTraceElement[] stackTrace) {
			// Uncomment code below to get notification of thread violations
			// Commented because there are some false positives
			// Utils.log("EDT violation detected");
			// for (StackTraceElement st : stackTrace) {
			// Utils.log("\tat " + st);
			// }
		}
	}

	public void runPipeline() {
		table.runPipeline();
	}

	public void uncheckTableExceptLast() {
		Utils.log("Unchecking the table", LogLevel.DEBUG);
		table.uncheckTableExceptLast();
	}

	/**
	 * 
	 * @return Path to directory containing native_libs and icons directories
	 */
	public static String getBaseDir() {
		return new File(A0PipeLine_Manager.class.getProtectionDomain().getCodeSource().getLocation().getPath())
				.getParentFile(). // plugins directory
				getParent();
	}

	public static void main(String[] args) {
		int returnValue = 0;
		boolean exitOnFinish = true;
		Utils.headless = true;
		try {
			addDir(getBaseDir() + "/native_libs/" + DylibInfo.getDylibInfo().directoryName + "/");
			System.setProperty("jna.library.path", getBaseDir() + "/native_libs/"
					+ DylibInfo.getDylibInfo().directoryName + "/");

			CommandLineArguments argValues = new CommandLineArguments();
			JCommander commander = new JCommander();
			commander.setAcceptUnknownOptions(true);
			commander.setAllowAbbreviatedOptions(false);

			commander.addObject(argValues);
			commander.parse(args);

			if (argValues.help) {
				commander.usage();
				return;
			}

			Utils.logLevelThreshold = argValues.logLevel;

			Utils.setLogFile(argValues.logFile);

			if (argValues.suppressSaveTable == 1)
				SaveTable.disableSaving = true;

			String command = argValues.action.get(0);
			switch (command) {
				case "pipeline": {
					A0PipeLine_Manager pipeline = new A0PipeLine_Manager();
					pipeline.arguments = args;
					TableSelectionDemo table = pipeline.new TableSelectionDemo();
					Utils.logLevelThreshold = argValues.logLevel;
					table.loadTable(argValues.action.get(1));
					table.createAndShowGUI();
					table.runPipeline();
					break;
				}
				case "batch":
				case "singleRun":
				case "singleRunLog": {
					A0PipeLine_Manager pipeline = new A0PipeLine_Manager();
					pipeline.arguments = args;
					TableSelectionDemo table = pipeline.new TableSelectionDemo();
					Utils.logLevelThreshold = argValues.logLevel;

					if (argValues.action.size() < 2) {
						throw new IllegalArgumentException("Missing parameter for command " + command);
					}

					table.loadTable(argValues.action.get(1));
					table.createAndShowGUI();

					switch (command) {
						case "singleRun":
						case "singleRunLog":
							if (argValues.suppressSaveTable == -1)
								SaveTable.disableSaving = true;
							returnValue = table.batch(true, true);
							break;
						case "batch":
							returnValue = table.batch(true, false);
							break;
						default:
							throw new IllegalStateException();
					}

					break;
				}
				default:
					System.out.println("Unrecognized command " + command);
					System.exit(1);
			}
		} catch (Throwable e) {
			Utils.printStack(e);
			System.exit(1);
		}
		if (exitOnFinish)
			System.exit(returnValue);
	}

	private static void addDir(String s) throws IOException {
		try {
			/**
			 * This enables the java.library.path to be modified at runtime.
			 * From a Sun engineer at http://forums.sun.com/thread.jspa?threadID=707176
			 */
			Field field = ClassLoader.class.getDeclaredField("usr_paths");
			field.setAccessible(true);
			String[] paths = (String[]) field.get(null);
			for (String path : paths) {
				if (s.equals(path)) {
					return;
				}
			}
			String[] tmp = new String[paths.length + 1];
			System.arraycopy(paths, 0, tmp, 0, paths.length);
			tmp[paths.length] = s;
			field.set(null, tmp);
			System.setProperty("java.library.path", System.getProperty("java.library.path") + File.pathSeparator + s);
		} catch (IllegalAccessException e) {
			throw new IOException("Failed to get permissions to set library path");
		} catch (NoSuchFieldException e) {
			throw new IOException("Failed to get field handle to set library path");
		}
	}

	@Override
	public void run(String s) {
		Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
		try {
			addDir(getBaseDir() + "/native_libs/" + DylibInfo.getDylibInfo().directoryName + "/");
		} catch (IOException e) {
			Utils.printStack(e);
		}
		System.setProperty("jna.library.path", getBaseDir() + "/native_libs/" + DylibInfo.getDylibInfo().directoryName
				+ "/");

		Utils.setLogFile(A0PipeLine_Manager.getBaseDir() + "/pipeline_log.txt");

		javax.swing.SwingUtilities.invokeLater(() -> {
			table = new TableSelectionDemo();
			// RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());
				table.createAndShowGUI();
			});
	}

	private static int pipelineWindowCounter = 0;
	public static ReflectionProvider reflectionProvider;

	static {
		boolean b = true;
		if (b) {
			reflectionProvider = new Sun14ReflectionProvider();
		} else {
			reflectionProvider = new PureJavaReflectionProvider();
		}
	}

}
