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

import javax.swing.JCheckBox;
import javax.swing.JTable;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils;
import pipeline.parameters.BooleanParameter;

public class CheckBox extends AbstractParameterCellView {

	private static final long serialVersionUID = 1L;

	private BooleanParameter currentParameter;
	private JCheckBox jCheckBox;

	private boolean silenceUpdate;

	public CheckBox() {
		super();

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		jCheckBox = new JCheckBox();
		jCheckBox.setMaximumSize(new Dimension(500, Utils.MAXIMAL_BUTTON_HEIGHT));
		this.setMaximumSize(new Dimension(500, Utils.MAXIMAL_BUTTON_HEIGHT));
		currentParameter = null;
		jCheckBox.addActionListener(new boxListener());
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 1;
		add(jCheckBox, c);
	}

	private class boxListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				JCheckBox source = jCheckBox;
				if (source != null) {
					boolean s = source.isSelected();

					currentParameter.setValue(s);
					currentParameter.fireValueChanged(false, false, true);
				} else {

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
		currentParameter = (BooleanParameter) value;
		silenceUpdate = true;
		Object[] selection = (Object[]) (currentParameter.getValue());
		boolean selected = ((Boolean) selection[0]);
		jCheckBox.setSelected(selected);
		jCheckBox.setText(currentParameter.getParamNameDescription()[0]);

		this.setToolTipText(currentParameter.getParamNameDescription()[1]);
		int height_wanted = (int) getPreferredSize().getHeight();
		if ((table != null) && (height_wanted > table.getRowHeight(row)))
			table.setRowHeight(row, height_wanted);

		silenceUpdate = false;
		return this;
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}
}
