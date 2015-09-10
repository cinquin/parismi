/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jdom.Document;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;

public interface IPluginIO {

	/**
	 * Stores or updates object to a file and returns a reference to that file.
	 * 
	 * @param saveTo
	 *            file to save to; if null, create a temporary file and return it.
	 * @param useBigTIFF
	 *            TODO
	 * @return File containing a representation of the object.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@NonNull File asFile(@Nullable File saveTo, boolean useBigTIFF) throws IOException, InterruptedException;

	Document getMetadata();

	void setMetadata(Document metadata);

	/**
	 * For now, used by #PluginHyperstacks to allow to access individual subchannels
	 * 
	 * @return A list of strings describing individually-accessible parts of this input/output
	 * 
	 */
	@NonNull String @NonNull[] listOfSubObjects();

	void copyInto(IPluginIO outputCells);

	Object getProperty(String string);

	byte @Nullable[] asProtobufBytes();

	void restoreFromProtobuf() throws NotRestorableFromProtobuf, InterruptedException;

	void setProperty(String string, Object value);

	void addListener(PluginIOListener listener);

	void removeListener(PluginIOListener listener);

	void fireValueChanged(boolean stillChanging, boolean dontClearProtobuf);

	void firePluginIOViewEvent(PluginIOView trigger, boolean stillChanging);

	/**
	 * @param description
	 *            description that led to this object being created by IPluginShell; can be null.
	 */
	void setDescription(InputOutputDescription description);

	/**
	 * @return description that led to this object being created by IPluginShell; can be null.
	 */
	InputOutputDescription getDescription();

	void setName(@NonNull String name);

	@NonNull String getName();

	void setLastTimeModified(long lastTimeModified);

	long getLastTimeModified();

	void setProtobuf(byte @Nullable[] protobuf);

	byte @Nullable[] getProtobuf();

	/**
	 * @param persistent
	 *            Set to true for inputs that should not be recomputed when the pipeline is restored.
	 *            For example when a ROI has been specified by the user.
	 */
	void setPersistent(boolean persistent);

	/**
	 * @return true for inputs that should not be recomputed when the pipeline is restored.
	 *         For example when a ROI has been specified by the user.
	 */
	boolean isPersistent();

	/**
	 * @param derivation
	 *            Description of how the user specified that IO in the pipeline (e.g. by naming an open window, by
	 *            a relative reference to another row, or by absolute reference)
	 */
	void setDerivation(String derivation);

	/**
	 * @return Description of how the user specified that IO in the pipeline (e.g. by naming an open window, by
	 *         a relative reference to another row, or by absolute reference)
	 */
	String getDerivation();

	LinkedList<Thread> getLockingThreads();

	void setLockingThreads(LinkedList<Thread> lockingThreads);

	void setIsUpdating(AtomicBoolean isUpdating);

	AtomicBoolean getIsUpdating();

	/**
	 * @return File on disk the PluginIO was loaded from, if any. That file is not guaranteed to reflect
	 *         modifications that have been made after it was loaded.
	 */
	String getDiskLocation();

	/**
	 * 
	 * @param diskLocation
	 *            File on disk the PluginIO was loaded from, if any. That file is not guaranteed to reflect
	 *            modifications that have been made after it was loaded.
	 */
	void setDiskLocation(String diskLocation);

	void setInputForRows(List<Integer> list);

	void setOutputForRows(List<Integer> list);

	IPluginIO duplicateStructure();

	PluginIOView createView();

	List<ListOfPointsView<?>> getViews();

	void addView(ListOfPointsView<?> view);
	
	boolean defaultToNoSaving();

}
