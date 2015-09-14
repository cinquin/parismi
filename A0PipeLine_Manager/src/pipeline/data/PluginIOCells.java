/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import ij.measure.Calibration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.boris.expr.ExprDouble;
import org.eclipse.jdt.annotation.NonNull;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOCellsListeningSeries;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.XYSeriesReflection;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.GUI_utils.bean_table.DoNotShowInTable;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.SpreadsheetCell;
import pipeline.plugins.c_plugins.ProtobufSeedsOrCells.SegDirectory;
import pipeline.plugins.c_plugins.ProtobufSeedsOrCells.SegDirectory.Builder;
import pipeline.plugins.c_plugins.ProtobufSeedsOrCells.SegInfo;
import pipeline.plugins.input_output.LoadProtobufBinary;

import com.google.protobuf.InvalidProtocolBufferException;

// FIXME userCellDescriptions loaded from protobuf files are discarded to allow compatibility with
// corrupt protobuf files (see FIXMEs below).

// FIXME ClickedPoints create by default 3 user cells when they are constructed. There need to be matching
// descriptions in userCellDescriptions in the owning PluginIOCells, but PluginIOCells does not create names by default,
// and instead retrieves them from ClickedPoints it creates.
public class PluginIOCells extends PluginIOListOfQ<ClickedPoint> implements Cloneable, PluginIOCalibrable,
		IQuantifiableNames, IDimensions {

	public static PluginIOCells readFromFile(File f) {
		byte[] bytes = LoadProtobufBinary.readProtobufFile(f);
		PluginIOCells seeds = new PluginIOCells();
		seeds.setProperty("Protobuf", bytes);
		return seeds;
	}

	// Override setProperty so we defer restoring of internal fields from protobuf data
	// This is because restoring can be time consuming, and protobuf contents might be set
	// to something new a number of times before the contents actually need to be accessed.
	// We can thus save a number of rounds of unnecessary parsing by deferring.
	@Override
	public void setProperty(String string, Object value) {
		if ("Protobuf".equals(string)) {
			setProtobuf((byte[]) value);
			internalList = null;
		} else
			super.setProperty(string, value);
	}

	@Override
	public PluginIOView createView() {
		return new ListOfPointsView<>(this);
	}

	/**
	 * Does NOT copy cells or their associated values and descriptions
	 */
	@Override
	public void copyInto(IPluginIO destination) {
		super.copyInto(destination);
		PluginIOCells dest = (PluginIOCells) destination;
		dest.setCalibration((Calibration) (getCalibration() == null ? null : getCalibration().clone()));
		dest.setDepth(getDepth());
		dest.setWidth(getWidth());
		dest.setHeight(getHeight());

		dest.getUserCellDescriptions().clear();
		dest.getUserCellDescriptions().addAll(getUserCellDescriptions());

		dest.getQuantifiedPropertyNames().clear();
		dest.getQuantifiedPropertyNames().addAll(getQuantifiedPropertyNames());

	}

	@Override
	public Object clone() {
		parseOrReallocate();
		PluginIOCells copy = new PluginIOCells();
		copyInto(copy);

		copy.internalList = new ArrayList<>(internalList.size());

		for (ClickedPoint point : internalList) {
			ClickedPoint newPoint = (ClickedPoint) point.clone();
			newPoint.listNamesOfQuantifiedProperties = copy.getQuantifiedPropertyNames();
			newPoint.userCellDescriptions = copy.getUserCellDescriptions();
			copy.internalList.add(newPoint);
		}

		copy.maxId = maxId;
		return copy;
	}

	/**
	 * 
	 * @param points
	 *            null not allowed
	 */
	public PluginIOCells(List<ClickedPoint> points) {
		this();
		if (points == null)
			throw new IllegalArgumentException();
		this.internalList = points;
	}

	public PluginIOCells() {
		this.internalList = new ArrayList<>(20);
		setName("Cell list");
		int i = 0;
		while (userCellDescriptions.size() < 3) {
			userCellDescriptions.add("userCell " + (i + 1));
			i++;
		}
	}

	public PluginIOCells(File inputFile) {
		this();
		byte[] bytes = LoadProtobufBinary.readProtobufFile(inputFile);
		setProperty("Protobuf", bytes);
		setName(FileNameUtils.getShortNameFromPath(inputFile.getAbsolutePath(), 40));
	}

	public PluginIOCells(@NonNull String name) {
		this();
		this.setName(name);
	}

	private static final long serialVersionUID = 1L;

	@Override
	public @NonNull File asFile(File saveTo, boolean useBigTIFF) throws IOException {
		if (saveTo == null) {
			throw new RuntimeException("Temp file creation not yet implemented");
		}
		String path = saveTo.getAbsolutePath();
		if (!path.endsWith(".proto")) {
			path += ".proto";
			saveTo = new File(path);
		}
		try (FileOutputStream fos = new FileOutputStream(path)) {
			fos.write(asProtobufBytes());
		}
		return saveTo;
	}

	{
		internalList = new ArrayList<>(20);
	}

	public List<ClickedPoint> getPoints() {
		parseOrReallocate();
		return internalList;
	}

	@Override
	public final void parseOrReallocate() {
		if (updatingProtobuf.get()) {
			Utils.log("Updating protobuf so not reparsing", LogLevel.DEBUG);
			return;
		}
		if (internalList == null) {
			if (getProtobuf() != null) {
				restoreFromProtobuf();
				return;
			}
			internalList = new ArrayList<>(20);
			clearListeningSeries();
		}
	}

	private boolean addDontFireValueChanged(ClickedPoint p) {
		parseOrReallocate();
		boolean result = internalList.add(p);
		setProtobuf(null);
		addPointToListeningSeries(p);
		return result;
	}

	@Override
	public boolean add(ClickedPoint p) {
		boolean result = addDontFireValueChanged(p);
		fireValueChanged(false, false);
		return result;
	}

	@Override
	public void clear() {
		if (internalList == null)
			internalList = new ArrayList<>(20);
		internalList.clear();
		setProtobuf(null);
		clearListeningSeries();
	}

	@Override
	public byte @NonNull[] asProtobufBytes() {
		// FIXME Need to clean up behavior of userCells/userCellFormulas printing
		byte [] local = getProtobuf();
		if (local != null)
			return local;
		parseOrReallocate();
		Builder protobufListOfPoints = SegDirectory.newBuilder();
		protobufListOfPoints.setImageDimx(width).setImageDimy(height).setImageDimz(depth).addAllUserCellDescriptions(
				getUserCellDescriptions()).addAllQuantifiedPropertyNames(getQuantifiedPropertyNames());

		if (calibration != null)
			protobufListOfPoints.setXMicronCalibration((float) calibration.pixelWidth).setYMicronCalibration(
					(float) calibration.pixelHeight).setZMicronCalibration((float) calibration.pixelDepth);

		int nUserCellsDescriptions = getUserCellDescriptions().size();

		for (ClickedPoint clickedPoint : internalList) {
			// add a seed if mouse was pressed without modifiers
			// remove seed if mouse was pressed with shift

			// Utils.log("read point "+clickedPoint.x+" "+clickedPoint.y,LogLevel.VERBOSE_DEBUG);

			SegInfo.Builder protobufPoint =
					SegInfo.newBuilder().setIdx(clickedPoint.getSeedId()).setSeedX((int) clickedPoint.x).setSeedY(
							(int) clickedPoint.y).setSeedT1(clickedPoint.t).setSeedZ((float) clickedPoint.z).setSeedT2(
							(float) (clickedPoint.z - ((float) clickedPoint.z))).setContourMergeGroup(
							clickedPoint.clickGroup).addAllQuantifiedProperties(clickedPoint.quantifiedProperties);

			if (clickedPoint.hsz != 0)
				protobufPoint.setSeedHsz(clickedPoint.hsz);

			if (clickedPoint.imageFullSegCoordsX != null) {
				int[] coordArray = clickedPoint.imageFullSegCoordsX;
				protobufPoint.ensureImageFullsegCoordsXIsMutable(coordArray.length);
				System.arraycopy(coordArray, 0, protobufPoint.getImageFullsegCoordsXArray(), 0, coordArray.length);
				protobufPoint.numUsedInImageFullsegCoordsX_ = coordArray.length;

				coordArray = clickedPoint.imageFullSegCoordsY;
				if (coordArray != null) {
					protobufPoint.ensureImageFullsegCoordsYIsMutable(coordArray.length);
					System.arraycopy(coordArray, 0, protobufPoint.getImageFullsegCoordsYArray(), 0, coordArray.length);
					protobufPoint.numUsedInImageFullsegCoordsY_ = coordArray.length;
				}

				coordArray = clickedPoint.imageFullSegCoordsZ;
				if (coordArray != null) {
					protobufPoint.ensureImageFullsegCoordsZIsMutable(coordArray.length);
					System.arraycopy(coordArray, 0, protobufPoint.getImageFullsegCoordsZArray(), 0, coordArray.length);
					protobufPoint.numUsedInImageFullsegCoordsZ_ = coordArray.length;
				}
			}

			if (clickedPoint.imagePerimsegCoordsX != null) {
				int[] coordArray = clickedPoint.imagePerimsegCoordsX;
				protobufPoint.ensureImagePerimsegCoordsXIsMutable(coordArray.length);
				System.arraycopy(coordArray, 0, protobufPoint.getImagePerimsegCoordsXArray(), 0, coordArray.length);
				protobufPoint.numUsedInImagePerimsegCoordsX_ = coordArray.length;

				coordArray = clickedPoint.imagePerimsegCoordsY;
				protobufPoint.ensureImagePerimsegCoordsYIsMutable(coordArray.length);
				System.arraycopy(coordArray, 0, protobufPoint.getImagePerimsegCoordsYArray(), 0, coordArray.length);
				protobufPoint.numUsedInImagePerimsegCoordsY_ = coordArray.length;

				coordArray = clickedPoint.imagePerimsegCoordsZ;
				protobufPoint.ensureImagePerimsegCoordsZIsMutable(coordArray.length);
				System.arraycopy(coordArray, 0, protobufPoint.getImagePerimsegCoordsZArray(), 0, coordArray.length);
				protobufPoint.numUsedInImagePerimsegCoordsZ_ = coordArray.length;
			}

			if (clickedPoint.imageUsersegCoordsX != null) {
				int[] coordArray = clickedPoint.imageUsersegCoordsX;
				protobufPoint.ensureImageUsersegCoordsXIsMutable(coordArray.length);
				System.arraycopy(coordArray, 0, protobufPoint.getImageUsersegCoordsXArray(), 0, coordArray.length);
				protobufPoint.numUsedInImageUsersegCoordsX_ = coordArray.length;

				coordArray = clickedPoint.imageUsersegCoordsY;
				protobufPoint.ensureImageUsersegCoordsYIsMutable(coordArray.length);
				System.arraycopy(coordArray, 0, protobufPoint.getImageUsersegCoordsYArray(), 0, coordArray.length);
				protobufPoint.numUsedInImageUsersegCoordsY_ = coordArray.length;

				coordArray = clickedPoint.imageUsersegCoordsZ;
				protobufPoint.ensureImageUsersegCoordsZIsMutable(coordArray.length);
				System.arraycopy(coordArray, 0, protobufPoint.getImageUsersegCoordsZArray(), 0, coordArray.length);
				protobufPoint.numUsedInImageUsersegCoordsZ_ = coordArray.length;

			}

			int modifiers = clickedPoint.modifiers;
			if ((modifiers & DELETE_MODIFIER) > 0) {
				protobufPoint.setContourAddRemoveMerge(2);// we want to delete the seed
			} else if ((modifiers & MERGE_MODIFIER) > 0) {
				protobufPoint.setContourAddRemoveMerge(3);// we want to merge the seed
				Utils.log("Merge", LogLevel.DEBUG);
				protobufPoint.setContourMergeGroup(clickedPoint.clickGroup);
				protobufPoint.setSeedManual(1);
			} else if ((modifiers & LABEL_MODIFIER) > 0) {
				protobufPoint.setContourAddRemoveMerge(4);
			} else if ((modifiers & DELETE_LABELS_MODIFIER) > 0) {
				protobufPoint.setContourAddRemoveMerge(5);
			} else
				protobufPoint.setContourAddRemoveMerge(1);// we want to add the seed

			if (clickedPoint.userCells.size() > 3) {
				Utils.log("More than 3 cells", LogLevel.DEBUG);
			}

			List<SpreadsheetCell> cells = clickedPoint.userCells;
			int nAddedUserCells = 0;
			if (cells.size() > 0) {
				for (SpreadsheetCell cell : cells) {
					protobufPoint.addUserCellFormula(cell.getFormula());
					if (cell.getEvaluationResult() instanceof ExprDouble)
						protobufPoint.addUserCellValue((float) ((ExprDouble) cell.getEvaluationResult()).value);
					else if (cell.getEvaluationResult() instanceof Float)
						protobufPoint.addUserCellValue((Float) (cell.getEvaluationResult()));
					else
						protobufPoint.addUserCellValue(0.0f);
					nAddedUserCells++;
				}
			} else if (clickedPoint.userCellFormulas != null) {
				// We might have formulas even if spreadsheet cells were not created
				protobufPoint.addAllUserCellFormula(clickedPoint.userCellFormulas);
			}
			while (nAddedUserCells < nUserCellsDescriptions) {
				protobufPoint.addUserCellValue(0.0f);
				nAddedUserCells++;
			}
			if (clickedPoint.userCells.size() > 3) {
				Utils.log("More than 3 cells", LogLevel.DEBUG);
			}

			// The following is to avoid null pointer errors if fields have not been filled in
			// This is probably a bug in the protobuf patch that gives access to primitive arrays

			protobufPoint.ensureImageFullsegCoordsXIsMutable(1);
			protobufPoint.ensureImageFullsegCoordsYIsMutable(1);
			protobufPoint.ensureImageFullsegCoordsZIsMutable(1);
			protobufPoint.ensureImagePerimsegCoordsXIsMutable(1);
			protobufPoint.ensureImagePerimsegCoordsYIsMutable(1);
			protobufPoint.ensureImagePerimsegCoordsZIsMutable(1);
			protobufPoint.ensureImageUsersegCoordsXIsMutable(1);
			protobufPoint.ensureImageUsersegCoordsYIsMutable(1);
			protobufPoint.ensureImageUsersegCoordsZIsMutable(1);

			protobufListOfPoints.addProtobufInfo(protobufPoint);
		}

		@SuppressWarnings("null")
		byte @NonNull[] result = protobufListOfPoints.build().toByteArray();
		setProtobuf(result);

		return result;
	}

	public static final int DELETE_MODIFIER = java.awt.event.InputEvent.SHIFT_MASK;
	public static final int MERGE_MODIFIER = java.awt.event.InputEvent.ALT_MASK;
	public static final int RESIZE_MODIFIER = 16;
	public static final int LABEL_MODIFIER = 32;
	public static final int DELETE_LABELS_MODIFIER = 64;

	private int width;

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public int getWidth() {
		parseOrReallocate();
		return width;
	}

	private int height;

	@Override
	public int getHeight() {
		parseOrReallocate();
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	private int depth;

	@Override
	public int getDepth() {
		parseOrReallocate();
		return depth;
	}

	@Override
	public void setDepth(int depth) {
		this.depth = depth;
	}

	private transient Calibration calibration;

	@Override
	public Calibration getCalibration() {
		parseOrReallocate();
		return calibration;
	}

	@Override
	public void setCalibration(Calibration calibration) {
		this.calibration = calibration;
	}

	public static final int ADD_MODIFIER = 0;

	@Override
	public synchronized void restoreFromProtobuf() {
		if (getProtobuf() == null)
			throw new IllegalStateException("No protobuf to restore from");

		clearListeningSeries();
		internalList = null;// allow GC to free up the memory before parsing protobuf file
		SegDirectory segDir;
		try {
			/**
			 * The call below is where this method spends most of its time, because it parses the whole contents.
			 * Unfortunately there does not seem to be a way to request lazy parsing, which could allow for the
			 * individual SegInfo components of SegDirectory to be read in a multithreaded way.
			 * TODO Find a way to improve the performance. For large files there is a significant delay when
			 * the user tries to visualize the results.
			 */
			segDir = SegDirectory.parseFrom(getProtobuf());
		} catch (InvalidProtocolBufferException e) {
			Utils.displayMessage("Invalid protobuf " + e, false, LogLevel.ERROR);
			setProtobuf(null);
			throw new RuntimeException("Invalid protobuf", e);
		}
		if (segDir.getImageDimx() != 0)
			width = (int) segDir.getImageDimx();
		if (segDir.getImageDimy() != 0)
			height = (int) segDir.getImageDimy();
		if (segDir.getImageDimz() != 0)
			depth = (int) segDir.getImageDimz();

		if (segDir.hasXMicronCalibration()) {
			calibration = new Calibration();
			calibration.pixelDepth = segDir.getZMicronCalibration();
			calibration.pixelHeight = segDir.getYMicronCalibration();
			calibration.pixelWidth = segDir.getXMicronCalibration();
		}

		userCellDescriptions.clear();
		// userCellDescriptions.addAll(segDir.getUserCellDescriptionsList()); FIXME Temporarily disabled this step to
		// allow reading of
		// corrupt protobuf files
		// to debug corrupt protobuf files
		/*
		 * if (userCellDescriptions.size()>3){
		 * Utils.log("Too many desc",LogLevel.VERBOSE_DEBUG);
		 * }
		 */

		try {
			updatingProtobuf.set(true);

			getQuantifiedPropertyNames().clear();
			// Trim spaces from quantified property names
			for (String s : segDir.getQuantifiedPropertyNamesList()) {
				getQuantifiedPropertyNames().add(s.replace(" ", ""));
			}

			final List<SegInfo> segList = segDir.getProtobufInfoList();
			final ClickedPoint[] pointArray = new ClickedPoint[segList.size()];

			final Set<Integer> seedIds = new HashSet<>();
			final AtomicBoolean duplicateId = new AtomicBoolean(false);

			maxId = 0;

			/**
			 * The multithreading below ends up not saving a lot of time, because the parsing has already been
			 * done by the protobuf code (called in a non-parallelized way above).
			 */

			ParFor parFor = new ParFor("Restore cells from protobuf", 0, segList.size() - 1, null, true);

			for (int i = 0; i < parFor.getNThreads(); i++) {
				parFor.addLoopWorker((loopIndex, threadIndex) -> {

					SegInfo seg = segList.get(loopIndex);
					ClickedPoint p =
							new ClickedPoint((int) seg.getSeedX(), (int) seg.getSeedY(), seg.getSeedZ(), 0, 0,
									(int) seg.getContourAddRemoveMerge(), (int) seg.getContourMergeGroup());
					if (seg.hasSeedT1()) {
						p.t = seg.getSeedT1();
					}
					if (seg.hasSeedT2()) {
						p.z += seg.getSeedT2();
					}
					if (seg.hasSeedHsz()) {
						p.hsz = seg.getSeedHsz();
					}
					pointArray[loopIndex] = p;

					if (seg.getContourAddRemoveMerge() > 1) {
						Utils.log("Add/merge >1", LogLevel.DEBUG);
					}
					p.setSeedId(seg.getIdx());

					synchronized (seedIds) {
						if (!duplicateId.get()) {
							if (seedIds.contains((int) p.getSeedId())) {
								duplicateId.set(true);
							}
							// FIXME Update seedIds with newly-read seedIdx, if the aim is to
							// ensure unicity?
							if (p.getSeedId() > maxId)
								maxId = (int) p.getSeedId();
						}
						addPointToListeningSeries(p);

						if (userCellDescriptions.size() > 0)
							p.userCellDescriptions = userCellDescriptions;
						else {
							userCellDescriptions = p.userCellDescriptions;
							// because if there aren't enough user cells they will be added by
							// the first ClickedPoint that we instantiate

							if (p.userCellDescriptions == null) {
								throw new RuntimeException("Null user cell descriptions");
							}
							setProtobuf(null);// so if this object is reused the list of userCells is updated
						}
					}

					p.listNamesOfQuantifiedProperties = getQuantifiedPropertyNames();

					p.imageFullSegCoordsX = seg.getImageFullsegCoordsXArray();
					p.imageFullSegCoordsY = seg.getImageFullsegCoordsYArray();
					p.imageFullSegCoordsZ = seg.getImageFullsegCoordsZArray();
					p.imagePerimsegCoordsX = seg.getImagePerimsegCoordsXArray();
					p.imagePerimsegCoordsY = seg.getImagePerimsegCoordsYArray();
					p.imagePerimsegCoordsZ = seg.getImagePerimsegCoordsZArray();
					p.imageUsersegCoordsX = seg.getImageUsersegCoordsXArray();
					p.imageUsersegCoordsY = seg.getImageUsersegCoordsYArray();
					p.imageUsersegCoordsZ = seg.getImageUsersegCoordsZArray();

					p.userCellFormulas.clear();
					p.userCellValues.clear();
					p.quantifiedProperties.clear();
					p.userCellFormulas.addAll(seg.getUserCellFormulaList());
					p.userCellValues.addAll(seg.getUserCellValueList());
					p.quantifiedProperties.addAll(seg.getQuantifiedPropertiesList());
					if (calibration != null) {
						p.xyCalibration = (float) calibration.pixelWidth;
						p.zCalibration = (float) calibration.pixelDepth;
					}

					List<String> listUserFormulas = seg.getUserCellFormulaList();
					Iterator<Float> userValueIt = seg.getUserCellValueList().iterator();
					p.userCells.clear();
					for (String formula : listUserFormulas) {
						float value = userValueIt.next();
						p.userCells.add(new SpreadsheetCell(null, null, new Object[] { value, formula }, true, null,
								null));
					}
					p.expandUserCellList(ClickedPoint.numberUserCells);

					return null;
				});
			}

			try {
				parFor.run(true);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}

			if (duplicateId.get()) {
				// renumber the seeds to make sure they all have identical ID
				int seedId = 0;
				for (ClickedPoint p : pointArray) {
					p.setSeedId(seedId);
					seedId++;
				}
				maxId = seedId - 1;
			}

			internalList = new ArrayList<>(Arrays.asList(pointArray));
			segDir = null;
		} finally {
			updatingProtobuf.set(false);
		}

		silenceUpdates = true;
		try {
			fireValueChanged(false, true);// don't clear protobuf because we just parsed it
		} finally {
			silenceUpdates = false;
		}
	}

	private transient AtomicBoolean updatingProtobuf = new AtomicBoolean(false);

	private transient List<PluginIOCellsListeningSeries> listenerSeries;

	private void addPointToListeningSeries(ClickedPoint p) {
		checkListenerListInitialized();
		for (PluginIOCellsListeningSeries listener : listenerSeries) {
			listener.add(p);
		}
	}

	private void clearListeningSeries() {
		checkListenerListInitialized();
		for (PluginIOCellsListeningSeries listenerSery : listenerSeries) {
			listenerSery.clear();
		}
	}

	private void checkListenerListInitialized() {
		if (listenerSeries == null)
			listenerSeries = new CopyOnWriteArrayList<>();
	}

	public void addSeriesListener(PluginIOCellsListeningSeries series) {
		checkListenerListInitialized();
		listenerSeries.add(series);
	}

	public void removeSeriesListener(XYSeriesReflection series) {
		checkListenerListInitialized();
		while (listenerSeries.remove(series)) {
		}
	}

	@Override
	public XYSeriesReflection getJFreeChartXYSeries(String xName, String yName, int xIndex, int yIndex,
			String displayNameForXSeries, String displayNameForYSeries) {
		XYSeriesReflection jFreeChartSeries = new XYSeriesReflection(getName());
		addSeriesListener(jFreeChartSeries);
		if (internalList.size() > 0)
			jFreeChartSeries.setNameForXSeries(xName, xIndex, internalList.get(0));
		else
			jFreeChartSeries.setNameForXSeries(xName, xIndex, null);
		if (yName != null) {
			if (internalList.size() > 0) {
				jFreeChartSeries.setNameForYSeries(yName, yIndex, internalList.get(0));
			} else
				jFreeChartSeries.setNameForYSeries(yName, yIndex, null);
		}
		internalList.forEach(jFreeChartSeries::add);
		jFreeChartSeries.displayNameForXSeries = displayNameForXSeries;
		jFreeChartSeries.displayNameForYSeries = displayNameForYSeries;
		return jFreeChartSeries;
	}

	private ClickedPoint getPointClosestTo(int x, int y, double z, int t) {
		if (internalList.isEmpty())
			return null;
		ClickedPoint base = new ClickedPoint(x, y, z, 0, 0, 0);
		ClickedPoint closestPoint = null;
		double minDistance = Double.MAX_VALUE;
		for (ClickedPoint p : internalList) {
			if (p.t != t)
				continue;
			double distance = base.distanceTo(p);
			if (distance < minDistance) {
				minDistance = distance;
				closestPoint = p;
			}
		}
		return closestPoint;
	}

	public void clearLabelsOfPointClosestTo(int x, int y, double z, int t) {
		if (internalList.isEmpty())
			return;
		setProtobuf(null);// so if this object is reused the list of userCells is updated
		getPointClosestTo(x, y, z, t).clearLabels();
	}

	private void checkAnnotationNames(String[] annotations) {
		for (String name : annotations) {
			boolean found = false;
			String prefixedName = "_anno_" + name;
			for (String s : getQuantifiedPropertyNames()) {
				if (s.equals(prefixedName)) {
					found = true;
					break;
				}
			}
			if (!found) {
				getQuantifiedPropertyNames().add(prefixedName);
				for (ClickedPoint p : internalList) {
					p.getQuantifiedProperties().add(0f);
					p.setQuantifiedPropertyNames(getQuantifiedPropertyNames());
				}
			}
		}

	}

	public void annotatePointsInBox(int x, int y, int boxWidth, int boxHeight, double z, int t, String[] annotations) {
		if (internalList.isEmpty())
			return;
		setProtobuf(null);// so if this object is reused the list of userCells is updated
		checkAnnotationNames(annotations);
		for (ClickedPoint p : internalList) {
			if (p.t != t)
				continue;
			if ((p.x >= x) && (p.x <= x + boxWidth) && (p.y >= y) && (p.y <= y + boxHeight))
				p.setAnnotations(annotations);
		}
	}

	public void annotatePointClosestTo(int x, int y, double z, int t, String[] annotations) {
		if (internalList.isEmpty())
			return;
		setProtobuf(null);// so if this object is reused the list of userCells is updated
		checkAnnotationNames(annotations);
		ClickedPoint p = getPointClosestTo(x, y, z, t);
		if (p == null)
			throw new IllegalStateException("No preexisting point to annotate");
		p.setAnnotations(annotations);
	}

	private int maxId = 0;

	public ClickedPoint createAnnotatedPoint(int x, int y, double z, int t, String[] annotations) {
		setProtobuf(null);// so if this object is reused the list of userCells is updated
		checkAnnotationNames(annotations);
		ClickedPoint newPoint = new ClickedPoint(x, y, z, t, 0, 0);
		maxId++;
		newPoint.setSeedId(maxId);
		newPoint.listNamesOfQuantifiedProperties = getQuantifiedPropertyNames();
		if (getUserCellDescriptions().size() >= newPoint.userCellDescriptions.size())
			newPoint.userCellDescriptions = getUserCellDescriptions();
		else
			setUserCellDescriptions(newPoint.userCellDescriptions);
		if (calibration != null) {
			newPoint.xyCalibration = (float) getCalibration().pixelWidth;// assume same calibration for x and y
			newPoint.zCalibration = (float) getCalibration().pixelDepth;
		}
		for (int i = 0; i < getQuantifiedPropertyNames().size(); i++) {
			newPoint.quantifiedProperties.add(0f);
		}
		newPoint.setAnnotations(annotations);
		internalList.add(newPoint);
		return newPoint;
	}

	public void deletePointClosestTo(int x, int y, double z, int t) {
		if (internalList.isEmpty())
			return;
		setProtobuf(null);// so if this object is reused the list of userCells is updated
		ClickedPoint p = getPointClosestTo(x, y, z, t);
		internalList.remove(p);
	}

	public void deletePointsInBox(int x, int y, double z, int t, int boxWidth, int boxHeight, int boxDepth) {
		if (internalList.isEmpty())
			return;
		setProtobuf(null);// so if this object is reused the list of userCells is updated

		Iterator<ClickedPoint> it = internalList.iterator();
		while (it.hasNext()) {
			ClickedPoint p = it.next();
			if (p.isInBox(x, y, z, t, boxWidth, boxHeight, boxDepth))
				it.remove();
		}

	}

	@Override
	@DoNotShowInTable
	public Class<?> getElementClass() {
		return ClickedPoint.class;
	}

	@Override
	public BeanTableModel<ClickedPoint> getBeanTableModel() {
		parseOrReallocate();
		return new BeanTableModel<>(ClickedPoint.class, internalList);
	}

	@Override
	public void addDontFireValueChanged(IPluginIOListMember<?> element) {
		parseOrReallocate();
		addDontFireValueChanged((ClickedPoint) element);
	}

	@Override
	public IPluginIOListOfQ<ClickedPoint> duplicateStructure() {
		parseOrReallocate();
		PluginIOCells result = new PluginIOCells();
		copyInto(result);
		return result;
	}

}
