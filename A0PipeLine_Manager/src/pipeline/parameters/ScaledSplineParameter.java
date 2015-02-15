/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.interpolation.ScaledSplineInterpolator;

public class ScaledSplineParameter extends SplitParameter {
	private static final long serialVersionUID = -8157759090326811890L;

	public SplineParameter getSplineParameter() {
		return splineParameter;
	}

	public void setSplineParameter(SplineParameter splineParameter) {
		this.splineParameter = splineParameter;
		interpolator.setCurvePoints(splineParameter.getCurvePoints());
		interpolator.setControlPoints(splineParameter.getControlPoints());
	}

	public FloatRangeParameter getxRangeParameter() {
		return xRangeParameter;
	}

	public void setxRangeParameter(FloatRangeParameter xRangeParameter) {
		this.xRangeParameter = xRangeParameter;
		interpolator.setxRange(xRangeParameter);
	}

	public FloatRangeParameter getyRangeParameter() {
		return yRangeParameter;
	}

	public void setyRangeParameter(FloatRangeParameter yRangeParameter) {
		this.yRangeParameter = yRangeParameter;
		interpolator.setyRange(yRangeParameter);
	}

	public BooleanParameter getCompressXParameter() {
		return compressXParameter;
	}

	public void setCompressXParameter(BooleanParameter compressXParameter) {
		this.compressXParameter = compressXParameter;
		interpolator.setCompressX(compressXParameter);
	}

	private SplineParameter splineParameter;
	private FloatRangeParameter xRangeParameter, yRangeParameter;
	private BooleanParameter compressXParameter;

	private ScaledSplineInterpolator interpolator;

	public ScaledSplineParameter(String name, String explanation, boolean editable, boolean editableMax,
			boolean editableMin, ParameterListener listener) {
		splineParameter =
				new SplineParameter(name, explanation, null, null, editable, editableMax, editableMin, listener);
		xRangeParameter = new FloatRangeParameter("x range", "", 0, 1, 0, 1, editableMax, editableMin, listener, null);
		xRangeParameter.histogram = null;
		yRangeParameter =
				new FloatRangeParameter("y range", "", 0, 15, 0, 50, editableMax, editableMin, listener, null);
		yRangeParameter.histogram = null;
		compressXParameter = new BooleanParameter("Compress x", "", false, true, listener);

		interpolator =
				new ScaledSplineInterpolator(splineParameter, xRangeParameter, yRangeParameter, compressXParameter);

		parameters = new Object[] { splineParameter, xRangeParameter, yRangeParameter, compressXParameter };

		if (listener == null) {
			Utils.log("registering null listener", LogLevel.WARNING);
		}

		addPluginListener(listener);

	}

	public float getInterpolatedY(float x) {
		return interpolator.getInterpolatedY(x);
	}

}
