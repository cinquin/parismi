/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.RowFilter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.collections.primitives.ArrayFloatList;
import org.apache.commons.collections.primitives.ArrayIntList;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.GUI_utils.bean_table.RowTableModel;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameter_cell_views.FloatRangeSlider;
import pipeline.parameter_cell_views.IntRangeSlider;
import pipeline.parameter_cell_views.TextBox;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.IntRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SpreadsheetCell;
import pipeline.parameters.TextParameter;

/**
 * JTable that displays controls to filter each column based on a range of integers or floats, or based on String
 * contents. The type
 * of each column is automatically detected by examining the first row of data. This class assumes that the underlying
 * model extends {@link RowTableModel}.
 *
 */
public class JXTablePerColumnFiltering extends JXTable {

	private RowFilter<Object, Object> filter = new RowFilter<Object, Object>() {
		@Override
		public boolean include(Entry<?, ?> entry) {
			// Utils.log("Row filter called",LogLevel.DEBUG);
			if (filteringModel.getColumnCount() == 0)
				return true;
			try {
				for (int i = entry.getValueCount() - 1; i >= 0; i--) {
					if (filteringModel.getValueAt(0, i) == null)
						continue;
					Object v = entry.getValue(i);
					if (v instanceof String) {
						if (!(filteringModel.getValueAt(0, i) instanceof TextParameter))
							continue;
						if ((!"".equals(((TextParameter) filteringModel.getValueAt(0, i)).getStringValue()))
								&& !v.equals(filteringModel.getValueAt(0, i)))
							return false;
					} else if (v instanceof Float || v instanceof Double) {
						float f = ((Number) v).floatValue();
						float[] range = (float[]) ((FloatRangeParameter) filteringModel.getValueAt(0, i)).getValue();
						if (f < range[0]) {
							// Utils.log("Excluding based on column "+i,LogLevel.VERBOSE_DEBUG);
							return false;
						}
						if (f > range[1]) {
							// Utils.log("Excluding based on column  "+i,LogLevel.VERBOSE_DEBUG);
							return false;
						}
					} else if (v instanceof SpreadsheetCell) {
						float f = ((SpreadsheetCell) v).getFloatValue();
						float[] range = (float[]) ((FloatRangeParameter) filteringModel.getValueAt(0, i)).getValue();
						if (f < range[0]) {
							// Utils.log("Excluding based on column  "+i,LogLevel.VERBOSE_DEBUG);
							return false;
						}
						if (f > range[1]) {
							// Utils.log("Excluding based on column  "+i,LogLevel.VERBOSE_DEBUG);
							return false;
						}
					} else if (v instanceof Integer) {
						int f = (Integer) v;
						int[] range = (int[]) ((IntRangeParameter) filteringModel.getValueAt(0, i)).getValue();
						if (f < range[0]) {
							// Utils.log("Excluding based on column  "+i,LogLevel.VERBOSE_DEBUG);
							return false;
						}
						if (f > range[1]) {
							// Utils.log("Excluding based on column  "+i,LogLevel.VERBOSE_DEBUG);
							return false;
						}
					}
				}
			} catch (Exception e) {
				Utils.printStack(e);
				return true;// If something went wrong during evaluation, do not hide the row
			}
			return true;
		}
	};

