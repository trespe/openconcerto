/*
 * DynamicJava - Copyright (C) 1999 Dyade
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL DYADE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * Except as contained in this notice, the name of Dyade shall not be used in advertising or
 * otherwise to promote the sale, use or other dealings in this Software without prior written
 * authorization from Dyade.
 */

package koala.dynamicjava.classinfo;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * The instances of this class provides informations about class compiled to JVM bytecode.
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/29
 */

public class JavaClassInfo implements ClassInfo {
    /**
     * The boolean info
     */
    public final static JavaClassInfo BOOLEAN = new JavaClassInfo(boolean.class);

    /**
     * The int info
     */
    public final static JavaClassInfo INT = new JavaClassInfo(int.class);

    /**
     * The long info
     */
    public final static JavaClassInfo LONG = new JavaClassInfo(long.class);

    /**
     * The float info
     */
    public final static JavaClassInfo FLOAT = new JavaClassInfo(float.class);

    /**
     * The double info
     */
    public final static JavaClassInfo DOUBLE = new JavaClassInfo(double.class);

    /**
     * The string info
     */
    public final static JavaClassInfo STRING = new JavaClassInfo(String.class);

    /**
     * The Class info
     */
    public final static JavaClassInfo CLASS = new JavaClassInfo(Class.class);

    /**
     * The underlying class
     */
    private final Class javaClass;

    /**
     * Creates a new class info
     * 
     * @param c the java class
     */
    public JavaClassInfo(final Class c) {
        if (c == null) {
            throw new IllegalArgumentException("c == null");
        }

        this.javaClass = c;
    }

    /**
     * Creates a new class info representing an array
     * 
     * @param c the java class
     */
    public JavaClassInfo(final JavaClassInfo c) {
        this.javaClass = Array.newInstance(c.javaClass, 0).getClass();
    }

    /**
     * Returns the underlying class
     */
    public Class getJavaClass() {
        return this.javaClass;
    }

    /**
     * Whether the underlying class needs compilation
     */
    public boolean isCompilable() {
        return false;
    }

    /**
     * Sets the compilable property
     */
    public void setCompilable(final boolean b) {
        throw new IllegalStateException();
    }

    /**
     * Returns the declaring class or null
     */
    public ClassInfo getDeclaringClass() {
        final Class c = this.javaClass.getDeclaringClass();
        return c == null ? null : new JavaClassInfo(c);
    }

    /**
     * Returns the declaring class of an anonymous class or null
     */
    public ClassInfo getAnonymousDeclaringClass() {
        return null;
    }

    /**
     * Returns the modifiers flags
     */
    public int getModifiers() {
        return this.javaClass.getModifiers();
    }

    /**
     * Returns the fully qualified name of the underlying class
     */
    public String getName() {
        return this.javaClass.getName();
    }

    /**
     * Returns the class info of the superclass of the class represented by this info
     */
    public ClassInfo getSuperclass() {
        final Class c = this.javaClass.getSuperclass();
        return c == null ? null : new JavaClassInfo(c);
    }

    /**
     * Returns the class infos of the interfaces implemented by the class this info represents
     */
    public ClassInfo[] getInterfaces() {
        final Class[] interfaces = this.javaClass.getInterfaces();
        final ClassInfo[] result = new ClassInfo[interfaces.length];

        for (int i = 0; i < interfaces.length; i++) {
            result[i] = new JavaClassInfo(interfaces[i]);
        }
        return result;
    }

    /**
     * Returns the field infos for the current class
     */
    public FieldInfo[] getFields() {
        final Field[] fields = this.javaClass.getDeclaredFields();
        final FieldInfo[] result = new FieldInfo[fields.length];

        for (int i = 0; i < fields.length; i++) {
            result[i] = new JavaFieldInfo(fields[i]);
        }
        return result;
    }

    /**
     * Returns the constructor infos for the current class
     */
    public ConstructorInfo[] getConstructors() {
        final Constructor[] constructors = this.javaClass.getDeclaredConstructors();
        final ConstructorInfo[] result = new ConstructorInfo[constructors.length];

        for (int i = 0; i < constructors.length; i++) {
            result[i] = new JavaConstructorInfo(constructors[i]);
        }
        return result;
    }

    /**
     * Returns the method infos for the current class
     */
    public MethodInfo[] getMethods() {
        final Method[] methods = this.javaClass.getDeclaredMethods();
        final MethodInfo[] result = new MethodInfo[methods.length];

        for (int i = 0; i < methods.length; i++) {
            result[i] = new JavaMethodInfo(methods[i]);
        }
        return result;
    }

    /**
     * Returns the classes and interfaces declared as members of the class represented by this
     * ClassInfo object.
     */
    public ClassInfo[] getDeclaredClasses() {
        final Class[] classes = this.javaClass.getDeclaredClasses();
        final ClassInfo[] result = new ClassInfo[classes.length];

        for (int i = 0; i < classes.length; i++) {
            result[i] = new JavaClassInfo(classes[i]);
        }
        return result;
    }

    /**
     * Returns the array type that contains elements of this class
     */
    public ClassInfo getArrayType() {
        return new JavaClassInfo(this);
    }

    /**
     * Indicates whether some other object is "equal to" this one
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof ClassInfo)) {
            return false;
        }
        return this.javaClass.getName().equals(((ClassInfo) obj).getName());
    }

    /**
     * Whether this object represents an interface
     */
    public boolean isInterface() {
        return this.javaClass.isInterface();
    }

    /**
     * Whether this object represents an array
     */
    public boolean isArray() {
        return this.javaClass.isArray();
    }

    /**
     * Whether this object represents a primitive type
     */
    public boolean isPrimitive() {
        return this.javaClass.isPrimitive();
    }

    /**
     * Returns the component type of this array type
     * 
     * @exception IllegalStateException if this type do not represent an array
     */
    public ClassInfo getComponentType() {
        if (!isArray()) {
            throw new IllegalStateException();
        }

        return new JavaClassInfo(this.javaClass.getComponentType());
    }
}
