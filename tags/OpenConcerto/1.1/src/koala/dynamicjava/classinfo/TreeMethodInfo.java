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

import java.util.Iterator;
import java.util.List;

import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.MethodDeclaration;

/**
 * The instances of this class provides informations about class methods not yet compiled to JVM
 * bytecode.
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/29
 */

public class TreeMethodInfo implements MethodInfo {
    /**
     * The abstract syntax tree of this method
     */
    private final MethodDeclaration methodTree;

    /**
     * The class finder for this class
     */
    private final ClassFinder classFinder;

    /**
     * The parameters types
     */
    private ClassInfo[] parameters;

    /**
     * The exception types
     */
    private ClassInfo[] exceptions;

    /**
     * The declaring class
     */
    private final ClassInfo declaringClass;

    /**
     * A visitor to load type infos
     */
    private final TypeVisitor typeVisitor;

    /**
     * Creates a new method info
     * 
     * @param f the method tree
     * @param cf the class finder
     * @param dc the declaring class
     */
    public TreeMethodInfo(final MethodDeclaration f, final ClassFinder cf, final ClassInfo dc) {
        this.methodTree = f;
        this.classFinder = cf;
        this.declaringClass = dc;
        this.typeVisitor = new TypeVisitor(this.classFinder, this.declaringClass);
    }

    /**
     * Returns the method declaration
     */
    public MethodDeclaration getMethodDeclaration() {
        return this.methodTree;
    }

    /**
     * Returns the modifiers for the method represented by this object
     */
    public int getModifiers() {
        return this.methodTree.getAccessFlags();
    }

    /**
     * Returns a Class object that represents the return type of the method represented by this
     * object
     */
    public ClassInfo getReturnType() {
        return (ClassInfo) this.methodTree.getReturnType().acceptVisitor(this.typeVisitor);
    }

    /**
     * Returns the name of the underlying method
     */
    public String getName() {
        return this.methodTree.getName();
    }

    /**
     * Returns an array of class infos that represent the parameter types, in declaration order, of
     * the method represented by this object
     */
    public ClassInfo[] getParameterTypes() {
        if (this.parameters == null) {
            final List ls = this.methodTree.getParameters();
            final Iterator it = ls.iterator();
            this.parameters = new ClassInfo[ls.size()];
            int i = 0;

            while (it.hasNext()) {
                final FormalParameter fp = (FormalParameter) it.next();
                this.parameters[i++] = (ClassInfo) fp.getType().acceptVisitor(this.typeVisitor);
            }
        }
        return this.parameters.clone();
    }

    /**
     * Returns an array of Class infos that represent the types of the exceptions declared to be
     * thrown by the underlying method
     */
    public ClassInfo[] getExceptionTypes() {
        if (this.exceptions == null) {
            final List ls = this.methodTree.getExceptions();
            final Iterator it = ls.iterator();
            this.exceptions = new ClassInfo[ls.size()];
            int i = 0;
            while (it.hasNext()) {
                this.exceptions[i++] = lookupClass((String) it.next(), this.declaringClass);
            }
        }
        return this.exceptions.clone();
    }

    /**
     * Indicates whether some other object is "equal to" this one
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof TreeMethodInfo)) {
            return false;
        }
        return this.methodTree.equals(((TreeMethodInfo) obj).methodTree);
    }

    /**
     * Looks for a class from its name
     * 
     * @param s the name of the class to find
     * @param c the context
     * @exception NoClassDefFoundError if the class cannot be loaded
     */
    protected ClassInfo lookupClass(final String s, final ClassInfo c) {
        try {
            if (c == null) {
                return this.classFinder.lookupClass(s, c);
            } else {
                return this.classFinder.lookupClass(s);
            }
        } catch (final ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

}
