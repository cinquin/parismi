/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JTable;

import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIOListOfQ;
import pipeline.misc_util.Utils;

public class ButtonForListDisplay extends AbstractParameterCellView {

	private static final long serialVersionUID = 1L;

	private IPluginIOListOfQ<?> list;
	private JButton theButton;
	private boolean silenceUpdate = false;

	public ButtonForListDisplay() {
		super();

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		theButton = new JButton();
		theButton.setMaximumSize(new Dimension(500, Utils.MAXIMAL_BUTTON_HEIGHT));
		list = null;
		theButton.addActionListener(new boxListener());
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 1;
		add(theButton, c);
	}

	private class boxListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				PluginIOView view = list.createView();
				view.show();
			}
		}
	}

	private boolean evenTableRow;

	Component getTableCellRendererOrEditorComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow) {
			this.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		} else {
			this.setBackground(Utils.COLOR_FOR_ODD_ROWS);
		}
		list = (IPluginIOListOfQ<?>) value;
		if (value == null) {
			return this;
		}
		silenceUpdate = true;

		theButton.setEnabled(true);
		theButton.setText(list.toString());

		this.setToolTipText("");
		int height_wanted = (int) getPreferredSize().getHeight();
		if (height_wanted > table.getRowHeight(row))
			table.setRowHeight(row, height_wanted);

		silenceUpdate = false;
		return this;

	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellRendererOrEditorComponent(table, value, isSelected, hasFocus, row, column);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return getTableCellRendererOrEditorComponent(table, value, isSelected, true, row, column);
	}

	@Override
	public Object getCellEditorValue() {
		return list;
	}
}
