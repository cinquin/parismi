/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.principal_curves;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.IPluginIOStack;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.data.PluginIOPrincipalCurve;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;
import pipeline.plugins.SpecialDimPlugin;
import processing_utilities.pcurves.LinearAlgebra.Vektor;
import processing_utilities.pcurves.LinearAlgebra.VektorDD;
import processing_utilities.pcurves.PrincipalCurve.PrincipalCurveClass;

/**
 * @author Olivier Cinquin
 */
public class DrawPrincipalCurve extends FourDPlugin implements AuxiliaryInputOutputPlugin, SpecialDimPlugin {

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		Utils.log("Creating image", LogLevel.DEBUG);
		initializeOutputs();

		IPluginIOImage inputImage = getImageInput();

		PluginIOHyperstack output =
				new PluginIOHyperstack("Orthogonal distance to principal curve", getOutputWidth(inputImage),
						getOutputHeight(inputImage), getOutputDepth(inputImage), 1, 1, PixelType.FLOAT_TYPE, false);
		PluginIOPrincipalCurve pCurve = (PluginIOPrincipalCurve) pluginInputs.get("pCurve");
		if (pCurve.getCalibration() != null)
			output.setCalibration((Calibration) pCurve.getCalibration().clone());
		else if ((inputImage != null) && (inputImage.getCalibration() != null)) {
			output.setCalibration((Calibration) inputImage.getCalibration().clone());
		}
		PluginIOHyperstackViewWithImagePlus view = new PluginIOHyperstackViewWithImagePlus("Orth distance view");
		output.setImp(view);
		view.addImage(output);
		pluginOutputs.put("Orth distance view", output);

		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);

		return views;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		getOutput().setName("Principal curve");
		final PluginIOPrincipalCurve pCurve = (PluginIOPrincipalCurve) pluginInputs.get("pCurve");
		((PluginIOHyperstack) getOutput()).setCalibration(pCurve.getCalibration());

		final IPluginIOStack destination =
				((PluginIOHyperstack) getOutput()).getChannels().entrySet().iterator().next().getValue();

		destination.computePixelArray();

		final int widthFinal = destination.getWidth();
		final int heightFinal = destination.getHeight();
		final int depthFinal = destination.getDepth();

		final float xCalFinal =
				destination.getCalibration() != null ? (float) destination.getCalibration().pixelWidth : 1;
		final float yCalFinal =
				destination.getCalibration() != null ? (float) destination.getCalibration().pixelHeight : 1;
		final float zCalFinal =
				destination.getCalibration() != null ? (float) destination.getCalibration().pixelDepth : 1;

		final PrincipalCurveClass actualPCurve = pCurve.getPCurve();
		actualPCurve.initializeForGeodesicDistance();

		ParFor parFor = new ParFor(0, depthFinal - 1, r, threadPool, true);

		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker((z, threadIndex) -> {
				double[] coord = new double[] { 0, 0, 0 };
				float[] pixels = (float[]) destination.getStackPixelArray()[z];
				for (int x = 0; x < widthFinal; x++) {
					if (Thread.interrupted())
						return null;
					for (int y = 0; y < heightFinal; y++) {
						int pixelAddress = x + y * widthFinal;
						coord[0] = x * xCalFinal;
						coord[1] = y * yCalFinal;
						coord[2] = z * zCalFinal;
						Vektor transformedCoord = actualPCurve.getGeodesicDistances(new VektorDD(coord));
						pixels[pixelAddress] = (float) transformedCoord.GetCoords(0);
					}
				}
				return null;
			});

		parFor.run(true);
	}

	@Override
	public String operationName() {
		return "Draw skeletons";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "pCurve" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] {};
	}

	@Override
	public int getFlags() {
		return SAME_AS_FLOAT + SPECIAL_DIMENSIONS;
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
	public int getOutputWidth(IPluginIO input) {
		if (input != null)
			return ((IPluginIOImage) input).getDimensions().width;
		PluginIOPrincipalCurve pCurve = (PluginIOPrincipalCurve) pluginInputs.get("pCurve");
		return pCurve.getWidth();
	}

	@Override
	public int getOutputHeight(IPluginIO input) {
		if (input != null)
			return ((IPluginIOImage) input).getDimensions().height;
		PluginIOPrincipalCurve pCurve = (PluginIOPrincipalCurve) pluginInputs.get("pCurve");
		return pCurve.getHeight();
	}

	@Override
	public int getOutputDepth(IPluginIO input) {
		if (input != null)
			return ((IPluginIOImage) input).getDimensions().depth;
		PluginIOPrincipalCurve pCurve = (PluginIOPrincipalCurve) pluginInputs.get("pCurve");
		return pCurve.getDepth();
	}

	@Override
	public int getOutputNTimePoints(IPluginIO input) {
		return 1;
	}

	@Override
	public int getOutputNChannels(IPluginIO input) {
		return 1;
	}

	@Override
	public PixelType getOutputPixelType(IPluginIOStack input) {
		return PixelType.FLOAT_TYPE;
	}
}
