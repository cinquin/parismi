/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline;

import pipeline.misc_util.PipelineROI;

/**
 * Class that describes the subset of data that a plugin should work on to generate a quick preview, most likely
 * triggered from the GUI by a user who is exploring different parameter values. If doable, the plugin should
 * compute a rough estimate of its output rather than taking time to compute an exact result.
 * 
 * @see pipeline.misc_util.PipelineROI
 */
public class PreviewType {
	/**
	 * Region in time and space of the input dataset that the plugin should work on.
	 */
	public PipelineROI roiToUpdate;
	/**
	 * Degree of detail desired in the output, on a scale from 1 to 100, 1 corresponding to the roughest and quickest
	 * possible output, and 100 to the normal output of the plugin.
	 */
	public int fineness;
}
