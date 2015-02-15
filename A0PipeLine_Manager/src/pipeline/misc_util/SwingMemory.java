/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.ActionMap;
import javax.swing.MenuSelectionManager;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import pipeline.misc_util.Utils.LogLevel;

/**
 * Copied from http://forums.yourkit.com/viewtopic.php?f=3&t=737
 * 
 * @author The person that submitted Bug Parade #<a
 *         href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4907798">4907798<a>
 * @author <a href="http://shurl.org/ncOON" target="_blank" onclick=
 *         "w=window.open('http://shurl.org/ncOON', 'E-Mail Address Decryption', 'width=760,height=480,resizable=yes'); w.focus (); return false;"
 *         >Greg Kedge</a>
 * @since 1.0 (Oct 29, 2006)
 */
public class SwingMemory {

	private static final Object[] nullAry = new Object[] { null };

	public static enum MemoryDump {
		NONE, YOUR_KIT, HPROF
	}

	/**
	 * This utility method assists with obtaining an accurate memory capture
	 * when hunting down memory leaks within a Swing application. There are
	 * places within Swing where references to components are retained that cloud
	 * a memory capture report. By freeing up the references to these objects,
	 * it may be easier to hone in on real memory leak problems...
	 *
	 * @param memoryDump
	 *            Should the memory dump be performed and if so, by
	 *            YourKit or HPROF(exercise for the JDK 6.0 user. Check out Alan Bateman's
	 *            <a href="http://blogs.sun.com/alanb/date/20050919">log entry<a> on how
	 *            to do that.
	 */
	public static void swingGC(MemoryDump memoryDump) {
		assert EventQueue.isDispatchThread();

		clearFocusOwners();
		clearRealOpposites();
		clearTemoraryLostComponent();
		cleanupJPopupMenuGlobals(true);
		cleanupJMenuBarGlobals();

		/*
		 * for (int ii = 0; ii < 4; ii++) {
		 * try {
		 * System.gc();
		 * // I want GC to run at least 4 times and I want to prevent
		 * // anyting associated with Swing from running during the time I
		 * // am GC'ing. So, I am purposely typing up the EDT for the
		 * // duration.
		 * Thread.sleep(2000);
		 * Runtime.getRuntime().runFinalization();
		 * Thread.sleep(500);
		 * } catch (InterruptedException e) {
		 * log.log(Level.SEVERE, "Sleeping sickeness", e);
		 * }
		 * }
		 * if (memoryDump.equals(MemoryDump.YOUR_KIT)) {
		 * try {
		 * final Controller c = new Controller();
		 * c.captureMemorySnapshot();
		 * }
		 * catch (Exception e) {
		 * log.log(Level.SEVERE, "YourKit hurled", e);
		 * }
		 * }
		 */
	}

	// Java 5 memory leak bug fix hack for releasing mem ref in
	// KeyBoardFocusManager
	private static void clearFocusOwners() {
		setPrivateFieldToNull("java.awt.KeyboardFocusManager", "newFocusOwner", null);
		setPrivateFieldToNull("java.awt.KeyboardFocusManager", "permanentFocusOwner", null);
	}

	// Java 5 memory leak bug fix hack for relasing mem ref in
	// KeyBoardFocusManager
	private static void clearRealOpposites() {
		final KeyboardFocusManager currentFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		setPrivateFieldToNull("java.awt.DefaultKeyboardFocusManager", "realOppositeWindow", currentFocusManager);
		setPrivateFieldToNull("java.awt.DefaultKeyboardFocusManager", "realOppositeComponent", currentFocusManager);
	}

	private static void cleanupJPopupMenuGlobals(boolean removeOnlyMenuKeyboardHelpers) {
		try {
			MenuSelectionManager aMenuSelectionManager = MenuSelectionManager.defaultManager();
			Object anObject =
					getPrivateField("javax.swing.MenuSelectionManager", "listenerList", aMenuSelectionManager);
			if (null != anObject) {
				EventListenerList anEventListenerList = (EventListenerList) anObject;
				Object[] listeners = anEventListenerList.getListenerList();

				if (removeOnlyMenuKeyboardHelpers) {
					// This gives us back an Array and the even entries are the
					// class type. In this case they are all
					// javax.swing.event.ChangeListeners. The odd number entries
					// are the instance themselves. We were having a problem
					// just blindly removing all of the listeners because the
					// next time a popupmenu was show, it wasn't getting dispose
					// (i.e you right click and click off to cancel and the menu
					// doesn't go away). We traced the memory leak down to this
					// javax.swing.plaf.basic.BasicPopupMenuUI$MenuKeyboardHelper
					// holding onto an instance of the JRootPane. Therefore we
					// just remove all of the instances of this class and it
					// cleans up fine and seems to work.
					Class<?> aClass = Class.forName("javax.swing.plaf.basic.BasicPopupMenuUI$MenuKeyboardHelper");
					for (int i = listeners.length - 1; i >= 0; i -= 2) {
						if (aClass.isInstance(listeners[i]))
							aMenuSelectionManager.removeChangeListener((ChangeListener) listeners[i]);
					}
				} else {
					for (int i = listeners.length - 1; i >= 0; i -= 2)
						aMenuSelectionManager.removeChangeListener((ChangeListener) listeners[i]);
				}
			}
		} catch (Exception e) {
			Utils.printStack(e);
		}

		try {
			ActionMap anActionMap = (ActionMap) UIManager.getLookAndFeelDefaults().get("PopupMenu.actionMap");
			while (anActionMap != null) {
				Object[] keys = { "press", "release" };
				boolean anyFound = false;
				for (Object aKey : keys) {
					Object aValue = anActionMap.get(aKey);
					anyFound = anyFound || aValue != null;
					anActionMap.remove(aKey);
				}
				if (!anyFound)
					break;
				anActionMap = anActionMap.getParent();
			}
		} catch (Exception e) {
			Utils.printStack(e);
		}

		final Object menuKeyboardHelper =
				getPrivateField("javax.swing.plaf.basic.BasicPopupMenuUI", "menuKeyboardHelper", null);
		if (menuKeyboardHelper != null) {
			setPrivateFieldToNull("javax.swing.plaf.basic.BasicPopupMenuUI$MenuKeyboardHelper", "lastFocused",
					menuKeyboardHelper);
		}
		// I don't think this whole think has to be whacked, just lastFocused
		// above...
		// setPrivateFieldToNull("javax.swing.plaf.basic.BasicPopupMenuUI",
		// "menuKeyboardHelper", null);

		Object anObject =
				getPrivateField("com.sun.java.swing.plaf.windows.WindowsPopupMenuUI", "mnemonicListener", null);
		if (null != anObject)
			setPrivateFieldToNull(anObject.getClass(), "repaintRoot", anObject);
	}

