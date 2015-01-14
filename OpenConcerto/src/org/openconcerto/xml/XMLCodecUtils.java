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
 
 package org.openconcerto.xml;

import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.cache.CacheResult;
import org.openconcerto.utils.cache.ICache;
import org.openconcerto.utils.cc.ExnTransformer;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.BeanInfo;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PersistenceDelegate;
import java.beans.PropertyDescriptor;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Stack;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * To encode and decode using {@link XMLEncoder} and {@link XMLDecoder}.
 * 
 * @author Sylvain CUAZ
 */
public class XMLCodecUtils {

    private static final String END_DECL = "?>";

    private static final ICache<Object, Method, Object> cache = new ICache<Object, Method, Object>(60, -1, "methods for " + XMLCodecUtils.class);
    private static final ICache<Object, Constructor<?>, Object> cacheCtor = new ICache<Object, Constructor<?>, Object>(60, -1, "constructors for " + XMLCodecUtils.class);

    // the same as XMLEncoder
    private static final Charset CS = Charset.forName("UTF-8");

    // just to get to java.beans.MetaData :
    // Encoder.setPersistenceDelegate() actually add its arguments to a static object,
    // which is used by all instances of Encoder.
    private static final Encoder bogus = new Encoder();
    private static PersistenceDelegate defaultPersistenceDelegate = new DefaultPersistenceDelegate();
    private static final Map<Class, PersistenceDelegate> persDelegates = new HashMap<Class, PersistenceDelegate>();

    /**
     * Just throws an {@link IllegalStateException}.
     */
    public static final ExceptionListener EXCEPTION_LISTENER = new ExceptionListener() {
        @Override
        public void exceptionThrown(Exception e) {
            throw new IllegalStateException(e);
        }
    };

    /**
     * Register a {@link PersistenceDelegate} for the passed class. This method tries to set
     * <code>del</code> as the default for any subsequent {@link Encoder}, but with the current JRE
     * this cannot be guaranteed.
     * 
     * @param c a {@link Class}.
     * @param del the delegate to use.
     */
    public synchronized static void register(Class c, PersistenceDelegate del) {
        persDelegates.put(c, del);
        bogus.setPersistenceDelegate(c, del);
    }

    public synchronized static void unregister(Class c) {
        persDelegates.remove(c);
        // cannot put null, the implementation uses a Hashtable
        bogus.setPersistenceDelegate(c, defaultPersistenceDelegate);
    }

    private static final byte[] encode2Bytes(Object o) {
        // converting the encoder to a field, only improves execution time by 10%
        // but the xml is no longer enclosed by <java> and the encoder is never closed
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final XMLEncoder enc = new XMLEncoder(out);
        synchronized (XMLCodecUtils.class) {
            for (final Entry<Class, PersistenceDelegate> e : persDelegates.entrySet()) {
                enc.setPersistenceDelegate(e.getKey(), e.getValue());
            }
        }
        // we want to know if some objects are discarded
        enc.setExceptionListener(EXCEPTION_LISTENER);
        // ATTN this only prints exception name (no stacktrace) and continue
        // MAYBE use enc.setExceptionListener();
        enc.writeObject(o);
        enc.close();
        final byte[] res = out.toByteArray();
        try {
            out.close();
        } catch (IOException exn) {
            // shouldn't happen on a ByteArrayOutputStream
            throw new IllegalStateException(exn);
        }
        return res;
    }

    /**
     * Encodes an object using {@link XMLEncoder}, stripping the XML declaration.
     * 
     * @param o the object to encode.
     * @return the xml string suitable to pass to {{@link #decode1(String)}.
     */
    public static final String encode(Object o) {
        final String res = new String(encode2Bytes(o), CS);
        // see XMLEncoder#flush(), it adds an xml declaration
        final int decl = res.indexOf(END_DECL);
        return res.substring(decl + END_DECL.length());
    }

