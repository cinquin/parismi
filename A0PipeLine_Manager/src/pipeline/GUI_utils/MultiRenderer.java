/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
/*
 * Author: Bart Cremers
 * Date: 1-dec-2006
 * Time: 12:26:24
 */
// Modified to also handle cell editing by Olivier Cinquin 7th March 2010
package pipeline.GUI_utils;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import pipeline.parameter_cell_views.Button;
import pipeline.parameter_cell_views.ButtonForListDisplay;
import pipeline.parameter_cell_views.CheckBox;
import pipeline.parameter_cell_views.ComboBox;
import pipeline.parameter_cell_views.CurveEditor;
import pipeline.parameter_cell_views.DateEditor;
import pipeline.parameter_cell_views.DirectoryPopupDialog;
import pipeline.parameter_cell_views.FloatRangeSlider;
import pipeline.parameter_cell_views.FloatSlider;
import pipeline.parameter_cell_views.IntRangeSlider;
import pipeline.parameter_cell_views.IntSlider;
import pipeline.parameter_cell_views.MultiList;
import pipeline.parameter_cell_views.OneColumnJTable;
import pipeline.parameter_cell_views.SplitParameterDisplay;
import pipeline.parameter_cell_views.SpreadsheetCellView;
import pipeline.parameter_cell_views.TextBox;
import pipeline.parameters.ActionParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.ComboBoxParameter;
import pipeline.parameters.ComboBoxParameterPrintValueAsString;
import pipeline.parameters.DateParameter;
import pipeline.parameters.DirectoryParameter;
import pipeline.parameters.FileNameParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.IntParameter;
import pipeline.parameters.IntRangeParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.SplineParameter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SpreadsheetCell;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;

/**
 * This class is registered as a renderer and editor for the pipeline table columns corresponding to
 * parameters. It examines the value of the particular parameter object for which it is asked to return
 * a renderer or editor, and returns one registered in prior calls for the type of the parameter object
 * (e.g. a slider for an integer).
 * Modified from original version by Bart Cremers
 **/
public class MultiRenderer implements TableCellRenderer, TableCellEditor {
	private final TableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
	private TableCellEditor defaultEditor = null;

	private Map<Class<?>, TableCellRenderer> registeredRenderers = new HashMap<>();

	private Map<Class<?>, TableCellEditor> registeredEditors = new HashMap<>();

	public void setDefaultEditor(TableCellEditor e) {
		defaultEditor = e;
	}

	private TableCellEditor currentEditor;
	public boolean singleClickToEdit = true;

	public static void fillMultiRendererWithSpreadSheet(MultiRenderer multiRenderer) {
		multiRenderer.registerRenderer(SpreadsheetCell.class, new SpreadsheetCellView());
		multiRenderer.registerEditor(SpreadsheetCell.class, new SpreadsheetCellView());
	}

