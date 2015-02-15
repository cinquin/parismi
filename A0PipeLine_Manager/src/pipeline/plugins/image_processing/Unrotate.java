/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import ij.process.ImageProcessor;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.SpecialDimPlugin;
import pipeline.plugins.TwoDPlugin;

public class Unrotate extends TwoDPlugin implements SpecialDimPlugin {

	@Override
	public String operationName() {
		return "Unrotate";
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
	public void runSlice(ImageProcessor ip, ImageProcessor dest, PreviewType previewType) {

		IPluginIOImage image = getImageInput();
		if (image == null) {
			throw new IllegalStateException("No access to source image to retrieve rotation");
		}

		double theta = 0;
		if (image.getRotation() == null) {
			Utils.log("No registered rotation for source image; assuming it is 0", LogLevel.WARNING);
		} else {
			theta = Math.toRadians(-image.getRotation());
		}

		Utils.log("Found angle " + theta + " for unrotation", LogLevel.DEBUG);

		BufferedImage i = new BufferedImage(ip.getWidth(), ip.getHeight(), BufferedImage.TYPE_USHORT_GRAY);
		WritableRaster inputRaster = i.getRaster();
		for (int x = 0; x < ip.getWidth(); x++) {
			for (int y = 0; y < ip.getHeight(); y++) {
				inputRaster.setPixel(x, y, new int[] { ip.getPixel(x, y) & 0xffff });
			}
		}

		BufferedImage rot = new BufferedImage(dest.getWidth(), dest.getHeight(), BufferedImage.TYPE_USHORT_GRAY);

		AffineTransform affineTransform = new AffineTransform();
		float rotx = dest.getWidth() / 2f;// was ip
		float roty = dest.getHeight() / 2f;// was ip
		affineTransform.rotate(theta, rotx, roty);
		affineTransform.translate((dest.getWidth() - ip.getWidth()) / 2f, (dest.getHeight() - ip.getHeight()) / 2f);
		Graphics2D g = rot.createGraphics();

		// Other two interpolation types result in blank image
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		// VALUE_RENDER_QUALITY results in blank image
		// g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
		// Utils.log("Hardware acceleration: "+i.getCapabilities(null).isAccelerated(),LogLevel.VERBOSE_DEBUG);
		boolean b = g.drawImage(i, affineTransform, null);
		if (b != true)
			throw new RuntimeException();
		g.dispose();

		WritableRaster raster = rot.getRaster();
		for (int x = 0; x < dest.getWidth(); x++) {
			for (int y = 0; y < dest.getHeight(); y++) {
				int[] values = raster.getPixel(x, y, (int[]) null);
				dest.putPixel(x, y, values[0]);
			}
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
		HashMap<String, InputOutputDescription> result = new HashMap<>();// PixelType.FLOAT_TYPE,PixelType.SHORT_TYPE
		result.put("Default destination", new InputOutputDescription(null, null,
				new PixelType[] { PixelType.SHORT_TYPE }, InputOutputDescription.KEEP_IN_RAM,
				InputOutputDescription.CUSTOM, true, false));
		return result;
	}

	private static double getLargestDim(IPluginIO input) {
		int width = ((IPluginIOHyperstack) input).getWidth();
		int height = ((IPluginIOHyperstack) input).getHeight();
		return (Math.max(width, height) * Math.sqrt(2.0));
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		return (int) (getLargestDim(input));
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		return (int) (getLargestDim(input));
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getDepth();
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getnTimePoints();
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return ((IPluginIOHyperstack) input).getnChannels();
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return input.getPixelType();
	}

}
