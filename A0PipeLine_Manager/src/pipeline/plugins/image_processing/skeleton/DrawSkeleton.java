/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.image_processing.skeleton;

import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIOHyperstack;
import pipeline.data.PluginIOImage.PixelType;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import processing_utilities.skeleton.Edge;
import processing_utilities.skeleton.SkeletonGraph;
import processing_utilities.skeleton.SkeletonPoint;
import processing_utilities.skeleton.Vertex;

/**
 * @author Olivier Cinquin
 */
public class DrawSkeleton extends ShortestSkeletonPath implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Create an image from a list of skeleton objects";
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		Utils.log("Creating image", LogLevel.DEBUG);
		initializeOutputs();
		@SuppressWarnings("unchecked")
		IPluginIOList<SkeletonGraph> inputs = (IPluginIOList<SkeletonGraph>) pluginInputs.get("Cells");
		if (inputs.size() == 0) {
			throw new IllegalArgumentException("Skeleton list is empty; nothing to draw");
		}
		PluginIOHyperstack output =
				new PluginIOHyperstack("Shortest skeleton path", inputs.get(0).getImageWidth(), inputs.get(0)
						.getImageHeight(), inputs.get(0).getImageDepth(), 1, 1, PixelType.BYTE_TYPE, false);
		output.setCalibration((Calibration) inputs.get(0).getCalibration().clone());
		PluginIOHyperstackViewWithImagePlus view = new PluginIOHyperstackViewWithImagePlus("Drawn skeleton");
		output.setImp(view);
		view.addImage(output);
		pluginOutputs.put("Drawn skeleton", output);

		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);

		return views;
	}

	private static final byte DEFAULT_COLOR = 127;

	@SuppressWarnings("unchecked")
	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		getOutput().setName("Skeleton");

		IPluginIOStack destination =
				((PluginIOHyperstack) getOutput()).getChannels().entrySet().iterator().next().getValue();

		destination.computePixelArray();

		// clear image
		for (int z = 0; z < destination.getDepth(); z++) {
			byte[] bytes = (byte[]) destination.getStackPixelArray()[z];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = 0;
			}
		}

		boolean finished = false;
		while (!finished) {
			try {
				for (SkeletonGraph s : (IPluginIOList<SkeletonGraph>) pluginInputs.get("Cells")) {
					byte intensity;
					if (s.hasQuantifiedProperty("color")) {
						Float f = s.getQuantifiedProperty("color");
						if (!f.isNaN())
							intensity = (f.byteValue());
						else
							intensity = DEFAULT_COLOR;
					} else
						intensity = DEFAULT_COLOR;
					drawSkeleton(s, destination, intensity);
				}

				finished = true;
			} catch (ConcurrentModificationException e) {
				// try again
			}
		}

	}

	@Override
	public String operationName() {
		return "Draw skeletons";
	}

	@Override
	public String version() {
		return "1.0";
	}

	private static void drawSkeleton(SkeletonGraph graph, IPluginIOStack output, byte intensity) {

		ArrayList<Edge> edgeList = graph.getEdges();
		ArrayList<Vertex> vertexList = graph.getVertices();

		// create empty adjacency and predecessor matrix

		for (Vertex v : vertexList) {
			for (SkeletonPoint p : v.getPoints()) {
				AnalyzeSkeleton.setPixel(output, p.x, p.y, p.z, intensity);
			}
		}

		for (Edge edge : edgeList) {
			for (SkeletonPoint p : edge.getSlabs()) {
				AnalyzeSkeleton.setPixel(output, p.x, p.y, p.z, intensity);
			}
		}

	}
}
