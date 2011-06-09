/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.utils.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * @author ILM Informatique 25 août 2004
 */
public class DottedPropertyDescriptor {

	/**
	 * @param propertyName "span.start"
	 * @param beanClass Task.class
	 * @return la prop start de PSpan.class.
	 * @throws IntrospectionException
	 */
	public static PropertyDescriptor create(String propertyName, Class beanClass, Map propDescriptors) throws IntrospectionException {
		String[] props = propertyName.split("\\.");
		if (props.length == 1)
			// si c'est une propriété normale
			return new PropertyDescriptor(propertyName, beanClass);

		List targetPath = new ArrayList(props.length - 1);

		// le chemin pour arriver au dernier bean
		PropertyDescriptor desc = (PropertyDescriptor) propDescriptors.get(props[0]);
		targetPath.add(desc.getReadMethod());
		Class previousClass = desc.getPropertyType();
		for (int i = 1; i < props.length - 1; i++) {
			String propName = props[i];
			desc = get(Introspector.getBeanInfo(previousClass), propName);
			targetPath.add(desc.getReadMethod());
			previousClass = desc.getPropertyType();
		}
		// la derniere prop eg PSpan.start
		desc = get(Introspector.getBeanInfo(previousClass), props[props.length - 1]);
		// on corrige le nom
		desc.setName(propertyName);
		desc.setValue("methods", targetPath);
		return desc;
	}

	static private PropertyDescriptor get(BeanInfo bi, String name) {
		PropertyDescriptor[] props = bi.getPropertyDescriptors();
		for (int i = 0; i < props.length; i++) {
			PropertyDescriptor descriptor = props[i];
			if (descriptor.getName().equals(name))
				return descriptor;
		}
		return null;
	}

	/** Seulement si m est public */
	static private Object getValue(Object target, Method m) throws InvocationTargetException {
		Object result = null;
		try {
			result = m.invoke(target, new Object[0]);
		} catch (IllegalArgumentException e) {
			// impossible (pas d'arguments)
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// impossible (un getter est public)
			e.printStackTrace();
		}
		return result;
	}

	static private Object getValue(Object first, List methods) throws InvocationTargetException {
		Object current = first;
		Iterator iter = methods.iterator();
		while (iter.hasNext()) {
			Method m = (Method) iter.next();
			current = getValue(current, m);
		}
		return current;
	}

	static public Object getTarget(PropertyDescriptor desc, Object first) throws InvocationTargetException {
		List methods = (List) desc.getValue("methods");
		if (methods != null) {
			return getValue(first, methods);
		} else
			return first;
	}

}
