/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.awt.Component;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.JXTable;

public class TableBetterFocus extends JXTable {

	private static final long serialVersionUID = 1L;

	public TableBetterFocus(TableModel model) {
		super(model);
	}

	// from http://stackoverflow.com/questions/1412252/select-all-data-when-start-editing-jtable-cell
	@Override
	public Component prepareEditor(TableCellEditor editor, int row, int column) {
		Component c = super.prepareEditor(editor, row, column);

		// Do this to start editing cell contents straight away
		if (c instanceof JTextComponent) {
			((JTextComponent) c).selectAll();
		}
		if (c != null)
			c.requestFocusInWindow();
		return c;
	}
}
