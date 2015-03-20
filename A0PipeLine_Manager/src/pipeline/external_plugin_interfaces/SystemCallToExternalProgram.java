/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.external_plugin_interfaces;

import ij.IJ;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import pipeline.misc_util.ProgressReporter;
import pipeline.misc_util.Utils;
import pipeline.plugins.SpecialDimPlugin;

/**
 * Implements a link to an external program provided by a system call.
 *
 */
public class SystemCallToExternalProgram extends LinkToExternalProgram {

	transient private Process process = null;
	transient private DataInputStream processOutput = null;
	transient private DataOutputStream processInput = null;
	transient private BufferedReader bufferedOutput = null;

	private List<String> programNameAndArguments = null;
	transient private volatile AtomicBoolean isRunning = new AtomicBoolean(false);

	@Override
	public void establish(List<String> programNameAndArguments, SpecialDimPlugin dimensionAccessor) {
		this.programNameAndArguments = programNameAndArguments;
		try {
			ProcessBuilder pb = new ProcessBuilder(programNameAndArguments);
			pb.redirectErrorStream(true);
			process = pb.start();
		} catch (IOException e1) {
			throw new RuntimeException("Error in system call: " + programNameAndArguments + " detail: "
					+ e1.getMessage());
		}
		processOutput = new DataInputStream(process.getInputStream());
		processInput = new DataOutputStream(process.getOutputStream());
		bufferedOutput = new BufferedReader(new InputStreamReader(processOutput));
	}

	@Override
	public void run(final String[] arguments, final boolean keepAlive, final boolean blockUntilDone,
			final ProgressReporter progress, SpecialDimPlugin dimensionAccessor) {
		synchronized (isRunning) {
			if (isRunning.get()) {
				throw new RuntimeException("Run external program synchronzation problem");
			}
			isRunning.set(true);

			boolean processStillAlive = stillAlive();

			if (!processStillAlive) {
				isRunning.set(false);
				establish(programNameAndArguments, null);
			}

			Thread runThread = new Thread("SystemCallToExternalProgram " + programNameAndArguments) {
				@Override
				public void run() {
					isComputing = true;
					try {
						processInput.writeBytes(Utils.printStringArray(arguments, " "));
						processInput.flush();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						IJ.log("error while writing arguments " + Utils.printStringArray(arguments, " ")
								+ " to program " + programNameAndArguments);
						Utils.printStack(e);
					}
					int intProgress = 0;
					String progressString = null;
					try {
						while (intProgress < 100) {
							progressString = bufferedOutput.readLine();
							if (progressString == null) {
								IJ.log("Null output from program; it probably died; exiting");
								break;
							}
							if (!Utils.isParsableToFloat(progressString)) {
								IJ.log("______" + progressString + " not parsable to float, output by program "
										+ programNameAndArguments);
							} else {
								intProgress = (int) Math.floor(Float.parseFloat(progressString));
								progress.setValue(intProgress);
							}
						}
					} catch (IOException e) {
						IJ.log("Error while reading progress of program " + programNameAndArguments + " found "
								+ progressString + " " + e.getMessage());
					}
					isComputing = false;
					lastActiveTime = System.currentTimeMillis();
				}

				@Override
				protected void finalize() {
					try {
						if (process != null)
							process.destroy();
					} finally {
						try {
							super.finalize();
						} catch (Throwable e) {
							Utils.printStack(e);
						}
					}
				}
			};
			runThread.start();
			if (blockUntilDone)
				try {
					runThread.join();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();// so that our calling plugin knows we were interrupted
					// and doesn't try to read a non-existing or corrupted output
					IJ.log("Interrupted exception in " + "SystemCallToExternalProgram " + programNameAndArguments
							+ "; interrupting process and returning");
					e.printStackTrace();
					try {
						processInput.writeBytes("I\n");
						processInput.flush();
					} catch (IOException e2) {
						IJ.log("Exception will interrupting program " + programNameAndArguments + " detail: "
								+ e2.getMessage());
						e2.printStackTrace();
					}
				}
			isRunning.set(false);
		}
	}

	@SuppressWarnings("null")
	@Override
	public void terminate(boolean blockUntilTerminated) {
		if (process != null) {
			synchronized (this) {
				if (process != null) {
					process.destroy();
					process = null;
				}
			}
		}
	}

	@Override
	public void interrupt() {
		try {
			processInput.writeBytes("I\n");
			processInput.flush();
		} catch (IOException e) {
			throw new RuntimeException("Error in interrupt of process " + programNameAndArguments);
		}
	}

	@Override
	public boolean stillAlive() {
		boolean processStillAlive = false;
		if (process != null) {
			try {
				@SuppressWarnings("unused")
				int exitValue = process.exitValue();
				processStillAlive = false;// if the process is still alive, an exception is thrown by the previous line
				process = null;
			} catch (IllegalThreadStateException e) {
				// process is still running
				processStillAlive = true;
			}
		} else
			processStillAlive = false;
		return processStillAlive;
	}

	@Override
	public void terminateForcibly() {
		if (process != null) {
			process.destroy();
			process = null;
		}
	}

}
