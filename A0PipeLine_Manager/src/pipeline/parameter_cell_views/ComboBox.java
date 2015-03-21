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
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils;
import pipeline.parameters.ComboBoxParameter;

public class ComboBox extends AbstractParameterCellView {

	private static final long serialVersionUID = 1L;

	private JLabel parameterName;

	private String currentValueString;
	private int currentValueInteger;

	private ComboBoxParameter currentParameter;
	private JComboBox<Object> theBox;
	private DefaultComboBoxModel<Object> model;

	private boolean silenceUpdate;

	public ComboBox() {
		super();

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		parameterName = new JLabel("");

		Object[] empty_array = {};
		theBox = new javax.swing.JComboBox<>(empty_array);
		currentParameter = null;
		theBox.addActionListener(new boxListener());
		model = (DefaultComboBoxModel<Object>) theBox.getModel();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 1;
		add(theBox, c);

		parameterName = new JLabel("parameter");
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 1.0;
		c.weightx = 0.0;
		c.gridwidth = 1;
		add(parameterName, c);
	}

	private class boxListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if ("comboBoxEdited".equals(e.getActionCommand())) {
				if (theBox.isEditable()) {
					String editedSelection = (String) theBox.getSelectedItem();
					currentParameter.setItemName(currentValueInteger, editedSelection);
					model.removeAllElements();
					for (int i = 0; i < currentParameter.getChoices().length; i++) {
						model.addElement(currentParameter.getChoices()[i]);
					}
					model.setSelectedItem(editedSelection);
				}
			}
			if (!silenceUpdate) {
				JComboBox<Object> source = theBox;
				if (source != null) {
					String s = (String) source.getSelectedItem();
					if (s != null) {
						if (source.getSelectedIndex() >= 0) {
							currentValueInteger = new Integer(source.getSelectedIndex());
							currentValueString = s;

							Object[] cv = { currentValueString, currentValueInteger };
							currentParameter.setValue(cv);
							currentParameter.fireValueChanged(false, false, true);
						}
					}
				}
			}
		}
	}

	private boolean evenTableRow;

	@Override
	protected Component getRendererOrEditorComponent(JTable table, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow) {
			this.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		} else {
			this.setBackground(Utils.COLOR_FOR_ODD_ROWS);
		}
		parameterName.setBackground(getBackground());
		currentParameter = (ComboBoxParameter) value;
		silenceUpdate = true;
		model = (DefaultComboBoxModel<Object>) theBox.getModel();
		model.removeAllElements();
		for (int i = 0; i < currentParameter.getChoices().length; i++) {
			model.addElement(currentParameter.getChoices()[i]);
		}
		Object[] selection = (Object[]) (currentParameter.getValue());
		int selInt = (Integer) selection[1];
		theBox.setSelectedIndex(selInt);

		parameterName.setText(currentParameter.getParamNameDescription()[0]);
		parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
		this.setToolTipText(currentParameter.getParamNameDescription()[1]);
		theBox.setEditable(currentParameter.editable()[0]);
		int heightWanted = (int) getPreferredSize().getHeight();
		if (heightWanted > table.getRowHeight(row))
			table.setRowHeight(row, heightWanted);
		silenceUpdate = false;
		return this;

	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	@Override
	public void editingFinished() {

	}
}
