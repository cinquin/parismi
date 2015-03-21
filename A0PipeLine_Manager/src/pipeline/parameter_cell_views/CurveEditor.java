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
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.misc_util.Utils;
import pipeline.parameter_cell_views.splines.SplineDisplay;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SplineParameter;

public class CurveEditor extends AbstractParameterCellView implements ParameterListener {

	private static final long serialVersionUID = 1L;

	private JLabel parameterName;

	private boolean silenceUpdate = true;

	private SplineParameter currentParameter;

	private SplineDisplay splineEditor;

	public CurveEditor() {
		super();

		setPreferredSize(new Dimension(500, 500));

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.gridheight = 2;

		parameterName = new JLabel("");
		splineEditor = new SplineDisplay();

		splineEditor.addPropertyChangeListener(evt -> {
			currentParameter.setValue(new Object[] { splineEditor.getCurvePoints(), splineEditor.getControlPoints() });
			boolean saveSilenceUpdate = silenceUpdate;
			silenceUpdate = true;
			currentParameter.fireValueChanged(false, true, true);// The parameter value might still be
				// changing, but the information is not passed up from the SplineDisplay
				silenceUpdate = saveSilenceUpdate;
			});

		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		add(splineEditor, c);
		c.gridwidth = 1;
		c.ipady = 0;
		parameterName = new JLabel("parameter");
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 0.01;
		c.weightx = 0.0;
		parameterName.setMaximumSize(new Dimension(10000, 30));
		add(parameterName, c);
	}

	private boolean evenTableRow;

	@SuppressWarnings("unchecked")
	@Override
	protected Component getRendererOrEditorComponent(JTable table, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {

		if (table != null) {
			Dimension d = getPreferredSize();
			d.width = table.getColumnModel().getColumn(column).getWidth();
			setSize(d);
		}

		if (currentParameter != null) {
			currentParameter.removeListener(this);
		}
		currentParameter = (SplineParameter) value;
		currentParameter.addGUIListener(this);
		splineEditor.setCurvePoints((ArrayList<Point2D>) ((Object[]) currentParameter.getValue())[0]);
		splineEditor.setControlPoints((ArrayList<Point2D>) ((Object[]) currentParameter.getValue())[1]);
		silenceUpdate = true;
		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow) {
			this.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		} else
			this.setBackground(Utils.COLOR_FOR_ODD_ROWS);

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

	@SuppressWarnings("unchecked")
	private void updateDisplay() {
		Boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;

		parameterName.setText(currentParameter.getParamNameDescription()[0]);
		parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
		this.setToolTipText(currentParameter.getParamNameDescription()[1]);

		splineEditor.setCurvePoints((ArrayList<Point2D>) ((Object[]) currentParameter.getValue())[0]);
		splineEditor.setControlPoints((ArrayList<Point2D>) ((Object[]) currentParameter.getValue())[1]);

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
