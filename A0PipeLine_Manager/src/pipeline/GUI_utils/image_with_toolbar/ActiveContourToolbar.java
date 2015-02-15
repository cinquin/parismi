/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils.image_with_toolbar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.table.DefaultTableModel;

import pipeline.A0PipeLine_Manager;
import pipeline.GUI_utils.OrthogonalViewsWithComposites;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.TableBetterFocus;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameter_cell_views.FloatSlider;
import pipeline.parameter_cell_views.IntSlider;
import pipeline.parameter_cell_views.OneColumnJTable;
import pipeline.parameter_cell_views.TextBox;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.ParameterListenerAdapter;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;
import pipeline.plugins.MouseEventPlugin;

// TODO Icons are from http://www.famfamfam.com/lab/icons/silk/ **which requires attribution in a credits page**

/**
 * A toolbar designed for editing of segmentations obtained by active contours.
 */
public class ActiveContourToolbar extends Toolbar {

	private static final long serialVersionUID = 1L;

	private JToolBar toolbar;
	/**
	 * Group of buttons in the toolbar. Use getSelection().getActionCommand() to retrieve the selected button.
	 */
	private ButtonGroup buttonGroup;

	private JRadioButton createNewPoints;
	private JRadioButton editExistingCells;
	private TableParameter annotationNames;
	private IntParameter labelDepth, lineThickness, depthOfField;
	private FloatParameter transparency, hoverDelay;
	private TextParameter seedColorParam;

	public int getLabelDepth() {
		return labelDepth.getintValue();
	}

	public float getTransparency() {
		return transparency.getFloatValue();
	}

	private static String[] lastAnnotationNames;
	private static int lastLabelDepth = 10;
	private static int lastLineThickness = 3;
	private static float lastTransparency = 0.0f;
	private static float lastHoverDelay = 2f;
	private static int lastDepthOfField = 0;
	private static String lastSeedColor = "green";

	private int currentModifier = 0;

	/**
	 * A negative value means that the click should be ignored (probably because the user has selected "browsing mode",
	 * where the mouse is used to adjust the view rather than to trigger plugins).
	 */
	@Override
	public int getCurrentModifier() {
		return currentModifier;
	}

	private static String[] loadAnnotationNamesFromFile(File f) throws IOException {
		try (FileReader reader = new FileReader(f); BufferedReader bufferedReader = new BufferedReader(reader, 100); Scanner s =
				new Scanner(bufferedReader)) {
			s.useDelimiter("\t");

			ArrayList<String> names = new ArrayList<>();

			while (s.hasNext()) {
				names.add(s.next());
			}

			return names.toArray(new String[] {});
		}
	}

	private void saveAnnotationNamesToFile(File f) throws IOException {
		try (FileWriter writer = new FileWriter(f, false)) {
			for (String s : annotationNames.getElements()) {
				writer.append(s);
				writer.append("\t");
			}
		}
	}

	{
		try {
			lastAnnotationNames =
					loadAnnotationNamesFromFile(new File(FileNameUtils.expandPath("~/pipeline_labels.txt")));
		} catch (IOException e) {
			Utils.log("Could not load pipeline labels", LogLevel.DEBUG);
			Utils.printStack(e, LogLevel.DEBUG);
		}
	}

	private transient ParameterListener annotationNamesListener;
	private JToggleButton newButton, deleteButton;
	static private String pathToIcons = A0PipeLine_Manager.getBaseDir() + "/icons/";

