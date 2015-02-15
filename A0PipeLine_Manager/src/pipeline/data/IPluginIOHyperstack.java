/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import ij.ImagePlus;
import ij.gui.Roi;

import java.util.Map;

public interface IPluginIOHyperstack extends IPluginIOImage, IPluginIO {

	abstract IPluginIOStack[] getChannels(String[] channelNames);

	/**
	 * This does not allocate pixels.
	 * 
	 * @param destination
	 */
	abstract void copyIntoLeavePixelsNull(PluginIOHyperstack destination);

	abstract IPluginIOStack addChannel(String name);

	abstract float getFloat(int x, int y, int z, int channel, int timePoint);

	abstract double getDouble(int x, int y, int z, int channel, int timePoint);

	/**
	 * Indexing begins at 1 for z and c
	 * 
	 * @param z
	 * @param c
	 * @param v
	 */
	abstract void multiply(int z, int c, float v);

	/**
	 * Indexing begins at 0.
	 * 
	 * @param z
	 * @param channel
	 * @param timePoint
	 * @return 1D array of pixels
	 */
	abstract Object getPixels(int z, int channel, int timePoint);

	abstract float getPixelValue(int x, int y, int z, int channel, int timePoint);

	abstract void setPixelValue(int x, int y, int z, float value, int channel, int timePoint);

	abstract void setPixels(Object o, int s, int channel, int timePoint);

	/**
	 * This needs to go away when we've separated GUI more cleanly.
	 * 
	 * @return ROI
	 */
	abstract Roi getRoi();

	abstract int getType();

	abstract void setDepth(int depth);

	abstract int getDepth();

	abstract void setWidth(int width);

	abstract int getWidth();

	abstract void setHeight(int height);

	abstract int getHeight();

	abstract void setnChannels(int nChannels);

	abstract void setnTimePoints(int nTimePoints);

	abstract int getnChannels();

	abstract int getnTimePoints();

	/**
	 * @param supportsWritingToPixels
	 *            the supportsWritingToPixels to set
	 */
	abstract void setSupportsWritingToPixels(boolean supportsWritingToPixels);

	/**
	 * @return the supportsWritingToPixels
	 */
	abstract boolean isSupportsWritingToPixels();

	/**
	 * @param channels
	 *            the channels to set
	 */
	abstract void setChannels(Map<String, IPluginIOStack> channels);

	/**
	 * @return the channels
	 */
	abstract Map<String, IPluginIOStack> getChannels();

	abstract void setImagePlusDisplay(ImagePlus imagePlusDisplay);

	abstract ImagePlus getImagePlusDisplay();

	abstract ImageAccessor getImageAccessor();

	abstract void setImageAccessor(ImageAccessor imageAccessor);

}
