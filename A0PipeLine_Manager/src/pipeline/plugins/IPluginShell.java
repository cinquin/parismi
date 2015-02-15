/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.plugins;

/**
 * Interface implemented by the IPluginShell plugin, which takes care of hiding the complexity of the
 * source dataset to plugins that do not need to be aware of it.
 *
 */
public interface IPluginShell {
	/**
	 * Sets the plugin that the shell should manage.
	 * 
	 * @param plugInstance
	 *            Managed plugin.
	 */
	void setPlugin(PipelinePlugin plugInstance);
}