	private static void cleanupJMenuBarGlobals() {
		setPrivateFieldToNull("com.sun.java.swing.plaf.windows.WindowsRootPaneUI$AltProcessor", "root", null);
		setPrivateFieldToNull("com.sun.java.swing.plaf.windows.WindowsRootPaneUI$AltProcessor", "winAncestor", null);
	}

	private static void clearTemoraryLostComponent() {
		try {
			final Method getTemporaryLostComponentMethod = Window.class.getDeclaredMethod("getTemporaryLostComponent");
			// PRIVATE! WHO CARES! scarry...
			getTemporaryLostComponentMethod.setAccessible(true);

			final Method setTemporaryLostComponentMethod =
					Window.class.getDeclaredMethod("setTemporaryLostComponent", Component.class);
			// PRIVATE! WHO CARES! scarry...
			setTemporaryLostComponentMethod.setAccessible(true);

			for (Frame frame : Frame.getFrames()) {
				Object o = getTemporaryLostComponentMethod.invoke(frame);

				if (o != null) {
					// Null out frame's temporaryLostComponent field...
					// Don't use null for vararg; won't work!
					setTemporaryLostComponentMethod.invoke(frame, nullAry);

				}
				for (Window ownedWindow : frame.getOwnedWindows()) {
					o = getTemporaryLostComponentMethod.invoke(ownedWindow);

					if (o != null) {
						// Null out frame's temporaryLostComponent field...
						// Don't use null for vararg; won't work!
						setTemporaryLostComponentMethod.invoke(ownedWindow, nullAry);

					}
				}
			}
		} catch (Exception e) {
			Utils.printStack(e);
		}
	}

	private static void setPrivateFieldToNull(Class<?> aClass, String aFieldName, Object anObject) {
		try {
			final Object o = getPrivateField(aClass, aFieldName, anObject);

			if (o != null) {
				final Field aField = aClass.getDeclaredField(aFieldName);
				aField.setAccessible(true);
				aField.set(anObject, null);

			}
		} catch (Exception e) {
			final StringBuilder sb = new StringBuilder("Can't set private field: ");
			sb.append(aClass.getName()).append('.').append(aFieldName).append(" on: ");
			if (anObject == null)
				sb.append("<static>");
			else {
				sb.append(anObject.getClass().getName()).append('@').append(Integer.toHexString(anObject.hashCode()));
			}
			Utils.log(sb.toString(), LogLevel.ERROR);
		}
	}

	private static void setPrivateFieldToNull(String aClassName, String aFieldName, Object anObject) {
		try {
			Class<?> aClass = Class.forName(aClassName);
			setPrivateFieldToNull(aClass, aFieldName, anObject);
		} catch (Exception e) {
			final StringBuilder sb = new StringBuilder("Can't set private field: ");
			sb.append(aClassName).append('.').append(aFieldName).append(" on: ");
			if (anObject == null)
				sb.append("<static>");
			else {
				sb.append(anObject.getClass().getName()).append('@').append(Integer.toHexString(anObject.hashCode()));
			}
			Utils.log(sb.toString(), LogLevel.ERROR);
		}
	}

	private static Object getPrivateField(String aClassName, String aFieldName, Object anObject) {
		try {
			return getPrivateField(Class.forName(aClassName), aFieldName, anObject);
		} catch (Exception e) {
			final StringBuilder sb = new StringBuilder("Can't get private field: ");
			sb.append(aClassName).append('.').append(aFieldName).append(" on: ");
			if (anObject == null)
				sb.append("<static>");
			else {
				sb.append(anObject.getClass().getName()).append('@').append(Integer.toHexString(anObject.hashCode()));
			}
			Utils.log(sb.toString(), LogLevel.ERROR);
			return null;
		}
	}

	private static Object getPrivateField(Class<?> aClass, String aFieldName, Object anObject) {
		try {
			Field aField = aClass.getDeclaredField(aFieldName);
			aField.setAccessible(true);
			return aField.get(anObject);
		} catch (Exception e) {
			final StringBuilder sb = new StringBuilder("Can't get private field: ");
			sb.append(aClass.getName()).append('.').append(aFieldName).append(" on: ");
			if (anObject == null)
				sb.append("<static>");
			else {
				sb.append(anObject.getClass().getName()).append('@').append(Integer.toHexString(anObject.hashCode()));
			}
			Utils.log(sb.toString(), LogLevel.ERROR);
			return null;
		}
	}
}
