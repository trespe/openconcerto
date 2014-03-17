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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
import koala.dynamicjava.interpreter.throwable.ThrownException;
import koala.dynamicjava.tree.ClassAllocation;
import koala.dynamicjava.tree.ClassDeclaration;
import koala.dynamicjava.tree.ConstructorDeclaration;
import koala.dynamicjava.tree.ConstructorInvocation;
import koala.dynamicjava.tree.Expression;
import koala.dynamicjava.tree.FieldDeclaration;
import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.Identifier;
import koala.dynamicjava.tree.IdentifierToken;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.ObjectFieldAccess;
import koala.dynamicjava.tree.QualifiedName;
import koala.dynamicjava.tree.ReferenceType;
import koala.dynamicjava.tree.SimpleAllocation;
import koala.dynamicjava.tree.SimpleAssignExpression;
import koala.dynamicjava.tree.TreeUtilities;
import koala.dynamicjava.tree.TypeDeclaration;
import koala.dynamicjava.tree.TypeExpression;
import koala.dynamicjava.util.ImportationManager;
import koala.dynamicjava.util.ReflectionUtilities;

/**
 * A method method context.
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/28
 */

public class MethodContext extends StaticContext {
    /**
     * The "this" identifier
     */
    protected final static Identifier thisIdentifier = new Identifier("this");

    /**
     * Creates a new context
     * 
     * @param i the interpreter
     * @param c the declaring class of the method
     * @param obj the current object
     * @param im the importation manager
     */
    public MethodContext(final Interpreter i, final Class c, final Object obj, final ImportationManager im) {
        super(i, c, im);
        this.importationManager = im;

        final List l = new LinkedList();
        l.add(thisIdentifier);
        this.defaultQualifier = new QualifiedName(l);
        setConstant("this", obj);
    }

    /**
     * Creates a new context
     * 
     * @param i the interpreter
     * @param c the declaring class of the method
     * @param obj the current object
     * @param fp the formal parameters
     */
    public MethodContext(final Interpreter i, final Class c, final Object obj, final Set fp) {
        super(i, c, fp);

        final List l = new LinkedList();
        l.add(thisIdentifier);
        this.defaultQualifier = new QualifiedName(l);
        setConstant("this", obj);
    }

