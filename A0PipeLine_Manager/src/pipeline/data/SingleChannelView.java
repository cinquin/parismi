/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.LookUpTable;
import ij.VirtualStack;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.io.RoiEncoder;
import ij.io.TiffEncoder;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.ShortProcessor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.jdom.Document;

import pipeline.ParseImageMetadata;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

/**
 * Abstracts away the structure of hyperstacks, and provides a view of a single channel that actually resides
 * in a hyperstack with any number of other channels, or within a regular stack. Also provides transparent storage
 * to a temporary TIFF file of the channel; this is used by the {@link pipeline.plugins.ExternalCall} plugin.
 * 
 */
public class SingleChannelView extends PluginIOStack {

	private static final long serialVersionUID = 1L;

	private int currentChannel;
	private int nFrames;
	private int sliceInterval;

	private String tempFileStorage, fileStorage;
	public String channelName;

	private float[][] pixelsCastAsFloat = null;

	@Override
	public Calibration getCalibration() {
		if (getImp() == null)
			return null;
		else
			return getImagePlusDisplay().getCalibration();
	}

	@Override
	public Object getImageAcquisitionMetadata() {
		if (getImp() == null)
			return null;
		else if (getImagePlusDisplay() != null)
			return getImagePlusDisplay().getProperty("Info");
		else
			return null;
	}

	/**
	 * Returns the value of a pixel at given coordinates, cast to double.
	 * This assumes that the underlying image is a float image.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return pixel value
	 */
	@Override
	public double getDouble(int x, int y, int z) {
		if (pixelsCastAsFloat == null)
			pixelsCastAsFloat = (float[][]) getStackPixelArray();
		return pixelsCastAsFloat[z][x + y * getWidth()];
	}

	/**
	 * Returns the value of a pixel at given coordinates.
	 * This assumes that the underlying image is a float image.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return pixel value
	 */
	@Override
	public final float getFloat(int x, int y, int z) {
		Object pixels = getStackPixelArray()[z];
		if (pixels instanceof float[])
			return ((float[]) pixels)[x + y * getWidth()];
		else if (pixels instanceof byte[])
			return ((byte[]) pixels)[x + y * getWidth()] & 0xff;
		else if (pixels instanceof short[])
			return ((short[]) pixels)[x + y * getWidth()] & 0xffff;
		else
			throw new RuntimeException("Unknown pixel array type " + pixels);
	}

	/**
	 * Generates the name of a temporary file to store the channel contents into, and makes a note a that
	 * name in the ImagePlus where the channel actually resides. If a name already exists, it is reused.
	 * The caller is expected to fill the contents of the temporary file itself.
	 * 
	 * @param namePrefix
	 *            Prefix to use for the temporary file name.
	 * @param nameSuffix
	 *            Suffix to use for the temporary file name.
	 */
	public void createNewFileBackingName(String namePrefix, String nameSuffix) {
		if (fileStorage != null) {
			Utils.log("Temp file already exists; do nothing", LogLevel.DEBUG);
			return;
		}
		NameAndFileBacking namesAndFiles = ParseImageMetadata.extractChannelNamesAndFileStore(getImagePlusDisplay());
		fileStorage = namesAndFiles.filePaths[currentChannel - 1];

		if (fileStorage != null)
			return;
		// IF NAME ALREADY EXISTS, REUSE THE SAME FILE SO WE DON'T PILE UP
		// GIGANTIC TEMPORARY FILES

		try {
			File f = File.createTempFile(namePrefix, nameSuffix);
			fileStorage = f.getPath();
			viewIsStoredInFile(fileStorage);
		} catch (IOException e) {
			Utils.log("Could not create new file backing name " + namePrefix + " " + nameSuffix, LogLevel.DEBUG);
			e.printStackTrace();
			return;
		}

	}

