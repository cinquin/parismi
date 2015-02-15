/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util.interpolation;

import java.awt.event.ActionEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.List;

import pipeline.parameters.AbstractParameter;
import pipeline.parameters.BooleanParameter;
import pipeline.parameters.FloatRangeParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SplineParameter;

public class ScaledSplineInterpolator implements Interpolator1D, ParameterListener {

	private List<Point2D> curvePoints, controlPoints;
	private FloatRangeParameter yRange;
	private FloatRangeParameter xRange;

	private BooleanParameter compressX;
	private FloatRangeParameter actualXRange;
	private SplineParameter splineParameter;

	private Object readResolve() {
		xRange.addPluginListener(this);
		yRange.addPluginListener(this);
		compressX.addPluginListener(this);
		splineParameter.addPluginListener(this);

		return this;
	}

	public ScaledSplineInterpolator(SplineParameter splineParameter, FloatRangeParameter xRangeParameter,
			FloatRangeParameter yRangeParameter, BooleanParameter compressXParameter) {

		curvePoints = splineParameter.getCurvePoints();
		controlPoints = splineParameter.getControlPoints();
		xRange = xRangeParameter;
		yRange = yRangeParameter;
		compressX = compressXParameter;
		this.splineParameter = splineParameter;

		xRange.addPluginListener(this);
		yRange.addPluginListener(this);
		compressX.addPluginListener(this);
		splineParameter.addPluginListener(this);

		resetInterpolator();
	}

	public List<Point2D> getCurvePoints() {
		return curvePoints;
	}

	public void setCurvePoints(List<Point2D> curvePoints) {
		this.curvePoints = curvePoints;
		resetInterpolator();
	}

	public List<Point2D> getControlPoints() {
		return controlPoints;
	}

	public void setControlPoints(List<Point2D> controlPoints) {
		this.controlPoints = controlPoints;
		resetInterpolator();
	}

	public FloatRangeParameter getyRange() {
		return yRange;
	}

	public void setyRange(FloatRangeParameter yRange) {
		if (this.yRange != null)
			(this.yRange).removeListener(this);
		this.yRange = yRange;
		yRange.addPluginListener(this);
		resetInterpolator();
	}

	public FloatRangeParameter getxRange() {
		return xRange;
	}

	public void setxRange(FloatRangeParameter xRange) {
		if (this.xRange != null)
			(this.xRange).removeListener(this);
		this.xRange = xRange;
		xRange.addPluginListener(this);
		resetInterpolator();
	}

	public BooleanParameter getCompressX() {
		return compressX;
	}

	public void setCompressX(BooleanParameter compressX) {
		if (this.compressX != null)
			(this.compressX).removeListener(this);
		this.compressX = compressX;
		compressX.addPluginListener(this);
	}

	public FloatRangeParameter getActualXRange() {
		return actualXRange;
	}

	public void setActualXRange(FloatRangeParameter actualXRange) {
		this.actualXRange = actualXRange;
	}

	// private Interpolator spline;

	private float x0, x1, y0, y1, yRange0, yRange1;

	private void resetInterpolator() {
		/*
		 * spline = new SplineInterpolator((float) controlPoints.get(0).getX(),
		 * (float) controlPoints.get(0).getY(),
		 * (float) controlPoints.get(1).getX(),
		 * (float) controlPoints.get(1).getY());
		 */
		if (xRange != null) {
			x0 = xRange.lowValue();
			x1 = xRange.highValue();
		} else {
			x0 = (float) curvePoints.get(0).getX();
			x1 = (float) curvePoints.get(1).getX();
		}

		y0 = (float) curvePoints.get(0).getY();
		y1 = (float) curvePoints.get(1).getY();

		if (yRange != null) {
			yRange0 = yRange.lowValue();// +y0*(yRange.highValue()-yRange.lowValue());
			yRange1 = yRange.highValue();// +y1*(yRange.highValue()-yRange.lowValue());
		} else {
			yRange0 = y0;
			yRange1 = y1;
		}

	}

