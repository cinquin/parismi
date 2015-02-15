/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views.splines;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class SplineDisplay extends EquationDisplay {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final double CONTROL_POINT_SIZE = 12.0;

	private Point2D control1 = new Point2D.Double(0.25, 0.75);
	private Point2D control2 = new Point2D.Double(0.75, 0.25);

	private Point2D curve1 = new Point2D.Double(0, 0);
	private Point2D curve2 = new Point2D.Double(1, 1);

	private List<Point2D> curvePoints = new ArrayList<>(10);

	public List<Point2D> getCurvePoints() {
		return curvePoints;
	}

	private List<Point2D> controlPoints = new ArrayList<>(10);

	public List<Point2D> getControlPoints() {
		return controlPoints;
	}

	private List<Point2D> allPoints = new ArrayList<>(10);

	{
		controlPoints.add(control1);
		controlPoints.add(control2);
		curvePoints.add(curve1);
		curvePoints.add(curve2);
		allPoints.addAll(controlPoints);
		allPoints.addAll(curvePoints);
	}

	private Point2D selected = null;
	private Point dragStart = null;

	private boolean isSaving = false;

	private PropertyChangeSupport support;

	public SplineDisplay() {
		super(0.0, 0.0, -0.1, 1.1, -0.1, 1.1, 0.2, 6, 0.2, 6);

		setEnabled(true);

		addMouseMotionListener(new ControlPointsHandler());
		addMouseListener(new SelectionHandler());

		support = new PropertyChangeSupport(this);
	}

	public void setControlPoints(List<Point2D> newPoints) {
		allPoints.clear();
		controlPoints = newPoints;
		allPoints.addAll(controlPoints);
		allPoints.addAll(curvePoints);
		control1 = newPoints.get(0);
		control2 = newPoints.get(1);
		if (!allPoints.contains(selected)) {
			selected = null;
			dragStart = null;
		}
		repaint();
	}

	public void setCurvePoints(List<Point2D> newPoints) {
		allPoints.clear();
		curvePoints = newPoints;
		allPoints.addAll(controlPoints);
		allPoints.addAll(curvePoints);
		curve1 = newPoints.get(0);
		curve2 = newPoints.get(1);
		if (!allPoints.contains(selected)) {
			selected = null;
			dragStart = null;
		}
		repaint();
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		support.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		support.removePropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		support.addPropertyChangeListener(propertyName, listener);
	}

	@Override
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		support.removePropertyChangeListener(propertyName, listener);
	}

	public Point2D getControl1() {
		return (Point2D) control1.clone();
	}

	public Point2D getControl2() {
		return (Point2D) control2.clone();
	}

	public void setControl1(Point2D control1) {
		controlPoints.remove(this.control1);
		allPoints.remove(this.control1);
		support.firePropertyChange("control1", this.control1.clone(), control1.clone());
		this.control1 = (Point2D) control1.clone();
		controlPoints.add(control1);
		allPoints.add(control1);
		repaint();
	}

	public void setControl2(Point2D control2) {
		controlPoints.remove(this.control2);
		allPoints.remove(this.control2);
		support.firePropertyChange("control2", this.control2.clone(), control2.clone());
		this.control2 = (Point2D) control2.clone();
		controlPoints.add(control2);
		allPoints.add(control2);
		repaint();
	}

	@Override
	protected void paintInformation(Graphics2D g2) {
		if (!isSaving) {
			paintAllPoints(g2);
		}
		paintSpline(g2);
	}

	private void paintAllPoints(Graphics2D g2) {
		for (Point2D p : allPoints)
			paintPoint(g2, p);

		for (int i = 0; i < curvePoints.size(); i++) {
			Point2D curvePoint = curvePoints.get(i);
			Point2D controlPoint = controlPoints.get(i);
			g2.drawLine((int) xPositionToPixel(curvePoint.getX()), (int) yPositionToPixel(curvePoint.getY()),
					(int) xPositionToPixel(controlPoint.getX()), (int) yPositionToPixel(controlPoint.getY()));
		}
	}

	private void paintPoint(Graphics2D g2, Point2D control) {
		double origin_x = xPositionToPixel(control.getX());
		double origin_y = yPositionToPixel(control.getY());
		// double pos = control == control1 ? 0.0 : 1.0;

		Ellipse2D outer = getDraggableArea(control);
		Ellipse2D inner =
				new Ellipse2D.Double(origin_x + 2.0 - CONTROL_POINT_SIZE / 2.0, origin_y + 2.0 - CONTROL_POINT_SIZE
						/ 2.0, 8.0, 8.0);

		Area circle = new Area(outer);
		circle.subtract(new Area(inner));

		Stroke stroke = g2.getStroke();
		g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 5, new float[] { 5, 5 }, 0));
		g2.setColor(new Color(1.0f, 0.0f, 0.0f, 0.4f));
		// g2.drawLine(0, (int) origin_y, (int) origin_x, (int) origin_y);
		// g2.drawLine((int) origin_x, (int) origin_y, (int) origin_x, getHeight());
		g2.setStroke(stroke);

		if (selected == control) {
			g2.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
		} else {
			g2.setColor(new Color(0.8f, 0.8f, 0.8f, 0.6f));
		}
		g2.fill(inner);

		g2.setColor(new Color(0.0f, 0.0f, 0.5f, 0.5f));
		g2.fill(circle);

		// g2.drawLine((int) origin_x, (int) origin_y,
		// (int) xPositionToPixel(pos), (int) yPositionToPixel(pos));
	}

	private Ellipse2D getDraggableArea(Point2D control) {
		Ellipse2D outer =
				new Ellipse2D.Double(xPositionToPixel(control.getX()) - CONTROL_POINT_SIZE / 2.0,
						yPositionToPixel(control.getY()) - CONTROL_POINT_SIZE / 2.0, CONTROL_POINT_SIZE,
						CONTROL_POINT_SIZE);
		return outer;
	}

	private void paintSpline(Graphics2D g2) {
		CubicCurve2D spline =
				new CubicCurve2D.Double(xPositionToPixel(curve1.getX()), yPositionToPixel(curve1.getY()),
						xPositionToPixel(control1.getX()), yPositionToPixel(control1.getY()), xPositionToPixel(control2
								.getX()), yPositionToPixel(control2.getY()), xPositionToPixel(curve2.getX()),
						yPositionToPixel(curve2.getY()));

		g2.setColor(new Color(0.0f, 0.3f, 0.0f, 1.0f));
		g2.draw(spline);
	}

	private void resetSelection() {
		Point2D oldSelected = selected;
		selected = null;

		if (oldSelected != null) {
			Rectangle bounds = getDraggableArea(oldSelected).getBounds();
			repaint(bounds.x, bounds.y, bounds.width, bounds.height);
		}
	}

	private class ControlPointsHandler extends MouseMotionAdapter {
		@Override
		public void mouseMoved(MouseEvent e) {

			boolean found = false;
			for (Point2D p : allPoints) {
				if (getDraggableArea(p).contains(e.getPoint())) {
					found = true;
				}
			}
			if (found)
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			else {
				setCursor(Cursor.getDefaultCursor());
			}
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			// Utils.log("mouse dragged",LogLevel.VERBOSE_VERBOSE_DEBUG);
			if (selected == null) {
				return;
			}
			Utils.log("selected not null", LogLevel.VERBOSE_VERBOSE_DEBUG);

			Point dragEnd = e.getPoint();

			double distance = xPixelToPosition(dragEnd.getX()) - xPixelToPosition(dragStart.getX());
			double x = selected.getX() + distance;
			if (x < 0.0) {
				x = 0.0;
			} else if (x > 1.0) {
				x = 1.0;
			}

			distance = yPixelToPosition(dragEnd.getY()) - yPixelToPosition(dragStart.getY());
			double y = selected.getY() + distance;
			if (y < 0.0) {
				y = 0.0;
			} else if (y > 1.0) {
				y = 1.0;
			}

			Point2D selectedCopy = (Point2D) selected.clone();
			selected.setLocation(x, y);
			support.firePropertyChange("control" + (selected == control1 ? "1" : "2"), selectedCopy, selected.clone());

			repaint();

			double xPos = xPixelToPosition(dragEnd.getX());
			double yPos = -yPixelToPosition(dragEnd.getY());

			if (xPos >= 0.0 && xPos <= 1.0) {
				dragStart.setLocation(dragEnd.getX(), dragStart.getY());
			}
			if (yPos >= 0.0 && yPos <= 1.0) {
				dragStart.setLocation(dragStart.getX(), dragEnd.getY());
			}
		}
	}

	private class SelectionHandler extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {

			boolean found = false;
			for (Point2D p : allPoints) {
				if (getDraggableArea(p).contains(e.getPoint())) {
					found = true;
					selected = p;
					dragStart = e.getPoint();
					Rectangle bounds = getDraggableArea(p).getBounds();
					repaint(bounds.x, bounds.y, bounds.width, bounds.height);

				}
			}

			if (!found)
				resetSelection();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			resetSelection();
		}
	}
}