    /**
     * Encodes an object using the same format as {@link XMLEncoder}. This method can be up until 70
     * times faster than XMLEncoder but doesn't handle cycles.
     * 
     * @param o the object to encode.
     * @return the xml without the XML declaration.
     * @see #encode(Object)
     */
    public static final String encodeSimple(Object o) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("<java version=\"1.6.0\" class=\"java.beans.XMLDecoder\">");
        encodeSimpleRec(o, sb);
        sb.append("</java>");
        return sb.toString();
    }

    private static final void createElemEscaped(final String elemName, final Object o, final StringBuilder sb) {
        createElem(elemName, JDOMUtils.OUTPUTTER.escapeElementEntities(o.toString()), sb);
    }

    private static final void createElem(final String elemName, final Object o, final StringBuilder sb) {
        assert o != null;
        sb.append('<');
        sb.append(elemName);
        sb.append('>');
        sb.append(o.toString());
        sb.append("</");
        sb.append(elemName);
        sb.append('>');
    }

    // TODO support cycles, perhaps by using an IdentityMap<Object, Element> since we need to add an
    // id attribute afterwards (only when we know that the object is encountered more than once)
    private static final void encodeSimpleRec(final Object o, final StringBuilder sb) {
        if (o == null) {
            sb.append("<null/>");
            return;
        }

        final Class<?> c = o.getClass();
        if (c == Boolean.class) {
            createElem("boolean", o, sb);
        } else if (c == String.class) {
            createElemEscaped("string", o, sb);
        } else if (c == Character.class) {
            createElemEscaped("char", o, sb);
        } else if (c == Integer.class) {
            createElem("int", o, sb);
        } else if (c == Long.class) {
            createElem("long", o, sb);
        } else if (c == Float.class) {
            createElem("float", o, sb);
        } else if (c == Double.class) {
            createElem("double", o, sb);
        } else if (c == Class.class) {
            createElem("class", ((Class) o).getName(), sb);
        } else if (c == Short.class) {
            createElem("short", o, sb);
        } else if (c == Byte.class) {
            createElem("byte", o, sb);
        } else if (o instanceof Enum) {
            sb.append("<object class=\"");
            sb.append(c.getName());
            sb.append("\" method=\"valueOf\"><string>");
            sb.append(((Enum) o).name());
            sb.append("</string></object>");
        } else if (c.isArray()) {
            sb.append("<array class=\"");
            sb.append(c.getComponentType().getName());
            sb.append("\">");

            final int stop = Array.getLength(o);
            for (int j = 0; j < stop; j++) {
                encodeSimpleRec(Array.get(o, j), sb);
            }

            sb.append("</array>");
        } else
        // see java.beans.*_PersistenceDelegate
        if (o instanceof Map) {
            sb.append("<object class=\"");
            // TODO handle like java_util_Collections
            if (c.getName().startsWith("java.util.Collections$"))
                sb.append("java.util.HashMap");
            else
                sb.append(c.getName());
            sb.append("\">");

            final Map<?, ?> m = (Map<?, ?>) o;
            for (final Entry<?, ?> e : m.entrySet()) {
                sb.append("<void method=\"put\" >");
                encodeSimpleRec(e.getKey(), sb);
                encodeSimpleRec(e.getValue(), sb);
                sb.append("</void>");
            }

            sb.append("</object>");
        } else if (o instanceof Collection) {
            sb.append("<object class=\"");
            // TODO handle like java_util_Collections
            if (c.getName().startsWith("java.util.Arrays$")) {
                sb.append("java.util.ArrayList");
            } else if (c.getName().startsWith("java.util.Collections$")) {
                if (o instanceof Set)
                    sb.append("java.util.HashSet");
                else
                    sb.append("java.util.ArrayList");
            } else {
                sb.append(c.getName());
            }
            sb.append("\">");

            if (o instanceof RandomAccess && o instanceof List) {
                final List<?> list = (List<?>) o;
                final int stop = list.size();
                for (int i = 0; i < stop; i++) {
                    sb.append("<void method=\"add\" >");
                    encodeSimpleRec(list.get(i), sb);
                    sb.append("</void>");
                }
            } else {
                for (final Object item : (Collection<?>) o) {
                    sb.append("<void method=\"add\" >");
                    encodeSimpleRec(item, sb);
                    sb.append("</void>");
                }
            }

            sb.append("</object>");
        } else if (o instanceof Point) {
            final Point p = (Point) o;
            sb.append("<object class=\"java.awt.Point\">");
            encodeSimpleRec(p.x, sb);
            encodeSimpleRec(p.y, sb);
            sb.append("</object>");
        } else if (o instanceof Dimension) {
            final Dimension p = (Dimension) o;
            sb.append("<object class=\"java.awt.Dimension\">");
            encodeSimpleRec(p.width, sb);
            encodeSimpleRec(p.height, sb);
            sb.append("</object>");
        } else if (o instanceof Rectangle) {
            final Rectangle p = (Rectangle) o;
            sb.append("<object class=\"java.awt.Rectangle\">");
            encodeSimpleRec(p.x, sb);
            encodeSimpleRec(p.y, sb);
            encodeSimpleRec(p.width, sb);
            encodeSimpleRec(p.height, sb);
            sb.append("</object>");
        } else {
            final BeanInfo info;
            try {
                info = Introspector.getBeanInfo(c);
            } catch (IntrospectionException e) {
                throw new IllegalStateException("Couldn't inspect " + o, e);
            }

            sb.append("<object class=\"");
            sb.append(c.getName());
            sb.append("\">");

            // Properties
            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
                final Method getter = pd.getReadMethod();
                final Method setter = pd.getWriteMethod();

                if (getter != null && setter != null && pd.getValue("transient") != Boolean.TRUE) {
                    try {
                        sb.append("<void method=\"" + setter.getName() + "\">");
                        encodeSimpleRec(getter.invoke(o, new Object[0]), sb);
                        sb.append("</void>");
                    } catch (Exception e) {
                        throw new RuntimeException("Couldn't get the value of the '" + pd.getDisplayName() + "' property for " + o, e);
                    }
                }
            }

            sb.append("</object>");
        }
    }

    /**
     * Encodes an object using {@link XMLEncoder} returning the root element.
     * 
     * @param o the object to encode.
     * @return the "java" element suitable to pass to {{@link #decode1(Element)}.
     */
    public static final Element encodeToJDOM(Object o) {
        final SAXBuilder builder = new SAXBuilder();
        try {
            return (Element) builder.build(new ByteArrayInputStream(encode2Bytes(o))).getRootElement().detach();
        } catch (JDOMException e) {
            // shouldn't happen since the JRE is supposed to generate valid XML
            throw new IllegalStateException(e);
        } catch (IOException e) {
            // shouldn't happen since we're using byte[] streams
            throw new IllegalStateException(e);
        }
    }

    public static final Object decode1(String s) {
        final ByteArrayInputStream ins = new ByteArrayInputStream(s.getBytes(CS));
        final XMLDecoder dec = new XMLDecoder(ins);
        dec.setExceptionListener(EXCEPTION_LISTENER);
        final Object res = dec.readObject();
        dec.close();
        return res;
    }

    /**
     * Tries to decode an xml element parsed from a string obtained from XMLEncoder. This doesn't
     * use {@link XMLDecoder} as it requires outputting it first to string which is inefficient.
     * NOTE: this decoder supports only a subset of XMLDecoder.
     * 
     * @param javaElem a "java" element.
     * @return the decoded object.
     */
    public static final Object decode1(Element javaElem) {
        final Element elem = (Element) javaElem.getChildren().get(0);
        try {
            return eval(elem, new Stack<Object>(), new HashMap<String, Object>());
        } catch (Exception e) {
            throw new IllegalStateException("error decoding " + JDOM2Utils.output(javaElem), e);
        }
    }

    private static final Object eval(Element elem, Stack<Object> context, final Map<String, Object> ids) throws ClassNotFoundException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        final String n = elem.getName();
        if (n.equals("null")) {
            return null;
        } else if (n.equals("boolean")) {
            return Boolean.valueOf(elem.getText());
        } else if (n.equals("byte")) {
            return Byte.valueOf(elem.getText());
        } else if (n.equals("char")) {
            return Character.valueOf(elem.getText().charAt(0));
        } else if (n.equals("string")) {
            return elem.getText();
        } else if (n.equals("short")) {
            return Short.valueOf(elem.getText());
        } else if (n.equals("int")) {
            return Integer.valueOf(elem.getText());
        } else if (n.equals("long")) {
            return Long.valueOf(elem.getText());
        } else if (n.equals("float")) {
            return Float.valueOf(elem.getText());
        } else if (n.equals("double")) {
            return Double.valueOf(elem.getText());
        } else if (n.equals("array")) {
            final String classAttr = elem.getAttributeValue("class");
            final String lengthAttr = elem.getAttributeValue("length");

            final Class<?> componentClass = parseClassName(classAttr);
            if (lengthAttr != null) {
                context.push(Array.newInstance(componentClass, Integer.parseInt(lengthAttr)));
                for (final Object child : elem.getChildren()) {
                    eval((Element) child, context, ids);
                }
                return context.pop();
            } else {
                return evalContainer(elem, context, ids, new ExnTransformer<List<Object>, Object, RuntimeException>() {
                    @Override
                    public Object transformChecked(List<Object> args) {
                        final Object res = Array.newInstance(componentClass, args.size());
                        for (int j = 0; j < args.size(); j++) {
                            Array.set(res, j, args.get(j));
                        }
                        return res;
                    }
                });
            }
        } else if (n.equals("void") || n.equals("object")) {
            final String idref = elem.getAttributeValue("idref");
            if (idref != null) {
                if (!ids.containsKey(idref))
                    throw new IllegalStateException("id '" + idref + "' wasn't defined");
                return ids.get(idref);
            }
            final String id = elem.getAttributeValue("id");
            final String targetClass = elem.getAttributeValue("class");
            final Object target = targetClass == null ? context.peek() : Class.forName(targetClass);
            final String propAttr = elem.getAttributeValue("property");
            final String indexAttr = elem.getAttributeValue("index");
            final String methodAttr = elem.getAttributeValue("method");

            // statement or expression
            final Object res = evalContainer(elem, context, ids, new ExnTransformer<List<Object>, Object, Exception>() {
                @Override
                public Object transformChecked(List<Object> args) throws Exception {
                    // call the statement
                    final Object res;

                    if (propAttr != null) {
                        final String methodName = (args.size() == 0 ? "get" : "set") + StringUtils.firstUp(propAttr);
                        res = invoke(target, methodName, args);
                    } else if (indexAttr != null) {
                        final String methodName;
                        if (target instanceof List) {
                            methodName = args.size() == 0 ? "get" : "set";
                            // get(index) or set(index, value)
                            args.add(0, Integer.valueOf(indexAttr));
                            res = invoke(target, methodName, args);
                        } else if (target.getClass().isArray()) {
                            final Class<?> componentType = target.getClass().getComponentType();
                            // in Array there's set(array, int index, Object value) or
                            // setPrimitive(array, int index, primitive value)
                            methodName = (args.size() == 0 ? "get" : "set") + (componentType.isPrimitive() ? StringUtils.firstUp(componentType.getSimpleName()) : "");
                            args.add(0, target);
                            args.add(1, Integer.valueOf(indexAttr));
                            res = invoke(Array.class, methodName, args);
                        } else
                            throw new IllegalStateException("use index with neither List nor array: " + target);
                    } else if (methodAttr != null) {
                        res = invoke(target, methodAttr, args);
                    } else
                        res = getCtor((Class<?>) target, args).newInstance(args.toArray());
                    return res;
                }
            });
            // not very functional but it works
            if (id != null)
                ids.put(id, res);
            return res;
        } else if (n.equals("class")) {
            return Class.forName(elem.getText());
        } else
            throw new UnsupportedOperationException("doesn't yet support " + n);
    }

    private static final Object evalContainer(final Element parent, Stack<Object> context, final Map<String, Object> ids, final ExnTransformer<List<Object>, Object, ? extends Exception> transf)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final List<Object> args = new ArrayList<Object>();
        final List<?> children = parent.getChildren();
        int i = 0;
        boolean noVoid = true;
        while (i < children.size() && noVoid) {
            final Element child = (Element) children.get(i);
            if (child.getName().equals("void"))
                noVoid = false;
            else {
                args.add(eval(child, context, ids));
                i++;
            }
        }

        // call the statement
        final Object res = transf.transformCheckedWithExn(args, false, InvocationTargetException.class, InstantiationException.class, IllegalAccessException.class);

        context.push(res);

        // now call the voids
        while (i < children.size()) {
            final Element child = (Element) children.get(i);
            eval(child, context, ids);
            i++;
        }
        return context.pop();
    }

    private static final Object invoke(final Object target, String methodName, final List<Object> args) throws IllegalAccessException, InvocationTargetException {
        // <object class="Cell" method="createEmpty" >
        // for static methods the target is already a class
        final Class clazz = target instanceof Class ? (Class) target : target.getClass();
        final Method m = getMethod(clazz, methodName, args);
        return m.invoke(target, args.toArray());
    }

    private static final Method getMethod(Class<?> clazz, String name, List<Object> actualArgs) {
        final List<Class<?>> actualClasses = objectsToClasses(actualArgs);
        final List<Object> key = new ArrayList<Object>(3);
        key.add(clazz);
        key.add(name);
        key.add(actualClasses);

        final CacheResult<Method> cacheRes = cache.check(key);
        if (cacheRes.getState() == CacheResult.State.VALID)
            return cacheRes.getRes();

        final Method res = findMethod(clazz, name, actualClasses);
        cache.put(key, res);
        return res;
    }

    private static final Constructor getCtor(Class<?> clazz, List<Object> actualArgs) {
        final List<Class<?>> actualClasses = objectsToClasses(actualArgs);
        final List<Object> key = new ArrayList<Object>(3);
        key.add(clazz);
        key.add(actualClasses);

        final CacheResult<Constructor<?>> cacheRes = cacheCtor.check(key);
        if (cacheRes.getState() == CacheResult.State.VALID)
            return cacheRes.getRes();

        final Constructor res = findCtor(clazz, actualClasses);
        cacheCtor.put(key, res);
        return res;
    }

    private static final List<Class<?>> objectsToClasses(List<Object> actualArgs) {
        final List<Class<?>> actualClasses = new ArrayList<Class<?>>(actualArgs.size());
        for (final Object actualArg : actualArgs)
            actualClasses.add(actualArg == null ? null : actualArg.getClass());
        return actualClasses;
    }

    // TODO return the most specific matching method instead of the first one
    // (handle both Sub/Superclass and primitive/object type)
    private static final Method findMethod(Class<?> clazz, String name, List<Class<?>> actualArgs) {
        for (final Method m : clazz.getMethods()) {
            if (m.getName().equals(name) && callableWith(m.getParameterTypes(), actualArgs)) {
                return m;
            }
        }
        return null;
    }

    // TODO see findMethod()
    private static final Constructor findCtor(Class<?> clazz, List<Class<?>> actualArgs) {
        for (final Constructor m : clazz.getConstructors()) {
            if (callableWith(m.getParameterTypes(), actualArgs)) {
                return m;
            }
        }
        return null;
    }

    private static final boolean callableWith(Class<?>[] formalArgs, List<Class<?>> actualArgs) {
        if (formalArgs.length != actualArgs.size())
            return false;
        int i = 0;
        for (final Class<?> argClass : formalArgs) {
            final Class<?> actualArg = actualArgs.get(i);
            // null match everything
            if (actualArg != null && !argClass.isAssignableFrom(actualArg) && argClass != getPrimitive(actualArg))
                return false;
            i++;
        }

        return true;
    }

    private static Class<?> getPrimitive(Class<?> argClass) {
        if (argClass == Boolean.class)
            return Boolean.TYPE;
        else if (argClass == Character.class)
            return Character.TYPE;
        else if (argClass == Byte.class)
            return Byte.TYPE;
        else if (argClass == Short.class)
            return Short.TYPE;
        else if (argClass == Integer.class)
            return Integer.TYPE;
        else if (argClass == Long.class)
            return Long.TYPE;
        else if (argClass == Float.class)
            return Float.TYPE;
        else if (argClass == Double.class)
            return Double.TYPE;
        else
            return null;
    }

    private static final Map<String, Class> primitiveNames = new HashMap<String, Class>();

    static {
        primitiveNames.put("boolean", boolean.class);
        primitiveNames.put("byte", byte.class);
        primitiveNames.put("char", char.class);
        primitiveNames.put("short", short.class);
        primitiveNames.put("int", int.class);
        primitiveNames.put("long", long.class);
        primitiveNames.put("float", float.class);
        primitiveNames.put("double", double.class);
    }

    /**
     * Parse class names (including primitive).
     * 
     * @param className a class name, eg "java.lang.String" or "int".
     * @return the matching class, eg java.lang.String.class or Integer.TYPE.
     * @throws ClassNotFoundException if the passed name doesn't exist.
     */
    private static Class<?> parseClassName(String className) throws ClassNotFoundException {
        final Class<?> primitive = primitiveNames.get(className);
        if (primitive != null)
            return primitive;
        else
            return Class.forName(className);
    }

    private XMLCodecUtils() {
    }

}
