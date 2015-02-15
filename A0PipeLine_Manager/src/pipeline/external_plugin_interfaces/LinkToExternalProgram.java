/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.external_plugin_interfaces;

import java.util.List;

import pipeline.misc_util.ProgressReporter;
import pipeline.plugins.SpecialDimPlugin;

/**
 * Abstract class that describes a generic link between a plugin and an external program.
 *
 */
public abstract class LinkToExternalProgram {

	/**
	 * Establish the link to the external program (e.g. make a system call).
	 * 
	 * @param programNameAndArguments
	 *            First element of the list is the program name (e.g. the name of the executable).
	 * @param dimensionAccessor
	 *            TODO
	 * @throws InterruptedException
	 */
	public abstract void establish(List<String> programNameAndArguments, SpecialDimPlugin dimensionAccessor)
			throws InterruptedException;

	/**
	 * Run the program (probably in response to the pipeline calling the plugin to run), and monitor its progress.
	 * 
	 * @param arguments
	 *            Arguments to be passed to the program for this specific run
	 * @param keepAlive
	 *            Whether the connection to the program should be kept alive after the program is done with this
	 *            particular computation.
	 * @param blockUntilDone
	 *            If true, the call to "run" will not return until the external program has finished its computation
	 * @param progress
	 *            A listener to call when the external program reports it has progressed.
	 * @param dimensionAccessor
	 *            TODO
	 */
	public abstract void run(String[] arguments, boolean keepAlive, boolean blockUntilDone, ProgressReporter progress,
			SpecialDimPlugin dimensionAccessor);

	/**
	 * Terminate the external program.
	 * 
	 * @param blockUntilTerminated
	 *            If true, the call to "terminate" does not return until the external program has been effectively
	 *            terminated.
	 */
	public abstract void terminate(boolean blockUntilTerminated);

	/**
	 * Terminate the external program by killing the thread or process it runs in.
	 */
	public abstract void terminateForcibly();

	/**
	 * Sends a signal to the external program to tell it to interrupt its current computation (e.g. if the pipeline
	 * decides to immediately force
	 * an update because a parameter has been changed).
	 */
	public abstract void interrupt();

	/**
	 * 
	 * @return True if the link is still alive (e.g. if the system call has not returned).
	 */
	public abstract boolean stillAlive();

	volatile transient boolean isComputing = false;

	public boolean isComputing() {
		return isComputing;
	}

	private long usedMemorySize = 0;

	/**
	 * 
	 * @return Number of bytes the external program is currently holding in memory. Used by the pipeline to determine
	 *         which links to terminate
	 *         when memory needs to be freed up.
	 */
	public long getCurrentMemoryUsage() {
		return usedMemorySize;
	}

	long lastActiveTime = 0;

	/**
	 * 
	 * @return Last system time (in milliseconds) run returned from this link. Used by the pipeline to determine which
	 *         have not been active
	 *         for a long time and should be terminated.
	 */
	public long getLastTimeActive() {
		return lastActiveTime;
	}
}