	/**
	 * 
	 * @param parentView
	 *            In the current implementation, must implement MouseEventPlugin (it would not
	 *            be difficult to relax that requirement).
	 */
	public ActiveContourToolbar(final PluginIOView parentView) {
		super();
		this.parentView = parentView;
		toolbar = new JToolBar();
		toolbar.setFocusable(false);
		buttonGroup = new ButtonGroup();

		browseButton = new JToggleButton(new ImageIcon(pathToIcons + "cursor.gif", "Browse image"));
		browseButton.setFocusable(false);
		browseButton.setText("Browse");
		browseButton.setMnemonic('B');
		browseButton.setActionCommand("Browse image");
		browseButton.addItemListener(this);
		browseButton.addActionListener(e -> currentModifier = -1);
		currentModifier = -1;
		toolbar.add(browseButton);
		buttonGroup.add(browseButton);
		browseButton.setSelected(true);

		JToggleButton button = new JToggleButton(new ImageIcon(pathToIcons + "add.gif", "New active contour"));
		button.setFocusable(false);
		button.setText("New");
		button.setMnemonic('N');
		button.setActionCommand("New active contour");
		button.addActionListener(this);
		button.addActionListener(e -> currentModifier = PluginIOCells.ADD_MODIFIER);
		toolbar.add(button);
		buttonGroup.add(button);
		newButton = button;

		button = new JToggleButton(new ImageIcon(pathToIcons + "delete.gif", "Delete cell"));
		button.setFocusable(false);
		button.setText("Delete");
		button.setMnemonic('D');
		button.setActionCommand("Delete cell");
		button.addActionListener(this);
		button.addActionListener(e -> currentModifier = PluginIOCells.DELETE_MODIFIER);
		toolbar.add(button);
		buttonGroup.add(button);
		deleteButton = button;

		button = new JToggleButton(new ImageIcon(pathToIcons + "arrow_join.gif", "Join cells"));
		button.setFocusable(false);
		button.setText("Merge");
		button.setMnemonic('M');
		button.setActionCommand("Join cells");
		button.addActionListener(this);
		button.addActionListener(e -> {
			currentModifier = PluginIOCells.MERGE_MODIFIER;
			clickGroup++;
		});
		toolbar.add(button);
		buttonGroup.add(button);

		button = new JToggleButton(new ImageIcon(pathToIcons + "wand.gif", "Resize"));
		button.setFocusable(false);
		button.setText("Resize");
		button.setMnemonic('R');
		button.setActionCommand("Resize");
		button.addActionListener(this);
		button.addActionListener(e -> currentModifier = PluginIOCells.RESIZE_MODIFIER);
		toolbar.add(button);
		buttonGroup.add(button);

		button = new JToggleButton(new ImageIcon(pathToIcons + "application_tile_horizontal.gif", "Orthogonal views"));
		button.setFocusable(false);
		button.setText("Orthogonal");
		button.setMnemonic('O');
		button.setActionCommand("Orthogonal views");
		button.addActionListener(this);
		toolbar.add(button);

		JButton jbutton = new JButton(new ImageIcon(pathToIcons + "arrow_right.gif", "Flush clicks"));
		button.setFocusable(false);
		jbutton.setText("Flush");
		jbutton.setMnemonic('F');
		jbutton.setActionCommand("Flush clicks");
		jbutton.addActionListener(e -> ((MouseEventPlugin) parentView).processClicks());
		toolbar.add(jbutton);

		button = new JToggleButton(new ImageIcon(pathToIcons + "pencil_add.png", "Label cells"));
		button.setFocusable(false);
		button.setText("Label");
		button.setMnemonic('L');
		button.setActionCommand("Label cells");
		button.addActionListener(this);
		button.addActionListener(e -> currentModifier = PluginIOCells.LABEL_MODIFIER);
		toolbar.add(button);
		buttonGroup.add(button);

		button = new JToggleButton(new ImageIcon(pathToIcons + "pencil_delete.png", "Clear labels"));
		button.setFocusable(false);
		button.setText("Clear labels");
		button.setMnemonic('L');
		button.setActionCommand("Clear labels");
		button.addActionListener(this);
		button.addActionListener(e -> currentModifier = PluginIOCells.DELETE_LABELS_MODIFIER);

		toolbar.add(button);
		buttonGroup.add(button);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = 1;
		c.gridheight = 3;
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		add(toolbar, c);

		final JCheckBox overrideRuntimeButton = new JCheckBox("Override active contour runtime", false);
		overrideRuntimeButton.addActionListener(e -> overrideRuntime = overrideRuntimeButton.isSelected());
		add(overrideRuntimeButton, c);

		final JCheckBox stopVideoOnCellsButton = new JCheckBox("Stop video on cells", true);
		stopVideoOnCellsButton.addActionListener(e -> stopVideoOnCells = stopVideoOnCellsButton.isSelected());
		add(stopVideoOnCellsButton, c);

		DefaultTableModel smoothingTableModel = new DefaultTableModel(1, 1);
		TableBetterFocus smoothingParameterTable = new TableBetterFocus(smoothingTableModel);
		smoothingParameterTable.setFillsViewportHeight(false);
		smoothingParameterTable.getColumn(0).setCellEditor(new FloatSlider());
		smoothingParameterTable.getColumn(0).setCellRenderer(new FloatSlider());
		smoothingTableModel.setValueAt(new FloatParameter("Active contour runtime", "Active contour runtime",
				activeContourRuntime, 0.0f, 100.0f, true, true, true, new runtimeListener()), 0, 0);

		smoothingParameterTable.setPreferredSize(new Dimension(300, 50));

		add(smoothingParameterTable, c);

		// Controls for annotations
		ButtonGroup group = new ButtonGroup();
		editExistingCells = new JRadioButton("Label existing cells or points");
		createNewPoints = new JRadioButton("Create and annotate new points");
		group.add(editExistingCells);
		group.add(createNewPoints);
		editExistingCells.setSelected(true);
		add(editExistingCells);

		String[] initialNames =
				lastAnnotationNames != null ? lastAnnotationNames.clone() : new String[] { "Label 1", "Label 2" };

		annotationNames = new TableParameter("Annotation names", "", initialNames, null);

		annotationNames.setPostProcessor(input -> {
			if (input == null)
				return null;
			if (!(input instanceof String)) {
				throw new IllegalArgumentException();
			}
			String s = (String) input;
			return s.replace(" ", "");
		});

		annotationNames.setEnforceUniqueEntries(true);

		annotationNamesListener = new ParameterListenerAdapter() {
			@Override
			public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
					boolean keepQuiet) {
				try {
					saveAnnotationNamesToFile(new File(FileNameUtils.expandPath("~/pipeline_labels.txt")));
				} catch (IOException e) {
					Utils.log("Could not save pipeline labels", LogLevel.DEBUG);
					Utils.printStack(e, LogLevel.DEBUG);
				}
			}
		};

