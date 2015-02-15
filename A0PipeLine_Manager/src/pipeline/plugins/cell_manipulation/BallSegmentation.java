/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.primitives.ArrayIntList;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOList;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ILoopWorker;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

/**
 * From a set of seeds, add to each seed a segmentation with a ball centered on the seed.
 *
 */
public class BallSegmentation extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "From a set of seeds, add to each seed a segmentation with a ball centered on the seed";
	}

	@ParameterInfo(userDisplayName = "Use embedded diameter", stringValue = "FALSE", noErrorIfMissingOnReload = false)
	private boolean usedEmbeddedDiameter;

	@ParameterInfo(userDisplayName = "Diameter in microns", stringValue = "TRUE", noErrorIfMissingOnReload = false)
	private boolean inMicrons;

	@ParameterInfo(userDisplayName = "Diameter", floatValue = 2, permissibleFloatRange = { 0, 10 },
			noErrorIfMissingOnReload = false)
	private float diameter;

	@ParameterInfo(userDisplayName = "Disk with same Z as seed", stringValue = "FALSE", noErrorIfMissingOnReload = true)
	private boolean diskOnly = false;

	@Override
	public String operationName() {
		return "BallSegmentation";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return SAME_AS_BINARY;
	}

	@Override
	public Map<String, InputOutputDescription> getInputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, false, false);
		desc0.name = "Seeds";
		result.put("Seeds", desc0);
		return result;
	}

	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) {
		Utils.log("Creating seeds", LogLevel.DEBUG);
		initializeOutputs();
		IPluginIOList<?> out = (IPluginIOList<?>) getInput("Seeds").duplicateStructure();
		PluginIOView view = out.createView();
		view.setData(out);
		pluginOutputs.put("Cells", out);
		ArrayList<PluginIOView> views = new ArrayList<>();
		views.add(view);
		return views;
	}

	@Override
	public Map<String, InputOutputDescription> getOutputDescriptions() {
		HashMap<String, InputOutputDescription> result = new HashMap<>();
		InputOutputDescription desc0 = new InputOutputDescription(null, null, null, 0, 0, true, false);
		desc0.name = "Cells";
		desc0.pluginWillAllocateOutputItself = true;
		result.put("Cells", desc0);
		return result;
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return new String[] { "Cells" };
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		final PluginIOCells inputCells = (PluginIOCells) pluginInputs.get("Seeds");

		final PluginIOCells outputCells = (PluginIOCells) pluginOutputs.get("Cells");
		inputCells.copyInto(outputCells);
		outputCells.clear();

		float rad = 0;
		if (!usedEmbeddedDiameter) {
			if (inMicrons) {
				if (inputCells.getCalibration() == null || inputCells.getCalibration().pixelDepth == 0) {
					throw new IllegalArgumentException("Calibration information not present");
				}
				rad = (float) ((diameter / inputCells.getCalibration().pixelWidth) / 2);
			} else
				rad = diameter / 2;
		}
		final int xyradius = (int) rad;
		final float zradius =
				(float) (rad * (inputCells.getCalibration().pixelWidth / inputCells.getCalibration().pixelDepth));
		final int width = inputCells.getWidth();
		final int height = inputCells.getHeight();
		final int depth = inputCells.getDepth();

		ParFor parFor = new ParFor(0, inputCells.getPoints().size() - 1, r, threadPool, true);
		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker(new ILoopWorker() {
				int localRadius = xyradius;
				int localRadiusSq = xyradius * xyradius;
				List<ClickedPoint> pointList = inputCells.getPoints();

				@Override
				public final Object run(int loopIndex, int threadIndex) {
					ClickedPoint p = pointList.get(loopIndex);
					ClickedPoint pCloned = (ClickedPoint) p.clone();
					pCloned.listNamesOfQuantifiedProperties = outputCells.getQuantifiedPropertyNames();
					pCloned.userCellDescriptions = outputCells.getUserCellDescriptions();

					if (usedEmbeddedDiameter) {
						localRadius = (int) p.getQuantifiedProperty("localRadius");
						localRadiusSq = localRadius * localRadius;
					}
					int x = (int) p.x;
					int y = (int) p.y;
					int z = (int) p.z;

					int z0 = (int) (diskOnly ? 0 : Math.min(z, zradius));
					int z1 = (int) (diskOnly ? 0 : Math.min(depth - 1 - z, zradius));

					int y0 = Math.min(y, localRadius);
					int y1 = Math.min(height - 1 - y, localRadius);

					int x0 = Math.min(x, localRadius);
					int x1 = Math.min(width - 1 - x, localRadius);

					ArrayIntList xCoord = new ArrayIntList(500);
					ArrayIntList yCoord = new ArrayIntList(500);
					ArrayIntList zCoord = new ArrayIntList(500);

					for (int k = -z0; k <= z1; k++) {
						int kSq = k * k;
						for (int j = -y0; j <= y1; j++) {
							int jSq = j * j;
							for (int i = -x0; i <= x1; i++) {
								int iSq = i * i;
								if (kSq + jSq + iSq > localRadiusSq)
									continue;
								xCoord.add(x + i);
								yCoord.add(y + j);
								zCoord.add(z + k);
							}
						}
					}
					pCloned.imageFullSegCoordsX = xCoord.getIntArrayFast();
					pCloned.imageFullSegCoordsY = yCoord.getIntArrayFast();
					pCloned.imageFullSegCoordsZ = zCoord.getIntArrayFast();

					return pCloned;
				}
			});

		for (Object p : parFor.run(true)) {
			outputCells.addDontFireValueChanged((ClickedPoint) p);
		}

		outputCells.fireValueChanged(false, false);
	}

}
