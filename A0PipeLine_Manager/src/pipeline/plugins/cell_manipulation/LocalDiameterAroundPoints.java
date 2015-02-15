/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.ThreeDPlugin;

/**
 * Starting from an image and a list of points, quantify the largest diameter of balls centered on each of those
 * points that only contain non-0 pixels of the source image.
 * FIXME For now, this plugin should NOT be run on more than 1 channel at a time.
 * FIXME Not dealing with z well (should take into account different z and xy resolutions when considering diameter).
 */
public class LocalDiameterAroundPoints extends ThreeDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "From a set of cells and image, quantify the largest diameter of balls centered" + " on each cell";
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		result.put("Default source", new InputOutputDescription(null, null, new PixelType[] { PixelType.FLOAT_TYPE,
				PixelType.SHORT_TYPE, PixelType.BYTE_TYPE }, InputOutputDescription.NOT_SPECIFIED,
				InputOutputDescription.NOT_SPECIFIED, false, false));
		return result;
	}

	@Override
	public boolean createOutput(InputOutputDescription desc, List<PluginIOView> views) {
		if (desc.name.equals("Seeds")) {
			Utils.log("Creating seeds", LogLevel.DEBUG);
			initializeOutputs();
			PluginIOCells seeds = new PluginIOCells();
			ListOfPointsView<ClickedPoint> view = new ListOfPointsView<>(seeds);
			view.setData(seeds);
			pluginOutputs.put("Seeds", seeds);
			views.add(view);
			return true;
		} else
			return false;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Seeds";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Seeds", desc0);
		result.put("Default destination",
				new InputOutputDescription(null, null, null, InputOutputDescription.KEEP_IN_RAM,
						InputOutputDescription.SAME_XY_MATCH_SELECTED_CHANNELS, true, false));

		return result;
	}

	@Override
	public int getFlags() {
		return 0;
	}

	@Override
	public String operationName() {
		return "Local Diameter Around Point";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public void runChannel(IPluginIOStack input, final IPluginIOStack output, ProgressReporter r,
			PreviewType previewType, boolean inputHasChanged) throws InterruptedException {

		Utils.log("Running ball quantification", LogLevel.DEBUG);

		output.clearPixels();

		PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");
		PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Seeds");
		outputCells.clear();
		inputCells.copyInto(outputCells);

		outputCells.addQuantifiedPropertyName("localDiameter");
		boolean fieldIsNew =
				outputCells.getQuantifiedPropertyNames().size() > inputCells.getQuantifiedPropertyNames().size();

		int width = input.getWidth();
		int height = input.getHeight();
		int maxRadius = 500;
		int depth = input.getDepth();

		float zRatio = 1;
		if (input.getCalibration() != null) {
			zRatio = (float) (input.getCalibration().pixelDepth / input.getCalibration().pixelWidth);
			Utils.log("Found calibration information; using a z ratio of " + zRatio, LogLevel.INFO);
		}

		input.computePixelArray();

		// FIXME Change to BallIterator, from which following code was copied

		for (ClickedPoint p : inputCells.getPoints()) {
			ClickedPoint pCloned = (ClickedPoint) p.clone();
			pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
			pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();
			if (fieldIsNew)
				pCloned.getQuantifiedProperties().add(0f);

			int x = (int) p.x;
			int y = (int) p.y;
			int z = (int) p.z;

			if (input.getPixelValue(x, y, z) != 0) {
				pCloned.setQuantifiedProperty("localDiameter", -1);
				outputCells.addDontFireValueChanged(pCloned);
				continue;
			}

			boolean done = false, oneIt = false;
			int radius;
			for (radius = 0; (radius < maxRadius) && (!done); radius++) {

				int radiusSq = radius * radius;

				int z0 = Math.min(z, radius);
				int z1 = Math.min(depth - 1 - z, radius);

				int y0 = Math.min(y, radius);
				int y1 = Math.min(height - 1 - (y), radius);

				int x0 = Math.min(x, radius);
				int x1 = Math.min(width - 1 - x, radius);

				for (int k = -z0; (k <= z1) && (!done); k++) {
					float kSq = k * k * zRatio * zRatio;
					for (int j = -y0; (j <= y1) && (!done); j++) {
						int jSq = j * j;
						for (int i = -x0; (i <= x1) && (!done); i++) {
							int iSq = i * i;
							if (kSq + jSq + iSq > radiusSq)
								continue;
							oneIt = true;
							if (input.getPixelValue(x + i, y + j, z + k) != 0)
								done = true;
						}
					}
				}
				if (!oneIt)
					done = true;
			}

			if (!done)
				Utils.displayMessage("Warning: maximum attempted radius " + maxRadius + " was too low", true,
						LogLevel.WARNING);

			radius--;

			pCloned.setQuantifiedProperty("localDiameter", radius * 2);

			// Update output image
			int radiusSq = radius * radius;

			int z0 = Math.min(z, radius);
			int z1 = Math.min(depth - 1 - z, radius);

			int y0 = Math.min(y, radius);
			int y1 = Math.min(height - 1 - (y), radius);

			int x0 = Math.min(x, radius);
			int x1 = Math.min(width - 1 - x, radius);

			for (int k = -z0; k <= z1; k++) {
				float kSq = k * k * zRatio * zRatio;
				for (int j = -y0; j <= y1; j++) {
					int jSq = j * j;
					for (int i = -x0; i <= x1; i++) {
						int iSq = i * i;
						if (kSq + jSq + iSq > radiusSq)
							continue;
						output.setPixelValue(x + i, y + j, z + k, 1);
					}
				}
			}

			// End update output image

			outputCells.addDontFireValueChanged(pCloned);
		}

		outputCells.fireValueChanged(false, false);
	}

}
