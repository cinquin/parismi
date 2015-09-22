/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.GUI_utils.bean_table;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import pipeline.misc_util.Utils;
import pipeline.misc_util.Utils.LogLevel;
import pipeline.parameters.SpreadsheetCell;

// from http://tips4java.wordpress.com/2008/11/27/bean-table-model/
/**
 * The BeanTableModel will use reflection to determine the columns of
 * data to be displayed in the table model. Reflection is used to find all
 * the methods declared in the specified bean. The criteria used for
 * adding columns to the model are:
 *
 * a) the method name must start with either "get" or "is"
 * b) the parameter list for the method must contain 0 parameters
 *
 * You can also specify an ancestor class in which case the declared methods
 * of the ancestor and all its descendants will be included in the model.
 *
 * A column name will be assigned to each column based on the method name.
 *
 * The cell will be considered editable when a corresponding "set" method
 * name is found.
 *
 * Reflection will also be used to implement the getValueAt() and
 * setValueAt() methods.
 * 
 * 
 * Modified by Olivier Cinquin to add a set columns for each getter that
 * returns a List. A matching method is required that begins by
 * "getListNamesOf" and that returns a List of Strings that will be
 * used as column names.
 */
public class BeanTableModel<T> extends RowTableModel<T> {
	// Map "type" to "class". Class is needed for the getColumnClass() method.

	private static final long serialVersionUID = 1L;
	private static Map<Class<?>, Class<?>> primitives = new HashMap<>(10);

	static {
		primitives.put(Boolean.TYPE, Boolean.class);
		primitives.put(Byte.TYPE, Byte.class);
		primitives.put(Character.TYPE, Character.class);
		primitives.put(Double.TYPE, Double.class);
		primitives.put(Float.TYPE, Float.class);
		primitives.put(Integer.TYPE, Integer.class);
		primitives.put(Long.TYPE, Long.class);
		primitives.put(Short.TYPE, Short.class);
	}

	private Class<?> beanClass;
	// private Class<?> ancestorClass;

	public List<ColumnInformation> columns = new ArrayList<>();

	/**
	 * Constructs an empty <code>BeanTableModel</code> for the specified bean.
	 *
	 * @param beanClass
	 *            class of the beans that will be added to the model.
	 *            The class is also used to determine the columns that
	 *            will be displayed in the model
	 */
	public BeanTableModel(Class<?> beanClass) {
		this(beanClass, beanClass, new CopyOnWriteArrayList<T>());
	}

	/**
	 * Constructs an empty <code>BeanTableModel</code> for the specified bean.
	 *
	 * @param beanClass
	 *            class of the beans that will be added to the model.
	 * @param ancestorClass
	 *            the methods of this class and its descendants down
	 *            to the bean class can be included in the model.
	 */
	public BeanTableModel(Class<?> beanClass, Class<?> ancestorClass) {
		this(beanClass, ancestorClass, new CopyOnWriteArrayList<T>());
	}

	/**
	 * Constructs an empty <code>BeanTableModel</code> for the specified bean.
	 *
	 * @param beanClass
	 *            class of the beans that will be added to the model.
	 * @param modelData
	 *            the data of the table
	 */
	public BeanTableModel(Class<?> beanClass, List<T> modelData) {
		this(beanClass, beanClass, modelData);
	}

	/**
	 * Constructs an empty <code>BeanTableModel</code> for the specified bean.
	 *
	 * @param beanClass
	 *            class of the beans that will be added to the model.
	 * @param ancestorClass
	 *            the methods of this class and its descendents down
	 *            to the bean class can be included in the model.
	 * @param modelData
	 *            the data of the table
	 */
	private BeanTableModel(Class<?> beanClass, Class<?> ancestorClass, List<T> modelData) {
		super(beanClass);
		this.beanClass = beanClass;
		// this.ancestorClass = ancestorClass;

		// Use reflection on the beanClass and ancestorClass to find properties
		// to add to the TableModel

		createColumnInformation(modelData);

		// Initialize the column name List to the proper size. The actual
		// column names will be reset in the resetModelDefaults() method.

		columns.sort((a, b) -> a.getName().compareTo(b.getName()));
		
		for (int i = 0; i < columns.size(); i++) {
			if (columns.get(i).getName().startsWith("userCell")) {
				columns.add(0, columns.remove(i));
			}
		}
		
		List<String> columnNames = columns.stream().map(ColumnInformation::getName).collect(Collectors.toList());

		// Reset all the values in the RowTableModel

		super.setDataAndColumnNames(modelData, columnNames);
		resetModelDefaults();
	}

	/*
	 * Use reflection to find all the methods that should be included in the
	 * model.
	 */
	private void createColumnInformation(List<T> modelData) {
		Method[] theMethods = beanClass.getMethods();

		// Check each method to make sure it should be used in the model

		for (Method theMethod : theMethods) {
			if (theMethod.getParameterTypes().length == 0
			// && ancestorClass.isAssignableFrom(theMethod.getDeclaringClass())
			) {
				String methodName = theMethod.getName();

				if (theMethod.getName().startsWith("get"))
					buildColumnInformation(theMethod, methodName.substring(3), modelData);

				if (theMethod.getName().startsWith("is"))
					buildColumnInformation(theMethod, methodName.substring(2), modelData);
			}
		}
	}

