/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils.image_with_toolbar;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.MouseListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

/**
 * A class to display 4D stacks along with an arbitrary toolbar at the bottom that can be used to
 * switch orthogonal views on and off, and/or to control plugins that edit the image in response
 * to user clicks.
 *
 */
public class PluginIOHyperstackWithToolbar extends PluginIOHyperstackViewWithImagePlus {

	public PluginIOHyperstackWithToolbar(String s) {
		super(s);
	}

	private transient Toolbar toolbar;

	/**
	 * Returns the toolbar currently displayed below the image.
	 * 
	 * @return may be null
	 */
	public Toolbar getToolbar() {
		return toolbar;
	}

	/**
	 * Set the toolbar to be displayed below the image, within the same window.
	 * 
	 * @param toolbar
	 *            may be null
	 */
	public void setToolbar(Toolbar toolbar) {
		this.toolbar = toolbar;
		if ((imp != null) && (imp.getWindow() != null) && (imp.getWindow() instanceof StackWindowWithToolbar)) {
			StackWindowWithToolbar stackWindow = (StackWindowWithToolbar) imp.getWindow();
			stackWindow.setToolbar(toolbar);
		} else if (imp != null)
			imp.setWindow(new StackWindowWithToolbar(imp, toolbar, createCanvas(null)));
	}

	/**
	 * 
	 * @param impOwner
	 *            If null, current imp is used
	 * @return
	 */
	private ImageCanvasWithAnnotations createCanvas(ImagePlus impOwner) {
		ImageCanvasWithAnnotations canvas =
				new ImageCanvasWithAnnotations(impOwner == null ? imp : impOwner, cellsToOverlay, getZRange(), "x",
						"y", "z", "t", "xyCalibration", "xyCalibration", "zCalibration");
		if (toolbar != null) {
			if (toolbar instanceof ActiveContourToolbar) {
				canvas.setLabelDepth(((ActiveContourToolbar) toolbar).getLabelDepth());
				canvas.setUnscaledLabelDepth(((ActiveContourToolbar) toolbar).getLabelDepth());
				canvas.setTransparency(((ActiveContourToolbar) toolbar).getTransparency());
				canvas.setDisplayColor(((ActiveContourToolbar) toolbar).getDisplayColor());
				canvas.setLineThickness(((ActiveContourToolbar) toolbar).getLineThickness());
				canvas.setHoverDelay(((ActiveContourToolbar) toolbar).getHoverDelay());
				setDepthOfField(((ActiveContourToolbar) toolbar).getDepthOfField());
			}
		}
		return canvas;
	}

	@SuppressWarnings("unused")
	@Override
	protected void createImagePlus(String name, ImageStack imageStack, Object metadata) {
		if (imp == null) {
			imp = new ImagePlus(name, imageStack);
			imp.setOpenAsHyperStack(true);
			imp.setDimensions(nChannels, depth, nTimePoints);
			toolbar = new ActiveContourToolbar(this);
			orthogonalViewsIgnoreClicks = !toolbar.browse;
			toolbar.addListener(this);
			new StackWindowWithToolbar(imp, toolbar, createCanvas(null));
			updateRefsToImp();
			if (imp.isVisible()) {// temporary workaround for problem with image showing even when it should not
				imp.getCanvas().addMouseListener(new MouseListenerWeakRef(this));
				setShowOrthogonalViews(showOrthogonalViews, false);
			}
		} else {
			imp.setStack(imageStack);
		}
		imp.setProperty("Info", metadata);
	}

