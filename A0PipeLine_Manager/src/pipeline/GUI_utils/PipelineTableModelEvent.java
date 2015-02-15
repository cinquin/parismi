/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils;

import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

class PipelineTableModelEvent extends TableModelEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3586277638944272389L;

	public int eventType;

	public static final int FILTER_ADJUSTING = 1;

	public PipelineTableModelEvent(TableModel source, int eventType) {
		super(source);
		this.eventType = eventType;
	}

}
