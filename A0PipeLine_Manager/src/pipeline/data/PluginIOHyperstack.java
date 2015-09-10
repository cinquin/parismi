/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNull;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.data.InputOutputObjectDimensions.dimensionType;
import pipeline.data.tiff_read_write.TIFFFileAccessor;
import pipeline.external_plugin_interfaces.JNACallToNativeLibrary;
import pipeline.misc_util.FormatException;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public class PluginIOHyperstack extends PluginIOImage implements IPluginIO, IPluginIOImage, IPluginIOHyperstack {

	private int depth;

	private int width;

	private int height;

	private int nChannels;

	protected int nTimePoints = 1;

	/**
	 * True if this image can be written to by manipulating its pixel array. This is not necessarily the case,
	 * for example for files that reside on disk. Used for example by {@link JNACallToNativeLibrary}.
	 */
	private boolean supportsWritingToPixels;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getChannels(java.lang.String[])
	 */
	@Override
	public IPluginIOStack[] getChannels(String[] channelNames) {
		IPluginIOStack[] result = new IPluginIOStack[channelNames.length];
		for (int i = 0; i < channelNames.length; i++) {
			result[i] = channels.get(channelNames[i]);
			// if ((result[i].pType==PixelType.FLOAT_TYPE)&&(!(result[i].stackPixelArray[0] instanceof float[]))){
			// throw new RuntimeException("Channel pixels are the wrong type");
			// }
			if (result[i] == null) {
				Utils.log("Could not find channel " + channelNames[i] + " in hyperstack " + getName()
						+ "; choices are " + Utils.printStringArray(channels.keySet().toArray(new String[] {}))
						+ "; returning random channel", LogLevel.DEBUG);
				result[i] = channels.values().iterator().next();
			}
		}
		return result;
	}

	PluginIOHyperstack(boolean justOneChannel, @NonNull String name) {
		setSupportsWritingToPixels(true);
		setChannels(new TreeMap<String, IPluginIOStack>());
		this.setName(name);
		if (justOneChannel)
			getChannels().put("Ch0", (PluginIOStack) this);// this is probably wrong; cast should fail
	}

	public PluginIOHyperstack(ImagePlus imp) {
		setSupportsWritingToPixels(true);
		setChannels(new TreeMap<String, IPluginIOStack>());
		setnChannels(imp.getNChannels());
		String impTitle = imp.getTitle();
		Objects.requireNonNull(impTitle);
		setName(impTitle);
		this.setImagePlusDisplay(imp);
		calibration = imp.getCalibration();
		setImageAcquisitionMetadata(imp.getProperty("Info"));

		try {
			Object pixels = imp.getStack().getPixels(1);
			if (pixels instanceof byte[])
				pType = PixelType.BYTE_TYPE;
			else if (pixels instanceof short[])
				pType = PixelType.SHORT_TYPE;
			else if (pixels instanceof float[])
				pType = PixelType.FLOAT_TYPE;
			else
				Utils.log("Unknown pixel type " + pixels, LogLevel.WARNING);
		} catch (Exception e) {
			Utils.printStack(e);
		}

		// TODO add support for 5D stacks

		try {
			FileInfo fi = imp.getOriginalFileInfo();
			if (fi != null) {
				setDerivation(fi.directory + Utils.fileNameSeparator + fi.fileName);
				if (getDerivation() == null) {
					setDerivation("");
				}
				if (getDerivation().equals("")) {
					setDerivation(fi.url);
					if (getDerivation() == null)
						setDerivation("");
				}
			}
		} catch (Exception e) {
			Utils.printStack(e);
		}
		if ("".equals(getDerivation()))
			setDerivation("Could not resolve image source");

		PluginIOHyperstackViewWithImagePlus impMtd = new PluginIOHyperstackViewWithImagePlus(getName(), imp);
		this.setImp(impMtd);

		for (int i = 0; i < getnChannels(); i++) {
			// Give the single channel a name that identifies them in the hyperstack
			// because some plugins (such as Plot3D in create destination) need that information.
			// Maybe it would be better to set the parent hyperstack
			SingleChannelView scv = new SingleChannelView(imp, i + 1, "Ch" + i);
			scv.pType = pType;
			scv.setImp(impMtd);
			getChannels().put("Ch" + i, scv);
			if (i == 0) {
				setDepth(scv.getDepth());
				setWidth(scv.getWidth());
				setHeight(scv.getHeight());
				setnTimePoints(scv.getnTimePoints());
				pixelArray = new Object[getnChannels()][getDepth() * getnTimePoints()];
			}
			for (int z = 0; z < getDepth(); z++) {
				pixelArray[i][z] = scv.getPixels(z);
			}
		}

	}

	public PluginIOHyperstack(@NonNull String name, int width, int height, int depth, int nChannels, int nTimePoints,
			PixelType pType, boolean dontAllocatePixels) throws InterruptedException {
		setSupportsWritingToPixels(true);
		setChannels(new TreeMap<String, IPluginIOStack>());
		this.setnChannels(nChannels);
		this.setName(name);
		this.setWidth(width);
		this.setHeight(height);
		this.setDepth(depth);
		this.setnTimePoints(nTimePoints);
		this.pType = pType;

		// TODO add support for 5D stacks

		setDerivation("De novo hyperstack creation");

		pixelArray = new Object[nChannels][depth * nTimePoints];
		setChannels(new TreeMap<String, IPluginIOStack>());

		for (int c = 0; c < nChannels; c++) {
			for (int z = 0; z < depth * nTimePoints; z++) {
				if (dontAllocatePixels && ((z > 0) || (c > 0)))
					// for now, if we're asked not to allocate pixels we do allocate one new pixel array
					// and use it over and over again while filling the pixelArray. That way if an ImagePlus
					// is created it won't crash when trying to acces its pixels
					pixelArray[c][z] = pixelArray[0][0];
				else {
					if (Thread.interrupted())
						throw new InterruptedException();
					if (pType == PixelType.BYTE_TYPE)
						pixelArray[c][z] = new byte[width * height];
					else if (pType == null || (pType == PixelType.FLOAT_TYPE))
						pixelArray[c][z] = new float[width * height];
					else if (pType == PixelType.SHORT_TYPE)
						pixelArray[c][z] = new short[width * height];
					else
						throw new RuntimeException("Unkown pixel type " + pType);
				}
			}
			getChannels().put("Ch" + c, new PluginIOStack(this, c, "Ch" + c));

		}
	}

	protected PluginIOHyperstack() {
		setSupportsWritingToPixels(true);
	}

	private SortedMap<String, IPluginIOStack> channels;

	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#asPixelArray()
	 */
	@Override
	public Object asPixelArray() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#canConvertTo()
	 */
	@Override
	public PixelType[] canConvertTo() {
		return new PixelType[] { PixelType.BYTE_TYPE, PixelType.SHORT_TYPE, PixelType.FLOAT_TYPE };
	}

	protected static Object convertArray(Object originalArray, PixelType destinationType) {
		if (destinationType == null)
			return originalArray;
		Object o;
		PixelType originalpType = null;
		int length;

		if (originalArray instanceof byte[])
			originalpType = PixelType.BYTE_TYPE;
		else if (originalArray instanceof short[])
			originalpType = PixelType.SHORT_TYPE;
		else if (originalArray instanceof float[])
			originalpType = PixelType.FLOAT_TYPE;

		if (originalpType == destinationType)
			return originalArray;

		if (originalpType == PixelType.BYTE_TYPE) {
			length = ((byte[]) originalArray).length;
			byte[] originalBytes = (byte[]) originalArray;
			if (destinationType == PixelType.FLOAT_TYPE) {
				o = new float[length];
				float[] oFloat = (float[]) o;
				for (int p = 0; p < length; p++)
					oFloat[p] = (originalBytes[p] & 0xff);
			} else if (destinationType == PixelType.SHORT_TYPE) {
				o = new short[length];
				short[] oShort = (short[]) o;
				for (int p = 0; p < length; p++)
					oShort[p] = (short) (originalBytes[p] & 0xff);
			} else
				throw new RuntimeException("Nothing to convert");
		}

		else if (originalpType == PixelType.SHORT_TYPE) {

			length = ((short[]) originalArray).length;
			short[] originalShort = (short[]) originalArray;
			if (destinationType == PixelType.FLOAT_TYPE) {
				o = new float[length];
				float[] oFloat = (float[]) o;
				for (int p = 0; p < length; p++) {
					oFloat[p] = (originalShort[p] & 0xffff);
				}
			} else if (destinationType == PixelType.BYTE_TYPE) {
				o = new byte[length];
				byte[] oBytes = (byte[]) o;
				for (int p = 0; p < length; p++) {
					oBytes[p] = (byte) (originalShort[p] & 0xffff);
				}
			} else
				throw new RuntimeException("Nothing to convert");
		} else if (originalpType == PixelType.FLOAT_TYPE) {

			length = ((float[]) originalArray).length;
			float[] originalFloat = (float[]) originalArray;
			if (destinationType == PixelType.BYTE_TYPE) {
				o = new byte[length];
				byte[] oBytes = (byte[]) o;
				for (int p = 0; p < length; p++) {
					oBytes[p] = (byte) originalFloat[p];
				}
			} else if (destinationType == PixelType.SHORT_TYPE) {
				o = new short[length];
				short[] oShort = (short[]) o;
				for (int p = 0; p < length; p++) {
					oShort[p] = (short) originalFloat[p];
				}
			} else
				throw new RuntimeException("Nothing to convert");
		} else
			throw new RuntimeException("Unsupported pixel type");
		// if (!(o instanceof float[])){
		// throw new RuntimeException("o not float array");
		// }
		return o;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#convertTo(pipeline.data.PluginIOImage.PixelType)
	 */
	@Override
	public void convertTo(PixelType newPixelType) {
		// TODO We should erase all references to each old array just after it is converted
		// so it can be garbage collected. The peak memory usage of the current code is twice
		// the size of the image, which is much more than necessary.
		for (int c = 0; c < getnChannels(); c++) {
			for (int i = 0; i < getDepth() * getnTimePoints(); i++) {
				pixelArray[c][i] = convertArray(pixelArray[c][i], newPixelType);
			}
		}

		Object[] channelArray = getChannels().values().toArray();

		for (int i = 0; i < channelArray.length; i++) {
			((PluginIOStack) channelArray[i]).pType = newPixelType;
			if (channelArray[i] instanceof SingleChannelView) {
				// ((SingleChannelView) channelArray[i]).computePixelArray(newPixelType)
				SingleChannelView scv = (SingleChannelView) channelArray[i];
				if (scv.getStackPixelArray() == null)
					scv.setStackPixelArray(new Object[getDepth() * getnTimePoints()]);
				for (int z = 0; z < getDepth() * getnTimePoints(); z++) {
					scv.getStackPixelArray()[z] = pixelArray[i][z];
				}
			} else
				((IPluginIOStack) channelArray[i]).computePixelArray();

		}

		pType = newPixelType;
		/*
		 * if (!(pixelArray[0][0] instanceof float [])){
		 * Utils.log("warning",LogLevel.WARNING);
		 * }
		 */
	}

	protected transient Calibration calibration;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getCalibration()
	 */
	@Override
	public Calibration getCalibration() {
		return calibration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pipeline.data.PluginIOHyperstackInterface#getDimensionLabels(pipeline.data.InputOutputObjectDimensions.dimensionType
	 * )
	 */
	@Override
	public String[] getDimensionLabels(dimensionType dim) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getDimensions()
	 */
	@Override
	public InputOutputObjectDimensions getDimensions() {
		return new InputOutputObjectDimensions(getWidth(), getHeight(), getDepth(), getnChannels(), getnTimePoints());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getPixelType()
	 */
	@Override
	public PixelType getPixelType() {
		if (pType != null)
			return pType;
		if (pixelArray != null) {
			if (pixelArray[0][0] instanceof float[])
				pType = PixelType.FLOAT_TYPE;
			else if (pixelArray[0][0] instanceof byte[])
				pType = PixelType.BYTE_TYPE;
			else if (pixelArray[0][0] instanceof short[])
				pType = PixelType.SHORT_TYPE;
			else
				throw new RuntimeException("Pixel type of hyperstack " + getName() + " unknown");
			return pType;
		}
		throw new RuntimeException("Pixel type of hyperstack " + getName() + " unknown");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#asFile(java.io.File)
	 */
	@Override
	public File asFile(File saveTo, boolean useBigTIFF) throws IOException, InterruptedException {
		for (IPluginIOStack channel : getChannels().values()) {
			channel.computePixelArray();
		}
		@SuppressWarnings("null")
		@NonNull File f = saveTo != null ? saveTo : File.createTempFile("HyperstackSaveToOutput:", ".tiff");
		TIFFFileAccessor testTIFF = new TIFFFileAccessor(f, getName(), pType, calibration, useBigTIFF);
		testTIFF.setImageAcquisitionMetadata(getImageAcquisitionMetadata());
		testTIFF.setDimensions(getWidth(), getHeight(), getDepth(), getnChannels(), getnTimePoints());
		// setDimensions already opens for writing testTIFF.openForSequentialWrite();
		try {
			int sliceIndex = 0;
			for (int i = 0; i < getDepth() * getnTimePoints(); i++) {
				for (IPluginIOStack channel : getChannels().values()) {
					testTIFF.copyPixelsIntoZSlice(channel.getStackPixelArray()[i], sliceIndex, 0);
					sliceIndex++;
				}
			}
		} catch (FormatException e) {
			throw new RuntimeException("Format problem while writing tiff", e);
		}
		testTIFF.close();
		return f;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#listOfSubObjects()
	 */
	@SuppressWarnings("null")
	@Override
	public @NonNull String @NonNull[] listOfSubObjects() {
		if (getChannels() == null) {
			if (this instanceof IPluginIOStack)
				return new @NonNull String[] { "Only channel" };
			return new @NonNull String[] {};
		}
		return getChannels().keySet().toArray(new String[] {});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#copyIntoLeavePixelsNull(pipeline.data.PluginIOHyperstack)
	 */
	@Override
	public void copyIntoLeavePixelsNull(PluginIOHyperstack destination) {
		super.copyInto(destination);
		destination.setWidth(width);
		destination.setHeight(height);
		destination.setDepth(depth);
		destination.setnChannels(nChannels);
		destination.setnTimePoints(nTimePoints);
		destination.calibration = calibration;
		destination.setImageAcquisitionMetadata(getImageAcquisitionMetadata());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#duplicateStructure(pipeline.data.PluginIOImage.PixelType, int,
	 * int, boolean)
	 */
	@Override
	public IPluginIOImage duplicateStructure(PixelType newPixelType, int forceNSlices, int forceNChannels,
			boolean dontAllocatePixels) {
		PluginIOHyperstack duplicate = new PluginIOHyperstack();
		copyIntoLeavePixelsNull(duplicate);
		if (forceNSlices > -1)
			duplicate.setDepth(forceNSlices);
		duplicate.pixelArray = new Object[getnChannels()][duplicate.getDepth() * getnTimePoints()];
		duplicate.setChannels(new TreeMap<String, IPluginIOStack>());
		duplicate.setnTimePoints(getnTimePoints());
		duplicate.setName("Derived from " + getName());
		if (forceNChannels >= 0)
			duplicate.setnChannels(forceNChannels);
		duplicate.pType = newPixelType;
		duplicate.setCalibration(getCalibration());
		duplicate.setImageAcquisitionMetadata(getImageAcquisitionMetadata());

		for (int c = 0; c < duplicate.getnChannels(); c++) {
			for (int z = 0; z < duplicate.getDepth() * getnTimePoints(); z++) {
				if (dontAllocatePixels && ((z > 0) || (c > 0)))
					// For now, if we're asked not to allocate pixels we do allocate one new pixel array
					// and use it over and over again while filling the pixelArray. That way if an ImagePlus
					// is created it won't crash when trying to acces its pixels
					duplicate.pixelArray[c][z] = duplicate.pixelArray[0][0];
				else {
					if (newPixelType == PixelType.BYTE_TYPE)
						duplicate.pixelArray[c][z] = new byte[getWidth() * getHeight()];
					else if (newPixelType == PixelType.FLOAT_TYPE)
						duplicate.pixelArray[c][z] = new float[getWidth() * getHeight()];
					else if (newPixelType == PixelType.SHORT_TYPE)
						duplicate.pixelArray[c][z] = new short[getWidth() * getHeight()];
				}
			}
			PluginIOStack stack = new PluginIOStack(duplicate, c, "Ch" + c);
			stack.setImageAcquisitionMetadata(getChannels().get("Ch" + c).getImageAcquisitionMetadata());
			duplicate.getChannels().put("Ch" + c, stack);
		}
		return duplicate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#addChannel(java.lang.String)
	 */
	@Override
	public IPluginIOStack addChannel(String name) {
		PixelType localpType = getPixelType();

		int newChannelIndex = getnChannels();

		if (name == null)
			name = "Ch " + newChannelIndex;

		Object[][] newPixelArray = new Object[getnChannels() + 1][];
		for (int c = 0; c < getnChannels(); c++) {
			newPixelArray[c] = pixelArray[c];
		}
		pixelArray = newPixelArray;

		pixelArray[newChannelIndex] = new Object[getDepth() * getnTimePoints()];
		for (int z = 0; z < getDepth() * getnTimePoints(); z++) {
			if (localpType == PixelType.BYTE_TYPE)
				pixelArray[newChannelIndex][z] = new byte[getWidth() * getHeight()];
			else if (localpType == PixelType.FLOAT_TYPE)
				pixelArray[newChannelIndex][z] = new float[getWidth() * getHeight()];
			else if (localpType == PixelType.SHORT_TYPE)
				pixelArray[newChannelIndex][z] = new short[getWidth() * getHeight()];
		}

		setnChannels(getnChannels() + 1);
		PluginIOStack newChannel = new PluginIOStack(this, newChannelIndex, name);

		getChannels().put(name, newChannel);
		return newChannel;
	}

	/**
	 * For fast access to pixels, store a channel-array of z-arrays of xy pixel values
	 */
	transient Object[][] pixelArray = null;

	private transient ImagePlus imagePlusDisplay;// temporary fix for plugins that need access to that; this should be
													// removed when we have

	// a way of specifying GUI objects that are cleanly separated from the data

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getFloat(int, int, int, int)
	 */
	@Override
	public final float getFloat(int x, int y, int z, int channel, int timePoint) {
		return ((float[]) getPixels(z, channel, timePoint))[y * getWidth() + x];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getDouble(int, int, int, int)
	 */
	@Override
	public final double getDouble(int x, int y, int z, int channel, int timePoint) {
		return ((float[]) getPixels(z, channel, timePoint))[y * getWidth() + x];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#multiply(int, int, float)
	 */
	@Override
	public void multiply(int z, int c, float v) {
		if (getPixelType() == PixelType.FLOAT_TYPE) {
			float[] a = (float[]) getPixels(z - 1, c - 1, 0);
			for (int i = 0; i < a.length; i++) {
				a[i] *= v;
			}
		} else if (getPixelType() == PixelType.BYTE_TYPE) {
			byte[] a = (byte[]) getPixels(z - 1, c - 1, 0);
			for (int i = 0; i < a.length; i++) {
				a[i] *= v;
			}
		} else if (getPixelType() == PixelType.SHORT_TYPE) {
			short[] a = (short[]) getPixels(z - 1, c - 1, 0);
			for (int i = 0; i < a.length; i++) {
				a[i] *= v;
			}
		} else
			throw new RuntimeException("Unsupported pixel type");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getPixels(int, int)
	 */
	@Override
	public Object getPixels(int z, int channel, int timePoint) {
		if (pixelArray == null) {
			if (this instanceof IPluginIOStack) {
				return ((IPluginIOStack) this).getPixels(z + timePoint * getDepth());
			} else
				return null;
		}
		return pixelArray[channel][z + timePoint * getDepth()];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getPixelValue(int, int, int, int)
	 */
	@Override
	public final float getPixelValue(int x, int y, int z, int channel, int timePoint) {
		if (getPixelType() == PixelType.FLOAT_TYPE) {
			return (((float[]) getPixels(z, channel, timePoint))[y * getWidth() + x]);
		}
		if (getPixelType() == PixelType.BYTE_TYPE) {
			return (((byte[]) getPixels(z, channel, timePoint))[y * getWidth() + x]) & 0xff;
		}
		if (getPixelType() == PixelType.SHORT_TYPE) {
			return (((short[]) getPixels(z, channel, timePoint))[y * getWidth() + x]) & 0xffff;
		} else
			throw new RuntimeException("Unsupported pixel type");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setPixelValue(int, int, int, float, int)
	 */
	@Override
	public void setPixelValue(int x, int y, int z, float value, int channel, int timePoint) {
		if (getPixelType() == PixelType.FLOAT_TYPE) {
			((float[]) getPixels(z, channel, timePoint))[y * getWidth() + x] = value;
		} else if (getPixelType() == PixelType.BYTE_TYPE) {
			((byte[]) getPixels(z, channel, timePoint))[y * getWidth() + x] = (byte) value;
		} else if (getPixelType() == PixelType.SHORT_TYPE) {
			((short[]) getPixels(z, channel, timePoint))[y * getWidth() + x] = (short) value;
		} else
			throw new RuntimeException("Unsupported pixel type");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setPixels(java.lang.Object, int, int)
	 */
	@Override
	public void setPixels(Object o, int s, int channel, int timePoint) {
		pixelArray[channel][s + timePoint * getDepth()] = convertArray(o, pType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getRoi()
	 */
	@Override
	public Roi getRoi() {
		if (getImagePlusDisplay() != null)
			return getImagePlusDisplay().getRoi();
		else
			return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getType()
	 */
	@Override
	public int getType() {
		// lookup imp getType()
		// TODO Auto-generated method stub
		throw new RuntimeException("Not yet implemented");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setCalibration(ij.measure.Calibration)
	 */
	@Override
	public void setCalibration(Calibration calibration) {
		this.calibration = calibration;
		if (getChannels() != null) {
			getChannels().values().stream().filter(channel -> channel != this).forEach(
					channel -> channel.setCalibration(calibration));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setImageAcquisitionMetadata(java.lang.Object)
	 */
	@Override
	public void setImageAcquisitionMetadata(Object imageAcquisitionMetadata) {
		this.imageAcquisitionMetadata = imageAcquisitionMetadata;
		if (getChannels() != null) {
			for (int i = 0; i < getnChannels(); i++) {
				// for (PluginIOStackInterface channel: getChannels().values()){
				IPluginIOStack channel = getChannels().get("Ch" + i);
				if ((channel != this) && (channel != null) && (imageAcquisitionMetadata instanceof ChannelInfo[])
						&& (((ChannelInfo[]) imageAcquisitionMetadata).length > 0)) {
					channel.setImageAcquisitionMetadata(((ChannelInfo[]) imageAcquisitionMetadata)[i]);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setDepth(int)
	 */
	@Override
	public void setDepth(int depth) {
		this.depth = depth;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getDepth()
	 */
	@Override
	public int getDepth() {
		return depth;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setWidth(int)
	 */
	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getWidth()
	 */
	@Override
	public final int getWidth() {
		return width;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setHeight(int)
	 */
	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getHeight()
	 */
	@Override
	public int getHeight() {
		return height;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setnChannels(int)
	 */
	@Override
	public void setnChannels(int nChannels) {
		this.nChannels = nChannels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getnChannels()
	 */
	@Override
	public int getnChannels() {
		return nChannels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setSupportsWritingToPixels(boolean)
	 */
	@Override
	public void setSupportsWritingToPixels(boolean supportsWritingToPixels) {
		this.supportsWritingToPixels = supportsWritingToPixels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#isSupportsWritingToPixels()
	 */
	@Override
	public boolean isSupportsWritingToPixels() {
		return supportsWritingToPixels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setChannels(java.util.HashMap)
	 */
	@Override
	public void setChannels(SortedMap<String, IPluginIOStack> channels) {
		this.channels = channels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getChannels()
	 */
	@Override
	public SortedMap<String, IPluginIOStack> getChannels() {
		if (channels == null)
			channels = new TreeMap<>();
		return channels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#setImagePlusDisplay(ij.ImagePlus)
	 */
	@Override
	public void setImagePlusDisplay(ImagePlus imagePlusDisplay) {
		this.imagePlusDisplay = imagePlusDisplay;
		if (channels != null)
			channels.values().stream().filter(channel -> channel != this).forEach(
					channel -> channel.setImagePlusDisplay(imagePlusDisplay));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOHyperstackInterface#getImagePlusDisplay()
	 */
	@Override
	public ImagePlus getImagePlusDisplay() {
		return imagePlusDisplay;
	}

	@Override
	public boolean isVirtual() {
		return false;
	}

	@Override
	public void setnTimePoints(int nTimePoints) {
		this.nTimePoints = nTimePoints;
	}

	@Override
	public int getnTimePoints() {
		return nTimePoints;
	}

	@Override
	public void linkToList(IPluginIOList<?> list) {
	}

	@SuppressWarnings("null")
	@Override
	public List<Float> getQuantifiedProperties() {
		//Not implemented
		return Collections.emptyList();
	}

	@Override
	public void setQuantifiedProperties(List<Float> qp) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public float getQuantifiedProperty(String name) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public boolean setQuantifiedProperty(String name, float value) {
		throw new RuntimeException("Not implemented");
	}

	@SuppressWarnings("null")
	@Override
	public List<String> getQuantifiedPropertyNames() {
		//Not implemented
		return Collections.emptyList();
	}

	@Override
	public boolean hasQuantifiedProperty(String name) {
		return false;
	}

	@Override
	public boolean addQuantifiedPropertyName(String name) {
		throw new RuntimeException("Not implemented");

	}

	@Override
	public void setQuantifiedPropertyNames(List<String> desc) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public ImageAccessor getImageAccessor() {
		if (imageAccessor != null)
			return imageAccessor;
		else
			return (ImageAccessor) this;
	}

	private ImageAccessor imageAccessor = null;

	@Override
	public void setImageAccessor(ImageAccessor imageAccessor) {
		this.imageAccessor = imageAccessor;
	}
}
