/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import java.awt.Toolkit;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

import pipeline.misc_util.Utils.LogLevel;

public class DefaultExceptionHandler implements UncaughtExceptionHandler {
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		Toolkit.getDefaultToolkit().beep();
		Utils.log("Uncaught exception; details follow", LogLevel.ERROR);
		Utils.log(sw.toString(), LogLevel.ERROR);
	}
}
