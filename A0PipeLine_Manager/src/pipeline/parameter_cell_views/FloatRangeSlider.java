/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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

import org.apache.commons.lang3.text.WordUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.ParameterListener;

public class FloatRangeSlider extends AbstractParameterCellView implements MouseListener, MouseMotionListener,
		ParameterListener {

	private static final long serialVersionUID = 1L;

	private RangeSlider slider;
	private JTextField textMinimum;
	private JTextField textMaximum;
	private JTextField currentTextValue0, currentTextValue1;
	private JLabel parameterName;
	private JButton resetMin, resetMax, resetRange;
	private JTable table;
	private JPanel panelForHistogram;
	private int tableRow;

	private float maximum;
	private float minimum;
	private float currentValue0, currentValue1;
	private JPanel textValueFrame;

	private FloatRangeParameter currentParameter;

	private boolean silenceUpdate;

	private NumberFormat nf = NumberFormat.getInstance();

	private GridBagConstraints cForHistogram;

	@Override
	protected NumberFormat getNumberFormatter() {
		return nf;
	}

	public FloatRangeSlider() {
		super();
		addMouseWheelListener(e -> {

			int rotation = e.getWheelRotation();

			float[] float_values = (float[]) (currentParameter.getValue());
			currentValue0 = float_values[0];
			currentValue1 = float_values[1];
			minimum = float_values[2];
			maximum = float_values[3];

			float change = (currentValue1 - currentValue0 + 1) * rotation * Utils.getMouseWheelClickFactor();

			currentValue0 += change;
			currentValue1 += change;

			if (!((e.getModifiers() & java.awt.event.InputEvent.ALT_MASK) > 0)) {
				if (currentValue1 > maximum) {
					float difference = currentValue1 - currentValue0;
					currentValue1 = maximum;
					currentValue0 = currentValue1 - difference;
				}
				if (currentValue0 < minimum) {
					float difference = currentValue1 - currentValue0;
					currentValue0 = minimum;
					currentValue1 = currentValue0 + difference;
				}
			}

			currentParameter.setValue(new float[] { currentValue0, currentValue1, minimum, maximum });

			readInValuesFromParameter();
			updateDisplays();
			currentParameter.fireValueChanged(false, false, true);
		});
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(5);
		nf.setMaximumIntegerDigits(10);

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;

		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 4;

		cForHistogram = (GridBagConstraints) c.clone();

		panelForHistogram = new JPanel();
		panelForHistogram.setPreferredSize(new Dimension(200, 150));
		panelForHistogram.setLayout(new BorderLayout());

		c.gridx = 0;
		c.gridy = 1;// 1
		c.weighty = 0.0;
		c.weightx = 0.0;
		c.gridwidth = 4;
		add(Box.createRigidArea(new Dimension(0, 5)), c);

		slider = new RangeSlider(0, 20);
		slider.addChangeListener(new sliderListener());
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = 0.0;
		c.weightx = 0.0;
		c.gridwidth = 4;
		add(slider, c);

		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.0;
		c.weightx = 1.0;
		c.gridwidth = 4;
		Component comp = Box.createRigidArea(new Dimension(0, 10));
		((JComponent) comp).setOpaque(true);
		add(comp, c);
		c.gridwidth = 1;

		final textBoxListener minMaxListener = new textBoxListener();

		currentTextValue0 = new JTextField("");
		currentTextValue1 = new JTextField("");
		currentTextValue0.addActionListener(new textBoxListenerTriggersUpdate());
		currentTextValue1.addActionListener(new textBoxListenerTriggersUpdate());
		Font smallerFont =
				new Font(currentTextValue0.getFont().getName(), currentTextValue0.getFont().getStyle(),
						currentTextValue0.getFont().getSize() - 2);
		textMinimum = new JTextField("0");
		textMinimum.setFont(smallerFont);
		textMinimum.addActionListener(minMaxListener);
		textMaximum = new JTextField("50");
		textMaximum.setFont(smallerFont);
		textMaximum.addActionListener(minMaxListener);
		textMaximum.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				minMaxListener.actionPerformed(new ActionEvent(textMaximum, 0, ""));
			}
		});
		textMinimum.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				minMaxListener.actionPerformed(new ActionEvent(textMinimum, 0, ""));
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
		c.gridy = 4;
		c.weighty = 0.0;
		c.weightx = 0.3;
		c.gridwidth = 4;
		add(textValueFrame, c);
		c.gridwidth = 1;

		parameterName = new JLabel("parameter");
		c.gridx = 0;
		c.gridy = 5;
		c.weighty = 0.0;
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
		c.gridy = 5;
		c.weighty = 0.0;
		c.weightx = 0.2;
		c.gridwidth = 1;
		add(resetMin, c);

		c.gridx = 2;
		c.gridy = 5;
		c.weighty = 0.0;
		c.weightx = 0.2;
		c.gridwidth = 1;
		add(resetMax, c);

		c.gridx = 3;
		c.gridy = 5;
		c.weighty = 0.0;
		c.weightx = 0.2;
		c.gridwidth = 1;
		add(resetRange, c);
		// ,resetMax,resetRange;

	}

	private class buttonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			currentParameter.buttonPressed(e.getActionCommand(), false, null);
			// now update text boxes and slider in "silenced" mode
			float[] float_values = (float[]) (currentParameter.getValue());
			silenceUpdate = true;

			currentValue0 = float_values[0];
			currentValue1 = float_values[1];
			minimum = float_values[2];
			maximum = float_values[3];
			updateDisplayAndParameter(false, false);

			parameterName.setText(currentParameter.getParamNameDescription()[0]);
			parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
			textMinimum.setEditable(currentParameter.editable()[0]);
			textMaximum.setEditable(currentParameter.editable()[1]);
			setToolTipText(Utils.encodeHTML(WordUtils.wrap(currentParameter.getParamNameDescription()[1], 50, null, true)).
					replace("\n", "<br>\n"));
			if (table != null) {
				int height_wanted = (int) getPreferredSize().getHeight();
				if (height_wanted > table.getRowHeight(tableRow))
					table.setRowHeight(tableRow, height_wanted);
			}
			silenceUpdate = false;
		}
	}

	private class textBoxListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (silenceUpdate)
				return;
			JTextField source = (JTextField) e.getSource();
			try {
				minimum = parseTextBox(textMinimum).floatValue();
				maximum = parseTextBox(textMaximum).floatValue();
				currentParameter.updateBounds(minimum, maximum);
				readInValuesFromParameter();
				currentParameter.setValue(new float[] { currentValue0, currentValue1, minimum, maximum });
				// Utils.log("Changed values to "+minimum+"-"+maximum+", "+currentValue0+"-"+currentValue1,LogLevel.VERBOSE_DEBUG);
				updateDisplayAndParameter(false, false);
			} catch (NumberFormatException nfe) {
				Utils.log("cant parse something, maybe " + source.getText() + ", as a float; ignoring",
						LogLevel.WARNING);
			}
		}
	}

	private class textBoxListenerTriggersUpdate implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (silenceUpdate)
				return;
			JTextField source = (JTextField) e.getSource();
			try {
				currentValue0 = parseTextBox(currentTextValue0).floatValue();
				currentValue1 = parseTextBox(currentTextValue1).floatValue();
				// Utils.log("Changed values to "+minimum+"-"+maximum+", "+currentValue0+"-"+currentValue1,LogLevel.VERBOSE_DEBUG);
				currentParameter.setValue(new float[] { currentValue0, currentValue1, minimum, maximum });
				readInValuesFromParameter();
				updateDisplayAndParameter(true, false);

			} catch (NumberFormatException nfe) {
				Utils.log("cant parse something, maybe " + source.getText() + ", as a float; ignoring",
						LogLevel.WARNING);
			}
		}
	}

	private class sliderListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			if (silenceUpdate) {
				return;
			}
			currentValue0 = (slider.getValue()) / 100f;
			currentValue1 = (slider.getUpperValue()) / 100f;
			updateDisplayAndParameter(true, slider.getValueIsAdjusting());
		}
	}

	/**
	 * Updates a text field displaying a number
	 * 
	 * @param f
	 *            JTextField displaying the number
	 * @param v
	 *            Number to format and display
	 */
	private void updateTextValue(JTextField f, float v) {
		f.setText("" + nf.format(v));
	}

	private boolean evenTableRow;
	private ChartPanel chartPanel;
	private JFreeChart chart;
	private IntervalMarker selectionRange;

	/**
	 * Called whenever currentValue0, currentValue1, minimum, or maximum have been updates
	 * Updates all elements in the GUI.
	 * 
	 * @param triggerParameterUpdate
	 *            Trigger a callback to the parameter and therefore to the pipeline
	 * @param stillChanging
	 *            True if parameter is still changing (ie if the user is still dragging the slider of the histogram
	 *            interval marker
	 */
	private void updateDisplayAndParameter(boolean triggerParameterUpdate, boolean stillChanging) {
		if (silenceUpdate)
			return;
		currentParameter.setValue(new float[] { currentValue0, currentValue1, minimum, maximum });

		silenceUpdate = true;

		updateDisplays();

		silenceUpdate = false;
		if (triggerParameterUpdate)
			currentParameter.fireValueChanged(stillChanging, false, true);
	}

	private void readInValuesFromParameter() {
		float[] float_values = (float[]) (currentParameter.getValue());
		currentValue0 = float_values[0];
		currentValue1 = float_values[1];
		minimum = float_values[2];
		maximum = float_values[3];
	}

	private void updateDisplays() {
		boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;
		readInValuesFromParameter();
		slider.setValue((int) (currentValue0 * 100f));
		slider.setUpperValue((int) (currentValue1 * 100f));
		slider.setMaximum((int) (maximum * 100f));
		slider.setMinimum((int) (minimum * 100f));
		slider.setValue((int) (currentValue0 * 100f));
		slider.setUpperValue((int) (currentValue1 * 100f));
		parameterName.setText(currentParameter.getParamNameDescription()[0]);
		parameterName.setVisible(!currentParameter.getParamNameDescription()[0].equals(""));
		textMinimum.setEditable(currentParameter.editable()[0]);
		textMaximum.setEditable(currentParameter.editable()[1]);
		this.setToolTipText(currentParameter.getParamNameDescription()[1]);

		if (chart != null) {
			final XYPlot plot = chart.getXYPlot();
			if (plot != null) {
				updatePlot();
			}
		}

		updateTextValue(currentTextValue0, currentValue0);
		updateTextValue(currentTextValue1, currentValue1);
		updateTextValue(textMinimum, minimum);
		updateTextValue(textMaximum, maximum);

		if (table != null) {
			int height_wanted = (int) getPreferredSize().getHeight();
			if (height_wanted > table.getRowHeight(tableRow))
				table.setRowHeight(tableRow, height_wanted);
		}

		revalidate();

		silenceUpdate = saveSilenceUpdate;
	}

	private void updatePlot() {

		final XYPlot plot = chart.getXYPlot();
		final NumberAxis domainAxis = new NumberAxis(null);
		domainAxis.setAutoRange(false);
		domainAxis.setTickLabelFont(new Font("Times", 0, 20));
		domainAxis.setLowerBound(minimum);
		domainAxis.setUpperBound(maximum);
		plot.setDomainAxis(domainAxis);

		if (selectionRange != null) { // if we're displaying a histogram
			selectionRange.setStartValue(currentValue0);
			selectionRange.setEndValue(currentValue1);
		}

	}

	@Override
	protected Component getRendererOrEditorComponent(JTable table0, @NonNull Object value, boolean isSelected,
			boolean hasFocus, int row, int column, boolean rendererCalled) {
		if (currentParameter != null) {
			currentParameter.removeListener(this);
		}

		currentParameter = (FloatRangeParameter) value;
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

		silenceUpdate = true;

		readInValuesFromParameter();

		if (currentParameter.histogram == null)
			;// currentParameter.histogram=new XYSeries("");

		if (currentParameter.histogram instanceof XYSeries) {

			XYSeries xyData = (XYSeries) currentParameter.histogram;

			chart = ChartFactory.createXYLineChart(null, // chart title
					null,// "Category", // domain axis label
					null,// "Value", // range axis label
					new XYSeriesCollection(xyData), // data
					PlotOrientation.VERTICAL, false, // include legend
					true, false);

		} else if (currentParameter.histogram != null) { // assume for now it's a histogram

			String plotTitle = "Histogram";
			String xaxis = "number";
			String yaxis = "value";
			PlotOrientation orientation = PlotOrientation.VERTICAL;
			boolean show = false;
			boolean toolTips = false;
			boolean urls = false;
			chart =
					ChartFactory.createHistogram(plotTitle, xaxis, yaxis,
							(IntervalXYDataset) currentParameter.histogram, orientation, show, toolTips, urls);// dataset
		}

		if (currentParameter.histogram != null) {
			add(panelForHistogram, cForHistogram);
			final XYPlot plot = chart.getXYPlot();
			final NumberAxis domainAxis = new NumberAxis(null);
			domainAxis.setAutoRange(false);
			domainAxis.setTickLabelFont(new Font("Times", 0, 20));
			domainAxis.setLowerBound(minimum);
			domainAxis.setUpperBound(maximum);
			plot.setDomainAxis(domainAxis);

			final NumberAxis rangeAxis = new NumberAxis(null);
			rangeAxis.setAutoRange(true);
			rangeAxis.setVisible(false);
			plot.setRangeAxis(rangeAxis);
			chart.setBackgroundPaint(Color.white);
			chart.setPadding(new RectangleInsets(0, 0, 0, 0));

			plot.setBackgroundImage(null);
			plot.setBackgroundPaint(Color.white);
			plot.setOutlinePaint(Color.black);

			if (chartPanel == null) {
				chartPanel = new ChartPanel(chart);
				chartPanel.addMouseListener(this);
				chartPanel.addMouseMotionListener(this);
				chartPanel.setMouseWheelEnabled(true);
				chartPanel.setMouseZoomable(false);
				chartPanel.setRangeZoomable(false);

				panelForHistogram.add(chartPanel);

			} else
				chartPanel.setChart(chart);

			chartPanel.setSize(panelForHistogram.getSize());

			((XYPlot) chart.getPlot()).getRenderer().setSeriesStroke(0, new BasicStroke(5.0f));
			selectionRange = new IntervalMarker(currentValue0, currentValue1);
			((XYPlot) chart.getPlot()).addDomainMarker(selectionRange);
		} else {
			remove(panelForHistogram);
			setMaximumSize(new Dimension(700, 50));
			setPreferredSize(new Dimension(700, 50));
		}

		updateDisplays();

		silenceUpdate = false;
		return this;
	}

	@Override
	public Object getCellEditorValue() {
		return currentParameter;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	private boolean trackingChartMouseDrag = false;
	private int markerIntervalEdgeSelected;// 0 for left side, 1 for right side

	/**
	 * Translates MouseEvent screen coordinates to coordinates in chart units
	 * 
	 * @param e
	 *            MouseEvent containing coordinates of interest
	 * @return Chart coordinates (evaluated as double)
	 */
	private Point2D getChartCoordinates(MouseEvent e) {
		if (chartPanel.getChartRenderingInfo().getChartArea().getHeight() == 0)
			Utils.log("Cannot translate to chart coordinates", LogLevel.ERROR);// throw new
																				// RuntimeException("Cannot translate to chart coordinates");
		int mouseX = e.getX();
		int mouseY = e.getY();
		Utils.log("x = " + mouseX + ", y = " + mouseY, LogLevel.VERBOSE_DEBUG);
		Point2D p = chartPanel.translateScreenToJava2D(new Point(mouseX, mouseY));
		XYPlot plot = (XYPlot) chart.getPlot();
		Rectangle2D plotArea = this.chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
		ValueAxis domainAxis = plot.getDomainAxis();
		RectangleEdge domainAxisEdge = plot.getDomainAxisEdge();
		ValueAxis rangeAxis = plot.getRangeAxis();
		RectangleEdge rangeAxisEdge = plot.getRangeAxisEdge();
		double chartX = domainAxis.java2DToValue(p.getX(), plotArea, domainAxisEdge);
		double chartY = rangeAxis.java2DToValue(p.getY(), plotArea, rangeAxisEdge);
		return (new Point2D.Double(chartX, chartY));
	}

	void moveIntervalEdgeToMousePosition(MouseEvent e) {
		if (!trackingChartMouseDrag)
			return; // is this necessary?
		if (silenceUpdate)
			return;
		Point2D chartCoordinates = getChartCoordinates(e);

		if (markerIntervalEdgeSelected == 0) {
			// prevent the minimum and maximum from sliding past each other
			if (chartCoordinates.getX() > selectionRange.getEndValue())
				return;
			currentValue0 = (float) chartCoordinates.getX();
		} else {
			// prevent the minimum and maximum from sliding past each other
			if (chartCoordinates.getX() < selectionRange.getStartValue())
				return;
			currentValue1 = (float) chartCoordinates.getX();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		trackingChartMouseDrag = true;
		if (silenceUpdate)
			return;

		Point2D chartCoordinates = getChartCoordinates(e);
		double distanceToIntervalLeft = Math.abs(chartCoordinates.getX() - currentValue0);
		double distanceToIntervalRight = Math.abs(chartCoordinates.getX() - currentValue1);

		markerIntervalEdgeSelected = (distanceToIntervalLeft < distanceToIntervalRight) ? 0 : 1;
		moveIntervalEdgeToMousePosition(e);
		updateDisplayAndParameter(true, true);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!trackingChartMouseDrag)
			return; // is this necessary?
		if (silenceUpdate)
			return;
		moveIntervalEdgeToMousePosition(e);

		trackingChartMouseDrag = false;
		if (silenceUpdate)
			return;

		updateDisplayAndParameter(true, false);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!trackingChartMouseDrag)
			return; // is this necessary?
		if (silenceUpdate)
			return;
		moveIntervalEdgeToMousePosition(e);
		updateDisplayAndParameter(true, true);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	private void updateDisplay() {
		Boolean saveSilenceUpdate = silenceUpdate;
		silenceUpdate = true;

		readInValuesFromParameter();
		updateDisplays();

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