		annotationNames.addPluginListener(annotationNamesListener);

		lastAnnotationNames = annotationNames.getElements().clone();

		final OneColumnJTable annotationView = new OneColumnJTable();

		JPanel annotationButtons = new JPanel();
		JButton newAnnotation = new JButton("+");
		newAnnotation.addActionListener(e -> {
			String[] nameList = annotationNames.getElements();
			int[] selection = annotationNames.getSelection();
			int insertAt = (selection == null || selection.length == 0) ? 0 : selection[0];

			ArrayList<String> s = new ArrayList<>();
			s.addAll(Arrays.asList(nameList));
			s.add(insertAt, "New");

			annotationNames.setElements(s.toArray(new String[] {}));

			annotationNames.fireValueChanged(false, true, true);
		});
		JButton deleteAnnotation = new JButton("-");
		deleteAnnotation.addActionListener(e -> {
			int[] selection = annotationNames.getSelection();
			if (selection == null || selection.length == 0)
				return;

			Arrays.sort(selection);

			String[] nameList = annotationNames.getElements();

			String[] newElements = new String[nameList.length - selection.length];

			int indexNewArray = 0;
			int indexSelection = 0;
			for (int i = 0; i < nameList.length; i++) {
				if ((indexSelection < selection.length) && (selection[indexSelection] == i)) {
					indexSelection++;
					continue;
				}
				newElements[indexNewArray] = nameList[i];
				indexNewArray++;
			}

			annotationNames.setElements(newElements);
			annotationNames.fireValueChanged(false, true, true);

		});
		JButton loadAnnotationNames = new JButton("Load names");
		loadAnnotationNames.addActionListener(e -> {
			FileDialog dialog = new FileDialog(new Frame(), "Load annotation names from...", FileDialog.LOAD);
			dialog.setVisible(true);
			String filePath = dialog.getDirectory();
			if (filePath == null)
				return;
			filePath += dialog.getFile();

			try {
				annotationNames.setElements(loadAnnotationNamesFromFile(new File(filePath)));
				annotationNames.fireValueChanged(false, true, true);
			} catch (IOException e1) {
				Utils.displayMessage(e1.toString(), true, LogLevel.ERROR);
			}
		});

