/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pipeline.GUI_utils.XYSeriesReflection;
import pipeline.GUI_utils.bean_table.BeanTableModel;

public abstract class PluginIOList<T extends IPluginIOListMember<T>> extends PluginIO implements IPluginIOList<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -474846608321185518L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#getElementClass()
	 */
	@Override
	public abstract Class<?> getElementClass();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#getBeanTableModel()
	 */
	@Override
	public abstract BeanTableModel<T> getBeanTableModel();

	// Should be T instead of ? but callers do not necessarily have access to T
	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#addDontFireValueChanged(pipeline.data.PluginIOListMember)
	 */
	@Override
	public abstract void addDontFireValueChanged(IPluginIOListMember<?> element);

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#duplicateStructure()
	 */
	@Override
	public abstract IPluginIOList<T> duplicateStructure();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#getJFreeChartXYSeries(java.lang.String, java.lang.String, int, int,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public abstract XYSeriesReflection getJFreeChartXYSeries(String xName, String yName, int xIndex, int yIndex,
			String displayNameForXSeries, String displayNameForYSeries);

	/**
	 * For lazy lists that provide access to unparsed protobuf binary representation, this needs to be
	 * called before any elements are accessed.
	 */
	@Override
	public void parseOrReallocate() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#setUserCellDescription(int, java.lang.String)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.IPluginIOListCore2#setUserCellDescription(int, java.lang.String)
	 */
	@Override
	public void setUserCellDescription(int index, String o) {
		setProtobuf(null);
		while (userCellDescriptions.size() < index + 1) {
			userCellDescriptions.add("");
		}
		userCellDescriptions.set(index, o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#getUserCellDescriptions()
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.IPluginIOListCore2#getUserCellDescriptions()
	 */
	@Override
	public List<String> getUserCellDescriptions() {
		parseOrReallocate();
		return userCellDescriptions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#setUserCellDescriptions(java.util.List)
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.IPluginIOListCore2#setUserCellDescriptions(java.util.List)
	 */
	@Override
	public void setUserCellDescriptions(List<String> desc) {
		userCellDescriptions.clear();
		if (desc != null) {
			userCellDescriptions.addAll(desc);
		}
	}

	List<String> userCellDescriptions = new ArrayList<>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see pipeline.data.PluginIOListI#addAllAndLink(java.util.Collection)
	 */
	@Override
	public abstract boolean addAllAndLink(Collection<? extends T> c);

}
