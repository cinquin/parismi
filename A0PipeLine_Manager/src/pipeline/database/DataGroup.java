/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.database;

import java.util.Map;

import pipeline.data.IPluginIO;
import pipeline.data.IPluginIOListMemberQ;

public interface DataGroup extends IPluginIOListMemberQ<DataGroup> {
	public long getID();

	public Map<String, IPluginIO> getPIOs();
}