		JButton saveAnnotationNames = new JButton("Save names");
		saveAnnotationNames.addActionListener(e -> {
			File file = FileNameUtils.chooseFile("Save annotation names to...", FileDialog.SAVE);
			if (file == null)
				return;

			try {
				saveAnnotationNamesToFile(file);
			} catch (IOException e1) {
				Utils.displayMessage(e1.toString(), true, LogLevel.ERROR);
			}
		});

		JButton loadCells = new JButton("Load cells");
		loadCells.addActionListener(e -> {
			File file = FileNameUtils.chooseFile("Choose cells to load", FileDialog.LOAD);
			if (file == null)
				return;
			try {
				PluginIOCells cells = new PluginIOCells(file);
				((PluginIOHyperstackWithToolbar) parentView).setCellsToOverlay(cells);
			} catch (Exception e1) {
				Utils.displayMessage("Error loading cells: " + e1.getMessage(), true, LogLevel.ERROR);
				Utils.printStack(e1);
			}
		});

		JButton saveCells = new JButton("Save cells");
		saveCells.addActionListener(e -> {
			File file = FileNameUtils.chooseFile("Save cells to...", FileDialog.SAVE);
			if (file == null)
				return;
			try {
				((PluginIOHyperstackWithToolbar) parentView).getCellsToOverlay().asFile(file, false);
			} catch (Exception e1) {
				Utils.displayMessage("Error saving cells: " + e1.getMessage(), true, LogLevel.ERROR);
				Utils.printStack(e1);
			}
		});

		annotationButtons.add(editExistingCells);
		annotationButtons.add(createNewPoints);
		annotationButtons.add(newAnnotation);
		annotationButtons.add(deleteAnnotation);
		annotationButtons.add(loadCells);
		annotationButtons.add(saveCells);
		annotationButtons.add(loadAnnotationNames);
		annotationButtons.add(saveAnnotationNames);
		add(annotationButtons, c);

		annotationNames.displayHorizontally = true;
		add(annotationView.getTableCellRendererOrEditorComponent(null, annotationNames, false, false, 0, 0), c);