	/**
	 * Makes a note in the metadata of the ImagePlus where the SingleChannelView contents reside that the
	 * channel we are working on has been stored to a temporary file.
	 * 
	 * @param f
	 *            Path to the temporary file containing the channel.
	 */
	void viewIsStoredInFile(String f) {
		fileStorage = f;
		if (channelName == null) {
			channelName = ParseImageMetadata.extractChannelNames(getImagePlusDisplay())[0];
			Utils.log("null channelName in viewIsStoredInFile; using the first name I'm finding in the metadata: "
					+ channelName, LogLevel.DEBUG);
		}
		if (!ParseImageMetadata.parseMetadata(getImagePlusDisplay()).hasRootElement()) {
			// No metadata yet in the imp
			Document doc = new Document();
			getImagePlusDisplay().setProperty("Info",
					ParseImageMetadata.addChannelNameToXML(doc, new String[] { channelName }));
		}
		ParseImageMetadata.updateChannelInfo(getImagePlusDisplay(), channelName, "FileBacking", f);
		ParseImageMetadata.updateChannelInfo(getImagePlusDisplay(), channelName, "LastStorageTime", ""
				+ System.currentTimeMillis());
	}

	/**
	 * Get the path to the temporary file where the SingleChannelView is stored. If that file does not
	 * exist, or if it appears to be stale, we create or update the file before passing the path back.
	 * 
	 * @param programNeedsToReloadFile
	 *            True if the file contents have been updated. In that case, if a running external
	 *            program has a reference to the file, the program needs to be terminated and relaunched or be
	 *            instructed
	 *            to reload the file
	 * @return Path to the temporary file containing an up-to-date copy of the channel.
	 */
	public String getStoreFile(Boolean programNeedsToReloadFile) {
		// for now, we assume that if a plugin stored a file backing it's not going to change the location
		// if it was to change the location, we would need to parse the imp metadata everytime
		// which would be a bit wasteful
		programNeedsToReloadFile = true;
		if (fileStorage == null) {
			// see if someone has stored something in the metadata
			NameAndFileBacking namesAndFiles =
					ParseImageMetadata.extractChannelNamesAndFileStore(getImagePlusDisplay());
			fileStorage = namesAndFiles.filePaths[currentChannel - 1];
			if (fileStorage != null) {
				// check if the file is up to date
				if (namesAndFiles.timesModified[currentChannel - 1] > namesAndFiles.timesStored[currentChannel - 1]) {
					Utils.log("Channel " + channelName + " has a stored file, but it is stale: modified "
							+ namesAndFiles.timesModified[currentChannel - 1] + ", stored "
							+ namesAndFiles.timesStored[currentChannel - 1], LogLevel.ERROR);
					fileStorage = null;
				} else {
					Utils.log("We're reusing file " + fileStorage, LogLevel.DEBUG);
					programNeedsToReloadFile = false;
				}
			}
			if (fileStorage == null) {
				// NO ONE HAS STORED ANYTHING FOR US TO USE, OR THE DATA IS STALE
				// FOR CONVENIENCE FOR THE CALLER, CREATE FILE AND STORE THE DATA TO IT
				// STORE THE NAME IN tempFileStorage SO WE KNOW TO RESAVE THE DATA THE NEXT TIME AROUND
				// BECAUSE THE INPUT COULD VERY WELL HAVE CHANGED SINCE IT DOES NOT COME FROM A PLUGIN
				// THAT SAVES ITS OWN DATA
				// TODO THIS SHOULD BE OPTIMIZED

				if (tempFileStorage == null) {
					File f = null;
					try {
						f = File.createTempFile("automatic_file_store", ".tiff");
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
					tempFileStorage = f.getPath();
					Utils.log("created  temp file " + tempFileStorage, LogLevel.DEBUG);
				}
				viewIsStoredInFile(tempFileStorage);
				this.saveAsTIFF(tempFileStorage);
				return tempFileStorage;
			}
			if (!channelName.equals(namesAndFiles.channelNames[currentChannel - 1])) {
				Utils.log("Warning: channel name appears to have changed from " + channelName + " to "
						+ namesAndFiles.channelNames[currentChannel - 1], LogLevel.DEBUG);
			}
		}

		File f = new File(fileStorage);
		if (!f.exists()) {
			Utils.log("File " + fileStorage + " has probably disappeared", LogLevel.DEBUG);
			fileStorage = null;
			return getStoreFile(programNeedsToReloadFile);
		}

		return fileStorage;
	}

	private boolean isHyperStack;

	/**
	 * Constructs a new SingleChannelView of specified depth by performing a deep copy of the specified 2D image.
	 * 
	 * @param processorToReplicate
	 *            ImageProcessor whose xy dimensions should be reused to specify the dimensions of the new channels,
	 *            and whose contents should be copied into every slices.
	 * @param depth
	 *            Number of slices to be created in new SingleChannelView.
	 */
	public SingleChannelView(ImageProcessor processorToReplicate, int depth) {
		super();
		ImageStack stack = new ImageStack(processorToReplicate.getWidth(), processorToReplicate.getHeight());
		for (int i = 0; i < depth; i++) {
			stack.addSlice("Slice " + (i + 1), processorToReplicate.duplicate());
		}
		this.setImagePlusDisplay(new ImagePlus("ImagePlus automatically created by SingleChannelView constructor",
				stack));
		this.currentChannel = 1;
		this.setnChannels(1);
		this.setDepth(depth);
		this.nFrames = 1;
		this.sliceInterval = 1;
		this.setWidth(getImagePlusDisplay().getWidth());
		this.setHeight(getImagePlusDisplay().getHeight());
	}

	/**
	 * Create a new SingleChannel view by duplicating an existing one. This is a deep copy of the pixel arrays.
	 * 
	 * @param inputToReplicate
	 */
	public SingleChannelView(SingleChannelView inputToReplicate) {
		this.currentChannel = 1;
		this.setnChannels(1);
		this.setDepth(inputToReplicate.getDepth());
		this.nFrames = 1;
		this.sliceInterval = 1;

		ImageStack stack =
				new ImageStack(inputToReplicate.getImagePlusDisplay().getWidth(), inputToReplicate
						.getImagePlusDisplay().getHeight());
		for (int i = 0; i < getDepth(); i++) {
			stack.addSlice("Slice " + i, inputToReplicate.getPixelsAsProcessor(i).duplicate());
		}
		this.setImagePlusDisplay(new ImagePlus(
				"ImagePlus automatically created by SingleChannelView constructor, duplicating from preexisting SingleChannelView",
				stack));
		this.setWidth(inputToReplicate.getWidth());
		this.setHeight(inputToReplicate.getHeight());
	}

	/**
	 * Create a blank float SingleChannel view of given dimensions. The public variable
	 * imp will be null so it should not be used by the caller.
	 * 
	 * @param width
	 * @param height
	 * @param depth
	 */
	public SingleChannelView(int width, int height, int depth) {
		this.currentChannel = 1;
		this.setnChannels(1);
		this.setDepth(depth);
		this.nFrames = 1;
		this.sliceInterval = 1;

		setStackPixelArray(new Object[depth]);

		for (int i = 0; i < depth; i++) {
			getStackPixelArray()[i] = new float[width * height];
		}
		this.setImp(null);
		this.setWidth(width);
		this.setHeight(height);
	}

	/**
	 * Construct a SingleChannelView from a given channel of a stack or hyperstack.
	 * 
	 * @param imp
	 *            ImagePlus stack or hyperstack to extract the channel from
	 * @param channel
	 *            Indexing starts from 1, per ImageJ convention.
	 * @param name
	 *            TODO
	 */
	public SingleChannelView(ImagePlus imp, int channel, String name) {
		super(name);

		this.setImagePlusDisplay(imp);
		currentChannel = channel;
		nFrames = 1;// ignore time frames for now, but this might be worth implementing at some point

		isHyperStack = getImagePlusDisplay().isHyperStack();
		if (isHyperStack) {
			setnChannels(1);
			sliceInterval = nFrames * imp.getNChannels();
			setDepth(imp.getStackSize() / sliceInterval);
		} else {
			setnChannels(1);
			setDepth(imp.getStackSize() / imp.getNChannels());
			sliceInterval = 1;
		}
		this.setWidth(imp.getWidth());
		this.setHeight(imp.getHeight());
		this.calibration = imp.getCalibration();
		setImageAcquisitionMetadata(imp.getProperty("Info"));
	}

	/**
	 * Construct a SingleChannelView from a stack that has many channels. The caller
	 * must specify the dimensions of the stack for us to be able to retrieve the channel
	 * 
	 * @param stack
	 *            stack to extract the channel from
	 * @param channel
	 *            Indexing starts from 1, per ImageJ convention.
	 * @param nChannels
	 *            Number of channels in the stack
	 * @param depth
	 *            Number of slices in the stack
	 */
	public SingleChannelView(ImageStack stack, int channel, int nChannels, int depth) { // minimum value for channel is
																						// 1
		this.setImp(null);
		currentChannel = channel;
		nFrames = 1;// ignore time frames for now, but this might be worth implementing at some point
		this.setnChannels(1);
		sliceInterval = nFrames * nChannels;
		this.setDepth(depth);
		this.setWidth(stack.getWidth());
		this.setHeight(stack.getHeight());

		setStackPixelArray(new Object[depth]);
		for (int i = 0; i < depth; i++) {
			getStackPixelArray()[i] = stack.getPixels(i + 1);
		}
	}

	/**
	 * Construct a SingleChannelView from an ImagePlus with metadata generated by the pipeline,
	 * naming the desired channel. Throws a RuntimeException if the channel cannot be found,
	 * 
	 * @param imp
	 *            Source ImagePlus
	 * @param channel
	 *            Name of the channel to select.
	 */
	public SingleChannelView(ImagePlus imp, String channel) {
		this.setImagePlusDisplay(imp);
		this.channelName = channel;

		String[] channelNames = ParseImageMetadata.extractChannelNames(imp);

		int channelIndex = Utils.indexOf(channelNames, channel);
		if (channelIndex < 0) {
			throw new PluginRuntimeException("Channel " + channel + " does not exist in "
					+ Utils.printStringArray(channelNames), true);
		}
		currentChannel = channelIndex + 1;// ImageJ channels need to be numbered from 1
		nFrames = 1;// ignore time frames for now, but this might be worth implementing at some point

		isHyperStack = imp.isHyperStack();
		if (isHyperStack) {
			setnChannels(1);
			sliceInterval = nFrames * imp.getNChannels();
			setDepth(imp.getStackSize() / sliceInterval);
		} else {
			setnChannels(1);
			setDepth(imp.getStackSize());
		}
		if (currentChannel > getnChannels()) {
			// throw new RuntimeException("wrong channel index");
		}
		this.setWidth(imp.getWidth());
		this.setHeight(imp.getHeight());
	}

	public final int getSliceIndex(int s) {
		if (isHyperStack) {
			return (s - 1) * sliceInterval + currentChannel;
		} else
			return s;
	}

	// public Object [] stackPixelArray=null;
	// Now exists in supertype

	/**
	 * Fill in stackPixelArray for direct, fast access by whoever is using this view
	 */
	@Override
	public void computePixelArray() {
		if (getImp() == null) {
			throw new RuntimeException(
					"No imp to compute pixel array from; this was probably created from an ImageStack, in which case the pixel array should already be filled in");
		}
		setStackPixelArray(new Object[getDepth()]);
		for (int i = 0; i < getDepth(); i++) {
			if (pType != null)
				getStackPixelArray()[i] =
						convertArray(getImagePlusDisplay().getStack()
								.getProcessor((i) * sliceInterval + currentChannel).getPixels(), pType);// getPixelsAsProcessor(i+1).getPixels(),pType
			else
				getStackPixelArray()[i] =
						getImagePlusDisplay().getStack().getProcessor((i) * sliceInterval + currentChannel).getPixels();
		}
	}

	/**
	 * Called when converting to a new pixel type
	 */
	public void computePixelArray(PixelType newType) {
		setStackPixelArray(new Object[getDepth()]);
		for (int i = 0; i < getDepth(); i++) {
			if (newType != null) {
				Object newArray = convertArray(getPixels(i + 1), newType);
				if (!(newArray instanceof float[])) {
					throw new RuntimeException("Unsuccessful conversion");
				}
				getStackPixelArray()[i] = newArray;
				if (!(getStackPixelArray()[i] instanceof float[])) {
					throw new RuntimeException("Unsuccessful conversion");
				}
			} else {
				getStackPixelArray()[i] = getPixels(i + 1);
			}
		}

		// TODO UPDATE THE IMAGEPLUS
	}

	@Override
	public final ImageProcessor getPixelsAsProcessor(int s) {
		if (getStackPixelArray() != null) {
			Object pixels = getStackPixelArray()[s];
			if (pixels instanceof float[])
				return new FloatProcessor(getWidth(), getHeight(), (float[]) pixels, null);
			else if (pixels instanceof short[])
				return new ShortProcessor(getWidth(), getHeight(), (short[]) pixels, null);
			else if (pixels instanceof byte[])
				return new ByteProcessor(getWidth(), getHeight(), (byte[]) pixels, null);
			throw new IllegalStateException("Unknown pixel type " + pixels);
		}
		if (isHyperStack) {
			return getImagePlusDisplay().getStack().getProcessor((s) * sliceInterval + currentChannel);
		} else
			return getImagePlusDisplay().getStack().getProcessor(s);
	}

	/**
	 * Indexing beings at 0
	 */
	@Override
	public final Object getPixels(int s) {
		if ((getStackPixelArray() == null) || getStackPixelArray()[s] == null)
			computePixelArray();
		if (getStackPixelArray() != null)
			return getStackPixelArray()[s];
		if (isHyperStack) {
			return getImagePlusDisplay().getStack().getProcessor((s) * sliceInterval + currentChannel).getPixels();
		} else
			return getImagePlusDisplay().getStack().getProcessor(s + 1).getPixels();
	}

	/**
	 * Just copied from ImageJ source; unfortunately the method is private.
	 * 
	 * @param imp
	 * @return overlay
	 */
	private static byte[][] getOverlay(ImagePlus imp) {
		if (imp.getHideOverlay())
			return null;
		Overlay overlay = imp.getOverlay();
		if (overlay == null) {
			ImageCanvas ic = imp.getCanvas();
			if (ic == null)
				return null;
			overlay = ic.getShowAllList(); // ROI Manager "Show All" list
			if (overlay == null)
				return null;
		}
		int n = overlay.size();
		if (n == 0)
			return null;
		byte[][] array = new byte[n][];
		for (int i = 0; i < overlay.size(); i++) {
			Roi roi = overlay.get(i);
			array[i] = RoiEncoder.saveAsByteArray(roi);
		}
		return array;
	}

	/**
	 * Just copied from ImageJ source; unfortunately the method is private.
	 * 
	 * @param imp
	 * @param fi
	 */
	static void saveDisplayRangesAndLuts(ImagePlus imp, FileInfo fi) {
		CompositeImage ci = (CompositeImage) imp;
		int channels = imp.getNChannels();
		fi.displayRanges = new double[channels * 2];
		for (int i = 1; i <= channels; i++) {
			LUT lut = ci.getChannelLut(i);
			fi.displayRanges[(i - 1) * 2] = lut.min;
			fi.displayRanges[(i - 1) * 2 + 1] = lut.max;
		}
		if (ci.hasCustomLuts()) {
			fi.channelLuts = new byte[channels][];
			for (int i = 0; i < channels; i++) {
				LUT lut = ci.getChannelLut(i + 1);
				byte[] bytes = lut.getBytes();
				if (bytes == null) {
					fi.channelLuts = null;
					break;
				}
				fi.channelLuts[i] = bytes;
			}
		}
	}

	/**
	 * Saves the SingleChannelView to a TIFF file, at the path given by String
	 * 
	 * @param path
	 */
	void saveAsTIFF(String path) {
		FileInfo fi = getFileInfo();
		FileSaver fileSaver = new FileSaver(getImagePlusDisplay());

		if (fi.nImages == 1) {
			throw new PluginRuntimeException("This is not a stack", true);
		}
		if (getImagePlusDisplay().getStack().isVirtual())
			fi.virtualStack = (VirtualStack) getImagePlusDisplay().getStack();
		Object info = getImagePlusDisplay().getProperty("Info");
		if (info != null && (info instanceof String))
			fi.info = (String) info;
		fi.description = fileSaver.getDescriptionString();
		fi.sliceLabels = getImagePlusDisplay().getStack().getSliceLabels();
		fi.roi = RoiEncoder.saveAsByteArray(getImagePlusDisplay().getRoi());
		fi.overlay = getOverlay(getImagePlusDisplay());
		if (getImagePlusDisplay().isComposite())
			saveDisplayRangesAndLuts(getImagePlusDisplay(), fi);
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))) {
			TiffEncoder file = new TiffEncoder(fi);
			file.write(out);
		} catch (IOException e) {
			Utils.printStack(e);
		}

	}

	/**
	 * Get a z-array of xy pixel values.
	 * 
	 * @return The xy pixels values come as Objects whose type depends on pixel depth
	 *         (probably byte[], short[], or float[])
	 */
	Object[] getSingleChannelArray() {
		Object[] thePixels = new Object[getDepth()];
		for (int i = 0; i < getDepth(); i++) {
			thePixels[i] = getPixelsAsProcessor(i).getPixels();
		}
		return thePixels;
	}

	/**
	 * Copied and modified from ImageJ source (ImagePlus.java)
	 * Modified to return only the selected channel, rather than
	 * the whole hyperstack
	 * Returns a FileInfo object containing information, including the
	 * pixel array, needed to save this image. Use getOriginalFileInfo()
	 * to get a copy of the FileInfo object used to open the image.
	 * 
	 * @see ij.io.FileInfo
	 *      see #getOriginalFileInfo
	 *      see #setFileInfo
	 */
	FileInfo getFileInfo() {
		FileInfo fi = new FileInfo();
		fi.width = getImagePlusDisplay().getWidth();
		fi.height = getImagePlusDisplay().getHeight();
		fi.nImages = getDepth();
		if (getImagePlusDisplay().isComposite())// ** original condition was imagePlusDisplay.compositeImage, WHICH IS
												// NOT EXACTLY THE SAME
			fi.nImages = getImagePlusDisplay().getImageStackSize();
		fi.whiteIsZero = getImagePlusDisplay().isInvertedLut();
		fi.intelByteOrder = false;
		// imagePlusDisplay.setupProcessor(); this is a private function
		if (fi.nImages == 1)
			fi.pixels = getImagePlusDisplay().getProcessor().getPixels();
		else
			fi.pixels = getSingleChannelArray();
		Calibration cal = getImagePlusDisplay().getCalibration();
		if (cal.scaled()) {
			fi.pixelWidth = cal.pixelWidth;
			fi.pixelHeight = cal.pixelHeight;
			fi.unit = cal.getUnit();
		}
		if (fi.nImages > 1)
			fi.pixelDepth = cal.pixelDepth;
		fi.frameInterval = cal.frameInterval;
		if (cal.calibrated()) {
			fi.calibrationFunction = cal.getFunction();
			fi.coefficients = cal.getCoefficients();
			fi.valueUnit = cal.getValueUnit();
		}
		switch (getImagePlusDisplay().getType()) {
			case ImagePlus.GRAY8:
			case ImagePlus.COLOR_256:
				LookUpTable lut = getImagePlusDisplay().createLut();
				if (getImagePlusDisplay().getType() == ImagePlus.COLOR_256 || !lut.isGrayscale())
					fi.fileType = FileInfo.COLOR8;
				else
					fi.fileType = FileInfo.GRAY8;
				fi.lutSize = lut.getMapSize();
				fi.reds = lut.getReds();
				fi.greens = lut.getGreens();
				fi.blues = lut.getBlues();
				break;
			case ImagePlus.GRAY16:
				if (getImagePlusDisplay().isComposite() && fi.nImages == 3)
					fi.fileType = FileInfo.RGB48;
				else
					fi.fileType = FileInfo.GRAY16_UNSIGNED;
				break;
			case ImagePlus.GRAY32:
				fi.fileType = FileInfo.GRAY32_FLOAT;
				break;
			case ImagePlus.COLOR_RGB:
				fi.fileType = FileInfo.RGB;
				break;
			default:
		}
		return fi;
	}

}
