/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pipeline.misc_util.Utils;
import pipeline.parameters.IntParameter;

public class IntSlider extends AbstractParameterCellView {

	private static final long serialVersionUID = 1L;

	private JSlider jSlider;
	private JTextField textMinimum;
	private JTextField textMaximum;
	private JTextField currentTextValue;
	private JLabel parameterName;

	private int maximum;
	private int minimum;
	private int currentValue;

	private IntParameter currentParameter;

	private boolean silenceUpdate;

	public IntSlider() {
		super();
		addMouseWheelListener(e -> {
			int rotation = e.getWheelRotation();

			float desiredChange = (maximum - minimum) * 0.001f * rotation * Utils.getMouseWheelClickFactor();
			if (Math.abs(desiredChange) < 1f)
				desiredChange /= Math.abs(desiredChange);

			currentValue += (int) desiredChange;
			if (!((e.getModifiers() & java.awt.event.InputEvent.ALT_MASK) > 0)) {
				if (currentValue > maximum)
					currentValue = maximum;
				if (currentValue < minimum)
					currentValue = minimum;
			}
			currentParameter.setValue(new int[] { currentValue, minimum, maximum });

			updateParameterValues();

			currentParameter.fireValueChanged(false, false, true);

		});
		silenceUpdate = true;

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		parameterName = new JLabel("");

		jSlider = new JSlider(0, 50, 10);
		jSlider.addChangeListener(new sliderListener());
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 3;
		add(jSlider, c);
		c.gridwidth = 1;

		currentTextValue = new JTextField(jSlider.getValue());
		currentTextValue.addActionListener(new valueListener());
		Font smallerFont =
				new Font(currentTextValue.getFont().getName(), currentTextValue.getFont().getStyle(), currentTextValue
						.getFont().getSize() - 2);
		textMinimum = new JTextField("0");
		textMinimum.setFont(smallerFont);
		textMinimum.addActionListener(new minimumListener());
		textMaximum = new JTextField("50");
		textMaximum.setFont(smallerFont);
		textMaximum.addActionListener(new maximumListener());

		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 1.0;
		c.weightx = 0.25;
		add(textMinimum, c);

		c.gridx = 2;
		c.gridy = 1;
		c.weighty = 1.0;
		c.weightx = 0.25;
		add(textMaximum, c);

		c.gridx = 1;
		c.gridy = 1;
		c.weighty = 1.0;
		c.weightx = 0.5;
		add(currentTextValue, c);

		parameterName = new JLabel("parameter");
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 1.0;
		c.weightx = 0.0;
		c.gridwidth = 3;
		add(parameterName, c);

	}

	@SuppressWarnings("unused")
	private static final boolean turnOffForDebugging = false;

	private class minimumListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				JTextField source = (JTextField) e.getSource();
				jSlider.setMinimum(parseTextBox(source).intValue());
				minimum = parseTextBox(source).intValue();
				currentParameter.setValue(new int[] { currentValue, minimum, maximum });
				revalidate();
			}
		}
	}

	private class maximumListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				JTextField source = (JTextField) e.getSource();
				jSlider.setMaximum(parseTextBox(source).intValue());
				maximum = parseTextBox(source).intValue();
				currentParameter.setValue(new int[] { currentValue, minimum, maximum });
				revalidate();
			}
		}
	}

	private class valueListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				JTextField source = (JTextField) e.getSource();
				silenceUpdate = true;
				jSlider.setValue(parseTextBox(source).intValue());
				currentValue = parseTextBox(source).intValue();
				silenceUpdate = false;
				currentParameter.setValue(new int[] { currentValue, minimum, maximum });
				updateParameterValuesButNotMainTextBox(); // In case parameter adjusted minimum or maximum
				currentParameter.fireValueChanged(false, false, true);
			}
		}
	}

	/**
	 * Read parameter value, min, and max and update our display.
	 */
	private void updateParameterValuesButNotMainTextBox() {
		int[] int_values = (int[]) (currentParameter.getValue());
		currentValue = int_values[0];
		minimum = int_values[1];
		maximum = int_values[2];

		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;
		updateTextValue(textMinimum, minimum);
		updateTextValue(textMaximum, maximum);

		jSlider.setMinimum((minimum));
		jSlider.setMaximum((maximum));
		jSlider.setValue(currentValue);
		jSlider.setEnabled(currentParameter.isEditable());

		revalidate();

		silenceUpdate = saveSilenceUpdate;

	}

	/**
	 * Read parameter value, min, and max and update our display.
	 */
	private void updateParameterValues() {
		int[] intValues = (int[]) (currentParameter.getValue());
		currentValue = intValues[0];
		minimum = intValues[1];
		maximum = intValues[2];

		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;
		updateTextValue(textMinimum, minimum);
		updateTextValue(textMaximum, maximum);
		updateTextValue(currentTextValue, currentValue);

		jSlider.setMinimum(minimum);
		jSlider.setMaximum(maximum);
		jSlider.setValue(currentValue);
		jSlider.setEnabled(currentParameter.isEditable());

		revalidate();

		silenceUpdate = saveSilenceUpdate;

	}

	private class sliderListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			if (!silenceUpdate) {
				JSlider source = (JSlider) e.getSource();
				source = jSlider;
				if (!source.getValueIsAdjusting()) {
					currentValue = (source.getValue());
					currentParameter.setValue(new int[] { currentValue, minimum, maximum });

					currentParameter.fireValueChanged(false, false, true);
				} else { // value is still adjusting
					int fps = (source.getValue());

					currentTextValue.setText("" + getNumberFormatter().format(fps));
					currentValue = source.getValue();
					currentParameter.setValue(new int[] { currentValue, minimum, maximum });

					currentParameter.fireValueChanged(true, false, true);
				}
			}
		}
	}

	private void updateTextValue(JTextField f, int v) {
		f.setText("" + getNumberFormatter().format(v));
	}

	private boolean evenTableRow;

	public Component getTableCellRendererOrEditorComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		currentParameter = (IntParameter) value;
		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow)
			setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		else
			setBackground(Utils.COLOR_FOR_ODD_ROWS);

		silenceUpdate = true;

		int[] intValues = (int[]) (currentParameter.getValue());
		currentValue = intValues[0];
		updateTextValue(currentTextValue, intValues[0]);
		updateTextValue(textMinimum, intValues[1]);
		updateTextValue(textMaximum, intValues[2]);
		minimum = intValues[1];
		maximum = intValues[2];
		if (true) {// rendererCalled
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;

			// For some strange reason, the slider doesn't get updated properly upon resizes
			// (only in the renderer, not the editor)
			// workaround is to remove it and re-create it

			remove(jSlider);
			jSlider = new JSlider(minimum, maximum, intValues[0]);

			c.gridx = 0;
			c.gridy = 0;
			c.weighty = 1.0;
			c.weightx = 1.0;
			c.gridwidth = 3;
			add(jSlider, c);
			jSlider.addChangeListener(new sliderListener());
		}

		parameterName.setText(currentParameter.getParamNameDescription()[0]);
		parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
		textMinimum.setEditable(currentParameter.editable()[0]);
		textMaximum.setEditable(currentParameter.editable()[1]);
		setToolTipText(currentParameter.getParamNameDescription()[1]);

		silenceUpdate = false;
		int heightWanted = (int) getPreferredSize().getHeight();
		if (table != null)
			if (heightWanted > table.getRowHeight(row))
				table.setRowHeight(row, heightWanted);
		return this;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellRendererOrEditorComponent(table, value, isSelected, hasFocus, row, column, true);
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		return getTableCellRendererOrEditorComponent(table, value, isSelected, true, row, column, false);
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

}
