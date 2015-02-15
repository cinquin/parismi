/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

/**
 * Interface implemented by plugins that have more than one input or more than one output.
 * Plugins specify names for those inputs and outputs, so users know how to match outputs from
 * one plugin to outputs from another plugin.
 *
 */

public interface AuxiliaryInputOutputPlugin {
	/**
	 * 
	 * @return Array of strings of arbitrary length (0 allowed) that contain input label names. Best to keep them short
	 *         for them to be easily read by the user while displayed in a small area.
	 */
	public String[] getInputLabels();

	/**
	 * 
	 * @return Array of strings of arbitrary length (0 allowed) that contain output label names. Best to keep them short
	 *         for them to be easily read by the user while displayed in a small area.
	 */
	public String[] getOutputLabels();

}
