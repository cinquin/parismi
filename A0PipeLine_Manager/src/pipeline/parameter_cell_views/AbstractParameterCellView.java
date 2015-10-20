/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;

/**
 * Abstract class that serves as renderer and editor for pipeline parameters. Since everything is
 * displayed and edited within a JTable, this class implements the TableCellRenderer and TableCellEditor
 * interfaces. This class and derivatives are registered with the {@link pipeline.GUI_utils.MultiRenderer}.
 * The MultiRenderer is itself registered for the parameter columns of the JTable, and for each parameter
 * it is asked to render finds the class in {@link pipeline.parameter_cell_views} that matches
 * the parameter.
 *
 */
public abstract class AbstractParameterCellView extends JPanel implements TableCellRenderer, TableCellEditor,
		ParameterListener {
	
	protected abstract Component getRendererOrEditorComponent(JTable table0,  @NonNull Object value, boolean isSelected, boolean hasFocus,
			int row, int column, boolean rendererCalled);
	
	private final EmptyRenderer emptyRenderer = new EmptyRenderer();
	
	@Override
	public final Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		if (value == null) {
			return emptyRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		return getRendererOrEditorComponent(table, value, isSelected, hasFocus, row, column, true);
	}

	@Override
	public final Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (value == null) {
			return null;
		}
		return getRendererOrEditorComponent(table, value, isSelected, false, row, column, false);
	}

	@Override
	public void scrollRectToVisible(Rectangle aRect) {
		// Important to override this method to prevent the table from scrolling
		// as it is being clicked, which causes misinteraction with the table cell editors
	}

	@Override
	public String getParameterName() {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void setParameterName(String name) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
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

	private static final long serialVersionUID = 1L;
	transient Vector<CellEditorListener> editorListeners; // for editor

	/**
	 * Used as a notification that the user has finished editing; the specific AbstractParameterCellView descendant
	 * can use this to save the changes so they're not lost.
	 */
	void editingFinished() {
	}

	AbstractParameterCellView() {
		super();
		editorListeners = new Vector<>();// for editor
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void cancelCellEditing() {
		ChangeEvent ce = new ChangeEvent(this);
		checkEditorListeners();
		for (CellEditorListener cellEditorListener : ((Vector<CellEditorListener>) editorListeners.clone())) {
			try {
				cellEditorListener.editingCanceled(ce);
			} catch (Exception e) {

			}
		}
	}

	private void checkEditorListeners() {
		if (editorListeners == null)
			editorListeners = new Vector<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean stopCellEditing() {
		checkEditorListeners();
		editingFinished();
		ChangeEvent ce = new ChangeEvent(this);
		for (CellEditorListener cellEditorListener : ((Vector<CellEditorListener>) editorListeners.clone())) {
			try {
				cellEditorListener.editingStopped(ce);
			} catch (Exception e) {

			}
		}
		return true;
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
		checkEditorListeners();
		editorListeners.add(l);
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		checkEditorListeners();
		while (editorListeners.remove(l)) {
		}
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return false;
	}

	Number parseTextBox(JTextField box) {
		try {
			return getNumberFormatter().parse(box.getText());
		} catch (ParseException nfe) {
			Utils.log("Cannot parse " + box.getText() + " as a number; ignoring", LogLevel.ERROR);
			throw new RuntimeException(nfe);
		}
	}

	private NumberFormat topLevelNF = NumberFormat.getInstance();

	NumberFormat getNumberFormatter() {
		return topLevelNF;
	}

}
