/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.CellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.GUI_utils.MultiRenderer;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.drag_and_drop.DnDUtils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.TableParameter;
import pipeline.parameters.TextParameter;
import pipeline.parameters.TextParameterIncrementable;
import pipeline.parameters.TwoColumnTableParameter;

public class SplitParameterDisplay extends AbstractParameterCellView {

	private static final long serialVersionUID = 1L;

	private SplitParameter currentParameter;

	public SplitParameterDisplay() {
		super();
	}

	private LinkedList<Object> localRenderers;

	private MultiRenderer multiRenderer;

	private Object getRenderer(Class<?> rendererClass, Class<?> valueClass) throws InstantiationException,
			IllegalAccessException {
		if (localRenderers != null)
			for (Object o : localRenderers) {
				if (rendererClass.isInstance(o)) {
					localRenderers.remove(o);
					return o;
				}
			}
		return multiRenderer.getDelegateEditor(valueClass).getClass().newInstance();
	}

	private List<CellEditor> editors = new ArrayList<>();

	private class LocalMouseListener implements AWTEventListener {
		@Override
		public void eventDispatched(AWTEvent e) {
			MouseEvent me = (MouseEvent) e;
			if (!(e.getSource() instanceof Component))
				return;
			Component j = (Component) e.getSource();

			int x = me.getX(), y = me.getY();

			while (j != null && !(j instanceof SplitParameterDisplay)) {
				x += j.getX();
				y += j.getY();
				j = j.getParent();
			}

			mouseMoved(x, y);
		}
	}

	private LocalMouseListener localMouseListener;

	@Override
	protected Component getRendererOrEditorComponent(JTable table, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		if (localMouseListener == null) {
			localMouseListener = new LocalMouseListener();
		} else {
			Toolkit.getDefaultToolkit().removeAWTEventListener(localMouseListener);
		}
		if (!rendererCalled) {
			long eventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK;
			Toolkit.getDefaultToolkit().addAWTEventListener(localMouseListener, eventMask);
		}

		if (multiRenderer == null) {
			multiRenderer = new MultiRenderer();
			MultiRenderer.fillMultiRendererWithFullSet(multiRenderer);
		}

		this.removeAll();
		setLayout(new GridBagLayout());
		currentParameter = (SplitParameter) value;

		// THESE RENDERERS AND EDITORS WILL BE DUPLICATED TO AVOID TROUBLE
		Object[] parameterArray = ((Object[]) currentParameter.getValue());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 1;

		LinkedList<Object> newRenderers = new LinkedList<>();
		editors.clear();

		int preferredSize = 0;

		for (Object valuei : parameterArray) {
			if (valuei == null)
				continue;

			try {
				if (valuei instanceof TextParameterIncrementable) {
					Utils.log("Incrementable", LogLevel.DEBUG);
				}
				Class<?> editorClass = multiRenderer.getDelegateEditor(valuei.getClass()).getClass();
				Object comp = getRenderer(editorClass, valuei.getClass());
				newRenderers.add(comp);

				if (editorClass == CurveEditor.class) {
					c.weighty = 10.0;
				} else {
					c.weighty = 1.0;
				}

				if (!rendererCalled) {
					Component editor =
							((TableCellEditor) comp)
									.getTableCellEditorComponent(table, valuei, isSelected, row, column);
					if (editor == null) {
						throw new RuntimeException();
					}
					preferredSize += editor.getPreferredSize().height;
					if (!((AbstractParameter) valuei).isCompactDisplay() &&
							(valuei instanceof TextParameter || valuei instanceof SplitParameter
									|| valuei instanceof TableParameter || valuei instanceof TwoColumnTableParameter || valuei instanceof MultiListParameter))
						c.weighty = editor.getPreferredSize().height;
					else
						c.weighty = 0;
					add(editor, c);
					editors.add((TableCellEditor) comp);
					editor.setDropTarget(null);
				} else {
					Component renderer =
							((TableCellRenderer) comp).getTableCellRendererComponent(table, valuei, isSelected,
									hasFocus, row, column);
					if (renderer == null) {
						throw new RuntimeException();
					}
					preferredSize += renderer.getPreferredSize().height;

					if (!((AbstractParameter) valuei).isCompactDisplay() &&
							(valuei instanceof TextParameter || valuei instanceof SplitParameter
									|| valuei instanceof TableParameter || valuei instanceof TwoColumnTableParameter || valuei instanceof MultiListParameter))
						c.weighty = renderer.getPreferredSize().height;
					else
						c.weighty = 0;
					add(renderer, c);
					renderer.setDropTarget(null);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				Utils.printStack(e);
			}
		}

		c.weighty = 1;
		add(Box.createHorizontalGlue(), c);
		localRenderers = newRenderers;
		this.revalidate();

		int heightWanted = (int) (preferredSize * 1.1);
		if ((table != null) && heightWanted > table.getRowHeight(row)) {
			Utils.log("Asking table for row height " + heightWanted, LogLevel.DEBUG);
			table.setRowHeight(row, heightWanted);
			Utils.log("Preferred height now " + ((int) (preferredSize * 1.1)), LogLevel.DEBUG);
		}

		this.setTransferHandler(new TransferHandler() {

			private static final long serialVersionUID = -1784776634846848302L;

			@Override
			public boolean canImport(TransferHandler.TransferSupport info) {
				return currentParameter.canImport(info);
			}

			@Override
			public boolean importData(TransferSupport support) {
				File f = DnDUtils.extractFile(support);
				if (f != null)
					return currentParameter.importPreprocessedData(f.getAbsolutePath());
				else
					return false;
			}
		});
		return this;
	}

	@Override
	public String getToolTipText(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		Component[] children = getComponents();
		for (Component element : children) {
			if (element.contains(x - element.getX(), y - element.getY()))
				return ((JComponent) element).getToolTipText();
		}
		return null;
	}

	@Override
	public boolean stopCellEditing() {
		Toolkit.getDefaultToolkit().removeAWTEventListener(localMouseListener);
		super.stopCellEditing();
		Component[] children = this.getComponents();
		for (Component element : children) {
			try {
				if (element instanceof AbstractParameterCellView)
					((AbstractParameterCellView) element).stopCellEditing();
			} catch (Exception e) {
				Utils.printStack(e);
			}
		}
		return true;
	}

	@Override
	public void cancelCellEditing() {
		Toolkit.getDefaultToolkit().removeAWTEventListener(localMouseListener);
		super.cancelCellEditing();
		Component[] children = this.getComponents();
		for (Component element : children) {
			try {
				if (element instanceof AbstractParameterCellView)
					((AbstractParameterCellView) element).cancelCellEditing();
			} catch (Exception e) {
				Utils.printStack(e);
			}
		}
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	private void mouseMoved(int x, int y) {
		Component[] children = getComponents();
		for (Component element : children) {
			// Utils.log(((JComponent) element).getToolTipText(),LogLevel.VERBOSE_DEBUG);
			if (element.contains(x - element.getX(), y - element.getY())) {
				// Utils.log(((JComponent) element).getToolTipText()+" IN",LogLevel.VERBOSE_DEBUG);
				// No way to force ToolTip display??
			}
		}

	}

}
