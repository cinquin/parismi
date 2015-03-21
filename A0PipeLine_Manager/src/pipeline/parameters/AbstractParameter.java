/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameters;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.eclipse.jdt.annotation.Nullable;

import pipeline.misc_util.Pair;
import pipeline.misc_util.ParameterListenerWeakRef;
import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;

/**
 * Abstract class that is used to allow the pipeline and the user to manipulate parameters given to the
 * plugins. Parameters can be of any type (e.g. integers, floats, booleans, selections with lists, whole tables, etc.).
 * To be displayed by the pipeline in the GUI (and therefore to be manipulable by the user), each parameter must have an
 * associated class that inherits from AbstractParameterCellView.
 *
 */
public abstract class AbstractParameter implements Serializable {

	private static final long serialVersionUID = 1896563175748302286L;

	public AbstractParameter(ParameterListener l, Object creatorReference) {
		pluginParameterListeners = new LinkedList<>();
		GUIParameterListeners = new LinkedList<>();
		if (l != null)
			pluginParameterListeners.add(l);
		this.creatorReference = creatorReference;
	}

	public AbstractParameter() {
		pluginParameterListeners = new LinkedList<>();
		GUIParameterListeners = new LinkedList<>();
	}

	/**
	 * Used by plugins so they know whether they should try to
	 * set sensible default values for the parameter, or whether they should stick
	 * with what is already there because the user has explicitly set those values.
	 */
	public boolean hasBeenSet = false;

	/**
	 * The plugin listeners are used by the parameter to notify the plugin instance(s) that own(s) it when its value has
	 * been modified (most probably as a result of user interaction). The plugin instances should immediately update any internal
	 * variables that depend on the parameter, and ask the pipeline to be run again if appropriate.
	 * This listener list a transient variable because it is not useful to store the listeners, and the listeners are
	 * likely to hold references to huge image data structures.
	 */

	private transient LinkedList<ParameterListener> pluginParameterListeners;

	/**
	 * The GUI listeners are used by the parameter to notify GUI objects that the contents of the parameter have been
	 * updated (e.g. if a plugin  modified the contents of a text box) so they update the visual representation.
	 * This listener list a transient variable because it is not useful to store the listeners, and the listeners are
	 * likely to hold references to huge image data structures.
	 */
	private transient LinkedList<ParameterListener> GUIParameterListeners;

	/**
	 * Initialize the listener lists if they are null. This is necessary because if the instance of the
	 * parameter was created by deserialization the listener lists will be null (and not just empty)/
	 * Another way of implementing this would be to create a function that instantiates lists upon
	 * deserialization.
	 */
	private void checkListsInitialized() {
		if (pluginParameterListeners == null)
			pluginParameterListeners = new LinkedList<>();
		if (GUIParameterListeners == null)
			GUIParameterListeners = new LinkedList<>();
		sanitizeListeners(GUIParameterListeners);
		sanitizeListeners(pluginParameterListeners);
	}

	/**
	 * Add a listener to the parameter. Null permissible. Does not do anything if listener is already registered.
	 * 
	 * @param listener
	 *            Listener that should be a pipeline plugin.
	 */
	public void addPluginListener(@Nullable ParameterListener listener) {
		checkListsInitialized();
		if (listener == null) {
			Utils.log("Warning: setting a null listener", LogLevel.WARNING);
		}
		synchronized (pluginParameterListeners) {
			if (!pluginParameterListeners.contains(listener))
				pluginParameterListeners.add(listener);
		}
	}

	private static void sanitizeListeners(LinkedList<ParameterListener> list) {
		synchronized (list) {
			LinkedList<ParameterListener> newList = new LinkedList<>();
			for (ParameterListener l : list) {
				if (l instanceof ParameterListenerWeakRef) {
					ParameterListenerWeakRef lwr = (ParameterListenerWeakRef) l;
					if (lwr.getDelegate() != null) {
						newList.add(lwr);
					} else {
						// Utils.log("Removed listener with null delegate", LogLevel.VERBOSE_VERBOSE_DEBUG);
					}
				} else
					newList.add(l);
			}
			list.clear();
			list.addAll(newList);
		}
	}

