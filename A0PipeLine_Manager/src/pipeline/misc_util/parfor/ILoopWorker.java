/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util.parfor;

@FunctionalInterface
public interface ILoopWorker {
	public Object run(int loopIndex, int threadIndex) throws InterruptedException;
}
