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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import koala.dynamicjava.classinfo.JavaClassInfo;
import koala.dynamicjava.classinfo.TreeClassInfo;
import koala.dynamicjava.interpreter.ClassLoaderContainer;
import koala.dynamicjava.interpreter.Interpreter;
import koala.dynamicjava.interpreter.NodeProperties;
import koala.dynamicjava.interpreter.TreeClassLoader;
import koala.dynamicjava.interpreter.TreeCompiler;
import koala.dynamicjava.interpreter.error.CatchedExceptionError;
import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.interpreter.modifier.FinalVariableModifier;
import koala.dynamicjava.interpreter.modifier.InvalidModifier;
import koala.dynamicjava.interpreter.modifier.LeftHandSideModifier;
import koala.dynamicjava.interpreter.modifier.ObjectFieldModifier;
import koala.dynamicjava.interpreter.modifier.StaticFieldModifier;
import koala.dynamicjava.interpreter.modifier.VariableModifier;
import koala.dynamicjava.interpreter.throwable.ThrownException;
import koala.dynamicjava.parser.wrapper.ParserFactory;
import koala.dynamicjava.parser.wrapper.SourceCodeParser;
import koala.dynamicjava.tree.ArrayInitializer;
import koala.dynamicjava.tree.ArrayType;
import koala.dynamicjava.tree.ClassAllocation;
import koala.dynamicjava.tree.ClassDeclaration;
import koala.dynamicjava.tree.ConstructorDeclaration;
import koala.dynamicjava.tree.ConstructorInvocation;
import koala.dynamicjava.tree.Expression;
import koala.dynamicjava.tree.FieldDeclaration;
import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.Identifier;
import koala.dynamicjava.tree.IdentifierToken;
import koala.dynamicjava.tree.ImportDeclaration;
import koala.dynamicjava.tree.InterfaceDeclaration;
import koala.dynamicjava.tree.MethodDeclaration;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.ObjectFieldAccess;
import koala.dynamicjava.tree.PackageDeclaration;
import koala.dynamicjava.tree.QualifiedName;
import koala.dynamicjava.tree.ReferenceType;
import koala.dynamicjava.tree.SimpleAllocation;
import koala.dynamicjava.tree.SimpleAssignExpression;
import koala.dynamicjava.tree.StaticFieldAccess;
import koala.dynamicjava.tree.StringLiteral;
import koala.dynamicjava.tree.SuperFieldAccess;
import koala.dynamicjava.tree.TreeUtilities;
import koala.dynamicjava.tree.Type;
import koala.dynamicjava.tree.TypeDeclaration;
import koala.dynamicjava.tree.TypeExpression;
import koala.dynamicjava.tree.visitor.Visitor;
import koala.dynamicjava.tree.visitor.VisitorObject;
import koala.dynamicjava.util.AmbiguousFieldException;
import koala.dynamicjava.util.BufferedImportationManager;
import koala.dynamicjava.util.ImportationManager;
import koala.dynamicjava.util.LibraryFinder;
import koala.dynamicjava.util.ReflectionUtilities;

/**
 * A global context.
 * 
 * @author Stephane Hillion
 * @version 1.3 - 1999/11/28
 */

public class GlobalContext extends VariableContext implements Context {
    // Constant objects
    protected final static ReferenceType CLASS_TYPE = new ReferenceType("java.lang.Class");
    protected final static ReferenceType MAP_TYPE = new ReferenceType("java.util.Map");
    protected final static ReferenceType OBJECT_TYPE = new ReferenceType("java.lang.Object");
    protected final static ArrayType OBJECT_ARRAY_ARRAY = new ArrayType(OBJECT_TYPE, 2);
    protected final static TypeExpression OBJECT_CLASS = new TypeExpression(OBJECT_TYPE);

    protected final static String LOCALS_NAME = "local$Variables$Reference$0";
    protected final static FieldDeclaration LOCALS = new FieldDeclaration(Modifier.PUBLIC, MAP_TYPE, LOCALS_NAME, null);

    /**
     * To generate an unique name for the generated classes
     */
    protected static int classCount = 0;

    /**
     * The importation manager
     */
    protected ImportationManager importationManager;

    /**
     * The interpreter
     */
    protected Interpreter interpreter;

