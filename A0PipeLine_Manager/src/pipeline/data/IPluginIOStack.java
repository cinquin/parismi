/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import ij.process.ImageProcessor;

public interface IPluginIOStack extends IPluginIOHyperstack, IPluginIOImage, IPluginIO {

	/**
	 * z indexing begins at 0.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return pixel value
	 */
	abstract float getFloat(int x, int y, int z);

	/**
	 * z indexing begins at 0.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return pixel value
	 */
	abstract double getDouble(int x, int y, int z);

	/**
	 * z indexing begins at 0.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return Pixel value as float
	 */
	abstract float getPixelValue(int x, int y, int z);

	/**
	 * z Indexing begins at 0.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param value
	 */
	abstract void setPixelValue(int x, int y, int z, float value);

	/**
	 * Fill in stackPixelArray for direct, fast access by whoever is using this view
	 */
	abstract void computePixelArray();

	/**
	 * Get 1D array of pixels for slice s
	 * 
	 * @param s
	 *            Indexing begins at 0
	 * @return Raster of pixels
	 */
	abstract Object getPixels(int s);

	/**
	 * Indexing beginning at 0
	 * 
	 * @param s
	 * @param a
	 *            Used to specify type of returned pixel array (for overloading)
	 * @return 1D float[]
	 */
	abstract float[] getPixels(int s, float a);

	abstract byte[] getPixels(int s, byte a);

	abstract Object getPixelsCopy(int s);

	abstract void setPixels(Object o, int s);

	abstract ImageProcessor getPixelsAsProcessor(int slice);

	abstract boolean sameDimensions(IPluginIOStack otherStack);

	abstract void clearPixels();

	abstract Object[] getStackPixelArray();

	abstract IPluginIOHyperstack getParentHyperstack();

	abstract void setStackPixelArray(Object object);

	PixelIterator getBallIterator(int xCenter, int yCenter, int zCenter, int radius);
}
