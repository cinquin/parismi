/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.ProgressSubrange;
import pipeline.GUI_utils.image_with_toolbar.PluginIOHyperstackWithToolbar;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOStack;
import pipeline.misc_util.PluginRuntimeException;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.SpecialDimPlugin;
import pipeline.plugins.input_output.MergeFiles;

/**
 * Concatenate TIFF stacks whose path is given a path that can contain wildcards.
 *
 */
public class ZStitch extends MergeFiles implements SpecialDimPlugin, AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Concatenate TIFF stacks whose path is given a path that can contain wildcards. "
				+ "At this point result must fit in RAM";
	}

	@Override
	public String operationName() {
		return "ZStitch";
	}

	@Override
	public String version() {
		return "1.0";
	}

	private int depth, width, height;

	private void computeDimensions() throws InterruptedException {

		List<IPluginIO> stacks = openInputFiles(null);
		int[] stackIndices = checkSelection(selectedFiles, stacks.size());

		depth = 0;
		width = 0;
		height = 0;

		for (int stackIndex : stackIndices) {
			IPluginIOStack stack = (IPluginIOStack) stacks.get(stackIndex);
			if (stack.getWidth() > width)
				width = stack.getWidth();
			if (stack.getHeight() > height)
				height = stack.getHeight();
			for (int z = 0; z < stack.getDepth(); z++) {
				try {
					stack.getPixels(z);
					depth++;
				} catch (Exception e) {
					break;
				}
			}
		}
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		return depth;
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		return height;
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return 1;
	}

	@Override
	public int getOutputWidth(IPluginIO input) {
		return width;
	}

	@Override
	public boolean shouldClearOutputs() {
		// Don't bother checking new dimensions because it can be time consuming and
		// might be unnecessarily repeated
		getOutputs().clear();
		return true;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		IPluginIOHyperstack destination = (IPluginIOStack) getOutput();
		int z = 0;

		ProgressSubrange r2 = new ProgressSubrange(r, 2);
		List<IPluginIO> stacks = openInputFiles(r2);
		r2.nextStep();

		// int [] stackIndices = checkSelection(selectedFiles, stacks.size());

		r2.setMin(0);
		r2.setMax(stacks.size());
		int loopIndex = 0;
		for (IPluginIO pIO : stacks) {
			IPluginIOStack stack = (IPluginIOStack) pIO;

			int localWidth = stack.getWidth();
			int localHeight = stack.getHeight();
			for (int localZ = 0; localZ < stack.getDepth(); localZ++) {
				try {

					/*
					 * slow, pixel-type independent code
					 * for (int x=0; x<localWidth; x++)
					 * for (int y=0; y<localHeight; y++)
					 * destination.setPixelValue(x, y, z, stack.getFloat(x, y, localZ), 1, 1);
					 */

					byte[] localSlice;
					byte[] globalSlice;
					try {
						localSlice = stack.getPixels(localZ, (byte) 0);
						globalSlice = (byte[]) destination.getPixels(z, 1, 1);
					} catch (ClassCastException e) {
						throw new PluginRuntimeException("ZStitch only implemented for 8-bit images", true);
					}

					for (int x = 0; x < localWidth; x++)
						for (int y = 0; y < localHeight; y++) {
							globalSlice[x + y * width] = localSlice[x + y * localWidth];
						}

					z++;
				} catch (Exception e) {
					// Hopefully we reach this point because we were trying to read past the number of
					// slices that really exist in the stack
					break;
				}
			}
			loopIndex++;
			r2.setValue(loopIndex);
		}
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {

		computeDimensions();

		PluginIOHyperstack createdOutput =
				new PluginIOStack("Z sitch", width, height, depth, 1, getOutputPixelType(null));

		setOutput("Default destination", createdOutput, true);

		PluginIOHyperstackViewWithImagePlus display = null;
		if (impForDisplay != null) {
			createdOutput.setImp(impForDisplay);
			display = impForDisplay;
		} else {
			display = new PluginIOHyperstackWithToolbar(createdOutput.getName());// PluginIOHyperstackViewWithImagePlus
			createdOutput.setImp(display);
		}

		ArrayList<PluginIOView> imagesToShow = new ArrayList<>();

		display.addImage(createdOutput);
		display.shouldUpdateRange = true;
		imagesToShow.add(display);

		Calibration inputCal = ((IPluginIOStack) openInputFiles(null).get(0)).getCalibration();
		if (inputCal != null)
			createdOutput.setCalibration((Calibration) inputCal.clone());
		else
			Utils.log("Z stitch: no calibration in input image; continuing anyway", LogLevel.INFO);

		return imagesToShow;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) throws InterruptedException {
		return ((IPluginIOStack) openInputFiles(null).get(0)).getPixelType();
	}

}
