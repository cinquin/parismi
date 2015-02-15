/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

public interface IDimensions {

	public void setWidth(int width);

	public int getWidth();

	public int getHeight();

	public void setHeight(int height);

	public int getDepth();

	public void setDepth(int depth);

}
