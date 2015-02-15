/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.cell_manipulation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import pipeline.PreviewType;
import pipeline.data.ClickedPoint;
import pipeline.data.IPluginIOHyperstack;
import pipeline.data.IPluginIOList;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.IntrospectionParameters.ParameterInfo;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.misc_util.parfor.ParFor;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * Generates an error if cell field values are not close enough. Used for pipeline testing.
 *
 */
public class CompareCells extends CellTransform {

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds", "Seeds2" };
	}

	@Override
	public String getToolTip() {
		return "Generates error if cell field values are not within tolerance";
	}

	@Override
	public String operationName() {
		return "Compare cells";
	}

	@ParameterInfo(userDisplayName = "Relative tolerance", floatValue = 0.1f, permissibleFloatRange = { 0.0001f, 0.2f })
	private float relativeTolerance;

	@ParameterInfo(userDisplayName = "Absolute tolerance", floatValue = 0.1f, permissibleFloatRange = { 0.0001f, 0.2f })
	private float absoluteTolerance;

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) throws InterruptedException {

		@SuppressWarnings("unchecked")
		final IPluginIOList<ClickedPoint> inputCells = (IPluginIOList<ClickedPoint>) pluginInputs.get("Seeds");
		@SuppressWarnings("unchecked")
		final IPluginIOList<ClickedPoint> inputCells2 = (IPluginIOList<ClickedPoint>) pluginInputs.get("Seeds2");

		@SuppressWarnings("unchecked")
		IPluginIOList<ClickedPoint> outputCells = (IPluginIOList<ClickedPoint>) pluginOutputs.get("Cells");
		inputCells.copyInto(outputCells);
		outputCells.clear();

		final AtomicBoolean error = new AtomicBoolean(false);

		if (inputCells2.size() != inputCells.size()) {
			Utils.log("Different numbers of cells: " + inputCells.size() + " vs " + inputCells2.size(), LogLevel.ERROR);
			error.set(true);
		}

		final Set<String> qProp1 = new HashSet<>();
		Set<String> qProp2 = new HashSet<>();
		qProp1.addAll(((PluginIOCells) inputCells).getQuantifiedPropertyNames());
		qProp2.addAll(((PluginIOCells) inputCells2).getQuantifiedPropertyNames());

		if (!qProp1.equals(qProp2)) {
			String message = "Lists of properties not equal: ";
			message += Utils.printStringArray(((PluginIOCells) inputCells).getQuantifiedPropertyNames());
			message += " vs ";
			message += Utils.printStringArray(((PluginIOCells) inputCells2).getQuantifiedPropertyNames());
			Utils.log(message, LogLevel.ERROR);
			error.set(true);
		}

		final List<Field> fields = new ArrayList<>();
		for (Field f : ClickedPoint.class.getFields()) {
			if (f.getType().equals(Float.TYPE) || f.getType().equals(Double.TYPE)) {
				f.setAccessible(true);
				fields.add(f);
			}
		}

		final KDTree<ClickedPoint> tree = new KDTree<>(3);
		for (ClickedPoint p : inputCells) {
			double[] key = new double[] { p.getx(), p.gety(), p.getz() };
			try {
				tree.insert(key, p);
			} catch (KeySizeException | KeyDuplicateException e) {
				throw new IllegalStateException(e);
			}
		}

		final double epsilon = 1.0E-10;

		final int VISITED = 0;
		final int NOT_VISITED = 1;

		for (ClickedPoint p : inputCells) {
			p.status = NOT_VISITED;
		}

		ParFor parFor = new ParFor("MergeCell comparison", 0, inputCells2.size() - 1, r, true);

		for (int i = 0; i < parFor.getNThreads(); i++)
			parFor.addLoopWorker((loopIndex, threadIndex) -> {
				ClickedPoint p1 = inputCells2.get(loopIndex);
				ClickedPoint p2;
				try {
					p2 = tree.nearest(new double[] { p1.getx(), p1.gety(), p1.getz() });
				} catch (KeySizeException e1) {
					throw new IllegalStateException(e1);
				}
				p2.status = VISITED;
				for (String s : qProp1) {
					double d1 = p1.getQuantifiedProperty(s);
					double d2 = p2.getQuantifiedProperty(s);
					if (Math.abs((d1 - d2) / (Math.abs(d1) + epsilon)) > relativeTolerance
							&& Math.abs(d1 - d2) > absoluteTolerance) {
						Utils.log("Field " + s + " differs in cells " + p1.x + "," + p1.y + "," + p1.z + " and " + p2.x
								+ "," + p2.y + "," + p2.z + ": " + d1 + " vs " + d2, LogLevel.ERROR);
						error.set(true);
					}
				}

				for (Field f : fields) {
					Object o1, o2;
					try {
						o1 = f.get(p1);
						o2 = f.get(p2);
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new RuntimeException(e);
					}

					double d1 = o1 instanceof Float ? ((Float) o1) : ((Double) o1);
					double d2 = o2 instanceof Float ? ((Float) o2) : ((Double) o2);
					if (Math.abs((d1 - d2) / (Math.abs(d1) + epsilon)) > relativeTolerance
							&& Math.abs(d1 - d2) > absoluteTolerance) {
						Utils.log("Field " + f.getName() + " differs in cells " + p1.x + "," + p1.y + "," + p1.z
								+ " and " + p2.x + "," + p2.y + "," + p2.z + ": " + d1 + " vs " + d2, LogLevel.ERROR);
						error.set(true);
					}
				}
				return null;
			});

		parFor.run(true);

		int unMatched = 0;
		String message = "";
		for (ClickedPoint p : inputCells) {
			if (p.status != VISITED) {
				message += p.x + "," + p.y + "," + p.z + " ";
				unMatched++;
			}
		}

		if (unMatched > 0) {
			Utils.log("Unmatched points: " + message, LogLevel.ERROR);
			error.set(true);
		}

		if (error.get()) {
			throw new RuntimeException("Equality test failed");
		}
	}

	@Override
	protected ClickedPoint transform(ClickedPoint point, IPluginIOList<ClickedPoint> allInputPoints,
			IPluginIOHyperstack inputImage, int pointIndex) {
		throw new IllegalStateException("Should not be called");
	}
}
