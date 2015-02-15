/**
 * Code adapted from Fiji.
 */

/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

// adapted from fiji
/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.SpecialDimPlugin;
import pipeline.plugins.ThreeDPlugin;
import processing_utilities.straightening.LocalStraightener;

public class Straighten extends ThreeDPlugin implements SpecialDimPlugin {

	@Override
	public String operationName() {
		return "Straighten";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + PARALLELIZE_WITH_NEW_INSTANCES + ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL
				+ SPECIAL_DIMENSIONS;
	}

	private static int getRoiWidth(Roi roi) {
		// PolygonRoi roi2=(PolygonRoi) roi.clone();
		synchronized (roi) {
			((PolygonRoi) roi).fitSplineForStraightening();
			// FloatPolygon p = roi.getFloatPolygon();
			return ((PolygonRoi) roi).splinePoints;
		}
	}

	@Override
	public void runChannel(final IPluginIOStack input, final IPluginIOStack output, final ProgressReporter p,
			final PreviewType previewType, boolean inputHasChanged) {

		Roi roi = (input.getParentHyperstack().getImp().imp.getRoi());// .simpleClone(); .clone() Roi OBJECTS ARE NOT
																		// THREAD-SAFE!!!!
		FloatPolygon polygon;
		int lineWidth = -1;
		synchronized (roi) {
			((PolygonRoi) roi).fitSplineForStraightening();
			polygon = roi.getFloatPolygon();

			Utils.log("Cloned dimensions " + polygon.npoints + " " + Math.round(roi.getStrokeWidth()),
					LogLevel.VERBOSE_DEBUG);
			Utils.log("Original dimensions " + roi.getFloatPolygon().npoints + " " + Math.round(roi.getStrokeWidth()),
					LogLevel.VERBOSE_DEBUG);
			if ((!(roi instanceof PolygonRoi)) || (roi instanceof PointRoi))
				roi = null;

			if (roi == null) {
				Utils.displayMessage("**** No appropriate ROI in Straighten " + input.getName(), true, LogLevel.ERROR);
				throw new IllegalArgumentException();
			}
			lineWidth = Math.round(roi.getStrokeWidth());
		}
		// ImageStack newStack=new ImageStack (getOutputWidth(input.imp),getOutputHeight(input.imp));

		ImageProcessor straightenedSlice = null;
		for (int i = 0; i < input.getDepth(); i++) {
			if (input.getPixels(i) == null) {
				Utils.log("null input slice", LogLevel.ERROR);
			}
			straightenedSlice =
					LocalStraightener.localStraightenLine(input.getPixelsAsProcessor(i), polygon, lineWidth);
			// newStack.addSlice("",straightenedSlice);
			System.arraycopy(straightenedSlice.getPixels(), 0, output.getPixels(i), 0, Math.min(
					((float[]) straightenedSlice.getPixels()).length, ((float[]) output.getPixels(i)).length));
			// output.getSlice(i+1).setPixels(straightenedSlice.getPixels());
		}
		// ImagePlus newImp=new ImagePlus("test",newStack);
		// newImp.show();
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		Roi roi = (((IPluginIOImage) input)).getImp().imp.getRoi();
		if ((!(roi instanceof PolygonRoi)) || (roi instanceof PointRoi))
			throw new RuntimeException("Wrong kind of ROI in Straighten plugin");
		return Math.round(roi.getStrokeWidth());
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		Roi roi = (((IPluginIOImage) input)).getImp().imp.getRoi();
		if ((!(roi instanceof PolygonRoi)) || (roi instanceof PointRoi))
			throw new RuntimeException("Wrong kind of ROI in Straighten plugin");
		return getRoiWidth(roi);
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getDepth();
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getnChannels();
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.FLOAT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.CUSTOM, true, false));
		return result;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getnTimePoints();
	}

}
