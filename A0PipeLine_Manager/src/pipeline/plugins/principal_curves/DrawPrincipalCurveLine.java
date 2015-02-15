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
import processing_utilities.pcurves.LinearAlgebra.LineSegment;
import processing_utilities.pcurves.LinearAlgebra.Vektor;
import processing_utilities.pcurves.PrincipalCurve.PrincipalCurveClass;

public class DrawPrincipalCurveLine extends FourDPlugin implements AuxiliaryInputOutputPlugin, SpecialDimPlugin {

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
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

	private static void draw3DLine(IPluginIOStack output, Vektor p1, Vektor p2) {

		final float xCalFinal = output.getCalibration() != null ? (float) output.getCalibration().pixelWidth : 1;
		final float yCalFinal = output.getCalibration() != null ? (float) output.getCalibration().pixelHeight : 1;
		final float zCalFinal = output.getCalibration() != null ? (float) output.getCalibration().pixelDepth : 1;

		int x1 = (int) (p1.GetCoords(0) / xCalFinal);
		int y1 = (int) (p1.GetCoords(1) / yCalFinal);
		int z1 = (int) (p1.GetCoords(2) / zCalFinal);

		int x2 = (int) (p2.GetCoords(0) / xCalFinal);
		int y2 = (int) (p2.GetCoords(1) / yCalFinal);
		int z2 = (int) (p2.GetCoords(2) / zCalFinal);

		// Following code translated from http://www.codeforge.com/read/40833/bresenham_line3d.m__html
		/*
		 * This program is ported to MATLAB from:
		 * % B.Pendleton. line3d - 3D Bresenham's (a 3D line drawing algorithm)
		 * % ftp://ftp.isc.org/pub/usenet/comp.sources.unix/volume26/line3d, 1992
		 * %
		 * % Which is referenced by:
		 * % Fischer, J., A. del Rio (2004). A Fast Method for Applying Rigid
		 * % Transformations to Volume Data, WSCG2004 Conference.
		 * % http://wscg.zcu.cz/wscg2004/Papers_2004_Short/M19.pdf
		 * %
		 * % - Jimmy Shen (jimmy@rotman-baycrest.on.ca)
		 */

		int dx = x2 - x1;
		int dy = y2 - y1;
		int dz = z2 - z1;

		int ax = Math.abs(dx) * 2;
		int ay = Math.abs(dy) * 2;
		int az = Math.abs(dz) * 2;

		int sx = (int) Math.signum(dx);
		int sy = (int) Math.signum(dy);
		int sz = (int) Math.signum(dz);

		int x = x1;
		int y = y1;
		int z = z1;
		int idx = 1;

		boolean givenOutOfBoundsWarning = false;

		if (ax >= Math.max(ay, az)) { // x dominant
			int yd = ay - ax / 2;
			int zd = az - ax / 2;

			while (true) {
				try {
					output.setPixelValue(x, y, z, 255);
				} catch (ArrayIndexOutOfBoundsException e) {
					if (!givenOutOfBoundsWarning) {
						Utils.log("Principal curve segment out of bounds; continuing anyway", LogLevel.WARNING);
						givenOutOfBoundsWarning = true;
					}
				}

				idx = idx + 1;

				if (x == x2) // end
					break;

				if (yd >= 0) { // move along y
					y = y + sy;
					yd = yd - ax;
				}

				if (zd >= 0) { // move along z
					z = z + sz;
					zd = zd - ax;
				}

				x = x + sx; // move along x
				yd = yd + ay;
				zd = zd + az;
			}
		} else if (ay >= Math.max(ax, az)) { // y dominant
			int xd = ax - ay / 2;
			int zd = az - ay / 2;

			while (true) {
				try {
					output.setPixelValue(x, y, z, 255);
				} catch (ArrayIndexOutOfBoundsException e) {
					if (!givenOutOfBoundsWarning) {
						Utils.log("Principal curve segment out of bounds; continuing anyway", LogLevel.WARNING);
						givenOutOfBoundsWarning = true;
					}
				}
				idx = idx + 1;

				if (y == y2) // end
					break;

				if (xd >= 0) { // move along x
					x = x + sx;
					xd = xd - ay;
				}

				if (zd >= 0) { // move along z
					z = z + sz;
					zd = zd - ay;
				}

				y = y + sy; // move along y
				xd = xd + ax;
				zd = zd + az;
			}
		} else if (az >= Math.max(ax, ay)) { // z dominant
			int xd = ax - az / 2;
			int yd = ay - az / 2;

			while (true) {
				try {
					output.setPixelValue(x, y, z, 255);
				} catch (ArrayIndexOutOfBoundsException e) {
					if (!givenOutOfBoundsWarning) {
						Utils.log("Principal curve segment out of bounds; continuing anyway", LogLevel.WARNING);
						givenOutOfBoundsWarning = true;
					}
				}
				idx = idx + 1;

				if (z == z2) // end
					break;

				if (xd >= 0) { // move along x
					x = x + sx;
					xd = xd - az;
				}

				if (yd >= 0) { // move along y
					y = y + sy;
					yd = yd - az;
				}

				z = z + sz; // move along z
				xd = xd + ax;
				yd = yd + ay;
			}
		}
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
		final int widthFinal = destination.getWidth();
		final int heightFinal = destination.getHeight();
		final int depthFinal = destination.getDepth();
		destination.computePixelArray();

		final PrincipalCurveClass actualPCurve = pCurve.getPCurve();
		// actualPCurve.get
		// actualPCurve.initializeForGeodesicDistance();

		ParFor parFor = new ParFor(0, depthFinal - 1, r, threadPool, true);
		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker((z, threadIndex) -> {
				for (int x = 0; x < widthFinal; x++) {
					for (int y = 0; y < heightFinal; y++) {
						destination.setPixelValue(x, y, z, 0);
					}
				}
				return null;
			});
		parFor.run(true);

		parFor = new ParFor(0, actualPCurve.GetNumOfLineSegments() - 1, r, threadPool, true);
		for (int i0 = 0; i0 < parFor.getNThreads(); i0++)
			parFor.addLoopWorker((i, threadIndex) -> {
				LineSegment segment = actualPCurve.GetLineSegmentAt(i);
				draw3DLine(destination, segment.GetVektor1(), segment.GetVektor2());
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
