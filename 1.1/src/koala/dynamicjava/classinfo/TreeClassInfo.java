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

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import koala.dynamicjava.tree.ClassDeclaration;
import koala.dynamicjava.tree.ConstructorDeclaration;
import koala.dynamicjava.tree.ConstructorInvocation;
import koala.dynamicjava.tree.FieldDeclaration;
import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.Identifier;
import koala.dynamicjava.tree.InterfaceDeclaration;
import koala.dynamicjava.tree.MethodDeclaration;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.QualifiedName;
import koala.dynamicjava.tree.ReferenceType;
import koala.dynamicjava.tree.SimpleAssignExpression;
import koala.dynamicjava.tree.TypeDeclaration;
import koala.dynamicjava.tree.visitor.VisitorObject;

/**
 * The instances of this class provides informations about classes not yet compiled to JVM bytecode
 * and represented by a syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/10/25
 */

public class TreeClassInfo implements ClassInfo {
    /**
     * The declaringClass property is defined for each inner class/interface declaration It contains
     * a TypeDeclaration
     */
    private final static String DECLARING_CLASS = "declaringClass";

    /**
     * The declaringClass property is defined for each anonymous inner class/interface declaration
     * It contains a TypeDeclaration
     */
    public final static String ANONYMOUS_DECLARING_CLASS = "anonymousDeclaringClass";

    /**
     * This property is used to ensure that the modifications on the tree are not done twice
     */
    private final static String TREE_VISITED = "treeVisited";

    /**
     * The abstract syntax tree of this class
     */
    private final TypeDeclaration classTree;

    /**
     * The class finder for this class
     */
    private final ClassFinder classFinder;

    /**
     * The dimension of this type
     */
    private int dimension;

    /**
     * The full class name
     */
    private final String name;

    /**
     * The class info of the superclass of the class represented by this field
     */
    private ClassInfo superclass;

    /**
     * Whether this class is an interface
     */
    private boolean interfaceInfo;

    /**
     * The interfaces
     */
    private ClassInfo[] interfaces;

    /**
     * The fields
     */
    private final Map fields = new HashMap();

    /**
     * The methods
     */
    private final Map methods = new HashMap();

    /**
     * The constructors
     */
    private final List constructors = new LinkedList();

    /**
     * The declared classes
     */
    private final List classes = new LinkedList();

    /**
     * The compilable property value
     */
    private boolean compilable = true;

    /**
     * The method count
     */
    private int methodCount;

    /**
     * Creates a new class info
     * 
     * @param cd the class declaration
     * @param cf the class finder
     */
    public TreeClassInfo(final TypeDeclaration cd, final ClassFinder cf) {
        this.classFinder = cf;
        this.classTree = cd;
        this.name = fullName();
        this.interfaceInfo = cd instanceof InterfaceDeclaration;
        new MembersVisitor();
        this.classTree.setProperty(TREE_VISITED, null);
    }

    /**
     * Creates a new array class info
     * 
     * @param ci the class info
     */
    public TreeClassInfo(final TreeClassInfo ci) {
        this.classFinder = ci.classFinder;
        this.classTree = ci.classTree;
        this.dimension = ci.dimension + 1;
        this.name = "[" + (ci.isArray() ? ci.getName() : "L" + ci.getName() + ";");
        new MembersVisitor();
    }

    /**
     * Returns the underlying class
     */
    public Class getJavaClass() {
        throw new IllegalStateException();
    }

    /**
     * Returns the abstract syntax tree
     */
    public TypeDeclaration getTypeDeclaration() {
        return this.classTree;
    }

    /**
     * Returns the class finder
     */
    public ClassFinder getClassFinder() {
        return this.classFinder;
    }

    /**
     * Whether the underlying class needs compilation
     */
    public boolean isCompilable() {
        return this.compilable;
    }

    /**
     * Sets the compilable property
     */
    public void setCompilable(final boolean b) {
        this.compilable = b;
    }

