/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins.gui_control;

import pipeline.PreviewType;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.data.IPluginIOImage;
import pipeline.data.PluginIOCells;
import pipeline.misc_util.ProgressReporter;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.MultiListParameter;
import pipeline.parameters.TableParameter;
import pipeline.plugins.AuxiliaryInputOutputPlugin;
import pipeline.plugins.FourDPlugin;

/**
 * Looks for a PluginIOCells source and attaches it to a PluginIOHyperstackView source.
 *
 */
public class AttachCellsToDisplay extends FourDPlugin implements AuxiliaryInputOutputPlugin {

	@Override
	public String getToolTip() {
		return "Attach a set of cells to an open image window for overlay";
	}

	@Override
	public String operationName() {
		return "AttachCellsToDisplay";
	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public int getFlags() {
		return NO_IMP_OUTPUT;
	}

	@Override
	public void run(ProgressReporter r, MultiListParameter inChannels, TableParameter outChannels,
			PreviewType previewType, boolean inputHasChanged, AbstractParameter parameterWhoseValueChanged,
			boolean stayInCoreLoop) {

		IPluginIOImage sourceImage = getImageInput();
		PluginIOHyperstackViewWithImagePlus imp = sourceImage.getImp();
		PluginIOCells cells = (PluginIOCells) pluginInputs.get("Seeds");
		if (cells == null) {
			throw new IllegalStateException("No seeds found to attach to display");
		}
		imp.setCellsToOverlay(cells);
	}

	@Override
	public String[] getInputLabels() {
		return new String[] { "Seeds" };
	}

	@Override
	public String[] getOutputLabels() {
		return null;
	}

}
