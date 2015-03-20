/*******************************************************************************
 * A0PipelineManager v1.0
 * Copyright (c) 2010 Olivier Cinquin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package pipeline.data.tiff_read_write;

import ij.ImageJ;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.data.ChannelInfo;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.ImageAccessor;
import pipeline.data.InputOutputObjectDimensions;
import pipeline.data.InputOutputObjectDimensions.dimensionType;
import pipeline.data.PixelIterator;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOStack;
import pipeline.data.SliceAccessor;
import pipeline.data.SliceAccessorAdapter;
import pipeline.misc_util.FileNameUtils;
import pipeline.misc_util.FormatException;
import pipeline.misc_util.SettableBoolean;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

// FIXME When opened for writing and accessed as a stack to write out slices, this class might not behave properly
// TODO Honor caching for writing
// TODO Reduce code duplication
public class TIFFFileAccessor extends PluginIOHyperstack implements ImageAccessor, IPluginIOStack {

	@Override
	public IPluginIOStack addChannel(String name) {
		if ((!openedForWriting))
			return super.addChannel(name);
		if (getnChannels() > 0)
			throw new IllegalStateException("Cannot add more than 1 channel to a TIFF already open for writing");
		getChannels().put(name, this);
		return this;
	}

	private static final long serialVersionUID = 1L;
	private @NonNull File f;
	private FileInfo fi;
	private BareBonesFileInfoLongOffsets[] fiArray;
	private RandomAccessFile outStream;
	private FileInputStream inStream;
	private SequentialTiffSliceEncoder writer;

	private boolean closed = false;

	@Override
	public void setDimensions(int x, int y, int z, int c, int t) {
		setWidth(x);
		setHeight(y);
		setnChannels(c);
		nTimePoints = t;
		setDepth(z);
		try {
			openForSequentialWrite();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InputOutputObjectDimensions getDimensions() {
		return new InputOutputObjectDimensions(getWidth(), getHeight(), getDepth(), getnChannels(), nTimePoints);
	}

	@Override
	protected void finalize() {
		try {
			if (!closed)
				close();
		} catch (Exception e) {
			Utils.printStack(e);
		}
		try {
			super.finalize();
		} catch (Throwable e) {
			Utils.printStack(e);
		}
	}

	public TIFFFileAccessor(@NonNull File f, String name) {
		setDiskLocation(f.getAbsolutePath());
		setSupportsWritingToPixels(false);
		// TODO We need a special implementation for virtual stacks
		// TODO The current code might not handle the case where the image is not a stack
		fi = new FileInfo();
		fi.fileName = f.getAbsolutePath();
		this.f = f;
		this.setName(name);
		// Hold off channel creation until we know dimensions
	}

	/**
	 * Constructor used to create a new file of a specified pixel depth. For now only float pixel type is implemented.
	 * 
	 * @param f
	 * @param name
	 * @param pType
	 * @param cal
	 * @param useBigTIFF
	 */
	public TIFFFileAccessor(@NonNull File f, String name, PixelType pType, Calibration cal, boolean useBigTIFF) {
		calibration = cal;
		openedForWriting = true;
		this.pType = pType;
		pixelTypeForWriting = pType;
		isBigTIFF = useBigTIFF;
		setSupportsWritingToPixels(false);
		// TODO We need a special implementation for virtual stacks
		// TODO The current code might not handle the case where the ImagePlus is not a stack
		fi = new FileInfo();
		fi.fileName = f.getAbsolutePath();
		this.f = f;
		this.setName(name);
		// Hold off channel creation until we know our dimensions
	}

	@Override
	synchronized public void close() throws IOException {
		if (closed)
			return;
		if (writer != null)
			writer.finishWriting();
		if (outStream != null)
			outStream.close();
		if (inStream != null)
			inStream.close();
		if (cachedSlices != null)
			cachedSlices.asMap().clear();
		closed = true;

	}

	private boolean isBigTIFF = true;
	private boolean littleEndian = true;

	public native int turnOffCaching(FileDescriptor fd);

	public native int turnOnCaching(FileDescriptor fd);

	private static boolean shownNativeLibError = false;

	// File is kept open on purpose
	@SuppressWarnings("resource")
	@Override
	public void openForSequentialRead() throws IOException, FormatException {
		long time0 = System.currentTimeMillis();
		inStream = new FileInputStream(f);

		RandomAccessFile file = null;

		try {
			file = new RandomAccessFile(f, "rw");
		} catch (FileNotFoundException e) {
			file = new RandomAccessFile(f, "r");
		}

		BigTiffDecoder decoder = new BigTiffDecoder(file, f.getName());
		// Utils.log("-----------Time to opening: "+(System.currentTimeMillis()-time0),LogLevel.VERBOSE_DEBUG);
		fiArray = decoder.getTiffInfo();
		boolean recomputefiArray = true;
		float spacing;
		double frameInterval = 0;
		ChannelInfo[] infoArray = null;
		try {
			Properties props = new Properties();
			nTimePoints = 1;
			if (fiArray[0].description != null) {
				InputStream is = new ByteArrayInputStream(fiArray[0].description.getBytes());
				props.load(is);
				is.close();
				spacing = props.get("spacing") == null ? 1f : Float.valueOf((String) props.get("spacing"));
				setnChannels(props.get("channels") == null ? 1 : Integer.valueOf((String) props.get("channels")));

				if (props.get("originalFile") != null) {
					String originalFile = (String) props.get("originalFile");
					int index = 0;
					ChannelInfo info = new ChannelInfo();
					info.setOriginalFilePath(originalFile);
					while (props.get("detection" + index) != null) {
						info.getDetectionRanges().add(Integer.parseInt((String) props.get("detection" + index)));
						index++;
					}

					Iterator<Integer> detectionRangeIt = info.getDetectionRanges().iterator();
					infoArray = new ChannelInfo[getnChannels()];
					for (int c = 0; c < getnChannels(); c++) {
						infoArray[c] = new ChannelInfo();
						infoArray[c].setOriginalFilePath(originalFile);
						int n = 0;
						while ((detectionRangeIt.hasNext()) && (n < 2)) {
							infoArray[c].getDetectionRanges().add(detectionRangeIt.next());
							n++;
						}
					}
					setImageAcquisitionMetadata(infoArray);
				}

			} else {
				// Probably a TIFF (or LSM) that wasn't created by ImageJ or by us, and that does not have an image
				// description
				String fileName = f.getName();
				int dot = fileName.lastIndexOf('.');
				String extension = fileName.substring(dot + 1);
				if ("lsm".equals(extension)) {
					// Reconstruct fiArray using strip offsets and samplesPerPixel; get rid of thumbnails
					Vector<BareBonesFileInfoLongOffsets> newFiArray = new Vector<>();
					nTimePoints = fiArray[0].nTimePoints;
					frameInterval = fiArray[0].frameInterval;
					@SuppressWarnings("unused")
					int counter = 0;

					infoArray = fiArray[0].channelInfo;
					String path = f.getAbsolutePath();
					if (infoArray != null) {
						for (ChannelInfo i : infoArray) {
							i.setOriginalFilePath(path);
						}
					}
					setImageAcquisitionMetadata(infoArray);

					for (BareBonesFileInfoLongOffsets fi1 : fiArray) {
						counter++;
						if (fi1.isThumbnail)
							continue;
						setnChannels(fi1.stripOffsets.length);
						if (fi1.stripOffsets.length == 1)
							newFiArray.add(fi1);
						else {
							for (int i = 0; i < fi1.stripOffsets.length; i++) {
								BareBonesFileInfoLongOffsets newFi = (BareBonesFileInfoLongOffsets) fi1.clone();
								newFi.samplesPerPixel = 1;
								newFi.offset = fi1.stripOffsets[i];
								newFi.stripOffsets = null;
								newFi.stripLengths = null;
								newFiArray.add(newFi);
								// Utils.log("Slice "+counter+" offset is "+fi.stripOffsets[i],LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
							}
						}
					}
					fiArray = newFiArray.toArray(new BareBonesFileInfoLongOffsets[] {});
					recomputefiArray = false;
					spacing = (float) fiArray[0].pixelDepth;
				} else {
					setnChannels(1);
					spacing = 1;
				}
			}
		} catch (IOException e) {
			Utils.log("Could not parse TIFF ImageJ properties: " + e, LogLevel.WARNING);
			setnChannels(1);
			spacing = 1;
		} catch (NumberFormatException e) {
			Utils.log("Could not parse TIFF ImageJ property number: " + e, LogLevel.WARNING);
			setnChannels(1);
			spacing = 1;
		}

		/*
		 * try {
		 * // Turn off file caching by the OS
		 * Utils.log("Library path: "+System.getProperty("java.library.path"),LogLevel.DEBUG);
		 * System.loadLibrary("pipeline.data.TIFFFileAccessor");
		 * 
		 * int cachingResult=turnOffCaching(inStream.getFD());
		 * if (cachingResult==-1)
		 * Utils.log("Caching not successfully turned off for file "+f.getAbsolutePath(),LogLevel.ERROR);
		 * Utils.log("Turned off caching for file descriptor "+cachingResult,LogLevel.DEBUG);
		 * } catch (Throwable e){
		 * if (!shownNativeLibError && !Utils.headless){
		 * Utils.printStack(e, LogLevel.DEBUG);
		 * shownNativeLibError=true;
		 * }
		 * }
		 */
		isBigTIFF = decoder.bigTIFF;
		littleEndian = decoder.littleEndian;

		if ((getWidth() > 0) && (fiArray[0].width != getWidth())) {
			throw new RuntimeException(
					"Width set before opening file and does not match what was found in the TIFF: expected "
							+ getWidth() + " but found " + fiArray[0].width);
		}
		if ((getHeight() > 0) && (fiArray[0].height != getHeight())) {
			throw new RuntimeException(
					"Width set before opening file and does not match what was found in the TIFF: expected "
							+ getHeight() + " but found " + fiArray[0].height);
		}
		setWidth(fiArray[0].width);
		setHeight(fiArray[0].height);

		calibration = new Calibration();
		calibration.pixelHeight = fiArray[0].pixelHeight;
		calibration.pixelWidth = fiArray[0].pixelWidth;
		calibration.pixelDepth = spacing;
		calibration.frameInterval = frameInterval;
		// FIXME Calibration units should be read from the file instead of forced to micron
		if (calibration.pixelWidth != 1) {
			calibration.setUnit("microns");
			calibration.setXUnit("microns");
			calibration.setYUnit("microns");
			calibration.setZUnit("microns");
		}
		Utils.log("Pixel depth is " + fiArray[0].pixelDepth, LogLevel.VERBOSE_DEBUG);
		// FIXME Read in pixel depth properly when we expand to non-float images
		String pixelTypeString = fiArray[0].getType();
		switch (pixelTypeString) {
			case "float":
				pType = PixelType.FLOAT_TYPE;
				break;
			case "ushort":
				pType = PixelType.SHORT_TYPE;
				break;
			case "short":
				pType = PixelType.SHORT_TYPE;
				break;
			case "byte":
				pType = PixelType.BYTE_TYPE;
				break;
			case "byte+lut":
				pType = PixelType.BYTE_TYPE;
				break;
			default:
				throw new FormatException("Unrecognized pixel type " + pixelTypeString);
		}

		if (pType == PixelType.BYTE_TYPE)
			fi.fileType = FileInfo.GRAY8;
		else if (pType == PixelType.SHORT_TYPE)
			fi.fileType = FileInfo.GRAY16_UNSIGNED;
		else if (pType == PixelType.FLOAT_TYPE)
			fi.fileType = FileInfo.GRAY32_FLOAT;

		BareBonesFileInfoLongOffsets fi1 = fiArray[0];
		int n = fi1.nImages;
		if (fiArray.length == 1 && n > 1 && recomputefiArray) {
			fiArray = new BareBonesFileInfoLongOffsets[n];
			long size = fi1.width * fi1.height * fi1.getBytesPerPixel();
			for (int i = 0; i < n; i++) {
				fiArray[i] = (BareBonesFileInfoLongOffsets) fi1.clone();
				fiArray[i].nImages = 1;
				fiArray[i].offset = fi1.getOffset() + i * (size + fi1.gapBetweenImages);
				Utils.log("Slice " + i + " offset is " + fiArray[i].offset, LogLevel.VERBOSE_VERBOSE_VERBOSE_DEBUG);
			}
		}

		if (fi1.rotation != 0) {
			calibration.rotation = fi1.rotation;
		}

		if ((getDepth() > 0) && (fiArray.length != getDepth())) {
			throw new RuntimeException(
					"Depth set before opening file and does not match what was found in the TIFF: expected "
							+ getDepth() + " but found " + fiArray.length);
		}

		setDepth(fiArray.length / (getnChannels() * nTimePoints));
		setnTimePoints(nTimePoints);
		Utils.log("Identified " + getDepth() + " slices with " + getnChannels() + " channels" + getnTimePoints()
				+ " time points", LogLevel.VERBOSE_VERBOSE_DEBUG);
		dirtySlices = new boolean[getDepth() * getnChannels() * getnTimePoints()];
		lastCachedSliceAccessTime = new long[getDepth() * getnChannels() * getnTimePoints()];
		sliceCanBeRead = new boolean[getDepth() * getnChannels() * getnTimePoints()];
		for (int i = 0; i < getDepth() * getnChannels() * getnTimePoints(); i++)
			sliceCanBeRead[i] = true;

		inputFileChannel = inStream.getChannel();
		byteBuffer = ByteBuffer.allocateDirect(getWidth() * getHeight() * fi1.getBytesPerPixel());
		if (!littleEndian)
			byteBuffer.order(java.nio.ByteOrder.BIG_ENDIAN);// ()
		else
			byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
		if (pType == PixelType.FLOAT_TYPE) {
			readingFloatBuffer = byteBuffer.asFloatBuffer();
			cachedFloatPixels = new float[getDepth() * getnChannels() * getnTimePoints()][];
		} else if (pType == PixelType.SHORT_TYPE) {
			readingShortBuffer = byteBuffer.asShortBuffer();
			cachedShortPixels = new short[getDepth() * getnChannels() * getnTimePoints()][];
		} else if (pType == PixelType.BYTE_TYPE) {
			cachedBytePixels = new byte[getDepth() * getnChannels() * getnTimePoints()][];
		} else
			throw new FormatException("Unsupported pixel type " + pType);
		Utils.log("Time to end of open method: " + (System.currentTimeMillis() - time0), LogLevel.VERBOSE_DEBUG);
		if (getName() == null)
			setName(FileNameUtils.compactPath(f.getName()));

		setChannels(new HashMap<String, IPluginIOStack>());
		for (int i = 0; i < getnChannels(); i++) {
			PluginIOStack stack = new PluginIOStack(this, i, "Ch" + i);
			if ((infoArray != null) && (infoArray.length > 0)) {
				stack.setImageAcquisitionMetadata(infoArray[i]);
			}
			getChannels().put("Ch" + i, stack);
		}

	}

	/**
	 * Used to reopen a file that has already been read and written (so that all the offsets are known already).
	 * 
	 * @throws IOException
	 */
	private void reOpen() throws IOException {
		if (fiArray.length == 0)
			throw new IllegalStateException("reOpen called before TIFF offsets were read in or established for writing");
		if (writer != null)
			writer.finishWriting();
		if (outStream != null)
			outStream.close();
		if (inStream != null)
			inStream.close();
		if (inputFileChannel != null)
			inputFileChannel.close();
		inStream = new FileInputStream(f);
		inputFileChannel = inStream.getChannel();
		try {
			outStream = new RandomAccessFile(f, "rw");
		} catch (FileNotFoundException e) {
			Utils.log("Could not reOpen outStream with write permission", LogLevel.DEBUG);
			outStream = new RandomAccessFile(f, "r");
		}
		closed = false;
	}

	private static boolean shouldCache(int cachePolicy) {
		// TODO Implement more complex logic later; will probably need to interact with the pipeline
		return (cachePolicy == TRY_TO_CACHE_PIXELS);
	}

	private ByteBuffer byteBuffer;
	private FloatBuffer readingFloatBuffer;
	private ShortBuffer readingShortBuffer;
	private FileChannel inputFileChannel;

	private boolean openedForWriting = false;
	private PixelType pixelTypeForWriting;

	@Override
	public void openForSequentialWrite() throws IOException {
		if (getDepth() == 0)
			throw new IllegalStateException("Dimensions need to be set before opening for writing");
		if (closed) {
			reOpen();
			return;
		}
		openedForWriting = true;
		pixelTypeForWriting = pType;
		cachedFloatPixels = new float[getDepth() * getnChannels() * nTimePoints][];
		dirtySlices = new boolean[getDepth() * getnChannels() * nTimePoints];
		lastCachedSliceAccessTime = new long[getDepth() * getnChannels() * nTimePoints];
		sliceCanBeRead = new boolean[getDepth() * getnChannels() * nTimePoints];
		lastWrittenSlice = -1;

		// The following is in case someone tries to read from this ImageAccessor later on
		byteBuffer = ByteBuffer.allocateDirect(getWidth() * getHeight() * 4);
		if (!littleEndian)
			byteBuffer.order(java.nio.ByteOrder.BIG_ENDIAN);
		else
			byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
		readingFloatBuffer = byteBuffer.asFloatBuffer();

		outStream = new RandomAccessFile(f, "rw");
		inStream = new FileInputStream(f);

		/*
		 * try {
		 * //Turn off file caching by the OS
		 * System.loadLibrary("pipeline.data.TIFFFileAccessor");
		 * Utils.log("Library path: "+System.getProperty("java.library.path"),LogLevel.DEBUG);
		 * int turnOffCachingResult=turnOffCaching(outStream.getFD());
		 * if (turnOffCachingResult==-1)
		 * Utils.log("Could not turn off caching for file "+f.getAbsolutePath(),LogLevel.ERROR);
		 * turnOffCachingResult=turnOffCaching(inStream.getFD());
		 * if (turnOffCachingResult==-1)
		 * Utils.log("Could not turn off caching for file "+f.getAbsolutePath(),LogLevel.ERROR);
		 * Utils.log("Turned off caching for file descriptor "+turnOffCachingResult,LogLevel.DEBUG);
		 * } catch (Throwable e){
		 * if (!shownNativeLibError && !Utils.headless){
		 * Utils.printStack(e);
		 * }
		 * }
		 */

		inputFileChannel = inStream.getChannel();

		fi.nImages = getDepth() * getnChannels() * nTimePoints;
		fi.width = getWidth();
		fi.height = getHeight();
		if (getCalibration() != null) {
			Calibration c = getCalibration();
			fi.pixelDepth = c.pixelDepth;
			fi.pixelHeight = c.pixelHeight;
			fi.pixelWidth = c.pixelWidth;
		}
		// No place to put nTimePoints in FileInfo??
		if (pixelTypeForWriting == PixelType.FLOAT_TYPE)
			fi.fileType = FileInfo.GRAY32_FLOAT;
		else if (pixelTypeForWriting == PixelType.SHORT_TYPE)
			fi.fileType = FileInfo.GRAY16_UNSIGNED;
		else if (pixelTypeForWriting == PixelType.BYTE_TYPE)
			fi.fileType = FileInfo.GRAY8;
		else
			throw new IllegalStateException("Unknown pixel type " + pixelTypeForWriting);
		fi.description = getDescriptionString(fi.nImages, getDepth());

		fiArray = new BareBonesFileInfoLongOffsets[getDepth()];
		for (int i = 0; i < getDepth(); i++) {
			fiArray[i] = new BareBonesFileInfoLongOffsets();
			fiArray[i].width = getWidth();
			fiArray[i].height = getHeight();
			fiArray[i].fileType = fi.fileType;
			fiArray[i].nImages = 1;
			fiArray[i].offset = -1;
			fiArray[i].pixelDepth = fi.pixelDepth;
			fiArray[i].pixelHeight = fi.pixelHeight;
			fiArray[i].pixelWidth = fi.pixelWidth;
		}

		writer =
				new SequentialTiffSliceEncoder(fi, getWidth(), getHeight(), getDepth() * getnChannels() * nTimePoints,
						outStream, isBigTIFF);// ()
		writer.beginWriting();
	}

	private volatile boolean stopWriting = false;

	@Override
	synchronized public void closeFileEarly() throws IOException {
		if (writer == null)
			throw new IllegalStateException("Not writing so cannot close file early");
		if (closed)
			return;
		if (writer.isFinishedWriting())
			return;
		if (lastWrittenSlice == -1)
			throw new IllegalStateException("No slices have been written");
		SequentialTiffSliceEncoder saveWriter = writer;
		stopWriting = true;
		// Set references to file and writer to null, and wait for thread doing the writing to fail
		// This is easier than doing proper synchronization, and does not incur synchronization penalties
		writer = null;
		/*
		 * try {
		 * Thread.sleep(300);
		 * } catch (InterruptedException e) {
		 * Utils.printStack(e);
		 * }
		 */

		saveWriter.finishWritingEarly(getDescriptionString((lastWrittenSlice + 1) * getnChannels() * nTimePoints,
				lastWrittenSlice + 1).getBytes());
		closed = true;
	}

	/** Returns a string containing information about the specified image. */
	// Copied from imageJ
	String getDescriptionString(int nImages, int slices) {
		Calibration cal = calibration;
		if (cal == null)
			cal = new Calibration();
		fi.unit = cal.getUnit();
		StringBuffer sb = new StringBuffer(100);
		sb.append("ImageJ=" + ImageJ.VERSION + "\n");
		if (nImages > 1 && fi.fileType != FileInfo.RGB48)
			sb.append("images=").append(String.format("%09d", nImages)).append("\n");// so length of metadata block
		// in TIFF does not change if number of frames is adjusted
		int channels = this.getnChannels();// 1;//imp.getNChannels();
		// FIXME Update when support for multiple channels is introduced
		if (channels > 1)
			sb.append("channels=").append(channels).append("\n");
		if (slices > 1)
			sb.append("slices=").append(String.format("%09d", slices)).append("\n");
		int frames = 1;
		// FIXME Update when support for multiple frames is introduced
		if (frames > 1)
			sb.append("frames=").append(frames).append("\n");
		/*
		 * if (imp.isHyperStack()) sb.append("hyperstack=true\n");
		 * if (imp.isComposite()) {
		 * String mode = ((CompositeImage)imp).getModeAsString();
		 * sb.append("mode="+mode+"\n");
		 * }
		 */
		sb.append("hyperstack=false\n");
		if (fi.unit != null)
			sb.append("unit=").append(fi.unit.equals("\u00B5m") ? "um" : fi.unit).append("\n");
		if (fi.valueUnit != null && fi.calibrationFunction != Calibration.CUSTOM) {
			sb.append("cf=").append(fi.calibrationFunction).append("\n");
			if (fi.coefficients != null) {
				for (int i = 0; i < fi.coefficients.length; i++)
					sb.append("c").append(i).append("=").append(fi.coefficients[i]).append("\n");
			}
			sb.append("vunit=").append(fi.valueUnit).append("\n");
			if (cal.zeroClip())
				sb.append("zeroclip=true\n");
		}

		// Get stack z-spacing and fps
		if (cal.frameInterval != 0.0) {
			if ((int) cal.frameInterval == cal.frameInterval)
				sb.append("finterval=").append((int) cal.frameInterval).append("\n");
			else
				sb.append("finterval=").append(cal.frameInterval).append("\n");
		}
		if (!cal.getTimeUnit().equals("sec"))
			sb.append("tunit=").append(cal.getTimeUnit()).append("\n");
		if (nImages > 1) {
			if (fi.pixelDepth != 0.0 && fi.pixelDepth != 1.0)
				sb.append("spacing=").append(fi.pixelDepth).append("\n");
			if (cal.fps != 0.0) {
				if ((int) cal.fps == cal.fps)
					sb.append("fps=").append((int) cal.fps).append("\n");
				else
					sb.append("fps=").append(cal.fps).append("\n");
			}
			sb.append("loop=").append(cal.loop ? "true" : "false").append("\n");
		}
		fi.pixelHeight = cal.pixelHeight;
		fi.pixelWidth = cal.pixelWidth;

		// get min and max display values
		/*
		 * ImageProcessor ip = imp.getProcessor();
		 * double min = ip.getMin();
		 * double max = ip.getMax();
		 * int type = imp.getType();
		 * boolean enhancedLut = (type==ImagePlus.GRAY8 || type==ImagePlus.COLOR_256) && (min!=0.0 || max !=255.0);
		 * if (enhancedLut || type==ImagePlus.GRAY16 || type==ImagePlus.GRAY32) {
		 * sb.append("min="+min+"\n");
		 * sb.append("max="+max+"\n");
		 * }
		 */

		// Get non-zero origins
		if (cal.xOrigin != 0.0)
			sb.append("xorigin=").append(cal.xOrigin).append("\n");
		if (cal.yOrigin != 0.0)
			sb.append("yorigin=").append(cal.yOrigin).append("\n");
		if (cal.zOrigin != 0.0)
			sb.append("zorigin=").append(cal.zOrigin).append("\n");
		if (cal.info != null && cal.info.length() <= 64 && cal.info.indexOf('=') == -1 && cal.info.indexOf('\n') == -1)
			sb.append("info=").append(cal.info).append("\n");

		if (getImageAcquisitionMetadata() != null) {
			if (getImageAcquisitionMetadata() instanceof String)
				sb.append(getImageAcquisitionMetadata());
			else if (getImageAcquisitionMetadata() instanceof ChannelInfo[])
				for (ChannelInfo i : (ChannelInfo[]) getImageAcquisitionMetadata()) {
					if (i != null)
						sb.append(i.toString());
				}
			else
				Utils.log("Unknown image acquisition metadata type " + getImageAcquisitionMetadata(), LogLevel.WARNING);
		}

		sb.append((char) 0);
		return new String(sb);
	}

	@Override
	final public void assignPixelsToZSlice(Object pixels, int sliceIndex, int cachePolicy, SettableBoolean justCopied)
			throws FormatException, IOException, InterruptedException {
		copyPixelsIntoZSlice(pixels, sliceIndex, cachePolicy);
		justCopied.value = true;
		// TODO Implement caching if requested by the caller
	}

	private int lastWrittenSlice = -1;

	/*
	 * This method assumes dimensions have already been set and slices will be written sequentially
	 * 
	 * @Override
	 * public void dumpLittleEndianFloatBufferIntoNextSlice(ByteBuffer buffer, int sliceIndex) throws IOException {
	 * //TODO Don't know how well the two writing streams are going to play with each other
	 * if (writer==null) openForSequentialWrite();
	 * if (sliceIndex!=lastWrittenSlice+1){
	 * throw new RuntimeException("Non-sequential slice writing not yet supported");
	 * }
	 * dumpLittleEndianFloatBufferIntoNextSlice(buffer, sliceIndex);
	 * }
	 */

	@Override
	final public void dumpLittleEndianFloatBufferIntoSlice(ByteBuffer buffer, int sliceIndex) throws IOException,
			InterruptedException {
		// Utils.log("Dumping slice "+lastWrittenSlice+1+" into TIFF file "+f.getAbsolutePath(),LogLevel.DEBUG);
		if (stopWriting) {
			return;
		}
		if (closed)
			reOpen();
		if (writer == null)
			openForSequentialWrite();

		writer.dumpBufferIntoSlice(buffer, sliceIndex);

		if (lastWrittenSlice < getDepth()) {// We are on the first pass of writing
			lastWrittenSlice++;
			sliceCanBeRead[lastWrittenSlice] = true;
		}

		// Utils.log(writer.nBytesWritten+" bytes written; just did slice "+sliceIndex,LogLevel.VERBOSE_DEBUG);
	}

	@Override
	final public void copyPixelsIntoZSlice(Object pixels, int sliceIndex, int cachePolicy) throws FormatException,
			IOException, InterruptedException {
		if (stopWriting) {
			return;
		}
		if (writer == null)
			openForSequentialWrite();
		if (sliceIndex != lastWrittenSlice + 1) {
			throw new IllegalStateException(
					"Non-sequential slice writing not yet supported through copyPixelsIntoZSlice");
		}
		if (closed)
			throw new RuntimeException("Cannot write to closed file through copyPixelsIntoZSlice");
		writer.writeSlice(pixels);
		lastWrittenSlice = sliceIndex;
		sliceCanBeRead[sliceIndex] = true;
		if (lastWrittenSlice == (getDepth() * getnChannels() * nTimePoints) - 1)
			close();
		// TODO Implement caching
	}

	private float[][] cachedFloatPixels;
	private short[][] cachedShortPixels;
	private byte[][] cachedBytePixels;
	private boolean[] dirtySlices;
	/**
	 * Used to know which to get rid of first if we're trying to cut down on memory usage
	 */
	private long[] lastCachedSliceAccessTime;
	private boolean[] sliceCanBeRead;

	@Override
	final public Object getPixelZSliceCopy(int sliceIndex, int cachePolicy) throws IOException {
		if (pType == PixelType.FLOAT_TYPE) {
			if (cachedFloatPixels[sliceIndex] != null) {
				lastCachedSliceAccessTime[sliceIndex] = System.currentTimeMillis();
				return cachedFloatPixels[sliceIndex];
			}
			float[] readInto;
			if (shouldCache(cachePolicy)) {
				cachedFloatPixels[sliceIndex] = new float[getWidth() * getHeight()];
				lastCachedSliceAccessTime[sliceIndex] = System.currentTimeMillis();
				readInto = cachedFloatPixels[sliceIndex];
			} else {
				readInto = new float[getWidth() * getHeight()];
			}
			copyPixelSliceIntoArray(sliceIndex, cachePolicy, readInto);
			if (convertToPixelType != null)
				return convertArray(readInto, convertToPixelType);
			else
				return readInto;
		} else if (pType == PixelType.SHORT_TYPE) {
			if (cachedShortPixels[sliceIndex] != null) {
				lastCachedSliceAccessTime[sliceIndex] = System.currentTimeMillis();
				return cachedShortPixels[sliceIndex];
			}
			short[] readInto;
			if (shouldCache(cachePolicy)) {
				cachedShortPixels[sliceIndex] = new short[getWidth() * getHeight()];
				lastCachedSliceAccessTime[sliceIndex] = System.currentTimeMillis();
				readInto = cachedShortPixels[sliceIndex];
			} else {
				readInto = new short[getWidth() * getHeight()];
			}
			copyPixelSliceIntoArray(sliceIndex, cachePolicy, readInto);
			if (convertToPixelType != null)
				return convertArray(readInto, convertToPixelType);
			else
				return readInto;
		} else if (pType == PixelType.BYTE_TYPE) {
			if (cachedBytePixels[sliceIndex] != null) {
				lastCachedSliceAccessTime[sliceIndex] = System.currentTimeMillis();
				return cachedBytePixels[sliceIndex];
			}
			byte[] readInto;
			if (shouldCache(cachePolicy)) {
				cachedBytePixels[sliceIndex] = new byte[getWidth() * getHeight()];
				lastCachedSliceAccessTime[sliceIndex] = System.currentTimeMillis();
				readInto = cachedBytePixels[sliceIndex];
			} else {
				readInto = new byte[getWidth() * getHeight()];
			}
			copyPixelSliceIntoArray(sliceIndex, cachePolicy, readInto);
			if (convertToPixelType != null)
				return convertArray(readInto, convertToPixelType);
			else
				return readInto;
		} else
			throw new IllegalStateException("pType " + pType + " does not correspond to a supported type");
	}

	/**
	 * Indexing begins at 0.
	 * 
	 * @param z
	 * @param channel
	 * @return 1D array of pixels
	 */
	@Override
	public final Object getPixels(int z, int channel, int timePoint) {
		try {
			// Below had channel-1
			Object result = getPixelZSliceCopy(z * getnChannels() + (channel), defaultCachePolicy);
			if (convertToPixelType != null)
				return convertArray(result, convertToPixelType);
			else
				return result;
		} catch (IOException e) {
			Utils.log("Error reading pixels from file " + f.getAbsolutePath(), LogLevel.ERROR);
			throw new RuntimeException(e);
		}
	}

	@Override
	final public Object getReferenceToPixelZSlice(int sliceIndex, int cachePolicy, boolean willModifyPixels,
			SettableBoolean changesNeedExplicitSaving) throws IOException {
		changesNeedExplicitSaving.value = true;
		dirtySlices[sliceIndex] = true;
		Object result = getPixelZSliceCopy(sliceIndex, cachePolicy);
		if (convertToPixelType != null)
			return convertArray(result, convertToPixelType);
		else
			return result;
	}

	final synchronized public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, float[] destinationArray)
			throws IOException {
		if (!sliceCanBeRead[sliceIndex])
			throw new IllegalStateException("Cannot read slice " + sliceIndex
					+ " before it has been written (file was originally opened for writing");
		if ((closed) || (!inputFileChannel.isOpen()))
			reOpen();// throw new RuntimeException("Cannot read from closed file");
		if (byteBuffer == null)
			throw new RuntimeException("Image " + f.getAbsolutePath() + " has not been properly opened for reading");

		byteBuffer.clear();
		int count = inputFileChannel.read(byteBuffer, fiArray[sliceIndex].offset);
		if (count == -1) {
			// The following might not be necessary
			// This was in case the input stream is not in sync with the modifications made by the output stream to the
			// file
			Utils.log("Reopening file " + f.getAbsolutePath(), LogLevel.DEBUG);
			reOpen();
			count = inputFileChannel.read(byteBuffer, fiArray[sliceIndex].offset);
		}
		// fileChannel.transferTo(fiArray[sliceIndex].longOffset+fiArray[sliceIndex].stripOffsets[0], count,
		// (WritableByteChannel) Channels.newChannel(byteBuffer));
		// Utils.log("read "+count+" from offset "+offset,LogLevel.VERBOSE_VERBOSE_DEBUG);
		if (count < getWidth() * getHeight() * 4)
			throw new IOException("Read " + count + " pixels instead of " + (getWidth() * getHeight())
					+ " expected from " + f.getAbsolutePath());

		readingFloatBuffer.clear();
		readingFloatBuffer.get(destinationArray);
		readingFloatBuffer.clear();

	}

	synchronized private void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, short[] destinationArray)
			throws IOException {
		if (!sliceCanBeRead[sliceIndex])
			throw new IllegalStateException("Cannot read slice " + sliceIndex
					+ " before it has been written (file was originally opened for writing");
		if (closed)
			reOpen();
		if (byteBuffer == null)
			throw new RuntimeException("Image " + f.getAbsolutePath() + " has not been properly opened for reading");

		byteBuffer.clear();
		int count = inputFileChannel.read(byteBuffer, fiArray[sliceIndex].offset);
		if (count == -1) {
			// The following might not be necessary
			// This was in case the input stream is not in sync with the modifications made by the output stream to the
			// file
			Utils.log("Reopening file " + f.getAbsolutePath(), LogLevel.DEBUG);
			reOpen();
			count = inputFileChannel.read(byteBuffer, fiArray[sliceIndex].offset);
		}
		if (count < getWidth() * getHeight() * 2)
			throw new IOException("Read " + count + " pixels instead of " + (getWidth() * getHeight())
					+ " expected from " + f.getAbsolutePath());

		readingShortBuffer.clear();
		readingShortBuffer.get(destinationArray);
		readingShortBuffer.clear();
	}

	synchronized private void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, byte[] destinationArray)
			throws IOException {
		if (!sliceCanBeRead[sliceIndex])
			throw new IllegalStateException("Cannot read slice " + sliceIndex
					+ " before it has been written (file was originally opened for writing");
		if (closed || (!inputFileChannel.isOpen()))
			reOpen();
		if (byteBuffer == null)
			throw new RuntimeException("Image " + f.getAbsolutePath() + " has not been properly opened for reading");

		byteBuffer.clear();
		int count = inputFileChannel.read(byteBuffer, fiArray[sliceIndex].offset);
		if (count == -1) {
			// The following might not be necessary
			// This was in case the input stream is not in sync with the modifications made by the output stream to the
			// file
			Utils.log("Reopening file " + f.getAbsolutePath(), LogLevel.DEBUG);
			reOpen();
			count = inputFileChannel.read(byteBuffer, fiArray[sliceIndex].offset);
		}
		if (count < getWidth() * getHeight())
			throw new IOException("Read " + count + " pixels instead of " + (getWidth() * getHeight())
					+ " expected from " + f.getAbsolutePath());

		byteBuffer.clear();
		byteBuffer.get(destinationArray);
		byteBuffer.clear();

	}

	@Override
	public void clearCache() {
		if (cachedFloatPixels != null) {
			for (int i = 0; i < cachedFloatPixels.length; i++) {
				cachedFloatPixels[i] = null;
				lastCachedSliceAccessTime[i] = 0;
			}
		}
		if (cachedShortPixels != null) {
			for (int i = 0; i < cachedShortPixels.length; i++) {
				cachedShortPixels[i] = null;
				lastCachedSliceAccessTime[i] = 0;
			}
		}
		if (cachedBytePixels != null) {
			for (int i = 0; i < cachedBytePixels.length; i++) {
				cachedBytePixels[i] = null;
				lastCachedSliceAccessTime[i] = 0;
			}
		}
	}

	@Override
	public void save() throws IOException {
		// TODO write out the dirty slices
		throw new RuntimeException("Saving of cached TIFF slices not implemented yet");
	}

	@Override
	public void cutDownCacheSizeTo(long size) {
		throw new RuntimeException("cutDownCacheSizeTo not yet implemented.");
	}

	@Override
	public void setMaximumCacheSize(long size) {
		throw new RuntimeException("setMaximumCacheSize not yet implemented.");
	}

	@Override
	public long getLastTimeCacheWasUsed() {
		throw new RuntimeException("getLastTimeCacheWasUsed not yet implemented.");
	}

	@Override
	public long getCurrentCacheSize() {
		throw new RuntimeException("getCurrentCacheSize not yet implemented.");
	}

	@Override
	public Object asPixelArray() {
		throw new RuntimeException("Cannot get pixel array directly from a TIFF file");
	}

	/**
	 * If not null, convert all pixels we send back to this type
	 */
	private PixelType convertToPixelType;

	@Override
	public void convertTo(PixelType newPixelType) {
		convertToPixelType = newPixelType;
		// Stack channel views need to be changed to the new pixel type too
		for (IPluginIOStack c : getChannels().values()) {
			c.setPixelType(convertToPixelType);
			c.setStackPixelArray(null);// To prevent the old pixel type array from being returned
			// it will be automatically recomputed
		}
	}

	@Override
	public PixelType getPixelType() {
		if (openedForWriting)
			return pixelTypeForWriting;
		else if (convertToPixelType != null)
			return convertToPixelType;
		else
			return pType;
	}

	@Override
	public String[] getDimensionLabels(dimensionType dim) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) {
		return f;
	}

	@Override
	public File getBackingFile() {
		return f;
	}

	private File originalSourceFile;

	@Override
	public File getOriginalSourceFile() {
		return originalSourceFile;
	}

	@Override
	public void setOriginalSourceFile(File f) {
		originalSourceFile = f;
	}

	@Override
	public float getFloat(int x, int y, int z) {
		try {
			if (pType == PixelType.FLOAT_TYPE) {
				float[] floatPixels = new float[getWidth() * getHeight()];
				copyPixelSliceIntoArray(z, ImageAccessor.DONT_CACHE_PIXELS, floatPixels);
				return floatPixels[x + y * getWidth()];
			} else if (pType == PixelType.BYTE_TYPE) {
				byte[] bytePixels = (byte[]) cachedSlices.get(z);
				// copyPixelSliceIntoArray(z, ImageAccessor.DONT_CACHE_PIXELS,bytePixels);
				return bytePixels[x + y * getWidth()] & 0xff;
			} else if (pType == PixelType.SHORT_TYPE) {
				short[] shortPixels = new short[getWidth() * getHeight()];
				copyPixelSliceIntoArray(z, ImageAccessor.DONT_CACHE_PIXELS, shortPixels);
				return shortPixels[x + y * getWidth()] & 0xffff;
			} else
				throw new RuntimeException("Unknown pixel type " + pType);
		} catch (IOException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public double getDouble(int x, int y, int z) {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	@Override
	public float getPixelValue(int x, int y, int z) {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	@Override
	public void setPixelValue(int x, int y, int z, float value) {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	@Override
	public void computePixelArray() {

	}

	@Override
	public Object getPixels(int s) {
		return getPixels(s, 1, 0);
	}

	@Override
	public float[] getPixels(int s, float a) {
		return (float[]) convertArray(getPixels(s), PixelType.FLOAT_TYPE);
	}

	@Override
	public byte[] getPixels(int s, byte a) {
		return (byte[]) convertArray(getPixels(s), PixelType.BYTE_TYPE);
	}

	@Override
	public Object getPixelsCopy(int s) {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	@Override
	public void setPixels(Object o, int s) {
		try {
			copyPixelsIntoZSlice(o, s + 1, DONT_CACHE_PIXELS);
		} catch (Exception e) {
			Utils.printStack(e);
		}
	}

	@Override
	public ImageProcessor getPixelsAsProcessor(int slice) {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	@Override
	public boolean sameDimensions(IPluginIOStack otherStack) {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	@Override
	public void clearPixels() {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	@Override
	public Object[] getStackPixelArray() {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	@Override
	public IPluginIOHyperstack getParentHyperstack() {
		return null;
	}

	@Override
	public void setStackPixelArray(Object object) {
		throw new RuntimeException("Operation not supported (at least for now)");
	}

	/**
	 * Cache policy for calls that do not explicitly specify one (e.g. calls made through the PluginIOStack
	 * interface).
	 */
	private int defaultCachePolicy = TRY_TO_CACHE_PIXELS;

	@Override
	public void setDefaultCachePolicy(int policy) {
		defaultCachePolicy = policy;
	}

	@Override
	public int getDefaultCachePolicy() {
		return defaultCachePolicy;
	}

	@Override
	public boolean isVirtual() {
		return (defaultCachePolicy == ImageAccessor.DONT_CACHE_PIXELS);
	}

	@Override
	public SliceAccessor getSlicesAccessor() throws IOException {
		if ((closed) || (!inputFileChannel.isOpen()))
			reOpen();

		SliceAccessorAdapter accessor = new SliceAccessorAdapter() {
			@Override
			final public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, float[] destinationArray)
					throws IOException {

				try {
					float[] floatPixels = (float[]) cachedSlices.get(sliceIndex);
					if (floatPixels.length != destinationArray.length)
						throw new IllegalArgumentException("Destination array length incorrect: "
								+ destinationArray.length + "instead of " + floatPixels.length);
					System.arraycopy(floatPixels, 0, destinationArray, 0, floatPixels.length);
				} catch (ExecutionException e) {
					throw new RuntimeException("Error in slice read: ", e);
				}
			}

			@Override
			final public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, short[] destinationArray)
					throws IOException {

				try {
					short[] shortPixels = (short[]) cachedSlices.get(sliceIndex);
					if (shortPixels.length != destinationArray.length)
						throw new IllegalArgumentException("Destination array length incorrect: "
								+ destinationArray.length + "instead of " + shortPixels.length);
					System.arraycopy(shortPixels, 0, destinationArray, 0, shortPixels.length);
				} catch (ExecutionException e) {
					throw new RuntimeException("Error in slice read: ", e);
				}
			}

			@Override
			final public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, byte[] destinationArray)
					throws IOException {
				try {
					byte[] bytePixels = (byte[]) cachedSlices.get(sliceIndex);
					if (bytePixels.length != destinationArray.length)
						throw new IllegalArgumentException("Destination array length incorrect: "
								+ destinationArray.length + "instead of " + bytePixels.length);
					System.arraycopy(bytePixels, 0, destinationArray, 0, bytePixels.length);
				} catch (ExecutionException e) {
					throw new RuntimeException("Error in slice read: ", e);
				}
			}

			@Override
			final public float getFloat(int x, int y, int z) throws IOException {
				try {
					if (pType == PixelType.BYTE_TYPE) {
						byte[] bytePixels;
						bytePixels = (byte[]) cachedSlices.get(z);
						return bytePixels[x + y * getWidth()] & 0xff;
					} else if (pType == PixelType.SHORT_TYPE) {
						short[] shortPixels;
						shortPixels = (short[]) cachedSlices.get(z);
						return shortPixels[x + y * getWidth()] & 0xffff;
					} else if (pType == PixelType.FLOAT_TYPE) {
						float[] floatPixels;
						floatPixels = (float[]) cachedSlices.get(z);
						return floatPixels[x + y * getWidth()];
					} else
						throw new RuntimeException("Unknown pixel type " + pType);
				} catch (ExecutionException e) {
					throw new IOException(e);
				}
			}
		};
		return accessor;
	}

	@Override
	public PixelIterator getBallIterator(int xCenter, int yCenter, int zCenter, int radius) {
		throw new RuntimeException("Ball iterator not implemented");
	}

	private static class BufferSet {
		public ByteBuffer localByteBuffer;
		public FloatBuffer localFloatBuffer;
		public ShortBuffer localShortBuffer;

		final int size;

		public BufferSet(int size) {
			this.size = size;
			localByteBuffer = ByteBuffer.allocateDirect(size);
			localFloatBuffer = localByteBuffer.asFloatBuffer();
			localShortBuffer = localByteBuffer.asShortBuffer();
		}
	}

	private class BufferPool {
		private final List<BufferSet> bufferSetList = new LinkedList<>();
		private int size = 0;

		public void setSize(int size) {
			synchronized (bufferSetList) {
				if (size == this.size)
					return;
				Utils.log("Changing buffer pool size from " + this.size + " to " + size, LogLevel.DEBUG);
				this.size = size;
				bufferSetList.clear();
				for (int i = 0; i < Runtime.getRuntime().availableProcessors(); i++) {
					bufferSetList.add(new BufferSet(size));
				}
			}
		}

		public BufferSet checkoutBuffers() {
			synchronized (bufferSetList) {
				while (bufferSetList.size() == 0) {
					try {
						bufferSetList.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
				return bufferSetList.remove(0);
			}
		}

		public void returnBuffers(BufferSet bufferSet) {
			if (bufferSet == null)
				throw new IllegalArgumentException();
			if (bufferSet.localByteBuffer == null)
				throw new IllegalArgumentException();
			if (bufferSet.localFloatBuffer == null)
				throw new IllegalArgumentException();
			if (bufferSet.localShortBuffer == null)
				throw new IllegalArgumentException();
			if (bufferSet.size != size)
				return;
			synchronized (bufferSetList) {
				bufferSetList.add(bufferSet);
				bufferSetList.notifyAll();
			}
		}

	}

	private BufferPool bufferPool = new BufferPool();

	private LoadingCache<Integer, Object> cachedSlices = CacheBuilder.newBuilder().maximumWeight(300000000)
			.softValues().expireAfterAccess(5, TimeUnit.MINUTES).weigher((k, g) -> {
				if (g instanceof float[])
					return ((float[]) g).length * 4;
				else if (g instanceof short[])
					return ((short[]) g).length * 2;
				else if (g instanceof byte[])
					return ((byte[]) g).length;
				else
					throw new RuntimeException("Unknown slice type");
			}).build(new CacheLoader<Integer, Object>() {

				@Override
				public Object load(Integer key) throws IOException {
					ByteBuffer localByteBuffer;
					FloatBuffer localFloatBuffer;
					ShortBuffer localShortBuffer;

					bufferPool.setSize(getWidth() * getHeight() * fi.getBytesPerPixel());

					BufferSet bufferSet = bufferPool.checkoutBuffers();
					localByteBuffer = bufferSet.localByteBuffer;
					localFloatBuffer = bufferSet.localFloatBuffer;
					localShortBuffer = bufferSet.localShortBuffer;

					try {

						if (!sliceCanBeRead[key])
							throw new IllegalStateException("Cannot read slice " + key
									+ " before it has been written (file was originally opened for writing");
						if ((closed) || (!inputFileChannel.isOpen()))
							reOpen();

						if (pType == PixelType.FLOAT_TYPE) {
							localByteBuffer.clear();
							int count = inputFileChannel.read(localByteBuffer, fiArray[key].offset);
							if (count == -1) {
								// The following might not be necessary
								// This was in case the input stream is not in sync with the modifications made by the
								// output stream to the file
								Utils.log("Reopening file " + f.getAbsolutePath(), LogLevel.DEBUG);
								reOpen();
								count = inputFileChannel.read(localByteBuffer, fiArray[key].offset);
							}

							if (count < getWidth() * getHeight() * 4)
								throw new IOException("Read " + count + " pixels instead of "
										+ (getWidth() * getHeight() * 4) + " expected from " + f.getAbsolutePath());

							float[] result = new float[getWidth() * getHeight()];

							localFloatBuffer.clear();
							localFloatBuffer.get(result);
							localFloatBuffer.clear();
							return result;
						} else if (pType == PixelType.SHORT_TYPE) {
							if (!sliceCanBeRead[key])
								throw new IllegalStateException("Cannot read slice " + key
										+ " before it has been written (file was originally opened for writing");
							if (closed)
								reOpen();

							localByteBuffer.clear();
							int count = inputFileChannel.read(localByteBuffer, fiArray[key].offset);
							if (count == -1) {
								// The following might not be necessary
								// This was in case the input stream is not in sync with the modifications made by the
								// output stream to the file
								Utils.log("Reopening file " + f.getAbsolutePath(), LogLevel.DEBUG);
								reOpen();
								count = inputFileChannel.read(localByteBuffer, fiArray[key].offset);
							}
							if (count < getWidth() * getHeight() * 2)
								throw new IOException("Read " + count + " pixels instead of "
										+ (getWidth() * getHeight() * 2) + " expected from " + f.getAbsolutePath());

							short[] result = new short[getWidth() * getHeight()];

							localShortBuffer.clear();
							localShortBuffer.get(result);
							localShortBuffer.clear();
							return result;
						} else if (pType == PixelType.BYTE_TYPE) {
							if (!sliceCanBeRead[key])
								throw new IllegalStateException("Cannot read slice " + key
										+ " before it has been written (file was originally opened for writing");
							if (closed || (!inputFileChannel.isOpen()))
								reOpen();

							localByteBuffer.clear();
							int count = inputFileChannel.read(localByteBuffer, fiArray[key].offset);
							if (count == -1) {
								// The following might not be necessary
								// This was in case the input stream is not in sync with the modifications made by the
								// output stream to the file
								Utils.log("Reopening file " + f.getAbsolutePath(), LogLevel.DEBUG);
								reOpen();
								count = inputFileChannel.read(localByteBuffer, fiArray[key].offset);
							}
							if (count < getWidth() * getHeight())
								throw new IOException("Read " + count + " pixels instead of "
										+ (getWidth() * getHeight()) + " expected from " + f.getAbsolutePath());

							byte[] result = new byte[getWidth() * getHeight()];

							localByteBuffer.clear();
							localByteBuffer.get(result);
							localByteBuffer.clear();

							return result;
						} else
							throw new RuntimeException("Unkown pixel type " + pType);
					} finally {
						bufferPool.returnBuffers(bufferSet);
					}
				}
			});
}
