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

package koala.dynamicjava.interpreter.context;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import koala.dynamicjava.classinfo.JavaClassInfo;
import koala.dynamicjava.classinfo.TreeClassInfo;
import koala.dynamicjava.interpreter.Interpreter;
import koala.dynamicjava.interpreter.InterpreterUtilities;
import koala.dynamicjava.interpreter.NodeProperties;
import koala.dynamicjava.interpreter.TreeCompiler;
import koala.dynamicjava.interpreter.error.CatchedExceptionError;
import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.interpreter.modifier.LeftHandSideModifier;
import koala.dynamicjava.interpreter.modifier.SuperFieldModifier;
import koala.dynamicjava.tree.ClassAllocation;
import koala.dynamicjava.tree.ClassDeclaration;
import koala.dynamicjava.tree.ConstructorDeclaration;
import koala.dynamicjava.tree.ConstructorInvocation;
import koala.dynamicjava.tree.Expression;
import koala.dynamicjava.tree.FieldDeclaration;
import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.Identifier;
import koala.dynamicjava.tree.IdentifierToken;
import koala.dynamicjava.tree.MethodDeclaration;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.QualifiedName;
import koala.dynamicjava.tree.ReferenceType;
import koala.dynamicjava.tree.SimpleAssignExpression;
import koala.dynamicjava.tree.StaticFieldAccess;
import koala.dynamicjava.tree.SuperFieldAccess;
import koala.dynamicjava.tree.TreeUtilities;
import koala.dynamicjava.tree.TypeDeclaration;
import koala.dynamicjava.tree.TypeExpression;
import koala.dynamicjava.util.AmbiguousFieldException;
import koala.dynamicjava.util.ImportationManager;
import koala.dynamicjava.util.ReflectionUtilities;

/**
 * A static method context.
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/28
 */

public class StaticContext extends GlobalContext {
    /**
     * The declaring class of the method
     */
    protected Class declaringClass;

    /**
     * The default qualifier
     */
    protected Node defaultQualifier;

    /**
     * Creates a new context
     * 
     * @param i the interpreter
     * @param c the declaring class of the method
     * @param im the importation manager
     */
    public StaticContext(final Interpreter i, final Class c, final ImportationManager im) {
        super(i);
        this.declaringClass = c;
        this.importationManager = im;
        this.defaultQualifier = new ReferenceType(c.getName());
    }

    /**
     * Creates a new context
     * 
     * @param i the interpreter
     * @param c the declaring class of the method
     * @param fp the formal parameters
     */
    public StaticContext(final Interpreter i, final Class c, final Set fp) {
        super(i, fp);
        this.declaringClass = c;
        this.defaultQualifier = new ReferenceType(c.getName());
    }

    /**
     * Tests whether a variable is defined in this context
     * 
     * @param name the name of the entry
     * @return false if the variable is undefined
     */
    @Override
    public boolean isDefined(final String name) {
        return isDefinedVariable(name) || fieldExists(name);
    }

    /**
     * Looks for a field
     * 
     * @param fc the field class
     * @param fn the field name
     * @exception NoSuchFieldException if the field cannot be found
     * @exception AmbiguousFieldException if the field is ambiguous
     */
    @Override
    public Field getField(final Class fc, final String fn) throws NoSuchFieldException, AmbiguousFieldException {
        Field f;
        try {
            f = ReflectionUtilities.getField(fc, fn);
        } catch (final Exception e) {
            f = InterpreterUtilities.getOuterField(fc, fn);
        }
        setAccessFlag(f);
        return f;
    }

    /**
     * Creates the tree that is associated with the given name
     * 
     * @param node the current node
     * @param name the variable name
     * @exception IllegalStateException if the variable is not defined
     */
    @Override
    public Expression createName(final Node node, final IdentifierToken name) {
        if (isDefinedVariable(name.image())) {
            return super.createName(node, name);
        } else {
            final String fname = name.image();
            try {
                ReflectionUtilities.getField(this.declaringClass, fname);
                return new StaticFieldAccess((ReferenceType) this.defaultQualifier, fname);
            } catch (final Exception e) {
                try {
                    final Field f = InterpreterUtilities.getOuterField(this.declaringClass, fname);
                    final Class c = f.getDeclaringClass();
                    return new StaticFieldAccess(new ReferenceType(c.getName()), fname);
                } catch (final Exception ex) {
                    throw new CatchedExceptionError(ex, node);
                }
            }
        }
    }

    /**
     * Returns the default qualifier for this context
     * 
     * @param node the current node
     */
    @Override
    public Node getDefaultQualifier(final Node node) {
        return this.defaultQualifier;
    }

    /**
     * Returns the modifier that match the given node
     * 
     * @param node a tree node
     */
    @Override
    public LeftHandSideModifier getModifier(final SuperFieldAccess node) {
        return new SuperFieldModifier((Field) node.getProperty(NodeProperties.FIELD), node);
    }

