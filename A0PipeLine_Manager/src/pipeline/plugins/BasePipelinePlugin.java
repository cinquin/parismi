/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

import ij.ImageListener;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import pipeline.PipelineCallback;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOImage;
import pipeline.data.InputOutputDescription;
import pipeline.data.PluginIOImage;
import pipeline.misc_util.ImageListenerWeakRef;
import pipeline.misc_util.IntrospectionParameters;
import pipeline.misc_util.Pair;
import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.SimpleThreadFactory;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.AbstractParameter;
import pipeline.parameters.ParameterListener;
import pipeline.parameters.SplitParameter;
import pipeline.parameters.SplitParameterListener;

/**
 * This is the main plugin class from which all plugins inherit, and that defines the basic
 * plugin functionality with respect to the pipeline.
 *
 */
public abstract class BasePipelinePlugin implements ImageListener, PipelinePlugin {

	private boolean triggerUpdates = true;

	@Override
	public void setUpdateTriggering(boolean t) {
		triggerUpdates = t;
	}

	@Override
	public boolean isUpdateTriggering() {
		return triggerUpdates;
	}

	static private SimpleThreadFactory threadFactory = new SimpleThreadFactory(Thread.MIN_PRIORITY);

	static public transient ExecutorService threadPool = Executors.newCachedThreadPool(threadFactory);

