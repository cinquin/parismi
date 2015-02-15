package processing_utilities.pcurves.Paintable;

import java.awt.Checkbox;
import java.awt.Event;
import java.awt.GridLayout;
import java.awt.Panel;

import processing_utilities.pcurves.AWT.ScrollbarTextFieldPanel;

final class DataPostScriptSetupPanel extends Panel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static int MIN_MARGIN_RATE = -20; // percentage
	private final static int MAX_MARGIN_RATE = 100; // percentage
	private final static int MARGIN_RATE_FACTOR = 100;
	private final static int MAX_WIDTH_PROPORTION = 50;
	private final static int WIDTH_PROPORTION_FACTOR = 10;
	private final static int MAX_FONT_SIZE = 100;
	private ScrollbarTextFieldPanel marginRatePanel;
	private ScrollbarTextFieldPanel widthProportionPanel;
	private ScrollbarTextFieldPanel fontSizePanel;
	private Checkbox blackAndWhiteCheckbox;

	public DataPostScriptSetupPanel() {
		super();
		marginRatePanel =
				new ScrollbarTextFieldPanel("Margin rate: ",
						(int) (MARGIN_RATE_FACTOR * DataPostScriptDocument.marginRate), MIN_MARGIN_RATE,
						MAX_MARGIN_RATE, true, MARGIN_RATE_FACTOR);
		widthProportionPanel =
				new ScrollbarTextFieldPanel("Width proportion: ",
						(int) (WIDTH_PROPORTION_FACTOR * DataPostScriptDocument.widthProportion), 0,
						MAX_WIDTH_PROPORTION, true, WIDTH_PROPORTION_FACTOR);
		fontSizePanel =
				new ScrollbarTextFieldPanel("Font size: ", (int) (DataPostScriptDocument.fontSize), 0, MAX_FONT_SIZE,
						true, 1);
		blackAndWhiteCheckbox = new Checkbox("Black & white ");
		blackAndWhiteCheckbox.setState(DataPostScriptDocument.blackAndWhite);
		setLayout(new GridLayout(0, 1));
		add(marginRatePanel);
		add(widthProportionPanel);
		add(fontSizePanel);
		add(blackAndWhiteCheckbox);
		validate();
	}

	@Override
	final public boolean action(Event event, Object arg) {
		if (event.target == blackAndWhiteCheckbox) {
			DataPostScriptDocument.blackAndWhite = blackAndWhiteCheckbox.getState();
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	@Override
	final public boolean handleEvent(Event event) {
		if (event.target == marginRatePanel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
			DataPostScriptDocument.marginRate = (Integer) event.arg / (double) MARGIN_RATE_FACTOR;
		} else if (event.target == widthProportionPanel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
			DataPostScriptDocument.widthProportion = (Integer) event.arg / (double) WIDTH_PROPORTION_FACTOR;
		} else if (event.target == fontSizePanel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
			DataPostScriptDocument.fontSize = (Integer) event.arg;
		}
		return super.handleEvent(event);
	}
}
