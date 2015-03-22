/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom.Document;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.bean_table.DoNotShowInTable;
import pipeline.misc_util.PluginIOListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

public abstract class PluginIO implements Serializable, IPluginIO {

	/**
	 * If relevant, the description that led to this object being created by IPluginShell; can be null.
	 */
	private InputOutputDescription description;

	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#asFile(java.io.File)
	 */
	@Override
	public abstract File asFile(File saveTo, boolean useBigTIFF) throws IOException, InterruptedException;

	/**
	 * List of pipeline rows that are using this object as an output.
	 */
	@SuppressWarnings("unused")
	private List<Integer> outputForRows;

	@Override
	public void setOutputForRows(List<Integer> list) {
		outputForRows = list;
	}

	/**
	 * List of pipeline rows that are using this object as an input.
	 */
	@SuppressWarnings("unused")
	private List<Integer> inputForRows;

	@Override
	public void setInputForRows(List<Integer> list) {
		inputForRows = list;
	}

	private String name;

	/**
	 * Any plugin that is using this object as an input and does not want it modified until it is done
	 * should register its thread in lockingThreads.
	 */
	private transient LinkedList<Thread> lockingThreads = new LinkedList<>();

	/**
	 * Set to true by plugin[s] that own this object while modifying it.
	 */
	private transient AtomicBoolean isUpdating = new AtomicBoolean(false);

