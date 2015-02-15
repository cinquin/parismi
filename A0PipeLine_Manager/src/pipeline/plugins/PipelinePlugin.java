/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import ij.ImagePlus;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.PipelineCallback;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.InputOutputDescription;
import pipeline.misc_util.Pair;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;

public interface PipelinePlugin {

	/**
	 * 
	 * @return A user-readable couple word-long description of what the plugin does.
	 */
	@NonNull String operationName();

	/**
	 * Returns an array of parameters that the plugin needs the user to be able to set. If more than two parameters need
	 * to be displayed, they need to be grouped in just two using the SplitParameter parameter, that allows to stuff
	 * as many parameters as desired in the same table column.
	 * See also getParameterListeners
	 * 
	 * @return Array of parameters instantiated by the plugin
	 */
	AbstractParameter[] getParameters();

	/**
	 * This is used by the pipeline to hand the plugin parameters that have already been instantiated (and set) by
	 * another
	 * instance of the plugin. In this function, the plugin needs to update its internal parameter variables to those
	 * given in the parameter array.
	 * 
	 * @param params
	 *            Array of parameters, arranged in the same order as that returned by the getParameterArray call
	 */
	void setParameters(AbstractParameter[] params);

	/**
	 * Returns a set of parameter listeners that correspond to the parameter arrays in getParameterArray and
	 * setParameterArray.
	 * The listeners are used to notify the plugin that the parameter values have changed (for example because the user
	 * edited them), and that the plugin needs to update any internal values it derives from those parameters.
	 * 
	 * @return Array of parameter listeners
	 */
	ParameterListener[] getParameterListeners();

	/**
	 * Called by the pipeline to inform the plugin of its row index in the pipeline table
	 * 
	 * @param r
	 *            Row index in the table (first index is 0)
	 */
	void setRow(int r);

	/**
	 * Gives the plugin a PipelineCallback that the plugin can use to make a number of callbacks
	 * to the pipeline to get information about general settings, and about itself or other plugins in
	 * the pipeline table.
	 * 
	 * @param p
	 */
	void setpipeLineListener(PipelineCallback p);

	/**
	 * Deprecated. Throw InterruptedException instead.
	 * Return value of the plugin when it detected that it was interrupted and
	 * it aborted its computation. This helps the pipeline figure out how to treat the results
	 * produced by the plugin.
	 */
	static final int THREAD_INTERRUPTED = 459723597;

	/**
	 * Not implemented properly yet because the shell does not keep
	 * a list of all the plugin instances it has created
	 */
	void cleanup();

	/**
	 * Used only by plugins that need to create their own output PluginIOImage. These should
	 * generally be plugins that are not handled by the shell (e.g. FourDPlugins) or plugins
	 * whose flags specify they have special requirements in terms of output image dimensions
	 * or pixel depth.
	 * 
	 * @param outputName
	 *            A reasonable name to give the output, suggested by the pipeline; can be null.
	 * @param impForDisplay
	 *            An ImagePlus to register created output with for GUI display; can be null.
	 * @param linkedOutputs
	 *            When not null, contains list of pre-existing outputs the user specified that
	 *            were created by another plugin.
	 * @return List of objects created for GUI display of the outputs; the pipeline will manipulate
	 *         those, for example showing/hiding them.
	 * @throws InterruptedException
	 */
	List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException;

	/**
	 * Sets the input PluginIOInterface that the plugin will be associated with.
	 * When setInput is called, plugin should do any required preparation before processing.
	 * If plugin needs to open a window of its own, now is probably a good time to create it
	 * or update it.
	 * 
	 * @param input
	 */
	void setInput(IPluginIO input);

	/**
	 * Sets the input PluginIOInterface that the plugin will be associated with.
	 * If this version of setInput is called, we assume that there should only be one
	 * output and remove all the other ones.
	 * When setInput is called, plugin should do any required preparation before processing.
	 * If plugin needs to open a window of its own, now is probably a good time to create it
	 * or update it.
	 * 
	 * @param input
	 * @param setAsDefault
	 *            TODO
	 */
	void setInput(String name, IPluginIO input, boolean setAsDefault);

	/**
	 * Returns the inputs that the plugin was given by the pipeline through a
	 * setInput call.
	 * 
	 * @return plugin input PluginIOImage
	 */
	Map<String, IPluginIO> getInputs();

	void clearInputs();

	void clearOutputs();

	/**
	 * Returns default input.
	 * 
	 * @return Default input
	 */
	IPluginIO getInput();

