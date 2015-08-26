/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import org.eclipse.jdt.annotation.NonNull;

import ij.measure.Calibration;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.data.InputOutputObjectDimensions.dimensionType;

public abstract class PluginIOImage extends PluginIO implements IPluginIO, IPluginIOImage {

	@Override
	public Object clone() {
		throw new RuntimeException("Not implemented");
	}

	private static final long serialVersionUID = 1L;

	private transient PluginIOHyperstackViewWithImagePlus imp;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#asPixelArray()
	 */
	@Override
	public abstract Object asPixelArray();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#getDimensions()
	 */
	@Override
	public abstract InputOutputObjectDimensions getDimensions();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * pipeline.data.PluginIOImageInterface#getDimensionLabels(pipeline.data.InputOutputObjectDimensions.dimensionType)
	 */
	@Override
	public abstract String[] getDimensionLabels(dimensionType dim);

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#getCalibration()
	 */
	@Override
	public abstract Calibration getCalibration();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#setCalibration(ij.measure.Calibration)
	 */
	@Override
	public abstract void setCalibration(Calibration calibration);

	Object imageAcquisitionMetadata;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#getImageAcquisitionMetadata()
	 */
	@Override
	public Object getImageAcquisitionMetadata() {
		return imageAcquisitionMetadata;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#setImageAcquisitionMetadata(java.lang.Object)
	 */
	@Override
	public void setImageAcquisitionMetadata(Object imageAcquisitionMetadata) {
		this.imageAcquisitionMetadata = imageAcquisitionMetadata;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#getPixelType()
	 */
	@Override
	public abstract PixelType getPixelType();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#convertTo(pipeline.data.PluginIOImage.PixelType)
	 */
	@Override
	public abstract void convertTo(PixelType newPixelType);

	protected PixelType pType;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#setPixelType(pipeline.data.PluginIOImage.PixelType)
	 */
	@Override
	public void setPixelType(PixelType pixelType) {
		pType = pixelType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#copyInto(pipeline.data.PluginIOImage)
	 */
	@Override
	public void copyInto(PluginIOImage destination) {
		destination.setImp(imp);
		destination.pType = pType;
		destination.setCalibration(getCalibration());
		destination.setImageAcquisitionMetadata(getImageAcquisitionMetadata());
		((IPluginIO) this).copyInto(destination);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#asProtobufBytes()
	 */
	@Override
	public byte @NonNull[] asProtobufBytes() {
		byte [] local = getProtobuf();
		if (local == null) {
			throw new RuntimeException();
		}
		return local;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#canConvertTo()
	 */
	@Override
	public abstract PixelType[] canConvertTo();

	public static boolean indexOf(PixelType[] pTypeArray, PixelType pixel) {
		if (pTypeArray == null)
			return false;
		for (PixelType element : pTypeArray) {
			if (element == pixel)
				return true;
		}
		return false;
	}

	public enum PixelType {
		FLOAT_TYPE, BYTE_TYPE, SHORT_TYPE, DOUBLE_TYPE
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#duplicateStructure(pipeline.data.PluginIOImage.PixelType, int, int,
	 * boolean)
	 */
	@Override
	public abstract IPluginIOImage duplicateStructure(PixelType newPixelType, int forceNSlices, int forceNChannels,
			boolean dontAllocatePixels);

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#setImp(pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus)
	 */
	@Override
	public void setImp(PluginIOHyperstackViewWithImagePlus imp) {
		this.imp = imp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOImageInterface#getImp()
	 */
	@Override
	public PluginIOHyperstackViewWithImagePlus getImp() {
		return imp;
	}

	@Override
	public Double getRotation() {
		return getCalibration().rotation;
	}

	@Override
	public void setRotation(Double angle) {
		getCalibration().rotation = angle;
	}
}