	static {
		if (!java.awt.GraphicsEnvironment.isHeadless())
			SwingUtilities.invokeLater(() -> Thread.currentThread().setPriority(Thread.MAX_PRIORITY));
		((ThreadPoolExecutor) threadPool).setCorePoolSize(Runtime.getRuntime().availableProcessors() + 10);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#operationName()
	 */
	@Override
	public abstract String operationName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getParameterArray()
	 */
	@Override
	public AbstractParameter[] getParameters() {
		AbstractParameter[] paramArray = { getParametersAsSplit(), null };
		return paramArray;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setParameterArray(pipeline.parameters.AbstractParameter[])
	 */
	@Override
	public void setParameters(AbstractParameter[] params) {
		IntrospectionParameters.setParameters(this, params);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getParameterListeners()
	 */
	@Override
	public ParameterListener[] getParameterListeners() {
		return new ParameterListener[] { getParameterListenersAsSplit() };
	}

	private Map<String, Pair<AbstractParameter, List<ParameterListener>>> paramsAndListeners;

	/**
	 * Stores the row index of the plugin the pipeline table.
	 */
	protected int ourRow = -1;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setRow(int)
	 */
	@Override
	public void setRow(int r) {
		ourRow = r;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setpipeLineListener(pipeline.PipelineCallback)
	 */
	@Override
	public void setpipeLineListener(PipelineCallback p) {
		pipelineCallback = p;
	}

	/**
	 * The plugin can use pipelineCallback to make a number of callbacks to the pipeline to get information
	 * about general settings, and about itself or other plugins in the piepeline table.
	 */
	protected transient PipelineCallback pipelineCallback;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#cleanup()
	 */
	@Override
	public void cleanup() {
		// Close windows, etc.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#createDestination(java.lang.String,
	 * pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus)
	 */
	@Override
	public List<PluginIOView> createOutput(String outputName, PluginIOHyperstackViewWithImagePlus impForDisplay,
			Map<String, IPluginIO> linkedOutputs) throws InterruptedException {
		throw new RuntimeException("Non overriden version of createDestination called");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setSource(pipeline.data.IPluginIO)
	 */
	@Override
	public void setInput(IPluginIO source) {
		pluginInputs.put("Default source", source);
		defaultSource = source;
	}

	private IPluginIO defaultSource = null;
	private IPluginIO defaultDestination = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setSource(java.lang.String, pipeline.data.IPluginIO,
	 * boolean)
	 */
	@Override
	public void setInput(String name, IPluginIO source, boolean setAsDefault) {
		pluginInputs.put(name, source);
		if (setAsDefault)
			defaultSource = source;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getSources()
	 */
	@Override
	public Map<String, IPluginIO> getInputs() {
		return pluginInputs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#clearSources()
	 */
	@Override
	public void clearInputs() {
		if (pluginInputs != null)
			pluginInputs.clear();
		defaultSource = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#clearDestinations()
	 */
	@Override
	public void clearOutputs() {
		if (pluginOutputs != null) {
			pluginOutputs.clear();
			if (pipelineCallback != null) {
				pipelineCallback.clearAllViews(ourRow);
			}
		}
		defaultDestination = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getSource()
	 */
	@Override
	public IPluginIO getInput() {
		if (defaultSource != null)
			return defaultSource;
		if ((pluginInputs == null) || pluginInputs.size() == 0)
			return null;
		if (pluginInputs.get("Default source") != null)
			return pluginInputs.get("Default source");
		return pluginInputs.values().iterator().next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getImageSource()
	 */
	@Override
	public IPluginIOImage getImageInput() {
		if (((defaultSource != null) && defaultSource instanceof IPluginIOImage))
			return (IPluginIOImage) defaultSource;
		if ((pluginInputs.containsKey("Default source"))
				&& pluginInputs.get("Default source") instanceof IPluginIOImage)
			return (IPluginIOImage) pluginInputs.get("Default source");
		else {
			for (IPluginIO source : pluginInputs.values()) {
				if (source instanceof IPluginIOImage) {
					source.setName("Default IO");
					return (IPluginIOImage) source;
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getDestination()
	 */
	@Override
	public IPluginIO getOutput() {
		if (defaultDestination != null)
			return defaultDestination;
		if (pluginOutputs == null)
			return null;
		IPluginIO dest = pluginOutputs.get("Default destination");
		if (dest != null)
			return dest;
		for (IPluginIO destination : pluginOutputs.values()) {
			if (destination instanceof IPluginIOImage)
				return destination;
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getDestination(java.lang.String)
	 */
	@Override
	public IPluginIO getOutput(String name) {
		if (pluginOutputs == null)
			return null;
		return pluginOutputs.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getSource(java.lang.String)
	 */
	@Override
	public IPluginIO getInput(String name) {
		if (pluginInputs == null)
			return null;
		return pluginInputs.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#initializeDestinations()
	 */
	@Override
	public void initializeOutputs() {
		if (pluginOutputs == null)
			pluginOutputs = new HashMap<>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setDestination(pipeline.data.IPluginIO)
	 */
	@Override
	public void setOutput(IPluginIO i) {
		initializeOutputs();
		pluginOutputs.put("Default destination", i);
		defaultDestination = i;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setDestination(java.lang.String, pipeline.data.IPluginIO,
	 * boolean)
	 */
	@Override
	public void setOutput(String name, IPluginIO i, boolean forceAsDefault) {
		initializeOutputs();
		pluginOutputs.put(name, i);
		if (forceAsDefault)
			defaultDestination = i;
	}

	/**
	 * The source PluginIOInterface that the plugin works on. Plugins do not necessarily need to make use of this
	 * reference.
	 */
	protected transient Map<String, IPluginIO> pluginInputs = new HashMap<>();

	/**
	 * The destination PluginIOInterface that the plugin works on. Plugins do not necessarily need to make use of this
	 * reference.
	 */
	protected transient Map<String, IPluginIO> pluginOutputs = null;// will be allocated when the pipeline first runs
																	// the plugin

	/**
	 * Used by some plugins to keep track of whether their progress bar is in the determinate or indeterminate state.
	 */
	protected transient boolean indeterminateProgress;

	/**
	 * Sets the progress bar reporter associated with the plugin to the determinate or indeterminate mode.
	 * If indeterminate, and pipelineCallback is not null, animates it every second.
	 * Users Swing invokeLater to make sure the updates are generated from the Swing thread.
	 * 
	 * @param p
	 *            bar passed to the plugin when it was called
	 * @param indeterminate
	 */
	protected static void progressSetIndeterminateThreadSafe(final ProgressReporter p, final boolean indeterminate) {
		if (p == null)
			return;
		p.setIndeterminate(indeterminate);
	}

	protected void progressSetValueThreadSafe(final ProgressReporter p, final int value) {
		SwingUtilities.invokeLater(() -> {
			if (p != null)
				p.setValue(value);
			if (pipelineCallback != null)
				pipelineCallback.redrawProgressRenderer(ourRow);
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getNonImageSources(java.util.List)
	 */
	@Override
	public int getNonImageInputs(List<String> listOfNames) {
		int numberAdded = 0;
		for (Entry<String, IPluginIO> inputSet : getInputs().entrySet()) {
			if (!(inputSet.getValue() instanceof IPluginIOImage)) {
				listOfNames.add(inputSet.getKey());
				numberAdded++;
			}
		}
		return numberAdded;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#memoryUsageEstimate(boolean)
	 */
	@Override
	public long memoryUsageEstimate(boolean includeSizeOfOutput) {
		if (!includeSizeOfOutput)
			return 0;// this will only work for plugins that don't allocate temporary structures
		// for (int)
		long estimatedSize = 0;
		for (IPluginIO o : pluginInputs.values()) {
			if (o instanceof IPluginIOImage) {
				IPluginIOImage image = (IPluginIOImage) o;
				PluginIOImage.PixelType pixelType = image.getPixelType();
				int bytes = 0;
				if (pixelType == PluginIOImage.PixelType.FLOAT_TYPE)
					bytes = 4;
				else if (pixelType == PluginIOImage.PixelType.BYTE_TYPE)
					bytes = 1;
				else if (pixelType == PluginIOImage.PixelType.SHORT_TYPE)
					bytes = 2;
				else
					throw new RuntimeException("Cannot determine pixel size to estimate memory usage " + pixelType);

				estimatedSize +=
						bytes * image.getDimensions().height * image.getDimensions().width
								* image.getDimensions().depth * image.getDimensions().nTimePoints;
			}
		}
		return estimatedSize;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#threadUsageEstimate()
	 */
	@Override
	public int threadUsageEstimate() {
		if (!(getInput() instanceof IPluginIOImage))
			return 1;
		return ((IPluginIOImage) getInput()).getDimensions().depth;
	}

	/**
	 * Used by the plugin to know how many threads to spawn. For now, they each spawn as
	 * many threads as there are logical CPUs, which is probably suboptimal is many
	 * plugins are running at the same time. Note that this value is sometimes internally
	 * decreased by plugins that don't have as many parallelizable chunks of data as there
	 * are CPUs.
	 * NB: this should be replaced by a call to the pipeline scheduler to run Runnables, so the
	 * scheduler can allocate threads in an optimal way.
	 */
	// @Deprecated
	protected transient int nCpus = Runtime.getRuntime().availableProcessors();

	// @Deprecated
	protected transient Thread[] threads = null;
	/**
	 * Atomic integer that is used for worker threads to grab a unique ID, which they sometimes
	 * need to properly share a common chunk of data.
	 */
	// @Deprecated
	protected transient final AtomicInteger slice_registry = new AtomicInteger(0);

	/**
	 * Set by the main thread to inform the worker threads that an interrupt was received,
	 * and that they should abort their computation and return.
	 * 
	 * @throws InterruptedException
	 */

	protected final static void startAndJoin(Thread[] threads) throws InterruptedException {
		for (Thread thread2 : threads) {
			thread2.start();
		}

		try {
			for (Thread thread : threads)
				thread.join();
		} catch (InterruptedException ie) {
			Utils.log("Plugin interrupted", LogLevel.DEBUG);
			for (Thread thread1 : threads)
				thread1.interrupt();

			/* Wait for the threads to finish */
			for (Thread thread : threads) {
				boolean finished = false;
				while (!finished) {
					try {
						thread.join();
						finished = true;
					} catch (InterruptedException ie2) {
						Utils.log("Interrupted while waiting for interrupted to return", LogLevel.DEBUG);
						throw ie2;
					}
				}
			}
		}
	}

	/**
	 * @Deprecated
	 * @return
	 */
	protected Thread[] newThreadArray() {
		return new Thread[nCpus];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getFlags()
	 */
	@Override
	public abstract int getFlags();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#version()
	 */
	@Override
	public abstract String version();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#impReplacement(ij.ImagePlus, ij.ImagePlus)
	 */
	@Override
	public void impReplacement(ImagePlus imp, ImagePlus newImp) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getDestinations()
	 */
	@Override
	public Map<String, IPluginIO> getOutputs() {
		initializeOutputs();
		return pluginOutputs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setDestinations(java.util.Map)
	 */
	@Override
	public void setOutputs(Map<String, IPluginIO> destinations) {
		this.pluginOutputs = destinations;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getInputDescriptions()
	 */
	@Override
	public abstract Map<String, InputOutputDescription> getInputDescriptions();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getOutputDescriptions()
	 */
	@Override
	public abstract Map<String, InputOutputDescription> getOutputDescriptions();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setSources(java.util.Map)
	 */
	@Override
	public void getInputs(Map<String, IPluginIO> inputs) {
		this.pluginInputs = inputs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#createDestination(pipeline.data.InputOutputDescription)
	 */
	@Override
	public boolean createOutput(InputOutputDescription desc, List<PluginIOView> views) {
		return false;
	}

	private transient ImageListenerWeakRef imageListenerWeakRef = new ImageListenerWeakRef(this, true);

	@Override
	public void imageClosed(ImagePlus imp) {
		if (pluginOutputs == null)
			return;
		if (clearMapIfLinksTo(pluginInputs, imp)) {
			pluginOutputs.clear();
		}
		clearMapIfLinksTo(pluginOutputs, imp);
	}

	private boolean clearMapIfLinksTo(Map<String, IPluginIO> hashMap, ImagePlus imp) {
		if (hashMap == null)
			return false;
		boolean foundSomething = false;
		for (IPluginIO io : hashMap.values()) {
			if (io instanceof IPluginIOImage) {
				if (((IPluginIOImage) io).getImp() != null) {
					PluginIOHyperstackViewWithImagePlus impMd = ((IPluginIOImage) io).getImp();
					if (impMd.imp == imp) {
						foundSomething = true;
						pipelineCallback.clearView(impMd);
					}
				}
			}
		}
		if (foundSomething) {
			Utils.log("Clearing pluginInputs or pluginOutputs for plugin " + this, LogLevel.DEBUG);
			hashMap.clear();
		}
		return foundSomething;
	}

	@Override
	public void imageOpened(ImagePlus imp) {

	}

	@Override
	public void imageUpdated(ImagePlus imp) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#finalize()
	 */
	@Override
	protected void finalize() {
		try {
			if (imageListenerWeakRef != null) {
				ImagePlus.removeImageListener(imageListenerWeakRef);
			}
		} finally {
			try {
				super.finalize();
			} catch (Throwable e) {
				Utils.printStack(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#shouldClearDestinations()
	 */
	@Override
	public boolean shouldClearOutputs() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getPipelineListener()
	 */
	@Override
	public PipelineCallback getPipelineListener() {
		return pipelineCallback;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getRow()
	 */
	@Override
	public int getRow() {
		return ourRow;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#setParametersAndListeners(java.util.Map)
	 */
	@Override
	public void setParametersAndListeners(Map<String, Pair<AbstractParameter, List<ParameterListener>>> pl) {
		paramsAndListeners = pl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getParameter(java.lang.String)
	 */
	@Override
	public AbstractParameter getParameter(String name) {
		if (paramsAndListeners.get(name) == null)
			throw new IllegalArgumentException("No such parameter " + name);
		return paramsAndListeners.get(name).getFst();
	}

	@Override
	public List<ParameterListener> getListener(String name) {
		if (paramsAndListeners.get(name) == null)
			throw new IllegalArgumentException("No such parameter " + name);
		return paramsAndListeners.get(name).getSnd();
	}

	private List<Pair<AbstractParameter, List<ParameterListener>>> getParamsSortedByDisplayOrder() {
		List<Pair<AbstractParameter, List<ParameterListener>>> list = new LinkedList<>(paramsAndListeners.values());
		Collections.sort(list, (o1, o2) -> {
			int x = o1.getFst().getDisplayIndex();
			int y = o2.getFst().getDisplayIndex();
			return (x < y) ? -1 : ((x == y) ? 0 : 1);
		});
		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getParametersAsSplit()
	 */
	@Override
	public AbstractParameter getParametersAsSplit() {
		if (paramsAndListeners == null)
			return null;
		if (paramsAndListeners.size() == 0)
			return null;
		Pair<AbstractParameter, List<ParameterListener>> firstPair = paramsAndListeners.values().iterator().next();
		if (paramsAndListeners.size() == 1)
			return firstPair.getFst();

		return new SplitParameter(firstPair.firstAsArray(getParamsSortedByDisplayOrder(), AbstractParameter.class));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.plugins.abstract_plugins.PipelinePlugin#getParameterListenersAsSplit()
	 */
	@Override
	public ParameterListener getParameterListenersAsSplit() {
		if (paramsAndListeners == null)
			return null;
		if (paramsAndListeners.size() == 0)
			return null;

		List<ParameterListener> allListeners = new ArrayList<>();

		for (Pair<AbstractParameter, List<ParameterListener>> p : paramsAndListeners.values()) {
			String paramName = p.getFst().getUserDisplayName();
			if (paramName == null)
				Utils.log("Null parameter name", LogLevel.WARNING);
			for (ParameterListener listener : p.getSnd()) {
				listener.setParameterName(paramName);
			}
			allListeners.addAll(p.getSnd());
		}

		return new SplitParameterListener(allListeners.toArray(new ParameterListener[] {}));
	}

	{ // initialization code for paramsAndListeners
		IntrospectionParameters.instantiateParameters(this);
	}

	private Set<IPipe> inputPipes = new HashSet<>();
	private Set<IPipe> outputPipes = new HashSet<>();

	@Override
	public Set<IPipe> getInputPipes() {
		return inputPipes;
	}

	@Override
	public Set<IPipe> getOutputPipes() {
		return outputPipes;
	}

	@Override
	public String getToolTip() {
		return "";
	}

}
