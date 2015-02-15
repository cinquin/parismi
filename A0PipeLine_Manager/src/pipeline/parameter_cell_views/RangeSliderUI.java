/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * UI delegate for the RangeSlider component. RangeSliderUI paints two thumbs,
 * one for the lower value and one for the upper value.
 * 
 * @author Ernie Yu, LimeWire LLC
 *         Seems to be under the GPL
 *         Adjustments by Olivier Cinquin to allow range to move as a whole, range than individual bounds,
 *         to allow each knob to move past the other, and to fix JTable behavior.
 */
class RangeSliderUI extends BasicSliderUI {

	/** Color of selected range. */
	private Color rangeColor = Color.GREEN;

	/** Location and size of thumb for upper value. */
	private Ellipse2D.Float upperThumbRect;// was Rectangle
	/** Indicator that determines whether upper thumb is selected. */
	private boolean upperThumbSelected;

	/** Indicator set when lower thumb is being dragged. */
	private transient boolean lowerDragging;
	/** Indicator set when upper thumb is being dragged. */
	private transient boolean upperDragging;
	private transient boolean trackDragging;
	private int offsetX, offsetY;

	/**
	 * Constructs a RangeSliderUI for the specified slider component.
	 * 
	 * @param b
	 *            RangeSlider
	 */
	public RangeSliderUI(RangeSlider b) {
		super(b);
	}

	/**
	 * Installs this UI delegate on the specified component.
	 */
	@Override
	public void installUI(JComponent c) {
		// upperThumbRect = new Rectangle(); xxxx
		upperThumbRect = new Ellipse2D.Float();
		super.installUI(c);
	}

	/**
	 * Creates a listener to handle track events in the specified slider.
	 */
	@Override
	protected TrackListener createTrackListener(JSlider slider) {
		return new RangeTrackListener();
	}

	/**
	 * Creates a listener to handle change events in the specified slider.
	 */
	@Override
	protected ChangeListener createChangeListener(JSlider slider) {
		return new ChangeHandler();
	}

	/**
	 * Updates the dimensions for both thumbs.
	 */
	@Override
	protected void calculateThumbSize() {
		// Call superclass method for lower thumb size.
		super.calculateThumbSize();

		// Set upper thumb size.
		// upperThumbRect.setSize(thumbRect.width, thumbRect.height); xxxxxx
		upperThumbRect.width = thumbRect.width + 8;// NEED TO USE AN EVEN NUMBER OF PIXELS OR RANGE KEEPS GETTING
													// SHORTER AFTER EACH SLIDE
		upperThumbRect.height = thumbRect.height + 5;
	}

