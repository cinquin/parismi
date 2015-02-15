/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.util.Collection;
import java.util.List;

import pipeline.GUI_utils.XYSeriesReflection;
import pipeline.GUI_utils.bean_table.BeanTableModel;

public interface IPluginIOListCore<T extends IPluginIOListMember<T>> {

	Class<?> getElementClass();

	BeanTableModel<T> getBeanTableModel();

	// Should be T instead of ? but callers do not necessarily have access to T
	void addDontFireValueChanged(IPluginIOListMember<?> element);

	XYSeriesReflection getJFreeChartXYSeries(String xName, String yName, int xIndex, int yIndex,
			String displayNameForXSeries, String displayNameForYSeries);

	void setUserCellDescription(int index, String o);

	List<String> getUserCellDescriptions();

	void setUserCellDescriptions(List<String> desc);

	boolean addAllAndLink(Collection<? extends T> c);

}
