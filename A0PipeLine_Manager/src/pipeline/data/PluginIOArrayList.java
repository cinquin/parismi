/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import pipeline.GUI_utils.XYSeriesReflection;
import pipeline.GUI_utils.bean_table.BeanTableModel;

public class PluginIOArrayList<T extends IPluginIOListMember<T>> extends PluginIOList<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6282785098954390074L;

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean add(T e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean remove(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public T get(int index) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public T set(int index, T element) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public void add(int index, T element) {
		// TODO Auto-generated method stub

	}

	@Override
	public T remove(int index) {
		throw new RuntimeException("Unimplemented");
	}

	@Override
	public int indexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<T> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getElementClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BeanTableModel<T> getBeanTableModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addDontFireValueChanged(IPluginIOListMember<?> element) {
		// TODO Auto-generated method stub

	}

	@Override
	public IPluginIOList<T> duplicateStructure() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public XYSeriesReflection getJFreeChartXYSeries(String xName, String yName, int xIndex, int yIndex,
			String displayNameForXSeries, String displayNameForYSeries) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addAllAndLink(Collection<? extends T> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