	String getRowAsString(int index) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < this.getColumnCount(); i++) {
			Object value = getValueAt(index, i);
			if (Utils.skipListsInTableTextExport
					&& (value instanceof List || value instanceof ArrayFloatList || value instanceof ArrayIntList))
				b.append("skipped_list");
			else
				b.append(value + "");
			if (i < getColumnCount() - 1)
				b.append("\t");
		}
		return b.toString();
	}

	public int getNumberFilteredRows() {
		int nFiltered = 0;
		for (int i = 0; i < model.getRowCount(); i++) {
			int rowIndex = JXTablePerColumnFiltering.this.convertRowIndexToView(i);
			if (rowIndex == -1) {
				nFiltered++;
			}
		}
		return nFiltered;
	}

	/**
	 * 
	 * @param columnsFormulasToPrint
	 *            If null, print all columns
	 * @param stripNewLinesInCells
	 * @param filePath
	 *            Full path to save file to; if null, user will be prompted.
	 */
	public void saveFilteredRowsToFile(List<Integer> columnsFormulasToPrint, boolean stripNewLinesInCells,
			String filePath) {
		String saveTo;
		if (filePath == null) {
			FileDialog dialog = new FileDialog(new Frame(), "Save filtered rows to", FileDialog.SAVE);
			dialog.setVisible(true);
			saveTo = dialog.getDirectory();
			if (saveTo == null)
				return;

			saveTo += "/" + dialog.getFile();
		} else {
			saveTo = filePath;
		}

		PrintWriter out = null;
		try {
			out = new PrintWriter(saveTo);

			StringBuffer b = new StringBuffer();
			if (columnsFormulasToPrint == null) {
				for (int i = 0; i < this.getColumnCount(); i++) {
					b.append(getColumnName(i));
					if (i < getColumnCount() - 1)
						b.append("\t");
				}
			} else {
				int index = 0;
				for (int i : columnsFormulasToPrint) {
					b.append(getColumnName(i));
					if (index + 1 < columnsFormulasToPrint.size())
						b.append("\t");
					index++;
				}
			}
			out.println(b);

			for (int i = 0; i < model.getRowCount(); i++) {
				int rowIndex = convertRowIndexToView(i);
				if (rowIndex > -1) {
					if (columnsFormulasToPrint == null)
						if (stripNewLinesInCells)
							out.println(getRowAsString(rowIndex).replaceAll("\n", " "));
						else
							out.println(getRowAsString(rowIndex));
					else {
						boolean first = true;
						for (int column : columnsFormulasToPrint) {
							if (!first) {
								out.print("\t");
							}
							first = false;
							SpreadsheetCell cell = (SpreadsheetCell) getValueAt(rowIndex, column);
							if (cell != null) {
								String formula = cell.getFormula();
								if (formula != null) {
									if (stripNewLinesInCells)
										out.print(formula.replaceAll("\n", " "));
									else
										out.print(formula);
								}
							}
						}
						out.println("");
					}
				}
			}

			out.close();
		} catch (FileNotFoundException e) {
			Utils.printStack(e);
			Utils.displayMessage("Could not save table", true, LogLevel.ERROR);
		}
	}

	private static final long serialVersionUID = 1L;

	JXTable filteringTable, tableForColumnDescriptions;
	private RowTableModel<?> model;
	private TableModel filteringModel;
	private int nColumns;

	private class filterListener implements ParameterListener {
		int columnIndex = -1;

		public filterListener(int index) {
			columnIndex = index;
		}

		@Override
		public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
				boolean keepQuiet) {
			// .log("Update filter for column "+parameterWhoseValueChanged.creatorReference,LogLevel.DEBUG);
			// sorterChanged(new RowSorterEvent(getRowSorter()));
			// invalidate();
			if (silenceUpdates.get() == 0) {
				model.fireTableChanged(new PipelineTableModelEvent(model, PipelineTableModelEvent.FILTER_ADJUSTING));
			}
			// if (needToInitializeFilterModel) initializeFilterModel();
			// Utils.log(""+model.getRowCount(),LogLevel.DEBUG);
		}

		@Override
		public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
			switch (commandName) {
				case "Reset Range":
					updateRangeOfColumn(columnIndex, true, BOTH_BOUNDS, true);
					break;
				case "Reset Min":
					updateRangeOfColumn(columnIndex, true, LOWER_BOUND, true);
					break;
				case "Reset Max":
					updateRangeOfColumn(columnIndex, true, UPPER_BOUND, true);
					break;
				default:
					throw new IllegalStateException("Unknown command " + commandName);
			}

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

	public void resetFilterRanges(boolean notifyTableListeners) {
		silenceUpdates.incrementAndGet();
		for (int i = 0; i < getColumnCount(); i++) {
			updateRangeOfColumn(i, true, 0, true);
		}
		silenceUpdates.decrementAndGet();
		if (notifyTableListeners)
			model.fireTableDataChanged();
	}

	public static final int BOTH_BOUNDS = 0, LOWER_BOUND = -1, UPPER_BOUND = 1;

	public void updateRangeOfColumn(int columnIndex, boolean reinitializeSelection, int boundsToUpdate,
			boolean suppressModelInit) {
		if (!suppressModelInit) {
			needToInitializeFilterModel = true;
			initializeFilterModel();
		}
		boolean isFloat = model.getValueAt(0, columnIndex) instanceof Float;
		boolean isDouble = model.getValueAt(0, columnIndex) instanceof Double;
		boolean isInteger = model.getValueAt(0, columnIndex) instanceof Integer;
		boolean isSpreadsheetCell = model.getValueAt(0, columnIndex) instanceof SpreadsheetCell;
		if (!(isFloat || isInteger || isSpreadsheetCell || isDouble))
			return;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;

		double[] valuesForHistogram = new double[model.getRowCount()];

		for (int i = 0; i < model.getRowCount(); i++) {
			double value;
			if (isFloat)
				value = (Float) model.getValueAt(i, columnIndex);
			else if (isDouble)
				value = (Double) model.getValueAt(i, columnIndex);
			else if (isInteger)
				value = (Integer) model.getValueAt(i, columnIndex);
			else {
				value = ((SpreadsheetCell) model.getValueAt(i, columnIndex)).getFloatValue();
			}
			if (Double.isNaN(value))
				value = 0.0d;
			if (value < min)
				min = value;
			if (value > max)
				max = value;
			valuesForHistogram[i] = value;
		}

		// Now compute a histogram; this could be optimized

		HistogramDataset dataset = new HistogramDataset();
		dataset.setType(HistogramType.RELATIVE_FREQUENCY);
		dataset.addSeries("Histogram", valuesForHistogram, 15);

		if (isFloat || isDouble || isSpreadsheetCell) {
			FloatRangeParameter param = (FloatRangeParameter) filteringModel.getValueAt(0, columnIndex);
			param.histogram = dataset;
			float[] currentValue = (float[]) param.getValue();
			if ((boundsToUpdate == BOTH_BOUNDS) || boundsToUpdate == LOWER_BOUND)
				currentValue[2] = (float) min;
			if ((boundsToUpdate == BOTH_BOUNDS) || boundsToUpdate == UPPER_BOUND)
				currentValue[3] = (float) max;
			if (reinitializeSelection) {
				currentValue[0] = currentValue[2];
				currentValue[1] = currentValue[3];
			}
			param.setValueFireIfAppropriate(currentValue, false, true, true);
		} else {
			IntRangeParameter param = (IntRangeParameter) filteringModel.getValueAt(0, columnIndex);
			int[] currentValue = (int[]) param.getValue();
			if ((boundsToUpdate == BOTH_BOUNDS) || boundsToUpdate == LOWER_BOUND)
				currentValue[2] = (int) min;
			if ((boundsToUpdate == BOTH_BOUNDS) || boundsToUpdate == UPPER_BOUND)
				currentValue[3] = (int) max;
			if (reinitializeSelection) {
				currentValue[0] = currentValue[2];
				currentValue[1] = currentValue[3];
			}
			param.setValueFireIfAppropriate(currentValue, false, true, true);
		}
	}

	transient public boolean needToInitializeFilterModel = true;

	public void initializeFilterModel() {
		if (!needToInitializeFilterModel)
			return;
		if (model.getRowCount() > 0) {
			needToInitializeFilterModel = false;
			updateFilteringTable();
			for (int i = 0; i < model.getColumnCount(); i++) {
				if ((model.getValueAt(0, i) instanceof Float) || (model.getValueAt(0, i) instanceof SpreadsheetCell) ||
						(model.getValueAt(0, i) instanceof Double)) {
					filteringModel.setValueAt(new FloatRangeParameter("", "", 0.0f, 0.0f, 0.0f, 0.0f, true, true,
							new filterListener(i), i), 0, i);
				} else if (model.getValueAt(0, i) instanceof Integer)
					filteringModel.setValueAt(new IntRangeParameter("", "", 0, 0, 0, 0, true, true, new filterListener(
							i), i), 0, i);
				else if (model.getValueAt(0, i) instanceof String)
					filteringModel.setValueAt(new TextParameter("", "", "", true, new filterListener(i), i), 0, i);
				else if (model.getValueAt(0, i) == null) {
					// Ignore the column since we can't determine the object type
				} else
					Utils.log("Unsupported object type at column " + i, LogLevel.DEBUG);// +": "+model.getValueAt(0, i)
				updateRangeOfColumn(i, true, 0, true);
			}
		} else
			needToInitializeFilterModel = true;
	}

	transient AtomicInteger silenceUpdates = new AtomicInteger(0);
	
	private ColumnHeaderToolTips tips;

	@Override
	public void setModel(TableModel newModel) {
		this.model = (RowTableModel<?>) newModel;
		super.setModel(newModel);
		updateFilteringTable();
		this.setRowFilter(filter);
	}

	private void updateFilteringTable() {
		nColumns = model.getColumnCount();
		filteringModel = new DefaultTableModel(1, nColumns);
		if (filteringTable != null) {
			filteringTable.setModel(filteringModel);
			for (int i = 0; i < nColumns; i++) {
				MultiRenderer multiRenderer = getMultiRenderer();
				filteringTable.getColumn(i).setCellEditor(multiRenderer);
				filteringTable.getColumn(i).setCellRenderer(multiRenderer);
			}
		}
	}
	
	private static void updateColumn(TableColumnExt template, TableColumnExt toUpdate) {
		if (template == null || toUpdate == null) {
			return;
		}
		if (toUpdate.getModelIndex() != template.getModelIndex()) {
			toUpdate.setModelIndex(template.getModelIndex());
		}
		if (toUpdate.getWidth() != template.getWidth()) {
			toUpdate.setWidth(template.getWidth());
			toUpdate.setPreferredWidth(template.getPreferredWidth());
			toUpdate.setMaxWidth(template.getWidth());
			toUpdate.setMinWidth(template.getWidth());
		}
		if (toUpdate.isVisible() != template.isVisible()) {
			toUpdate.setVisible(template.isVisible());
		}

	}
	
	void updateFilteringTableSetup() {
		if (filteringTable == null) {
			return;
		}
		int nColumns = getColumns(true).size();
		if (nColumns != filteringTable.getColumns(true).size()) {
			updateFilteringTable();
		}
		final boolean updateDesc = tableForColumnDescriptions != null &&
			tableForColumnDescriptions.getColumns().size() == nColumns;
		for (int i = 0; i < nColumns; i++) {
			TableColumnExt filteringColumn = (TableColumnExt) filteringTable.getColumns(true).get(i);
			TableColumnExt dataColumn = (TableColumnExt) getColumns(true).get(i);
			updateColumn(dataColumn, filteringColumn);

			if (updateDesc) {
				TableColumnExt descColumn = (TableColumnExt) tableForColumnDescriptions.getColumns(true).get(i);
				updateColumn(dataColumn, descColumn);
			}
		}
	}
	
	@Override
	public void columnMoved(TableColumnModelEvent e) {
		super.columnMoved(e);
		if (e.getFromIndex() == e.getToIndex()) {
			return;
		}
		try {
			filteringTable.editingCanceled(null);
			if (e.getFromIndex() != -1 && e.getFromIndex() < getColumns(true).size()) {
				filteringTable.moveColumn(e.getFromIndex(), e.getToIndex());
				tableForColumnDescriptions.moveColumn(e.getFromIndex(), e.getToIndex());
			}
		} catch (ArrayIndexOutOfBoundsException ex) {
			//Changes in column counts apparently occur when hiding/unhiding columns,
			//which generate column move events
			Utils.printStack(ex, LogLevel.DEBUG);
		}
		updateFilteringTableSetup();
	}
	
	@Override
	public void columnPropertyChange(PropertyChangeEvent event) {
		super.columnPropertyChange(event);
		updateFilteringTableSetup();
	}

	@Override
	public void columnMarginChanged(ChangeEvent e) {
		super.columnMarginChanged(e);
		updateFilteringTableSetup();
	}
	
	private static MultiRenderer getMultiRenderer() {
		MultiRenderer multiRenderer = new MultiRenderer();

		FloatRangeSlider myFloatSliderRange = new FloatRangeSlider();
		FloatRangeSlider myFloatSliderRange2 = new FloatRangeSlider();
		IntRangeSlider myIntSliderRange = new IntRangeSlider();
		IntRangeSlider myIntSliderRange2 = new IntRangeSlider();
		TextBox textBox = new TextBox();
		TextBox textBox2 = new TextBox();

		multiRenderer.registerRenderer(IntRangeParameter.class, myIntSliderRange);
		multiRenderer.registerRenderer(FloatRangeParameter.class, myFloatSliderRange);
		multiRenderer.registerRenderer(TextParameter.class, textBox);

		multiRenderer.registerEditor(FloatRangeParameter.class, myFloatSliderRange2);
		multiRenderer.registerEditor(IntRangeParameter.class, myIntSliderRange2);
		multiRenderer.registerEditor(TextParameter.class, textBox2);

		return multiRenderer;
	}
	
	public JXTablePerColumnFiltering(TableModel model) {
		super(model);
		this.model = (BeanTableModel<?>) model;

		// Create the 1-row filtering Table

		nColumns = model.getColumnCount();

		// DependencyEngine e = new DependencyEngine(new BasicEngineProvider());

		for (int row = 0; row < model.getRowCount(); row++) {
			for (int i = 0; i < nColumns; i++) {
				if (getColumnName(i).contains("userCell")) {
					// this is a column with cells that can contain formulas in addition to computed values
				} else {

				}
			}
		}

		filteringModel = new DefaultTableModel(1, nColumns);

		initializeFilterModel();

		filteringTable = new JXTableBetterFocus(filteringModel);
		filteringTable.setTableHeader(null);

		for (int i = 0; i < nColumns; i++) {
			TableColumn fColumn = filteringTable.getColumn(i);
			MultiRenderer multiRenderer = getMultiRenderer();
			
			fColumn.setCellRenderer(multiRenderer);
			fColumn.setCellEditor(multiRenderer);
			fColumn.setWidth(getColumn(i).getWidth());
		}

		this.setRowFilter(filter);

		JTableHeader header = this.getTableHeader();
		if (tips == null) {
			tips = new ColumnHeaderToolTips();
		}
	    header.addMouseMotionListener(tips);
	}
	
    @Override
    //Overridden to work around problem with editor returning null value and
    //to allow parsing to right object type
	public void editingStopped(ChangeEvent e) {
        // Take in the new value
    	int editingRow = this.editingRow;
    	int editingColumn = this.editingColumn;
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
        	if (editingRow > -1 && editingColumn > -1) {
        		Object value = editor.getCellEditorValue();
        		if (getValueAt(editingRow, editingColumn) instanceof Float && value != null) {
        			try {
        				value = Float.parseFloat((String) value);
        				setValueAt(value, editingRow, editingColumn);
        			} catch (NumberFormatException ex) {
        				Utils.log("Could not parse " + value + " to float", LogLevel.WARNING);
        			}
        		} else if (getValueAt(editingRow, editingColumn) instanceof Double && value != null) {
        			try {
        				value = Double.parseDouble((String) value);
        				setValueAt(value, editingRow, editingColumn);
        			} catch (NumberFormatException ex) {
        				Utils.log("Could not parse " + value + " to float", LogLevel.WARNING);
        			}
        		} else if (getValueAt(editingRow, editingColumn) instanceof Integer && value != null) {
        			try {
        				value = Integer.parseInt((String) value);
        				setValueAt(value, editingRow, editingColumn);
        			} catch (NumberFormatException ex) {
        				Utils.log("Could not parse " + value + " to int", LogLevel.WARNING);
        			}
        		} else if (value != null) {
        			setValueAt(value, editingRow, editingColumn);
        		}
        		//It might make more sense for update event to be fired by setValueAt,
        		//but BeanTableModel does not currently do that
        		model.fireTableCellUpdated(editingRow, editingColumn);
        	}
            removeEditor();
        }
    }
    
    @Override
    //Overridden to prevent cell editing from starting just because the
    //user pressed the command key (which is necessary for discontiguous
    //cell selection)
	public boolean editCellAt(int row, int column, EventObject e) {
    	if (e instanceof KeyEvent) {
    		KeyEvent ke = (KeyEvent) e;
    		if ((ke.getKeyCode() == 0 || ke.getKeyCode() == 157) &&
    				(ke.getModifiers() == 260 || ke.getModifiers() == 4)) {
    			return false;
    		}
    	}
   		return super.editCellAt(row, column, e);
    }
    
	@Override
	//Adapted from http://stackoverflow.com/questions/27102546/show-tooltips-in-jtable-only-when-column-is-cut-off
	public String getToolTipText(MouseEvent e) {
		Point p = e.getPoint();
		int col = columnAtPoint(p);
		int row = rowAtPoint(p);
		if (row == -1 || col == -1) {
			return null;
		}
		Rectangle bounds = getCellRect(row, col, false);
		Object value = getValueAt(row, col);
		Component comp = prepareRenderer(getCellRenderer(row, col), row, col);
		if (comp.getPreferredSize().width > bounds.width) {
		    return(value.toString());
		} else {
			return null;
		}
	}

}
