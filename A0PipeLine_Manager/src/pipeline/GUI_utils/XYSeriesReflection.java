/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.jfree.data.xy.XYSeries;

import pipeline.data.ClickedPoint;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.Utils;
import pipeline.parameters.SpreadsheetCell;

/**
 * Used for graph display. Presents a subset of the data found in a PluginIOCells object. Uses reflection to access any
 * two fields designated by name.
 * Automatically listens to the PluginIOCells object the class was instantiated with to keep up-to-date. Maintains a
 * trendline
 * obtained by local smoothing, whose scale can be set by {@link #setSmoothingScale}. If scale is 0 no trendline is
 * computed.
 *
 */
public class XYSeriesReflection extends XYSeriesE {

	private static final long serialVersionUID = 1L;

	private transient Field clickedPointFieldForXValues;
	private transient Field clickedPointFieldForYValues;

	private transient Method clickedPointMethodForXValues;
	private transient Method clickedPointMethodForYValues;

	private int indexForXSeries;

	private int indexForYSeries;

	/**
	 * 
	 * @param name
	 * @param listIndex
	 *            if field is a list, index in the list to use; must be -1 otherwise
	 * @param p
	 *            instance representative of the underlying data objects that will be used with this series. This
	 *            instance
	 *            is used to determine whether the field should be access directly or through a getter method. If p is
	 *            null,
	 *            the field is accessed directly.
	 */
	public void setNameForXSeries(String name, int listIndex, ClickedPoint p) {
		nameForXSeries = name;
		indexForXSeries = listIndex;
		try {
			Method useGetterMethod = ClickedPoint.class.getDeclaredMethod("useGetter", new Class[] { String.class });
			Boolean useGetter;
			useGetter = p == null ? false : (Boolean) useGetterMethod.invoke(p, nameForXSeries);
			if (useGetter) {
				clickedPointMethodForXValues = ClickedPoint.class.getDeclaredMethod("get" + nameForXSeries);
				clickedPointMethodForXValues.setAccessible(true);
			} else {
				try {
					clickedPointFieldForXValues = ClickedPoint.class.getDeclaredField(nameForXSeries);
				} catch (NoSuchFieldException e) {
					nameForXSeries = nameForXSeries.substring(0, 1).toLowerCase() + nameForXSeries.substring(1);
					clickedPointFieldForXValues = ClickedPoint.class.getDeclaredField(nameForXSeries);
				}
				clickedPointFieldForXValues.setAccessible(true);
			}
		} catch (Exception e) {
			Utils.printStack(e);
		}
	}

	/**
	 * 
	 * @param name
	 * @param listIndex
	 *            if field is a list, index in the list to use; must be -1 otherwise
	 */
	public void setNameForYSeries(String name, int listIndex, ClickedPoint p) {
		nameForYSeries = name;
		indexForYSeries = listIndex;
		try {
			Method useGetterMethod = ClickedPoint.class.getDeclaredMethod("useGetter", new Class[] { String.class });
			Boolean useGetter;
			useGetter = p == null ? false : (Boolean) useGetterMethod.invoke(p, nameForYSeries);
			if (useGetter) {
				clickedPointMethodForYValues = ClickedPoint.class.getDeclaredMethod("get" + nameForYSeries);
				clickedPointMethodForYValues.setAccessible(true);
			} else {
				try {
					clickedPointFieldForYValues = ClickedPoint.class.getDeclaredField(nameForYSeries);
				} catch (NoSuchFieldException e) {
					nameForYSeries = nameForYSeries.substring(0, 1).toLowerCase() + nameForYSeries.substring(1);
					clickedPointFieldForYValues = ClickedPoint.class.getDeclaredField(nameForYSeries);
				}
				clickedPointFieldForYValues.setAccessible(true);
			}
		} catch (Exception e) {
			Utils.printStack(e);
		}
	}

	public XYSeriesReflection(Comparable<?> key) {
		super(key);
	}

	public XYSeriesReflection(Comparable<?> key, PluginIOCells masterList) {
		super(key);
		masterList.addSeriesListener(this);
	}

	@Override
	public void fireSeriesChanged() {
		if (automaticallyUpdateTrendline)
			recomputeTrendLine();
		super.fireSeriesChanged();
	}

	private class NumberPair implements Comparable<NumberPair> {

		public NumberPair(double d1, double d2) {
			this.d1 = d1;
			this.d2 = d2;
		}

