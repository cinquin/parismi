/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import javax.swing.JSlider;

/**
 * An extension of JSlider to select a range of values using two thumb controls.
 * The thumb controls are used to select the lower and upper value of a range
 * with pre-determined minimum and maximum values.
 * 
 * <p>
 * RangeSlider makes use of the default BoundedRangeModel, which supports an inner range defined by a value and an
 * extent. The upper value returned by RangeSlider is simply the lower value plus the extent.
 * </p>
 * 
 * @author Ernie Yu, LimeWire LLC
 *         Adjustments by Olivier Cinquin to allow range to move as a whole,
 *         to allow each knob to move past the other, and to have each knob of a different
 *         size so that each can be grabbed even if they are at the same value
 */
class RangeSlider extends JSlider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a RangeSlider with default minimum and maximum values of 0
	 * and 100.
	 */
	public RangeSlider() {
		super(new BoundedRangeModelFineGrainedListenerNotification());
	}

	/**
	 * Constructs a RangeSlider with the specified default minimum and maximum
	 * values.
	 */
	public RangeSlider(int min, int max) {
		super(new BoundedRangeModelFineGrainedListenerNotification(min, 0, min, max));
	}

	/**
	 * Overrides the superclass method to install the UI delegate to draw two
	 * thumbs.
	 */
	@Override
	public void updateUI() {
		setUI(new RangeSliderUI(this));
		// Update UI for slider labels. This must be called after updating the
		// UI of the slider. Refer to JSlider.updateUI().
		updateLabelUIs();
	}

	/**
	 * Returns the lower value in the range.
	 */
	@Override
	public int getValue() {
		return super.getValue();
	}

	/**
	 * Sets the lower value in the range.
	 */
	@Override
	public void setValue(int value) {
		int oldValue = getValue();
		if (oldValue == value) {
			return;
		}

		// Compute new value and extent to maintain upper value.
		int oldExtent = getExtent();
		int newValue = Math.max(getMinimum(), value);// ALLOW THE MINIMUM TO PUSH UP THE MAXIMUM;
		int newExtent = oldExtent + oldValue - newValue;
		if (newExtent < 0)
			newExtent = 0;

		// Set new value and extent, and fire a single change event.
		getModel().setRangeProperties(newValue, newExtent, getMinimum(), getMaximum(), getValueIsAdjusting());
	}

	/**
	 * Returns the upper value in the range.
	 */
	public int getUpperValue() {
		return getValue() + getExtent();
	}

	/**
	 * Sets the upper value in the range.
	 */
	public void setUpperValue(int value) {
		// Compute new extent.
		int lowerValue = getValue();
		int newExtent = Math.max(getMinimum() - lowerValue, Math.min(value - lowerValue, getMaximum() - lowerValue)); // ALLOW
																														// MAX
																														// TO
																														// PUSH
																														// MINIMUM
																														// DOWN

		if (newExtent < 0) {// setExtent(0);
			getModel().setRangeProperties(lowerValue + newExtent, 0, getMinimum(), getMaximum(), getValueIsAdjusting());
		} else
			setExtent(newExtent);
	}
}