	public static void fillMultiRendererWithFullSet(MultiRenderer multiRenderer) {
		ComboBox mybox = new ComboBox();
		ComboBox mybox2 = new ComboBox();
		ComboBox mybox3 = new ComboBox();
		ComboBox mybox4 = new ComboBox();
		IntSlider myIntSlider = new IntSlider();
		IntSlider myIntSlider2 = new IntSlider();
		IntRangeSlider myIntSliderRange = new IntRangeSlider();
		IntRangeSlider myIntSliderRange2 = new IntRangeSlider();
		FloatRangeSlider myFloatSliderRange = new FloatRangeSlider();
		FloatRangeSlider myFloatSliderRange2 = new FloatRangeSlider();
		MultiList mySimpleMultiList = new MultiList();
		MultiList mySimpleMultiList2 = new MultiList();
		FloatSlider myFloatSlider = new FloatSlider();
		FloatSlider myFloatSlider2 = new FloatSlider();
		OneColumnJTable my1col = new OneColumnJTable();
		OneColumnJTable my1col2 = new OneColumnJTable();
		SplitParameterDisplay split1 = new SplitParameterDisplay();
		SplitParameterDisplay split2 = new SplitParameterDisplay();
		DirectoryPopupDialog dirDialog1 = new DirectoryPopupDialog();
		DirectoryPopupDialog dirDialog2 = new DirectoryPopupDialog();
		CheckBox checkBox1 = new CheckBox();
		CheckBox checkBox2 = new CheckBox();
		TextBox textBox = new TextBox();
		TextBox textBox2 = new TextBox();
		TextBox textBox3 = new TextBox();
		TextBox textBox4 = new TextBox();
		Button button1 = new Button();
		Button button2 = new Button();

		DateEditor dateEditor1 = new DateEditor();
		DateEditor dateEditor2 = new DateEditor();

		ButtonForListDisplay bList = new ButtonForListDisplay();
		ButtonForListDisplay bList2 = new ButtonForListDisplay();

		CurveEditor spline1 = new CurveEditor();
		CurveEditor spline2 = new CurveEditor();

		multiRenderer.registerRenderer(SplineParameter.class, spline1);
		multiRenderer.registerRenderer(IntRangeParameter.class, myIntSliderRange);
		multiRenderer.registerRenderer(FloatParameter.class, myFloatSlider);
		multiRenderer.registerRenderer(IntParameter.class, myIntSlider);
		multiRenderer.registerRenderer(ComboBoxParameter.class, mybox);
		multiRenderer.registerRenderer(ComboBoxParameterPrintValueAsString.class, mybox3);
		multiRenderer.registerRenderer(MultiListParameter.class, mySimpleMultiList);
		multiRenderer.registerRenderer(TableParameter.class, my1col);
		multiRenderer.registerRenderer(SplitParameter.class, split1);
		multiRenderer.registerRenderer(DirectoryParameter.class, dirDialog1);
		multiRenderer.registerRenderer(TextParameter.class, textBox);
		multiRenderer.registerRenderer(FileNameParameter.class, textBox3);
		multiRenderer.registerRenderer(FloatRangeParameter.class, myFloatSliderRange);
		multiRenderer.registerRenderer(BooleanParameter.class, checkBox1);
		multiRenderer.registerRenderer(ActionParameter.class, button1);
		multiRenderer.registerRenderer(List.class, bList);
		multiRenderer.registerRenderer(DateParameter.class, dateEditor1);

		multiRenderer.registerEditor(SplineParameter.class, spline2);
		multiRenderer.registerEditor(ComboBoxParameter.class, mybox2);
		multiRenderer.registerEditor(ComboBoxParameterPrintValueAsString.class, mybox4);
		multiRenderer.registerEditor(IntParameter.class, myIntSlider2);
		multiRenderer.registerEditor(FloatParameter.class, myFloatSlider2);
		multiRenderer.registerEditor(IntRangeParameter.class, myIntSliderRange2);
		multiRenderer.registerEditor(MultiListParameter.class, mySimpleMultiList2);
		multiRenderer.registerEditor(TableParameter.class, my1col2);
		multiRenderer.registerEditor(SplitParameter.class, split2);
		multiRenderer.registerEditor(DirectoryParameter.class, dirDialog2);
		multiRenderer.registerEditor(TextParameter.class, textBox2);
		multiRenderer.registerEditor(FileNameParameter.class, textBox4);
		multiRenderer.registerEditor(FloatRangeParameter.class, myFloatSliderRange2);
		multiRenderer.registerEditor(BooleanParameter.class, checkBox2);
		multiRenderer.registerEditor(ActionParameter.class, button2);
		multiRenderer.registerEditor(List.class, bList2);
		multiRenderer.registerEditor(DateParameter.class, dateEditor2);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		TableCellRenderer delegate = null;
		if (value != null) {
			delegate = getDelegateRenderer(value.getClass());
		}

		if (delegate == null || value == null) {
			delegate = defaultRenderer;
		}

		return delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	}

	public void registerRenderer(Class<?> type, TableCellRenderer renderer) {
		registeredRenderers.put(type, renderer);
	}

	public void registerEditor(Class<?> type, TableCellEditor renderer) {
		registeredEditors.put(type, renderer);
	}

	TableCellRenderer getDelegateRenderer(Class<?> type) {
		TableCellRenderer delegate = null;
		while (type != null && delegate == null) {
			delegate = registeredRenderers.get(type);
			type = type.getSuperclass();
		}
		return delegate;
	}

	public TableCellEditor getDelegateEditor(Class<?> type) {
		TableCellEditor delegate = null;
		while (type != null && delegate == null) {
			delegate = registeredEditors.get(type);
			type = type.getSuperclass();
		}
		return delegate;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		TableCellEditor delegateEditor = null;
		if (value != null) {
			delegateEditor = getDelegateEditor(value.getClass());
		}

		if (delegateEditor == null || value == null) {
			if (defaultEditor != null) {
				delegateEditor = defaultEditor;
			} else {
				delegateEditor = new DefaultCellEditor(new JTextField());
			}
		}

		currentEditor = delegateEditor;

		return delegateEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public Object getCellEditorValue() {
		return currentEditor != null ? currentEditor.getCellEditorValue() : null;
	}

	@Override
	public boolean isCellEditable(EventObject anEvent) {
		if (anEvent instanceof MouseEvent) {
			return singleClickToEdit || ((MouseEvent) anEvent).getClickCount() >= 2;
		}
		return true;
	}

	@Override
	public void cancelCellEditing() {
		if (currentEditor != null)
			currentEditor.cancelCellEditing();
	}

	@Override
	public boolean stopCellEditing() {
		// Utils.log("Multirenderer stop editing",LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
		if (currentEditor != null)
			currentEditor.stopCellEditing();
		return true;
	}

	@Override
	public void addCellEditorListener(CellEditorListener l) {
		if (currentEditor != null)
			currentEditor.addCellEditorListener(l);
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		if (currentEditor != null)
			currentEditor.removeCellEditorListener(l);
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		if (currentEditor != null)
			return currentEditor.shouldSelectCell(anEvent);
		else
			return true;
	}

}
