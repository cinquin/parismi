/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SpreadsheetCell;
import pipeline.parameters.TextParameter;

public class SpreadsheetCellView extends AbstractParameterCellView implements ParameterListener {

	private static final long serialVersionUID = 1L;
	private String lastFormula;
	private Component editor;

	private SpreadsheetCell currentParameter;

	private boolean silenceUpdate;

	@Override
	public void cancelCellEditing() {
		super.cancelCellEditing();
		if (editor instanceof TableCellEditor)
			((TableCellEditor) editor).cancelCellEditing();
	}

	@Override
	public boolean stopCellEditing() {
		super.stopCellEditing();
		if (editor instanceof TableCellEditor)
			((TableCellEditor) editor).stopCellEditing();
		return true;
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
		super.addCellEditorListener(l);
		if (editor instanceof TableCellEditor)
			((TableCellEditor) editor).addCellEditorListener(l);
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		super.removeCellEditorListener(l);
		if (editor instanceof TableCellEditor)
			((TableCellEditor) editor).removeCellEditorListener(l);
	}

	@Override
	protected void editingFinished() {
		if (!silenceUpdate) {
			silenceUpdate = true;
			String newFormula = (String) formulaParameter.getValue();
			if (!lastFormula.equals(newFormula)) {
				lastFormula = new String(newFormula);
				currentParameter.setFormula(newFormula);
				currentParameter.fireValueChanged(false, false, true);
				Object tableModel = table.getModel();
				if (tableModel instanceof AbstractTableModel)
					((AbstractTableModel) tableModel).fireTableCellUpdated(row, column);
			}
			silenceUpdate = false;
		}
	}

	private TextBox formulaView;
	private TextParameter formulaParameter;
	private JTable table;
	private int row;
	private int column;

	public SpreadsheetCellView() {
		super();
		formulaView = new TextBox();
	}

	private void cleanUpEditor() {
		if (!(editor instanceof TableCellEditor))
			return;
		TableCellEditor ed = (TableCellEditor) editor;
		editorListeners.forEach(ed::removeCellEditorListener);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		cleanUpEditor();
		this.table = table;
		this.row = row;
		this.column = column;
		// Render the value of the expression
		if (value == null || (!(value instanceof SpreadsheetCell)))
			return table.getDefaultRenderer(String.class).getTableCellRendererComponent(table, null, isSelected,
					hasFocus, row, column);
		currentParameter = (SpreadsheetCell) value;
		Object evaluationResult = ((Object[]) currentParameter.getValue())[0];
		if (evaluationResult == null)
			return table.getDefaultRenderer(String.class).getTableCellRendererComponent(table, evaluationResult,
					isSelected, hasFocus, row, column);
		else
			return table.getDefaultRenderer(evaluationResult.getClass()).getTableCellRendererComponent(table,
					evaluationResult, isSelected, hasFocus, row, column);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		cleanUpEditor();
		this.table = table;
		this.row = row;
		this.column = column;
		// Edit the String formula
		currentParameter = (SpreadsheetCell) value;

		if (formulaView == null)
			formulaView = new TextBox();

		lastFormula = currentParameter.getFormula();
		if (formulaParameter == null) {
			formulaParameter = new TextParameter("", "Formula", lastFormula, true, this, this);
		} else {
			formulaParameter.setValue(lastFormula);
		}
		editor =
				formulaView.getTableCellRendererOrEditorComponent(table, formulaParameter, isSelected, true, row,
						column, false);
		return editor;
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		if (!silenceUpdate) {
			if (currentParameter != null)
				currentParameter.fireValueChanged(stillChanging, true, true);
		}
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {

	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {

	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

}
