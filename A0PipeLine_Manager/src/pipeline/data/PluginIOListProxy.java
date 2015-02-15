/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pipeline.GUI_utils.ListOfPointsView;
import pipeline.GUI_utils.PluginIOView;
import pipeline.GUI_utils.XYSeriesReflection;
import pipeline.GUI_utils.bean_table.BeanTableModel;
import pipeline.misc_util.Utils;

public class PluginIOListProxy<T extends IPluginIOListMember<T>> implements InvocationHandler {

	private PluginIO pluginIO = new PluginIO() {
		private static final long serialVersionUID = -1471172603918639285L;

		@Override
		public File asFile(File saveTo, boolean useBigTIFF) throws IOException {
			throw new UnsupportedOperationException();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public IPluginIO duplicateStructure() {
			return newInstance((Class) elementClass);
		}

		@Override
		public PluginIOView createView() {
			@SuppressWarnings("unchecked")
			ListOfPointsView<T> view =
					new ListOfPointsView<>((IPluginIOList<T>) java.lang.reflect.Proxy.newProxyInstance(elementClass
							.getClassLoader(), new Class<?>[] { List.class, IPluginIO.class, IPluginIOListCore.class,
							IPluginIOListOfQ.class }, PluginIOListProxy.this));
			return view;
		}
	};

	private List<T> list = new ArrayList<>();

	private LocalListCoreImplementer localListCoreImplementer = new LocalListCoreImplementer();

	public class LocalListCoreImplementer implements IPluginIOListCore<T> {

		@Override
		public Class<?> getElementClass() {
			return list.get(0).getClass();
		}

		@Override
		public BeanTableModel<T> getBeanTableModel() {
			return new BeanTableModel<>(elementClass, list);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void addDontFireValueChanged(IPluginIOListMember<?> element) {
			list.add((T) element);
		}

		@Override
		public XYSeriesReflection getJFreeChartXYSeries(String xName, String yName, int xIndex, int yIndex,
				String displayNameForXSeries, String displayNameForYSeries) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setUserCellDescription(int index, String o) {
			// TODO Auto-generated method stub

		}

		@Override
		public List<String> getUserCellDescriptions() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setUserCellDescriptions(List<String> desc) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean addAllAndLink(Collection<? extends T> c) {
			return list.addAll(c);
		}
	}

	private Class<?> elementClass;

	private <U> PluginIOListProxy(Class<?> elementClass) {
		this.elementClass = elementClass;
	}

	@SuppressWarnings("unchecked")
	private static <U extends IPluginIOListMember<U>> IPluginIOList<U> newInstance(Class<?> elementClass) {
		return (IPluginIOList<U>) java.lang.reflect.Proxy.newProxyInstance(elementClass.getClassLoader(),
				new Class<?>[] { List.class, IPluginIO.class, IPluginIOListCore.class, IPluginIOListOfQ.class },
				new PluginIOListProxy<U>(elementClass));
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		try {
			Class<?> clazz = method.getDeclaringClass();
			if (clazz.equals(List.class))
				return method.invoke(list, args);
			else if (clazz.equals(IPluginIO.class))
				return method.invoke(pluginIO, args);
			else if (clazz.equals(IPluginIOListCore.class)) {
				return method.invoke(localListCoreImplementer, args);
			} else if (clazz.equals(Object.class))
				return method.invoke(this, args);
			else
				throw new IllegalStateException();
		} catch (Throwable e) {
			Utils.printStack(e);
			throw (e.getCause());
		}
	}

}