    /**
     * The class loader
     */
    protected ClassLoader classLoader;

    /**
     * The class loader container
     */
    protected ClassLoaderContainer clc;

    /**
     * The functions
     */
    protected List functions = new LinkedList();

    /**
     * Creates a new context
     * 
     * @param i the interpreter
     */
    public GlobalContext(final Interpreter i) {
        this.importationManager = new BufferedImportationManager(i.getClassLoader());
        this.interpreter = i;
    }

    /**
     * Creates a new context
     * 
     * @param i the interpreter
     * @param cl the classloader to use
     * @param cl2 the additional classloader
     */
    public GlobalContext(final Interpreter i, final ClassLoader cl) {
        this.importationManager = new BufferedImportationManager(cl);
        this.interpreter = i;
        this.classLoader = cl;
    }

    /**
     * Creates a new context initialized with the given entries defined in the initial scope.
     * 
     * @param i the interpreter
     * @param entries a set of string
     */
    public GlobalContext(final Interpreter i, final Set entries) {
        super(entries);
        this.interpreter = i;
    }

    /**
     * Sets the additional class loader container
     */
    public void setAdditionalClassLoaderContainer(final ClassLoaderContainer clc) {
        this.clc = clc;
    }

    /**
     * Gets the additional class loader
     */
    protected ClassLoader getAdditionalClassLoader() {
        if (this.clc != null) {
            return this.clc.getClassLoader();
        }
        return null;
    }

    /**
     * Sets the defined functions
     */
    public void setFunctions(final List l) {
        this.functions = l;
    }

    /**
     * Returns the defined functions
     */
    public List getFunctions() {
        return this.functions;
    }

    /**
     * Returns the current interpreter
     */
    public Interpreter getInterpreter() {
        return this.interpreter;
    }

    /**
     * Returns the importation manager
     */
    public ImportationManager getImportationManager() {
        return this.importationManager;
    }

    /**
     * Sets the importation manager
     */
    public void setImportationManager(final ImportationManager im) {
        this.importationManager = im;
    }

    /**
     * Whether a simple identifier represents an existing variable or field or type in this context.
     * 
     * @param name the identifier
     */
    public boolean exists(final String name) {
        return isDefined(name) || classExists(name);
    }

