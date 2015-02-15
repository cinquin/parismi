/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.input_output;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOString;
import pipeline.misc_util.FileNameUtils;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.TwoDPlugin;

public class LazyCopyChannel extends TwoDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String operationName() {
		return "Lazy Copy Channel";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return 0;
	}

	@Override
	public void postRun() {
		for (IPluginIO io : pluginInputs.values()) {
			if (io.getDiskLocation() != null) {
				String filePath = io.getDiskLocation();
				String fileName = new File(filePath).getName();
				// Be careful when stripping the extension that the file might not have an extension,
				// and a "." character could occur in a parent directory name
				if (fileName.contains(".")) {
					fileName = filePath.substring(0, filePath.lastIndexOf('.'));
				}
				fileName = FileNameUtils.compactPath(fileName);
				pluginOutputs.put("File name", new PluginIOString(fileName));
			}
		}
	}

	@Override
	public void runSlice(ImageProcessor ip, ImageProcessor dest, PreviewType previewType) {

		int width = ip.getWidth();
		Rectangle roi = ip.getRoi();

		if (ip instanceof FloatProcessor) {
			float[] fp_pixels = (float[]) ip.getPixels();
			float[] output_pixels = (float[]) dest.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++)
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
					output_pixels[p] = fp_pixels[p];

		} else if (ip instanceof ByteProcessor) {
			byte[] byte_pixels = (byte[]) ip.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++)
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
					dest.putPixelValue(x, y, byte_pixels[p] & 0xff);
		} else if (ip instanceof ShortProcessor) {
			short[] short_pixels = (short[]) ip.getPixels();
			for (int y = roi.y; y < roi.y + roi.height; y++)
				for (int x = roi.x, p = width * y + x; x < roi.x + roi.width; x++, p++)
					dest.putPixelValue(x, y, short_pixels[p] & 0xffff);
		}
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default destination", new InputOutputDescription(null, null, new PixelType[] {
				PixelType.FLOAT_TYPE, PixelType.BYTE_TYPE, PixelType.SHORT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));
		return result;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] {};
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "File name" };
	}

}