	/**
	 * Returns default input if it is an image. If it is not an image or no default image exists, returns any
	 * input that is an image.
	 * 
	 * @return Input that implements IPluginIOImage; null if no such input
	 */
	IPluginIOImage getImageInput();

	/**
	 * Returns the output PluginIOImage the plugin was given by the pipeline through a
	 * setOutput call.
	 * 
	 * @return PluginIOImage output
	 */
	IPluginIO getOutput();

	IPluginIO getOutput(String name);

	IPluginIO getInput(String name);

	/**
	 * Creates the output HashMap if it is null.
	 */
	void initializeOutputs();

	/**
	 * Sets the output PluginIOImage that the plugin will be associated with.
	 * The plugin does not necessarily need to use that information.
	 */
	void setOutput(IPluginIO i);

	/**
	 * Sets the output PluginIOImage that the plugin will be associated with.
	 * The plugin does not necessarily need to use that information.
	 */
	void setOutput(String name, IPluginIO i, boolean forceAsDefault);

	int getNonImageInputs(List<String> listOfNames);

	/**
	 * Estimate of the peak memory usage of the plugin. This method will only be called by the scheduler after the
	 * input of the plugin has been set. If the plugin is running while this method is called, it should return
	 * the estimated peak memory usage between the time the method is called and the time the plugin finishes running.
	 * This method should be overridden by plugins that allocate temporary structures or whose output is not
	 * the same size as the input.
	 * 
	 * @param includeSizeOfOutput
	 *            True if the size of the output image should be included in the memory estimate.
	 * @return Memory size in bytes
	 */
	long memoryUsageEstimate(boolean includeSizeOfOutput);

	/**
	 * Estimate of the peak number of threads the plugin has a use for. This method will only be called by the scheduler
	 * after the
	 * input of the plugin has been set. If the plugin is running while this method is called, it should return
	 * the estimated peak number of threads between the time the method is called and the time the plugin finishes
	 * running.
	 * This method should be overridden by plugins that don't use one thread per slice in the input image.
	 * 
	 * @return Number of threads
	 */
	int threadUsageEstimate();

	/**
	 * Dimensions of product image and ImageProcessor type (ie pixel depth) are the same as input
	 */
	static final int PRESERVES_DIMENSIONS = 1;
	/**
	 * Dimensions of product image and ImageProcessor type (ie pixel depth) are the same as input, but final result
	 * needs to be float
	 */
	static final int SAME_AS_FLOAT = 2;
	/**
	 * Dimensions of product image and ImageProcessor type (ie pixel depth) are the same as input, but final result
	 * needs to be binary
	 */
	static final int SAME_AS_BINARY = 512;
	/**
	 * Plugin projects slices.
	 */
	static final int Z_PROJECTS = 4;
	/**
	 * Plugin does something in terms of image dimensions or pixel depth that the pipeline shell does not understand.
	 */
	static final int CUSTOM = 8;
	/**
	 * @deprecated Plugin wants to work on individual slices (ie in 2D), but not on stacks or hyperstacks
	 *             This flag is now unnecessary since this sort of plugin would inherit from TwoDPlugin.
	 */
	@Deprecated
	static final int ONLY_2D = 16; // needs to be spoon-fed slices instead of working on a stack
	/**
	 * @deprecated Plugin wants to work on whole channels (ie in 3D), but not on hyperstacks (ie doesn't want to know
	 *             about the different channels in an image)
	 *             This flag is now unnecessary since this sort of plugin would inherit from ThreeDPlugin.
	 */
	@Deprecated
	static final int ONLY_3D = 2048;
	/**
	 * Don't attempt to run a bunch of slices or channels in different threads. The channel setting might not be honored
	 * for now.
	 */
	static final int DONT_PARALLELIZE = 32;
	/**
	 * To run correctly in parallel, new a new instance of the plugin must be created for every thread.
	 */
	static final int PARALLELIZE_WITH_NEW_INSTANCES = 64;
	static final int DISPOSE_WHEN_NSLICES_CHANGES = 128;
	/**
	 * Plugin only wants to deal with float (ie 32-bit) images. Other images will be converted to float by the pipeline
	 * before the plugin is called.
	 */
	static final int ONLY_FLOAT_INPUT = 256;
	/**
	 * Plugin only wants to deal with binary (8-bit) images.
	 */
	static final int ONLY_BINARY_INPUT = 1024;
	/**
	 * Plugin wants the pipeline to take care of creating as many output channels as there are dimensions.
	 */
	static final int ONE_OUTPUT_CHANNEL_PER_INPUT_CHANNEL = 4096;
	/**
	 * Plugin just wants a single, standalone (mono-channel) ImagePlus as output (e.g. maybe it displays a profile
	 * plot).
	 */
	static final int PLUGIN_CREATES_OUTPUT_ITSELF = 8192;//
	/**
	 * Plugin has no ImagePlus output (or at least no image output it wants the pipeline to consider as its
	 * main visualizable output and take care of displaying and passing to other plugins that want to use it
	 * as their input).
	 */
	static final int NO_IMP_OUTPUT = 16384;
	/**
	 * The xyz dimensions of the plugin output image need not match those of the input. In this case the plugin
	 * should create its own output in response to the createOutput() call.
	 */
	static final int SPECIAL_DIMENSIONS = 32768;//
	/**
	 * If set, fresh arrays of pixels will not be allocated when an output image is automatically
	 * created by the pipeline for the plugin. In that case, the pixels will point to the pixels of some
	 * other already-open image. The plugin should be careful not to modify the values before allocating
	 * fresh arrays for the pixels, or there will be unintended side-effects in the image the pixels
	 * belong to.
	 */
	static final int DONT_ALLOCATE_OUTPUT_PIXELS = 65536;
	/**
	 * Set if the plugin needs its input to be locked by the pipeline while it is running. Use for example
	 * if modification of the input (by a different plugin) would cause the plugin to crash or invalidate
	 * all of the output. Used by AnalyzeSkeleton.
	 */
	static final int NEED_INPUT_LOCKED = 131072;
	/**
	 * Take all the outputs from input row and stuff them as inputs in the input HashMap, irrespective
	 * of their names.
	 */
	static final int STUFF_ALL_INPUTS = 262144;
	/**
	 * Plugin does not use any inputs
	 */
	static final int NO_INPUT = 262144 * 2;