	/*
	 * We found a method candidate so gather the information needed to fully
	 * implemennt the table model.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void buildColumnInformation(Method theMethod, String theMethodName, List<T> modelData) {
		DoNotShowInTable ignore = theMethod.getAnnotation(DoNotShowInTable.class);
		if (ignore != null)
			return;

		TableInsertionIndex order = theMethod.getAnnotation(TableInsertionIndex.class);

		// Make sure the method returns an appropriate type

		Class returnType = getReturnType(theMethod);

		if (returnType == null)
			return;

		// Convert the method name to a display name for each column and
		// then check for a related "set" method.

		// String headerName = formatColumnName( theMethodName );
		String headerName = theMethodName;

		Method setMethod = null;

		try {
			String setMethodName = "set" + theMethodName;
			setMethod = beanClass.getMethod(setMethodName, theMethod.getReturnType());
		} catch (NoSuchMethodException e) {
		}

		if (returnType == List.class && theMethod.getAnnotation(DoNotFlatten.class) == null) {
			// add a column for each element in the list
			// assume that all rows have the same number of elements in the list, and that
			// there is a method to get the column names

			int numberColumns = 0;
			try {
				if (modelData.size() == 0) {
					Utils.log("Data model is empty and cannot be used to determine column number and names",
							LogLevel.DEBUG);
					return;
				} else {
					List<?> list = (List) theMethod.invoke(modelData.get(0));
					if (list == null || list.size() == 0) {
						Utils.log("List is empty is empty and cannot be used to determine column number and names",
								LogLevel.DEBUG);
						return;
					}
					returnType = list.get(0).getClass();
					numberColumns = list.size();
				}
			} catch (Exception e) {
				Utils.printStack(e);
			}

			Method listOfNames = null;
			try {
				MethodToGetColumnNames methodNameAnnotation = theMethod.getAnnotation(MethodToGetColumnNames.class);
				if (methodNameAnnotation == null) {
					Utils.log("Class " + beanClass
							+ " does not have a method to get the names of the elements of list returned by "
							+ theMethodName + "; ignoring this method", LogLevel.DEBUG);
					return;
				}
				listOfNames = beanClass.getMethod(methodNameAnnotation.value());
			} catch (Exception e) {
				Utils.log("Class " + beanClass
						+ " does not have a method to get the names of the elements of list returned by "
						+ theMethodName + "; ignoring this method", LogLevel.DEBUG);
				return;
			}

			List<String> columnNames = null;
			try {
				columnNames = (List<String>) listOfNames.invoke(modelData.get(0));
			} catch (Exception e) {
				Utils.log("Unable to get list of names for list returned by " + theMethodName, LogLevel.WARNING);
				Utils.printStack(e);
				return;
			}

			for (int i = numberColumns - 1; i >= 0; i--) {
				String columnName = i < columnNames.size() ? columnNames.get(i) : "ERR no name; " + i;
				ColumnInformation ci = new ColumnInformation(columnName, returnType, theMethod, setMethod);
				ci.fieldName = theMethodName;
				ci.indexInList = i;
				if (order != null)
					columns.add(order.value(), ci);
				else
					columns.add(ci);
			}

		} else {
			ColumnInformation ci = new ColumnInformation(headerName, returnType, theMethod, setMethod);
			if (order != null)
				columns.add(order.value(), ci);
			else
				columns.add(ci);
		}
	}

	/*
	 * Make sure the return type of the method is something we can use
	 */
	private static Class<?> getReturnType(Method theMethod) {
		Class<?> returnType = theMethod.getReturnType();

		// if (returnType.isInterface()
		// || returnType.isArray()
		// )
		// return null;

		// The primitive class type is different then the wrapper class of the
		// primitive. We need the wrapper class.

		if (returnType.isPrimitive())
			returnType = primitives.get(returnType);

		return returnType;
	}

	/*
	 * Use information collected from the bean to set model default values.
	 */
	private void resetModelDefaults() {
		columnNames.clear();

		for (int i = 0; i < columns.size(); i++) {
			ColumnInformation info = columns.get(i);
			columnNames.add(info.getName());
			super.setColumnClass(i, info.getReturnType());
			super.setColumnEditable(i, info.getSetter() != null);
		}
	}

	public float getFloatValueAt(int row, int column) {
		Object o = getValueAt(row, column);
		if (o instanceof Number)
			return ((Number) o).floatValue();
		if (o instanceof SpreadsheetCell) {
			return ((SpreadsheetCell) o).getFloatValue();
		}
		throw new RuntimeException("Cannot convert bean table model element class " + o.getClass().getName()
				+ " to float");
	}

