/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

public class ThresholdSubtractFloatSlider extends ThresholdFloatSlider {
	@Override
	public String operationName() {
		return "Threshold Float Slider Subtract Background";
	}

	@Override
	protected float operation(float pixelValue, float lowThreshold, float highThreshold, float a, float b) {
		return Math.max(pixelValue - lowThreshold, 0);
	}
}
