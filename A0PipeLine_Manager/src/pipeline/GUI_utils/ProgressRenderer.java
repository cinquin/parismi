/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.TableCellRenderer;

import pipeline.PipelineCallback;
import pipeline.misc_util.ProgressBarWrapper;
import pipeline.misc_util.Utils;
import pipeline.plugins.PipelinePlugin;

/**
 * Class used as a cell renderer to display an integer cell value (from 0 to 100) as a graphical progress bar.
 * This class is used as a renderer for the column in the pipeline JTable that shows progress.
 *
 */
public class ProgressRenderer extends ProgressBarWrapper implements TableCellRenderer {
	private static final long serialVersionUID = 1L;

	public ProgressRenderer(JTable table, PipelinePlugin plugin) {
		super(SwingConstants.VERTICAL, plugin);
	}

	private boolean evenTableRow;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		evenTableRow = (row % 2 == 0);
		setOpaque(true);
		if (evenTableRow)
			this.setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		else
			this.setBackground(Utils.COLOR_FOR_ODD_ROWS);
		this.getParent().setBackground(Utils.COLOR_FOR_EVEN_ROWS);
		return this;
	}

	@Override
	public boolean isDisplayable() {
		return true;
	}

	/**
	 * Sets the progress bar reporter to the determinate or indeterminate mode.
	 * If indeterminate, and pipelineCallback is not null, animates it every second.
	 * 
	 * @param p
	 *            progress bar
	 * @param indeterminate
	 * @param pipelineCallback
	 *            callback to the pipeline used to redraw the line
	 * @param row
	 *            index in the table of the progress bar
	 */
	public static void progressSetIndeterminateAndAnimate(final ProgressBarWrapper p, final boolean indeterminate,
			final PipelineCallback pipelineCallback, final int row) {

		if (p == null)
			return;

		synchronized (p) {
			p.setIndeterminate(indeterminate);
			if (pipelineCallback == null)
				return;

			if (indeterminate) {
				if (p.timer == null) {
					p.timer = new Timer(1000, null);
				} else {
					for (ActionListener al : p.timer.getActionListeners())
						p.timer.removeActionListener(al);
				}
				final Action t = new AbstractAction() {
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent action) {
						if (!p.isIndeterminate())
							p.timer.stop();

						pipelineCallback.redrawProgressRenderer(row);
					}
				};

				p.timer.addActionListener(t);
				p.timer.start();

			} else {

				SwingUtilities.invokeLater(() -> pipelineCallback.redrawProgressRenderer(row));

			}
		}
	}

	public void setPlugin(PipelinePlugin the_plugin) {
		pluginOwner = the_plugin;
	}
}
