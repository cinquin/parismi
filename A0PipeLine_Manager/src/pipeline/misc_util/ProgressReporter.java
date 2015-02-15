/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

public interface ProgressReporter {

	public void setValue(int value);

	public void setValueThreadSafe(int value);

	public int getValue();

	public void setIndeterminate(boolean indeterminate);

	public boolean isIndeterminate();

	public void setMin(int min);

	public void setMax(int max);

}
