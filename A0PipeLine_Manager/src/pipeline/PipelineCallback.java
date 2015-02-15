/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline;

import java.util.List;

import pipeline.A0PipeLine_Manager.TableSelectionDemo.TableNotComputed;
import pipeline.GUI_utils.PluginIOHyperstackViewWithImagePlus;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.PluginIO;
import pipeline.external_plugin_interfaces.LinkToExternalProgram;
import pipeline.external_plugin_interfaces.RemoteMachine;
import pipeline.parameters.AbstractParameter;

/**
 * Used by plugins to get information from the pipeline.
 *
 */
public interface PipelineCallback {
	/**
	 * Notify the pipeline that a parameter value has changed in row. The pipeline then decides whether and when
	 * to run the plugin corresponding to that row.
	 * 
	 * @param row
	 * @param changedParameter
	 * @param stayInCoreLoop
	 *            True if plugins responding to the change should stay in their core loop and
	 *            update when the parameter changes again; this is used to increase responsiveness to GUI input
	 */
	public void parameterValueChanged(int row, AbstractParameter changedParameter, boolean stayInCoreLoop);

	/**
	 * Ask the pipeline to redraw row. Used for example if the progress value has been changed and the graphical display
	 * needs to be updated.
	 * 
	 * @param row
	 */
	public void redrawLine(int row);

	/**
	 * Ask the pipeline to redraw progress renderer.
	 * 
	 * @param row
	 */
	public void redrawProgressRenderer(int row);

	/**
	 * Ask the pipeline to update the list of channel names from which the user selects the channels the plugin should
	 * work on.
	 * Used by the Z Projector.
	 * Unused so far.
	 * 
	 * @param row
	 */
	public void updateWorkOnChannelField(int row);

	/**
	 * Get the list of auxiliary output ImagePlus of the plugin at row "row". Plugins use this list when they create
	 * more than 1 output ImagePlus.
	 * This callback is normally used by a plugin to get a reference to its own list that it can then update to keep the
	 * pipeline aware of the list. This is used by Plot3DProfile.
	 * 
	 * @param row
	 * @return A list of ImagePlus. The list is guaranteed not to be null.
	 */
	public List<PluginIOHyperstackViewWithImagePlus> getAuxiliaryOutputImps(int row);

	/**
	 * @param lastRow
	 *            last row to include in the string; if -1, include all the table. Table must have been computed up to
	 *            last row
	 *            for the call to be successful.
	 * @return An XML string representation of the pipeline table. Used by SaveTable to save the table to a file.
	 * @throws TableNotComputed
	 */
	public String getTableString(int lastRow) throws TableNotComputed;

	/**
	 * Get a reference to the external program launched by the plugin at row. This is mainly so that the pipeline is
	 * aware of this
	 * program and can terminate it if it likes.
	 * NB: it might not be necessary for the pipeline to be able to terminate this program directly. If it interrupts
	 * the threads
	 * that controls the program, that thread should terminate the program itself.
	 * 
	 * @param row
	 * @return a descriptor of the external program
	 */
	public LinkToExternalProgram getExternalProgram(int row);

	/**
	 * Store a reference to the external program launched by the plugin at row. This is mainly so that the pipeline is
	 * aware of this
	 * program and can terminate it if it likes.
	 * NB: it might not be necessary for the pipeline to be able to terminate this program directly. If it interrupts
	 * the threads
	 * that controls the program, that thread should terminate the program itself.
	 * 
	 * @param row
	 * @param l
	 *            descriptor of the external program
	 */
	public void setExternalProgram(int row, LinkToExternalProgram l);

	/**
	 * Index of the row that created the image that row "row" uses as an output. The result is only meaningful is the
	 * user
	 * specified that the output of row "row" should go into a pre-existing image that was created as the output of some
	 * other row.
	 * This is used by RegisterClick to know which row should be notified when a mouse event is generated in the output
	 * window.
	 * 
	 * @param row
	 *            Index of a row whose output goes into a pre-existing image.
	 * @return Index of the row that created the output image.
	 */
	public int getOwnerOfOurOutput(int row);

	/**
	 * Ask the pipeline to notify the given row that clicks have been generated.
	 * 
	 * @param row
	 *            Row to be notified.
	 * @param pointsToAdd
	 *            List of points that have been clicked (contains coordinates and modifiers)
	 * @param allowInterruptionOfUpdateAlreadyUnderway
	 *            True if the clicks should lead to pipeline to try to cancel an update that is already running on the
	 *            target row.
	 * @param blockUntilCompleted
	 *            If true, call only return once updates have been completed
	 */
	public void passClickToRow(int row, PluginIO pointsToAdd, boolean allowInterruptionOfUpdateAlreadyUnderway,
			boolean blockUntilCompleted);

	/**
	 * If true, C programs should be kept in their loop over their standard input to be given
	 * new sets of parameters. The downside is that they will keep using up memory.
	 * 
	 * @param row
	 * @return Whether to keep program alive
	 */
	public boolean keepCProgramAlive(int row);

	/**
	 * Ask the pipeline to clear any reference to view. This happens when a view is closed by the user.
	 * 
	 * @param view
	 */
	public void clearView(PluginIOView view);

	public void clearDestinations(int row);

	/**
	 * Ask the pipeline to clear references to all views. This happens when the pluginOutputs of a plugin are cleared.
	 * 
	 * @param row
	 */
	void clearAllViews(int row);

	/**
	 * Sets the path of the default input for plugin at specified row to the given file.
	 * 
	 * @param row
	 * @param path
	 */
	public void setInputPath(int row, String path);

	/**
	 * @return Machine to run external plugins on
	 */
	public RemoteMachine getRemoteMachine();
}
