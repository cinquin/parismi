/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import ij.process.ImageProcessor;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pipeline.misc_util.FormatException;
import pipeline.misc_util.SettableBoolean;

public class PluginIOSubstack implements IPluginIOSubstack {

	public static IPluginIOStack getSubstack(IPluginIOStack stack, int startIndex, int stopIndex) {
		PluginIOSubstack subStack = new PluginIOSubstack();
		subStack.startIndex = startIndex;
		subStack.stopIndex = stopIndex;
		subStack.originalStack = stack;

		PluginIOSubstackProxy proxy = new PluginIOSubstackProxy(subStack, stack);

		subStack.proxy =
				(IPluginIOStack) Proxy.newProxyInstance(stack.getClass().getClassLoader(), new Class<?>[] {
						IPluginIOStack.class, ImageAccessor.class, IPluginIOSubstack.class }, proxy);

		return subStack.proxy;
	}

	private IPluginIOStack originalStack;
	private IPluginIOStack proxy;

	private int startIndex, stopIndex;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.IPluginIOSubstack#setStartIndex(int)
	 */
	@Override
	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.IPluginIOSubstack#setStopIndex(int)
	 */
	@Override
	public void setStopIndex(int stopIndex) {
		this.stopIndex = stopIndex;
	}

	public boolean isVirtual() {
		return originalStack.isVirtual();
	}

	public InputOutputObjectDimensions getDimensions() {
		InputOutputObjectDimensions result = originalStack.getDimensions();
		result.depth = stopIndex - startIndex + 1;
		return result;
	}

	public int getDepth() {
		return stopIndex - startIndex + 1;
	}

	public ImageProcessor getPixelsAsProcessor(int slice) {
		return originalStack.getPixelsAsProcessor(slice + startIndex);
	}

	public final Object[] getStackPixelArray() {
		Object[] result = new Object[stopIndex - startIndex + 1];
		Object[] fullSet = originalStack.getStackPixelArray();
		System.arraycopy(fullSet, startIndex, result, 0, stopIndex - startIndex + 1);
		return result;
	}

	public PixelIterator getBallIterator(final int xCenter, final int yCenter, final int zCenter, final int radius) {
		return originalStack.getBallIterator(xCenter, yCenter, zCenter + startIndex, radius);
	}

	public float getFloat(int x, int y, int z) {
		return originalStack.getFloat(x, y, z + startIndex);
	}

	public double getDouble(int x, int y, int z) {// put back double when SingleChannelView problem resolved
		return originalStack.getDouble(x, y, z + startIndex);
	}

	public final float getPixelValue(int x, int y, int z) {
		return originalStack.getPixelValue(x, y, z + startIndex);
	}

	public final void setPixelValue(int x, int y, int z, float value) {
		originalStack.setPixelValue(x, y, z + startIndex, value);
	}

	public Object getPixels(int s) {
		return originalStack.getPixels(s + startIndex);
	}

	public Object getPixels(int s, int channel, int timePoint) {
		return originalStack.getPixels(s + startIndex, channel, timePoint);
	}

	public float[] getPixels(int s, float a) {
		return originalStack.getPixels(s + startIndex, a);
	}

	public byte[] getPixels(int s, byte a) {
		return originalStack.getPixels(s + startIndex, a);
	}

	public final Object getPixelsCopy(int s) {
		return originalStack.getPixels(s + startIndex);
	}

	public void setPixels(Object o, int s) {
		originalStack.setPixels(o, s + startIndex);
	}

	// ImageAccessor methods

	public void dumpLittleEndianFloatBufferIntoSlice(ByteBuffer buffer, int sliceIndex) throws IOException,
			InterruptedException {
		((ImageAccessor) originalStack).dumpLittleEndianFloatBufferIntoSlice(buffer, sliceIndex + startIndex);
	}

	public void assignPixelsToZSlice(Object pixels, int sliceIndex, int cachePolicy, SettableBoolean justCopied)
			throws FormatException, IOException, InterruptedException {
		((ImageAccessor) originalStack).assignPixelsToZSlice(pixels, sliceIndex + startIndex, cachePolicy, justCopied);
	}

	public void copyPixelsIntoZSlice(Object pixels, int sliceIndex, int cachePolicy) throws FormatException,
			IOException, InterruptedException {
		((ImageAccessor) originalStack).copyPixelsIntoZSlice(pixels, sliceIndex + startIndex, cachePolicy);
	}

	public Object getReferenceToPixelZSlice(int sliceIndex, int cachePolicy, boolean willModifyPixels,
			SettableBoolean changesNeedExplicitSaving) throws IOException {
		return ((ImageAccessor) originalStack).getReferenceToPixelZSlice(sliceIndex + startIndex, cachePolicy,
				willModifyPixels, changesNeedExplicitSaving);
	}

	public Object getPixelZSliceCopy(int sliceIndex, int cachePolicy) throws IOException {
		return ((ImageAccessor) originalStack).getPixelZSliceCopy(sliceIndex + startIndex, cachePolicy);
	}

	public IPluginIOStack[] getChannels(String[] channelNames) {
		IPluginIOStack[] channels = originalStack.getChannels(channelNames);
		IPluginIOStack[] newChannels = new IPluginIOStack[channels.length];

		for (int i = 0; i < channels.length; i++) {
			IPluginIOStack newChannel = getSubstack(channels[i], startIndex, stopIndex);
			newChannel.setImageAccessor((ImageAccessor) proxy);
			newChannels[i] = newChannel;
		}

		return newChannels;
	}

	public HashMap<String, IPluginIOStack> getChannels() {
		Map<String, IPluginIOStack> channels = originalStack.getChannels();
		HashMap<String, IPluginIOStack> newChannels = new HashMap<>();

		for (Entry<String, IPluginIOStack> channel : channels.entrySet()) {
			IPluginIOStack newChannel = getSubstack(channel.getValue(), startIndex, stopIndex);
			newChannel.setImageAccessor((ImageAccessor) proxy);
			newChannels.put(channel.getKey(), newChannel);
		}

		return newChannels;

	}

	public SliceAccessor getSlicesAccessor() throws IOException {
		final SliceAccessor accessor0 = originalStack.getImageAccessor().getSlicesAccessor();

		SliceAccessor accessor1 = new SliceAccessorAdapter() {
			@Override
			public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, float[] destinationArray)
					throws IOException {
				accessor0.copyPixelSliceIntoArray(sliceIndex + startIndex, cachePolicy, destinationArray);
			}

			@Override
			public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, short[] destinationArray)
					throws IOException {
				accessor0.copyPixelSliceIntoArray(sliceIndex + startIndex, cachePolicy, destinationArray);
			}

			@Override
			public void copyPixelSliceIntoArray(int sliceIndex, int cachePolicy, byte[] destinationArray)
					throws IOException {
				accessor0.copyPixelSliceIntoArray(sliceIndex + startIndex, cachePolicy, destinationArray);
			}

			@Override
			public float getFloat(int x, int y, int z) throws IOException {
				return accessor0.getFloat(x, y, z + startIndex);
			}
		};

		return accessor1;
	}

	public ImageAccessor getImageAccessor() {
		return (ImageAccessor) proxy;
	}

}