    /**
     * Returns the declaring class or null
     */
    public ClassInfo getDeclaringClass() {
        return this.dimension == 0 ? (ClassInfo) this.classTree.getProperty(DECLARING_CLASS) : null;
    }

    /**
     * Returns the declaring class of an anonymous class or null
     */
    public ClassInfo getAnonymousDeclaringClass() {
        return this.dimension == 0 ? (ClassInfo) this.classTree.getProperty(ANONYMOUS_DECLARING_CLASS) : null;
    }

    /**
     * Returns the modifiers flags
     */
    public int getModifiers() {
        return this.dimension == 0 ? this.classTree.getAccessFlags() : Modifier.PUBLIC;
    }

    /**
     * Returns the fully qualified name of the underlying class
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the class info of the superclass of the class represented by this class
     * 
     * @exception NoClassDefFoundError if the class cannot be loaded
     */
    public ClassInfo getSuperclass() {
        if (this.superclass == null) {
            if (this.interfaceInfo) {
                this.superclass = lookupClass("java.lang.Object");
            } else {
                final ClassDeclaration cd = (ClassDeclaration) this.classTree;
                this.superclass = lookupClass(cd.getSuperclass(), getDeclaringClass());
            }
        }
        return this.superclass;
    }

    /**
     * Returns the class infos of the interfaces implemented by the class this info represents
     * 
     * @exception NoClassDefFoundError if an interface cannot be loaded
     */
    public ClassInfo[] getInterfaces() {
        if (this.interfaces == null) {
            if (this.dimension > 0) {
                this.interfaces = new ClassInfo[] { lookupClass("java.lang.Cloneable"), lookupClass("java.io.Serializable") };
            } else {
                final List l = this.classTree.getInterfaces();
                if (l != null) {
                    this.interfaces = new ClassInfo[l.size()];
                    final Iterator it = l.iterator();
                    int i = 0;
                    while (it.hasNext()) {
                        final String s = (String) it.next();
                        this.interfaces[i++] = lookupClass(s, getDeclaringClass());
                    }
                } else {
                    this.interfaces = new ClassInfo[0];
                }
            }
        }
        return this.interfaces.clone();
    }

    /**
     * Returns the field represented by the given node
     * 
     * @param node the node that represents the field
     */
    public FieldInfo getField(final FieldDeclaration node) {
        return (TreeFieldInfo) this.fields.get(node.getName());
    }

    /**
     * Returns the field infos for the current class
     */
    public FieldInfo[] getFields() {
        if (this.dimension == 0) {
            final Set keys = this.fields.keySet();
            final Iterator it = keys.iterator();

            final FieldInfo[] result = new FieldInfo[keys.size()];
            int i = 0;
            while (it.hasNext()) {
                result[i++] = (FieldInfo) this.fields.get(it.next());
            }
            return result;
        } else {
            return new FieldInfo[0];
        }
    }

    /**
     * Returns the constructor infos for the current class
     */
    public ConstructorInfo[] getConstructors() {
        if (this.dimension == 0) {
            final Iterator it = this.constructors.iterator();
            final ConstructorInfo[] result = new ConstructorInfo[this.constructors.size()];
            int i = 0;
            while (it.hasNext()) {
                result[i++] = (ConstructorInfo) it.next();
            }
            return result;
        } else {
            return new ConstructorInfo[0];
        }
    }

