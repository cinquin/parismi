/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils.image_with_toolbar;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.data.ClickedPoint;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameter_cell_views.TwoColumnJTable;
import pipeline.parameters.TwoColumnTableParameter;

class CellInfoToolTip extends JDialog implements MouseListener, ComponentListener {

	private BeanTableModel<ClickedPoint> tableModel = new BeanTableModel<>(ClickedPoint.class);

	public class TargetedMouseHandler implements AWTEventListener {

		private Component parent;

		// private Component innerBound;
		// private boolean hasExited = true;

		public TargetedMouseHandler(Component p, Component p2) {
			parent = p;
			// innerBound = p2;
		}

		@Override
		public void eventDispatched(AWTEvent e) {
			if (e instanceof MouseEvent) {
				if (SwingUtilities.isDescendingFrom((Component) e.getSource(), parent)) {
					MouseEvent m = (MouseEvent) e;
					if ((m.getID() == MouseEvent.MOUSE_PRESSED) || (m.getID() == MouseEvent.MOUSE_CLICKED))
						ignoreCloseRequest = true;
				}
			}
		}
	}

	private TargetedMouseHandler listener = new TargetedMouseHandler(this, this);

	public CellInfoToolTip(Frame owner, boolean modal, ClickedPoint cell) {
		super(owner, modal);
		addComponentListener(this);
		addMouseListener(this);

		Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.MOUSE_EVENT_MASK);

		final Timer t = new Timer(10000, e -> Toolkit.getDefaultToolkit().removeAWTEventListener(listener));
		t.setRepeats(false);
		t.start();

		/*
		 * setWindowOpacity does not work on a decorated dialog?
		 * try {
		 * Class<?> awtUtilitiesClass = Class.forName("com.sun.awt.AWTUtilities");
		 * Method mSetWindowOpacity = awtUtilitiesClass.getMethod("setWindowOpacity", Window.class, float.class);
		 * mSetWindowOpacity.invoke(null, this, Float.valueOf(0.75f));
		 * } catch (NoSuchMethodException ex) {
		 * ex.printStackTrace();
		 * } catch (SecurityException ex) {
		 * ex.printStackTrace();
		 * } catch (ClassNotFoundException ex) {
		 * ex.printStackTrace();
		 * } catch (IllegalAccessException ex) {
		 * ex.printStackTrace();
		 * } catch (IllegalArgumentException ex) {
		 * ex.printStackTrace();
		 * } catch (InvocationTargetException ex) {
		 * ex.printStackTrace();
		 * }
		 */

		// tipFrame.setUndecorated(true); THIS DOES NOT WORK AND PREVENTS THE WINDOW FROM SHOWING AT ALL
		JLabel testLabel = new JLabel("test");

		add(testLabel);
		// setFocusableWindowState(true);
		// setFocusable(true);

		List<ClickedPoint> oneElementList = new LinkedList<>();
		oneElementList.add(cell);

		tableModel = new BeanTableModel<>(ClickedPoint.class, oneElementList);

		Object[] column1 = new Object[tableModel.columns.size()];
		Object[] column2 = new Object[tableModel.columns.size()];

		int counter = 0;
		for (BeanTableModel<ClickedPoint>.ColumnInformation column : tableModel.columns) {
			column1[counter] = column.getName();
			try {
				column2[counter] = tableModel.getValueAt(0, counter);// ""+ //column.getGetter().invoke(cell);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			counter++;
		}

		TwoColumnTableParameter tableParam = new TwoColumnTableParameter("Values", "", column1, column2, null);

		TwoColumnJTable twoColumnTable = new TwoColumnJTable();
		add(twoColumnTable.getTableCellRendererOrEditorComponent(null, tableParam, false, false, 0, 0));
		pack();
		setPreferredSize(new Dimension(250, getHeight()));
		// twoColumnTable.setPreferredSize(new Dimension(100, twoColumnTable.getHeight()));
		pack();
	}

	/**
	 * True if the user has clicked within the window. In this case the window shouldn't be automatically
	 * close if the mouse wanders far away.
	 */
	private boolean ignoreCloseRequest;

	@Override
	public void mousePressed(MouseEvent e) {
		ignoreCloseRequest = true;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public void mouseClicked(MouseEvent e) {
		ignoreCloseRequest = true;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	// private boolean mouseHasEntered;

	@Override
	public void mouseEntered(MouseEvent e) {
		// mouseHasEntered=true;
	}

	public void requestClose() {
		mouseExited(null);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
		if (ignoreCloseRequest)
			return;
		setVisible(false);
		dispose();
	}

	/**
	 * The first componentResized call does not correspond to an actual move, so ignore it.
	 */
	private int numberResizes;

	@Override
	public void componentResized(ComponentEvent e) {
		// if (true) return;
		if (systemGeneratedField != null) {
			try {
				if (!systemGeneratedField.getBoolean(e))
					ignoreCloseRequest = true;
			} catch (Exception e1) {
				Utils.log("Could not access isSystemGenerated field " + e, LogLevel.WARNING);
			}
		} else {
			if (numberResizes < 3)
				numberResizes++;
			else
				ignoreCloseRequest = true;
		}
	}

	/**
	 * The first componentMoved call does not correspond to an actual move, so ignore it.
	 */
	private int numberMoves;

	private static Field systemGeneratedField;
	{
		try {
			// systemGeneratedField=ComponentEvent.class.getField("isSystemGenerated");
			// systemGeneratedField.setAccessible(true);
		} catch (Exception e) {
			Utils.log("Could not access isSystemGenerated field " + e, LogLevel.WARNING);
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		if (systemGeneratedField != null) {
			try {
				if (!systemGeneratedField.getBoolean(e))
					ignoreCloseRequest = true;
			} catch (Exception e1) {
				Utils.log("Could not access isSystemGenerated field " + e, LogLevel.WARNING);
			}
		} else {
			if (numberMoves < 4)
				numberMoves++;
			else
				ignoreCloseRequest = true;
		}
	}

	@Override
	public void componentShown(ComponentEvent e) {

	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

}
