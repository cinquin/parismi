/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import pipeline.data.InputOutputObjectDimensions.dimensionType;
import pipeline.data.tiff_read_write.TIFFFileAccessor;
import pipeline.misc_util.FormatException;

public class PluginIOStack extends PluginIOHyperstack implements IPluginIOStack {

	/**
	 * Array of slices. Each element in the array is an array of pixels; the type of the pixel array depends on the
	 * pixel type (at the moment, pixels can be short, int, or float). Modifications to this array will affect the stack
	 * (i.e. the
	 * array gives a reference to the pixels, not a copy).
	 * Using this array directly should be more efficient than making method calls to {@link #getPixels}.
	 * Before this array can be used, it must be initialized by a call to {@link #computePixelArray}.
	 */
	private transient Object[] stackPixelArray;

	private IPluginIOHyperstack parentHyperstack;

	private static final long serialVersionUID = 1L;

	/**
	 * Index of the channel to select for pixel access in the parent hyperstack structure.
	 */
	private int hyperstackChannel = -1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#setCalibration(ij.measure.Calibration)
	 */
	@Override
	public void setCalibration(Calibration calibration) {
		this.calibration = calibration;
		if (parentHyperstack != null && parentHyperstack != this && parentHyperstack.getCalibration() == null)
			parentHyperstack.setCalibration(calibration);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#setImageAcquisitionMetadata(java.lang.Object)
	 */
	@Override
	public void setImageAcquisitionMetadata(Object imageAcquisitionMetadata) {
		this.imageAcquisitionMetadata = imageAcquisitionMetadata;
	}

	public PluginIOStack(PluginIOHyperstack parent, int channelIndex, String name) {
		super();
		this.setName(name);
		setParentHyperstack(parent);
		setWidth(parent.getWidth());
		setHeight(parent.getHeight());
		pType = parent.pType;
		hyperstackChannel = channelIndex;
		setDepth(parent.getDepth());
		pixelArray = parent.pixelArray;
		nTimePoints = parent.getnTimePoints();
		setImp(parent.getImp());
		this.setImp(parent.getImp());
		calibration = parent.calibration;
		computePixelArray();
	}

	PluginIOStack(String name) {
		super(true, name);
	}

	PluginIOStack() {
		super(true, "No name");
	}

	/*
	 * public PluginIOStack(PluginIOStack taggedImage) {
	 * // TODO Auto-generated constructor stub
	 * throw new RuntimeException("Not yet implemented");
	 * }
	 */

	public PluginIOStack(String name, ImageStack tempStack, int width, int height, int depth, int nTimePoints) {
		// TODO
		super(true, name);
		setWidth(width);
		setHeight(height);
		this.setDepth(depth);

		Object slice = tempStack.getPixels(1);
		if (slice instanceof byte[])
			pType = PixelType.BYTE_TYPE;
		else if (slice instanceof short[])
			pType = PixelType.SHORT_TYPE;
		else if (slice instanceof float[])
			pType = PixelType.FLOAT_TYPE;

		setStackPixelArray(new Object[depth]);
		for (int z = 0; z < depth; z++) {
			getStackPixelArray()[z] = tempStack.getPixels(z + 1);
			// stackPixelArray[z]=stackPixelArray[z];
		}

	}

	public PluginIOStack(String name, int width, int height, int depth, int nTimePoints, PixelType pType)
			throws InterruptedException {
		super(true, name);
		setStackPixelArray(new Object[depth]);
		this.setWidth(width);
		this.setHeight(height);
		this.setDepth(depth);
		this.pType = pType;
		this.setnChannels(1);
		for (int i = 0; i < depth; i++) {
			if (Thread.interrupted())
				throw new InterruptedException();
			Object o = null;
			if (pType == PixelType.BYTE_TYPE)
				o = new byte[height * width];
			else if (pType == PixelType.SHORT_TYPE)
				o = new short[height * width];
			else if (pType == PixelType.FLOAT_TYPE)
				o = new float[height * width];
			getStackPixelArray()[i] = o;
		}
	}

	private SliceAccessor sliceAccessor;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getFloat(int, int, int)
	 */
	@Override
	public float getFloat(int x, int y, int z) {// put back double when SingleChannelView problem resolved
		Object[] localStackPixelArray = getStackPixelArray();
		if (localStackPixelArray != null) {
			if (pType == null)
				return ((float[]) localStackPixelArray[z])[y * getWidth() + x];

			if (pType == PixelType.FLOAT_TYPE)
				return ((float[]) localStackPixelArray[z])[y * getWidth() + x];
			else if (pType == PixelType.SHORT_TYPE)
				return ((short[]) localStackPixelArray[z])[y * getWidth() + x] & 0xffff;
			else if (pType == PixelType.BYTE_TYPE)
				return ((byte[]) localStackPixelArray[z])[y * getWidth() + x] & 0xff;
			else
				throw new RuntimeException("Unknown pixel type " + pType);
		} else { // we're probably dealing with a virtual stack
			try {
				if (sliceAccessor == null) {
					ImageAccessor imageAccessor = isVirtual() ? (ImageAccessor) getParentHyperstack() : null;
					sliceAccessor = imageAccessor.getSlicesAccessor();
				}
				return sliceAccessor.getFloat(x, y, z);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getDouble(int, int, int)
	 */
	@Override
	public double getDouble(int x, int y, int z) {// put back double when SingleChannelView problem resolved
		return ((float[]) getStackPixelArray()[z])[y * getWidth() + x];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getPixelValue(int, int, int)
	 */
	@Override
	public final float getPixelValue(int x, int y, int z) {
		if (getPixelType() == PixelType.FLOAT_TYPE)
			return ((float[]) getStackPixelArray()[z])[y * getWidth() + x];
		else if (getPixelType() == PixelType.SHORT_TYPE)
			return ((short[]) getStackPixelArray()[z])[y * getWidth() + x] & 0xffff;
		else if (getPixelType() == PixelType.BYTE_TYPE)
			return ((byte[]) getStackPixelArray()[z])[y * getWidth() + x] & 0xff;
		else
			throw new IllegalStateException("Unknown pixel type " + getPixelType());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#setPixelValue(int, int, int, float)
	 */
	@Override
	public final void setPixelValue(int x, int y, int z, float value) {
		if (getPixelType() == PixelType.FLOAT_TYPE)
			((float[]) getStackPixelArray()[z])[y * getWidth() + x] = value;
		else if (getPixelType() == PixelType.SHORT_TYPE)
			((short[]) getStackPixelArray()[z])[y * getWidth() + x] = (short) value;
		else if (getPixelType() == PixelType.BYTE_TYPE)
			((byte[]) getStackPixelArray()[z])[y * getWidth() + x] = (byte) value;
		else
			// unknown pixel type; assume float and possibly fail
			((float[]) getStackPixelArray()[z])[y * getWidth() + x] = value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#computePixelArray()
	 */
	@Override
	public void computePixelArray() {
		if ((getStackPixelArray() != null)
				&& (getStackPixelArray().length == getDepth())
				&& (getStackPixelArray().length > 0)
				&& (getStackPixelArray()[getStackPixelArray().length - 1] != null)
				&& (((getStackPixelArray()[0] instanceof byte[]) && pType == PixelType.BYTE_TYPE)
						|| ((getStackPixelArray()[0] instanceof float[]) && pType == PixelType.FLOAT_TYPE) || ((getStackPixelArray()[0] instanceof short[]) && pType == PixelType.SHORT_TYPE)))
			return;
		if (hyperstackChannel > -1) {// we're getting our data from a hyperstack
			Object parent = getParentHyperstack();
			if (parent instanceof TIFFFileAccessor) {
				if (((TIFFFileAccessor) parent).getDefaultCachePolicy() == ImageAccessor.DONT_CACHE_PIXELS) {
					getPixelsSlicesFromParentOnebyOne = true;
					return;// could be virtual stack; we don't want to force reading in of all the data
				}
			}
			setStackPixelArray(new Object[getDepth() * getnTimePoints()]);
			for (int i = 0; i < getDepth() * getnTimePoints(); i++) {
				getStackPixelArray()[i] = getParentHyperstack().getPixels(i, hyperstackChannel, 0);
			}
		} else {
			// there is no data no find; it must already be in our own pixel array
		}
	}

	/**
	 * If we derive from a virtual parent stack, we cannot compute a pixel array
	 */
	private boolean getPixelsSlicesFromParentOnebyOne;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getPixels(int)
	 */
	@Override
	public Object getPixels(int s) {
		if (getPixelsSlicesFromParentOnebyOne) {
			return getParentHyperstack().getPixels(s, hyperstackChannel, 0);
		}
		if (getStackPixelArray() == null)
			computePixelArray();
		return getStackPixelArray()[s];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getPixels(int, int)
	 */
	@Override
	public Object getPixels(int s, int channel, int timePoint) {
		if (getParentHyperstack() == this)
			return super.getPixels(s, channel, timePoint);
		if (getParentHyperstack() != null)
			return getParentHyperstack().getPixels(s, channel, timePoint);
		if (channel > 1)
			throw new IllegalArgumentException("Only 1 channel in a stack");
		return getPixels(s);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getPixels(int, float)
	 */
	@Override
	public float[] getPixels(int s, float a) {
		if (getStackPixelArray() == null)
			computePixelArray();
		return (float[]) getStackPixelArray()[s];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getPixels(int, byte)
	 */
	@Override
	public byte[] getPixels(int s, byte a) {
		return (byte[]) getPixels(s, 0, 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getPixelsCopy(int)
	 */
	@Override
	public Object getPixelsCopy(int s) {
		Object copy = null;
		Object slice = getStackPixelArray()[s];
		if (slice instanceof byte[])
			copy = ((byte[]) slice).clone();
		else if (slice instanceof short[])
			copy = ((short[]) slice).clone();
		else if (slice instanceof float[])
			copy = ((float[]) slice).clone();
		else
			throw new RuntimeException("Unsupported or null pixels in getPixelsCopy");
		return copy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getPixelType()
	 */
	@Override
	public PixelType getPixelType() {
		if (pType != null)
			return pType;
		if (getStackPixelArray() != null) {
			if (getStackPixelArray()[0] instanceof float[])
				pType = PixelType.FLOAT_TYPE;
			else if (getStackPixelArray()[0] instanceof byte[])
				pType = PixelType.BYTE_TYPE;
			else if (getStackPixelArray()[0] instanceof short[])
				pType = PixelType.SHORT_TYPE;
			else
				throw new RuntimeException("Pixel type of stack " + getName() + " unknown");
			return pType;
		}
		throw new RuntimeException("Pixel type of stack " + getName() + " unknown");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#setPixels(java.lang.Object, int)
	 */
	@Override
	public void setPixels(Object o, int s) {
		if (getStackPixelArray() == null)
			computePixelArray();
		getStackPixelArray()[s] = convertArray(o, pType);
		if (hyperstackChannel > -1)
			super.setPixels(o, s, hyperstackChannel, 0);
	}

	// Just added this; hope it won't interfere with supertype duplicateStructure
	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#duplicateStructure(pipeline.data.PluginIOImage.PixelType, int, int,
	 * boolean)
	 */
	@Override
	public IPluginIOImage duplicateStructure(PixelType newPixelType, int forceNSlices, int forceNChannels,
			boolean dontAllocatePixels) {
		PluginIOStack duplicate = new PluginIOStack();
		copyIntoLeavePixelsNull(duplicate);
		duplicate.hyperstackChannel = -1;
		duplicate.setParentHyperstack(null);
		duplicate.calibration = calibration;
		duplicate.nTimePoints = nTimePoints;
		duplicate.setImageAcquisitionMetadata(getImageAcquisitionMetadata());
		if (newPixelType == null)
			newPixelType = getPixelType();
		duplicate.pType = newPixelType;
		if (forceNSlices > -1)
			duplicate.setDepth(forceNSlices);
		if (forceNChannels > -1) {
			if (forceNChannels > 0)
				throw new IllegalArgumentException("forceNChannels>0 not implemented");
			duplicate.setnChannels(0);
			// Tell this stack to access its pixels from its own hyperstack stucture,
			// because it is likely the user will call addChannel on this viewed as a hyperstack
			duplicate.hyperstackChannel = 0;
			duplicate.setParentHyperstack(duplicate);
			return duplicate;
		} else
			duplicate.setStackPixelArray(new Object[duplicate.getDepth()]);

		for (int z = 0; z < duplicate.getDepth(); z++) {
			if (dontAllocatePixels && ((z > 0)))
				// for now, if we're asked not to allocate pixels we do allocate one new pixel array
				// and use it over and over again while filling the pixelArray. That way if an ImagePlus
				// is created it won't crash when trying to access its pixels.
				duplicate.getStackPixelArray()[z] = duplicate.getStackPixelArray()[0];
			else {
				if (newPixelType == PixelType.BYTE_TYPE)
					duplicate.getStackPixelArray()[z] = new byte[getWidth() * getHeight()];
				else if (newPixelType == PixelType.FLOAT_TYPE)
					duplicate.getStackPixelArray()[z] = new float[getWidth() * getHeight()];
				else if (newPixelType == PixelType.SHORT_TYPE)
					duplicate.getStackPixelArray()[z] = new short[getWidth() * getHeight()];
			}
		}
		return duplicate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#asFile(java.io.File)
	 */
	@Override
	public File asFile(File saveTo, boolean useBigTIFF) throws IOException, InterruptedException {
		computePixelArray();
		File f = saveTo != null ? saveTo : File.createTempFile("StackSaveToOutput:", ".tiff");
		TIFFFileAccessor testTIFF = new TIFFFileAccessor(f, getName(), pType, calibration, useBigTIFF);
		if (getImageAcquisitionMetadata() instanceof ChannelInfo)
			testTIFF.setImageAcquisitionMetadata(new ChannelInfo[] { (ChannelInfo) getImageAcquisitionMetadata() });
		else
			testTIFF.setImageAcquisitionMetadata(getImageAcquisitionMetadata());
		testTIFF.setDimensions(getWidth(), getHeight(), getDepth(), 1, 1);
		testTIFF.openForSequentialWrite();
		try {
			for (int i = 0; i < getDepth(); i++) {
				testTIFF.copyPixelsIntoZSlice(getStackPixelArray()[i], i, 0);
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
	 * @see pipeline.data.PluginIOStackInterface#asPixelArray()
	 */
	@Override
	public Object asPixelArray() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pipeline.data.PluginIOStackInterface#getDimensionLabels(pipeline.data.InputOutputObjectDimensions.dimensionType)
	 */
	@Override
	public String[] getDimensionLabels(dimensionType dim) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getDimensions()
	 */
	@Override
	public InputOutputObjectDimensions getDimensions() {
		return new InputOutputObjectDimensions(getWidth(), getHeight(), getDepth(), 1, nTimePoints);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getPixelsAsProcessor(int)
	 */
	@Override
	public ImageProcessor getPixelsAsProcessor(int slice) {
		Object[] pixelArray = getStackPixelArray();
		if (pixelArray == null) {
			computePixelArray();
			pixelArray = getStackPixelArray();
		}
		Object pixels = pixelArray[slice];
		if (pixels instanceof byte[])
			return new ByteProcessor(getWidth(), getHeight(), (byte[]) pixels, null);
		else if (pixels instanceof short[])
			return new ShortProcessor(getWidth(), getHeight(), (short[]) pixels, null);
		else if (pixels instanceof float[])
			return new FloatProcessor(getWidth(), getHeight(), (float[]) pixels, null);
		throw new RuntimeException("Unknown pixel type");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#convertTo(pipeline.data.PluginIOImage.PixelType)
	 */
	@Override
	public void convertTo(PixelType newPixelType) {
		// super.convertTo(newPixelType);
		computePixelArray();
		Object[] pixels = getStackPixelArray();
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = convertArray(pixels[i], newPixelType);
		}
		pType = newPixelType;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#sameDimensions(pipeline.data.PluginIOStack)
	 */
	@Override
	public boolean sameDimensions(IPluginIOStack otherStack) {
		if (otherStack == null)
			return false;
		return (otherStack.getWidth() == getWidth()) && (otherStack.getHeight() == getHeight())
				&& (otherStack.getDepth() == getDepth());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#clearPixels()
	 */
	@Override
	public void clearPixels() {
		if ((getStackPixelArray() == null) || getStackPixelArray()[0] == null)
			computePixelArray();
		for (int i = 0; i < getDepth(); i++) {
			Object pixels = getStackPixelArray()[i];
			if (pixels instanceof byte[])
				Arrays.fill((byte[]) pixels, (byte) 0);
			else if (pixels instanceof short[])
				Arrays.fill((short[]) pixels, (short) 0);
			else if (pixels instanceof float[])
				Arrays.fill((float[]) pixels, (float) 0);
		}
	}

	void setStackPixelArray(Object[] stackPixelArray) {
		this.stackPixelArray = stackPixelArray;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getStackPixelArray()
	 */
	@Override
	public final Object[] getStackPixelArray() {
		return stackPixelArray;
	}

	private void setParentHyperstack(IPluginIOHyperstack parentHyperstack) {
		this.parentHyperstack = parentHyperstack;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOStackInterface#getParentHyperstack()
	 */
	@Override
	public IPluginIOHyperstack getParentHyperstack() {
		return parentHyperstack;
	}

	@Override
	public void setStackPixelArray(Object object) {
		stackPixelArray = (Object[]) object;
	}

	@Override
	public boolean isVirtual() {
		Object parent = getParentHyperstack();
		if (parent instanceof TIFFFileAccessor) {
			if (((TIFFFileAccessor) parent).getDefaultCachePolicy() == ImageAccessor.DONT_CACHE_PIXELS) {
				return true;
			}
		}
		return false;
	}

	@Override
	public PixelIterator getBallIterator(final int xCenter, final int yCenter, final int zCenter, final int radius) {
		return new PixelIterator() {

			int z0 = Math.min(zCenter, radius);
			int z1 = Math.min(getDepth() - 1 - zCenter, radius);

			int y0 = Math.min(yCenter, radius);
			int y1 = Math.min(getHeight() - 1 - (yCenter), radius);

			int x0 = Math.min(xCenter, radius);
			int x1 = Math.min(getWidth() - 1 - xCenter, radius);

			int x = -x0, y = -y0, z = -z0;

			private boolean nextAvailable = (x1 >= x0) && (y1 >= y0) && (z1 >= z0);// at least the center should be
																					// included
			private float[] zSliceArray = (float[]) getStackPixelArray()[z + zCenter];

			private int width = getWidth();

			private float zFactor = (float) (getCalibration() != null ? 1f : getCalibration().pixelDepth
					/ getCalibration().pixelWidth);
			private float zFactorSq = zFactor * zFactor;

			@Override
			public final boolean hasNext() {
				return nextAvailable;
			}

			float cachedNext = 0;

			int radiusSq = radius * radius;

			{
				if (nextAvailable)
					cacheNext();
			}

			private void cacheNext() {
				x++;
				if (x > x1) {
					x = -x0;
					y++;
					if (y > y1) {
						y = -y0;
						z++;
						if (z > z1) {
							nextAvailable = false;
							return;
						}
						zSliceArray = (float[]) getStackPixelArray()[z + zCenter];
					}
				}

				if (x * x + y * y + z * z * zFactorSq > radiusSq)
					cacheNext();
				else {
					// cachedNext=getPixelValue(x+xCenter, y+yCenter,1+ z+zCenter);
					cachedNext = zSliceArray[(y + yCenter) * width + (x + xCenter)];
				}

			}

			@Override
			public final float nextFloatValue() {
				if (!nextAvailable)
					throw new IllegalStateException("No values to iterate");
				float returnValue = cachedNext;
				cacheNext();
				return returnValue;
			}

		};
	}

	@Override
	public ImageAccessor getImageAccessor() {
		if (this instanceof ImageAccessor)
			return (ImageAccessor) this;
		else
			return getParentHyperstack().getImageAccessor();
	}
}