    /**
     * Returns the method represented by the given node
     * 
     * @param node the node that represents the method
     */
    public MethodInfo getMethod(final MethodDeclaration node) {
        final Set keys = this.methods.keySet();
        final Iterator it = keys.iterator();

        while (it.hasNext()) {
            final List l = (List) this.methods.get(it.next());
            final Iterator lit = l.iterator();
            while (lit.hasNext()) {
                final TreeMethodInfo mi = (TreeMethodInfo) lit.next();
                if (mi.getMethodDeclaration() == node) {
                    return mi;
                }
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Returns the method infos for the current class
     */
    public MethodInfo[] getMethods() {
        if (this.dimension == 0) {
            final MethodInfo[] result = new MethodInfo[this.methodCount];
            final Iterator it = this.methods.values().iterator();
            int i = 0;
            while (it.hasNext()) {
                final Iterator lit = ((List) it.next()).iterator();
                while (lit.hasNext()) {
                    result[i++] = (MethodInfo) lit.next();
                }
            }
            return result;
        } else {
            return new MethodInfo[0];
        }
    }

    /**
     * Returns the classes and interfaces declared as members of the class represented by this
     * ClassInfo object.
     */
    public ClassInfo[] getDeclaredClasses() {
        if (this.dimension == 0) {
            final Iterator it = this.classes.iterator();
            final ClassInfo[] result = new ClassInfo[this.classes.size()];
            int i = 0;
            while (it.hasNext()) {
                result[i++] = (ClassInfo) it.next();
            }
            return result;
        } else {
            return new ClassInfo[0];
        }
    }

    /**
     * Returns the array type that contains elements of this class
     */
    public ClassInfo getArrayType() {
        return new TreeClassInfo(this);
    }

    /**
     * Whether this object represents an interface
     */
    public boolean isInterface() {
        return this.classTree instanceof InterfaceDeclaration;
    }

    /**
     * Whether this object represents an array
     */
    public boolean isArray() {
        return this.dimension > 0;
    }

    /**
     * Whether this object represents a primitive type
     */
    public boolean isPrimitive() {
        return false;
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

        TreeClassInfo bt = new TreeClassInfo(this.classTree, this.classFinder);
        for (int i = 0; i < this.dimension - 1; i++) {
            bt = new TreeClassInfo(bt);
        }
        return bt;
    }

    /**
     * Indicates whether some other object is "equal to" this one
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof ClassInfo)) {
            return false;
        }
        return getName().equals(((ClassInfo) obj).getName());
    }

    /**
     * Returns the full name of this class
     */
    private String fullName() {
        String s;
        final ClassInfo ci = (ClassInfo) this.classTree.getProperty(DECLARING_CLASS);
        if (ci != null) {
            s = ci.getName() + "$";
        } else {
            s = this.classFinder.getCurrentPackage();
            if (!s.equals("")) {
                s += ".";
            }
        }
        return s + this.classTree.getName();
    }

    /**
     * Looks for a class from its name
     * 
     * @param s the name of the class to find
     * @exception NoClassDefFoundError if the class cannot be loaded
     */
    private ClassInfo lookupClass(final String s) {
        try {
            return this.classFinder.lookupClass(s);
        } catch (final ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    /**
     * Looks for a class from its name
     * 
     * @param s the name of the class to find
     * @param c the context
     * @exception NoClassDefFoundError if the class cannot be loaded
     */
    private ClassInfo lookupClass(final String s, final ClassInfo c) {
        try {
            if (c != null) {
                return this.classFinder.lookupClass(s, c);
            } else {
                return this.classFinder.lookupClass(s);
            }
        } catch (final ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    /**
     * Returns the nesting level of the class
     */
    private int getNestingLevel() {
        int result = -1;
        ClassInfo ci = this;
        while (!Modifier.isStatic(ci.getModifiers()) && (ci = ci.getDeclaringClass()) != null) {
            result++;
        }
        return result;
    }

    /**
     * To initialize the ClassInfo
     */
    private class MembersVisitor extends VisitorObject {
        /**
         * Creates a new members visitor and iterate over the members of the class represented by
         * this ClassInfo
         */
        MembersVisitor() {
            if (!isArray()) {
                final Iterator it = TreeClassInfo.this.classTree.getMembers().iterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(this);
                }

                if (!TreeClassInfo.this.classTree.hasProperty(TREE_VISITED)) {
                    final ClassInfo dc = getDeclaringClass();
                    if (dc != null && !Modifier.isStatic(getModifiers())) {
                        // Add a reference to the outer instance
                        FieldDeclaration fd;
                        fd = new FieldDeclaration(Modifier.PUBLIC, new ReferenceType(dc.getName()), "this$" + getNestingLevel(), null);
                        fd.acceptVisitor(this);
                        TreeClassInfo.this.classTree.getMembers().add(fd);
                    }

                    if (TreeClassInfo.this.constructors.size() == 0 && !isInterface() && !isPrimitive()) {

                        // Add a default constructor
                        ConstructorInvocation ci;
                        ci = new ConstructorInvocation(null, null, true);
                        ConstructorDeclaration cd;
                        cd = new ConstructorDeclaration(Modifier.PUBLIC, TreeClassInfo.this.classTree.getName(), new LinkedList(), new LinkedList(), ci, new LinkedList());
                        cd.acceptVisitor(this);
                        TreeClassInfo.this.classTree.getMembers().add(cd);
                    }
                }
            }
        }

        /**
         * Visits a ClassDeclaration
         * 
         * @param node the node to visit
         */
        @Override
        public Object visit(final ClassDeclaration node) {
            node.setProperty(DECLARING_CLASS, TreeClassInfo.this);
            TreeClassInfo.this.classes.add(TreeClassInfo.this.classFinder.addClassInfo(getName() + "$" + node.getName(), node));
            return null;
        }

        /**
         * Visits a ClassDeclaration
         * 
         * @param node the node to visit
         */
        @Override
        public Object visit(final InterfaceDeclaration node) {
            node.setProperty(DECLARING_CLASS, TreeClassInfo.this);
            TreeClassInfo.this.classes.add(TreeClassInfo.this.classFinder.addClassInfo(getName() + "$" + node.getName(), node));
            return null;
        }

        /**
         * Visits a FieldDeclaration
         * 
         * @param node the node to visit
         */
        @Override
        public Object visit(final FieldDeclaration node) {
            TreeClassInfo.this.fields.put(node.getName(), new TreeFieldInfo(node, TreeClassInfo.this.classFinder, TreeClassInfo.this));
            return null;
        }

        /**
         * Visits a ConstructorDeclaration
         * 
         * @param node the node to visit
         */
        @Override
        public Object visit(final ConstructorDeclaration node) {
            if (node.getConstructorInvocation() == null) {
                ConstructorInvocation ci;
                ci = new ConstructorInvocation(null, null, true);
                node.setConstructorInvocation(ci);
            }

            // Add the outer parameter if needed
            final ClassInfo dc = getDeclaringClass();
            if (!TreeClassInfo.this.classTree.hasProperty(TREE_VISITED)) {
                if (dc != null && !Modifier.isStatic(getModifiers())) {
                    final ReferenceType t = new ReferenceType(dc.getName());
                    node.getParameters().add(0, new FormalParameter(false, t, "param$0"));
                }
            }

            if (dc != null && !Modifier.isStatic(getModifiers())) {
                // Add the initialization of the outer instance reference
                SimpleAssignExpression sae;
                final List l1 = new LinkedList();
                l1.add(new Identifier("this$" + getNestingLevel()));
                final List l2 = new LinkedList();
                l2.add(new Identifier("param$0"));
                sae = new SimpleAssignExpression(new QualifiedName(l1), new QualifiedName(l2));
                node.getStatements().add(0, sae);
            }

            TreeClassInfo.this.constructors.add(new TreeConstructorInfo(node, TreeClassInfo.this.classFinder, TreeClassInfo.this));
            return null;
        }

        /**
         * Visits a MethodDeclaration
         * 
         * @param node the node to visit
         */
        @Override
        public Object visit(final MethodDeclaration node) {
            List l = (List) TreeClassInfo.this.methods.get(node.getName());
            if (l == null) {
                l = new LinkedList();
            }

            l.add(new TreeMethodInfo(node, TreeClassInfo.this.classFinder, TreeClassInfo.this));
            TreeClassInfo.this.methods.put(node.getName(), l);
            TreeClassInfo.this.methodCount++;
            return null;
        }
    }
}
