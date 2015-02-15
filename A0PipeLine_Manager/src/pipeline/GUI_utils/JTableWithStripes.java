/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;

import pipeline.A0PipeLine_Manager;
import pipeline.A0PipeLine_Manager.TableSelectionDemo.MyTableModel;
import pipeline.misc_util.Utils;

/**
 * Just a regular JTable modified to start editing cell contents with less clicking, and to outline rows in stripes of
 * alternating
 * colors for better visibility when the table has rows of different heights.
 *
 */
public class JTableWithStripes extends JTable {

	private static final long serialVersionUID = 1L;

	// from http://stackoverflow.com/questions/1412252/select-all-data-when-start-editing-jtable-cell
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
	public void scrollRectToVisible(Rectangle aRect) {
		// Overridden to avoid the table jumping around like crazy in response to clicks

		for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
			if (e.getMethodName().contains("mousePressed")) {
				return;
			}
		}
		reallyScrollRectToVisible(aRect);
	}

	public void reallyScrollRectToVisible(Rectangle aRect) {
		super.scrollRectToVisible(aRect);
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
		Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
		Object[] theRow = ((MyTableModel) this.getModel()).data[rowIndex];
		// the following creates blue and yellow stripes in the table to outline the rows
		if ((theRow.length > A0PipeLine_Manager.COMPUTING_ERROR)
				&& (theRow[A0PipeLine_Manager.COMPUTING_ERROR] != null)
				&& ((Boolean) theRow[A0PipeLine_Manager.COMPUTING_ERROR]))
			if (!isCellSelected(rowIndex, vColIndex))
				c.setBackground(Color.RED);
			else
				c.setBackground(Color.ORANGE);
		else if (!isCellSelected(rowIndex, vColIndex)) {
			if (rowIndex % 2 == 0) {
				c.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
			} else { // If not shaded, match the table's background
				c.setBackground(Utils.COLOR_FOR_ODD_ROWS);
			}
		} else
			c.setBackground(Utils.COLOR_FOR_SELECTED_ROWS);
		return c;
	}

	public JTableWithStripes(AbstractTableModel model) {
		super(model);
	}

}