	@Override
	public float getInterpolatedY(float x) {
		float scaledX = x;

		if (compressX.getBooleanValue()) {
			if (actualXRange == null)
				throw new IllegalStateException(
						"Interpolator set to compress the x range but actual x range has not been set");
			scaledX = (x - actualXRange.lowValue()) / (actualXRange.highValue() - actualXRange.lowValue());
		} else {
			scaledX = (x - x0) / (x1 - x0);
		}

		double f;

		if (scaledX < 0)
			f = y0;
		else if (scaledX > 1)
			f = y1;
		else {
			// f= spline.interpolate(scaledX);
			double[] eqn = new double[4];
			double[] res = new double[4];
			fillEqn(eqn, scaledX, curvePoints.get(0).getX(), controlPoints.get(0).getX(), controlPoints.get(1).getX(),
					curvePoints.get(1).getX());
			int num = CubicCurve2D.solveCubic(eqn, res);
			if (num != 1) {
				// Utils.log("Not just 1 solution",LogLevel.WARNING);
			}
			num =
					evalCubic(res, num, true, true, null, curvePoints.get(0).getY(), controlPoints.get(0).getY(),
							controlPoints.get(1).getY(), curvePoints.get(1).getY());
			f = res[0];
		}

		f = yRange0 + f * (yRange1 - yRange0);

		if (f == 0) {
			// Utils.log("0",LogLevel.VERBOSE_DEBUG);
		}

		return (float) f;
	}

	/*
	 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
	 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
	 */

	/*
	 * Fill an array with the coefficients of the parametric equation
	 * in t, ready for solving against val with solveCubic.
	 * We currently have:
	 * <pre>
	 * val = P(t) = C1(1-t)^3 + 3CP1 t(1-t)^2 + 3CP2 t^2(1-t) + C2 t^3
	 * = C1 - 3C1t + 3C1t^2 - C1t^3 +
	 * 3CP1t - 6CP1t^2 + 3CP1t^3 +
	 * 3CP2t^2 - 3CP2t^3 +
	 * C2t^3
	 * 0 = (C1 - val) +
	 * (3CP1 - 3C1) t +
	 * (3C1 - 6CP1 + 3CP2) t^2 +
	 * (C2 - 3CP2 + 3CP1 - C1) t^3
	 * 0 = C + Bt + At^2 + Dt^3
	 * C = C1 - val
	 * B = 3*CP1 - 3*C1
	 * A = 3*CP2 - 6*CP1 + 3*C1
	 * D = C2 - 3*CP2 + 3*CP1 - C1
	 * </pre>
	 */
	private static void fillEqn(double eqn[], double val, double c1, double cp1, double cp2, double c2) {
		eqn[0] = c1 - val;
		eqn[1] = (cp1 - c1) * 3.0;
		eqn[2] = (cp2 - cp1 - cp1 + c1) * 3.0;
		eqn[3] = c2 + (cp1 - cp2) * 3.0 - c1;
		return;
	}

	/*
	 * Evaluate the t values in the first num slots of the vals[] array
	 * and place the evaluated values back into the same array. Only
	 * evaluate t values that are within the range <0, 1>, including
	 * the 0 and 1 ends of the range iff the include0 or include1
	 * booleans are true. If an "inflection" equation is handed in,
	 * then any points which represent a point of inflection for that
	 * cubic equation are also ignored.
	 */
	private static int evalCubic(double vals[], int num, boolean include0, boolean include1, double inflect[],
			double c1, double cp1, double cp2, double c2) {
		int j = 0;
		for (int i = 0; i < num; i++) {
			double t = vals[i];
			if ((include0 ? t >= 0 : t > 0) && (include1 ? t <= 1 : t < 1)
					&& (inflect == null || inflect[1] + (2 * inflect[2] + 3 * inflect[3] * t) * t != 0)) {
				double u = 1 - t;
				vals[j++] = c1 * u * u * u + 3 * cp1 * t * u * u + 3 * cp2 * t * t * u + c2 * t * t * t;
			}
		}
		return j;
	}

	@Override
	public void parameterValueChanged(boolean stillChanging, AbstractParameter parameterWhoseValueChanged,
			boolean keepQuiet) {
		resetInterpolator();
	}

	@Override
	public void parameterPropertiesChanged(AbstractParameter parameterWhosePropertiesChanged) {
		resetInterpolator();
	}

	@Override
	public void buttonPressed(String commandName, AbstractParameter parameter, ActionEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean alwaysNotify() {
		return false;
	}

	@Override
	public String getParameterName() {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void setParameterName(String name) {
		throw new RuntimeException("Unimplemented");
	}

}