	/**
	 * {@inheritDoc} Overridden to make the composite image use a {@link StackWindowWithToolbar} instead of a regular
	 * StackWindow.
	 */
	@SuppressWarnings("unused")
	@Override
	public void toComposite() {
		// *** CHANGES MADE IN THIS METHOD SHOULD ALSO BE MADE IN PluginIOHyperstackViewWithImagePlus
		if (imp == null || nChannels < 2)
			return;
		try {
			Runnable r = () -> {
				if (imp instanceof CompositeImage) {
					((CompositeImage) imp).setMode(CompositeImage.COMPOSITE);
					imp.updateAndRepaintWindow();
					Utils.log(name + " already a composite", LogLevel.INFO);
					return;
				}
				CompositeImage newComposite = null;
				newComposite = new CompositeImage(imp);

				toolbar = new ActiveContourToolbar(PluginIOHyperstackWithToolbar.this);
				orthogonalViewsIgnoreClicks = !toolbar.browse;
				toolbar.addListener(PluginIOHyperstackWithToolbar.this);
				new StackWindowWithToolbar(newComposite, toolbar, createCanvas(newComposite));
				orthogonalViewsIgnoreClicks = !toolbar.browse;

				newComposite.setMode(CompositeImage.COMPOSITE);
				newComposite.show();

				imp.setIgnoreFlush(true);
				closeImpDontNotifyListeners(imp);

				imp = newComposite;
				updateRefsToImp();
				imp.getCanvas().addMouseListener(new MouseListenerWeakRef(PluginIOHyperstackWithToolbar.this));
				if (imp.getWindow().getCanvas() != null) {
					ImageCanvasWithAnnotations c = (ImageCanvasWithAnnotations) imp.getWindow().getCanvas();
					c.removePrivateKeyListener(keyListenerWeakRef);
					c.addPrivateKeyListener(keyListenerWeakRef);
				}
				((ImageCanvasWithAnnotations) imp.getCanvas()).setCellsToOverlay(getCellsToOverlay());
				refreshOrthogonalViews();
				isComposite = true;
			};
			if (SwingUtilities.isEventDispatchThread())
				r.run();
			else
				SwingUtilities.invokeAndWait(r);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void pluginIOViewEvent(PluginIOView trigger, boolean stillChanging, AWTEvent event) {
		// This could be an event from the toolbar that we need to respond to
		orthogonalViewsIgnoreClicks = !toolbar.browse;
		if (orthogonalViews != null) {
			orthogonalViews.ignoreClicks = orthogonalViewsIgnoreClicks;
		}
		super.pluginIOViewEvent(trigger, stillChanging, event);
	}

	private void createCells() {
		PluginIOCells cells = new PluginIOCells();
		IPluginIOHyperstack hs = displayedImages.getFirst();
		cells.setCalibration(hs.getCalibration());
		cells.setWidth(hs.getWidth());
		cells.setHeight(hs.getHeight());
		cells.setDepth(hs.getDepth());
		setCellsToOverlay(cells);
	}

	private void handlePointLabeling(int x, int y, double z, float zThickness, int t, int boxWidth, int boxHeight,
			int modifier) {

		if (mousePluginListeners.size() == 0) {
			// handle labeling ourselves if no plugin is listening
			if (modifier == PluginIOCells.DELETE_LABELS_MODIFIER) {
				if (cellsToOverlay == null) {
					Utils.log("Cannot clear labels because there are no cells", LogLevel.WARNING);
					return;
				}
				cellsToOverlay.clearLabelsOfPointClosestTo(x, y, z - 1, t);
				updateImpAndDrawLater();
				return;
			} else if (modifier == PluginIOCells.LABEL_MODIFIER) {
				if (cellsToOverlay == null) {
					createCells();
				}
				ActiveContourToolbar activeContourToolbar = (ActiveContourToolbar) toolbar;
				String[] labels = activeContourToolbar.getLabels();
				if (activeContourToolbar.labelExistingCell()) {
					if (boxWidth > 0)
						cellsToOverlay.annotatePointsInBox(x, y, boxWidth, boxHeight, z - 1, t, labels);
					else
						cellsToOverlay.annotatePointClosestTo(x, y, z - 1, t, labels);
				} else {
					ClickedPoint p = cellsToOverlay.createAnnotatedPoint(x, y, z - 1, t, labels);
					if (zThickness > 0)
						p.setQuantifiedProperty("zThickness", zThickness);
				}
				updateImpAndDrawLater();
				return;
			} else if (modifier == PluginIOCells.DELETE_MODIFIER) {
				if (cellsToOverlay == null) {
					Utils.log("Cannot delete cell because there are no cells", LogLevel.WARNING);
					return;
				}
				if ((boxWidth == 0) && (boxHeight == 0)) {
					cellsToOverlay.deletePointClosestTo(x, y, z - 1, t);
				} else {
					int labelDepth = ((ActiveContourToolbar) toolbar).getLabelDepth();
					if ((labelDepth & 1) == 1)
						labelDepth--;
					cellsToOverlay.deletePointsInBox(x, y, z - 1 - labelDepth / 2, t, boxWidth, boxHeight, labelDepth);
				}
				updateImpAndDrawLater();
				return;
			} else if (modifier == PluginIOCells.ADD_MODIFIER) {
				if (cellsToOverlay == null) {
					createCells();
				}
				ClickedPoint newPoint = cellsToOverlay.createAnnotatedPoint(x, y, z - 1, t, new String[] {});
				if (zThickness > 0) {
					cellsToOverlay.addQuantifiedPropertyName("zThickness");
					newPoint.setQuantifiedProperty("zThickness", zThickness);
				}
				updateImpAndDrawLater();
				return;
			}
			Utils.log("No one listening to clicks", LogLevel.WARNING);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if ((mousePressedPoint == null) || e.isConsumed())
			return;
		// Translate click coordinates
		// Utils.log("Mouse released in hyperstack view with toolbar at time "+System.currentTimeMillis(),LogLevel.VERBOSE_DEBUG);

		ImageCanvas canvas = imp.getWindow().getCanvas();
		int x = canvas.offScreenX(e.getX());
		int y = canvas.offScreenY(e.getY());

		int boxWidth = 0;
		int boxHeight = 0;

		if (mousePressedPoint != null) {
			boxWidth = Math.abs(mousePressedPoint.x - x);
			boxHeight = Math.abs(mousePressedPoint.y - y);

			x = Math.min(mousePressedPoint.x, x);
			y = Math.min(mousePressedPoint.y, y);
		}
		mousePressedPoint = null;

		if ((toolbar.getCurrentModifier() < 0)
				&& ((e.getModifiers() & ActionEvent.SHIFT_MASK) != ActionEvent.SHIFT_MASK)
				&& ((e.getModifiers() & ActionEvent.ALT_MASK) != ActionEvent.ALT_MASK)) {
			Utils.log("Ignoring click because negative current modifier", LogLevel.VERBOSE_DEBUG);
			return;// If modifier is negative we are meant to ignore the clicks
		}

		long z;
		int t;
		/*
		 * if (!imp.isHyperStack()){
		 * z=imp.getCurrentSlice();
		 * t=0;
		 * } else {
		 * int currentSlice=imp.getCurrentSlice();
		 * z=(currentSlice-1)/(imp.getNChannels()*imp.getNFrames())+1;
		 * t=(imp.getCurrentSlice()-1)/(imp.getNChannels()*imp.getNSlices());
		 * }
		 */

		long zThickness = 0;
		if (z0 == -1)
			z = imp.getSlice();
		else {
			z = z0;
			if (z1 > -1) {
				zThickness = z1 - z0;
			}
		}
		t = imp.getFrame() - 1;

		ClickedPoint p = new ClickedPoint(x, y, z, t, toolbar.getCurrentModifier(), toolbar.getClickGroup(e));

		if ((e.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) {
			p.modifiers = PluginIOCells.DELETE_MODIFIER;
		} else if ((e.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK) {
			p.modifiers = PluginIOCells.ADD_MODIFIER;
		}

		if ((boxWidth > 0) || (boxHeight > 0)) {
			p.listNamesOfQuantifiedProperties = new ArrayList<>(Arrays.asList(new String[] { "width", "height" }));
			p.quantifiedProperties =
					new ArrayList<>(Arrays.asList(new Float[] { (float) boxWidth, (float) boxHeight }));
		}
		if (zThickness != 0) {
			p.setQuantifiedProperty("zThickness", zThickness);
		}

		// TODO Find a more elegant structure; maybe link to a labeling plugin

		if (mousePluginListeners.size() == 0) {
			handlePointLabeling(x, y, z, zThickness, t, boxWidth, boxHeight, p.modifiers);
			return;// redundant
		}
		// TODO the following needs to be cleaned up so there's no active contour specific code here
		if (toolbar instanceof ActiveContourToolbar) {
			ActiveContourToolbar activeContourToolbar = (ActiveContourToolbar) toolbar;
			if (activeContourToolbar.getOverrideRuntime()) {
				p.contourRuntime = activeContourToolbar.getActiveContourRuntime();
			}
			p.userCells.clear();// so that the formulas are read when translating to protobuf; if not
			// empty spreadsheetcells will be used
			p.userCellFormulas = new ArrayList<>(Arrays.asList(activeContourToolbar.getLabels()));
		}
		List<ClickedPoint> clickList = new ArrayList<>();
		clickList.add(p);
		PluginIOCells click = new PluginIOCells(clickList);
		notifyMousePluginListeners(click, false);
		if (mousePluginListeners.size() == 0) {
			Utils.log("No one listening to clicks", LogLevel.WARNING);
		}
	}

	@Override
	public int mouseClicked(PluginIO clickedPoints, boolean inputHasChanged, MouseEvent generatingEvent) {
		super.mouseClicked(clickedPoints, inputHasChanged, generatingEvent);
		// We might get mouse click events from orthogonal views; we just need to pass them on
		// after filling in the modifiers and click groups
		int modifier = toolbar.getCurrentModifier();
		if ((generatingEvent.getModifiers() & ActionEvent.SHIFT_MASK) == ActionEvent.SHIFT_MASK) {
			modifier = PluginIOCells.DELETE_MODIFIER;
		} else if ((generatingEvent.getModifiers() & ActionEvent.ALT_MASK) == ActionEvent.ALT_MASK) {
			modifier = PluginIOCells.ADD_MODIFIER;
		}

		if (modifier < 0)
			return 0;

		for (ClickedPoint p : ((PluginIOCells) clickedPoints).getPoints()) {
			p.clickGroup = toolbar.getClickGroup(generatingEvent);
			p.modifiers = modifier;
			// TODO the following needs to be cleaned up so there's no active contour specific code here
			if (toolbar instanceof ActiveContourToolbar) {
				ActiveContourToolbar activeContourToolbar = (ActiveContourToolbar) toolbar;
				if (activeContourToolbar.getOverrideRuntime()) {
					p.contourRuntime = activeContourToolbar.getActiveContourRuntime();
				}
			}
		}
		notifyMousePluginListeners(clickedPoints, inputHasChanged);
		if (mousePluginListeners.size() == 0) {
			for (ClickedPoint p : ((PluginIOCells) clickedPoints).getPoints()) {
				// FIXME This does not test properly for set width and height
				int width =
						p.getQuantifiedPropertyNames().contains("width") ? (int) p.getQuantifiedProperty("width") : 0;
				int height =
						p.getQuantifiedPropertyNames().contains("height") ? (int) p.getQuantifiedProperty("height") : 0;

				handlePointLabeling((int) p.x, (int) p.y, (int) p.z, 0 /* FIXME thickness not handled */, (int) p.t,
						width, height, p.modifiers);
			}
		}
		return 0;
	}

}
