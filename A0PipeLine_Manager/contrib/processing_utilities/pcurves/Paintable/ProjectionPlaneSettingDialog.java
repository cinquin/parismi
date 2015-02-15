package processing_utilities.pcurves.Paintable;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Dialog;
import java.awt.Event;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;

import processing_utilities.pcurves.AWT.ScrollbarTextFieldPanel;

final public class ProjectionPlaneSettingDialog extends Dialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Panel panel;
	private Button doneButton;
	private ProjectionPlane projectionPlane;
	private Recallable recallable;

	public ProjectionPlaneSettingDialog(Frame frame, ProjectionPlane in_projectionPlane, Recallable in_recallable) {
		super(frame, "Projection plane", false);
		recallable = in_recallable;
		Constructor(in_projectionPlane);
	}

	public ProjectionPlaneSettingDialog(Frame frame, ProjectionPlane in_projectionPlane) {
		super(frame, "Projection plane", false);
		Constructor(in_projectionPlane);
	}

	private void Constructor(ProjectionPlane in_projectionPlane) {
		projectionPlane = in_projectionPlane;
		panel = new Panel();
		setLayout(new BorderLayout());
		add("South", panel);
		doneButton = new Button("Done");
		add("North", doneButton);
		SetupPanel();
		validate();
		pack();
	}

	private int dimension;

	private Panel settingsPanel;
	private Panel originPanel;
	private Panel axisXPanel;
	private Panel axisYPanel;
	private ScrollbarTextFieldPanel[] originPanels;
	private ScrollbarTextFieldPanel[] axisXPanels;
	private ScrollbarTextFieldPanel[] axisYPanels;
	private static int FACTOR = 100; // resetable by applications
	private static int MAX_XY = 103;
	private static int MAX_ORIGIN = 1000;

	private Panel controlPanel;
	private Button resetButton;
	private Button firstTwoPrincipalComponentsButton;
	private Checkbox normalizeCheckbox;
	private Checkbox orthogonalizeCheckbox;
	private Checkbox repaintCheckbox;

	private void SetupPanel() {
		dimension = projectionPlane.axisX.Dimension();

		settingsPanel = new Panel();
		settingsPanel.setLayout(new GridLayout(1, 0));
		axisXPanel = new Panel();
		axisXPanel.setLayout(new GridLayout(0, 1));
		axisXPanel.add(new Label("X axis                        "));
		axisYPanel = new Panel();
		axisYPanel.setLayout(new GridLayout(0, 1));
		axisYPanel.add(new Label("Y axis                        "));
		originPanel = new Panel();
		originPanel.setLayout(new GridLayout(0, 1));
		originPanel.add(new Label("Origin                        "));

		axisXPanels = new ScrollbarTextFieldPanel[dimension];
		axisYPanels = new ScrollbarTextFieldPanel[dimension];
		originPanels = new ScrollbarTextFieldPanel[dimension];
		String space;
		if (dimension <= 10)
			space = "  ";
		else if (dimension <= 100)
			space = "   ";
		else
			space = "    ";
		for (int i = 0; i < dimension; i++) {
			axisXPanels[i] =
					new ScrollbarTextFieldPanel(space + i + ":", (int) (projectionPlane.axisX.GetCoords(i) * FACTOR),
							-MAX_XY + 3, MAX_XY, true, FACTOR);
			axisXPanel.add(axisXPanels[i]);
			axisYPanels[i] =
					new ScrollbarTextFieldPanel(space + i + ":", (int) (projectionPlane.axisY.GetCoords(i) * FACTOR),
							-MAX_XY + 3, MAX_XY, true, FACTOR);
			axisYPanel.add(axisYPanels[i]);
			originPanels[i] =
					new ScrollbarTextFieldPanel(space + i + ":", (int) (projectionPlane.origin.GetCoords(i) * FACTOR),
							-MAX_ORIGIN, MAX_ORIGIN, true, FACTOR);
			originPanel.add(originPanels[i]);
		}

		settingsPanel.add(axisXPanel);
		settingsPanel.add(axisYPanel);
		settingsPanel.add(originPanel);
		panel.add(settingsPanel);

		controlPanel = new Panel();
		controlPanel.setLayout(new GridLayout(1, 0));

		resetButton = new Button("Reset");
		firstTwoPrincipalComponentsButton = new Button("First 2 PC's");
		normalizeCheckbox = new Checkbox("Normalize", false);
		orthogonalizeCheckbox = new Checkbox("Orthogonalize", false);
		repaintCheckbox = new Checkbox("Repaint", true);

		controlPanel.add(resetButton);
		controlPanel.add(firstTwoPrincipalComponentsButton);
		controlPanel.add(normalizeCheckbox);
		controlPanel.add(orthogonalizeCheckbox);
		controlPanel.add(repaintCheckbox);
		add("Center", controlPanel);
	}

	private void ConditionalRepaint() {
		if (repaintCheckbox.getState()) {
			try {
				recallable.Recall();
			} catch (NullPointerException e) {
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	final public boolean action(Event event, Object arg) {
		// Done
		if (event.target == doneButton) {
			postEvent(new Event(this, Event.WINDOW_DESTROY, ""));
		} else if (event.target == resetButton) {
			projectionPlane.Reset(dimension);
			UpdateScrollbars();
			ConditionalRepaint();
		} else if (event.target == firstTwoPrincipalComponentsButton) {
			projectionPlane.SetToFirstTwoPrincipalComponents();
			UpdateScrollbars();
			ConditionalRepaint();
		} else if (event.target == normalizeCheckbox) {
			if (normalizeCheckbox.getState()) {
				projectionPlane.Normalize();
				UpdateScrollbars();
				ConditionalRepaint();
			}
		} else if (event.target == orthogonalizeCheckbox) {
			if (orthogonalizeCheckbox.getState()) {
				projectionPlane.Orthogonalize(0); // X axis is unchanged
				UpdateScrollbars();
				ConditionalRepaint();
			}
		} else if (event.target == repaintCheckbox) {
			ConditionalRepaint();
		}
		return true;
	}

	private boolean realEvent = true;
	private int countNonrealEvents;

	@SuppressWarnings("deprecation")
	@Override
	final public boolean handleEvent(Event event) {
		// Done
		if (event.id == Event.WINDOW_DESTROY) {
			dispose();
		} else {
			for (int i = 0; i < dimension; i++) {
				if (event.target == axisXPanels[i] && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
					projectionPlane.axisX.SetCoord(i, ((Integer) event.arg) / (double) FACTOR);
					if (!realEvent) {
						if (countNonrealEvents == 3 * dimension)
							realEvent = true;
						else
							countNonrealEvents++;
					}
					if (realEvent) {
						if (orthogonalizeCheckbox.getState()) {
							projectionPlane.Orthogonalize(0); // X axis is unchanged
						}
						if (normalizeCheckbox.getState()) {
							projectionPlane.Normalize();
						}
						if (orthogonalizeCheckbox.getState() || normalizeCheckbox.getState())
							UpdateScrollbars();
						if (repaintCheckbox.getState()) {
							try {
								recallable.Recall();
							} catch (NullPointerException e) {
							}
						}
					}
					i = dimension;
				} else if (event.target == axisYPanels[i] && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
					projectionPlane.axisY.SetCoord(i, ((Integer) event.arg) / (double) FACTOR);
					if (!realEvent) {
						if (countNonrealEvents == 3 * dimension)
							realEvent = true;
						else
							countNonrealEvents++;
					}
					if (realEvent) {
						if (orthogonalizeCheckbox.getState()) {
							projectionPlane.Orthogonalize(1); // Y axis is unchanged
						}
						if (normalizeCheckbox.getState()) {
							projectionPlane.Normalize();
						}
						if (orthogonalizeCheckbox.getState() || normalizeCheckbox.getState())
							UpdateScrollbars();
						if (repaintCheckbox.getState()) {
							try {
								recallable.Recall();
							} catch (NullPointerException e) {
							}
						}
					}
					i = dimension;
				} else if (event.target == originPanels[i] && ScrollbarTextFieldPanel.ScrollbarEvent(event)) {
					projectionPlane.origin.SetCoord(i, ((Integer) event.arg) / (double) FACTOR);
					if (!realEvent) {
						if (countNonrealEvents == 3 * dimension)
							realEvent = true;
						else
							countNonrealEvents++;
					}
					if (realEvent && repaintCheckbox.getState()) {
						try {
							recallable.Recall();
						} catch (NullPointerException e) {
						}
					}
					i = dimension;
				}
			}
		}
		return super.handleEvent(event);
	}

	final void UpdateScrollbars() {
		realEvent = false;
		countNonrealEvents = 0;
		for (int i = 0; i < dimension; i++) {
			originPanels[i].setValue((int) (projectionPlane.origin.GetCoords(i) * FACTOR));
			axisXPanels[i].setValue((int) (projectionPlane.axisX.GetCoords(i) * FACTOR));
			axisYPanels[i].setValue((int) (projectionPlane.axisY.GetCoords(i) * FACTOR));
		}
	}
}
