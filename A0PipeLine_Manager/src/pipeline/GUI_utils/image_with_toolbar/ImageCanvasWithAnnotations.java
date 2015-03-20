/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils.image_with_toolbar;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageCanvas;
import ij.gui.Roi;
import ij.util.Java2;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOListener;
import pipeline.misc_util.Pair;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

public class ImageCanvasWithAnnotations extends ImageCanvas implements MouseMotionListener, MouseListener, KeyListener,
		PluginIOListener {

	private ClickedPoint lastCellOver = null;
	// private long timeLastCellOver=Long.MAX_VALUE;
	private long timeBeforeHoverPopup = 1000;

	private Timer aTimer = new Timer();
	private SimpleTimerTask mouseOverTask = new SimpleTimerTask();
	private Point lastMousePoint;

	private CellInfoToolTip tipFrame;

	private class SimpleTimerTask extends TimerTask {
		@Override
		public final void run() {
			ClickedPoint lastCellCopy = lastCellOver;
			if (lastCellCopy == null)
				return;
			tipFrame = new CellInfoToolTip(imp.getWindow(), false, lastCellCopy);

			tipFrame.setLocation(lastMousePoint);
			tipFrame.setVisible(true);

			cancel();
		}
	}

	@Override
	public void mouseExited(MouseEvent e) {
		super.mouseExited(e);
		lastCellOver = null;
	}

	private boolean pointWithinCell(int x, int y, ClickedPoint cell) {
		DoublePoint p = getCellGraphicCoordinates(cell);
		currentSlice = fixedDim3 < Double.MAX_VALUE ? fixedDim3 : imp.getSlice() - 1;
		double zClosenessToCenter = z0 == -1 ? 1 - Math.abs(p.z - currentSlice) / (labelDepth * 0.5f) : 1;
		float squaredDistance = (float) Math.pow(unscaledLabelDepth * zClosenessToCenter * magnification * 0.5, 2);

		boolean zOK = false;
		float sliceThickness = labelDepth * 0.5f;
		if (z0 != -1) {
			// z range set, probably by Z projector
			zOK = (z0 - p.z < sliceThickness) && (p.z - z1 < sliceThickness);

		} else
			zOK = (Math.abs(p.z - currentSlice) < labelDepth * 0.5);

		if ((p.t == displayedT) && zOK) {
			if ((p.x - x) * (p.x - x) + (p.y - y) * (p.y - y) <= squaredDistance) {
				return true;
			}
		}
		return false;
	}

	private static final int maxMouseDistanceToTip = 50;

	private ClickedPoint cellAtPoint(int ox, int oy) {
		ClickedPoint cellOver = null;
		List<ClickedPoint> pointCopy = new ArrayList<>(cells.getPoints());
		for (ClickedPoint cell : pointCopy) {
			if (pointWithinCell(ox, oy, cell)) {
				cellOver = cell;
				break;
			}
		}
		return cellOver;
	}

	private ClickedPoint draggedCell = null;
	private int dragXOrigin, dragYOrigin;

	@Override
	public void mouseReleased(MouseEvent e) {
		if (wasDragging()) {
			e.consume();
		} else
			super.mouseReleased(e);
		Utils.log("Mouse released; modifiers " + e.getModifiers() + "; extended modifiers " + e.getModifiersEx(),
				LogLevel.VERBOSE_DEBUG);
		draggedCell = null;
	}

	private boolean wasDragging() {
		long timeElapsed = System.currentTimeMillis() - timeMousePressed;
		return (draggedCell != null) && ((dragMovementAmplitude > 10) || (timeElapsed > 200));
	}

	private int dragMovementAmplitude;
	private long timeMousePressed;

	@Override
	public void mousePressed(MouseEvent e) {
		timeMousePressed = System.currentTimeMillis();
		dragMovementAmplitude = 0;
		aTimer.cancel();
		if (draggedCell != null) {
			// throw new IllegalStateException();
			Utils.log("Dragged cell not previously cleared", LogLevel.ERROR);
		}

		if (cells == null) {
			super.mousePressed(e);
			return;
		}
		int sx = e.getX();
		int sy = e.getY();
		int ox = sx;// offScreenX(sx);
		int oy = sy;// offScreenY(sy);

		dragXOrigin = offScreenX(sx);
		dragYOrigin = offScreenY(sy);

		draggedCell = cellAtPoint(ox, oy);
		if (draggedCell != null)
			e.consume();
		else
			super.mousePressed(e);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (wasDragging()) {
			e.consume();
		} else
			super.mouseClicked(e);
		draggedCell = null;
	}

	private static void setFieldNumberValue(Field f, Object o, double d) {
		try {
			if (f.getType().equals(java.lang.Float.TYPE)) {
				f.set(o, new Float(d));
			} else if (f.getType().equals(java.lang.Double.TYPE)) {
				f.set(o, d);
			} else
				throw new RuntimeException("Unhandle number type " + f.getType());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		aTimer.cancel();
		if ((tipFrame != null) && (tipFrame.isVisible())) {
			tipFrame.requestClose();
			tipFrame = null;
		}
		if (cells == null) {
			super.mouseDragged(e);
			return;
		}
		int sx = e.getX();
		int sy = e.getY();
		int ox = offScreenX(sx);
		int oy = offScreenY(sy);

		if (draggedCell != null) {
			try {
				float localXScaling = xScaling != 0 ? xScaling : 1;
				float localYScaling = yScaling != 0 ? yScaling : 1;

				dragMovementAmplitude += Math.abs(ox - dragXOrigin) + Math.abs(oy - dragYOrigin);

				// FIXME Don't know why the following works. Might break if axes of orthogonal views are changed around

				if (xField.getName().equals("x")) {
					setFieldNumberValue(xField, draggedCell, retrieveNumber(xField.get(draggedCell))
							+ (ox - dragXOrigin) * localXScaling);
					setFieldNumberValue(yField, draggedCell, retrieveNumber(yField.get(draggedCell))
							+ (oy - dragYOrigin) * localYScaling);
				} else {
					setFieldNumberValue(xField, draggedCell, retrieveNumber(xField.get(draggedCell))
							+ (ox - dragXOrigin) * localYScaling);
					setFieldNumberValue(yField, draggedCell, retrieveNumber(yField.get(draggedCell))
							+ (oy - dragYOrigin) * localXScaling);
				}

			} catch (IllegalArgumentException | IllegalAccessException e1) {
				throw new RuntimeException(e1);
			}
			// draggedCell.x+=ox-dragXOrigin;
			// draggedCell.y+=oy-dragYOrigin;
			dragXOrigin = ox;
			dragYOrigin = oy;
			cells.fireValueChanged(true, false);
			// this.repaint();
			return;
		} else
			super.mouseDragged(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {

		if (cells == null) {
			super.mouseMoved(e);
			return;
		}
		if (draggedCell != null)
			return;// Don't want to display mouseover windows when dragging a cell
		int sx = e.getX();
		int sy = e.getY();
		int ox = sx;// offScreenX(sx);
		int oy = sy;// offScreenY(sy);

		if (tipFrame != null) {
			if (!tipFrame.isVisible()) {
				tipFrame = null;
			} else {
				Point mouseLocation = e.getLocationOnScreen();
				Point topLeftTip = tipFrame.getLocationOnScreen();
				int distanceToUse =
						unscaledLabelDepth < maxMouseDistanceToTip ? unscaledLabelDepth : maxMouseDistanceToTip;
				Rectangle tipBounds =
						new Rectangle(topLeftTip.x - distanceToUse, topLeftTip.y - distanceToUse, tipFrame.getWidth()
								+ distanceToUse, tipFrame.getHeight() + distanceToUse);
				if (!tipBounds.contains(mouseLocation)) {
					tipFrame.requestClose();
					tipFrame = null;
				}
			}
		}

		ClickedPoint cellOver = cellAtPoint(ox, oy);

		if (cellOver == lastCellOver) {
			super.mouseMoved(e);
			return;
		}

		mouseOverTask.cancel();

		if (tipFrame != null) {
			tipFrame.requestClose();
			tipFrame = null;
		}

		lastCellOver = cellOver;
		if (lastCellOver != null) {
			mouseOverTask = new SimpleTimerTask();
			lastMousePoint = e.getLocationOnScreen();
			aTimer = new Timer();
			aTimer.schedule(mouseOverTask, timeBeforeHoverPopup);
		}

		super.mouseMoved(e);
	}

	private float markDiameter = 5;

	public float getMarkDiameter() {
		return markDiameter;
	}

	public void setMarkDiameter(float markDiameter) {
		this.markDiameter = markDiameter;
	}

	private float xScaling = 0, yScaling = 0;

	public float getxScaling() {
		return xScaling;
	}

	public void setxScaling(float xScaling) {
		this.xScaling = xScaling;
	}

	public float getyScaling() {
		return yScaling;
	}

	public void setyScaling(float yScaling) {
		this.yScaling = yScaling;
	}

	private double fixedDim3 = Double.MAX_VALUE;

	public double getFixedDim3() {
		return fixedDim3;
	}

	public void setFixedDim3(double fixedZ) {
		this.fixedDim3 = fixedZ;
	}

	public float getFixedDim4() {
		return fixedDim4;
	}

	public void setFixedDim4(float fixedT) {
		this.fixedDim4 = fixedT;
	}

	private float fixedDim4 = Float.MAX_VALUE;

	private static final long serialVersionUID = -9015464572924220986L;

	private PluginIOCells cells;

	private final Method xMethod, yMethod, zMethod, tMethod, xCalMethod, yCalMethod, zCalMethod;
	private final Field xField, yField;

	static final Class<ClickedPoint> pointClass = ClickedPoint.class;

	public ImageCanvasWithAnnotations(ImagePlus imp, PluginIOCells cells, Pair<Long, Long> range, String nameOfXDim,
			String nameOfYDim, String nameOfZDim, String nameOfTDim, String nameOfXCal, String nameOfYCal,
			String nameOfZCal) {
		super(imp);

		addKeyListener(this);
		if (cells != null)
			cells.addListener(this);

		this.cells = cells;
		if (range == null) {
			z0 = -1;
			z1 = -1;
		} else {
			z0 = Math.min(range.fst, range.snd);
			z1 = Math.max(range.fst, range.snd);
		}

		try {
			xMethod = pointClass.getMethod("get" + nameOfXDim, (Class<?>[]) null);
			xMethod.setAccessible(true);
			yMethod = pointClass.getMethod("get" + nameOfYDim, (Class<?>[]) null);
			yMethod.setAccessible(true);
			zMethod = pointClass.getMethod("get" + nameOfZDim, (Class<?>[]) null);
			zMethod.setAccessible(true);
			tMethod = pointClass.getMethod("get" + nameOfTDim, (Class<?>[]) null);
			tMethod.setAccessible(true);

			xField = pointClass.getField(nameOfXDim);
			yField = pointClass.getField(nameOfYDim);

			xCalMethod = pointClass.getMethod("get" + nameOfXCal, (Class<?>[]) null);
			xCalMethod.setAccessible(true);
			yCalMethod = pointClass.getMethod("get" + nameOfYCal, (Class<?>[]) null);
			yCalMethod.setAccessible(true);
			zCalMethod = pointClass.getMethod("get" + nameOfZCal, (Class<?>[]) null);
			zCalMethod.setAccessible(true);
		} catch (SecurityException | NoSuchFieldException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public void setCellsToOverlay(PluginIOCells cells) {
		if (this.cells != null) {
			this.cells.removeListener(this);
		}
		this.cells = cells;
		if (cells != null)
			cells.addListener(this);
	}

	private long z0, z1;

	public void setZRange(Pair<Long, Long> range) {
		if (range == null) {
			z0 = z1 = -1;
			return;
		}
		z0 = Math.min(range.fst, range.snd);
		z1 = Math.max(range.fst, range.snd);
	}

	private static List<String> getCellAnnotations(ClickedPoint p) {
		List<String> propNames = p.listNamesOfQuantifiedProperties;
		List<Float> quantifiedProperties = p.getQuantifiedProperties();
		// StringBuilder builder=new StringBuilder();
		List<String> list = new ArrayList<>();
		for (int propIndex = 0; propIndex < quantifiedProperties.size(); propIndex++) {
			if (propIndex >= propNames.size()) {
				Utils.log("property length name list too short", LogLevel.ERROR);
				continue;
			}
			String name = propNames.get(propIndex);
			if (name.contains("_anno_")) {
				if (quantifiedProperties.get(propIndex) > 0) {
					list.add(name.substring("_anno_".length()));
					// builder.append(name.substring("_anno_".length()));
					// builder.append("\n");
				}
			}
		}
		return list;
		// return builder.toString();
	}

	private Font font = new Font("SansSerif", Font.BOLD, 10);
	private Rectangle srcRect1 = null;

	private static RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_OFF);

	static {
		renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
	}

	private float transparency = 0.5f;

	public float getTransparency() {
		return transparency;
	}

	public void setTransparency(float transparency) {
		this.transparency = transparency;
		alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - transparency);
		repaint();
	}

	private AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - transparency);

	private double xCalibrationCorrection, yCalibrationCorrection, zCalibrationCorrection;
	private double currentSlice, displayedT;

	private void initializeCalibrationCorrection(ClickedPoint cell) {
		double xcal = 0, ycal = 0, zcal = 0;
		try {
			xcal = (Float) xCalMethod.invoke(cell);
			ycal = (Float) yCalMethod.invoke(cell);
			zcal = (Float) zCalMethod.invoke(cell);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		xCalibrationCorrection = xcal == 0 ? 1f : 1f / xcal;
		yCalibrationCorrection = ycal == 0 ? 1f : 1f / ycal;
		zCalibrationCorrection = zcal == 0 ? 1f : 1f / zcal;

		if (xScaling > 0)
			xCalibrationCorrection /= xScaling;
		if (yScaling > 0)
			yCalibrationCorrection /= yScaling;
	}

	private static double retrieveNumber(Object o) {
		if (o instanceof Float)
			return (Float) o;
		else if (o instanceof Double)
			return (Double) o;
		else if (o instanceof Integer)
			return (Integer) o;
		throw new RuntimeException("Object " + o + " is not a recognized number");
	}

	private DoublePoint getCellGraphicCoordinates(ClickedPoint cell) {
		if (srcRect1 == null)
			srcRect1 = getSrcRect();
		DoublePoint result = new DoublePoint();
		double z = 0, y = 0, x = 0, t = 0;
		try {
			x = retrieveNumber(xMethod.invoke(cell));
			y = retrieveNumber(yMethod.invoke(cell));
			z = retrieveNumber(zMethod.invoke(cell));
			t = (Float) tMethod.invoke(cell);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		initializeCalibrationCorrection(cell);

		result.x = (x * xCalibrationCorrection - srcRect1.x) * (float) magnification;
		result.y = (y * yCalibrationCorrection - srcRect1.y) * (float) magnification;
		result.z = z * zCalibrationCorrection;
		result.t = t;
		return result;
	}

	private static final class DoublePoint {
		public double x, y, z, t;

		@SuppressWarnings("unused")
		public DoublePoint(double x, double y, double z, double t) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.t = t;
		}

		public DoublePoint() {
		}
	}

	// Use double buffer to reduce flicker when drawing complex ROIs.
	// Author: Erik Meijering
	@Override
	protected Image paintDoubleBuffered(Graphics g) {
		final int srcRectWidthMag = (int) (srcRect.width * magnification);
		final int srcRectHeightMag = (int) (srcRect.height * magnification);
		if (offScreenImage == null || offScreenWidth != srcRectWidthMag || offScreenHeight != srcRectHeightMag) {
			offScreenImage = createImage(srcRectWidthMag, srcRectHeightMag);
			offScreenWidth = srcRectWidthMag;
			offScreenHeight = srcRectHeightMag;
		}
		Roi roi = imp.getRoi();
		if (imageUpdated) {
			imageUpdated = false;
			imp.updateImage();
		}
		Graphics offScreenGraphics = offScreenImage.getGraphics();
		Java2.setBilinearInterpolation(offScreenGraphics, Prefs.interpolateScaledImages);
		Image img = imp.getImage();
		if (img != null)
			offScreenGraphics.drawImage(img, 0, 0, srcRectWidthMag, srcRectHeightMag, srcRect.x, srcRect.y, srcRect.x
					+ srcRect.width, srcRect.y + srcRect.height, null);
		if (overlay != null)
			drawOverlay(offScreenGraphics);
		if (showAllROIs)
			drawAllROIs(offScreenGraphics);
		if (roi != null)
			drawRoi(roi, offScreenGraphics);
		if (srcRect.width < imageWidth || srcRect.height < imageHeight)
			drawZoomIndicator(offScreenGraphics);
		if (IJ.debugMode)
			showFrameRate(offScreenGraphics);
		// g.drawImage(offScreenImage, 0, 0, null);
		return offScreenImage;
	}

	@Override
	public void paint(Graphics g) {
		srcRect1 = getSrcRect();
		Image buffered = paintDoubleBuffered(g);
		addPipelineOverlaysDoubleBuffered(buffered.getGraphics());
		g.drawImage(buffered, 0, 0, null);
	}

	public void addPipelineOverlaysDoubleBuffered(Graphics g) {
		// super.paint(g);
		if (cells == null)
			return;

		Graphics2D g2 = (Graphics2D) g;
		Composite originalComposite = null;

		if (transparency > 0) {
			originalComposite = g2.getComposite();
			g2.setComposite(alpha);
		}

		g2.setRenderingHints(renderHints);

		g2.setStroke(new BasicStroke(lineThickness));
		g2.setColor(displayColor);
		g2.setFont(font);

		currentSlice = fixedDim3 < Double.MAX_VALUE ? fixedDim3 : imp.getSlice() - 1;
		displayedT = fixedDim4 < Float.MAX_VALUE ? fixedDim4 : imp.getFrame() - 1;

		double sliceThickness = labelDepth * 0.5f;

		List<ClickedPoint> pointCopy = new ArrayList<>(cells.getPoints());

		setMarkDiameter(labelDepth);

		boolean isShowingCellsUpdated = false;

		for (ClickedPoint cell : pointCopy) {
			// g2.setColor(mv.getColor());

			DoublePoint p = getCellGraphicCoordinates(cell);

			boolean zOK;
			double zClosenessToCenter;
			// int greenIntensity;
			if (z0 == -1) {
				zOK = Math.abs(p.z - currentSlice) < sliceThickness;
				zClosenessToCenter = 1 - Math.abs(p.z - currentSlice) / sliceThickness;
				// greenIntensity=(int) (255f * (1f - Math.abs(p.z-currentSlice)/sliceThickness) );
			} else {// range set explicitly, probably for projection of a large set of z slices
				double iA = p.z;
				double iB =
						p.z + (cell.hasQuantifiedProperty("zThickness") ? cell.getQuantifiedProperty("zThickness") : 0);

				double jA = z0 - sliceThickness;
				double jB = z1 + sliceThickness;

				zOK = !((iB < jA) || (jB < iA));

				// boolean zOK=(z0-p.z<sliceThickness)&&(p.z-z1<sliceThickness);

				zClosenessToCenter = 1;// don't want to vary diameter for z projections
				currentSlice = z0;
				// greenIntensity=255;
			}

			if ((p.t == displayedT) && zOK) {
				isShowingCells = true;
				isShowingCellsUpdated = true;
				// If there is a calibration, the cell's getx and gety methods will return
				// the result in physical dimensions, not pixels; we need to convert back

				// g2.setColor(new Color(0, greenIntensity, 0));

				// Rectangle2D.Float r=new Rectangle2D.Float((int) xM-unscaledLabelDepth/2, (int)
				// yM-unscaledLabelDepth/2,
				// unscaledLabelDepth, unscaledLabelDepth);
				// g2.fill(r);

				double diameter = unscaledLabelDepth * (float) magnification * zClosenessToCenter;

				g2.drawOval((int) (p.x - diameter / 2f), (int) (p.y - diameter / 2f), (int) diameter, (int) diameter);
				int yPosition = (int) p.y;
				// g2.setColor(new Color(0, 255, 0));
				for (String annotation : getCellAnnotations(cell)) {
					g2.drawString(annotation, (int) (p.x + unscaledLabelDepth / 2 + 3), yPosition);
					yPosition += g.getFontMetrics().getHeight();
				}
			}

		}
		if (transparency > 0)
			g2.setComposite(originalComposite);
		isShowingCells = isShowingCellsUpdated;
	}

	private int labelDepth = 1;
	private int unscaledLabelDepth = 1;

	private float lineThickness = 3;

	public float getLineThickness() {
		return lineThickness;
	}

	public int getUnscaledLabelDepth() {
		return unscaledLabelDepth;
	}

	public void setUnscaledLabelDepth(int unscaledLabelDepth) {
		this.unscaledLabelDepth = unscaledLabelDepth;
		repaint();
	}

	public void setLabelDepth(int newDepth) {
		labelDepth = newDepth;
		repaint();
	}

	public int getLabelDepth() {
		return labelDepth;
	}

	public void setHoverDelay(float newHoverDelay) {
		timeBeforeHoverPopup = (long) (1000 * newHoverDelay);
	}

	public float getHoverDelay() {
		return timeBeforeHoverPopup / 1000;
	}

	public void setLineThickness(float newThickness) {
		lineThickness = newThickness;
		repaint();
	}

	private Color displayColor = new Color(0, 255, 0);

	public Color getDisplayColor() {
		return displayColor;
	}

	public void setDisplayColor(Color displayColor) {
		if (displayColor != null)
			this.displayColor = displayColor;
		repaint();
	}

	LinkedList<KeyListener> privateKeyListeners = new LinkedList<>();

	public void addPrivateKeyListener(KeyListener l) {
		privateKeyListeners.add(l);
	}

	public void removePrivateKeyListener(KeyListener l) {
		privateKeyListeners.remove(l);
	}

	@Override
	public void keyTyped(KeyEvent e) {
		Utils.log("Key typed " + e.getKeyChar(), LogLevel.DEBUG);
		for (KeyListener l : privateKeyListeners) {
			l.keyTyped(e);
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		Utils.log("Key pressed " + e.getKeyChar(), LogLevel.DEBUG);
		for (KeyListener l : privateKeyListeners) {
			l.keyPressed(e);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		for (KeyListener l : privateKeyListeners) {
			l.keyReleased(e);
		}
	}

	@Override
	public void pluginIOValueChanged(boolean stillChanging, IPluginIO pluginIOWhoseValueChanged) {
		repaint();
	}

	@Override
	public void pluginIOViewEvent(PluginIOView trigger, boolean stillChanging, AWTEvent event) {

	}

	private volatile boolean isShowingCells;

	public boolean isShowingCells() {
		return isShowingCells;
	}

}
