/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import pipeline.data.PluginIOImage.PixelType;

/**
 * Used by plugins to specify what their inputs and outputs are, and specify their pixel depth,
 * whether the data are best kept on disk or in RAM, whether the user is likely to want to view
 * the output by default, etc.
 *
 */
public class InputOutputDescription {

	public InputOutputDescription(String name, String matchingInputOrOutput, PixelType[] acceptablePixelTypes,
			int bestStorageMethod, int dimensions, boolean showAViewerByDefault, boolean bufferViewer) {
		this.name = name;
		this.matchingInputOrOutput = matchingInputOrOutput;
		this.bestStorageMethod = bestStorageMethod;
		this.acceptablePixelTypes = acceptablePixelTypes;
		this.dimensions = dimensions;
		this.showAViewerByDefault = showAViewerByDefault;
		if (acceptablePixelTypes != null)
			this.preferredPixelType = acceptablePixelTypes[0];
		else
			this.preferredPixelType = PixelType.FLOAT_TYPE;
		this.bufferViewer = bufferViewer;
	}

	public Class<?> objectType;

	public String name;

	public String matchingInputOrOutput;// If dimensions of an output depend on the matching input, or if there
	// is more than one acceptable pixel depth, the output that goes with this input (or vice-versa) will
	// be used to make a choice.

	public boolean useDefaultIfMatchingAbsent = true;

	public PixelType preferredPixelType;
	public PixelType[] acceptablePixelTypes;

	public boolean pluginWillAllocateOutputItself;

	/**
	 * True if the plugin wants the overall image structure created, but does not want the pixel arrays allocated.
	 * Used by LazyCopy.
	 */
	public boolean dontAllocatePixels;

	public boolean bufferViewer;

	public int bestStorageMethod;
	public static final int NOT_SPECIFIED = 0;
	public static final int KEEP_IN_RAM = 1;
	public static final int STORE_IN_TIFF_FILE = 2;

	public int dimensions = 1;
	// public static final int NOT_SPECIFIED=0;
	/**
	 * Same width and height, and add a channel in the output for each channel selected by the user for the default
	 * input hyperstack. This is an ad-hoc setting for hyperstacks.
	 */
	public static final int SAME_XY_MATCH_SELECTED_CHANNELS = 1;
	public static final int CUSTOM = 2;// The plugin will need to allocate the output itself
	public static final int Z_PROJECTS = 3;

	public boolean dontExpandChannelsBasedOnUserSelectionInDefaultSource = false;

	public boolean showAViewerByDefault;

	/**
	 * True if the plugin cannot work on input that is being modified (e.g. if that leads to an inconsistent data state
	 * that
	 * can lead to a crash; this is the case of AnalyzeSkeletton). If true, the pipeline will make sure the input is not
	 * being modified by any other plugin while the plugin that set this flag runs.
	 */
	public boolean pluginNeedsInputLocked;

	public boolean dontAutoRange;

	public boolean inputIsOptional = false;

	public Class<?> clazz;
}
