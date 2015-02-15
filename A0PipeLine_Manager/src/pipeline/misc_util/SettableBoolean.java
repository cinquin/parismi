/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

/**
 * Used for methods that need to return more than one result, one of which is a boolean. The boolean is passed as a
 * SettableBoolean parameter, set
 * by the method, and read by the caller after the method returns.
 *
 */
public class SettableBoolean {
	public boolean value;

}