	/**
	 * Updates the locations for both thumbs.
	 */
	@Override
	protected void calculateThumbLocation() {
		// Call superclass method for lower thumb location.
		super.calculateThumbLocation();

		// Adjust upper value to snap to ticks if necessary.
		if (slider.getSnapToTicks()) {
			int upperValue = slider.getValue() + slider.getExtent();
			int snappedValue = upperValue;
			int majorTickSpacing = slider.getMajorTickSpacing();
			int minorTickSpacing = slider.getMinorTickSpacing();
			int tickSpacing = 0;

			if (minorTickSpacing > 0) {
				tickSpacing = minorTickSpacing;
			} else if (majorTickSpacing > 0) {
				tickSpacing = majorTickSpacing;
			}

			if (tickSpacing != 0) {
				// If it's not on a tick, change the value
				if ((upperValue - slider.getMinimum()) % tickSpacing != 0) {
					float temp = (float) (upperValue - slider.getMinimum()) / (float) tickSpacing;
					int whichTick = Math.round(temp);
					snappedValue = slider.getMinimum() + (whichTick * tickSpacing);
				}

				if (snappedValue != upperValue) {
					slider.setExtent(snappedValue - slider.getValue());
				}
			}
		}

		// Calculate upper thumb location. The thumb is centered over its
		// value on the track.
		if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
			int upperPosition = xPositionForValue(slider.getValue() + slider.getExtent());
			upperThumbRect.x = upperPosition - (upperThumbRect.width / 2);
			upperThumbRect.y = trackRect.y;

		} else {
			int upperPosition = yPositionForValue(slider.getValue() + slider.getExtent());
			upperThumbRect.x = trackRect.x;
			upperThumbRect.y = upperPosition - (upperThumbRect.height / 2);
		}
	}

	/**
	 * Returns the size of a thumb.
	 */
	@Override
	protected Dimension getThumbSize() {
		return new Dimension(12, 12);
	}

	/**
	 * Paints the slider. The lower thumb is always painted on top of the
	 * upper thumb.
	 */
	@Override
	public void paint(Graphics g, JComponent c) {
		super.paint(g, c);

		Rectangle clipRect = g.getClipBounds();
		// if (upperThumbSelected) {
		// Paint lower thumb first, then upper thumb.
		// if (clipRect.intersects(thumbRect)) {
		// paintLowerThumb(g);
		// }
		// if (upperThumbRect.intersects(clipRect)) { //(clipRect.intersects(upperThumbRect))
		// paintUpperThumb(g);
		// }

		// } else {
		// Paint upper thumb first, then lower thumb.
		if (upperThumbRect.intersects(clipRect)) { // (clipRect.intersects(upperThumbRect))
			paintUpperThumb(g);
		}
		if (clipRect.intersects(thumbRect)) {
			paintLowerThumb(g);
		}
		// }
	}

	/**
	 * Paints the track.
	 */
	@Override
	public void paintTrack(Graphics g) {
		// Draw track.
		super.paintTrack(g);

		Rectangle trackBounds = trackRect;

		if (slider.getOrientation() == SwingConstants.HORIZONTAL) {
			// Determine position of selected range by moving from the middle
			// of one thumb to the other.
			int lowerX = thumbRect.x + (thumbRect.width / 2);
			int upperX = (int) (upperThumbRect.x + (upperThumbRect.width / 2));

			// Determine track position.
			int cy = (trackBounds.height / 2) - 2;

			// Save color and shift position.
			Color oldColor = g.getColor();
			g.translate(trackBounds.x, trackBounds.y + cy);

			// Draw selected range.
			g.setColor(rangeColor);
			for (int y = 0; y <= 3; y++) {
				g.drawLine(lowerX - trackBounds.x, y, upperX - trackBounds.x, y);
			}

			// Restore position and color.
			g.translate(-trackBounds.x, -(trackBounds.y + cy));
			g.setColor(oldColor);

		} else {
			// Determine position of selected range by moving from the middle
			// of one thumb to the other.
			int lowerY = thumbRect.y + (thumbRect.height / 2);
			int upperY = (int) (upperThumbRect.y + (upperThumbRect.height / 2));

			// Determine track position.
			int cx = (trackBounds.width / 2) - 2;

			// Save color and shift position.
			Color oldColor = g.getColor();
			g.translate(trackBounds.x + cx, trackBounds.y);

			// Draw selected range.
			g.setColor(rangeColor);
			for (int x = 0; x <= 3; x++) {
				g.drawLine(x, lowerY - trackBounds.y, x, upperY - trackBounds.y);
			}

			// Restore position and color.
			g.translate(-(trackBounds.x + cx), -trackBounds.y);
			g.setColor(oldColor);
		}
	}

	/**
	 * Overrides superclass method to do nothing. Thumb painting is handled
	 * within the <code>paint()</code> method.
	 */
	@Override
	public void paintThumb(Graphics g) {
		// Do nothing.
	}

	/**
	 * Paints the thumb for the lower value using the specified graphics object.
	 */
	private void paintLowerThumb(Graphics g) {
		Rectangle knobBounds = thumbRect;
		int w = knobBounds.width;
		int h = knobBounds.height;

		// Create graphics copy.
		Graphics2D g2d = (Graphics2D) g.create();

		// Create default thumb shape.
		Shape thumbShape = createThumbShape(w, h);

		// Draw thumb.
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.translate(knobBounds.x, knobBounds.y);

		g2d.setColor(Color.CYAN);
		g2d.fill(thumbShape);

		g2d.setColor(Color.BLUE);
		g2d.draw(thumbShape);

		// Dispose graphics.
		g2d.dispose();
	}

	/**
	 * Paints the thumb for the upper value using the specified graphics object.
	 */
	private void paintUpperThumb(Graphics g) {
		Ellipse2D.Float knobBounds = upperThumbRect;// xxx Rectangle knobBounds = upperThumbRect
		float w = knobBounds.width;
		float h = knobBounds.height;

		// Create graphics copy.
		Graphics2D g2d = (Graphics2D) g.create();

		// Create default thumb shape.
		Shape thumbShape = createThumbShape(w, h);

		// Draw thumb.
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.translate(knobBounds.x, knobBounds.y);

		g2d.setColor(Color.PINK);
		g2d.fill(thumbShape);

		g2d.setColor(Color.RED);
		g2d.draw(thumbShape);

		// Dispose graphics.
		g2d.dispose();
	}

	/**
	 * Returns a Shape representing a thumb.
	 */

	private static Shape createThumbShape(double width, double height) {
		// Use circular shape.
		Ellipse2D shape = new Ellipse2D.Double(0, 0, width, height);
		return shape;
	}

	private static Shape createThumbShape(int width, int height) {
		// Use circular shape.
		Ellipse2D shape = new Ellipse2D.Double(0, 0, width, height);
		return shape;
	}

	/**
	 * Sets the location of the upper thumb, and repaints the slider. This is
	 * called when the upper thumb is dragged to repaint the slider. The <code>setThumbLocation()</code> method performs
	 * the same task for the
	 * lower thumb.
	 */
	private void setUpperThumbLocation(float x, float y) {
		// Rectangle upperUnionRect = new Rectangle();
		// upperUnionRect.setBounds(upperThumbRect);

		// upperThumbRect.setLocation(x, y);
		upperThumbRect.x = x;
		upperThumbRect.y = y;

		// SwingUtilities.computeUnion(upperThumbRect.x, upperThumbRect.y, upperThumbRect.width, upperThumbRect.height,
		// upperUnionRect);
		slider.repaint();// (upperUnionRect.x, upperUnionRect.y, upperUnionRect.width, upperUnionRect.height);
	}

	/**
	 * Moves the selected thumb in the specified direction by a block increment.
	 * This method is called when the user presses the Page Up or Down keys.
	 */
	@Override
	public void scrollByBlock(int direction) {
		synchronized (slider) {
			int blockIncrement = (slider.getMaximum() - slider.getMinimum()) / 10;
			if (blockIncrement <= 0 && slider.getMaximum() > slider.getMinimum()) {
				blockIncrement = 1;
			}
			int delta = blockIncrement * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);

			if (upperThumbSelected) {
				int oldValue = ((RangeSlider) slider).getUpperValue();
				((RangeSlider) slider).setUpperValue(oldValue + delta);
			} else {
				int oldValue = slider.getValue();
				slider.setValue(oldValue + delta);
			}
		}
	}

	/**
	 * Moves the selected thumb in the specified direction by a unit increment.
	 * This method is called when the user presses one of the arrow keys.
	 */
	@Override
	public void scrollByUnit(int direction) {
		synchronized (slider) {
			int delta = 1 * ((direction > 0) ? POSITIVE_SCROLL : NEGATIVE_SCROLL);

			if (upperThumbSelected) {
				int oldValue = ((RangeSlider) slider).getUpperValue();
				((RangeSlider) slider).setUpperValue(oldValue + delta);
			} else {
				int oldValue = slider.getValue();
				slider.setValue(oldValue + delta);
			}
		}
	}

	/**
	 * Listener to handle model change events. This calculates the thumb
	 * locations and repaints the slider if the value change is not caused by
	 * dragging a thumb.
	 */
	private class ChangeHandler implements ChangeListener {
		@Override
		public void stateChanged(ChangeEvent arg0) {
			if (!lowerDragging && !upperDragging && !trackDragging) {
				calculateThumbLocation();
				slider.repaint();
			}
		}
	}

	/**
	 * Listener to handle mouse movements in the slider track.
	 */
	public class RangeTrackListener extends TrackListener {

		@Override
		public void mousePressed(MouseEvent e) {

			calculateGeometry();// This is REQUIRED for proper behavior on first few clicks of a slider shown as a
								// JTable cell

			trackDragging = false;
			currentMouseX = e.getX();
			currentMouseY = e.getY();

			if (slider.isRequestFocusEnabled()) {
				slider.requestFocus();
			}

			boolean lowerPressed = false;
			boolean upperPressed = false;
			// Look for intersection with lower knob first because it is on top
			// of the higher one (displayed with a larger circle)
			if (thumbRect.contains(currentMouseX, currentMouseY)) {
				lowerPressed = true;
			} else if (upperThumbRect.contains(currentMouseX, currentMouseY)) {
				upperPressed = true;
			}

			// Handle lower thumb pressed.
			if (lowerPressed) {
				switch (slider.getOrientation()) {
					case SwingConstants.VERTICAL:
						offset = currentMouseY - thumbRect.y;
						break;
					case SwingConstants.HORIZONTAL:
						offset = currentMouseX - thumbRect.x;
						break;
					default:
						throw new IllegalStateException("Unknown slider orientation " + slider.getOrientation());
				}
				upperThumbSelected = false;
				lowerDragging = true;
				return;
			}
			lowerDragging = false;

			// Handle upper thumb pressed.
			if (upperPressed) {
				switch (slider.getOrientation()) {
					case SwingConstants.VERTICAL:
						offset = currentMouseY - (int) upperThumbRect.y;
						break;
					case SwingConstants.HORIZONTAL:
						offset = currentMouseX - (int) upperThumbRect.x;
						break;
					default:
						throw new IllegalStateException("Unknown slider orientation " + slider.getOrientation());
				}
				upperThumbSelected = true;
				upperDragging = true;
				return;
			}
			upperDragging = false;

			if (trackRect.contains(currentMouseX, currentMouseY)) {
				trackDragging = true;
				switch (slider.getOrientation()) {
					case SwingConstants.VERTICAL:
						offsetX = currentMouseY - thumbRect.y;
						break;
					case SwingConstants.HORIZONTAL:
						offsetX = currentMouseX - thumbRect.x;
						break;
					default:
						throw new IllegalStateException("Unknown slider orientation " + slider.getOrientation());
				}
				switch (slider.getOrientation()) {
					case SwingConstants.VERTICAL:
						offsetY = currentMouseY - (int) upperThumbRect.y;
						break;
					case SwingConstants.HORIZONTAL:
						offsetY = currentMouseX - (int) upperThumbRect.x;
						break;
					default:
						throw new IllegalStateException("Unknown slider orientation " + slider.getOrientation());
				}

			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			lowerDragging = false;
			upperDragging = false;
			trackDragging = false;
			slider.setValueIsAdjusting(false);
			super.mouseReleased(e);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (!slider.isEnabled()) {
				return;
			}

			currentMouseX = e.getX();
			currentMouseY = e.getY();

			if (lowerDragging) {
				slider.setValueIsAdjusting(true);
				moveLowerThumb();
			} else if (upperDragging) {
				slider.setValueIsAdjusting(true);
				moveUpperThumb();
			} else if (trackDragging) {
				slider.setValueIsAdjusting(true);

				// NEED TO AVOID DOUBLE NOTIFICATIONS by moveLowerThumb and moveUpperThumb
				// especially since they can generate ranges that the user never intended to have,
				// and that can cause lengthy operations (like projection of a Z-range much larger than
				// intended on large virtual stacks)
				BoundedRangeModelFineGrainedListenerNotification castModel =
						(BoundedRangeModelFineGrainedListenerNotification) (RangeSliderUI.this.slider.getModel());
				castModel.setSilenceUpdates(true);

				try {
					offset = offsetX;
					moveLowerThumb();
					offset = offsetY;
					moveUpperThumb();
				} finally {
					// Make sure if an exception is thrown the slider does not stay stuck in a state
					// where it won't respond to events other than track dragging
					castModel.setSilenceUpdates(false);
				}
				castModel.fireStateChanged(); // FIXME We are not checking that the values actually did change
			}
		}

		@Override
		public boolean shouldScroll(int direction) {
			return false;
		}

		/**
		 * Moves the location of the lower thumb, and sets its corresponding
		 * value in the slider.
		 */
		private void moveLowerThumb() {
			int thumbMiddle = 0;

			switch (slider.getOrientation()) {
				case SwingConstants.VERTICAL:
					int halfThumbHeight = thumbRect.height / 2;
					int thumbTop = currentMouseY - offset;
					int trackTop = trackRect.y;
					int trackBottom = trackRect.y + (trackRect.height - 1);
					int vMax = yPositionForValue(slider.getValue() + slider.getExtent());

					// Apply bounds to thumb position.
					if (drawInverted()) {
						trackBottom = vMax;
					} else {
						trackTop = vMax;
					}
					thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
					thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);

					setThumbLocation(thumbRect.x, thumbTop);

					// Update slider value.
					thumbMiddle = thumbTop + halfThumbHeight;
					slider.setValue(valueForYPosition(thumbMiddle));
					break;

				case SwingConstants.HORIZONTAL:
					int halfThumbWidth = thumbRect.width / 2;
					int thumbLeft = currentMouseX - offset;
					int trackLeft = trackRect.x;
					int trackRight = trackRect.x + (trackRect.width - 1);
					// int hMax = xPositionForValue(slider.getValue() + slider.getExtent());
					int hMax = xPositionForValue(slider.getMaximum());// THIS ALLOWS TO DRAG LOW END PAST HIGH END

					// Apply bounds to thumb position.
					if (drawInverted()) {
						trackLeft = hMax;
					} else {
						trackRight = hMax;
					}
					thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
					thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);

					setThumbLocation(thumbLeft, thumbRect.y);

					// Update slider value.
					thumbMiddle = thumbLeft + halfThumbWidth;
					slider.setValue(valueForXPosition(thumbMiddle));
					break;

				default:
					return;
			}
		}

		/**
		 * Moves the location of the upper thumb, and sets its corresponding
		 * value in the slider.
		 */
		private void moveUpperThumb() {
			int thumbMiddle = 0;

			switch (slider.getOrientation()) {
				case SwingConstants.VERTICAL:
					int halfThumbHeight = thumbRect.height / 2;
					int thumbTop = currentMouseY - offset;
					int trackTop = trackRect.y;
					int trackBottom = trackRect.y + (trackRect.height - 1);
					int vMin = yPositionForValue(slider.getValue());

					// Apply bounds to thumb position.
					if (drawInverted()) {
						trackTop = vMin;
					} else {
						trackBottom = vMin;
					}
					thumbTop = Math.max(thumbTop, trackTop - halfThumbHeight);
					thumbTop = Math.min(thumbTop, trackBottom - halfThumbHeight);

					setUpperThumbLocation(thumbRect.x, thumbTop);

					// Update slider extent.
					thumbMiddle = thumbTop + halfThumbHeight;
					slider.setExtent(valueForYPosition(thumbMiddle) - slider.getValue());
					break;

				case SwingConstants.HORIZONTAL:
					int halfThumbWidth = (int) (upperThumbRect.width / 2);
					int thumbLeft = currentMouseX - offset;
					int trackLeft = trackRect.x;
					int trackRight = trackRect.x + (trackRect.width - 1);
					// int hMin = xPositionForValue(slider.getValue());
					int hMin = xPositionForValue(slider.getMinimum());// THIS ALLOWS TO DRAG LOW END PAST HIGH END

					// Apply bounds to thumb position.
					if (drawInverted()) {
						trackRight = hMin;
					} else {
						trackLeft = hMin;
					}
					thumbLeft = Math.max(thumbLeft, trackLeft - halfThumbWidth);
					thumbLeft = Math.min(thumbLeft, trackRight - halfThumbWidth);

					setUpperThumbLocation(thumbLeft, thumbRect.y);

					// Update slider extent.
					thumbMiddle = thumbLeft + halfThumbWidth;
					int newExtent = valueForXPosition(thumbMiddle) - slider.getValue();
					if (newExtent >= 0)
						slider.setExtent(newExtent);
					else {
						slider.setExtent(0);
						slider.setValue(Math.max(slider.getMinimum(), valueForXPosition(thumbMiddle)));
					}
					break;

				default:
					return;
			}
		}
	}
}
