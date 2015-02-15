/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.database;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.data.PluginIOListOfQ;

public class ListDataGroup extends PluginIOListOfQ<DataGroup> {
	private static final long serialVersionUID = -6265907371382894874L;

	@Override
	public String toString() {
		return size() + " elements";
	}

	@Override
	public PluginIOView createView() {
		return new ListOfPointsView<>(this);
	}
}
