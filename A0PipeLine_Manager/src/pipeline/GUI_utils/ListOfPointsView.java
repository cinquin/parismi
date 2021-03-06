/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.apache.commons.collections.primitives.ArrayFloatList;
import org.apache.commons.collections.primitives.FloatIterator;
import org.boris.expr.BasicEngineProvider;
import org.boris.expr.ExprException;
import org.boris.expr.ExprInteger;
import org.boris.expr.ExprString;
import org.boris.expr.engine.DependencyEngine;
import org.boris.expr.engine.GridReference;
import org.boris.expr.engine.Range;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jdesktop.swingx.table.TableColumnExt;

import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.GUI_utils.bean_table.BeanTableModel.ColumnInformation;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOListMember;
import pipeline.data.PluginIOListener;
import pipeline.database.ListDataGroup;
import pipeline.misc_util.Expr4jAdditions;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.WindowListenerWeakRef;
import pipeline.parameter_cell_views.ButtonForListDisplay;
import pipeline.parameter_cell_views.SpreadsheetCellView;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SpreadsheetCell;

/**
 * Used to view segmented cells and their quantified properties as a dynamic table that can generate scatter plots and
 * histograms.
 * The displayed PluginIO subclass is U.
 *
 */
public class ListOfPointsView<T extends IPluginIOListMember<T>> extends PluginIOView implements ActionListener,
		TableModelListener, PluginIOListener, ParameterListener {

	private BeanTableModel<T> tableModel;

	private JXTablePerColumnFiltering table;

	private IPluginIOList<T> points;

	private DependencyEngine spreadsheetEngine;

	public ListOfPointsView(IPluginIOList<T> points) {
		setData(points);
		if (points != null)
			points.addView(this);
	}

	private transient AtomicInteger silenceUpdates = new AtomicInteger(0);

	private transient Object modelSemaphore = new Object();

	private void setupTableModel(List<T> pointList) {
		tableModel = points.getBeanTableModel();
		try {
			Runnable r = () -> {
				synchronized (modelSemaphore) {
					tableModel.removeTableModelListener(ListOfPointsView.this);
					tableModel.addTableModelListener(ListOfPointsView.this);
					if (table != null) {
						table.silenceUpdates.incrementAndGet();
						table.setModel(tableModel);
						table.needToInitializeFilterModel = true;
						table.initializeFilterModel();
						setSpreadsheetColumnEditorAndRenderer();
						modelForColumnDescriptions = new dataModelAllEditable(1, tableModel.getColumnCount());
						@SuppressWarnings("unchecked")
						Vector<String> rowVector0 = (Vector<String>) modelForColumnDescriptions.getDataVector().get(0);
						for (int j = 0; j < tableModel.getColumnCount(); j++) {
							// FIXME This ignores the names the user may have set
							rowVector0.setElementAt(tableModel.getColumnName(j), j);
						}
						modelForColumnDescriptions.fireTableDataChanged();
						table.silenceUpdates.decrementAndGet();
						((AbstractTableModel) table.getModel()).fireTableStructureChanged();
					}
				}
			};
			if (SwingUtilities.isEventDispatchThread())
				r.run();
			else
				SwingUtilities.invokeAndWait(r);
		} catch (InvocationTargetException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setData(IPluginIO points) {
		this.points = (IPluginIOList<T>) points;
		if (points != null) {
			points.addListener(this);
			if (frame != null)
				frame.setTitle(points.getName());
		}
	}

	private volatile boolean closed = false;

	@Override
	public void close() {
		spreadsheetEngine = null;
		tableModel = null;
		table = null;
		coloringComboBox = null;
		realTimeUpdateCheckbox = null;
		if (frame != null && frame.isVisible())
			frame.setVisible(false);
		if (frame != null)
			frame.dispose();
		closed = true;
		synchronized (dirty) {
			dirty.notifyAll();
		}
		frame = null;
		g = null;
		KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
	}

	private void updateImage() {
		Utils.log("xx Updating image with " + getSelectedCells().size(), LogLevel.DEBUG);
		points.firePluginIOViewEvent(this, false);
	}

	private void saveToFile() {
		table.saveFilteredRowsToFile(null, true, null);
	}

	public IPluginIOList<T> getSelectedCells() {
		List<@Nullable T> selectedPoints = new ArrayList<>(points.size());
		Utils.log("Filtering starting from " + tableModel.getRowCount() + "rows", LogLevel.DEBUG);
		for (int i = 0; i < tableModel.getRowCount(); i++) {
			int rowIndex = table.convertRowIndexToView(i);
			if (rowIndex > -1) {
				selectedPoints.add(tableModel.getRow(i));
				// Utils.log("Added point with x "+tableModel.getRow(i).x,LogLevel.VERBOSE_VERBOSE_DEBUG);
			} else {
				// Utils.log("Filtering out row "+i,LogLevel.VERBOSE_DEBUG);
			}
		}

		@SuppressWarnings("unchecked")
		IPluginIOList<T> result = (IPluginIOList<T>) points.duplicateStructure();
		result.addAllAndLink(selectedPoints);

		// if (result.getHeight()==0) throw new RuntimeException("0 image height in ListOfPointsView");

		return result;
	}

	private void setSpreadsheetColumnEditorAndRenderer() {
		MultiRenderer multiRenderer = new MultiRenderer();
		multiRenderer.singleClickToEdit = false;
		MultiRenderer multiRenderer2 = new MultiRenderer();
		multiRenderer2.singleClickToEdit = false;

		multiRenderer.registerRenderer(ListDataGroup.class, new ButtonForListDisplay());
		multiRenderer.registerEditor(ListDataGroup.class, new ButtonForListDisplay());

		multiRenderer2.registerRenderer(ListDataGroup.class, new ButtonForListDisplay());
		multiRenderer2.registerEditor(ListDataGroup.class, new ButtonForListDisplay());

		for (TableColumn column: table.getColumns(true)) {
			TableColumnExt castColumn = (TableColumnExt) column;
			if (castColumn.getTitle().contains("userCell")) {
				castColumn.setCellEditor(new SpreadsheetCellView());
				castColumn.setCellRenderer(new SpreadsheetCellView());
			} else {
				castColumn.setCellEditor(multiRenderer);
				castColumn.setCellRenderer(multiRenderer2);
			}
		}
	}

	private class dataModelAllEditable extends DefaultTableModel {
		private static final long serialVersionUID = 1L;

		public dataModelAllEditable(int i, int j) {
			super(i, j);
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setValueAt(Object o, int row, int column) {
			Object oldValue = getValueAt(row, column);
			if (o != null && o.equals(oldValue))
				return;
			super.setValueAt(o, row, column);
			try {
				tableModel.setColumnName(column, (String) o);
				if (silenceUpdates.get() == 0) {
					tableModel.fireTableStructureChanged();
					setSpreadsheetColumnEditorAndRenderer();
				}
				@SuppressWarnings("rawtypes")
				DefaultComboBoxModel comboBoxModel =
						coloringComboBox == null ? null : (DefaultComboBoxModel) coloringComboBox.getModel();
				if (comboBoxModel != null) {
					Object selection = comboBoxModel.getSelectedItem();
					comboBoxModel.removeAllElements();
					for (int i = 0; i < tableModel.getColumnCount(); i++) {
						comboBoxModel.addElement(tableModel.getColumnName(i));
					}
					comboBoxModel.setSelectedItem(selection);
				}
			} catch (Exception e) {
				Utils.printStack(e);
			}
			if (points.getUserCellDescriptions().contains(oldValue)) {
				points.setUserCellDescription(column, (String) o);
				points.setProtobuf(null);// if this is passed back to C plugin, this forces a recomputation
			}
		}
	}

	public String getFieldForColoring() {
		@SuppressWarnings("rawtypes")
		DefaultComboBoxModel comboBoxModel =
				coloringComboBox == null ? null : (DefaultComboBoxModel) coloringComboBox.getModel();
		if (comboBoxModel != null) {
			if (comboBoxModel.getSelectedItem() == null)
				return null;
			else
				return comboBoxModel.getSelectedItem().toString();
		} else
			return null;
	}

	@SuppressWarnings("unchecked")
	public void setFieldForColoring(@NonNull String fieldName) {
		@SuppressWarnings("rawtypes")
		DefaultComboBoxModel comboBoxModel = (DefaultComboBoxModel) coloringComboBox.getModel();
		for (int i = 0; i < comboBoxModel.getSize(); i++) {
			if (fieldName.equals(comboBoxModel.getElementAt(i))) {
				comboBoxModel.setSelectedItem(fieldName);
				return;
			}
		}
		comboBoxModel.addElement(fieldName);
		comboBoxModel.setSelectedItem(fieldName);
	}

	@SuppressWarnings("rawtypes")
	private JComboBox coloringComboBox;
	private JCheckBox realTimeUpdateCheckbox;
	private JFrame frame;

	private JXTable tableForColumnDescriptions;
	private DefaultTableModel modelForColumnDescriptions;

	/**
	 * Update name of user columns based on strings stored in the U this view is showing.
	 * Importantly, the user columns must come first in the table.
	 */
	private void updateColumnDescriptions() {
		int col = 0;
		@SuppressWarnings("unchecked")
		Vector<String> rowVector0 = (Vector<String>) modelForColumnDescriptions.getDataVector().get(0);
		List<String> descriptions = points.getUserCellDescriptions();
		if (descriptions == null)
			return;
		for (String desc : descriptions) {
			rowVector0.setElementAt(desc, col);
			col++;
		}
		SwingUtilities.invokeLater(modelForColumnDescriptions::fireTableDataChanged);
	}
	
	//Copied from JTable code
	private static JViewport getTableViewPort(JTable table) {
		Container parent = SwingUtilities.getUnwrappedParent(table);
		if (parent instanceof JViewport) {
			JViewport port = (JViewport) parent;
			Container gp = port.getParent();
			if (gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane) gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				JViewport viewPort = scrollPane.getViewport();
				if (viewPort == null ||
						SwingUtilities.getUnwrappedView(viewPort) != table) {
					return null;
				}
				return viewPort;
			}
		}
		return null;
	}

	private WindowListener listener;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void show() {
		if (frame != null)
			frame.toFront();
		if (table == null) {
			spreadsheetEngine = new DependencyEngine(new BasicEngineProvider());
			setupTableModel(points);
			silenceUpdates.incrementAndGet();
			table = new JXTablePerColumnFiltering(tableModel);

			table.setRolloverEnabled(true);
			// table.setDragEnabled(true);
			table.setFillsViewportHeight(false);
			table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			table.setShowGrid(true);
			table.setShowHorizontalLines(true);
			table.setColumnSelectionAllowed(true);
			table.setRowSelectionAllowed(true);
			table.setColumnControlVisible(true);
			table.setHighlighters(new Highlighter[] { HighlighterFactory.createAlternateStriping() });

			table.addPropertyChangeListener("horizontalScrollEnabled", new PropertyChangeListener() {

				JViewport viewPort, filteringViewPort, columnDescViewPort;
				int lastX;
				
				ChangeListener scrollListener = new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent e) {
						if (viewPort == null || filteringViewPort == null) {
							return;
						}
						Point position = viewPort.getViewPosition();
						if (position.x == lastX) {
							return;
						}
						filteringViewPort.setViewPosition(position);
						columnDescViewPort.setViewPosition(position);
						lastX = position.x;
					}
					
				};
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					if (viewPort != null) {
						viewPort.removeChangeListener(scrollListener);
					}
					if (evt.getNewValue().equals(true)) {
						viewPort = getTableViewPort(table);
						if (viewPort == null) {
							return;
						}
						table.filteringTable.setHorizontalScrollEnabled(true);
						table.tableForColumnDescriptions.setHorizontalScrollEnabled(true);
						table.updateFilteringTableSetup();
						filteringViewPort = getTableViewPort(table.filteringTable);
						columnDescViewPort = getTableViewPort(table.tableForColumnDescriptions);
						viewPort.addChangeListener(scrollListener);
						scrollListener.stateChanged(null);
					} else {
						table.filteringTable.setHorizontalScrollEnabled(false);
						table.tableForColumnDescriptions.setHorizontalScrollEnabled(false);
					}
				}
			});
			
			modelForColumnDescriptions = new dataModelAllEditable(1, tableModel.getColumnCount());
			Vector<String> rowVector0 = (Vector<String>) modelForColumnDescriptions.getDataVector().get(0);
			for (int j = 0; j < tableModel.getColumnCount(); j++) {
				rowVector0.setElementAt(tableModel.getColumnName(j), j);
			}
			
			boolean done;
			do {
				done = true;
				for (TableColumn i: table.getColumns(true)) {
					TableColumnExt iCast = (TableColumnExt) i;
					if (iCast.getTitle().equals("Class") ||
							iCast.getTitle().equals("c") || 
							iCast.getTitle().equals("t") || 
							iCast.getTitle().equals("clusterID") ||
							iCast.getTitle().equals("userCell 2") ||
							iCast.getTitle().equals("userCell 3")) {
						if (iCast.isVisible()) {
							iCast.setVisible(false);
							done = false;
							break;
						}
					}
				}
			} while (!done);
			
			SwingUtilities.invokeLater(modelForColumnDescriptions::fireTableDataChanged);

			JScrollPane scrollPane = new JScrollPane(table);
			scrollPane.setPreferredSize(new Dimension(2000, 2000));

			updateColumnDescriptions();
			silenceUpdates.decrementAndGet();

			setSpreadsheetColumnEditorAndRenderer();

			tableForColumnDescriptions = new JXTable(modelForColumnDescriptions);
			table.tableForColumnDescriptions = tableForColumnDescriptions;

			JScrollPane jScrollPaneForNames = new JScrollPane(tableForColumnDescriptions);
			jScrollPaneForNames.setHorizontalScrollBarPolicy(
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

			JPanel controlPanel = new JPanel();
			controlPanel.setLayout(new FlowLayout());

			JButton createScatterPlotButton = new JButton("Scatter plot from selected columns");
			controlPanel.add(createScatterPlotButton);
			createScatterPlotButton.setActionCommand("Scatter plot from selected columns");
			createScatterPlotButton.addActionListener(this);

			realTimeUpdateCheckbox = new JCheckBox("Update display in real time");
			controlPanel.add(realTimeUpdateCheckbox);
			realTimeUpdateCheckbox.setActionCommand("Update display in real time");
			realTimeUpdateCheckbox.addActionListener(this);

			JButton forceUpdate = new JButton("Force display update");
			controlPanel.add(forceUpdate);
			forceUpdate.setActionCommand("Force display update");
			forceUpdate.addActionListener(this);

			JButton extendFormula = new JButton("Extend formula to column");
			controlPanel.add(extendFormula);
			extendFormula.setActionCommand("Extend formula to column");
			extendFormula.addActionListener(this);

			JButton saveFormulas = new JButton("Save user formulas...");
			saveFormulas.addActionListener(this);
			saveFormulas.setActionCommand("Save user formulas");
			controlPanel.add(saveFormulas);

			JButton reloadFormulas = new JButton("Reload user formulas...");
			reloadFormulas.addActionListener(this);
			reloadFormulas.setActionCommand("Reload user formulas");
			controlPanel.add(reloadFormulas);

			controlPanel.add(new JLabel("Color with:"));
			coloringComboBox = new JComboBox();
			controlPanel.add(coloringComboBox);
			DefaultComboBoxModel comboBoxModel = (DefaultComboBoxModel) coloringComboBox.getModel();
			coloringComboBox.addActionListener(this);

			for (int i = 0; i < tableModel.getColumnCount(); i++) {
				comboBoxModel.addElement(tableModel.getColumnName(i));
			}

			JButton saveTableToFile = new JButton("Save table to file");
			controlPanel.add(saveTableToFile);
			saveTableToFile.setActionCommand("Save table to file");
			saveTableToFile.addActionListener(this);

			/*
			final JCheckBox useCalibration = new JCheckBox("Use calibration");
			useCalibration.addActionListener(e -> {
				if (points == null)
					return;
				boolean selected = useCalibration.isSelected();
				if (selected && !(points instanceof PluginIOCalibrable)) {
					Utils.displayMessage("Type " + points.getClass().getName() + " does not have calibration", true,
							LogLevel.ERROR);
					return;
				}
				PluginIOCalibrable calibrable = (PluginIOCalibrable) points;
				if (selected && (calibrable.getCalibration() == null)) {
					Utils.displayMessage("Calibration information is not present in the segmentation; one "
							+ "way of adding it is to give the source image (with calibration) as an input "
							+ "to the active contour plugin", true, LogLevel.ERROR);
					return;
				}
				float xyCalibration = selected ? ((float) calibrable.getCalibration().pixelWidth) : 0;
				float zCalibration = selected ? ((float) calibrable.getCalibration().pixelDepth) : 0;
				updateCalibration(xyCalibration, zCalibration);
			});
			PluginIOCalibrable calibrable = null;
			if (points instanceof PluginIOCalibrable)
				calibrable = (PluginIOCalibrable) points;
			boolean calibrationPresent = calibrable != null && calibrable.getCalibration() != null;
			useCalibration.setSelected(calibrationPresent);
			if (calibrationPresent) {
				updateCalibration((float) calibrable.getCalibration().pixelWidth,
						(float) calibrable.getCalibration().pixelDepth);
			}
			controlPanel.add(useCalibration);
			*/
			
			frame = new JFrame(points.getName());
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

			listener = new WindowListenerWeakRef(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					close();// So all references to data are nulled, to ensure garbage collection
				}

			});
			frame.addWindowListener(listener);

			frame.setLayout(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = GridBagConstraints.RELATIVE;
			c.weighty = 0.75;
			c.weightx = 1.0;
			c.gridwidth = 1;
			c.gridheight = 1;

			frame.add(scrollPane, c);

			c.weighty = 0.0;
			JScrollPane scrollPane2 = new JScrollPane(table.filteringTable);
			scrollPane2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane2.setMinimumSize(new Dimension(1, 250));
			frame.add(scrollPane2, c);

			c.weighty = 0.0;
			jScrollPaneForNames.setMinimumSize(new Dimension(1, 40));
			jScrollPaneForNames.setMaximumSize(new Dimension(9999999, 40));
			frame.add(jScrollPaneForNames, c);

			c.weighty = 0.0;
			c.fill = GridBagConstraints.HORIZONTAL;
			controlPanel.setMinimumSize(new Dimension(1, 80));
			frame.add(controlPanel, c);
			
			table.setHorizontalScrollEnabled(true);
			table.updateFilteringTableSetup();

			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int height = screenSize.height;
			int width = screenSize.width;
			frame.setSize((int) (0.67 * width), height / 2);
			frame.setLocation((int) (0.33 * width), height / 2);
			frame.setVisible(true);
		}

		if ((tableUpdateThread == null) || (!tableUpdateThread.isAlive())) {
			tableUpdateThread = new Thread(() -> {
				try {
					checkForDirtiness();
				} catch (Exception e) {
					Utils.log("Exception in ListOfPointsView GUI update thread", LogLevel.ERROR);
					Utils.printStack(e);
				}
			}, "ListOfPointsView GUI update thread");

			tableUpdateThread.start();
		}
	}

	private Thread tableUpdateThread;

	// This is currently not functional
	@SuppressWarnings("unused")
	private void updateCalibration(float xyCalibration, float zCalibration) {
		points.stream().filter(p -> p instanceof ClickedPoint).forEach(p -> {
			ClickedPoint cp = (ClickedPoint) p;
			cp.xyCalibration = xyCalibration;
			cp.zCalibration = zCalibration;
		});
		points.fireValueChanged(false, true);
	}

	private boolean realTimeUpdates;

	@Override
	@SuppressWarnings({ "rawtypes" })
	public void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals("Update display in real time")) {
			realTimeUpdates = realTimeUpdateCheckbox.isSelected();
		} else if (event.getActionCommand().equals("Force display update")) {
			updateImage();
		} else if (event.getActionCommand().equals("Save table to file")) {
			saveToFile();
		} else if (event.getActionCommand().equals("Scatter plot from selected columns")) {
			int[] selectedColumns = table.getSelectedColumns();
			String name1 = table.getColumnName(selectedColumns[0]);
			XYScatterPlotView scv = null;
			if (selectedColumns.length == 1) {
				// display a histogram rather than a scatter plot
				scv = new XYScatterPlotView(1);
				scv.setWindowTitle("Histogram of " + name1);
				
				final int columnModelIndex = table.convertColumnIndexToModel(selectedColumns[0]);

				int nFilteredRows = table.getNumberFilteredRows();
				if (nFilteredRows == 0) {
					// No row filtered, so create a plot that should be dynamically updated as
					// the underlying PluginIO is updated by the pipeline plugins
					ColumnInformation columnInfo = tableModel.columns.get(columnModelIndex);
					if (columnInfo.getReturnType().equals(ArrayFloatList.class)) {
						// Display histogram of values for the selected cell, NOT a histogram for
						// a single value for all cells in table
						ArrayFloatList values =
								(ArrayFloatList) table.getValueAt(table.getSelectedRow(), selectedColumns[0]);
						XYSeriesE series = new XYSeriesE(columnInfo.fieldName);
						FloatIterator it = values.iterator();
						while (it.hasNext()) {
							series.add(it.next(), 0);
						}
						scv.addSeries(name1, series);

					} else {
						if (columnInfo.indexInList > -1) {
							scv.addSeries(name1, points.getJFreeChartXYSeries(columnInfo.fieldName, null,
									columnInfo.indexInList, -1, tableModel.columns.get(columnModelIndex).getName(),
									null));
						} else
							scv.addSeries(name1, points.getJFreeChartXYSeries(name1, null, -1, -1, tableModel.columns
									.get(columnModelIndex).getName(), null));
					}
				} else {
					// Some rows are filtered, so we do not want to display everything
					// TODO Ideally we would want to dynamically refilter when master list is changed by pipeline plugin
					List<@Nullable T> filteredPoints = new ArrayList<>(table.getRowCount());// -nFilteredRows
					for (int i = 0; i < tableModel.getRowCount(); i++) {
						int rowIndex = table.convertRowIndexToView(i);
						if (rowIndex > -1) {
							filteredPoints.add(tableModel.getRow(i));
						}
					}
					ColumnInformation columnInfo = tableModel.columns.get(columnModelIndex);
					@SuppressWarnings("unchecked")
					IPluginIOList<T> subList = (IPluginIOList<T>) points.duplicateStructure();
					subList.addAll(filteredPoints);
					if (columnInfo.indexInList > -1) {
						scv.addSeries(name1, subList.getJFreeChartXYSeries(columnInfo.fieldName, null,
								columnInfo.indexInList, -1, tableModel.columns.get(columnModelIndex).getName(), null));
					} else
						scv.addSeries(name1, subList.getJFreeChartXYSeries(name1, null, -1, -1, tableModel.columns.get(
								columnModelIndex).getName(), null));
				}
			} else { // display a scatter plot
				final int xColumnModelIndex = table.convertColumnIndexToModel(selectedColumns[0]);
				final int yColumnModelIndex = table.convertColumnIndexToModel(selectedColumns[1]);
				String name2 = table.getColumnName(selectedColumns[1]);
				// Utils.log("Creating pairwise plot for columns "+name1+" and "+name2,LogLevel.DEBUG);
				// For now just deal with the first two columns
				scv = new XYScatterPlotView(0);
				scv.setWindowTitle("Scatterplot: " + name2 + " vs " + name1);
				int nFilteredRows = table.getNumberFilteredRows();
				ColumnInformation xColumnInfo = tableModel.columns.get(xColumnModelIndex);
				ColumnInformation yColumnInfo = tableModel.columns.get(yColumnModelIndex);
				int xListIndex = xColumnInfo.indexInList;
				int yListIndex = yColumnInfo.indexInList;
				String xName = xListIndex > -1 ? xColumnInfo.fieldName : name1;
				String yName = yListIndex > -1 ? yColumnInfo.fieldName : name2;
				if (nFilteredRows == 0) {
					// No row filtered, so create a plot that should be dynamically updated as
					// the underlying PluginIO is updated by the pipeline plugins
					scv.addSeries(name1 + " and " + name2, points.getJFreeChartXYSeries(xName, yName, xListIndex, yListIndex,
							tableModel.columns.get(xColumnModelIndex).getName(),
							tableModel.columns.get(yColumnModelIndex).getName()));
				} else {
					// Some rows are filtered, so we do not want to display everything
					// TODO Ideally we would want to dynamically refilter when master list is changed by pipeline plugin
					List<@Nullable T> filteredPoints = new ArrayList<>(table.getRowCount());
					for (int i = 0; i < tableModel.getRowCount(); i++) {
						int rowIndex = table.convertRowIndexToView(i);
						if (rowIndex > -1) {
							filteredPoints.add(tableModel.getRow(i));
						}
					}
					@SuppressWarnings("unchecked")
					IPluginIOList<T> subList = (IPluginIOList<T>) points.duplicateStructure();
					subList.addAll(filteredPoints);
					scv.addSeries(name1 + " and " + name2, subList.getJFreeChartXYSeries(xName, yName, xListIndex, yListIndex,
							tableModel.columns.get(xColumnModelIndex).getName(),
							tableModel.columns.get(yColumnModelIndex).getName()));
				}
			}
			scv.show();

		} else if (event.getActionCommand().equals("Extend formula to column")) {
			int[] selectedColumns = table.getSelectedColumns();
			if ((selectedColumns.length < 1) || (!table.getColumnName(selectedColumns[0]).contains("userCell"))) {
				Utils.displayMessage("No user column selected; length is " + selectedColumns.length, true,
						LogLevel.ERROR);
				return;
			}
			int[] selectedRows = table.getSelectedRows();
			Utils.log("Extending formula in cell " + selectedRows[0] + ", " + selectedColumns[0], LogLevel.DEBUG);
			// String formula=((SpreadsheetCell) table.getValueAt(selectedRows[0], selectedColumns[0])).getFormula();
			silenceUpdates.incrementAndGet();// for performance reasons

			try {

				for (int i = 0; i < tableModel.getRowCount(); i++) {
					Expr4jAdditions.extendFormula(spreadsheetEngine, selectedRows[0] + 1, selectedColumns[0] + 1,
							i + 1, selectedColumns[0] + 1);
					String formula =
							spreadsheetEngine
									.getInput(new Range(null, new GridReference(selectedColumns[0] + 1, i + 1)));
					if (tableModel.getValueAt(i, selectedColumns[0]) == null) {
						SpreadsheetCell cell =
								new SpreadsheetCell("", "", new Object[] { 0.0, formula }, true, null, null);
						tableModel.setValueAt(cell, i, selectedColumns[0]);
					} else {

						SpreadsheetCell cell = (SpreadsheetCell) table.getValueAt(i, selectedColumns[0]);
						cell.setFormula(formula);
					}
				}
			} catch (Exception excp) {
				Utils.printStack(excp);
			} finally {
				silenceUpdates.decrementAndGet();
			}
			// the following is to get a histogram displayed for the column we've computed
			table.needToInitializeFilterModel = true;
			table.initializeFilterModel();
			table.updateRangeOfColumn(selectedColumns[0], true, 0, false);
			tableModel.fireTableDataChanged();
		} else if ("Save user formulas".equals(event.getActionCommand())) {
			List<Integer> userColumns = getUserColumnList();
			table.saveFilteredRowsToFile(userColumns, true, null);
		} else if ("Reload user formulas".equals(event.getActionCommand())) {
			reloadUserFormulasFromFile();
		} else if ("comboBoxEdited".equals(event.getActionCommand())
				|| ("comboBoxChanged".equals(event.getActionCommand()))) {
			Object colorSelection = coloringComboBox.getModel().getSelectedItem();
			if (colorSelection != null)
				points.firePluginIOViewEvent(this, false);
		}
	}

	private List<Integer> getUserColumnList() {
		List<Integer> result = new ArrayList<>();
		for (int j = 0; j < tableModel.getColumnCount(); j++) {
			if (tableModel.getColumnName(j).contains("userCell")) {
				result.add(j);
			}
		}
		return result;
	}

	private void reloadUserFormulasFromFile() {
		FileDialog dialog =
				new FileDialog(new Frame(), "Choose a tab-separated file to load formulas from.", FileDialog.LOAD);
		dialog.setVisible(true);
		String filePath = dialog.getDirectory();
		if (filePath == null)
			return;
		filePath += "/" + dialog.getFile();

		silenceUpdates.incrementAndGet();

		try (BufferedReader r = new BufferedReader(new FileReader(filePath))) {
			List<Integer> userColumns = getUserColumnList();
			int nUserColumns = userColumns.size();
			int row = 0;
			boolean firstLine = true;// the first line contains column names
			while (true) {
				String line = r.readLine();
				if (line == null)
					break;
				StringTokenizer stok = new java.util.StringTokenizer(line);

				int currentColumn = 0;
				while (stok.hasMoreTokens()) {
					String element = stok.nextToken("\t");
					if (firstLine) {
						// name columns
						tableModel.setColumnName(userColumns.get(currentColumn), element);
						modelForColumnDescriptions.setValueAt(element, 0, userColumns.get(currentColumn));
					} else {
						SpreadsheetCell cell =
								(SpreadsheetCell) tableModel.getValueAt(row, userColumns.get(currentColumn));
						if (cell == null) {
							cell = new SpreadsheetCell("", "", new Object[] { "", element }, true, this, null);
							tableModel.setValueAt(cell, row, userColumns.get(currentColumn));
						} else {
							cell.setFormula(element);
						}
					}
					currentColumn++;
					if (currentColumn == nUserColumns) {
						Utils.log("File has more columns than user columns; discarding remaining columns from file",
								LogLevel.WARNING);
						break;
					}
				}
				if (!firstLine)
					row++;
				else
					firstLine = false;
			}
		} catch (IOException e) {
			Utils.printStack(e);
		} finally {
			silenceUpdates.decrementAndGet();
		}

		tableModel.fireTableStructureChanged();
		setSpreadsheetColumnEditorAndRenderer();
	}

	private void updateComputedCells() {
		silenceUpdates.incrementAndGet();// to avoid infinite loops: we might be setting the value of some cells,
		// and are also called in response to cell values being changed
		try {
			synchronized (spreadsheetEngine) {
				for (int i = 0; i < tableModel.getRowCount(); i++) {
					for (int j = 0; j < tableModel.getColumnCount(); j++) {
						if (tableModel.getColumnName(j).contains("userCell")) {// columns.get(j).indexInList>-1
							Object eval = null;
							Range r = new Range(null, new GridReference(j + 1, i + 1));
							eval = spreadsheetEngine.getValue(r);
							try {
								if ((eval instanceof ExprString) && ((ExprString) eval).str.length() == 0) {

								} else if (eval != null) {
									float f = Float.valueOf(eval.toString());
									eval = new Float(f);
								}
							} catch (Exception e) {
								// raised if evaluation not parsed to float, i.e. if it is a string representing an
								// error
							}
							if (tableModel.getValueAt(i, j) == null) {
								SpreadsheetCell cell =
										new SpreadsheetCell("", "", new Object[] { eval, "" }, true, this, null);
								tableModel.setValueAt(cell, i, j);

							} else {
								Object value = tableModel.getValueAt(i, j);
								if (value instanceof SpreadsheetCell) {
									SpreadsheetCell cell = (SpreadsheetCell) tableModel.getValueAt(i, j);
									cell.setEvaluationResult(eval);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			Utils.printStack(e);
		} finally {
			silenceUpdates.decrementAndGet();
		}
	}

	private void updateExpr4jModel() {
		// spreadsheetEngine = new DependencyEngine(new BasicEngineProvider());
		if (spreadsheetEngine == null)// Could happen if window was closed just before this method was called
			return;
		if (tableModel.getRowCount() > 5000) {
			Utils.log("Not updating spreadsheet engine because number of rows is >5000", LogLevel.INFO);
			return;
		}
		synchronized (spreadsheetEngine) {
			spreadsheetEngine.setVariable("NCELLS", new ExprInteger(tableModel.getRowCount()));
			for (int i = 0; i < tableModel.getRowCount(); i++) {
				for (int j = tableModel.getColumnCount() - 1; j > -1; j--) {
					try {
						Object cell = tableModel.getValueAt(i, j);
						if (cell instanceof SpreadsheetCell) {
							// read the formula (if any) set in the SpreadSheetCell object
							String formula = ((SpreadsheetCell) cell).getFormula();
							Range r = new Range(null, new GridReference(j + 1, i + 1));
							// spreadsheetEngine.set(new String(columnAsLetter)+(i+1),formula);
							spreadsheetEngine.set(r, formula);
						} else {
							if (cell != null) {
								Range r = new Range(null, new GridReference(j + 1, i + 1));
								spreadsheetEngine.set(r, cell.toString());
							}
						}
					} catch (ExprException e) {
						Utils.printStack(e);
					}
				}
			}
		}
	}

	private GlassFrame g = new GlassFrame();

	private AtomicBoolean dirty = new AtomicBoolean(false);

	/*
	 * private Color darkGrey=new Color(0.3f, 0.3f, 0.3f);
	 * private Color lightGrey=new Color(0.6f, 0.6f, 0.6f);
	 */

	@SuppressWarnings("unchecked")
	private void checkForDirtiness() {
		while (!closed) {
			boolean assumeTableStructureChanged = false;
			boolean copyOfDirty = false;
			synchronized (dirty) {
				try {
					while (!dirty.get() && !closed)
						dirty.wait();
				} catch (InterruptedException e) {
					if (closed || frame == null)
						break;
				}
				copyOfDirty = dirty.get();
				dirty.set(false);
				assumeTableStructureChanged = tableStructurePossiblyChanged;
				tableStructurePossiblyChanged = false;
			}

			if (copyOfDirty && !(closed || frame == null)) {
				Component previousGlassPane = frame.getGlassPane();
				final Timer timer = new Timer(2_000, null);
				timer.setInitialDelay(4_000);
				final Action t = new AbstractAction() {
					private static final long serialVersionUID = 1L;
					private boolean high;
					private boolean firstRun = true;
					
					@Override
					public void actionPerformed(ActionEvent action) {
						if (frame == null || !frame.isVisible()) {
							// The user might have closed the window; just exit
							timer.stop();
							return;
						}
						if (firstRun) {
							firstRun = false;
							frame.setGlassPane(g);
							g.setBounds(table.getBounds());
							g.setVisible(true);
						}
						g.setAlpha(high ? 200 : 100);
						// table.setBackground(high?darkGrey:lightGrey);
						high = !high;
						g.repaint();
					}
				};

				timer.addActionListener(t);
				timer.start();

				IPluginIOList<T> localPointsCopy = null;

				boolean filterUpdating = false;

				synchronized (dataCopySemaphore) {
					localPointsCopy = (IPluginIOList<T>) pointsCopy.duplicateStructure();
					localPointsCopy.addAllAndLink(pointsCopy);
					if ((modelEvent != null) && (modelEvent.eventType == PipelineTableModelEvent.FILTER_ADJUSTING))
						filterUpdating = true;
				}

				silenceUpdates.incrementAndGet();

				try {

					if (assumeTableStructureChanged) {
						setupTableModel(localPointsCopy);
						updateColumnDescriptions();
					}

					updateExpr4jModel();
					// Now read the values computed by expr4j and update the result to display (but keep the formula),
					// only for user-defined columns
					updateComputedCells();
					if (!assumeTableStructureChanged) {
						// Reset filter range because if any new user values are generated the rows
						// might automatically be filtered out, which is very confusing for the user
						if (!filterUpdating)
							table.resetFilterRanges(false);
					}

					points.fireValueChanged(false, false);
					final boolean copyOfAssumeTableStructureChanged = assumeTableStructureChanged;
					SwingUtilities.invokeLater(() -> {
						timer.stop();
						g.setVisible(false);
						frame.setGlassPane(previousGlassPane);

						synchronized (modelSemaphore) {
							silenceUpdates.incrementAndGet();
							try {
								table.setBackground(Color.WHITE);
								if (copyOfAssumeTableStructureChanged) {
									// tableModel.fireTableStructureChanged();
									// Not necessary because already indirectly triggered above
								} else {
									final ListSelectionModel saveRowSelection, saveColumnSelection;
									try {
										saveRowSelection =  (ListSelectionModel)
												((DefaultListSelectionModel) table.getSelectionModel()).clone();
										saveColumnSelection =  (ListSelectionModel)
												((DefaultListSelectionModel) table.getColumnModel().getSelectionModel()).clone();

									} catch (Exception e) {
										throw new RuntimeException(e);
									}
									tableModel.fireTableDataChanged();
									Utils.log("Resetting selection", LogLevel.DEBUG);
									table.setSelectionModel(saveRowSelection);
									table.getColumnModel().setSelectionModel(saveColumnSelection);
								}
							} finally {
								silenceUpdates.decrementAndGet();
							}
							frame.repaint();// For glass pane
						}
					});
				} catch (Exception e) {
					Utils.log("Exception: " + e, LogLevel.WARNING);
					dirty.set(false);
				} finally {
					silenceUpdates.decrementAndGet();
				}

			}
		}
	}

	private PipelineTableModelEvent modelEvent = null;

	@SuppressWarnings("unchecked")
	@Override
	public void tableChanged(TableModelEvent e) {
		if ((silenceUpdates.get() == 0) && (realTimeUpdates))
			updateImage();
		if (silenceUpdates.get() == 0) {
			Utils.log("Recomputing table because of a change", LogLevel.DEBUG);

			synchronized (dataCopySemaphore) {
				pointsCopy = (IPluginIOList<T>) points.duplicateStructure();
				pointsCopy.addAllAndLink(points);
				if (e instanceof PipelineTableModelEvent) {
					modelEvent = (PipelineTableModelEvent) e;
				} else
					modelEvent = null;
			}
			synchronized (dirty) {
				dirty.set(true);
				tableStructurePossiblyChanged = !(e instanceof PipelineTableModelEvent || 
						e.getType() == TableModelEvent.UPDATE);
				if (tableStructurePossiblyChanged)
					Utils.log("Table structure change event", LogLevel.DEBUG);
				dirty.notifyAll();
			}
		}
	}

	private transient Object dataCopySemaphore = new Object();
	private transient IPluginIOList<T> pointsCopy;

	private transient volatile boolean tableStructurePossiblyChanged = false;

	@SuppressWarnings("unchecked")
	@Override
	public void pluginIOValueChanged(boolean stillChanging, IPluginIO pluginIOWhoseValueChanged) {
		if (silenceUpdates.get() > 0)
			return;
		if (table == null)
			return;// this view has not been activated yet
		if (points != pluginIOWhoseValueChanged)
			Utils.log("Different plugins", LogLevel.WARNING);

		synchronized (dataCopySemaphore) {
			pointsCopy = (IPluginIOList<T>) points.duplicateStructure();
			pointsCopy.addAllAndLink(points);
		}

		synchronized (dirty) {
			dirty.set(true);
			tableStructurePossiblyChanged = true;
			dirty.notifyAll();
		}
	}

	@Override
	public void pluginIOViewEvent(PluginIOView trigger, boolean stillChanging, AWTEvent event) {
		// Do nothing: we don't care about events generated by other views
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		// This is called when a SpreadsheetCell is modified (i.e. its formula is changed)

		// updateExpr4jModel();
		// updateComputedCells();
		tableModel.fireTableDataChanged();
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

	@Override
	public String getParameterName() {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void setParameterName(String name) {
		throw new RuntimeException("Unimplemented");
	}

}