    /**
     * Looks for a method
     * 
     * @param prefix the method prefix
     * @param mname the method name
     * @param params the parameter types
     * @exception NoSuchMethodException if the method cannot be found
     */
    @Override
    public Method lookupMethod(final Node prefix, final String mname, final Class[] params) throws NoSuchMethodException {
        final Class c = NodeProperties.getType(prefix);
        Method m = null;
        try {
            m = ReflectionUtilities.lookupMethod(c, mname, params);
            setAccessFlag(m);
            if (m.getName().equals("clone")) {
                m.setAccessible(true);
            }
            return m;
        } catch (final NoSuchMethodException e) {
            if (prefix instanceof ReferenceType && c == this.declaringClass) {
                m = InterpreterUtilities.lookupOuterMethod(c, mname, params);
                m.setAccessible(true);
                return m;
            } else {
                throw e;
            }
        }
    }

    /**
     * Looks for a field in the super class
     * 
     * @param node the current node
     * @param fn the field name
     * @exception NoSuchFieldException if the field cannot be found
     * @exception AmbiguousFieldException if the field is ambiguous
     */
    @Override
    public Field getSuperField(final Node node, final String fn) throws NoSuchFieldException, AmbiguousFieldException {
        final Class sc = this.declaringClass.getSuperclass();
        final Field result = ReflectionUtilities.getField(sc, fn);
        setAccessFlag(result);
        return result;
    }

    /**
     * Looks for a class
     * 
     * @param cname the class name
     * @exception ClassNotFoundException if the class cannot be found
     */
    @Override
    public Class lookupClass(final String cname) throws ClassNotFoundException {
        try {
            return this.importationManager.lookupClass(cname, this.declaringClass.getName());
        } catch (final ClassNotFoundException e) {
            Class dc = this.declaringClass.getDeclaringClass();
            if (dc != null) {
                try {
                    return this.importationManager.lookupClass(cname, dc.getName());
                } catch (final Exception ex) {
                }
            }
            dc = this.declaringClass.getSuperclass();
            while (dc != null) {
                try {
                    return this.importationManager.lookupClass(cname, dc.getName());
                } catch (final Exception ex) {
                }
                dc = dc.getSuperclass();
            }
            throw e;
        }
    }

    /**
     * Defines a MethodDeclaration as a function
     * 
     * @param node the function declaration
     */
    @Override
    public void defineFunction(final MethodDeclaration node) {
        throw new IllegalStateException("internal.error");
    }

    /**
     * Defines a class from its syntax tree
     * 
     * @param node the class declaration
     */
    @Override
    public void defineClass(final TypeDeclaration node) {
        throw new ExecutionError("not.implemented");
    }

    /**
     * Sets the properties of a ClassAllocation node
     * 
     * @param node the allocation node
     * @param c the class of the constructor
     * @param args the classes of the arguments of the constructor
     * @param memb the class members
     */
    @Override
    public Class setProperties(final ClassAllocation node, final Class c, Class[] args, final List memb) {
        final String dname = this.declaringClass.getName();
        final String cname = dname + "$" + classCount++;
        FieldDeclaration fd;
        ConstructorDeclaration csd;

        // Create the reference to the declaring class
        fd = new FieldDeclaration(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, CLASS_TYPE, "declaring$Class$Reference$0", new TypeExpression(new ReferenceType(dname)));
        memb.add(fd);

        // Add the reference to the final local variables map
        memb.add(LOCALS);

        // Create the reference to the final local variables map
        fd = new FieldDeclaration(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, OBJECT_ARRAY_ARRAY, "local$Variables$Class$0", createClassArrayInitializer());
        memb.add(fd);

        // Create the constructor
        final List params = new LinkedList();
        final List stmts = new LinkedList();

        // Add the final local variables map parameter
        params.add(new FormalParameter(false, MAP_TYPE, "param$0"));

        // Add the other parameters
        final List superArgs = new LinkedList();
        for (int i = 0; i < args.length; i++) {
            params.add(new FormalParameter(false, TreeUtilities.classToType(args[i]), "param$" + (i + 1)));
            final List l = new LinkedList();
            l.add(new Identifier("param$" + (i + 1)));
            superArgs.add(new QualifiedName(l));
        }

        // Create the explicit constructor invocation
        ConstructorInvocation ci = null;
        if (superArgs.size() > 0) {
            ci = new ConstructorInvocation(null, superArgs, true);
        }

        // Add the outer instance reference initialization statement
        final List p1 = new LinkedList();
        p1.add(new Identifier("local$Variables$Reference$0"));
        final List p2 = new LinkedList();
        p2.add(new Identifier("param$0"));
        stmts.add(new SimpleAssignExpression(new QualifiedName(p1), new QualifiedName(p2)));

        csd = new ConstructorDeclaration(Modifier.PUBLIC, cname, params, new LinkedList(), ci, stmts);
        memb.add(csd);

        // Set the inheritance
        List ext = null;
        List impl = null;
        if (c.isInterface()) {
            impl = new LinkedList();
            final List intf = new LinkedList();
            intf.add(new Identifier(c.getName()));
            impl.add(intf);
        } else {
            ext = new LinkedList();
            ext.add(new Identifier(c.getName()));
        }

        // Create the class
        final TypeDeclaration type = new ClassDeclaration(Modifier.PUBLIC, cname, ext, impl, memb);

        type.setProperty(TreeClassInfo.ANONYMOUS_DECLARING_CLASS, new JavaClassInfo(this.declaringClass));

        final Class cl = new TreeCompiler(this.interpreter).compileTree(this, type);

        // Update the argument types
        final Class[] tmp = new Class[args.length + 1];
        tmp[0] = Map.class;
        for (int i = 1; i < tmp.length; i++) {
            tmp[i] = args[i - 1];
        }
        args = tmp;
        try {
            node.setProperty(NodeProperties.CONSTRUCTOR, lookupConstructor(cl, args));
        } catch (final NoSuchMethodException e) {
            // Never get here
            e.printStackTrace();
        }
        node.setProperty(NodeProperties.TYPE, cl);
        return cl;
    }

