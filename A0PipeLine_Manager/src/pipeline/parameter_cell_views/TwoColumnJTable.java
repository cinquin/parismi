/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.GUI_utils.ColumnHeaderToolTips;
import pipeline.GUI_utils.MultiRenderer;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.TwoColumnTableParameter;

// TODO Restore selection from parameter; see how OneColumnJTable table does it
public class TwoColumnJTable extends AbstractParameterCellView implements TableModelListener {
	private static final long serialVersionUID = 1L;

	private JTable localTable;
	private ColumnHeaderToolTips tips;
	private Object[] firstColumn;
	private Object[] secondColumn;

	private TwoColumnTableParameter currentParameter;

	@SuppressWarnings("unused")
	private boolean silenceUpdate;

	private class localJTable extends JTable {
		private static final long serialVersionUID = 1L;

		public localJTable(AbstractTableModel a) {
			super(a);
			// this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		}

		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
			Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
			if (c == null) {
				return null;
			}
			if (!isCellSelected(rowIndex, vColIndex)) {
				if (evenTableRow)
					c.setBackground(new Color(255, 255, 200));
				else
					c.setBackground(new Color(230, 230, 255));
			}
			return c;
		}

		@Override
		public Component prepareEditor(TableCellEditor editor, int row, int column) {
			Component c = super.prepareEditor(editor, row, column);

			// Do this to start editing cell contents straight away
			if (c instanceof JTextComponent) {
				((JTextComponent) c).selectAll();
			}

			return c;
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

	private class MyTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		@Override
		public int getColumnCount() {
			if (currentParameter == null)
				return 0;
			return 2;
		}

		@Override
		public int getRowCount() {
			if (currentParameter == null)
				return 0;
			return firstColumn.length;
		}

		@Override
		public Object getValueAt(int row, int col) {
			if (currentParameter == null)
				return null;
			if (col == 0)
				return firstColumn[row];
			else
				return secondColumn[row];
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return (col > 0);
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			if (currentParameter == null)
				return;

			if (col == 0) {
				Utils.log("cannot set value of first column", LogLevel.DEBUG);
				return;
			}

			if (currentParameter.getPostProcessor() != null)
				value = currentParameter.getPostProcessor().postProcess(value);

			if (value != null && currentParameter.isEnforceUniqueEntries()) {
				for (int r = 0; r < getRowCount(); r++) {
					if (r == row)
						continue;
					if (value.equals(secondColumn[row]))
						return;
				}
			}

			secondColumn[row] = value;
			fireTableCellUpdated(row, col);
		}
	}

	public TwoColumnJTable() {
		super();
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 1;

		localTable = new localJTable(new MyTableModel());
		localTable.setFillsViewportHeight(true);
		localTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		localTable.setRowSelectionAllowed(false);

		localTable.getModel().addTableModelListener(this);

		MultiRenderer multiRenderer = new MultiRenderer();
		MultiRenderer.fillMultiRendererWithSpreadSheet(multiRenderer);
		DefaultCellEditor defaultEditor = new DefaultCellEditor(new JTextField());
		multiRenderer.setDefaultEditor(defaultEditor);

		MultiRenderer multiRenderer2 = new MultiRenderer();
		MultiRenderer.fillMultiRendererWithSpreadSheet(multiRenderer2);

		defaultEditor.setClickCountToStart(1);

		localTable.setDefaultEditor(Object.class, multiRenderer);
		localTable.setDefaultRenderer(Object.class, multiRenderer2);
		
	    tips = new ColumnHeaderToolTips();
		JTableHeader header = localTable.getTableHeader();
	    header.addMouseMotionListener(tips);

		add(localTable, c);
	}

	@Override
	public void editingFinished() {
		if (localTable != null && localTable.getCellEditor() != null)
			localTable.getCellEditor().stopCellEditing();
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public void tableChanged(TableModelEvent e) {
	}

	private boolean evenTableRow;

	@Override
	public Component getRendererOrEditorComponent(JTable table, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		if (!rendererCalled) {
			hasFocus = true;
		}
		currentParameter = (TwoColumnTableParameter) value;

		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow)
			localTable.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		else
			localTable.setBackground(Utils.COLOR_FOR_ODD_ROWS);

		silenceUpdate = true;

		firstColumn = currentParameter.getFirstColumn();
		secondColumn = currentParameter.getSecondColumn();
		
		localTable.clearSelection();
		((MyTableModel) localTable.getModel()).fireTableDataChanged();
		localTable.createDefaultColumnsFromModel();
		
	    tips.clear();
	    for (int c = 0; c < localTable.getColumnCount(); c++) {
	      TableColumn col = localTable.getColumnModel().getColumn(c);
	      tips.setToolTip(col, localTable.getColumnName(c));
	    }

		// TODO Restore selection from parameter; see how OneColumnJTable does it

		int heightWanted = (int) getPreferredSize().getHeight();
		if (table != null)
			if (heightWanted > table.getRowHeight(row))
				table.setRowHeight(row, heightWanted);

		silenceUpdate = false;
		return this;
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

}