    /**
     * Whether a simple identifier is a class
     * 
     * @param name the identifier
     */
    public boolean classExists(final String name) {
        boolean result = false;
        this.importationManager.setClassLoader(new PseudoClassLoader());
        try {
            lookupClass(name);
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
     * Defines a MethodDeclaration as a function
     * 
     * @param node the function declaration
     */
    public void defineFunction(final MethodDeclaration node) {
        this.functions.add(0, node);
    }

    /**
     * Defines a class from its syntax tree
     * 
     * @param node the class declaration
     */
    public void defineClass(final TypeDeclaration node) {
        new TreeCompiler(this.interpreter).compileTree(this, node);
    }

    /**
     * Tests whether a variable is defined in this context
     * 
     * @param name the name of the entry
     * @return false if the variable is undefined
     */
    public boolean isDefined(final String name) {
        return isDefinedVariable(name);
    }

    /**
     * Sets the current package
     * 
     * @param pkg the package name
     */
    public void setCurrentPackage(final String pkg) {
        this.importationManager.setCurrentPackage(pkg);
    }

    /**
     * Returns the current package
     */
    public String getCurrentPackage() {
        return this.importationManager.getCurrentPackage();
    }

    /**
     * Declares a new import-on-demand clause
     * 
     * @param pkg the package name
     */
    public void declarePackageImport(final String pkg) {
        this.importationManager.declarePackageImport(pkg);
    }

    /**
     * Declares a new single-type-import clause
     * 
     * @param cname the fully qualified class name
     * @exception ClassNotFoundException if the class cannot be found
     */
    public void declareClassImport(final String cname) throws ClassNotFoundException {
        this.importationManager.setClassLoader(new PseudoClassLoader());
        try {
            this.importationManager.declareClassImport(cname);
        } catch (final PseudoError e) {
        } finally {
            if (this.classLoader == null) {
                this.importationManager.setClassLoader(this.interpreter.getClassLoader());
            } else {
                this.importationManager.setClassLoader(this.classLoader);
            }
        }
    }

    /**
     * Returns the default qualifier for this context
     * 
     * @param node the current node
     */
    public Node getDefaultQualifier(final Node node) {
        return getDefaultQualifier(node, "");
    }

    /**
     * Returns the default qualifier for this context
     * 
     * @param node the current node
     * @param tname the qualifier of 'this'
     */
    public Node getDefaultQualifier(final Node node, final String tname) {
        return null;
    }

    /**
     * Returns the modifier that match the given node
     * 
     * @param node a tree node
     */
    public LeftHandSideModifier getModifier(final QualifiedName node) {
        if (isFinal(node.getRepresentation())) {
            return new FinalVariableModifier(node, NodeProperties.getType(node));
        } else {
            return new VariableModifier(node, NodeProperties.getType(node));
        }
    }

    /**
     * Returns the modifier that match the given node
     * 
     * @param node a tree node
     */
    public LeftHandSideModifier getModifier(final ObjectFieldAccess node) {
        final Field f = (Field) node.getProperty(NodeProperties.FIELD);
        if (f.isAccessible()) {
            return new ObjectFieldModifier(f, node);
        } else {
            return new InvalidModifier(node);
        }
    }

    /**
     * Returns the modifier that match the given node
     * 
     * @param node a tree node
     */
    public LeftHandSideModifier getModifier(final StaticFieldAccess node) {
        final Field f = (Field) node.getProperty(NodeProperties.FIELD);
        if (f.isAccessible()) {
            return new StaticFieldModifier(f, node);
        } else {
            return new InvalidModifier(node);
        }
    }

    /**
     * Returns the modifier that match the given node
     * 
     * @param node a tree node
     */
    public LeftHandSideModifier getModifier(final SuperFieldAccess node) {
        throw new IllegalStateException("internal.error");
    }

    /**
     * Returns the default argument to pass to methods in this context
     */
    public Object getHiddenArgument() {
        return null;
    }

    /**
     * Creates the tree that is associated with the given name
     * 
     * @param node the current node
     * @param name the variable name
     * @exception IllegalStateException if the variable is not defined
     */
    public Expression createName(final Node node, final IdentifierToken name) {
        if (!isDefined(name.image())) {
            throw new IllegalStateException();
        }

        final List l = new LinkedList();
        l.add(name);
        return new QualifiedName(l);
    }

    /**
     * Looks for a class
     * 
     * @param cname the class name
     * @exception ClassNotFoundException if the class cannot be found
     */
    public Class lookupClass(final String cname) throws ClassNotFoundException {
        return this.importationManager.lookupClass(cname, null);
    }

    /**
     * Looks for a class (context-free lookup)
     * 
     * @param cname the class name
     * @param ccname the fully qualified name of the context class
     * @exception ClassNotFoundException if the class cannot be found
     */
    public Class lookupClass(final String cname, final String ccname) throws ClassNotFoundException {
        return this.importationManager.lookupClass(cname, ccname);
    }

    /**
     * Sets the properties of a SimpleAllocation node
     * 
     * @param node the allocation node
     * @param c the class of the constructor
     * @param cargs the classes of the arguments of the constructor
     */
    public Class setProperties(final SimpleAllocation node, final Class c, final Class[] cargs) {
        Constructor cons = null;
        try {
            cons = lookupConstructor(c, cargs);
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }

        // Set the properties of this node
        node.setProperty(NodeProperties.TYPE, c);
        node.setProperty(NodeProperties.CONSTRUCTOR, cons);
        return c;
    }

    /**
     * Sets the properties of a ClassAllocation node
     * 
     * @param node the allocation node
     * @param c the class of the constructor
     * @param args the classes of the arguments of the constructor
     * @param memb the class members
     */
    public Class setProperties(final ClassAllocation node, final Class c, Class[] args, final List memb) {
        final String cname = "TopLevel" + "$" + classCount++;
        FieldDeclaration fd;
        ConstructorDeclaration csd;

        // Create the reference to the declaring class
        fd = new FieldDeclaration(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL, CLASS_TYPE, "declaring$Class$Reference$0", OBJECT_CLASS);
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

        type.setProperty(TreeClassInfo.ANONYMOUS_DECLARING_CLASS, new JavaClassInfo(Object.class));

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
     * Creates an initializer for the variable class array used to implement inner classes
     */
    protected ArrayInitializer createClassArrayInitializer() {
        final List cells = new LinkedList();
        ArrayInitializer cell;

        Type tp = new ReferenceType(Object.class.getName());
        final Map m = getConstants();
        final Iterator it = m.keySet().iterator();
        while (it.hasNext()) {
            final String s = (String) it.next();
            final List pair = new LinkedList();
            pair.add(new StringLiteral('\"' + s + '\"'));
            final Class c = (Class) m.get(s);
            pair.add(new TypeExpression(TreeUtilities.classToType(c)));

            cell = new ArrayInitializer(pair);
            cell.setElementType(tp);
            cells.add(cell);
        }
        tp = new ArrayType(tp, 1);
        final ArrayInitializer ai = new ArrayInitializer(cells);
        ai.setElementType(tp);
        return ai;
    }

    /**
     * Looks for a constructor
     * 
     * @param c the class of the constructor
     * @param params the parameter types
     * @exception NoSuchMethodException if the constructor cannot be found
     */
    public Constructor lookupConstructor(final Class c, final Class[] params) throws NoSuchMethodException {
        final Constructor cons = ReflectionUtilities.lookupConstructor(c, params);
        setAccessFlag(cons);
        return cons;
    }

    /**
     * Invokes a constructor
     * 
     * @param node the SimpleAllocation node
     * @param args the arguments
     */
    public Object invokeConstructor(final SimpleAllocation node, final Object[] args) {
        final Constructor cons = (Constructor) node.getProperty(NodeProperties.CONSTRUCTOR);

        try {
            return cons.newInstance(args);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            } else if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            }
            throw new ThrownException(e.getTargetException());
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }
    }

    /**
     * Invokes a constructor
     * 
     * @param node the ClassAllocation node
     * @param args the arguments
     */
    public Object invokeConstructor(final ClassAllocation node, Object[] args) {
        final Constructor cons = (Constructor) node.getProperty(NodeProperties.CONSTRUCTOR);

        final Object[] t = new Object[args.length + 1];
        t[0] = getConstants();
        for (int i = 1; i < t.length; i++) {
            t[i] = args[i - 1];
        }
        args = t;

        try {
            return cons.newInstance(args);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            } else if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
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
    public Method lookupMethod(final Node prefix, final String mname, final Class[] params) throws NoSuchMethodException {
        final Class c = NodeProperties.getType(prefix);
        final Method m = ReflectionUtilities.lookupMethod(c, mname, params);
        setAccessFlag(m);
        if (m.getName().equals("clone")) {
            m.setAccessible(true);
        }
        return m;
    }

    /**
     * Looks for a function
     * 
     * @param mname the function name
     * @param params the parameter types
     * @exception NoSuchFunctionException if the function cannot be found
     */
    public MethodDeclaration lookupFunction(final String mname, final Class[] params) throws NoSuchFunctionException {
        Iterator it = this.functions.iterator();
        final List f = new LinkedList();

        while (it.hasNext()) {
            final MethodDeclaration md = (MethodDeclaration) it.next();
            if (md.getName().equals(mname)) {
                f.add(md);
            }
        }

        it = f.iterator();
        while (it.hasNext()) {
            final MethodDeclaration md = (MethodDeclaration) it.next();
            final List l = md.getParameters();

            if (l.size() != params.length) {
                continue;
            }

            final Class[] p = new Class[l.size()];
            final Iterator it2 = l.iterator();
            int i = 0;
            while (it2.hasNext()) {
                p[i++] = NodeProperties.getType((Node) it2.next());
            }

            if (ReflectionUtilities.hasCompatibleSignatures(p, params)) {
                return md;
            }
        }

        throw new NoSuchFunctionException(mname);
    }

    /**
     * Looks for a super method
     * 
     * @param node the current node
     * @param mname the method name
     * @param params the parameter types
     * @exception NoSuchMethodException if the method cannot be find
     */
    public Method lookupSuperMethod(final Node node, final String mname, final Class[] params) throws NoSuchMethodException {
        throw new ExecutionError("super.method", node);
    }

    /**
     * Looks for a field
     * 
     * @param fc the field class
     * @param fn the field name
     * @exception NoSuchFieldException if the field cannot be find
     * @exception AmbiguousFieldException if the field is ambiguous
     */
    public Field getField(final Class fc, final String fn) throws NoSuchFieldException, AmbiguousFieldException {
        final Field f = ReflectionUtilities.getField(fc, fn);
        setAccessFlag(f);
        return f;
    }

    /**
     * Looks for a field in the super class
     * 
     * @param node the current node
     * @param fn the field name
     * @exception NoSuchFieldException if the field cannot be find
     * @exception AmbiguousFieldException if the field is ambiguous
     */
    public Field getSuperField(final Node node, final String fn) throws NoSuchFieldException, AmbiguousFieldException {
        throw new ExecutionError("super.field", node);
    }

    /**
     * To test the existance of a class without loading it
     */
    protected class PseudoClassLoader extends ClassLoader {
        /**
         * Finds the specified class.
         * 
         * @param name the name of the class
         * @return the resulting <code>Class</code> object
         * @exception ClassNotFoundException if the class could not be find
         */
        @Override
        protected Class findClass(final String name) throws ClassNotFoundException {
            try {
                if (getAdditionalClassLoader() != null) {
                    return Class.forName(name, true, getAdditionalClassLoader());
                }
            } catch (final ClassNotFoundException e) {
            }

            final ClassLoader cl = GlobalContext.this.classLoader == null ? GlobalContext.this.interpreter.getClassLoader() : GlobalContext.this.classLoader;
            // Was this class previously defined ?
            if (cl instanceof TreeClassLoader && ((TreeClassLoader) cl).hasDefined(name)) {
                throw new PseudoError();
            }

            // Is there a tree associated with this name ?
            final TreeClassLoader cld = (TreeClassLoader) GlobalContext.this.interpreter.getClassLoader();
            TypeDeclaration td = cld.getTree(name);
            if (td != null) {
                final ImportationManager im = (ImportationManager) td.getProperty(NodeProperties.IMPORTATION_MANAGER);

                final CompilationUnitVisitor v = new CompilationUnitVisitor(name, im);
                if (td.acceptVisitor(v).equals(Boolean.TRUE)) {
                    throw new PseudoError();
                }
            }

            // Is the class tree already loaded ?
            final LibraryFinder lf = GlobalContext.this.interpreter.getLibraryFinder();
            try {
                final String cun = lf.findCompilationUnitName(name);
                td = cld.getTree(cun);
                if (td != null) {
                    final ImportationManager im = (ImportationManager) td.getProperty(NodeProperties.IMPORTATION_MANAGER);

                    final CompilationUnitVisitor v = new CompilationUnitVisitor(name, im);
                    if (td.acceptVisitor(v).equals(Boolean.TRUE)) {
                        throw new PseudoError();
                    }
                }
            } catch (final ClassNotFoundException e) {
            }

            // Load the tree
            try {
                final File f = lf.findCompilationUnit(name);
                final FileInputStream fis = new FileInputStream(f);

                final ParserFactory pf = GlobalContext.this.interpreter.getParserFactory();
                final SourceCodeParser p = pf.createParser(fis, f.getCanonicalPath());
                final List stmts = p.parseCompilationUnit();

                final Iterator it = stmts.iterator();
                final CompilationUnitVisitor v = new CompilationUnitVisitor(name);
                boolean classFound = false;
                while (it.hasNext()) {
                    if (Boolean.TRUE.equals(((Node) it.next()).acceptVisitor(v))) {
                        classFound = true;
                    }
                }

                if (classFound) {
                    throw new PseudoError();
                }
            } catch (final IOException e) {
            }
            throw new ClassNotFoundException(name);
        }
    }

    /**
     * To test the existance of a class without loading it
     */
    protected class PseudoError extends Error {
    }

    /**
     * Sets the access flag of a member
     */
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
                if (samePkg) {
                    ((AccessibleObject) m).setAccessible(true);
                }
            } else if (!Modifier.isPrivate(mods)) {
                if (samePkg) {
                    ((AccessibleObject) m).setAccessible(true);
                }
            }
        }
    }

    /**
     * Gets the package name for the given class
     */
    protected String getPackageName(final Class c) {
        final String s = c.getName();
        final int i = s.lastIndexOf('.');
        return i == -1 ? "" : s.substring(0, i);
    }

    /**
     * To find a class in a compilation unit
     */
    private class CompilationUnitVisitor extends VisitorObject {
        /**
         * The class to find
         */
        private final String className;

        /**
         * The importation manager
         */
        private final ImportationManager importationManager;

        /**
         * The current class loader
         */
        private final TreeClassLoader classLoader;

        /**
         * Creates a new visitor
         */
        public CompilationUnitVisitor(final String cname) {
            this.className = cname;
            this.importationManager = new BufferedImportationManager(new PseudoClassLoader());
            this.classLoader = (TreeClassLoader) GlobalContext.this.interpreter.getClassLoader();
        }

        /**
         * Creates a new visitor
         */
        public CompilationUnitVisitor(final String cname, final ImportationManager im) {
            this.className = cname;
            this.importationManager = im;
            this.importationManager.setClassLoader(new PseudoClassLoader());
            this.classLoader = (TreeClassLoader) GlobalContext.this.interpreter.getClassLoader();
        }

        /**
         * Visits a PackageDeclaration
         * 
         * @param node the node to visit
         * @return null
         */
        @Override
        public Object visit(final PackageDeclaration node) {
            this.importationManager.setCurrentPackage(node.getName());
            return null;
        }

        /**
         * Visits an ImportDeclaration
         * 
         * @param node the node to visit
         */
        @Override
        public Object visit(final ImportDeclaration node) {
            // Declare the package or class importation
            if (node.isPackage()) {
                this.importationManager.declarePackageImport(node.getName());
            } else {
                try {
                    this.importationManager.declareClassImport(node.getName());
                } catch (final ClassNotFoundException e) {
                    throw new CatchedExceptionError(e, node);
                } catch (final PseudoError e) {
                }
            }
            return null;
        }

        /**
         * Visits a ClassDeclaration
         * 
         * @param node the node to visit
         */
        @Override
        public Object visit(final ClassDeclaration node) {
            return visitType(node);
        }

        /**
         * Visits an InterfaceDeclaration
         * 
         * @param node the node to visit
         */
        @Override
        public Object visit(final InterfaceDeclaration node) {
            return visitType(node);
        }

        /**
         * visits a TypeDeclaration
         */
        private Object visitType(final TypeDeclaration node) {
            String cname = this.importationManager.getCurrentPackage();
            cname = (cname.equals("") ? "" : cname + ".") + node.getName();
            this.classLoader.addTree(cname, node);
            node.setProperty(NodeProperties.IMPORTATION_MANAGER, this.importationManager);
            if (this.className.equals(cname)) {
                return Boolean.TRUE;
            } else {
                final Visitor v = new MembersVisitor(cname);
                final Iterator it = node.getMembers().iterator();
                while (it.hasNext()) {
                    final Boolean b = (Boolean) ((Node) it.next()).acceptVisitor(v);
                    if (Boolean.TRUE.equals(b)) {
                        return b;
                    }
                }
                return Boolean.FALSE;
            }
        }

        /**
         * To find a class in a compilation unit
         */
        private class MembersVisitor extends VisitorObject {
            /**
             * The outer class
             */
            private final String outerName;

            /**
             * Creates a new visitor
             */
            public MembersVisitor(final String cname) {
                this.outerName = cname;
            }

            /**
             * Visits a ClassDeclaration
             * 
             * @param node the node to visit
             */
            @Override
            public Object visit(final ClassDeclaration node) {
                return visitType(node);
            }

            /**
             * Visits an InterfaceDeclaration
             * 
             * @param node the node to visit
             */
            @Override
            public Object visit(final InterfaceDeclaration node) {
                return visitType(node);
            }

            /**
             * visits a TypeDeclaration
             */
            private Object visitType(final TypeDeclaration node) {
                if (CompilationUnitVisitor.this.className.equals(this.outerName + "$" + node.getName())) {
                    return Boolean.TRUE;
                } else {
                    final Visitor v = new MembersVisitor(this.outerName + "$" + node.getName());
                    final Iterator it = node.getMembers().iterator();
                    while (it.hasNext()) {
                        final Boolean b = (Boolean) ((Node) it.next()).acceptVisitor(v);
                        if (Boolean.TRUE.equals(b)) {
                            return b;
                        }
                    }
                    return Boolean.FALSE;
                }
            }
        }
    }
}