		ParameterListener labelDepthListener = new ParameterListenerAdapter() {

			@Override
			public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
					boolean keepQuiet) {
				int newDepth = labelDepth.getintValue();
				lastLabelDepth = newDepth;
				Object canvas = ((PluginIOHyperstackWithToolbar) parentView).imp.getCanvas();
				if (canvas instanceof ImageCanvasWithAnnotations) {
					((ImageCanvasWithAnnotations) canvas).setLabelDepth(newDepth);
					((ImageCanvasWithAnnotations) canvas).setUnscaledLabelDepth(newDepth);
				}
				OrthogonalViewsWithComposites orthViews =
						((PluginIOHyperstackWithToolbar) parentView).getOrthogonalViews();
				if (orthViews != null) {
					orthViews.setLabelDepth(newDepth);
				}
			}

		};

		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new GridBagLayout());
		// sliderPanel.setPreferredSize(new Dimension(600,70));
		GridBagConstraints d = new GridBagConstraints();
		d.fill = GridBagConstraints.BOTH;
		d.gridwidth = 3;
		d.gridheight = 1;
		d.gridx = GridBagConstraints.RELATIVE;
		d.gridy = 0;

		labelDepth =
				new IntParameter("Label depth", "Number of slices around center label appears in", lastLabelDepth, 1,
						Math.max(25, lastLabelDepth), true, true, labelDepthListener);
		IntSlider labelDepthView = new IntSlider();
		sliderPanel.add(labelDepthView.getTableCellRendererOrEditorComponent(null, labelDepth, false, false, 0, 0,
				false), d);

		ParameterListener transparencyListener = new ParameterListenerAdapter() {

			@Override
			public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
					boolean keepQuiet) {
				float newTransparencyValue = transparency.getFloatValue();
				lastTransparency = newTransparencyValue;
				Object canvas = ((PluginIOHyperstackWithToolbar) parentView).imp.getCanvas();
				if (canvas instanceof ImageCanvasWithAnnotations) {
					((ImageCanvasWithAnnotations) canvas).setTransparency(newTransparencyValue);
				}
				OrthogonalViewsWithComposites orthViews =
						((PluginIOHyperstackWithToolbar) parentView).getOrthogonalViews();
				if (orthViews != null) {
					orthViews.setTransparency(newTransparencyValue);
				}
			}
		};

		transparency =
				new FloatParameter("Transparency", "Sets how transparent labels appear on the original image",
						lastTransparency, 0f, 1f, true, true, true, transparencyListener);
		FloatSlider transparencySlider = new FloatSlider();
		sliderPanel.add(transparencySlider
				.getTableCellRendererOrEditorComponent(null, transparency, false, 0, 0, false), d);

		ParameterListener hoverDelayListener = new ParameterListenerAdapter() {

			@Override
			public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
					boolean keepQuiet) {
				float newHoverDelay = hoverDelay.getFloatValue();
				lastHoverDelay = newHoverDelay;
				Object canvas = ((PluginIOHyperstackWithToolbar) parentView).imp.getCanvas();
				if (canvas instanceof ImageCanvasWithAnnotations) {
					((ImageCanvasWithAnnotations) canvas).setHoverDelay(newHoverDelay);
				}
				OrthogonalViewsWithComposites orthViews =
						((PluginIOHyperstackWithToolbar) parentView).getOrthogonalViews();
				if (orthViews != null) {
					orthViews.setHoverDelay(newHoverDelay);
				}
			}
		};

		ParameterListener depthOfFieldListener = new ParameterListenerAdapter() {

			@Override
			public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
					boolean keepQuiet) {
				int newDepthOfField = depthOfField.getintValue();
				lastDepthOfField = newDepthOfField;
				((PluginIOHyperstackWithToolbar) parentView).setDepthOfField(newDepthOfField);
				/*
				 * if (canvas instanceof ImageCanvasWithAnnotations) {
				 * ((ImageCanvasWithAnnotations) canvas).setHoverDelay(newHoverDelay);
				 * }
				 * OrthogonalViewsWithComposites orthViews=((PluginIOHyperstackWithToolbar)
				 * parentView).getOrthogonalViews();
				 * if (orthViews!=null){
				 * orthViews.setHoverDelay(newHoverDelay);
				 * }
				 */
			}
		};

		depthOfField =
				new IntParameter("Depth of Field", "Number of slices to project in display", lastDepthOfField, 0, 15,
						true, true, depthOfFieldListener);
		IntSlider depthOfFieldSlider = new IntSlider();
		sliderPanel.add(depthOfFieldSlider.getTableCellRendererOrEditorComponent(null, depthOfField, false, false, 0,
				0, false), d);

		hoverDelay =
				new FloatParameter("Hover delay", "Time in seconds before cell info shows in a floating window",
						lastHoverDelay, 0f, 10f, true, true, true, hoverDelayListener);
		FloatSlider hoverDelaySlider = new FloatSlider();
		sliderPanel
				.add(hoverDelaySlider.getTableCellRendererOrEditorComponent(null, hoverDelay, false, 0, 0, false), d);

		ParameterListener lineThicknessListener = new ParameterListenerAdapter() {

			@Override
			public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
					boolean keepQuiet) {
				int newThickness = lineThickness.getintValue();
				lastLineThickness = newThickness;
				Object canvas = ((PluginIOHyperstackWithToolbar) parentView).imp.getCanvas();
				if (canvas instanceof ImageCanvasWithAnnotations) {
					((ImageCanvasWithAnnotations) canvas).setLineThickness(newThickness);
				}
				OrthogonalViewsWithComposites orthViews =
						((PluginIOHyperstackWithToolbar) parentView).getOrthogonalViews();
				if (orthViews != null) {
					orthViews.setLineThickness(newThickness);
				}
			}
		};

		lineThickness =
				new IntParameter("Line thickness", "Thickness of line outlining cells", lastLineThickness, 1, 25, true,
						true, lineThicknessListener);
		IntSlider lineThicknessView = new IntSlider();
		sliderPanel.add(lineThicknessView.getTableCellRendererOrEditorComponent(null, lineThickness, false, false, 0,
				0, false), d);

		ParameterListener colorListener = new ParameterListenerAdapter() {

			@Override
			public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
					boolean keepQuiet) {
				String newColorString = seedColorParam.getStringValue();
				lastSeedColor = newColorString;

				Color newColor = Utils.colorFromString(newColorString);

				Object canvas = ((PluginIOHyperstackWithToolbar) parentView).imp.getCanvas();
				if (canvas instanceof ImageCanvasWithAnnotations) {
					((ImageCanvasWithAnnotations) canvas).setDisplayColor(newColor);
				}
				OrthogonalViewsWithComposites orthViews =
						((PluginIOHyperstackWithToolbar) parentView).getOrthogonalViews();
				if (orthViews != null) {
					orthViews.setDisplayColor(newColor);
				}
			}
		};

		seedColorParam = new TextParameter("Color", "", lastSeedColor, true, colorListener, null);
		TextBox colorChoice = new TextBox();
		sliderPanel.add(colorChoice.getTableCellRendererOrEditorComponent(null, seedColorParam, false, false, 0, 0,
				false), d);

		add(sliderPanel, c);
		// labelDepth.parameterValueChanged(false, false, true);
	}

	private boolean overrideRuntime = false;
	private float activeContourRuntime = 0.0f;

	private boolean stopVideoOnCells = true;

	public boolean getStopVideoOnCells() {
		return stopVideoOnCells;
	}

	public boolean getOverrideRuntime() {
		return overrideRuntime;
	}

	public float getActiveContourRuntime() {
		return activeContourRuntime;
	}

	private class runtimeListener extends ParameterListenerAdapter {

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			activeContourRuntime = ((FloatParameter) parameterWhoseValueChanged).getFloatValue();
		}
	}

	/**
	 * Used to group clicks that should be processed by plugins as a group rather than as they arise. This is not
	 * a very useful feature anymore.
	 */
	private transient volatile int clickGroup = 1;

	@Override
	public int getClickGroup(MouseEvent e) {

		if ((currentModifier != PluginIOCells.MERGE_MODIFIER)) {
			return 0;
		} else
			return clickGroup;
	}

	public String[] getLabels() {
		return annotationNames.getSelectionString();
	}

	public boolean labelExistingCell() {
		return editExistingCells.isSelected();
	}

	public Color getDisplayColor() {
		Color color = Utils.colorFromString(seedColorParam.getStringValue());
		return color;
	}

	@Override
	public void publicProcessKeyEvent(KeyEvent e) {
		char c = e.getKeyChar();
		if (c == '�') {
			buttonGroup.setSelected(newButton.getModel(), true);
			currentModifier = PluginIOCells.ADD_MODIFIER;
		} else if (c == '�') {
			buttonGroup.setSelected(browseButton.getModel(), true);
			currentModifier = -1;
		}
		if (c == '�') {
			buttonGroup.setSelected(deleteButton.getModel(), true);
			currentModifier = PluginIOCells.DELETE_MODIFIER;
		} else {
			try {
				String s = new StringBuilder().append(c).toString();
				int i = Integer.parseInt(s);
				if (i > 0 && i <= annotationNames.getElements().length) {
					annotationNames.setSelection(new int[] { i - 1 });
					annotationNames.fireValueChanged(false, true, true);
				}
			} catch (NumberFormatException e1) {
				// Do nothing: key pressed was not a number
			}
		}
	}

	public float getLineThickness() {
		return lineThickness.getintValue();
	}

	public int getDepthOfField() {
		return depthOfField.getintValue();
	}

	public float getHoverDelay() {
		return hoverDelay.getFloatValue();
	}

}
