/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.misc_util;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import pipeline.plugins.PipelinePlugin;

public class ProgressBarWrapper extends JProgressBar implements ProgressReporter {

	public Timer timer;
	protected PipelinePlugin pluginOwner;

	protected ProgressBarWrapper(int vertical, PipelinePlugin pluginOwner) {
		super(vertical);
		this.pluginOwner = pluginOwner;
	}

	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.misc_util.ProgressReporter#setValue(int)
	 */
	@Override
	public final void setValue(int value) {
		int scaledValue = multFactor != 1 ? (int) (min + value * multFactor) : min + value;
		super.setValue(scaledValue);
		if (pluginOwner != null) {
			if (pluginOwner.getPipelineListener() != null)
				pluginOwner.getPipelineListener().redrawProgressRenderer(pluginOwner.getRow());
		}
	}

	@Override
	public final int getValue() {
		return (int) ((super.getValue() - min) / multFactor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.misc_util.ProgressReporter#setValueThreadSafe(int)
	 */
	@Override
	public final void setValueThreadSafe(final int value) {
		SwingUtilities.invokeLater(() -> {
			ProgressBarWrapper.super.setIndeterminate(false);
			setValue(value);
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.misc_util.ProgressReporter#setIndeterminate(boolean)
	 */
	@Override
	public final void setIndeterminate(final boolean indeterminate) {
		Runnable r = () -> ProgressBarWrapper.this.setIndeterminateFromEDT(indeterminate);

		if (SwingUtilities.isEventDispatchThread())
			r.run();
		else
			try {
				SwingUtilities.invokeLater(r);
			} catch (Exception e) {
				// Utils.printStack(e);
			}

	}

	/**
	 * This MUST be called from the EDT.
	 * 
	 * @param indeterminate
	 */
	final void setIndeterminateFromEDT(boolean indeterminate) {
		super.setIndeterminate(indeterminate);
	}

	private float multFactor = 1;
	private int min = 0;
	private int max;

	@Override
	public void setMin(int min) {
		this.min = min;
		updateRange();
	}

	@Override
	public void setMax(int max) {
		this.max = max;
		updateRange();
	}

	private void updateRange() {
		float f = max - min + 1;
		if (f == 0) {
			multFactor = 1;
			return;
		}
		multFactor = 100 / f;
	}
}