	/**
	 * Returns an attribute value for the cell at <code>row</code> and <code>column</code>.
	 *
	 * @param row
	 *            the row whose value is to be queried
	 * @param column
	 *            the column whose value is to be queried
	 * @return the value Object at the specified cell
	 * @exception IndexOutOfBoundsException
	 *                if an invalid row or column was given
	 */
	@Override
	public Object getValueAt(int row, int column) {
		ColumnInformation ci = columns.get(column);

		Object value = null;

		try {
			if (ci.indexInList > -1) {
				@Nullable
				T rowT = getRow(row);
				if (rowT != null) {
					@SuppressWarnings("rawtypes")
					List list = (List) ci.getGetter().invoke(getRow(row));
					value = list.get(ci.indexInList);
				} else {
					Utils.log("Ignoring null row in BeanTableModel", LogLevel.WARNING);
				}
			} else {
				@Nullable
				T rowT = getRow(row);
				if (rowT != null)
					value = ci.getGetter().invoke(rowT);
				// SILENTLY IGNORING MISSING ROWS (could happen if objects have been removed and
				// number of rows wasn't recomputed
				else {
					Utils.log("Ignoring null row in BeanTableModel", LogLevel.WARNING);
				}
			}
		} catch (Exception e) {
			Utils.printStack(e);
		}

		return value;
	}

	/**
	 * Sets the object value for the cell at <code>column</code> and <code>row</code>. <code>value</code> is the new
	 * value. This method
	 * will generate a <code>tableChanged</code> notification.
	 *
	 * @param value
	 *            the new value; this can be null
	 * @param row
	 *            the row whose value is to be changed
	 * @param column
	 *            the column whose value is to be changed
	 * @exception IndexOutOfBoundsException
	 *                if an invalid row or
	 *                column was given
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setValueAt(Object value, int row, int column) {
		ColumnInformation ci = columns.get(column);

		try {
			if (ci.indexInList > -1) {
				Method getMethod = ci.getGetter();
				@SuppressWarnings("rawtypes")
				List list = (List<?>) getMethod.invoke(getRow(row));
				list.set(ci.indexInList, value);
			} else {
				Method setMethod = ci.getSetter();

				if (setMethod != null) {
					setMethod.invoke(getRow(row), value);
					fireTableCellUpdated(row, column);
				}
			}
		} catch (Exception e) {
			Utils.printStack(e);
		}
	}

	/**
	 * You are not allowed to change the class of any column.
	 */
	@Override
	public void setColumnClass(int column, Class<?> columnClass) {
		throw new RuntimeException("Not supported");
	}

	/**
	 * Sets the editability for the specified column.
	 *
	 * Override to make sure you can't set a column editable that doesn't
	 * have a defined setter method.
	 *
	 * @param column
	 *            the column whose Class is being changed
	 * @param isEditable
	 *            indicates if the column is editable or not
	 * @exception ArrayIndexOutOfBoundsException
	 *                if an invalid column was given
	 */
	@Override
	public void setColumnEditable(int column, boolean isEditable) {
		ColumnInformation ci = columns.get(column);

		if (isEditable && ci.getSetter() == null)
			return;

		super.setColumnEditable(column, isEditable);
	}

	/**
	 * Convenience method to change the generated column header name.
	 *
	 * This method must be invoked before the model is added to the table.
	 *
	 * @param column
	 *            the column whose value is to be queried
	 * @exception IndexOutOfBoundsException
	 *                if an invalid column
	 *                was given
	 */
	public void setColumnName(int column, String name) {
		ColumnInformation ci = columns.get(column);
		ci.setName(name);
		resetModelDefaults();
	}

	/*
	 * Columns are created in the order in which they are defined in the
	 * bean class. This method will sort the columns by colum header name.
	 * 
	 * This method must be invoked before the model is added to the table.
	 */
	public void sortColumnNames() {
		Collections.sort(columns);
		resetModelDefaults();
	}

	/*
	 * Class to hold data required to implement the TableModel interface
	 */
	public class ColumnInformation implements Comparable<ColumnInformation> {
		/**
		 * If this column is matched to a method that returns a list, index in the list
		 * that corresponds to this column. -1 otherwise.
		 */
		public int indexInList = -1;
		public String fieldName;
		private String name;
		private Class<?> returnType;
		private Method getter;
		private Method setter;

		public ColumnInformation(String name, Class<?> returnType, Method getter, Method setter) {
			this.name = name;
			this.returnType = returnType;
			this.getter = getter;
			this.setter = setter;
		}

		/*
		 * The column class of the model
		 */
		public Class<?> getReturnType() {
			return returnType;
		}

		/*
		 * Used by the getValueAt() method to get the data for the cell
		 */
		public Method getGetter() {
			return getter;
		}

		/*
		 * The value used as the column header name
		 */
		public String getName() {
			return name;
		}

		/*
		 * Used by the setValueAt() method to update the bean
		 */
		public Method getSetter() {
			return setter;
		}

		/*
		 * Use to change the column header name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/*
		 * Implement the natural sort order for this class
		 */
		@Override
		public int compareTo(ColumnInformation o) {
			return getName().compareTo(o.getName());
		}
	}
}