	private static boolean alreadyListening(ParameterListener pl, LinkedList<ParameterListener> list) {
		synchronized (list) {
			for (ParameterListener l : list) {
				if (pl == l)
					return true;
				if (l instanceof ParameterListenerWeakRef) {
					if (((ParameterListenerWeakRef) l).getDelegate() == pl)
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Add a listener to the parameter. Null permissible.
	 * 
	 * @param listener
	 *            Listener that should be a GUI representation of the parameter.
	 */
	public void addGUIListener(ParameterListener listener) {
		checkListsInitialized();
		if (listener == null) {
			Utils.log("Warning: setting a null listener", LogLevel.WARNING);
		}

		synchronized (GUIParameterListeners) {
			if (!alreadyListening(listener, GUIParameterListeners))
				GUIParameterListeners.add(new ParameterListenerWeakRef(listener));
		}
	}

	/**
	 * Removes a parameter listener
	 * 
	 * @param listener
	 *            Listener to be removed. If it present in multiple copies, all are removed.
	 */
	public void removeListener(ParameterListener listener) {
		checkListsInitialized();
		synchronized (pluginParameterListeners) {
			while (pluginParameterListeners.remove(listener)) {
				Utils.log("Removing plugin listener " + listener, LogLevel.VERBOSE_VERBOSE_DEBUG);
			}
		}
		synchronized (GUIParameterListeners) {
			while (GUIParameterListeners.remove(listener)) {
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void iterateListenersParameterValueChanged(boolean stillChanging, LinkedList<ParameterListener> listeners,
			boolean onlyNotifyIfRequested) {
		if (listeners == null)
			return;
		for (ParameterListener l : ((LinkedList<ParameterListener>) listeners.clone())) {
			try {
				if (l instanceof ParameterListenerWeakRef) {
					if (((ParameterListenerWeakRef) l).actionListenerDelegate.get() == null) {
						// The client has been garbage collected, so the listener is of no use anymore
						synchronized (listeners) {
							listeners.remove(l);
						}
						continue;
					}
				}
				if (l != null) {
					if (!onlyNotifyIfRequested)
						l.parameterValueChanged(stillChanging, this, false);
					else if (l.alwaysNotify())
						l.parameterValueChanged(stillChanging, this, true);
				}
			} catch (Exception e) {
				Utils.log("Ignoring exception generated in listener notification: ", LogLevel.ERROR);
				Utils.printStack(e);
			}
		}
	}

	public void firePropertiesChanged() {
		firePropertiesChanged(GUIParameterListeners);
		firePropertiesChanged(pluginParameterListeners);
	}

	@SuppressWarnings("unchecked")
	public void firePropertiesChanged(LinkedList<ParameterListener> listeners) {
		if (listeners == null)
			return;
		for (ParameterListener l : ((LinkedList<ParameterListener>) listeners.clone())) {
			try {
				if (l instanceof ParameterListenerWeakRef) {
					if (((ParameterListenerWeakRef) l).actionListenerDelegate.get() == null) {
						// The client has been garbage collected, so the listener is of no use anymore
						synchronized (listeners) {
							listeners.remove(l);
						}
						continue;
					}
				}
				if (l != null)
					l.parameterPropertiesChanged(this);
			} catch (Exception e) {
				Utils.log("Ignoring exception generated in listener notification: ", LogLevel.ERROR);
				Utils.printStack(e);
			}
		}
	}

	/**
	 * Called by the GUI class handling the displaying and editing of this parameter when the
	 * value of the parameter has been changed. The parameter in turn notifies its listener. Note that it is possible
	 * for more than 1 call to be made with the same parameter value; the listener might therefore want to check if 
	 * the value really did change.
	 * 
	 * @param stillChanging
	 *            True if the user is still in the process of adjusting the parameter, e.g. if the user is using a
	 *            slider and has not released the mouse. If stillChanging is true, a call will always be made with the
	 *            final value once the parameter is not changing anymore. It is therefore safe to ignore calls where stillChanging is
	 *            true if one does not want to trigger numerous updates while the user is adjusting a parameter.
	 * @param notifyGUIListeners
	 *            If true notify listeners that are GUI representations.
	 * @param notifyParameterListeners
	 *            If true notify listeners that come from plugins.
	 */
	public void fireValueChanged(final boolean stillChanging, boolean notifyGUIListeners,
			boolean notifyParameterListeners) {
		checkListsInitialized();
		SwingUtilities.invokeLater(() -> {
			if (notifyGUIListeners) {
				iterateListenersParameterValueChanged(stillChanging, GUIParameterListeners, false);
			}
			if (notifyParameterListeners)
				iterateListenersParameterValueChanged(stillChanging, pluginParameterListeners, false);
			else {
				iterateListenersParameterValueChanged(stillChanging, pluginParameterListeners, true);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private void
			iterateListenersButtonPressed(String action, LinkedList<ParameterListener> listeners, ActionEvent event) {
		if (listeners == null)
			return;
		for (ParameterListener l : ((LinkedList<ParameterListener>) listeners.clone())) {
			try {
				if (l instanceof ParameterListenerWeakRef) {
					if (((ParameterListenerWeakRef) l).actionListenerDelegate.get() == null) {
						// The client has been garbage collected, so the listener is of no use anymore
						synchronized (listeners) {
							listeners.remove(l);
						}
						continue;
					}
				}
				if (l != null)
					l.buttonPressed(action, this, event);
			} catch (Exception e) {
				Utils.log("Ignoring exception generated in listener notification: ", LogLevel.ERROR);
				Utils.printStack(e);
			}
		}
	}

	/**
	 * Called by the GUI class handling the displaying and editing of this parameter when a
	 * button associated with the parameter display has been pressed. The parameter in turn notifies its listener.
	 * This is used for example to reset a range of possible values to the extremes present in the corresponding image.
	 * 
	 * @param action
	 * @param notifyGUIListeners
	 *            *ONLY* GUI listeners notified if true; not actually used anywhere at this point
	 * @param event
	 *            TODO
	 */
	public void buttonPressed(String action, boolean notifyGUIListeners, ActionEvent event) {
		checkListsInitialized();
		LinkedList<ParameterListener> listeners = notifyGUIListeners ? GUIParameterListeners : pluginParameterListeners;
		if (notifyGUIListeners) {
			synchronized (GUIParameterListeners) {
				iterateListenersButtonPressed(action, listeners, event);
			}
		} else {
			synchronized (pluginParameterListeners) {
				iterateListenersButtonPressed(action, listeners, event);
			}
		}

	}

	/**
	 * If true, valueAsString should be forced to return an empty string.
	 * This is used by the ExternalCall plugin to specify parameters whose values should not be automatically
	 * passed to the external program (e.g. parameters that specify the behavior of the ExternalCall plugin but that
	 * the external program should not be aware of).
	 */
	public boolean dontPrintOutValueToExternalPrograms = false;

	/**
	 * @return The value of the parameter, which can be an array of objects.
	 */
	public abstract Object getValue();

	/**
	 * Set the value of the parameter. This does not trigger a notification to the listeners.
	 * To trigger an notification, {@link #fireValueChanged} must be called explicitly.
	 */
	public abstract void setValue(Object o);

	/**
	 * Tells whether the parameter and the range from which to select should be editable. When only
	 * interested in whether the parameter itself is editable, see {@link #isEditable}.
	 */
	public abstract boolean[] editable();

	boolean editable = true;

	/**
	 * 
	 * @return True if parameter is editable. See {@link #editable} to determine whether the range
	 *         of the parameter can be edited.
	 */
	public boolean isEditable() {
		return editable;
	}

	/**
	 * Sets whether the parameter can be edited from the GUI.
	 * 
	 * @param editable
	 */
	public void setEditable(boolean editable) {
		this.editable = editable;
		firePropertiesChanged(GUIParameterListeners);
	}

	String userDisplayName;
	String explanation;

	/**
	 * @return the name of the parameter to be displayed to the user (element at index 0 in the array),
	 *         and a longer description that is displayed when the mouse hovers the parameter GUI representation
	 *         (element at index 1 in the array).
	 */
	public String[] getParamNameDescription() {
		String[] strings = { userDisplayName, explanation };
		return strings;
	}

	/**
	 * Private storage is used for plugins to store an object associated with the parameter that it is not in a format
	 * that is naturally displayable to the user. For example, a list of clicked points is better stored as an array, even
	 * though it is displayed as text. Note that object storage should be serializable and of a small size (it will be translated to
	 * XML and stored along with pipeline metadata).
	 */

	public Serializable privateStorage;

	void removeAllPluginListeners() {
		if (pluginParameterListeners != null)
			pluginParameterListeners.clear();
	}

	/**
	 * Can be used by the instantiator of the parameter to store a value that can be used in the callbacks the parameter
	 * makes when its value is changed.
	 */
	Object creatorReference;

	private boolean pipelineCalled;

	public boolean isPipelineCalled() {
		return pipelineCalled;
	}

	public void setPipelineCalled(boolean pipelineCalled) {
		this.pipelineCalled = pipelineCalled;
	}

	private boolean stillChanging;

	/**
	 * 
	 * @return True if the parameter is still in the process of changing as a result of user interaction. This is used
	 *         by plugins tracking this parameter to know whether they should stay within a core loop updating their
	 *         output in response to parameter changes.
	 */
	public boolean isStillChanging() {
		return stillChanging;
	}

	/**
	 * 
	 * @param changing
	 *            True if the parameter is still in the process of changing as a result of user interaction. This is
	 *            used by plugins tracking this parameter to know whether they should stay within a core loop updating their
	 *            output in response to parameter changes.
	 */
	public void setStillChanging(boolean changing) {
		stillChanging = changing;
	}

	long timeLastChange = -1;

	private long timeLastResponseToChange = -1;

	public long getTimeLastResponseToChange() {
		return timeLastResponseToChange;
	}

	public void setTimeLastResponseToChange(long timeLastResponseToChange) {
		this.timeLastResponseToChange = timeLastResponseToChange;
	}

	public void setTimeLastChange(long timeLastChange) {
		this.timeLastChange = timeLastChange;
	}

	public long getTimeLastChange() {
		return timeLastChange;
	}

	private Object semaphore = new Object();

	/**
	 * @return Semaphore used to notify listeners that parameter value has changed.
	 */
	public Object getSemaphore() {
		return semaphore;
	}

	transient NumberFormat nf = null;

	void initializeFormatter() {
		if (nf == null) {
			nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(3);
			nf.setMaximumIntegerDigits(10);
			nf.setGroupingUsed(false);
		}
	}

	public String getUserDisplayName() {
		return userDisplayName;
	}

	public void setUserDisplayName(String newName) {
		userDisplayName = newName;
	}

	private String fieldName;

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public abstract boolean valueEquals(Object value);

	@SuppressWarnings("static-method")
	public Object getSimpleValue() {
		throw new RuntimeException("Unimplemented");
	}

	private int displayIndex;

	public int getDisplayIndex() {
		return displayIndex;
	}

	public void setDisplayIndex(int index) {
		displayIndex = index;
	}

	private transient List<WeakReference<Pair<Field, Object>>> boundFields = new ArrayList<>();

	public void addBoundField(Field f, Object o) {
		boundFields.add(new WeakReference<Pair<Field, Object>>(new Pair<Field, Object>(f, o)));
	}

	void setFieldsValue(Object value) {
		Iterator<WeakReference<Pair<Field, Object>>> weakFieldIt = boundFields.iterator();
		while (weakFieldIt.hasNext()) {
			Pair<Field, Object> pair = weakFieldIt.next().get();
			if (pair == null) {
				weakFieldIt.remove();
				continue;
			}
			try {
				pair.getFst().set(pair.getSnd(), value);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected Object readResolve() {
		boundFields = new ArrayList<>();
		return this;
	}

	private boolean compactDisplay;

	public boolean isCompactDisplay() {
		return compactDisplay;
	}

	public void setCompactDisplay(boolean compactDisplay) {
		this.compactDisplay = compactDisplay;
	}
}