    /**
     * Returns the default qualifier for this context
     * 
     * @param s the qualifier of 'this'
     */
    @Override
    public Node getDefaultQualifier(final Node node, final String tname) {
        if (tname.equals("")) {
            return this.defaultQualifier;
        } else {
            try {
                final Class c = lookupClass(tname);
                Class t = this.declaringClass;
                Node result = this.defaultQualifier;
                while (t != null) {
                    if (t == c) {
                        return result;
                    }
                    result = new ObjectFieldAccess((Expression) result, getOuterThisName(t));
                    t = InterpreterUtilities.getDeclaringClass(t);
                }
                throw new ExecutionError("this.expression", node);
            } catch (final ClassNotFoundException e) {
                throw new CatchedExceptionError(e, node);
            }
        }
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
            Field f = null;
            try {
                f = ReflectionUtilities.getField(this.declaringClass, name.image());
                return new ObjectFieldAccess((Expression) this.defaultQualifier, name.image());
            } catch (final Exception e) {
                try {
                    f = InterpreterUtilities.getOuterField(this.declaringClass, name.image());
                    Expression exp = (Expression) this.defaultQualifier;
                    Class c = this.declaringClass;
                    final Class fc = f.getDeclaringClass();
                    while (!fc.isAssignableFrom(c)) {
                        exp = new ObjectFieldAccess(exp, getOuterThisName(c));
                        c = InterpreterUtilities.getDeclaringClass(c);
                    }
                    return new ObjectFieldAccess(exp, name.image());
                } catch (final Exception ex) {
                    throw new CatchedExceptionError(e, node);
                }
            }
        }
    }

    /**
     * Returns the default argument to pass to methods in this context
     */
    @Override
    public Object getHiddenArgument() {
        return get("this");
    }

    /**
     * Sets the properties of a SimpleAllocation node
     * 
     * @param node the allocation node
     * @param c the class of the constructor
     * @param cargs the classes of the arguments of the constructor
     */
    @Override
    public Class setProperties(final SimpleAllocation node, final Class c, final Class[] cargs) {
        Constructor cons = null;
        try {
            cons = lookupConstructor(c, cargs);
        } catch (final Exception e) {
            // Innerclass management
            if (isInnerclass(c, this.declaringClass)) {
                final Class[] cs = new Class[cargs.length + 1];
                cs[0] = this.declaringClass;
                for (int i = 1; i < cs.length; i++) {
                    cs[i] = cargs[i - 1];
                }
                try {
                    cons = lookupConstructor(c, cs);
                } catch (final Exception ex) {
                    throw new CatchedExceptionError(e, node);
                }
                node.setProperty(NodeProperties.INNER_ALLOCATION, null);
            } else if (c.getDeclaringClass() != null && c.getDeclaringClass() == this.declaringClass.getDeclaringClass()) {
                final Class[] cs = new Class[cargs.length + 1];
                cs[0] = this.declaringClass.getDeclaringClass();
                for (int i = 1; i < cs.length; i++) {
                    cs[i] = cargs[i - 1];
                }
                try {
                    cons = lookupConstructor(c, cs);
                } catch (final Exception ex) {
                    throw new CatchedExceptionError(e, node);
                }
                node.setProperty(NodeProperties.OUTER_INNER_ALLOCATION, null);
            } else {
                throw new CatchedExceptionError(e, node);
            }
        }

        // Set the properties of this node
        node.setProperty(NodeProperties.TYPE, c);
        node.setProperty(NodeProperties.CONSTRUCTOR, cons);
        return c;
    }

    /**
     * Invokes a constructor
     * 
     * @param node the SimpleAllocation node
     * @param args the arguments
     */
    @Override
    public Object invokeConstructor(final SimpleAllocation node, Object[] args) {
        final Constructor cons = (Constructor) node.getProperty(NodeProperties.CONSTRUCTOR);

        if (node.hasProperty(NodeProperties.INNER_ALLOCATION)) {
            final Object[] t = new Object[args.length + 1];
            t[0] = getHiddenArgument();
            for (int i = 1; i < t.length; i++) {
                t[i] = args[i - 1];
            }
            args = t;
        } else if (node.hasProperty(NodeProperties.OUTER_INNER_ALLOCATION)) {
            final Object[] t = new Object[args.length + 1];
            final Field[] fs = this.declaringClass.getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].getName().startsWith("this$")) {
                    try {
                        fs[i].setAccessible(true);
                        t[0] = fs[i].get(getHiddenArgument());
                    } catch (final IllegalAccessException e) {
                        throw new CatchedExceptionError(e, node);
                    }
                    break;
                }
            }
            for (int i = 1; i < t.length; i++) {
                t[i] = args[i - 1];
            }
            args = t;
        }

        try {
            return cons.newInstance(args);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            }
            throw new ThrownException(e.getTargetException());
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }
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
        final ReferenceType otype = new ReferenceType(dname);

        // Create the reference to the declaring class
        fd = new FieldDeclaration(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, CLASS_TYPE, "declaring$Class$Reference$0", new TypeExpression(otype));
        memb.add(fd);

        // create the reference to the declaring instance
        fd = new FieldDeclaration(Modifier.PUBLIC, otype, "this$0", null);
        memb.add(fd);

        // Add the reference to the final local variables map
        memb.add(LOCALS);

        // Create the reference to the final local variables map
        fd = new FieldDeclaration(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, OBJECT_ARRAY_ARRAY, "local$Variables$Class$0", createClassArrayInitializer());
        memb.add(fd);

        // Create the constructor
        final List params = new LinkedList();
        final List stmts = new LinkedList();

        // Add the outer instance parameter
        params.add(new FormalParameter(false, otype, "param$0"));

        // Add the final local variables map parameter
        params.add(new FormalParameter(false, MAP_TYPE, "param$1"));

        // Add the other parameters
        final List superArgs = new LinkedList();
        for (int i = 0; i < args.length; i++) {
            params.add(new FormalParameter(false, TreeUtilities.classToType(args[i]), "param$" + (i + 2)));
            final List l = new LinkedList();
            l.add(new Identifier("param$" + (i + 2)));
            superArgs.add(new QualifiedName(l));
        }

        // Create the explicit constructor invocation
        ConstructorInvocation ci = null;
        if (superArgs.size() > 0) {
            ci = new ConstructorInvocation(null, superArgs, true);
        }

        // Add the outer instance reference initialization statement
        List p1 = new LinkedList();
        p1.add(new Identifier("this$0"));
        List p2 = new LinkedList();
        p2.add(new Identifier("param$0"));
        stmts.add(new SimpleAssignExpression(new QualifiedName(p1), new QualifiedName(p2)));

        // Add the outer instance reference initialization statement
        p1 = new LinkedList();
        p1.add(new Identifier("local$Variables$Reference$0"));
        p2 = new LinkedList();
        p2.add(new Identifier("param$1"));
        stmts.add(new SimpleAssignExpression(new QualifiedName(p1), new QualifiedName(p2)));

        csd = new ConstructorDeclaration(Modifier.PUBLIC, cname, params, new LinkedList(), ci, stmts);
        memb.add(csd);

        // Set the inheritance clause
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
        final Class[] tmp = new Class[args.length + 2];
        tmp[0] = this.declaringClass;
        tmp[1] = Map.class;
        for (int i = 2; i < tmp.length; i++) {
            tmp[i] = args[i - 2];
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
     * Invokes a constructor
     * 
     * @param node the ClassAllocation node
     * @param args the arguments
     */
    @Override
    public Object invokeConstructor(final ClassAllocation node, Object[] args) {
        final Constructor cons = (Constructor) node.getProperty(NodeProperties.CONSTRUCTOR);

        final Object[] t = new Object[args.length + 2];
        t[0] = getHiddenArgument();
        t[1] = getConstants();
        for (int i = 2; i < t.length; i++) {
            t[i] = args[i - 2];
        }
        args = t;

        try {
            return cons.newInstance(args);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            }
            throw new ThrownException(e.getTargetException());
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }
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
            if (prefix instanceof QualifiedName && ((QualifiedName) prefix).getRepresentation().equals("this")) {
                m = InterpreterUtilities.lookupOuterMethod(c, mname, params);
                Expression exp = (Expression) this.defaultQualifier;
                Class cl = this.declaringClass;
                while (cl != m.getDeclaringClass()) {
                    final String s = getOuterThisName(cl);
                    if (s == null) {
                        // It must be a static method
                        break;
                    }
                    exp = new ObjectFieldAccess(exp, getOuterThisName(cl));
                    cl = InterpreterUtilities.getDeclaringClass(cl);
                }
                m.setAccessible(true);
                throw new MethodModificationError(exp, m);
            } else {
                throw e;
            }
        }
    }

    /**
     * Tests whether an class is an inner class of another
     * 
     * @param ic the possibly inner class
     * @param oc the possibly outer class
     */
    protected boolean isInnerclass(final Class ic, Class oc) {
        do {
            final Class[] cs = oc.getDeclaredClasses();
            for (int i = 0; i < cs.length; i++) {
                if (cs[i] == ic) {
                    return true;
                }
            }
        } while ((oc = oc.getSuperclass()) != null);
        return false;
    }

    /**
     * Finds the name of the reference to an outerclass in the given class
     */
    protected String getOuterThisName(final Class c) {
        final Field[] fs = c.getDeclaredFields();
        for (int i = 0; i < fs.length; i++) {
            if (fs[i].getName().startsWith("this$")) {
                return fs[i].getName();
            }
        }
        return null;
    }
}
