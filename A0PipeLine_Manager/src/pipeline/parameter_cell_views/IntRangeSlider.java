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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.IntRangeParameter;
import pipeline.parameters.ParameterListener;

public class IntRangeSlider extends AbstractParameterCellView implements ParameterListener {

	private static final long serialVersionUID = 1L;

	private RangeSlider slider;
	private JTextField textMinimum;
	private JTextField textMaximum;
	private JTextField currentTextValue0, currentTextValue1;
	private JLabel parameterName;
	private JButton resetMin, resetMax, resetRange;
	private JTable table;
	private int tableRow;

	private int maximum;
	private int minimum;
	private int currentValue0, currentValue1;
	private JPanel textValueFrame;

	private IntRangeParameter currentParameter;

	private boolean silenceUpdate;

	private NumberFormat nf = NumberFormat.getInstance();

	@Override
	protected NumberFormat getNumberFormatter() {
		return nf;
	}

	public IntRangeSlider() {
		super();
		addMouseWheelListener(e -> {
			int rotation = e.getWheelRotation();
			int change = (int) ((currentValue1 - currentValue0 + 1) * rotation * Utils.getMouseWheelClickFactor());

			int newCurrentValue0 = currentValue0;
			int newCurrentValue1 = currentValue1;
			int saveIntMinimum = minimum;
			int saveIntMaximum = maximum;

			newCurrentValue0 += change;
			newCurrentValue1 += change;

			if (!((e.getModifiers() & java.awt.event.InputEvent.ALT_MASK) > 0)) {
				if (newCurrentValue1 > saveIntMaximum) {
					int difference = newCurrentValue1 - newCurrentValue0;
					newCurrentValue1 = saveIntMaximum;
					newCurrentValue0 = newCurrentValue1 - difference;
					if (newCurrentValue0 < 0)
						Utils.log("<0", LogLevel.WARNING);
				}
				if (newCurrentValue0 < saveIntMinimum) {
					int difference = newCurrentValue1 - newCurrentValue0;
					newCurrentValue0 = saveIntMinimum;
					newCurrentValue1 = newCurrentValue0 + difference;
					if (newCurrentValue0 < 0)
						Utils.log("<0", LogLevel.WARNING);
				}
			} else {
			}

			currentParameter.setValue(new int[] { newCurrentValue0, newCurrentValue1, saveIntMinimum, saveIntMaximum });

			updateValues();
			currentParameter.fireValueChanged(false, false, true);
		});
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(3);
		nf.setMaximumIntegerDigits(12);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		parameterName = new JLabel("");

		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 0.0;
		c.gridwidth = 4;
		add(Box.createRigidArea(new Dimension(0, 5)), c);

		slider = new RangeSlider(0, 20);
		slider.addChangeListener(new sliderListener());
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 1.0;
		c.weightx = 0.0;
		c.gridwidth = 4;
		add(slider, c);
		c.gridwidth = 1;

		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 4;
		Component comp = Box.createRigidArea(new Dimension(0, 10));
		((JComponent) comp).setOpaque(true);
		add(comp, c);
		c.gridwidth = 1;

		currentTextValue0 = new JTextField("" + slider.getValue());
		currentTextValue1 = new JTextField("" + slider.getUpperValue());
		currentTextValue0.addActionListener(new textBoxListenerTriggersUpdate());
		currentTextValue1.addActionListener(new textBoxListenerTriggersUpdate());
		Font smallerFont =
				new Font(currentTextValue0.getFont().getName(), currentTextValue0.getFont().getStyle(),
						currentTextValue0.getFont().getSize() - 2);
		textMinimum = new JTextField("0");
		textMinimum.setFont(smallerFont);
		textMinimum.addActionListener(new minimumListener());
		textMaximum = new JTextField("50");
		textMaximum.setFont(smallerFont);
		textMaximum.addActionListener(new maximumListener());
		textMaximum.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				if (!silenceUpdate) {
					slider.setMinimum(parseTextBox(textMinimum).intValue());
					minimum = slider.getMinimum();
					slider.setMaximum(parseTextBox(textMaximum).intValue());
					maximum = slider.getMaximum();
					slider.setValue(parseTextBox(currentTextValue0).intValue());
					slider.setUpperValue(parseTextBox(currentTextValue1).intValue());
					currentValue0 = slider.getValue();
					currentValue1 = slider.getUpperValue();

					currentParameter.setValue(new int[] { currentValue0, currentValue1, minimum, maximum });
				}
			}

		});

		textValueFrame = new JPanel();
		textValueFrame.setBackground(getBackground());
		textValueFrame.setLayout(new GridBagLayout());

		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 0.1;
		textValueFrame.add(textMinimum, c);

		c.gridx = 1;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 0.3;
		textValueFrame.add(currentTextValue0, c);

		c.gridx = 2;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 0.3;
		textValueFrame.add(currentTextValue1, c);

		c.gridx = 3;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 0.1;
		textValueFrame.add(textMaximum, c);

		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 1.0;
		c.weightx = 0.3;
		c.gridwidth = 4;
		add(textValueFrame, c);
		c.gridwidth = 1;

		parameterName = new JLabel("parameter");
		c.gridx = 0;
		c.gridy = 4;
		c.weighty = 1.0;
		c.weightx = 0.01;
		c.gridwidth = 1;
		add(parameterName, c);

		resetMin = new JButton("Min");
		resetMin.setActionCommand("Reset Min");
		resetMin.addActionListener(new buttonListener());
		resetMax = new JButton("Max");
		resetMax.setActionCommand("Reset Max");
		resetMax.addActionListener(new buttonListener());
		resetRange = new JButton("MinMax");
		resetRange.setActionCommand("Reset Range");
		resetRange.addActionListener(new buttonListener());

		c.gridx = 1;
		c.gridy = 4;
		c.weighty = 1.0;
		c.weightx = 0.2;
		c.gridwidth = 1;
		add(resetMin, c);

		c.gridx = 2;
		c.gridy = 4;
		c.weighty = 1.0;
		c.weightx = 0.2;
		c.gridwidth = 1;
		add(resetMax, c);

		c.gridx = 3;
		c.gridy = 4;
		c.weighty = 1.0;
		c.weightx = 0.2;
		c.gridwidth = 1;
		add(resetRange, c);

	}

	private class buttonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			currentParameter.buttonPressed(e.getActionCommand(), false, null);
			// now update text boxes and slider in "silenced" mode
			int[] intValues = (int[]) (currentParameter.getValue());
			silenceUpdate = true;
			slider.setValue(intValues[0]);
			slider.setUpperValue(intValues[1]);
			updateTextValue(currentTextValue0, intValues[0]);
			updateTextValue(currentTextValue1, intValues[1]);
			updateTextValue(textMinimum, intValues[2]);
			updateTextValue(textMaximum, intValues[3]);
			minimum = intValues[2];
			maximum = intValues[3];
			slider.setMaximum(maximum);
			slider.setMinimum(minimum);
			parameterName.setText(currentParameter.getParamNameDescription()[0]);
			parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
			textMinimum.setEditable(currentParameter.editable()[0]);
			textMaximum.setEditable(currentParameter.editable()[1]);
			setToolTipText(currentParameter.getParamNameDescription()[1]);
			int height_wanted = (int) getPreferredSize().getHeight();
			if (height_wanted > table.getRowHeight(tableRow))
				table.setRowHeight(tableRow, height_wanted);

			silenceUpdate = false;
		}
	}

	private class minimumListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				minimum = parseTextBox(textMinimum).intValue();
				currentParameter.updateBounds(minimum, maximum);
				updateValues();
			}
		}
	}

	private class maximumListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				maximum = parseTextBox(textMaximum).intValue();
				currentParameter.updateBounds(minimum, maximum);
				updateValues();
			}
		}
	}

	private class textBoxListenerTriggersUpdate implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!silenceUpdate) {
				JTextField source = (JTextField) e.getSource();
				try {
					minimum = slider.getMinimum();
					maximum = slider.getMaximum();
					currentValue0 = parseTextBox(currentTextValue0).intValue();
					currentValue1 = parseTextBox(currentTextValue1).intValue();
				} catch (NumberFormatException nfe) {
					Utils.log("cant parse " + source.getText() + " as an int; ignoring", LogLevel.ERROR);
					return;
				}
				currentParameter.setValue(new int[] { currentValue0, currentValue1, minimum, maximum });
				updateParameterValuesButNotMainTextBox(); // In case parameter adjusted minimum or maximum
				currentParameter.fireValueChanged(false, false, true);
			}
		}
	}

	private transient boolean wasChanging = false;

	private class sliderListener implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent e) {
			if (!silenceUpdate) {
				if (!slider.getValueIsAdjusting()) {
					if ((currentValue0 == slider.getValue()) && (currentValue1 == slider.getUpperValue())) {
						if (wasChanging) {
							currentParameter.fireValueChanged(false, false, true);
							wasChanging = false;
						}
						return;
					}
					currentValue0 = slider.getValue();
					currentValue1 = slider.getUpperValue();
					currentParameter.setValue(new int[] { currentValue0, currentValue1, minimum, maximum });
					updateParameterValuesButNotMainTextBox(); // In case parameter adjusted minimum or maximum
					currentParameter.fireValueChanged(false, false, true);
				} else { // value is still adjusting
					wasChanging = true;
					if ((currentValue0 == slider.getValue()) && (currentValue1 == slider.getUpperValue()))
						return;
					currentValue0 = slider.getValue();
					currentValue1 = slider.getUpperValue();
					currentTextValue0.setText("" + nf.format(currentValue0));
					currentTextValue1.setText("" + nf.format(currentValue1));
					currentParameter.setValue(new int[] { currentValue0, currentValue1, minimum, maximum });
					updateParameterValuesButNotMainTextBox(); // In case parameter adjusted minimum or maximum
					currentParameter.fireValueChanged(true, false, true);
				}
			}
		}
	}

	private void updateTextValue(JTextField f, int v) {
		f.setText("" + nf.format(v));
	}

	private boolean evenTableRow;

	private void updateValues() {
		boolean saveSilenceUpdate = silenceUpdate;

		try {
			int[] intValues = (int[]) (currentParameter.getValue());
			silenceUpdate = true;

			updateTextValue(currentTextValue0, intValues[0]);
			updateTextValue(currentTextValue1, intValues[1]);
			updateTextValue(textMinimum, intValues[2]);
			updateTextValue(textMaximum, intValues[3]);
			minimum = intValues[2];
			maximum = intValues[3];
			currentValue0 = intValues[0];
			currentValue1 = intValues[1];

			slider.setMaximum(maximum);
			slider.setMinimum(minimum);
			slider.setValue(intValues[0]);
			slider.setUpperValue(intValues[1]);
			revalidate();
		} finally {
			silenceUpdate = saveSilenceUpdate;
		}
	}

	/**
	 * Read parameter value, min, and max and update our display.
	 */
	private void updateParameterValuesButNotMainTextBox() {
		int[] intValues = (int[]) (currentParameter.getValue());
		currentValue0 = intValues[0];
		currentValue1 = intValues[1];
		minimum = intValues[2];
		maximum = intValues[3];

		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;
		updateTextValue(textMinimum, minimum);
		updateTextValue(textMaximum, maximum);

		slider.setMaximum(maximum);
		slider.setMinimum(minimum);
		slider.setValue(currentValue0);
		slider.setUpperValue(currentValue1);
		slider.repaint();

		silenceUpdate = saveSilenceUpdate;
	}

	@Override
	protected Component getRendererOrEditorComponent(JTable table0, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {

		if (currentParameter != null) {
			currentParameter.removeListener(this);
		}

		currentParameter = (IntRangeParameter) value;
		currentParameter.addGUIListener(this);

		table = table0;
		tableRow = row;

		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow) {
			this.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		} else
			this.setBackground(Utils.COLOR_FOR_ODD_ROWS);
		textValueFrame.setBackground(getBackground());

		updateValues();

		parameterName.setText(currentParameter.getParamNameDescription()[0]);
		parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
		textMinimum.setEditable(currentParameter.editable()[0]);
		textMaximum.setEditable(currentParameter.editable()[1]);
		this.setToolTipText(currentParameter.getParamNameDescription()[1]);
		int height_wanted = (int) getPreferredSize().getHeight();
		if (height_wanted > table0.getRowHeight(row))
			table0.setRowHeight(row, height_wanted);
		if (!rendererCalled) {
			slider.setBounds(table0.getCellRect(row, column, false));
			slider.updateUI();
		}

		silenceUpdate = false;
		return this;
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	private void updateDisplay() {
		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;

		updateValues();

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
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

}
