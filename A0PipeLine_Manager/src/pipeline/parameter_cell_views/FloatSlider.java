/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatParameter;
import pipeline.parameters.ParameterListener;

public class FloatSlider extends AbstractParameterCellView implements ParameterListener {

	private static final long serialVersionUID = 1L;

	private JSlider slider;
	private JTextField textMinimum;
	private JTextField textMaximum;
	private JTextField currentTextValue;
	private JLabel parameterName;

	private float maximum;
	private float minimum;
	private float currentValue;

	private boolean silenceUpdate = true;

	private FloatParameter currentParameter;

	private NumberFormat nf = NumberFormat.getInstance();
	private DecimalFormat decimalFormatter = new DecimalFormat("0.00E0");

	public FloatSlider() {
		super();
		addMouseWheelListener(e -> {
			int rotation = e.getWheelRotation();

			float change = (maximum - minimum) * 0.001f * rotation * Utils.getMouseWheelClickFactor();

			currentValue += change;

			if (!((e.getModifiers() & java.awt.event.InputEvent.ALT_MASK) > 0)) {
				if (currentValue > maximum)
					currentValue = maximum;
				if (currentValue < minimum)
					currentValue = minimum;
			}

			currentParameter.setValue(new float[] { currentValue, minimum, maximum });

			updateParameterValues();
			currentParameter.fireValueChanged(false, false, true);

		});
		nf.setMaximumFractionDigits(5);
		nf.setMaximumIntegerDigits(8);
		nf.setGroupingUsed(false);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.gridheight = 1;

		c.ipady = 10;
		parameterName = new JLabel("");

		slider = new JSlider(0, 50, 10);
		slider.addChangeListener(new sliderListener());
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(slider, c);
		c.gridwidth = 1;
		c.ipady = 0;

		currentTextValue = new JTextField(slider.getValue());
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

	private String format(float f) {
		if (currentParameter.useExponentialFormat)
			return decimalFormatter.format(f);
		else
			return getNumberFormatter().format(f);
	}

	private class minimumListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				JTextField source = textMinimum;// (JTextField)e.getSource();
				slider.setMinimum(((int) (100f * parseTextBox(source).floatValue())));
				minimum = parseTextBox(source).floatValue();
				currentParameter.setValue(new float[] { currentValue, minimum, maximum });
			}
		}
	}

	private class maximumListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				JTextField source = textMaximum;// (JTextField)e.getSource();
				slider.setMaximum(((int) (100f * parseTextBox(source).floatValue())));
				maximum = parseTextBox(source).floatValue();
				currentParameter.setValue(new float[] { currentValue, minimum, maximum });
			}
		}
	}

	private class valueListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				JTextField source = currentTextValue;// (JTextField)e.getSource();
				silenceUpdate = true;
				slider.setValue(((int) (100f * parseTextBox(source).floatValue())));
				silenceUpdate = false;
				currentValue = parseTextBox(source).floatValue();
				currentParameter.setValue(new float[] { currentValue, minimum, maximum });
				updateParameterValuesButNotMainTextBox(); // In case parameter adjusted minimum or maximum
				currentParameter.fireValueChanged(false, false, true);
			}
		}
	}

	private class sliderListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (!silenceUpdate) {
				JSlider source = slider;
				currentValue = (source.getValue()) / 100.0f;
				currentParameter.setValue(new float[] { currentValue, minimum, maximum });
				if (!silenceUpdate)
					currentTextValue.setText("" + format(currentValue));
				currentParameter.fireValueChanged(source.getValueIsAdjusting(), false, true);
			}
		}
	}

	private void updateTextValue(JTextField f, float v) {
		f.setText("" + format(v));
	}

	private boolean evenTableRow;
	
	@SuppressWarnings("unused")
	@Override
	public Component getRendererOrEditorComponent(JTable table, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {

		if (table != null) {
			Dimension d = getPreferredSize();
			d.width = table.getColumnModel().getColumn(column).getWidth();
			setSize(d);
		}

		if (currentParameter != null) {
			currentParameter.removeListener(this);
		}

		currentParameter = (FloatParameter) value;
		/*if (value == null) {
			slider.setEnabled(true);
			textMinimum.setEditable(true);
			textMaximum.setEditable(true);
			currentTextValue.setEditable(true);
			return this;
		}*/
		currentParameter.addGUIListener(this);
		currentParameter.validateRange();

		silenceUpdate = true;
		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow)
			this.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		else
			this.setBackground(Utils.COLOR_FOR_ODD_ROWS);

		if (true) {// rendererCalled
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;

			// For some strange reason, the slider doesn't get updated properly upon resizes
			// (only in the renderer, not the editor)
			// workaround is to remove it and re-create it

			remove(slider);
			readInParameterValues();
			slider = new JSlider((int) (minimum * 100f), (int) (maximum * 100f), (int) (currentValue * 100f));

			c.gridx = 0;
			c.gridy = 0;
			c.weighty = 1.0;
			c.weightx = 1.0;
			c.gridwidth = 3;
			add(slider, c);
			slider.addChangeListener(new sliderListener());
		} else {
			if (table != null)
				slider.setBounds(table.getCellRect(row, column, false));
			slider.updateUI();
		}

		updateDisplay();

		if (table != null) {
			int height_wanted = (int) getPreferredSize().getHeight();
			if (height_wanted > table.getRowHeight(row))
				table.setRowHeight(row, height_wanted);
		}

		silenceUpdate = false;
		return this;

	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		// nothing to do
	}

	private void readInParameterValues() {
		float[] float_values = (float[]) (currentParameter.getValue());
		currentValue = float_values[0];
		minimum = float_values[1];
		maximum = float_values[2];
	}

	/**
	 * Read parameter value, min, and max and update our display.
	 */
	private void updateParameterValues() {
		readInParameterValues();

		updateTextValue(currentTextValue, currentValue);
		updateTextValue(textMinimum, minimum);
		updateTextValue(textMaximum, maximum);

		slider.setMinimum((int) (100.0f * minimum));
		slider.setMaximum((int) (100.0f * maximum));
		slider.setValue((int) (100.0f * currentValue));
		slider.setEnabled(currentParameter.isEditable());
	}

	/**
	 * Read parameter value, min, and max and update our display.
	 */
	private void updateParameterValuesButNotMainTextBox() {
		float[] float_values = (float[]) (currentParameter.getValue());
		currentValue = float_values[0];
		minimum = float_values[1];
		maximum = float_values[2];

		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;
		updateTextValue(textMinimum, minimum);
		updateTextValue(textMaximum, maximum);

		slider.setEnabled(currentParameter.isEditable());
		slider.setValue((int) (100.0f * currentValue));
		slider.setMinimum((int) (100.0f * minimum));
		slider.setMaximum((int) (100.0f * maximum));
		slider.setValue((int) (100.0f * currentValue));

		silenceUpdate = saveSilenceUpdate;
	}

	private void updateDisplay() {
		Boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;

		updateParameterValues();
		parameterName.setText(currentParameter.getParamNameDescription()[0]);
		parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
		textMinimum.setEditable(currentParameter.editable()[0]);
		textMaximum.setEditable(currentParameter.editable()[1]);
		currentTextValue.setEditable(currentParameter.isEditable());
		this.setToolTipText(currentParameter.getParamNameDescription()[1]);

		this.revalidate();
		silenceUpdate = saveSilenceUpdate;
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		if (!silenceUpdate) {
			updateDisplay();
		}
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		if (!silenceUpdate) {
			updateDisplay();
		}
	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

}
