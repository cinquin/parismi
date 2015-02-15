/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.data.InputOutputObjectDimensions.dimensionType;
import pipeline.data.PluginIOImage.PixelType;

public interface IPluginIOImage extends IPluginIO, PluginIOCalibrable, IPluginIOListMember<IPluginIOImage>,
		IPluginIOListMemberQ<IPluginIOImage> {

	@Override
	Object clone();

	abstract Object asPixelArray();

	abstract InputOutputObjectDimensions getDimensions();

	/**
	 * @return Rotation at which the image was acquired
	 */
	abstract Double getRotation();

	/**
	 * 
	 * @param angle
	 */
	abstract void setRotation(Double angle);

	abstract String[] getDimensionLabels(dimensionType dim);

	abstract Object getImageAcquisitionMetadata();

	abstract void setImageAcquisitionMetadata(Object imageAcquisitionMetadata);

	abstract PixelType getPixelType();

	abstract void convertTo(PixelType newPixelType);

	/**
	 * This does not automatically convert pixel arrays, and can cause errors if used on an
	 * image that already has pixels associated with it.
	 * 
	 * @param PixelType
	 */
	abstract void setPixelType(PixelType pixelType);

	abstract void copyInto(PluginIOImage destination);

	/**
	 * 
	 * @return A list of pixel types this image can be converted to
	 */
	abstract PixelType[] canConvertTo();

	/**
	 * Create a new image with identical structure to the current one, with newly-allocated, blank pixel arrays.
	 * 
	 * @param newPixelType
	 *            If non-null, specifies pixel type for returned image. If null, use same as original image.
	 * @param forceNSlices
	 *            Has no effect if <0. If >=0, forces the number of slices of a stack or hyperstack to 1.
	 * @param forceNChannels
	 *            Has no effect if <0. If >=0, forces the number of channels of a hyperstack to 1.
	 * @param dontAllocatePixels
	 *            If true, the structure will be duplicated but the pixel arrays will not be allocated.
	 * @return New PluginIOImage.
	 */
	abstract IPluginIOImage duplicateStructure(PixelType newPixelType, int forceNSlices, int forceNChannels,
			boolean dontAllocatePixels);

	abstract void setImp(PluginIOHyperstackViewWithImagePlus imp);

	abstract PluginIOHyperstackViewWithImagePlus getImp();

	/**
	 * 
	 * @return true if the image is not backed by a pixel array that is all in memory. This helps accessing methods
	 *         to know whether it is more efficient to ask for a reference to the pixels, or to have them copied into
	 *         a preallocated array (see e.g. Z projector).
	 */
	boolean isVirtual();

}
