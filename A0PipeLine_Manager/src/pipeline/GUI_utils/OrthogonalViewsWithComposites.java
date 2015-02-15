/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import ij.CommandListener;
import ij.CompositeImage;
import ij.Executer;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.ScrollbarWithLabel;
import ij.gui.StackWindow;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.GeneralPath;
import java.awt.image.ColorModel;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import pipeline.GUI_utils.image_with_toolbar.ImageCanvasWithAnnotations;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOListener;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.MouseEventPlugin;

/**
 * 
 * @author Dimiter Prodanov
 *         IMEC
 * 
 * @author Modified by Olivier Cinquin 2010 to handle composite images
 * 
 * @acknowledgments Many thanks to Jerome Mutterer for the code contributions and testing.
 *                  Thanks to Wayne Rasband for the code that properly handles the image magnification.
 * 
 * @version 1.2 28 April 2009
 *          - added support for arrow keys
 *          - fixed a bug in the cross position calculation
 *          - added FocusListener behavior
 *          - added support for magnification factors
 *          1.1.6 31 March 2009
 *          - added AdjustmentListener behavior thanks to Jerome Mutterer
 *          - improved pane visualization
 *          - added window rearrangement behavior. Initial code suggested by Jerome Mutterer
 *          - bug fixes by Wayne Raspband
 *          1.1 24 March 2009
 *          - improved projection image resizing
 *          - added ImageListener behaviors
 *          - added check-ups
 *          - improved pane updating
 *          1.0.5 23 March 2009
 *          - fixed pane updating issue
 *          1.0 21 March 2009
 * 
 * @contents This plugin projects dynamically orthogonal XZ and YZ views of a stack.
 *           The output images are calibrated, which allows measurements to be performed more easily.
 */

