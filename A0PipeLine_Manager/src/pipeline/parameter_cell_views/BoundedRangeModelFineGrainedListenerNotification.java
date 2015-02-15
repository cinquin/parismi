/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.parameter_cell_views;

import javax.swing.DefaultBoundedRangeModel;

class BoundedRangeModelFineGrainedListenerNotification extends DefaultBoundedRangeModel {

	public BoundedRangeModelFineGrainedListenerNotification(int value, int extent, int min, int max) {
		super(value, extent, min, max);
	}

	public BoundedRangeModelFineGrainedListenerNotification() {
		super();
	}

	private static final long serialVersionUID = 1L;

	private boolean silenceUpdates;

	public boolean isSilenceUpdates() {
		return silenceUpdates;
	}

	public void setSilenceUpdates(boolean silenceUpdates) {
		this.silenceUpdates = silenceUpdates;
	}

	@Override
	protected void fireStateChanged() {
		if (!silenceUpdates)
			super.fireStateChanged();
	}
}
