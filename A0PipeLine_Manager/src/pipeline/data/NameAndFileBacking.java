/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

/**
 * Just a struct to describe channel names, where they are stored on disk, and the times of storage and last
 * modification.
 * 
 * @see SingleChannelView
 */
public class NameAndFileBacking {

	public String[] channelNames;
	public String[] filePaths;
	public long[] timesStored;
	public long[] timesModified;

}