    /**
     * Looks for a super method
     * 
     * @param node the current node
     * @param mname the method name
     * @param params the parameter types
     * @exception NoSuchMethodException if the method cannot be found
     */
    @Override
    public Method lookupSuperMethod(final Node node, final String mname, final Class[] params) throws NoSuchMethodException {
        Method m = null;
        try {
            m = ReflectionUtilities.lookupMethod(this.declaringClass, "super$" + mname, params);
        } catch (final NoSuchMethodException e) {
            m = ReflectionUtilities.lookupMethod(this.declaringClass, mname, params);
        }
        setAccessFlag(m);
        return m;
    }

    /**
     * Whether a simple identifier is a class
     * 
     * @param name the identifier
     */
    @Override
    public boolean classExists(final String name) {
        boolean result = false;
        this.importationManager.setClassLoader(new PseudoClassLoader());
        try {
            this.importationManager.lookupClass(name, this.declaringClass.getName());
            result = true;
        } catch (final ClassNotFoundException e) {
        } catch (final PseudoError e) {
            result = true;
        } finally {
            if (this.classLoader == null) {
                this.importationManager.setClassLoader(this.interpreter.getClassLoader());
            } else {
                this.importationManager.setClassLoader(this.classLoader);
            }
        }
        return result;
    }

    /**
     * Sets the access flag of a member
     */
    @Override
    protected void setAccessFlag(final Member m) {
        final int mods = m.getModifiers();
        final Class c = m.getDeclaringClass();
        final int cmods = c.getModifiers();
        final String pkg = this.importationManager.getCurrentPackage();
        final String mp = getPackageName(c);
        final boolean samePkg = pkg.equals(mp);

        if (Modifier.isPublic(cmods) || samePkg) {
            if (Modifier.isPublic(mods)) {
                ((AccessibleObject) m).setAccessible(true);
            } else if (Modifier.isProtected(mods)) {
                if (c.isAssignableFrom(this.declaringClass.getSuperclass()) || samePkg) {
                    ((AccessibleObject) m).setAccessible(true);
                }
            } else if (!Modifier.isPrivate(mods)) {
                if (samePkg) {
                    ((AccessibleObject) m).setAccessible(true);
                }
            } else {
                if (this.declaringClass == c || isInnerClass(this.declaringClass, c)) {
                    ((AccessibleObject) m).setAccessible(true);
                }
            }
        }
    }

    /**
     * Is c1 an inner class of c2?
     */
    protected boolean isInnerClass(Class c1, final Class c2) {
        Class c = c1.getDeclaringClass();
        if (c == null) {
            try {
                final Field f = c1.getField("declaring$Class$Reference$0");
                c = (Class) f.get(null);
            } catch (final Exception e) {
            }
        }
        c1 = c;
        while (c != null) {
            if (c == c2) {
                return true;
            }
            c = c.getDeclaringClass();
            if (c == null) {
                try {
                    final Field f = c1.getField("declaring$Class$Reference$0");
                    c = (Class) f.get(null);
                } catch (final Exception e) {
                }
            }
            c1 = c;
        }
        return false;
    }

    /**
     * Whether the given name represents a field in this context
     * 
     * @param name the field name
     */
    protected boolean fieldExists(final String name) {
        boolean result = false;
        try {
            ReflectionUtilities.getField(this.declaringClass, name);
            result = true;
        } catch (final NoSuchFieldException e) {
            try {
                InterpreterUtilities.getOuterField(this.declaringClass, name);
                result = true;
            } catch (final NoSuchFieldException ex) {
            } catch (final AmbiguousFieldException ex) {
                result = true;
            }
        } catch (final AmbiguousFieldException e) {
            result = true;
        }
        return result;
    }
}
