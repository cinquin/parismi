/*******************************************************************************
 * Parismi v0.1
 * Copyright (c) 2009-2015 Cinquin Lab.
 * All rights reserved. This code is made available under a dual license:
 * the two-clause BSD license or the GNU Public License v2.
 ******************************************************************************/
package pipeline.data;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class PluginIOSubstackProxy implements InvocationHandler {

	private IPluginIOSubstack subStack;
	private IPluginIOStack originalStack;

	public PluginIOSubstackProxy(IPluginIOSubstack subStack, IPluginIOStack originalStack) {
		createMethodMap();
		this.subStack = subStack;
		this.originalStack = originalStack;
	}

	private Map<String, Method> methodMap = new HashMap<>();

	private static String typesToString(Class<?> types[]) {
		StringBuilder result = new StringBuilder();
		for (Class<?> clazz : types) {
			result.append(clazz.getName());
		}
		return result.toString();
	}

	private void createMethodMap() {
		Method[] localMethods = PluginIOSubstack.class.getMethods();
		for (Method m : localMethods) {
			methodMap.put(m.getName() + typesToString(m.getParameterTypes()), m);
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Method m = methodMap.get(method.getName() + typesToString(method.getParameterTypes()));
		if (m != null)
			return m.invoke(subStack, args);
		return method.invoke(originalStack, args);
	}
}
