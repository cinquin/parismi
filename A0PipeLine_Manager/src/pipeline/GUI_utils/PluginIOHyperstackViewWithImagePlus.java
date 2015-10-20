/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.awt.AWTEvent;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.ColorModel;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.NonNull;

import ij.CompositeImage;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import pipeline.GUI_utils.image_with_toolbar.ImageCanvasWithAnnotations;
import pipeline.GUI_utils.image_with_toolbar.StackWindowWithToolbar;
import pipeline.data.ChannelInfo;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIO;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOListener;
import pipeline.data.PluginIOStack;
import pipeline.misc_util.ImageListenerWeakRef;
import pipeline.misc_util.KeyListenerWeakRef;
import pipeline.misc_util.MouseListenerWeakRef;
import pipeline.misc_util.Pair;
import pipeline.misc_util.PluginIOListenerWeakRef;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ILoopWorker;
import pipeline.misc_util.parfor.ParFor;
import pipeline.plugins.MouseEventPlugin;
import pipeline.plugins.image_processing.ZProjector;
import processing_utilities.projection.RayFunction;

/**
 * Provides a view, or a set of views (e.g. orthogonal views), of a 4D image using ImageJ's ImagePlus type.
 *
 */
public class PluginIOHyperstackViewWithImagePlus extends PluginIOView implements PluginIOListener, ImageListener,
		MouseListener, MouseEventPlugin, KeyListener {

	/**
	 * For images that were computed by projection, lower bound of the range in the original image the
	 * projection was performed on. -1 if not a projection or range has not been set.
	 */
	protected long z0 = -1;

	/**
	 * For images that were computed by projection, upper bound of the range in the original image the
	 * projection was performed on. -1 if not a projection or range has not been set.
	 */
	protected long z1 = -1;

	/**
	 * @return For images that were computed by projection, range in the original image the projection was performed on.
	 * 
	 */
	public Pair<Long, Long> getZRange() {
		if ((z0 == -1) || (z1 == -1))
			return null;
		else
			return new Pair<>(z0, z1);
	}

	/**
	 * 
	 * @param range
	 *            For images that were computed by projection, range in the original image the projection was performed
	 *            on.
	 */
	public void setZRange(Pair<Long, Long> range) {
		if (range == null) {
			z0 = -1;
			z1 = -1;
		} else {
			z0 = range.fst;
			z1 = range.snd;
		}
		if (imp != null) {
			((ImageCanvasWithAnnotations) imp.getCanvas()).setZRange(range);
			updateImpAndDrawLater();
		}
	}

	public PluginIOCells cellsToOverlay;

	public PluginIOCells getCellsToOverlay() {
		return cellsToOverlay;
	}

	public void setCellsToOverlay(PluginIOCells cellsToOverlay) {
		this.cellsToOverlay = cellsToOverlay;
		if (imp != null) {
			((ImageCanvasWithAnnotations) imp.getCanvas()).setCellsToOverlay(cellsToOverlay);
			updateImpAndDrawLater();
		}
		if (orthogonalViews != null) {
			orthogonalViews.setCellsToOverlay(cellsToOverlay);
		}
	}

	/**
	 * List of images (stored with pipeline structures) that are displayed in the current view, within the same window.
	 * There can
	 * be as many as desired (within ImageJ CompositeImage limits).
	 */
	public transient LinkedList<IPluginIOHyperstack> displayedImages;

	/**
	 * Main ImagePlus imp for displaying the image.
	 */
	public transient ImagePlus imp;

	protected boolean showOrthogonalViews;

	/**
	 * This is modified by subclasses dealing with toolbars. Clicks should be ignored by orthogonal views when toolbar
	 * says
	 * they should be directed somewhere else.
	 */
	protected boolean orthogonalViewsIgnoreClicks = false;

	public boolean isShowOrthogonalViews() {
		return showOrthogonalViews;
	}

	protected transient OrthogonalViewsWithComposites orthogonalViews;

	public OrthogonalViewsWithComposites getOrthogonalViews() {
		return orthogonalViews;
	}

	public void setShowOrthogonalViews(boolean showOrthogonalViews, boolean forceUpdate) {
		if ((this.showOrthogonalViews != showOrthogonalViews) && (imp != null)) {
			if (showOrthogonalViews) {
				orthogonalViews = new OrthogonalViewsWithComposites(imp, this, this, cellsToOverlay);
				orthogonalViews.ignoreClicks = orthogonalViewsIgnoreClicks;

				Object canvas = imp.getCanvas();
				if (canvas instanceof ImageCanvasWithAnnotations) {
					ImageCanvasWithAnnotations castCanvas = (ImageCanvasWithAnnotations) imp.getCanvas();
					int labelDepth = castCanvas.getLabelDepth();
					orthogonalViews.setLabelDepth(labelDepth);

					orthogonalViews.setDisplayColor(castCanvas.getDisplayColor());
					orthogonalViews.setHoverDelay(castCanvas.getHoverDelay());
					orthogonalViews.setLineThickness(castCanvas.getLineThickness());
					orthogonalViews.setTransparency(castCanvas.getTransparency());
				}

				orthogonalViews.show();
			} else if (orthogonalViews != null) { // get rid of existing views
				listeners.remove(orthogonalViews);
				try {
					orthogonalViews.dispose();
				} catch (Exception e) {
					Utils.printStack(e);
				}
				orthogonalViews = null;
			}
		}
		this.showOrthogonalViews = showOrthogonalViews;
	}

	protected transient @NonNull List<MouseEventPlugin> mousePluginListeners = new LinkedList<>();

	public void addListener(MouseEventPlugin listener) {
		mousePluginListeners.add(listener);
	}

	public void removeListener(MouseEventPlugin listener) {
		mousePluginListeners.remove(listener);
	}

	protected transient List<PluginIOListener> listeners = new ArrayList<>();

	public void addListener(PluginIOListener listener) {
		listeners.add(listener);
	}

	public void removeListener(PluginIOListener listener) {
		listeners.remove(listener);
	}

	public int width, height, depth, nChannels, nTimePoints;
	protected String name;

	/**
	 * True if display range should be updated when pixel values change.
	 */
	public boolean shouldUpdateRange;

	protected int lastNChannels = -1;

	/**
	 * Bypasses ImageJ listener system. Used when a window closed but immediately reopened under a
	 * different format (e.g. when switching to a composite image).
	 * 
	 * @param imp
	 */
	public static void closeImpDontNotifyListeners(ImagePlus imp) {
		if (imp == null)
			return;
		if (imp.getWindow() != null)
			imp.getWindow().closeDontNotifyListeners();
		else
			imp.close();
	}

	private int displayZThickening = 0;

	private ConcurrentLinkedQueue<ZProjector> projectorQueue = new ConcurrentLinkedQueue<>();

	public void setDepthOfField(int thickening) {
		displayZThickening = thickening;
		computeStack();
	}

	protected Object getPixelsToDisplay(IPluginIOHyperstack element, int z, int c) throws InterruptedException {
		if (displayZThickening == 0)
			return element.getPixels(z, c, 0);

		ZProjector projector;
		projector = projectorQueue.poll();
		if (projector == null) {
			projector = new ZProjector();
			projector.numberOfThreadsToUse = 1;
			projector.method = RayFunction.MAX_METHOD;
		}

		IPluginIOStack output =
				new PluginIOStack("temp", element.getWidth(), element.getHeight(), 1, 1, PixelType.FLOAT_TYPE);
		projector.startSlice = Math.max(1, z + 1 - displayZThickening);
		projector.stopSlice = Math.min(element.getDepth(), z + 1 + displayZThickening);
		projector.doProjection(element.getChannels().get("Ch" + c), output, null, null, true);
		projectorQueue.add(projector);

		output.computePixelArray();
		output.convertTo(element.getPixelType());
		return output.getPixels(0);
	}

	/**
	 * Create an ImageJ ImageStack to display the contents, and call {@link createImagePlus} to display the
	 * [hyper]stack.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void computeStack() {
		// FIXME THIS IS OVERRIDEN BY THE BUFFERED VIEWS; CHANGES MADE HERE NEED TO BE MADE THERE AS WELL
		if (displayedImages == null)
			return;
		final IPluginIOHyperstack[] hyperstackArray = displayedImages.toArray(new IPluginIOHyperstack[] {});
		if (hyperstackArray.length == 0)
			return;
		width = hyperstackArray[0].getWidth();
		height = hyperstackArray[0].getHeight();
		depth = hyperstackArray[0].getDepth();
		nTimePoints = hyperstackArray[0].getnTimePoints();
		ImageStack imageStack = new ImageStack(width, height);
		ColorModel colorModel = null;
		double displayMin = 0;
		double displayMax = 0;
		if ((imp != null) && (imp.getStack() != null) && (imp.getProcessor() != null)) {
			colorModel = imp.getProcessor().getCurrentColorModel();// imageStack.setColorModel(imp.getProcessor().getCurrentColorModel());
			imageStack.setColorModel(colorModel);
			displayMin = imp.getDisplayRangeMin();
			displayMax = imp.getDisplayRangeMax();
		}
		final ColorModel finalColorModel = colorModel;

		nChannels = 0;
		final StringBuilder metadata = new StringBuilder();
		final List[] slicesToAdd = new List[depth * nTimePoints];

		ParFor parFor = new ParFor("Compute stack", 0, depth * nTimePoints - 1, null, true);
		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker(new ILoopWorker() {

				@Override
				public Object run(int z, int threadIndex) throws InterruptedException {
					slicesToAdd[z] = new ArrayList();
					for (IPluginIOHyperstack element : hyperstackArray) {
						if (z == 0) {
							nChannels += element.getnChannels();
							Object hyperstackMetadata = element.getImageAcquisitionMetadata();
							Object asString = null;
							if (hyperstackMetadata instanceof ChannelInfo[]) {
								StringBuilder allChannels = new StringBuilder();
								for (ChannelInfo i : (ChannelInfo[]) hyperstackMetadata) {
									allChannels.append(i.toString());
								}
								asString = allChannels.toString();
							} else
								asString = hyperstackMetadata;
							if (asString instanceof String) {
								String s = (String) asString;
								if (!"".equals(s)) {
									metadata.append(s);
								}
							}
						}

						for (int c = 0; c < element.getnChannels(); c++) {
							Object pixels = getPixelsToDisplay(element, z, c);
							ImageProcessor sliceToAdd;
							if (pixels instanceof float[])
								sliceToAdd = new FloatProcessor(width, height, (float[]) pixels, null);
							else if (pixels instanceof byte[])
								sliceToAdd = new ByteProcessor(width, height, (byte[]) pixels, null);
							else if (pixels instanceof short[])
								sliceToAdd = new ShortProcessor(width, height, (short[]) pixels, null);
							else
								throw new RuntimeException("Unknow pixel type " + pixels);
							if (finalColorModel != null)
								sliceToAdd.setColorModel(finalColorModel);
							slicesToAdd[z].add(pixels);
						}
					}

					return null;
				}
			});

		try {
			parFor.run(true);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		for (List aSlicesToAdd : slicesToAdd) {
			for (Object o : aSlicesToAdd) {
				imageStack.addSlice("", o);
			}
		}

		if ((lastNChannels != nChannels) && (imp != null)) {
			closeImpDontNotifyListeners(imp);
			setShowOrthogonalViews(false, true);
			imp = null;
		}
		lastNChannels = nChannels;

		if (imageStack.getSize() > 0) {
			createImagePlus(name, imageStack, metadata.toString());
			// keep orthogonal view creation for later because orthogonal view might try to access data too early
		}
		if (imp != null) {
			imp.setDimensions(nChannels, depth, nTimePoints);
			imp.setCalibration(hyperstackArray[0].getCalibration());
			imp.setOpenAsHyperStack(true);
			imp.setDisplayRange(displayMin, displayMax);
		} else
			Utils.log("Empty stack for " + name, LogLevel.ERROR);

	}

	protected void updateRefsToImp() {
		for (IPluginIOHyperstack hs : displayedImages) {
			hs.setImagePlusDisplay(imp);
		}
	}

	/**
	 * Create the ImageJ ImagePlus to display.
	 * 
	 * @param name
	 *            Name of window
	 * @param imageStack
	 *            Stack to be displayed by the ImagePlus
	 * @param metadata
	 *            Stored in "Info" field of ImagePlus
	 */
	protected void createImagePlus(String name, ImageStack imageStack, Object metadata) {
		Utils.log("Setting imp stack", LogLevel.DEBUG);
		if (imp == null) {
			imp = new ImagePlus(name, imageStack);
			updateRefsToImp();
		} else {
			imp.setStack(imageStack);
		}
		Utils.log("Set imp stack", LogLevel.DEBUG);
		imp.setProperty("Info", metadata);
	}

	protected KeyListenerWeakRef keyListenerWeakRef = new KeyListenerWeakRef(this);

	@Override
	public void show() {
		try {
			Runnable r = new Runnable() {
				@Override
				public void run() {

					computeStack();
					if ((nChannels > 0) && (imp != null) && (!imp.isVisible())) {
						imp.show();
						if (isComposite && !(imp instanceof CompositeImage))
							toComposite();
						imp.getCanvas().addMouseListener(
								new MouseListenerWeakRef(PluginIOHyperstackViewWithImagePlus.this));
						setShowOrthogonalViews(showOrthogonalViews, true);

					}
					if ((nChannels > 0) && (imp != null)) {
						updateImpAndDrawLater();
					}
					if (orthogonalViews != null)
						orthogonalViews.update();
					if ((imp != null) && (imp.getWindow().getCanvas() instanceof ImageCanvasWithAnnotations)) {
						ImageCanvasWithAnnotations c = (ImageCanvasWithAnnotations) imp.getWindow().getCanvas();
						c.removePrivateKeyListener(keyListenerWeakRef);
						c.addPrivateKeyListener(keyListenerWeakRef);
					}
				}
			};

			if (SwingUtilities.isEventDispatchThread())
				r.run();
			else
				SwingUtilities.invokeAndWait(r);

		} catch (Exception e) {
			Utils.printStack(e);
		}
	}

	public void redrawAndWait() throws InterruptedException {
		if (orthogonalViews != null)
			orthogonalViews.update();
		if (imp == null)
			return;
		if (!SwingUtilities.isEventDispatchThread())
			try {
				SwingUtilities.invokeAndWait(imp::updateAndDraw);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		else
			imp.updateAndDraw();

	}

	protected void updateImpAndDrawLater() {
		if (orthogonalViews != null)
			orthogonalViews.update();
		if (imp == null)
			return;
		if (!SwingUtilities.isEventDispatchThread())
			SwingUtilities.invokeLater(imp::updateAndDraw);
		else
			imp.updateAndDraw();
	}

	/**
	 * Add a channel, or set of channels, to be displayed in the same window as the content that is already present.
	 * 
	 * @param hyperstack
	 */
	public void addImage(IPluginIOHyperstack hyperstack) {
		if (displayedImages == null)
			displayedImages = new LinkedList<>();
		if (!displayedImages.contains(hyperstack)) {

			if ((displayedImages.size() > 0) && (displayedImages.get(0).getPixelType() != hyperstack.getPixelType())) {
				throw new PluginRuntimeException("Cannot add hyperstack " + hyperstack.getName() + " of pixel type "
						+ hyperstack.getPixelType() + " to a display that already has stack "
						+ displayedImages.get(0).getName() + " of different pixel type "
						+ displayedImages.get(0).getPixelType() + ". Convert images to same "
						+ "pixel type, e.g. using LazyCopy plugin with conversion option, and try again. "
						+ "Not displaying hyperstack " + hyperstack.getName(), true);
			}

			displayedImages.add(hyperstack);
			hyperstack.setImagePlusDisplay(imp);
			hyperstack.addListener(new PluginIOListenerWeakRef(this));// should this be a hard reference instead??
		}
	}

	@SuppressWarnings("unused")
	public PluginIOHyperstackViewWithImagePlus(String s) {
		new ImageListenerWeakRef(this, true);
		name = s;
		if (name == null)
			name = "";
	}

	/**
	 * True when this object is just a container for an ImagePlus created by some plugin rather than the pipeline.
	 */
	public boolean preExistingImp;

	@SuppressWarnings("unused")
	public PluginIOHyperstackViewWithImagePlus(String name, ImagePlus counterImg) {
		new ImageListenerWeakRef(this, true);
		this.name = name;
		imp = counterImg;
		preExistingImp = true;
	}

	public String[] getChannelNames() {
		String[] result = new String[displayedImages.size()];
		int i = 0;
		for (IPluginIOHyperstack hs : displayedImages) {
			result[i] = hs.getName();
			i++;
		}
		return result;
	}

	/**
	 * Called when the main ImagePlus is about to be closed (or has just been closed?).
	 */
	private void cleanUp() {
		if (orthogonalViews != null) {
			listeners.remove(orthogonalViews);
			orthogonalViews.dispose();
		}
		if (displayedImages != null)
			for (IPluginIOHyperstack hs : displayedImages) {
				hs.setImagePlusDisplay(null);
			}
		displayedImages = null;
		orthogonalViews = null;
		imp = null;
	}

	@Override
	public void close() {
		Utils.log("Closing imp " + name, LogLevel.DEBUG);
		// showOrthogonalViews=false;
		// closeImpDontNotifyListeners(imp);
		if (imp != null) {
			imp.changes = false;
			imp.close();
		}
		cleanUp();
	}

	/**
	 * Recreate the orthogonal views if they should be shown (for example if the structure of the image, or the main
	 * window displaying it, have changed).
	 */
	protected void refreshOrthogonalViews() {
		setShowOrthogonalViews(false, false);
		setShowOrthogonalViews(showOrthogonalViews, false);
	}

	protected boolean isComposite = false;

	/**
	 * Convert display to composite mode.
	 */
	public void toComposite() {
		// *** Changes made in this method should also be made in PluginIOHyperstackWithToolbar
		if (imp == null || nChannels < 2)
			return;
		try {
			Runnable r = new Runnable() {
				@Override
				public void run() {

					if (imp instanceof CompositeImage) {
						((CompositeImage) imp).setMode(CompositeImage.COMPOSITE);
						imp.updateAndRepaintWindow();
						Utils.log(name + " already a composite", LogLevel.DEBUG);
						return;
					}
					CompositeImage newComposite = null;
					newComposite = new CompositeImage(imp);

					newComposite.setMode(CompositeImage.COMPOSITE);
					newComposite.show();

					imp.setIgnoreFlush(true);
					closeImpDontNotifyListeners(imp);

					imp = newComposite;
					updateRefsToImp();
					imp.getCanvas()
							.addMouseListener(new MouseListenerWeakRef(PluginIOHyperstackViewWithImagePlus.this));

					if (imp.getWindow().getCanvas() instanceof ImageCanvasWithAnnotations) {
						ImageCanvasWithAnnotations c = (ImageCanvasWithAnnotations) imp.getWindow().getCanvas();
						c.removePrivateKeyListener(keyListenerWeakRef);
						c.addPrivateKeyListener(keyListenerWeakRef);
						c.setCellsToOverlay(getCellsToOverlay());
						c.setZRange(getZRange());
					}
					refreshOrthogonalViews();
					isComposite = true;
				}
			};
			if (SwingUtilities.isEventDispatchThread())
				r.run();
			else
				SwingUtilities.invokeAndWait(r);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unused")
	public void toSeparateChannelMode() {
		if (true)
			throw new RuntimeException("Unimplemented");
		isComposite = false;
	}

	@Override
	public void pluginIOValueChanged(boolean stillChanging, IPluginIO pluginIOWhoseValueChanged) {
		if ((imp != null) && (imp.isVisible())) {
			updateImpAndDrawLater();
		} else {
			Utils.log("Not updating imp because it's null or not visible", Utils.LogLevel.DEBUG);
		}
		for (PluginIOListener listener : listeners) {
			try {
				listener.pluginIOValueChanged(stillChanging, pluginIOWhoseValueChanged);
			} catch (Exception e) {
				Utils.printStack(e);
			}
		}
	}

	@Override
	public void pluginIOViewEvent(PluginIOView trigger, boolean stillChanging, AWTEvent event) {
		// This could be an event from the toolbar that we need to respond to
		if (event == null)
			return;
		if ("Orthogonal views".equals(((ActionEvent) event).getActionCommand())) {
			setShowOrthogonalViews(((JToggleButton) event.getSource()).isSelected(), false);
		} else if ("Browse image".equals(((ActionEvent) event).getActionCommand())) {
			Utils.log("browse is " + ((JToggleButton) event.getSource()).isSelected(), LogLevel.DEBUG);
		}
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		if (imp == this.imp) {
			cleanUp();
		}
	}

	@Override
	public void imageOpened(ImagePlus imp) {
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
	}

	protected void notifyMousePluginListeners(PluginIO clickedPoints, boolean inputHasChanged) {
		List<MouseEventPlugin> listenerCopy = new ArrayList<>(mousePluginListeners);
		for (MouseEventPlugin l : listenerCopy) {
			try {
				l.mouseClicked(clickedPoints, inputHasChanged, null);
			} catch (Exception e) {
				Utils.printStack(e);
			}
		}
	}

	protected void notifyMousePluginListenersToProcess() {
		for (MouseEventPlugin l : mousePluginListeners) {
			try {
				l.processClicks();
			} catch (Exception e) {
				Utils.printStack(e);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	protected Point mousePressedPoint;

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.isConsumed()) {
			mousePressedPoint = null;
		}
		ImageCanvas canvas = imp.getWindow().getCanvas();
		int x = canvas.offScreenX(e.getX());
		int y = canvas.offScreenY(e.getY());

		if (mousePressedPoint == null)
			mousePressedPoint = new Point(x, y);
		else {
			mousePressedPoint.x = x;
			mousePressedPoint.y = y;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// do something
	}

	@Override
	public int mouseClicked(PluginIO clickedPoints, boolean inputHasChanged, MouseEvent generatingEvent) {
		// Do not do anything; subclasses with toolbars (which provide information on how to deal with
		// the clicks) will override this method.
		if (imp.getWindow() instanceof StackWindow) {
			// StackWindow sw=(StackWindow) imp.getWindow();
			ClickedPoint p = ((PluginIOCells) clickedPoints).get(0);
			// sw.incrementZ((int) p.z-sw.getZ());
			// imp.setSlice((int) p.z);
			// int currentSlice=imp.getCurrentSlice()/imp.getNChannels();
			// if (imp.getChannel()<imp.getNChannels()) currentSlice++;
			imp.setPosition(imp.getChannel(), (int) p.z, imp.getFrame());

			// ((StackWindow) imp.getWindow()).setPosition(channel, p.z, frame);
		}
		if (orthogonalViews != null)
			orthogonalViews.update();
		return 0;
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// Do not do anything; subclasses with toolbars (which provide information on how to deal with
		// the clicks) will override this method.
		mousePressedPoint = null;
		Utils.log("Ignored click", LogLevel.DEBUG);
	}

	@Override
	public void processClicks() {
		notifyMousePluginListenersToProcess();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		ActionListener action = null;
		char keyTyped = e.getKeyChar();
		if (keyTyped == 'Z') {
			action = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					incrementZ(-1);
				}
			};
		} else if (keyTyped == 'z') {
			action = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					incrementZ(1);
				}
			};
		} else if (keyTyped == 'Y') {
			action = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					incrementY(-1);
				}
			};
		} else if (keyTyped == 'y') {
			action = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					incrementY(1);
				}
			};
		} else if (keyTyped == 'X') {
			action = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					incrementX(-1);
				}
			};
		} else if (keyTyped == 'x') {
			action = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					incrementX(1);
				}
			};
		}

		if (action != null) {
			action.actionPerformed(null);
			e.consume();
		}
	}

	private void incrementZ(int i) {
		if (imp.getWindow() instanceof StackWindow)
			((StackWindow) imp.getWindow()).incrementZ(i);
		if (orthogonalViews != null)
			orthogonalViews.update();
	}

	private void incrementX(int i) {
		if (orthogonalViews != null)
			orthogonalViews.incrementX(i);
	}

	private void incrementY(int i) {
		if (orthogonalViews != null)
			orthogonalViews.incrementY(i);
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (imp.getWindow() instanceof StackWindowWithToolbar)
			((StackWindowWithToolbar) imp.getWindow()).toolbar.publicProcessKeyEvent(e);
	}

	@Override
	public void setData(IPluginIO data) {
		throw new RuntimeException("Unimplemented");
	}

	public boolean isShowingCells() {
		return ((ImageCanvasWithAnnotations) imp.getCanvas()).isShowingCells();
	}
}