public class OrthogonalViewsWithComposites implements MouseListener, MouseMotionListener, KeyListener, ActionListener,
		AdjustmentListener, MouseWheelListener, FocusListener, CommandListener, PluginIOListener {

	/**
	 * True if the owner of the views wants the clicks in the "master" ImagePlus ignored (for example if those
	 * clicks are used to edit the image).
	 */
	public boolean ignoreClicks;

	private ImageWindow win;
	private ImagePlus imp;
	private ImageCanvas canvas;
	private ImagePlus xzImage, yzImage; // Will hold a CompositeImage if there is more than 1 channel
	private ImagePlus xzImageAsImp, yzImageAsImp;
	private ImageProcessor[] xzImages;
	private ImageProcessor[] yzImages;
	private double ax, ay, az;
	// private static boolean rotate=(boolean)Prefs.getBoolean(YROT,false);
	// private static boolean sticky=(boolean)Prefs.getBoolean(SPANELS,false);
	private static boolean rotate = false;
	private static boolean sticky = true;

	private int xyX, xyY;
	private Calibration cal = null, cal_xz = new Calibration(), cal_yz = new Calibration();
	private Color color = Roi.getColor();
	private Updater updater = new Updater();
	private double min, max;
	private boolean flipXZ;
	private boolean syncZoom = true;
	private Point crossLoc;

	private static void fillStack(int nSlices, ImagePlus impToGetTypeFrom, int width, int height, ImageStack stack) {
		for (int slice = 0; slice < nSlices; slice++) {
			if (impToGetTypeFrom.getProcessor() instanceof FloatProcessor) {
				stack.addSlice("", new FloatProcessor(width, height));
			} else if (impToGetTypeFrom.getProcessor() instanceof ByteProcessor) {
				stack.addSlice("", new ByteProcessor(width, height));
			} else if (impToGetTypeFrom.getProcessor() instanceof ShortProcessor) {
				stack.addSlice("", new ShortProcessor(width, height));
			}
		}
	}

	private transient boolean listenersAdded = false;

	private static boolean seenSmallRotation = false;

	public void show() {
		if (yzImage != null) {
			yzImage.show();
		}
		if (xzImage != null) {
			xzImage.show();
		}

		if (listenersAdded)
			return;

		yzImage.getWindow().getCanvas().addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// Translate click coordinates
				ImageCanvas canvas = yzImage.getWindow().getCanvas();
				int y = canvas.offScreenY(e.getY());
				int z =
						(int) (((float) canvas.offScreenX(e.getX())) * ((float) imp.getNSlices()) / (yzImage.getWidth()));
				int x = crossLoc.x;

				crossLoc.y = y;
				update();

				ClickedPoint p = new ClickedPoint(x, y, z + 1, 0, 0, -999, -999);
				List<ClickedPoint> clickList = new ArrayList<>();
				clickList.add(p);
				PluginIOCells click = new PluginIOCells(clickList);
				try {
					masterView.mouseClicked(click, false, e);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		});

		xzImage.getWindow().getCanvas().addMouseWheelListener(e -> {
			if (e.getModifiers() != 0) {
				xzImage.getWindow().dispatchEvent(e);
				return;
			}
			if (e.getModifiers() != 0)
				return;
			int rotation = e.getWheelRotation();
			if (!seenSmallRotation) {
				if (Math.abs(rotation) < 6)
					seenSmallRotation = true;
				else
					rotation = rotation / 10;
			}
			incrementY(rotation);
		});

		yzImage.getWindow().getCanvas().addMouseWheelListener(e -> {
			if (e.getModifiers() != 0) {
				yzImage.getWindow().dispatchEvent(e);
				return;
			}
			int rotation = e.getWheelRotation();
			if (!seenSmallRotation) {
				if (Math.abs(rotation) < 6)
					seenSmallRotation = true;
				else
					rotation = rotation / 10;
			}
			incrementX(rotation);
		});

		yzImage.getWindow().getCanvas().setFocusable(true);
		xzImage.getWindow().getCanvas().setFocusable(true);

		((ImageCanvasWithAnnotations) yzImage.getWindow().getCanvas()).addPrivateKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				keyListener.keyTyped(e);
				e.consume();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				keyListener.keyReleased(e);
				e.consume();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				keyListener.keyPressed(e);
				e.consume();
			}
		});

		((ImageCanvasWithAnnotations) xzImage.getWindow().getCanvas()).addPrivateKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
				keyListener.keyTyped(e);
				e.consume();
			}

			@Override
			public void keyReleased(KeyEvent e) {
				keyListener.keyReleased(e);
				e.consume();
			}

			@Override
			public void keyPressed(KeyEvent e) {
				keyListener.keyPressed(e);
				e.consume();
			}
		});

		xzImage.getWindow().getCanvas().addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// Translate click coordinates
				ImageCanvas canvas = xzImage.getWindow().getCanvas();
				int x = canvas.offScreenX(e.getX());
				int z =
						(int) (((float) canvas.offScreenY(e.getY())) * ((float) imp.getNSlices()) / (xzImage
								.getHeight()));
				int y = crossLoc.y;

				crossLoc.x = x;
				update();

				ClickedPoint p = new ClickedPoint(x, y, z + 1, 0, 0, -999, -999);
				List<ClickedPoint> clickList = new ArrayList<>();
				clickList.add(p);
				PluginIOCells click = new PluginIOCells(clickList);
				try {
					masterView.mouseClicked(click, false, e);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		});
		listenersAdded = true;
		update();
	}

	/**
	 * Used to notify the main window when clicks occur in the orthogonal views
	 */
	private MouseEventPlugin masterView;

	private KeyListener keyListener;

	private double araty, aratx;

	public OrthogonalViewsWithComposites(ImagePlus imp, MouseEventPlugin masterView, KeyListener keyListener,
			PluginIOCells cellsToOverlay) {
		this.masterView = masterView;
		this.keyListener = keyListener;
		this.imp = imp;
		int nChannels = imp.getNChannels();
		int height = imp.getHeight();
		int width = imp.getWidth();
		int depth = imp.getNSlices();
		if (imp.getStackSize() == 1) {
			Utils.displayMessage("Othogonal Views: this command requires a stack.", true, LogLevel.ERROR);
			return;
		}
		cal = this.imp.getCalibration();
		double calx = cal.pixelWidth;
		double caly = cal.pixelHeight;
		double calz = cal.pixelDepth;

		if (calx == 0 || caly == 0 || calz == 0) {
			calx = 1;
			caly = 1;
			calz = 1;
			Utils.log(
					"Warning: no calibration information for composite view; assuming all pixel dimensions are the same",
					LogLevel.WARNING);
		}

		ax = 1.0;
		ay = caly / calx;
		az = calz / calx;

		araty = az / ay;
		int width2 = (int) Math.round(depth * (az / ay));
		int height2 = (int) Math.round(imp.getHeight() * ay);

		if (yzImage == null || yzImage.getHeight() != imp.getHeight() || yzImage.getBitDepth() != imp.getBitDepth()) {
			ImageStack stack = new ImageStack(width2, height);
			fillStack(nChannels, imp, width2, height, stack);
			yzImageAsImp = new ImagePlus();
			yzImageAsImp.setStack(stack);
			yzImageAsImp.setDimensions(nChannels, 1, 1);
			if (imp instanceof CompositeImage) {
				yzImage = new CompositeImage(yzImageAsImp, CompositeImage.COMPOSITE, null);// imp
				imp.getLuts();
				((CompositeImage) yzImage).setLuts(imp.getLuts());
			} else
				yzImage = yzImageAsImp;
			yzImage.setTitle("yz orth view");
			ImageCanvasWithAnnotations yzCanvas =
					new ImageCanvasWithAnnotations(yzImage, cellsToOverlay, null, "z", "y", "x", "t", "xyCalibration",
							"zCalibration", "xyCalibration");
			yzCanvas.setName("yz orth");
			yzCanvas.setyScaling(1 / (float) araty);// (((float) imp.getNSlices())/(yzImage.getWidth() ));
			yzImage.setWindow(new StackWindow(yzImage, yzCanvas));
		}

		aratx = az / ax;
		width2 = (int) Math.round(imp.getWidth() * ax);
		height2 = (int) Math.round(depth * aratx);// imp.getHeight()

		// if (xzImage!=null)
		// IJ.log(imp+"  "+xzImage+"  "+xzImage.getHeight()+"  "+imp.getHeight()+"  "+xzImage.getBitDepth()+"  "+imp.getBitDepth());
		if (xzImage == null || xzImage.getWidth() != imp.getWidth() || xzImage.getBitDepth() != imp.getBitDepth()) {
			ImageStack stack = new ImageStack(width, height2);
			fillStack(nChannels, imp, width, height2, stack);
			xzImageAsImp = new ImagePlus();
			xzImageAsImp.setStack(stack);
			xzImageAsImp.setDimensions(nChannels, 1, 1);
			if (imp instanceof CompositeImage) {
				xzImage = new CompositeImage(xzImageAsImp, CompositeImage.COMPOSITE, null);
				((CompositeImage) xzImage).setLuts(imp.getLuts());
			} else
				xzImage = xzImageAsImp;
			xzImage.setTitle("yz orth view");

			ImageCanvasWithAnnotations xzCanvas =
					new ImageCanvasWithAnnotations(xzImage, cellsToOverlay, null, "x", "z", "y", "t", "xyCalibration",
							"zCalibration", "xyCalibration");
			xzCanvas.setName("xz orth");
			xzCanvas.setyScaling(((float) imp.getNSlices()) / (xzImage.getHeight()));
			xzImage.setWindow(new StackWindow(xzImage, xzCanvas));
		}
		ImageProcessor ip = imp.getProcessor();

		if (!(imp instanceof CompositeImage)) {
			min = imp.getDisplayRangeMin();
			max = imp.getDisplayRangeMax();
			yzImage.setDisplayRange(min, max);
			xzImage.setDisplayRange(min, max);
		}
		win = imp.getWindow();
		canvas = win.getCanvas();
		addListeners(canvas);
		imp.killRoi();
		crossLoc = new Point(imp.getWidth() / 2, imp.getHeight() / 2);
		calibrate();
		if (createProcessors(imp)) {
			if (ip.isColorLut() || ip.isInvertedLut()) {
				ColorModel cm = ip.getColorModel();
				for (ImageProcessor f : xzImages)
					f.setColorModel(cm);
				for (ImageProcessor f : yzImages)
					f.setColorModel(cm);
			}
			update();
		} else
			dispose();

	}

	private void addListeners(ImageCanvas canvass) {
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
		canvas.addKeyListener(this);
		// win.addWindowListener ((WindowListener) this);
		win.addMouseWheelListener(this);
		win.addFocusListener(this);
		Component[] c = win.getComponents();
		((ScrollbarWithLabel) c[1]).addAdjustmentListener(this);
		Executer.addCommandListener(this);
	}

	private void calibrate() {
		double arat = az / ax;
		double brat = az / ay;
		String unit = cal.getUnit();
		double o_depth = cal.pixelDepth;
		double o_height = cal.pixelHeight;
		double o_width = cal.pixelWidth;
		cal_xz.setUnit(unit);
		if (rotate) {
			cal_xz.pixelHeight = o_depth / arat;
			cal_xz.pixelWidth = o_width * ax;
		} else {
			cal_xz.pixelHeight = o_width * ax;// o_depth/arat;
			cal_xz.pixelWidth = o_depth / arat;
		}
		xzImage.setCalibration(cal_xz);
		cal_yz.setUnit(unit);
		cal_yz.pixelWidth = o_height * ay;
		cal_yz.pixelHeight = o_depth / brat;
		yzImage.setCalibration(cal_yz);
	}

	private void updateMagnification(int x, int y) {
		double magnification = win.getCanvas().getMagnification();
		int z = imp.getCurrentSlice() - 1;
		ImageWindow win1 = xzImage.getWindow();
		if (win1 == null)
			return;
		ImageCanvas ic1 = win1.getCanvas();
		double mag1 = ic1.getMagnification();
		double arat = az / ax;
		int zcoord = (int) (arat * z);

		if (flipXZ)
			zcoord = (int) (arat * (imp.getStackSize() - z));
		while (mag1 < magnification) {
			ic1.zoomIn(x, zcoord);
			mag1 = ic1.getMagnification();
		}
		while (mag1 > magnification) {
			ic1.zoomOut(x, zcoord);
			mag1 = ic1.getMagnification();
		}
		ImageWindow win2 = yzImage.getWindow();
		if (win2 == null)
			return;
		ImageCanvas ic2 = win2.getCanvas();
		double mag2 = ic2.getMagnification();
		zcoord = (int) (arat * z);
		while (mag2 < magnification) {
			ic2.zoomIn(zcoord, y);
			mag2 = ic2.getMagnification();
		}
		while (mag2 > magnification) {
			ic2.zoomOut(zcoord, y);
			mag2 = ic2.getMagnification();
		}
	}

	void updateViews(Point p, ImagePlus imp) {
		int nChannels = imp.getNChannels();

		updateXZView(p, imp);

		double arat = az / ax;
		int width2 = (int) Math.round(imp.getWidth() * ax);
		int height2 = (int) Math.round(imp.getNSlices() * arat);// xx Height()
		int width = imp.getWidth();
		int height = imp.getHeight();

		if (imp.getWidth() != xzImages[0].getWidth() || height2 != xzImages[0].getHeight()) {
			// Utils.log("change in dimensions from "+xzImages[0].getWidth()+" "+width2+" "+xzImages[0].getHeight()+" "+height2,LogLevel.DEBUG);
			for (int channel = 0; channel < nChannels; channel++) {
				xzImages[channel].setInterpolate(true);
				ImageProcessor sfp1 = xzImages[channel].resize(width, height2);
				System.arraycopy(sfp1.getPixels(), 0, xzImageAsImp.getStack().getPixels(channel + 1), 0, width
						* height2);
			}
		} else {
			for (int channel = 0; channel < nChannels; channel++) {
				System.arraycopy(xzImages[channel].getPixels(), 0, xzImageAsImp.getStack().getPixels(channel + 1), 0,
						width * height2);
			}
		}

		if (xzImage.isVisible())
			xzImage.updateAndRepaintWindow();

		if (rotate)
			updateYZView(p, imp);
		else
			updateZYView(p, imp);

		arat = az / ay;
		width2 = (int) Math.round(yzImages[0].getWidth() * arat);
		height2 = (int) Math.round(yzImages[0].getHeight() * ay);
		if (rotate) {
			int tmp = width2;
			width2 = height2;
			height2 = tmp;
		}
		if (width2 != yzImages[0].getWidth() || height != yzImages[0].getHeight()) {
			for (int channel = 0; channel < nChannels; channel++) {
				yzImages[channel].setInterpolate(true);
				ImageProcessor sfp2 = yzImages[channel].resize(width2, height);
				System.arraycopy(sfp2.getPixels(), 0, yzImageAsImp.getStack().getPixels(channel + 1), 0, width2
						* height);
			}
		} else {
			for (int channel = 0; channel < nChannels; channel++) {
				System.arraycopy(yzImages[channel].getPixels(), 0, yzImageAsImp.getStack().getPixels(channel + 1), 0,
						width2 * height);
			}
		}
		if (yzImage.isVisible())
			yzImage.updateAndRepaintWindow();

		calibrate();

	}

	void arrangeWindows(boolean sticky) {
		ImageWindow xyWin = imp.getWindow();
		if (xyWin == null)
			return;
		Point loc = xyWin.getLocation();
		if ((xyX != loc.x) || (xyY != loc.y)) {
			xyX = loc.x;
			xyY = loc.y;
			ImageWindow yzWin = null;
			long start = System.currentTimeMillis();
			while (yzWin == null && (System.currentTimeMillis() - start) <= 2500L) {
				yzWin = yzImage.getWindow();
				if (yzWin == null)
					IJ.wait(50);
			}
			if (yzWin != null)
				yzWin.setLocation(xyX + xyWin.getWidth(), xyY);
			ImageWindow xzWin = null;
			start = System.currentTimeMillis();
			while (xzWin == null && (System.currentTimeMillis() - start) <= 2500L) {
				xzWin = xzImage.getWindow();
				if (xzWin == null)
					IJ.wait(50);
			}
			if (xzWin != null)
				xzWin.setLocation(xyX, xyY + xyWin.getHeight());
			/*
			 * if (firstTime) {
			 * imp.getWindow().toFront();
			 * imp.setSlice(imp.getStackSize()/2);
			 * firstTime = false;
			 * }
			 */
		}
	}

	private static ImageProcessor[] createFloatProcessorArray(ImagePlus imp2, int x, int y) {
		int nChannels = imp2.getNChannels();
		ImageProcessor[] result = new ImageProcessor[nChannels];
		for (int i = 0; i < nChannels; i++) {
			result[i] = new FloatProcessor(x, y);
		}
		return result;
	}

	private static ImageProcessor[] createByteProcessorArray(ImagePlus imp2, int x, int y) {
		int nChannels = imp2.getNChannels();

		ImageProcessor[] result = new ImageProcessor[nChannels];
		for (int i = 0; i < nChannels; i++) {
			result[i] = new ByteProcessor(x, y);
		}
		return result;
	}

	private static ImageProcessor[] createShortProcessorArray(ImagePlus imp2, int x, int y) {
		int nChannels = imp2.getNChannels();

		ImageProcessor[] result = new ImageProcessor[nChannels];
		for (int i = 0; i < nChannels; i++) {
			result[i] = new ShortProcessor(x, y);
		}
		return result;
	}

	/**
	 * @param imp2
	 *            - used to get the dimensions of the new ImageProcessors
	 * @return
	 */
	boolean createProcessors(ImagePlus imp2) {
		ImageProcessor ip = imp2.getStack().getProcessor(1);
		int width = imp2.getWidth();
		int height = imp2.getHeight();

		int depth = imp2.getNSlices();// /nChannels
		double arat = 1.0;// az/ax;
		double brat = 1.0;// az/ay;
		int za = (int) (depth * arat);
		int zb = (int) (depth * brat);

		if (ip instanceof FloatProcessor) {
			xzImages = createFloatProcessorArray(imp2, width, za);
			if (rotate)
				yzImages = createFloatProcessorArray(imp2, height, zb);
			else
				yzImages = createFloatProcessorArray(imp2, zb, height);
			return true;
		}

		if (ip instanceof ByteProcessor) {
			xzImages = createByteProcessorArray(imp2, width, za);
			if (rotate)
				yzImages = createByteProcessorArray(imp2, height, zb);
			else
				yzImages = createByteProcessorArray(imp2, zb, height);
			return true;
		}

		if (ip instanceof ShortProcessor) {
			xzImages = createShortProcessorArray(imp2, width, za);
			if (rotate)
				yzImages = createShortProcessorArray(imp2, height, zb);
			else
				yzImages = createShortProcessorArray(imp2, zb, height);
			return true;
		}
		return false;
	}

	void updateXZView(Point p, ImagePlus is) {
		int width = is.getWidth();
		int nChannels = is.getNChannels();
		int depth = is.getNSlices();// /nChannels
		ImageProcessor ip = is.getProcessor();

		int y = p.y;
		setFixedDim3(xzImage, y);

		// XZ
		if (ip instanceof ShortProcessor) {
			for (int channel = 0; channel < nChannels; channel++) {
				short[] newpix = new short[width * depth];
				for (int i = 0; i < depth; i++) {
					Object pixels = is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();
					if (flipXZ)
						System.arraycopy(pixels, width * y, newpix, width * (depth - i - 1), width);
					else {
						System.arraycopy(pixels, width * y, newpix, width * i, width);
						// System.arraycopy(pixels, width*y, xzImageAsImp.getImageStack().getPixels(channel+1), width*i,
						// width);
					}
				}
				xzImages[channel].setPixels(newpix);
			}
			return;
		}

		if (ip instanceof ByteProcessor) {

			for (int channel = 0; channel < nChannels; channel++) {
				byte[] newpix = new byte[width * depth];
				for (int i = 0; i < depth; i++) {
					Object pixels = is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();
					if (flipXZ)
						System.arraycopy(pixels, width * y, newpix, width * (depth - i - 1), width);
					else
						System.arraycopy(pixels, width * y, newpix, width * i, width);
				}
				xzImages[channel].setPixels(newpix);
			}
			return;
		}

		if (ip instanceof FloatProcessor) {
			for (int channel = 0; channel < nChannels; channel++) {
				float[] newpix = new float[width * depth];
				for (int i = 0; i < depth; i++) {
					Object pixels = is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();
					if (flipXZ)
						System.arraycopy(pixels, width * y, newpix, width * (depth - i - 1), width);
					else
						System.arraycopy(pixels, width * y, newpix, width * i, width);
				}
				xzImages[channel].setPixels(newpix);
			}
			return;
		}

	}

	void updateYZView(Point p, ImagePlus is) {
		int width = is.getWidth();
		int height = is.getHeight();
		int nChannels = is.getNChannels();
		int depth = is.getNSlices();// /nChannels
		ImageProcessor ip = is.getStack().getProcessor(1);
		int x = p.x;
		setFixedDim3(yzImage, x);

		if (ip instanceof FloatProcessor) {
			for (int channel = 0; channel < nChannels; channel++) {
				float[] newpix = new float[depth * height];
				for (int i = 0; i < depth; i++) {
					float[] pixels = (float[]) is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();// toFloatPixels(pixels);
					for (int j = 0; j < height; j++)
						newpix[(depth - i - 1) * height + j] = pixels[x + j * width];
				}
				yzImages[channel].setPixels(newpix);
			}
		}

		if (ip instanceof ByteProcessor) {
			for (int channel = 0; channel < nChannels; channel++) {
				byte[] newpix = new byte[depth * height];
				for (int i = 0; i < depth; i++) {
					byte[] pixels = (byte[]) is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();
					for (int j = 0; j < height; j++)
						newpix[(depth - i - 1) * height + j] = pixels[x + j * width];
				}
				yzImages[channel].setPixels(newpix);
			}
		}

		if (ip instanceof ShortProcessor) {
			for (int channel = 0; channel < nChannels; channel++) {
				short[] newpix = new short[depth * height];
				for (int i = 0; i < depth; i++) {
					short[] pixels = (short[]) is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();
					for (int j = 0; j < height; j++)
						newpix[(depth - i - 1) * height + j] = pixels[x + j * width];
				}
				yzImages[channel].setPixels(newpix);
			}
		}

	}

	void updateZYView(Point p, ImagePlus is) {
		int width = is.getWidth();
		int height = is.getHeight();
		int nChannels = is.getNChannels();
		int depth = is.getNSlices();// /nChannels
		ImageProcessor ip = is.getStack().getProcessor(1);
		int x = p.x;

		setFixedDim3(yzImage, x);

		if (ip instanceof FloatProcessor) {
			for (int channel = 0; channel < nChannels; channel++) {
				float[] newpix = new float[depth * height];
				for (int i = 0; i < depth; i++) {
					float[] pixels = (float[]) is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();
					for (int y = 0; y < height; y++)
						newpix[i + y * depth] = pixels[x + y * width];
				}
				yzImages[channel].setPixels(newpix);
			}
		}

		if (ip instanceof ByteProcessor) {
			for (int channel = 0; channel < nChannels; channel++) {
				byte[] newpix = new byte[depth * height];
				for (int i = 0; i < depth; i++) {
					byte[] pixels = (byte[]) is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();// toFloatPixels(pixels);
					for (int y = 0; y < height; y++)
						newpix[i + y * depth] = pixels[x + y * width];
				}
				yzImages[channel].setPixels(newpix);
			}
		}

		if (ip instanceof ShortProcessor) {
			for (int channel = 0; channel < nChannels; channel++) {
				short[] newpix = new short[depth * height];
				for (int i = 0; i < depth; i++) {
					short[] pixels = (short[]) is.getStack().getProcessor(channel + i * nChannels + 1).getPixels();// toFloatPixels(pixels);
					for (int y = 0; y < height; y++)
						newpix[i + y * depth] = pixels[x + y * width];
				}
				yzImages[channel].setPixels(newpix);
			}
		}

	}

	/** draws the crosses in the images */
	static void drawCross(ImagePlus imp, Point p, GeneralPath path) {
		int width = imp.getWidth();
		int height = imp.getHeight();
		float x = p.x;
		float y = p.y;
		path.moveTo(0f, y);
		path.lineTo(width, y);
		path.moveTo(x, 0f);
		path.lineTo(x, height);
	}

	@SuppressWarnings("deprecation")
	void dispose() {
		if (updater != null)
			updater.quit();
		updater = null;
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		canvas.removeKeyListener(this);
		canvas.setDisplayList(null);
		canvas.setCustomRoi(false);
		ImageWindow win1 = xzImage.getWindow();
		if (win1 != null) {
			win1.getCanvas().setDisplayList(null);
			win1.getCanvas().removeKeyListener(this);
			xzImage.changes = false;
			if (xzImage.isVisible())
				xzImage.close();
		}
		ImageWindow win2 = yzImage.getWindow();
		if (win2 != null) {
			win2.getCanvas().setDisplayList(null);
			win2.getCanvas().removeKeyListener(this);
			yzImage.changes = false;
			if (yzImage.isVisible())
				yzImage.close();
		}
		Executer.removeCommandListener(this);
		// win.removeWindowListener(this);
		win.removeFocusListener(this);
		win.setResizable(true);

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (ignoreClicks)
			return;
		crossLoc = canvas.getCursorLoc();
		update();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * Refresh the output windows. This is done by sending a signal
	 * to the Updater() thread.
	 */
	public void update() {
		if (updater != null)
			updater.doUpdate();
	}

	@SuppressWarnings("deprecation")
	private void exec() {
		if (canvas == null)
			return;

		try {
			SwingUtilities.invokeAndWait(() -> {
				int width = imp.getWidth();
				int height = imp.getHeight();
				double arat = az / ax;
				double brat = az / ay;
				Point p = crossLoc;
				if (p.y >= height)
					p.y = height - 1;
				if (p.x >= width)
					p.x = width - 1;
				if (p.x < 0)
					p.x = 0;
				if (p.y < 0)
					p.y = 0;
				updateViews(p, imp);
				GeneralPath path = new GeneralPath();
				drawCross(imp, p, path);
				canvas.setDisplayList(path, color, new BasicStroke(1));
				canvas.setCustomRoi(true);
				updateCrosses(p.x, p.y, arat, brat);
				if (syncZoom)
					updateMagnification(p.x, p.y);
				arrangeWindows(sticky);
			});
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			Utils.printStack(e);
		} catch (InvocationTargetException e) {
			Utils.printStack(e);
		}
	}

	@SuppressWarnings("deprecation")
	private void updateCrosses(int x, int y, double arat, double brat) {
		// Flipping and rotation support temporarily removed

		Point p;
		// int depth=imp.getNSlices();

		int zSlice;
		zSlice = imp.getCurrentSlice() / imp.getNChannels() - imp.getChannel() + 1;
		// int zcoord=(int)Math.round(arat*zlice);
		int zcoord = (int) ((((float) xzImage.getHeight())) * ((float) zSlice) / (imp.getNSlices()));

		// if (flipXZ) zcoord=(int)Math.round(arat*(depth-zSlice));
		p = new Point(x, zcoord);
		ImageCanvas xzCanvas = xzImage.getCanvas();
		if (xzCanvas != null) {
			GeneralPath path = new GeneralPath();
			drawCross(xzImage, p, path);
			xzCanvas.setDisplayList(path, color, new BasicStroke(1));
		}
		// zcoord=(int)Math.round(brat*(z-zSlice));

		// if (rotate)
		// p=new SkeletonPoint (y, zcoord);
		// else {
		// zcoord=(int)Math.round(arat*zSlice);
		// p=new SkeletonPoint (zcoord, y);
		// }
		zcoord = (int) ((((float) yzImage.getWidth())) * ((float) zSlice) / (imp.getNSlices()));
		p = new Point(zcoord, y);
		ImageCanvas yzCanvas = yzImage.getCanvas();
		if (yzCanvas != null) {
			GeneralPath path = new GeneralPath();
			drawCross(yzImage, p, path);
			yzCanvas.setDisplayList(path, color, new BasicStroke(1));
		}
		IJ.showStatus(imp.getLocationAsString(crossLoc.x, crossLoc.y));
	}

	// @Override
	@Override
	public void mouseDragged(MouseEvent e) {
		if (ignoreClicks)
			return;
		crossLoc = canvas.getCursorLoc();
		update();
	}

	// @Override
	@Override
	public void mouseMoved(MouseEvent e) {
	}

	// @Override
	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_ESCAPE) {
			IJ.beep();
			dispose();
		} else if (IJ.shiftKeyDown()) {
			int width = imp.getWidth(), height = imp.getHeight();
			switch (key) {
				case KeyEvent.VK_LEFT:
					crossLoc.x--;
					if (crossLoc.x < 0)
						crossLoc.x = 0;
					break;
				case KeyEvent.VK_RIGHT:
					crossLoc.x++;
					if (crossLoc.x >= width)
						crossLoc.x = width - 1;
					break;
				case KeyEvent.VK_UP:
					crossLoc.y--;
					if (crossLoc.y < 0)
						crossLoc.y = 0;
					break;
				case KeyEvent.VK_DOWN:
					crossLoc.y++;
					if (crossLoc.y >= height)
						crossLoc.y = height - 1;
					break;
				default:
					return;
			}
			update();
		}
	}

	// @Override
	@Override
	public void keyReleased(KeyEvent e) {
	}

	// @Override
	@Override
	public void keyTyped(KeyEvent e) {
	}

	// @Override
	@Override
	public void actionPerformed(ActionEvent ev) {
	}

	/*
	 * public void imageClosed(ImagePlus imp) {
	 * dispose();
	 * }
	 * 
	 * public void imageOpened(ImagePlus imp) {
	 * }
	 */

	@Override
	public String commandExecuting(String command) {
		if (command.equals("In") || command.equals("Out")) {
			ImagePlus cimp = WindowManager.getCurrentImage();
			if (cimp == null)
				return command;
			if (cimp == imp) {
				/*
				 * if (syncZoom) {
				 * ImageWindow xyWin = cimp.getWindow();
				 * if (xyWin==null) return command;
				 * ImageCanvas ic = xyWin.getCanvas();
				 * Dimension screen = IJ.getScreenSize();
				 * int xyWidth = xyWin.getWidth();
				 * ImageWindow yzWin = yzImage.getWindow();
				 * double mag = ic.getHigherZoomLevel(ic.getMagnification());
				 * if (yzWin!=null&&xyX+xyWidth+(int)(yzWin.getWidth()*mag)>screen.width) {
				 * xyX = screen.width-xyWidth-(int)(yzWin.getWidth()*mag);
				 * if (xyX<10) xyX = 10;
				 * xyWin.setLocation(xyX, xyY);
				 * }
				 * }
				 */
				IJ.runPlugIn("ij.plugin.Zoom", command.toLowerCase());
				xyX = 0;
				xyY = 0;
				update();
				return null;
			} else if (cimp == xzImage || cimp == yzImage) {
				syncZoom = false;
				return command;
			} else
				return command;
		} else if (command.equals("Flip Vertically") && xzImage != null) {
			if (xzImage == WindowManager.getCurrentImage()) {
				flipXZ = !flipXZ;
				update();
				return null;
			} else
				return command;
		} else
			return command;
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		update();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		update();
	}

	@Override
	public void focusGained(FocusEvent e) {
		ImageCanvas ic = imp.getCanvas();
		if (ic != null)
			canvas.requestFocus();
		arrangeWindows(sticky);
	}

	@Override
	public void focusLost(FocusEvent e) {
		arrangeWindows(sticky);
	}

	/**
	 * This is a helper class for Othogonal_Views that delegates the
	 * repainting of the destination windows to another thread.
	 * 
	 * @author Albert Cardona
	 */
	private class Updater extends Thread {
		long request = 0;

		// Constructor autostarts thread
		Updater() {
			super("Othogonal Views Updater");
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void doUpdate() {
			if (isInterrupted())
				return;
			synchronized (this) {
				request++;
				notify();
			}
		}

		void quit() {
			interrupt();
			synchronized (this) {
				notify();
			}
		}

		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					final long r;
					synchronized (this) {
						r = request;
					}
					// Call update from this thread
					if (r > 0)
						exec();
					synchronized (this) {
						if (r == request) {
							request = 0; // reset
							wait();
						}
						// else loop through to update again
					}
				} catch (Exception e) {
					Utils.printStack(e);
				}
			}
		}

	} // Updater class

	public void incrementY(int i) {
		int newY = crossLoc.y + i;
		if (newY < 0)
			newY = 0;
		if (newY >= imp.getHeight())
			newY = imp.getHeight() - 1;
		crossLoc.y = newY;
		update();
	}

	public void incrementX(int i) {
		int newX = crossLoc.x + i;
		if (newX < 0)
			newX = 0;
		if (newX >= imp.getWidth())
			newX = imp.getWidth() - 1;
		crossLoc.x = newX;
		update();
	}

	@Override
	public void pluginIOValueChanged(boolean stillChanging, IPluginIO pluginIOWhoseValueChanged) {
		ImageProcessor ip = imp.getProcessor();
		min = ip.getMin();
		max = ip.getMax();
		update();
	}

	@Override
	public void pluginIOViewEvent(PluginIOView trigger, boolean stillChanging, AWTEvent event) {

	}

	private static void setFixedDim3(ImagePlus image, float value) {
		if (image == null)
			return;
		Object canvas = image.getCanvas();
		if (canvas instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) canvas).setFixedDim3(value);
		}
	}

	@SuppressWarnings("unused")
	private static void setFixedDim4(ImagePlus image, float value) {
		if (image == null)
			return;
		Object canvas = image.getCanvas();
		if (canvas instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) canvas).setFixedDim4(value);
		}
	}

	private static void setLabelDepth(ImagePlus image, int newDepth, int unscaled) {
		if (image == null)
			return;
		Object canvas = image.getCanvas();
		if (canvas instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) canvas).setLabelDepth(newDepth);
			((ImageCanvasWithAnnotations) canvas).setUnscaledLabelDepth(unscaled);
		}
	}

	private static void setTransparency(ImagePlus image, float transparency) {
		if (image == null)
			return;
		Object canvas = image.getCanvas();
		if (canvas instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) canvas).setTransparency(transparency);
		}
	}

	public void setLabelDepth(int newDepth) {
		setLabelDepth(xzImage, (int) (newDepth * aratx), newDepth);
		setLabelDepth(yzImage, (int) (newDepth * araty), newDepth);
		update();
	}

	public void setTransparency(float newTransparencyValue) {
		setTransparency(xzImage, newTransparencyValue);
		setTransparency(yzImage, newTransparencyValue);
		update();
	}

	private static void setHoverDelay(ImagePlus image, float hoverDelay) {
		if (image == null)
			return;
		Object canvas = image.getCanvas();
		if (canvas instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) canvas).setHoverDelay(hoverDelay);
		}
	}

	public void setHoverDelay(float newHoverDelay) {
		setHoverDelay(xzImage, newHoverDelay);
		setHoverDelay(yzImage, newHoverDelay);
	}

	private static void setLineThickness(ImagePlus image, float newThickness) {
		if (image == null)
			return;
		Object canvas = image.getCanvas();
		if (canvas instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) canvas).setLineThickness(newThickness);
		}
	}

	public void setLineThickness(float newThickness) {
		setLineThickness(xzImage, newThickness);
		setLineThickness(yzImage, newThickness);
	}

	private static void setCellsToOverlay(ImagePlus image, PluginIOCells cells) {
		if (image == null)
			return;
		Object canvas = image.getCanvas();
		if (canvas instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) canvas).setCellsToOverlay(cells);
		}
	}

	private static void setDisplayColor(ImagePlus image, Color newColor) {
		if (image == null)
			return;
		Object canvas = image.getCanvas();
		if (canvas instanceof ImageCanvasWithAnnotations) {
			((ImageCanvasWithAnnotations) canvas).setDisplayColor(newColor);
		}
	}

	public void setCellsToOverlay(PluginIOCells cellsToOverlay) {
		setCellsToOverlay(xzImage, cellsToOverlay);
		setCellsToOverlay(yzImage, cellsToOverlay);
	}

	public void setDisplayColor(Color newColor) {
		setDisplayColor(xzImage, newColor);
		setDisplayColor(yzImage, newColor);
	}

}