		public double d1;
		public double d2;

		@Override
		public int compareTo(NumberPair o) {
			return Double.compare(d1, o.d1);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(d1);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(d2);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NumberPair other = (NumberPair) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (Double.doubleToLongBits(d1) != Double.doubleToLongBits(other.d1))
				return false;
			if (Double.doubleToLongBits(d2) != Double.doubleToLongBits(other.d2))
				return false;
			return true;
		}

		private XYSeriesReflection getOuterType() {
			return XYSeriesReflection.this;
		}
	}

	@Override
	protected void recomputeTrendLine() {
		if (smoothingScale == 0) {
			if (trendLine != null)
				trendLine.clear();
			return;
		}
		if (trendLine == null)
			trendLine = new XYSeries("Smoothed");
		int current_x = 0;
		int numberPoints = getItemCount();
		int x_behind = 0;
		int x_forward = 0;

		NumberPair[] sortedValues = new NumberPair[numberPoints];

		for (int i = 0; i < numberPoints; i++) {
			sortedValues[i] = new NumberPair(getXFlipAware(i).doubleValue(), getYFlipAware(i).doubleValue());
		}
		Arrays.sort(sortedValues);

		double[] y2 = new double[numberPoints];
		double[] x2 = new double[numberPoints];

		trendLine.setNotify(false);
		trendLine.clear();
		while (current_x <= numberPoints - 1) {
			y2[current_x] = 0.0;
			while (sortedValues[x_behind].d1 < sortedValues[current_x].d1 - smoothingScale / 2.0) {
				x_behind++;
			}
			while (sortedValues[x_forward].d1 < sortedValues[current_x].d1 + smoothingScale / 2.0) {
				if (x_forward < numberPoints - 1) {
					x_forward++;
				} else
					break;
			}

			for (int j = x_behind; j <= x_forward; j++) {
				y2[current_x] += sortedValues[j].d2;
			}
			y2[current_x] /= x_forward - x_behind + 1;
			x2[current_x] = sortedValues[current_x].d1;
			if (current_x == numberPoints - 1)
				trendLine.setNotify(true);
			if (flipAxes)
				trendLine.add(y2[current_x], x2[current_x]);
			else
				trendLine.add(x2[current_x], y2[current_x]);
			current_x++;
		}

	}

	Number getXFlipAware(int index) {
		if (flipAxes)
			return getY(index);
		else
			return getX(index);
	}

	Number getYFlipAware(int index) {
		if (flipAxes)
			return getX(index);
		else
			return getY(index);
	}

	private final static Object[] emptyObjectArray = new Object[] {};

	private Number getXNumber(ClickedPoint p) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		if (indexForXSeries == -1) {
			if (clickedPointMethodForXValues != null) {
				return (Number) clickedPointMethodForXValues.invoke(p, emptyObjectArray);
			} else
				return (Number) clickedPointFieldForXValues.get(p);
		} else {
			@SuppressWarnings("rawtypes")
			Object cell = ((List) clickedPointFieldForXValues.get(p)).get(indexForXSeries);
			if (cell instanceof SpreadsheetCell)
				return (Number) ((SpreadsheetCell) cell).getEvaluationResult();
			else
				return (Number) cell;
		}
	}

	private Number getYNumber(ClickedPoint p) throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		if (indexForYSeries == -1) {
			if (clickedPointMethodForYValues != null) {
				return (Number) clickedPointMethodForYValues.invoke(p, emptyObjectArray);
			} else
				return (Number) clickedPointFieldForYValues.get(p);
		} else {
			@SuppressWarnings("rawtypes")
			Object cell = ((List) clickedPointFieldForYValues.get(p)).get(indexForYSeries);
			if (cell instanceof SpreadsheetCell)
				return (Number) ((SpreadsheetCell) cell).getEvaluationResult();
			else
				return (Number) cell;
		}
	}

	@Override
	public void add(ClickedPoint p) {
		try {
			if ((clickedPointFieldForYValues != null) || (clickedPointMethodForYValues != null))
				add(getXNumber(p), getYNumber(p));
			else
				add(getXNumber(p), 0f);
		} catch (Exception e) {
			Utils.printStack(e);
		}
	}

	@Override
	public void add(ClickedPoint[] p) {
		for (ClickedPoint element : p) {
			try {
				add(getXNumber(element), getYNumber(element));
			} catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
				Utils.printStack(e);
			}
		}
	}

}
