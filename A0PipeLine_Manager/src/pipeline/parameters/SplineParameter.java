/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

// TODO Implement interpolation (but ScaledSplineInterpolator already does it)
public class SplineParameter extends AbstractParameter {
	private static final long serialVersionUID = 7941078520754004621L;

	private List<Point2D> curvePoints;

	public List<Point2D> getCurvePoints() {
		return curvePoints;
	}

	public List<Point2D> getControlPoints() {
		return controlPoints;
	}

	private List<Point2D> controlPoints;
	private boolean editableMax = true, editableMin = true;

	public boolean useExponentialFormat = false;

	public SplineParameter(String name, String explanation, List<Point2D> curvePoints0, List<Point2D> controlPoints0,
			boolean editable, boolean editableMax, boolean editableMin, ParameterListener listener) {
		super(listener, null);
		this.curvePoints = curvePoints0;
		this.controlPoints = controlPoints0;

		if (curvePoints == null) {
			curvePoints = new ArrayList<>();
		}
		if (curvePoints.size() == 0) {
			curvePoints.add(new Point2D.Double(0, 0));
			curvePoints.add(new Point2D.Double(1, 1));
		}

		if (controlPoints == null) {
			controlPoints = new ArrayList<>();
		}
		if (controlPoints.size() == 0) {
			controlPoints.add(new Point2D.Double(0.25, 0.75));
			controlPoints.add(new Point2D.Double(0.75, 0.25));
		}

		this.editable = editable;
		this.editableMax = editableMax;
		this.editableMin = editableMin;
		this.userDisplayName = name;
		this.explanation = explanation;
	}

	@Override
	public Object getValue() {
		Object[] array = { curvePoints, controlPoints };
		return array;
	}

	@Override
	public String[] getParamNameDescription() {
		String[] strings = { userDisplayName, explanation };
		return strings;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setValue(Object o) {
		curvePoints = (List<Point2D>) ((Object[]) o)[0];
		controlPoints = (List<Point2D>) ((Object[]) o)[1];
	}

	@Override
	public boolean[] editable() {
		boolean[] array = { editable, editableMin, editableMax };
		return array;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public boolean valueEquals(Object value) {
		throw new RuntimeException("Unimplemented");
	}

}
