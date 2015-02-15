package processing_utilities.pcurves.Paintable;

import java.awt.Color;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;

import processing_utilities.pcurves.AWT.ChoiceLabelPanel;
import processing_utilities.pcurves.AWT.ColorChoice;
import processing_utilities.pcurves.AWT.ScrollbarLabelPanel;
import processing_utilities.pcurves.AWT.ScrollbarTextFieldPanel;

final public class PaintableObject {
	public PaintableInterface objectToPaint;
	public boolean background;
	public String name;
	public String title;
	public Color color;
	public String type;
	public int pixelSize;

	public PaintableObject(boolean in_background, String in_name, String in_title, String in_color, String in_type,
			int in_pixelSize) {
		background = in_background;
		name = in_name;
		color = ColorChoice.GetColor(in_color);
		type = in_type;
		title = in_title;
		pixelSize = in_pixelSize;
	}

	final public void Paint(Graphics g, DataCanvas canvas) {
		if (objectToPaint != null) {
			g.setColor(color);
			objectToPaint.Paint(g, canvas, pixelSize, type);
		}
	}

	final public void PrintToPostScript(DataPostScriptDocument ps) {
		if (objectToPaint != null) {
			ps.SetColor(color, pixelSize);
			objectToPaint.PrintToPostScript(ps, pixelSize, type, title);
		}
	}

	final public PaintableObjectCustomizingPanel GetCustomizingPanel(DataCanvas dataCanvas) {
		return new PaintableObjectCustomizingPanel(this, dataCanvas);
	}

	final static String[] CHOICE_LABEL_PANEL_ITEMS = { "Circle", "Diamond", "Square", "Disc", "DepthDisc" };

	class PaintableObjectCustomizingPanel extends Panel {
		/**
	 * 
	 */
		private static final long serialVersionUID = 1L;
		Label label;
		ChoiceLabelPanel typeChoiceLabelPanel;
		ColorChoice colorChoicePanel;
		ChoiceLabelPanel colorChoiceLabelPanel;
		ScrollbarLabelPanel sizeScrollbarLabelPanel;
		PaintableObject paintableObject;
		DataCanvas dataCanvas;

		public PaintableObjectCustomizingPanel(PaintableObject in_paintableObject, DataCanvas in_dataCanvas) {
			paintableObject = in_paintableObject;
			dataCanvas = in_dataCanvas;
			setLayout(new GridLayout(0, 1));
			label = new Label(paintableObject.name, Label.CENTER);
			setBackground(paintableObject.color);
			setForeground(ColorChoice.GetColor("Black"));
			add(label);
			typeChoiceLabelPanel = new ChoiceLabelPanel("Type", Label.LEFT, true);
			if (!paintableObject.type.equals("")) {
				for (String CHOICE_LABEL_PANEL_ITEM : CHOICE_LABEL_PANEL_ITEMS)
					typeChoiceLabelPanel.addItem(CHOICE_LABEL_PANEL_ITEM);
				typeChoiceLabelPanel.select(paintableObject.type);
				add(typeChoiceLabelPanel);
			}
			colorChoicePanel = new ColorChoice(paintableObject.color);
			colorChoiceLabelPanel = new ChoiceLabelPanel(colorChoicePanel, "Color", Label.LEFT, true);
			add(colorChoiceLabelPanel);
			sizeScrollbarLabelPanel =
					new ScrollbarLabelPanel("Size", Label.LEFT, paintableObject.pixelSize, 0, 100, true);
			add(sizeScrollbarLabelPanel);
			validate();
		}

		@SuppressWarnings("deprecation")
		@Override
		public boolean handleEvent(Event event) {
			if (event.target == sizeScrollbarLabelPanel && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
				paintableObject.pixelSize = (Integer) event.arg;
				dataCanvas.Repaint();
			} else if (event.target == colorChoicePanel) {
				paintableObject.color = colorChoicePanel.getSelectedColor();
				setBackground(paintableObject.color);
				dataCanvas.Repaint();
			} else if (event.target == typeChoiceLabelPanel) {
				paintableObject.type = typeChoiceLabelPanel.getSelectedItem();
				dataCanvas.Repaint();
			}
			return super.handleEvent(event);
		}
	}
}
