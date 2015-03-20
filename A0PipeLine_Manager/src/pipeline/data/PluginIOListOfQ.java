/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.jdt.annotation.NonNull;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.XYSeriesReflection;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.GUI_utils.bean_table.DoNotShowInTable;

public class PluginIOListOfQ<T extends IPluginIOListMemberQ<T>> extends PluginIOList<T> implements IPluginIOListOfQ<T> {

	private static final long serialVersionUID = 463546504975133689L;

	@Override
	public boolean addQuantifiedPropertyName(String name) {
		if (getQuantifiedPropertyNames().contains(name))
			return true;
		else {
			setProtobuf(null);
			getQuantifiedPropertyNames().add(name);
			for (T p : (this)) {
				p.getQuantifiedProperties().add(0f);
			}
			return false;
		}
	}

	@NonNull List<String> quantifiedPropertyNames = new ArrayList<>();

	@Override
	public List<String> getQuantifiedPropertyNames() {
		parseOrReallocate();
		return quantifiedPropertyNames;
	}

	@Override
	public void setQuantifiedPropertyNames(List<String> desc) {
		quantifiedPropertyNames = desc;
	}

	@Override
	@DoNotShowInTable
	public boolean hasQuantifiedProperty(String name) {
		throw new RuntimeException("No list-level quantified properties implemented");
	}

	transient List<T> internalList = initializeList();

	@Override
	public int size() {
		parseOrReallocate();
		return internalList.size();
	}

	@Override
	public boolean isEmpty() {
		parseOrReallocate();
		return internalList.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		parseOrReallocate();
		return internalList.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		parseOrReallocate();
		return internalList.iterator();
	}

	@Override
	public Object[] toArray() {
		parseOrReallocate();
		return internalList.toArray();
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		parseOrReallocate();
		return internalList.toArray(a);
	}

	@Override
	public boolean remove(Object o) {
		parseOrReallocate();
		return internalList.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		parseOrReallocate();
		return internalList.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		parseOrReallocate();
		return internalList.addAll(c);
	}

	@Override
	public boolean addAllAndLink(Collection<? extends T> c) {
		parseOrReallocate();
		boolean listChanged = false;
		for (T p : c) {
			boolean localChange = addAndLink(p);
			listChanged = listChanged || localChange;
		}
		return listChanged;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		parseOrReallocate();
		return internalList.addAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		parseOrReallocate();
		return internalList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		parseOrReallocate();
		return internalList.retainAll(c);
	}

	@Override
	public T get(int index) {
		parseOrReallocate();
		return internalList.get(index);
	}

	@Override
	public T set(int index, T element) {
		parseOrReallocate();
		return internalList.set(index, element);
	}

	@Override
	public void add(int index, T element) {
		parseOrReallocate();
		internalList.add(index, element);
	}

	@Override
	public T remove(int index) {
		parseOrReallocate();
		return internalList.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		parseOrReallocate();
		return internalList.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		parseOrReallocate();
		return internalList.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		parseOrReallocate();
		return internalList.listIterator();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		parseOrReallocate();
		return internalList.listIterator(index);
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		parseOrReallocate();
		return internalList.subList(fromIndex, toIndex);
	}

	public boolean addAndLink(T p) {
		parseOrReallocate();
		p.linkToList(this);
		return internalList.add(p);
	}

	List<T> initializeList() {
		internalList = new ArrayList<>();
		return internalList;
	}

	@Override
	public void clear() {
		if (internalList == null)
			initializeList();
		internalList.clear();
		setProtobuf(null);
		// clearListeningSeries();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void addDontFireValueChanged(IPluginIOListMember<?> element) {
		parseOrReallocate();
		internalList.add((T) element);
		setProtobuf(null);
	}

	private boolean addDontFireValueChanged(T p) {
		parseOrReallocate();
		boolean result = internalList.add(p);
		setProtobuf(null);
		// addPointToListeningSeries(p);
		return result;
	}

	@Override
	public boolean add(T p) {
		boolean result = addDontFireValueChanged(p);
		fireValueChanged(false, false);
		return result;
	}

	@Override
	public Class<?> getElementClass() {
		return get(0).getClass();
	}

	@Override
	public BeanTableModel<T> getBeanTableModel() {
		parseOrReallocate();
		if (internalList.size() == 0)
			throw new IllegalStateException("Need a non-empty list to work out table model");
		return new BeanTableModel<>(internalList.get(0).getClass(), internalList);
	}

	@Override
	public IPluginIOList<T> duplicateStructure() {
		parseOrReallocate();
		PluginIOListOfQ<T> result = new PluginIOListOfQ<>();
		copyInto(result);
		return result;
	}

	@Override
	public XYSeriesReflection getJFreeChartXYSeries(String xName, String yName, int xIndex, int yIndex,
			String displayNameForXSeries, String displayNameForYSeries) {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public File asFile(File saveTo, boolean useBigTIFF) throws IOException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void copyInto(IPluginIO destination) {
		super.copyInto(destination);
		IQuantifiableNames dest = (IQuantifiableNames) destination;

		dest.getQuantifiedPropertyNames().clear();
		dest.getQuantifiedPropertyNames().addAll(getQuantifiedPropertyNames());
	}

	@Override
	public PluginIOView createView() {
		ListOfPointsView<T> view = new ListOfPointsView<>(this);
		return view;
	}

}
