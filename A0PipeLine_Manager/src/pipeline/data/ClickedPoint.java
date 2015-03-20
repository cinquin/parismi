/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.primitives.ArrayFloatList;
import org.eclipse.jdt.annotation.NonNull;

import pipeline.GUI_utils.bean_table.DoNotShowInTable;
import pipeline.GUI_utils.bean_table.MethodToGetColumnNames;
import pipeline.GUI_utils.bean_table.TableInsertionIndex;
import pipeline.misc_util.Utils;
import pipeline.parameters.SpreadsheetCell;

/**
 * Used to store the coordinates of a point along with the corresponding mouse modifiers and a group identity. Modifiers
 * are
 * copied from the java.awt.event.MouseEvent modifiers.
 * There are two lists returned by getters. These will be detected by BeanTableModel, and tables will display one column
 * per list element.
 * XXX Now it's clearer what information objects of this class should contain, the fields, quantified
 * properties, and user cells should be streamlined, calibration should be taken care of properly,
 * and the class should be renamed.
 *
 */
public class ClickedPoint extends PluginIO implements Cloneable, Comparable<ClickedPoint>,
		IPluginIOListMemberQ<ClickedPoint>, IQuantifiable {

	private static final long serialVersionUID = -2641590844992645423L;

	/*
	 *  * IMPORTANT
	 * Keep public accessor for userCells first so they are displayed as the first column in ListOfPointsView.
	 * The table relies on that to properly update those names.
	 */

	@TableInsertionIndex(value = 0)
	@MethodToGetColumnNames(value = "getListNamesOfuserCells")
	public List<SpreadsheetCell> getuserCells() {
		return userCells;
	}

	public void setuserCells(List<SpreadsheetCell> qp) {
		userCells = qp;
	}

	public List<String> userCellDescriptions = new ArrayList<>();

	@DoNotShowInTable
	public List<String> getListNamesOfuserCells() {
		return userCellDescriptions;
	}

	/**
	 * For use with SVMSuppress
	 */
	public byte status = 0;

	@Override
	public int compareTo(ClickedPoint o) {
		if (this.getConfidence() > o.getConfidence())
			return -1;
		else if (this.getConfidence() < o.getConfidence())
			return 1;
		else
			return 0;
	}

	public ClickedPoint(int seedX, int seedY, double seedZ, float t, int c, int contourAddremovemerge,
			int contourMergegroup) {
		x = seedX;
		y = seedY;
		z = seedZ;
		this.t = t;
		this.c = c;
		this.contourAddremovemerge = contourAddremovemerge;
		clickGroup = contourMergegroup;
		expandUserCellList(numberUserCells);
	}

	@Override
	/**
	 * WARNING: this method does not deal with the NAMES of the user cells and quantified properties (to avoid unnecessary
	 * cloning of a large number of Strings when PluginIOCells is cloned).
	 */
	public Object clone() {
		ClickedPoint newPoint = new ClickedPoint();
		newPoint.x = x;
		newPoint.y = y;
		newPoint.z = z;
		newPoint.t = t;
		newPoint.c = c;
		newPoint.contourAddremovemerge = contourAddremovemerge;
		newPoint.clickGroup = clickGroup;
		newPoint.xyCalibration = xyCalibration;
		newPoint.zCalibration = zCalibration;
		newPoint.clusterID = clusterID;
		newPoint.contourRuntime = contourRuntime;
		newPoint.seedId = seedId;
		newPoint.modifiers = modifiers;
		newPoint.status = status;
		newPoint.imageFullSegCoordsX = imageFullSegCoordsX == null ? null : imageFullSegCoordsX.clone();
		newPoint.imageFullSegCoordsY = imageFullSegCoordsY == null ? null : imageFullSegCoordsY.clone();
		newPoint.imageFullSegCoordsZ = imageFullSegCoordsZ == null ? null : imageFullSegCoordsZ.clone();
		newPoint.imagePerimsegCoordsX = imagePerimsegCoordsX == null ? null : imagePerimsegCoordsX.clone();
		newPoint.imagePerimsegCoordsY = imagePerimsegCoordsY == null ? null : imagePerimsegCoordsY.clone();
		newPoint.imagePerimsegCoordsZ = imagePerimsegCoordsZ == null ? null : imagePerimsegCoordsZ.clone();
		newPoint.imageUsersegCoordsX = imageUsersegCoordsX == null ? null : imageUsersegCoordsX.clone();
		newPoint.imageUsersegCoordsY = imageUsersegCoordsY == null ? null : imageUsersegCoordsY.clone();
		newPoint.imageUsersegCoordsZ = imageUsersegCoordsZ == null ? null : imageUsersegCoordsZ.clone();

		newPoint.quantifiedProperties.addAll(quantifiedProperties);
		newPoint.userCellValues.addAll(userCellValues);
		newPoint.userCellFormulas.addAll(userCellFormulas);

		// FIXME Should clone each user cell instead of just copying the list
		newPoint.userCells.clear();
		newPoint.userCells.addAll(userCells);
		return newPoint;
	}

	public double distanceTo(ClickedPoint p) {
		return Math.sqrt((x - p.x) * (x - p.x) + (y - p.y) * (y - p.y) + (z - p.z) * (z - p.z));
	}

	public ClickedPoint(int seedX, int seedY, double seedZ, int t, int modifiers, int contourMergegroup) {
		x = seedX;
		y = seedY;
		z = seedZ;
		this.t = t;
		this.modifiers = modifiers;
		clickGroup = contourMergegroup;
		expandUserCellList(numberUserCells);
	}

	public ClickedPoint() {
		expandUserCellList(numberUserCells);
	}

	public ClickedPoint(float f, float g) {
		expandUserCellList(numberUserCells);
		x = f;
		y = g;
	}

	public float x, y;
	public double z;
	public float t;

	private transient int cachedConfidenceIndex = -1;

	private void updateConfidenceIndex() {
		int index = listNamesOfQuantifiedProperties.indexOf("Confidence");
		cachedConfidenceIndex = index;
	}

	@DoNotShowInTable
	public Float getConfidence() {
		if (cachedConfidenceIndex > -1)
			return quantifiedProperties.get(cachedConfidenceIndex);
		else
			updateConfidenceIndex(); // FIXME Potential performance issue because of repeated String comparisons
		if (cachedConfidenceIndex > -1)
			return quantifiedProperties.get(cachedConfidenceIndex);
		else
			return null;
	}

	public void setConfidence(Float confidence) {
		if (confidence == null) {
			if (cachedConfidenceIndex > -1) {
				quantifiedProperties.set(cachedConfidenceIndex, Float.MAX_VALUE);
			}
			return;
		}
		if (cachedConfidenceIndex == -1) {
			updateConfidenceIndex();
		}
		if (cachedConfidenceIndex == -1) {
			throw new IllegalStateException("Confidence was not initialized");
		}
		quantifiedProperties.set(cachedConfidenceIndex, confidence);
	}

	@Override
	public String toString() {
		return "x=" + getx() + ", y=" + gety() + ", z=" + getz() + ", t" + gett() + ", c=" + getConfidence();
	}

	private int c;
	public int modifiers;
	private int contourAddremovemerge;
	public float contourRuntime = -1;

	public float hsz;

	public static final int numberUserCells = 3;

	public void expandUserCellList(int minimalSize) {
		int i = 0;
		while (userCells.size() < minimalSize) {
			userCells
					.add(new SpreadsheetCell("userCell " + (i + 1), null, new Object[] { null, "" }, true, null, null));
			if (userCellDescriptions.size() < minimalSize)
				userCellDescriptions.add("userCell " + (i + 1));
			i++;
		}
	}

	public List<SpreadsheetCell> userCells = new ArrayList<>();

	public @NonNull List<Float> quantifiedProperties = new ArrayList<>();

	@Override
	@MethodToGetColumnNames(value = "getQuantifiedPropertyNames")
	public List<Float> getQuantifiedProperties() {
		return quantifiedProperties;
	}

	@Override
	public void setQuantifiedProperties(List<Float> qp) {
		quantifiedProperties = qp;
	}

	public @NonNull List<String> listNamesOfQuantifiedProperties = new ArrayList<>();

	public float getSeedId() {
		return seedId;
	}

	public void setSeedId(float seedId) {
		this.seedId = seedId;
	}

	private float seedId;

	public int clusterID;

	public float getxyCalibration() {
		return xyCalibration;
	}

	public float getzCalibration() {
		return zCalibration;
	}

	public float xyCalibration = 0;
	public float zCalibration = 0;

	/**
	 * Used to determine whether it's safe for another object to directly read fields from this class.
	 * 
	 * @param fieldName
	 * @return true if it's not safe to read directly field fieldName, and the getter should be used instead
	 */
	public boolean useGetter(String fieldName) {
		switch (fieldName) {
			case "x":
			case "y":
				return (xyCalibration != 0);
			case "z":
				return (zCalibration != 0);
			default:
				return false;
		}
	}

	/**
	 * @return x in microns if calibration is non-0, x in pixels otherwise.
	 *         To translate to protobuf x should be read directly (since we should not use the calibration).
	 */
	public final float getx() {
		if (xyCalibration == 0)
			return x;
		else
			return x * xyCalibration;
	}

	/**
	 * FIXME setx(getx) changes the value of x because of the calibration!
	 * 
	 * @param x
	 */
	public final void setx(float x) {
		this.x = x;
	}

	/**
	 * @return y in microns if calibration is non-0, y in pixels otherwise.
	 *         To translate to protobuf y should be read directly (since we should not use the calibration).
	 */
	public final float gety() {
		if (xyCalibration == 0)
			return y;
		else
			return y * xyCalibration;
	}

	public final void sety(float y) {
		this.y = y;
	}

	public final double getz() {
		if (zCalibration == 0)
			return z;
		return z * zCalibration;
	}

	public final void setz(double z) {
		this.z = z;
	}

	public final float gett() {
		return t;
	}

	public final void sett(float t) {
		this.t = t;
	}

	public final int getc() {
		return c;
	}

	public final void setc(int c) {
		this.c = c;
	}

	public int getclusterID() {
		return clusterID;
	}

	public void setclusterID(int clusterID) {
		this.clusterID = clusterID;
	}

	public String getuserLabel() {
		return userLabel;
	}

	public void setuserLabel(String userLabel) {
		this.userLabel = userLabel;
	}

	/**
	 * Used to sort clicked points into groups. This is used for example when merging segmentations into
	 * more than one set, to specify which segmentations go with which.
	 */
	public int clickGroup;

	/**
	 * Used by the user to mark specific points in the image (e.g. MR/TZ boundary, PH3+ cell, distal end of the
	 * gonad, etc.)
	 */
	public String userLabel;
	public int[] imageFullSegCoordsX = null;
	public int[] imageFullSegCoordsY = null;
	public int[] imageFullSegCoordsZ = null;
	public int[] imagePerimsegCoordsX = null;
	public int[] imagePerimsegCoordsY = null;
	public int[] imagePerimsegCoordsZ = null;
	public int[] imageUsersegCoordsX = null;
	public int[] imageUsersegCoordsY = null;
	public int[] imageUsersegCoordsZ = null;

	public List<Float> userCellValues = new ArrayList<>();
	public List<String> userCellFormulas = new ArrayList<>();

	public void clearLabels() {
		List<String> propNames = listNamesOfQuantifiedProperties;
		for (int propIndex = 0; propIndex < quantifiedProperties.size(); propIndex++) {
			String name = propNames.get(propIndex);
			if (name.contains("_anno_")) {
				quantifiedProperties.set(propIndex, 0f);
			}
		}

	}

	public void setAnnotations(String[] annotations) {
		List<String> propNames = listNamesOfQuantifiedProperties;
		for (String name : annotations) {
			String prefixedName = "_anno_" + name;
			for (int propIndex = 0; propIndex < quantifiedProperties.size(); propIndex++) {
				if (propNames.get(propIndex).equals(prefixedName)) {
					quantifiedProperties.set(propIndex, 1f);
					break;
				}
			}
		}
	}

	/**
	 * 
	 * @param name
	 * @param value
	 * @return true if property already existed
	 */
	@Override
	public boolean setQuantifiedProperty(String name, float value) {
		int index = listNamesOfQuantifiedProperties.indexOf(name);
		boolean alreadyExists = (index > -1);
		if (!alreadyExists) {
			listNamesOfQuantifiedProperties.add(name);
			quantifiedProperties.add(value);
		} else
			quantifiedProperties.set(index, value);
		return alreadyExists;
	}

	@Override
	@DoNotShowInTable
	public float getQuantifiedProperty(String name) {
		int index = listNamesOfQuantifiedProperties.indexOf(name);
		if (index < 0)
			throw new IllegalArgumentException("Property " + name + " does not exist in list "
					+ Utils.printStringArray(listNamesOfQuantifiedProperties));
		return quantifiedProperties.get(index);
	}

	@Override
	public boolean hasQuantifiedProperty(String name) {
		int index = listNamesOfQuantifiedProperties.indexOf(name);
		return (index > -1);
	}

	public boolean isInBox(int x2, int y2, double z2, int t2, int boxWidth, int boxHeight, int boxDepth) {
		if ((x < x2) || (x > x2 + boxWidth))
			return false;
		if ((y < y2) || (y > y2 + boxHeight))
			return false;
		if ((z < z2) || (z > z2 + boxDepth))
			return false;
		return (t == t2);
	}

	@Override
	public void linkToList(IPluginIOList<?> list) {
		if (!(list instanceof PluginIOCells)) {
			throw new IllegalArgumentException();
		}
		PluginIOCells cells = (PluginIOCells) list;

		listNamesOfQuantifiedProperties = cells.getQuantifiedPropertyNames();
		userCellDescriptions = cells.getUserCellDescriptions();

		if (cells.getCalibration() != null) {
			xyCalibration = (float) cells.getCalibration().pixelWidth;// assume same calibration for x and y
			zCalibration = (float) cells.getCalibration().pixelDepth;
		}

		while (quantifiedProperties.size() < listNamesOfQuantifiedProperties.size()) {
			quantifiedProperties.add(0f);
		}
	}

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) throws IOException {
		throw new RuntimeException("Unimplemented");
	}

	@DoNotShowInTable
	@Override
	public List<String> getQuantifiedPropertyNames() {
		return listNamesOfQuantifiedProperties;
	}

	@Override
	public boolean addQuantifiedPropertyName(String name) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void setQuantifiedPropertyNames(@NonNull List<String> desc) {
		listNamesOfQuantifiedProperties = desc;
	}

	@MethodToGetColumnNames(value = "getListNamesOfpixelValues")
	public List<ArrayFloatList> getpixelValues() {
		return new ArrayList<>(pixelValues.values());
	}

	@DoNotShowInTable
	public List<String> getListNamesOfpixelValues() {
		return new ArrayList<>(pixelValues.keySet());
	}

	public Map<String, ArrayFloatList> pixelValues = new HashMap<>();

}