	static final int ONLY_1_INPUT_CHANNEL = 262144 * 4;

	static final int NO_ERROR = 0;
	static final int ERROR = 1;

	/**
	 * Returns the flags of the plugin, which should be a sum of the flag integers declared in this file
	 * (each flag value is a different power of 2).
	 * 
	 * @return Sum of flags
	 */
	int getFlags();

	/**
	 * Returns the version of the plugin, which will be stored in the metadata generated by the pipeline.
	 * 
	 * @return Plugin version
	 */
	String version();

	/**
	 * Notifies plugin that a window has been replaced by another, for example when a hyperstack is transformed into a
	 * composite image.
	 * Plugins that attach listeners to windows should override this method.
	 * 
	 * @param imp
	 *            ImagePlus that is being closed
	 * @param newImp
	 *            Replacement ImagePlus
	 */
	void impReplacement(ImagePlus imp, ImagePlus newImp);

	Map<String, IPluginIO> getOutputs();

	void setOutputs(Map<String, IPluginIO> outputs);

	Map<String, InputOutputDescription> getInputDescriptions();

	Map<String, InputOutputDescription> getOutputDescriptions();

	void getInputs(Map<String, IPluginIO> inputs);

	/**
	 * Create an output that was declared in getOutputDescriptions as needing to be allocated by
	 * the plugin itself
	 * 
	 * @param desc
	 *            Description provided by the plugin, that has the pluginWillAllocateOutputItself field set to true.
	 * @param views
	 *            Add views to this list for created output
	 * @return true if the input description was recognized and the output created
	 */
	boolean createOutput(InputOutputDescription desc, List<PluginIOView> views);

	void finalize();

	/**
	 * Called by the pipeline after input to plugin has potentially changed. If the plugin needs to recreate its outputs
	 * (for example because dimensions have changed), it should clear the outputs and return true.
	 * 
	 * @return true if plugin cleared its outputs in response to the call, and they need to be recreated
	 */
	boolean shouldClearOutputs();

	PipelineCallback getPipelineListener();

	int getRow();

	void setParametersAndListeners(Map<String, Pair<AbstractParameter, List<ParameterListener>>> pl);

	AbstractParameter getParameter(String name);

	AbstractParameter getParametersAsSplit();

	ParameterListener getParameterListenersAsSplit();

	Set<IPipe> getInputPipes();

	Set<IPipe> getOutputPipes();

	List<ParameterListener> getListener(String name);

	void setUpdateTriggering(boolean t);

	boolean isUpdateTriggering();

	String getToolTip();

}
