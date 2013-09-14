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
 
 package org.openconcerto.utils.i18n;

import org.openconcerto.utils.Log;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Value;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle.Control;
import java.util.logging.Level;

import net.jcip.annotations.ThreadSafe;

/**
 * Allow to create a list of instances from classes and other files ending with a language tag.
 * 
 * @author Sylvain
 * @param <T> type of instances.
 * @see #createInstances(String, Locale)
 */
@ThreadSafe
public class LocalizedInstances<T> {

    private final Class<T> clazz;
    private final Control cntrl;
    private String staticMethodName;

    public LocalizedInstances(Class<T> clazz, Control cntrl) {
        super();
        this.clazz = clazz;
        this.cntrl = cntrl;
        this.staticMethodName = null;
    }

    public final Class<T> getClassToUse() {
        return this.clazz;
    }

    public final Control getControl() {
        return this.cntrl;
    }

    public final Tuple2<Locale, List<T>> createInstances(final Locale locale) {
        return this.createInstances(this.getClassToUse().getName(), locale);
    }

    public final Tuple2<Locale, List<T>> createInstances(final String baseName, final Locale locale) {
        return createInstances(baseName, locale, this.getClassToUse());
    }

    /**
     * Create instances of the same language. For each candidate locale, this method first looks for
     * {@link #getConstructor(Class) a class} and then for {@link #createInstance(String, Locale)
     * other files}.
     * 
     * @param baseName the base name of the classes, e.g. "org.acme.MyClass".
     * @param locale the desired locale, e.g. fr_FR.
     * @param cl the class to use as context for loading
     *        {@link Class#forName(String, boolean, ClassLoader) classes} and
     *        {@link Class#getResourceAsStream(String) resources}.
     * @return the used locale and the instances, e.g. if no classes ending in fr were found, this
     *         method will use the {@link Control#getFallbackLocale(String, Locale) fallback} and if
     *         this also doesn't exist, then the locale will be <code>null</code> and the list
     *         empty.
     */
    public final Tuple2<Locale, List<T>> createInstances(final String baseName, final Locale locale, final Class<?> cl) {
        final List<T> l = new ArrayList<T>();
        Locale localeRes = null;
        // test emptiness to not mix languages
        for (Locale targetLocale = locale; targetLocale != null && l.isEmpty(); targetLocale = this.cntrl.getFallbackLocale(baseName, targetLocale)) {
            localeRes = targetLocale;
            for (final Locale candidate : this.cntrl.getCandidateLocales(baseName, targetLocale)) {
                final String bundleName = this.cntrl.toBundleName(baseName, candidate);

                // code first
                final Class<?> loadedClass = loadClass(bundleName, cl);
                if (loadedClass != null && this.clazz.isAssignableFrom(loadedClass)) {
                    final Class<? extends T> subclazz = loadedClass.asSubclass(this.clazz);
                    try {
                        final Value<? extends T> instance = this.getInstance(subclazz);
                        if (instance.hasValue())
                            l.add(instance.getValue());
                        else
                            Log.get().warning(loadedClass + " exists but the constructor wasn't found");
                    } catch (Exception e) {
                        Log.get().log(Level.WARNING, "Couldn't create an instance using " + subclazz, e);
                    }
                }

                try {
                    final T newInstance = this.createInstance(bundleName, candidate, cl);
                    if (newInstance != null)
                        l.add(newInstance);
                } catch (IOException e) {
                    Log.get().log(Level.WARNING, "Couldn't create an instance using " + bundleName, e);
                }
            }
        }
        return Tuple2.create(localeRes, l);
    }

    private final Class<?> loadClass(final String name, final Class<?> cl) {
        try {
            return Class.forName(name, true, cl.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * The no-arg static method to use.
     * 
     * @param staticMethodName a static method name, <code>null</code> meaning the constructor.
     * @return this.
     * @see #getInstance(Class)
     */
    public final synchronized LocalizedInstances<T> setStaticMethodName(String staticMethodName) {
        this.staticMethodName = staticMethodName;
        return this;
    }

    public final synchronized String getStaticMethodName() {
        return this.staticMethodName;
    }

    /**
     * Get an instance of the passed class. In this implementation, if
     * {@link #getStaticMethodName()} isn't <code>null</code> then it is used, otherwise the no-arg
     * constructor is used.
     * 
     * @param subclass the class to use.
     * @return an instance or {@link Value#getNone()} if the method or constructor doesn't exist.
     * @exception IllegalAccessException if the method or constructor is inaccessible.
     * @exception InstantiationException if the class that declares the constructor represents an
     *            abstract class.
     * @exception InvocationTargetException if the method or constructor throws an exception.
     */
    protected <U extends T> Value<U> getInstance(final Class<U> subclass) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        final String staticMethodName = this.getStaticMethodName();
        try {
            if (staticMethodName != null) {
                final Method method = subclass.getMethod(staticMethodName);
                if (Modifier.isStatic(method.getModifiers()) && subclass.isAssignableFrom(method.getReturnType())) {
                    return Value.getSome(subclass.cast(method.invoke(null)));
                } else {
                    return Value.getNone();
                }
            } else {
                final Constructor<U> ctor = subclass.getConstructor();
                return Value.getSome(ctor.newInstance());
            }
        } catch (NoSuchMethodException e) {
            return Value.getNone();
        }
    }

    /**
     * Create an instance for the passed bundle and locale.
     * 
     * @param bundleName the bundle.
     * @param candidate the locale.
     * @param cl the class to use as context.
     * @return a new instance using a resource at <code>bundleName</code>, or <code>null</code>.
     * @throws IOException if an error occur while reading the resource.
     * @see Control#toResourceName(String, String)
     */
    protected T createInstance(final String bundleName, final Locale candidate, final Class<?> cl) throws IOException {
        return null;
    }
}
