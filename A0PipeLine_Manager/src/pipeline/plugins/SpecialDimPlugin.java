/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU License v2.
 ******************************************************************************/
package pipeline.plugins;

import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOStack;
import pipeline.data.PluginIOImage.PixelType;

/**
 * Interface implemented by plugins that create output pictures of a custom size (e.g. a straightening plugin)
 * These functions will be called by shell to know what dimensions to use when creating the output before
 * calling the plugin to do the actual processing.
 *
 */

public interface SpecialDimPlugin {
	int getOutputWidth(IPluginIO input);

	int getOutputHeight(IPluginIO input);

	int getOutputDepth(IPluginIO input);

	int getOutputNTimePoints(IPluginIO input);

	int getOutputNChannels(IPluginIO input);

	PixelType getOutputPixelType(IPluginIOStack input) throws InterruptedException;

}
