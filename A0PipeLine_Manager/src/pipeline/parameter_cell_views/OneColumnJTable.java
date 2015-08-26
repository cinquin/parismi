/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.TableParameter;

public class OneColumnJTable extends AbstractParameterCellView implements TableModelListener, ParameterListener {

	private static final long serialVersionUID = 1L;

	private JTable localJTable;
	private TableParameter currentParameter;
	private boolean silenceUpdate;

	private class localJTable extends JTable {

		private static final long serialVersionUID = 1L;

		public localJTable(AbstractTableModel a) {
			super(a);
			this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		}

		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
			Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
			return c;
		}
	}

	private class MyTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		public boolean transpose = false;

		@Override
		public int getColumnCount() {
			if (transpose)
				return data.length;
			else
				return 1;
		}

		@Override
		public int getRowCount() {
			if (transpose)
				return 1;
			else
				return data.length;
		}

		public Object[][] data = { { "bidule" }, { "truc" } };

		@Override
		public Object getValueAt(int row, int col) {
			if (transpose)
				return data[col][row];
			return data[row][col];
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return true;
		}

		@Override
		public void setValueAt(Object value, int row, int col) {

			if (currentParameter.getPostProcessor() != null)
				value = currentParameter.getPostProcessor().postProcess(value);

			if (value != null && currentParameter.isEnforceUniqueEntries()) {
				for (int r = 0; r < (transpose ? getColumnCount() : getRowCount()); r++) {
					if (r == (transpose ? col : row))
						continue;
					if (value.equals(transpose ? data[r][row] : data[r][col]))
						return;
				}
			}

			if (transpose)
				data[col][row] = value;
			else
				data[row][col] = value;
			String[] currentStrings = currentParameter.getElements();
			if (value == null)
				Utils.log("null value", LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);

			int realRow = transpose ? col : row;
			if (!currentStrings[realRow].equals(value)) {
				currentParameter.hasBeenEdited = true;
				Utils.log("setting value " + value, LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
				currentStrings[realRow] = (String) value;
				currentParameter.setValue(currentStrings);
				currentParameter.fireValueChanged(false, false, true);
			}
			fireTableCellUpdated(row, col);
		}

	}

	public OneColumnJTable() {
		super();
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 1;

		MyTableModel localTableModel = new MyTableModel();
		localJTable = new localJTable(localTableModel);
		localJTable.setFillsViewportHeight(true);
		localJTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		localJTable.getModel().addTableModelListener(this);
		localJTable.setRowSelectionAllowed(true);
		localJTable.setColumnSelectionAllowed(true);
		localJTable.addKeyListener(new TableListener());
		localJTable.addMouseListener(new TableListener());

		add(localJTable, c);

	}

	@SuppressWarnings("null")
	private void checkSelection() {
		if (!silenceUpdate) {
			if (currentParameter == null)
				return;

			int @NonNull[] newChoices;
			if (currentParameter.displayHorizontally)
				newChoices = localJTable.getSelectedColumns();
			else
				newChoices = localJTable.getSelectedRows();

			if (!Arrays.equals(currentParameter.getSelection(), newChoices)) {
				currentParameter.setSelection(newChoices);
				currentParameter.fireValueChanged(false, false, true);
			}
		}
	}

	private class TableListener implements KeyListener, MouseListener {
		@Override
		public void mousePressed(MouseEvent m) {
			checkSelection();
		}

		@Override
		public void keyPressed(KeyEvent e) {
		}

		@Override
		public void keyReleased(KeyEvent e) {
			checkSelection();
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			checkSelection();
		}

	}

	@Override
	public void tableChanged(TableModelEvent e) {
	}

	private boolean evenTableRow;

	@Override
	public void editingFinished() {
		if (localJTable.getCellEditor() != null)
			localJTable.getCellEditor().stopCellEditing();
		checkSelection();
	}

	private void updateDisplay() {
		String[] elements = null;
		if (currentParameter != null)
			elements = currentParameter.getElements();
		if (elements == null)
			elements = new String[0];
		Object[][] data = new Object[elements.length][1];
		for (int i = 0; i < elements.length; i++) {
			data[i][0] = elements[i];
		}
		MyTableModel localTableModel = (MyTableModel) localJTable.getModel();
		if (currentParameter != null)
			localTableModel.transpose = currentParameter.displayHorizontally;
		localTableModel.data = data;
		localTableModel.fireTableStructureChanged();
		localTableModel.fireTableDataChanged();

		int[] selection;
		if (currentParameter != null)
			selection = currentParameter.getSelection();
		else
			selection = new int[0];
		localJTable.getSelectionModel().clearSelection();

		if (currentParameter != null && selection.length > 0) {
			for (int element : selection) {
				if (element >= data.length) {
					Utils.log("Invalid selection index " + element, LogLevel.VERBOSE_VERBOSE_DEBUG);
				} else {
					if (currentParameter.displayHorizontally) {
						localJTable.getColumnModel().getSelectionModel().addSelectionInterval(element, element);
						localJTable.addRowSelectionInterval(0, 0);
					} else {
						localJTable.getSelectionModel().addSelectionInterval(element, element);
						localJTable.addColumnSelectionInterval(0, 0);
					}
				}
			}
		} else {
			// Null or empty selection
		}

	}

	@Override
	public Component getRendererOrEditorComponent(JTable table, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		// value should be a pair of object arrays: first is a set of choices (probably strings), and the second is the
		// set of selected items (int [])

		if (currentParameter != null)
			currentParameter.removeListener(this);

		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow)
			localJTable.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		else
			localJTable.setBackground(Utils.COLOR_FOR_ODD_ROWS);

		silenceUpdate = true;
		currentParameter = (TableParameter) value;
		currentParameter.addGUIListener(this);

		updateDisplay();

		int height_wanted = (int) getPreferredSize().getHeight();
		if (table != null && (height_wanted > table.getRowHeight(row)))
			table.setRowHeight(row, height_wanted);

		silenceUpdate = false;
		return this;
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		Utils.log("Parameter value changed in 1 column JTable", LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
		updateDisplay();
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

}
