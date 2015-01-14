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
 
 package org.openconcerto.xml.persistence;

import org.openconcerto.utils.ListMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.jdom.Element;

/**
 * Gère la transformation entre élément XML et objet Java.
 * 
 * @author ILM Informatique 29 juin 2004
 */
public class XMLFactory {

    static private final BidiMap elementNames = new DualHashBidiMap();
    static private final Map<Class, Method> methods = new HashMap<Class, Method>();

    /**
     * Doit être appelé par les XMLable pour déclarer le nom de leur élément XML.
     * 
     * @param elemName le nom de l'élément, eg "eleve".
     * @param clazz la classe, eg Eleve.class.
     * @throws IllegalArgumentException si clazz n'est pas un XMLable.
     * @throws IllegalArgumentException si la méthode fromXML n'est pas accessible dans clazz.
     */
    static public void addClass(String elemName, Class<?> clazz) {
        if (!XMLable.class.isAssignableFrom(clazz))
            throw new IllegalArgumentException("class is not a XMLable");
        try {
            final Method fromXML = clazz.getDeclaredMethod("fromXML", new Class[] { Element.class });
            methods.put(clazz, fromXML);
        } catch (SecurityException e) {
            throw new IllegalArgumentException("fromXML is not acessible in " + clazz);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("fromXML is does not exist in " + clazz);
        }
        elementNames.put(elemName, clazz);
    }

    /**
     * Renvoie un objet à partir d'un élément XML.
     * 
     * @param elem l'élément XML.
     * @return l'objet Java correspondant.
     */
    public static XMLable fromXML(Element elem) {
        Class clazz = getClass(elem.getName());
        if (clazz == null)
            throw new IllegalArgumentException("class of element unknown:" + elem.getName() + ":" + elementNames);
        final Method fromXML = methods.get(clazz);
        XMLable obj = null;
        try {
            obj = (XMLable) fromXML.invoke(null, new Object[] { elem });
        } catch (IllegalArgumentException e) {
            // impossible
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // impossible, testé dans addClass
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // fromXML ne renvoie pas d'exn
            throw (RuntimeException) e.getCause();
        }
        return obj;
    }

    /**
     * Transforme les éléments XML passés en objets Java et les retourne dans une Map.
     * 
     * @param elems une collection d'Element.
     * @return un Map indexée par les classes des objets.
     */
    public static Map<Class<?>, List<XMLable>> fromXML(Collection<? extends Element> elems) {
        final ListMap<Class<?>, XMLable> res = new ListMap<Class<?>, XMLable>();
        for (final Element elem : elems) {
            final Class<?> clazz = getClass(elem.getName());
            if (clazz != null)
                res.add(clazz, fromXML(elem));
        }
        return res;
    }

    static public String getElementName(Class clazz) {
        return (String) elementNames.getKey(clazz);
    }

    public static String getNonNullElementName(Class clazz) {
        final String name = getElementName(clazz);
        if (name == null) {
            throw new IllegalStateException(clazz + " is not registered with addClass.");
        }
        return name;
    }

    static public Element getElement(Class clazz) {
        return new Element(getNonNullElementName(clazz));
    }

    static Class getClass(String element) {
        return (Class) elementNames.get(element);
    }

    public static Class getNonNullClass(String element) {
        final Class clazz = getClass(element);
        if (clazz == null) {
            throw new IllegalStateException(element + " is not registered with addClass.");
        }
        return clazz;
    }

}
