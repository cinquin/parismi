/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import ij.ImagePlus;
import ij.ImageStack;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.SettableBoolean;
import pipeline.misc_util.Utils;

// TODO NUMBER OF CHANNELS NOT IMPLEMENTED
public class ImagePlusImageAccessor implements ImageAccessor {
	private ImageStack stack;
	private int width;
	private int height;
	private int depth;
	private int time;

	@Override
	protected void finalize() {
		try {
			close();
		} finally {
			try {
				super.finalize();
			} catch (Throwable e) {
				Utils.printStack(e);
			}
		}
	}

	public ImagePlusImageAccessor(ImagePlus imp) {
		// TODO We need a special implementation for virtual stacks
		// TODO The current code might not handle the case where the ImagePlus is not a stack
		this.stack = imp.getStack();
		this.width = imp.getWidth();
		this.height = imp.getHeight();
		this.depth = stack.getSize();
		this.time = 1;
	}

	@Override
	public void close() {
		// Nothing to do (or maybe save the image to disk if it has a path name associated with it??)
	}

	@Override
	public void openForSequentialRead() {
		// Nothing to do
	}

	@Override
	public void openForSequentialWrite() {
		// Nothing to do
	}

	@Override
	public void assignPixelsToZSlice(Object pixels, int sliceIndex, int cachePolicy, SettableBoolean justMadeACopy) {
		if (!(pixels instanceof float[]))
			throw new RuntimeException("Non-float stacks not implemented yet in ImagePlusImageAccessor");
		stack.getImageArray()[sliceIndex] = pixels;
		justMadeACopy.value = false;
	}

	@Override
	public void copyPixelsIntoZSlice(Object pixels, int sliceIndex, int cachePolicy) {
		if (!(pixels instanceof float[]))
			throw new RuntimeException("Non-float stacks not implemented yet in ImagePlusImageAccessor");
		System.arraycopy(pixels, 0, stack.getImageArray()[sliceIndex], 0, ((float[]) pixels).length);
	}

	@Override
	public Object getPixelZSliceCopy(int sliceIndex, int cachePolicy) {
		Object array = stack.getImageArray()[sliceIndex];
		if (array instanceof float[])
			return ((float[]) array).clone();
		else if (array instanceof short[])
			return ((short[]) array).clone();
		else if (array instanceof byte[])
			return ((byte[]) array).clone();
		else
			throw new RuntimeException("Unknown pixel type in ImagePlusWithAccessor");
	}

	@Override
	public Object getReferenceToPixelZSlice(int sliceIndex, int cachePolicy, boolean willModifyPixels,
			SettableBoolean isCopy) {
		isCopy.value = false;
		return stack.getImageArray()[sliceIndex];
	}

	@Override
	public void clearCache() {
		// Nothing to do since we never cache anything
	}

	@Override
	public InputOutputObjectDimensions getDimensions() {
		// TODO missing number of channels
		return new InputOutputObjectDimensions(width, height, depth, 1, time);
	}

	@Override
	public void save() throws IOException {
		// TODO save ImagePlus
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
	public void setDimensions(int x, int y, int z, int c, int t) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public File getBackingFile() {
		return null;// As far as we know (and care to find out), there is no preexisting backing file that corresponds
					// to this imp
		// Such a file can be explicitly created through other calls
	}

	@Override
	public void dumpLittleEndianFloatBufferIntoSlice(ByteBuffer buffer, int sliceIndex) throws IOException {
		// TODO Auto-generated method stub

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
	public PixelType getPixelType() {
		// TODO Update when pixel types other than float can be handled
		return PixelType.FLOAT_TYPE;
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
	public void closeFileEarly() throws IOException {
		throw new RuntimeException("Not implemented");
	}

	private static final SettableBoolean unusedBoolean = new SettableBoolean();

	@Override
	public SliceAccessor getSlicesAccessor() {
		SliceAccessorAdapter accessor = new SliceAccessorAdapter() {
			@Override
			public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, float[] destinationArray)
					throws IOException {
				System.arraycopy(getReferenceToPixelZSlice(sliceIndex, cachePolicy, false, unusedBoolean), 0,
						destinationArray, 0, destinationArray.length);
			}

			@Override
			public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, short[] destinationArray)
					throws IOException {
				System.arraycopy(getReferenceToPixelZSlice(sliceIndex, cachePolicy, false, unusedBoolean), 0,
						destinationArray, 0, destinationArray.length);
			}

			@Override
			public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, byte[] destinationArray)
					throws IOException {
				System.arraycopy(getReferenceToPixelZSlice(sliceIndex, cachePolicy, false, unusedBoolean), 0,
						destinationArray, 0, destinationArray.length);
			}
		};
		return accessor;
	}
}
