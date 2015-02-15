/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.HashMap;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.SpecialDimPlugin;
import pipeline.plugins.ThreeDPlugin;

/**
 * Plugin to display concatenated image of all images embedded in protobuf file
 * by {@link EmbedImageWithProtobufSeeds} plugin.
 *
 */
public class DisplayEmbeddedImages extends ThreeDPlugin implements SpecialDimPlugin, AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Plugin to display concatenated image of all images embedded in protobuf file by "
				+ "EmbedImageWithProtobufSeeds plugin";
	}

	@Override
	public String operationName() {
		return "DisplayEmbeddedImages";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc =
				new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE },
						InputOutputDescription.KEEP_IN_RAM, InputOutputDescription.CUSTOM, true, false);
		desc.useDefaultIfMatchingAbsent = true;
		result.put("Default destination", desc);
		return result;
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		int sumDepths = 0;
		for (ClickedPoint p : seeds.getPoints()) {
			int zThickness =
					(int) (p.hasQuantifiedProperty("zThickness") ? p.getQuantifiedProperty("zThickness") + 1 : 1);
			sumDepths += zThickness;
		}
		return sumDepths;
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		if (getImageInput() != null) {
			return getImageInput().getDimensions().height;
		}
		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		int height = seeds.getHeight();

		if (height == 0) {
			for (ClickedPoint p : seeds) {
				if (p.y > height)
					height = (int) p.y;
			}
			height++;
		}

		return height;
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		if (getImageInput() != null) {
			return getImageInput().getDimensions().width;
		}

		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		int width = seeds.getWidth();
		if (width == 0) {
			for (ClickedPoint p : seeds) {
				if (p.x > width)
					width = (int) p.x;
			}
			width++;
		}

		return width;
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return 1;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;// TODO Update this when we handle non-float images
	}

	@Override
	public void runChannel(IPluginIOStack input, IPluginIOStack output, ProgressReporter r, PreviewType previewType,
			boolean inputHasChanged) {

		int width = output.getWidth();
		int height = output.getHeight();

		PluginIOCells seeds = (PluginIOCells) getInput("Seeds");
		int sumDepths = 0;
		for (ClickedPoint p : seeds.getPoints()) {
			int zThickness =
					(int) (p.hasQuantifiedProperty("zThickness") ? p.getQuantifiedProperty("zThickness") + 1 : 1);

			int[] pixelValues = p.imageFullSegCoordsX;

			for (int z = 0; z < zThickness; z++)
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++) {
						output.setPixelValue(x, y, z + sumDepths, pixelValues[z * (width * height) + y * width + x]);
					}
				}

			sumDepths += zThickness;
		}

	}

	@Override
	public int getFlags() {
		return SPECIAL_DIMENSIONS;
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] {};
	}

}