	/**
	 * Description of how the user specified that IO in the pipeline (e.g. by naming an open window, by
	 * a relative reference to another row, or by absolute reference)
	 */
	private String derivation;

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		setLockingThreads(new LinkedList<Thread>());
		setIsUpdating(new AtomicBoolean(false));
		outputForRows = new LinkedList<>();
		inputForRows = new LinkedList<>();
	}

	private long lastTimeModified;

	private Document metadata;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#getMetadata()
	 */
	@Override
	@DoNotShowInTable
	public Document getMetadata() {
		return metadata;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#setMetadata(org.jdom.Document)
	 */
	@Override
	public void setMetadata(Document metadata) {
		this.metadata = metadata;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#listOfSubObjects()
	 */
	@Override
	public String[] listOfSubObjects() {
		return new String[] { getName() };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#copyInto(pipeline.data.PluginIO)
	 */
	@Override
	public void copyInto(IPluginIO destination) {
		destination.setName(name);
		destination.setInputForRows(new LinkedList<Integer>());
		destination.setOutputForRows(new LinkedList<Integer>());

		destination.setLockingThreads(new LinkedList<Thread>());
		destination.setIsUpdating(new AtomicBoolean(false));
		destination.setLastTimeModified(lastTimeModified);
		if (metadata != null)
			destination.setMetadata((Document) metadata.clone());
		destination.setDerivation(getDerivation());
		destination.setDescription(getDescription());
		destination.setLastTimeModified(getLastTimeModified());
		if (getMetadata() != null)
			destination.setMetadata((Document) getMetadata().clone());
		destination.setPersistent(isPersistent());
		if (getProtobuf() != null)
			destination.setProtobuf(getProtobuf().clone());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#getProperty(java.lang.String)
	 */
	@Override
	@DoNotShowInTable
	public Object getProperty(String string) {
		if ("Protobuf".equals(string)) {
			return getProtobuf();
		} else
			throw new RuntimeException("Unrecognized property " + string);
	}

	private @NonNull byte[] protobuf = new byte [] {};

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#asProtobufBytes()
	 */
	@Override
	public @NonNull byte[] asProtobufBytes() {
		return protobuf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#restoreFromProtobuf()
	 */
	@Override
	public void restoreFromProtobuf() throws NotRestorableFromProtobuf, InterruptedException {
		throw new NotRestorableFromProtobuf();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#setProperty(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setProperty(String string, Object value) {
		if ("Protobuf".equals(string)) {
			setProtobuf((byte[]) value);
			try {
				restoreFromProtobuf();
			} catch (NotRestorableFromProtobuf e) {
				// do nothing
			} catch (InterruptedException e) {
				throw new RuntimeException("Profobuf restore interrupted", e);
			}
		} else
			throw new RuntimeException("Unrecognized property " + string);
	}

	public boolean save;

	/**
	 * Set to true for inputs that should not be recomputed when the pipeline is restored.
	 * For example when a ROI has been specified by the user.
	 */
	private boolean persistent = false;

	private transient List<PluginIOListener> listeners;

	private void checkListsInitialized() {
		if (listeners == null)
			listeners = new CopyOnWriteArrayList<>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#addListener(pipeline.data.PluginIOListener)
	 */
	@Override
	public void addListener(PluginIOListener listener) {
		checkListsInitialized();
		if (listener == null) {
			Utils.log("Warning: setting a null listener", LogLevel.WARNING);
		}
		if (!listeners.contains(listener))
			listeners.add(listener);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#removeListener(pipeline.data.PluginIOListener)
	 */
	@Override
	public void removeListener(PluginIOListener listener) {
		checkListsInitialized();
		while (listeners.remove(listener)) {
		}
	}

	transient protected boolean silenceUpdates = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#fireValueChanged(boolean, boolean)
	 */
	@Override
	public synchronized void fireValueChanged(boolean stillChanging, boolean dontClearProtobuf) {
		if (silenceUpdates)
			return;
		if (!dontClearProtobuf)
			setProtobuf(null);// some value changed, so protobuf will need to be recomputed
		if (listeners == null)
			return;
		Iterator<PluginIOListener> it = listeners.iterator();
		while (it.hasNext()) {
			try {
				PluginIOListener l = it.next();
				if (l != null) {
					if (l instanceof PluginIOListenerWeakRef) {
						if (((PluginIOListenerWeakRef) l).actionListenerDelegate.get() == null) {
							// The client has been garbage collected, so the listener is of no use anymore
							it.remove();
							continue;
						}
					}
					l.pluginIOValueChanged(stillChanging, this);
				}
			} catch (Exception e) {
				Utils.printStack(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#firePluginIOViewEvent(pipeline.GUI_utils.PluginIOView, boolean)
	 */
	@Override
	public void firePluginIOViewEvent(PluginIOView trigger, boolean stillChanging) {
		if (listeners == null)
			return;
		Iterator<PluginIOListener> it = listeners.iterator();
		while (it.hasNext()) {
			PluginIOListener l = it.next();
			if (l != null) {
				if (l instanceof PluginIOListenerWeakRef) {
					if (((PluginIOListenerWeakRef) l).actionListenerDelegate.get() == null) {
						// The client has been garbage collected, so the listener is of no use anymore
						it.remove();
						continue;
					}
				}
				try {
					l.pluginIOViewEvent(trigger, stillChanging, null);
				} catch (Exception e) {
					Utils.printStack(e);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#setDescription(pipeline.data.InputOutputDescription)
	 */
	@Override
	public void setDescription(InputOutputDescription description) {
		this.description = description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#getDescription()
	 */
	@Override
	@DoNotShowInTable
	public InputOutputDescription getDescription() {
		return description;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#getName()
	 */
	@Override
	@DoNotShowInTable
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#setLastTimeModified(long)
	 */
	@Override
	public void setLastTimeModified(long lastTimeModified) {
		this.lastTimeModified = lastTimeModified;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#getLastTimeModified()
	 */
	@Override
	@DoNotShowInTable
	public long getLastTimeModified() {
		return lastTimeModified;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#setProtobuf(byte[])
	 */
	@Override
	public void setProtobuf(byte[] protobuf) {
		this.protobuf = protobuf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#getProtobuf()
	 */
	@Override
	@DoNotShowInTable
	public byte[] getProtobuf() {
		return protobuf;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#setPersistent(boolean)
	 */
	@Override
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#isPersistent()
	 */
	@Override
	@DoNotShowInTable
	public boolean isPersistent() {
		return persistent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#setDerivation(java.lang.String)
	 */
	@Override
	public void setDerivation(String derivation) {
		this.derivation = derivation;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOInterface#getDerivation()
	 */
	@Override
	@DoNotShowInTable
	public String getDerivation() {
		return derivation;
	}

	/**
	 * Any plugin that is using this object as an input and does not want it modified until it is done
	 * should register its thread in lockingThreads.
	 * 
	 * @param lockingThreads
	 */
	@Override
	public void setLockingThreads(LinkedList<Thread> lockingThreads) {
		this.lockingThreads = lockingThreads;
	}

	/**
	 * Any plugin that is using this object as an input and does not want it modified until it is done
	 * should register its thread in lockingThreads.
	 */
	@Override
	@DoNotShowInTable
	public LinkedList<Thread> getLockingThreads() {
		return lockingThreads;
	}

	@Override
	public void setIsUpdating(AtomicBoolean isUpdating) {
		this.isUpdating = isUpdating;
	}

	@Override
	@DoNotShowInTable
	public AtomicBoolean getIsUpdating() {
		return isUpdating;
	}

	/**
	 * True if it's OK for the pipeline to discard associated update event if updates are already underway.
	 */
	public boolean updatesCanBeCoalesced;

	private String diskLocation;

	@Override
	@DoNotShowInTable
	public String getDiskLocation() {
		return diskLocation;
	}

	@Override
	public void setDiskLocation(String string) {
		this.diskLocation = string;
	}

	private transient List<WeakReference<ListOfPointsView<?>>> weakReferencesToViews;

	// TODO Make this more general; it should apply to all views (including images)
	@Override
	@DoNotShowInTable
	public List<ListOfPointsView<?>> getViews() {
		if (weakReferencesToViews == null)
			return new ArrayList<>(0);
		List<ListOfPointsView<?>> result = new ArrayList<>(weakReferencesToViews.size());
		Iterator<WeakReference<ListOfPointsView<?>>> it = weakReferencesToViews.iterator();
		while (it.hasNext()) {
			WeakReference<ListOfPointsView<?>> weakRef = it.next();
			if (weakRef != null)
				result.add(weakRef.get());
			else
				it.remove();
		}
		return result;
	}

	// TODO Make this more general; it should apply to all views (including images)
	@Override
	public void addView(ListOfPointsView<?> view) {
		if (weakReferencesToViews == null)
			weakReferencesToViews = new ArrayList<>();
		weakReferencesToViews.add(new WeakReference<ListOfPointsView<?>>(view));
	}

	public void parseOrReallocate() {
	}

	@Override
	public IPluginIO duplicateStructure() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public PluginIOView createView() {
		throw new RuntimeException("Not implemented");
	}

}
