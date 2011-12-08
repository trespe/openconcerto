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

package koala.dynamicjava.interpreter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import koala.dynamicjava.interpreter.context.Context;
import koala.dynamicjava.interpreter.context.GlobalContext;
import koala.dynamicjava.interpreter.context.MethodContext;
import koala.dynamicjava.interpreter.context.StaticContext;
import koala.dynamicjava.interpreter.error.CatchedExceptionError;
import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.interpreter.throwable.ReturnException;
import koala.dynamicjava.parser.wrapper.ParseError;
import koala.dynamicjava.parser.wrapper.ParserFactory;
import koala.dynamicjava.parser.wrapper.SourceCodeParser;
import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.MethodDeclaration;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.visitor.Visitor;
import koala.dynamicjava.util.ImportationManager;
import koala.dynamicjava.util.LibraryFinder;

/**
 * This class contains method to interpret the constructs of the language.
 * 
 * @author Stephane Hillion
 * @version 1.5 - 2000/01/05
 */

public class TreeInterpreter implements Interpreter {
    /**
     * The parser
     */
    protected ParserFactory parserFactory;

    /**
     * The library finder
     */
    protected LibraryFinder libraryFinder = new LibraryFinder();

    /**
     * The class loader
     */
    protected TreeClassLoader classLoader;

    /**
     * The methods
     */
    protected static Map methods = new HashMap();
    List localMethods = new LinkedList();

    /**
     * The explicit constructor call parameters
     */
    protected static Map constructorParameters = new HashMap();
    List localConstructorParameters = new LinkedList();

    /**
     * Used to generate classes
     */
    protected static int nClass;

    protected Context nameVisitorContext;
    protected Context checkVisitorContext;
    protected Context evalVisitorContext;

    /**
     * Creates a new interpreter
     * 
     * @param pf the parser factory
     */
    public TreeInterpreter(final ParserFactory pf) {
        this.parserFactory = pf;
        this.classLoader = new TreeClassLoader(this);
        this.nameVisitorContext = new GlobalContext(this);
        this.nameVisitorContext.setAdditionalClassLoaderContainer(this.classLoader);
        this.checkVisitorContext = new GlobalContext(this);
        this.checkVisitorContext.setAdditionalClassLoaderContainer(this.classLoader);
        this.evalVisitorContext = new GlobalContext(this);
        this.evalVisitorContext.setAdditionalClassLoaderContainer(this.classLoader);
    }

    /**
     * Runs the interpreter
     * 
     * @param is the reader from which the statements are read
     * @param fname the name of the parsed stream
     * @return the result of the evaluation of the last statement
     */
    public Object interpret(final Reader r, final String fname) throws InterpreterException {
        try {
            final SourceCodeParser p = this.parserFactory.createParser(r, fname);
            final List statements = p.parseStream();
            final ListIterator it = statements.listIterator();
            Object result = null;

            while (it.hasNext()) {
                Node n = (Node) it.next();

                Visitor v = new NameVisitor(this.nameVisitorContext);
                final Object o = n.acceptVisitor(v);
                if (o != null) {
                    n = (Node) o;
                }

                v = new TypeChecker(this.checkVisitorContext);
                n.acceptVisitor(v);

                this.evalVisitorContext.defineVariables(this.checkVisitorContext.getCurrentScopeVariables());

                v = new EvaluationVisitor(this.evalVisitorContext);
                result = n.acceptVisitor(v);
            }

            return result;
        } catch (final ExecutionError e) {
            // e.printStackTrace();
            throw new InterpreterException(e);
        } catch (final ParseError e) {
            // e.printStackTrace();
            throw new InterpreterException(e);
        } catch (final Throwable d) {
            d.printStackTrace();
            throw new InterpreterException(new ParseError("Impossible d'évaluer l'expression!"));
        }
    }

    /**
     * Runs the interpreter
     * 
     * @param is the input stream from which the statements are read
     * @param fname the name of the parsed stream
     * @return the result of the evaluation of the last statement
     */
    public Object interpret(final InputStream is, final String fname) throws InterpreterException {
        return interpret(new InputStreamReader(is), fname);
    }

    /**
     * Runs the interpreter
     * 
     * @param fname the name of a file to interpret
     * @return the result of the evaluation of the last statement
     */
    public Object interpret(final String fname) throws InterpreterException, IOException {
        return interpret(new FileReader(fname), fname);
    }

    /**
     * Defines a variable in the interpreter environment
     * 
     * @param name the variable's name
     * @param value the initial value of the variable
     * @exception IllegalStateException if name is already defined
     */
    public void defineVariable(final String name, final Object value) {
        final Class c = value == null ? null : value.getClass();
        this.nameVisitorContext.define(name, c);
        this.checkVisitorContext.define(name, c);
        this.evalVisitorContext.define(name, value);
    }

    /**
     * Sets the value of a variable
     * 
     * @param name the variable's name
     * @param value the value of the variable
     * @exception IllegalStateException if the assignment is invalid
     */
    public void setVariable(final String name, final Object value) {
        final Class c = (Class) this.checkVisitorContext.get(name);
        if (InterpreterUtilities.isValidAssignment(c, value)) {
            this.evalVisitorContext.set(name, value);
        } else {
            throw new IllegalStateException(name);
        }
    }

    /**
     * Gets the value of a variable
     * 
     * @param name the variable's name
     * @exception IllegalStateException if the variable do not exist
     */
    public Object getVariable(final String name) {
        return this.evalVisitorContext.get(name);
    }

    /**
     * Gets the class of a variable
     * 
     * @param name the variable's name
     * @exception IllegalStateException if the variable do not exist
     */
    public Class getVariableClass(final String name) {
        return (Class) this.checkVisitorContext.get(name);
    }

    /**
     * Returns the defined variable names
     * 
     * @return a set of strings
     */
    public Set getVariableNames() {
        return this.evalVisitorContext.getCurrentScopeVariableNames();
    }

    /**
     * Returns the defined class names
     * 
     * @return a set of strings
     */
    public Set getClassNames() {
        return this.classLoader.getClassNames();
    }

    /**
     * Adds a class search path
     * 
     * @param path the path to add
     */
    public void addClassPath(final String path) {
        try {
            this.classLoader.addURL(new File(path).toURL());
        } catch (final MalformedURLException e) {
        }
    }

    /**
     * Adds a class search URL
     * 
     * @param url the url to add
     */
    public void addClassURL(final URL url) {
        this.classLoader.addURL(url);
    }

    /**
     * Adds a library search path
     * 
     * @param path the path to add
     */
    public void addLibraryPath(final String path) {
        this.libraryFinder.addPath(path);
    }

    /**
     * Adds a library file suffix
     * 
     * @param s the suffix to add
     */
    public void addLibrarySuffix(final String s) {
        this.libraryFinder.addSuffix(s);
    }

    /**
     * Loads an interpreted class
     * 
     * @param s the fully qualified name of the class to load
     * @exception ClassNotFoundException if the class cannot be find
     */
    public Class loadClass(final String name) throws ClassNotFoundException {
        return new TreeCompiler(this).compile(name);
    }

    /**
     * Converts an array of bytes into an instance of the class Class
     * 
     * @exception ClassFormatError if the class cannot be defined
     */
    public Class defineClass(final String name, final byte[] code) {
        return this.classLoader.defineClass(name, code);
    }

    /**
     * Gets the class loader
     */
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Gets the library finder
     */
    public LibraryFinder getLibraryFinder() {
        return this.libraryFinder;
    }

    /**
     * Gets the parser factory
     */
    public ParserFactory getParserFactory() {
        return this.parserFactory;
    }

    /**
     * Returns the class of the execution exception
     */
    public Class getExceptionClass() {
        return CatchedExceptionError.class;
    }

    /**
     * Registers a method.
     * 
     * @param sig the method's signature
     * @param md the method declaration
     * @param im the importation manager
     */
    public void registerMethod(final String sig, final MethodDeclaration md, final ImportationManager im) {
        this.localMethods.add(sig);
        methods.put(sig, new MethodDescriptor(md, im));
    }

    /**
     * Interprets the body of a method
     * 
     * @param key the key used to find the body of a method
     * @param obj the object (this)
     * @param params the arguments
     */
    public static Object invokeMethod(final String key, final Object obj, final Object[] params) {
        final MethodDescriptor md = (MethodDescriptor) methods.get(key);
        Class c = null;
        try {
            c = Class.forName(key.substring(0, key.lastIndexOf('#')), true, md.interpreter.getClassLoader());
        } catch (final ClassNotFoundException e) {
            // Should never append
            e.printStackTrace();
        }

        return md.interpreter.interpretMethod(c, md, obj, params);
    }

    /**
     * Interprets the body of a method
     * 
     * @param c the declaring class of the method
     * @param md the method descriptor
     * @param obj the object (this)
     * @param params the arguments
     */
    protected Object interpretMethod(final Class c, final MethodDescriptor md, final Object obj, final Object[] params) {
        final MethodDeclaration meth = md.method;
        final List mparams = meth.getParameters();
        final List stmts = meth.getBody().getStatements();
        final String name = meth.getName();

        Context context = null;

        if (Modifier.isStatic(md.method.getAccessFlags())) {
            if (md.variables == null) {
                md.importationManager.setClassLoader(this.classLoader);

                // pass 1: names resolution
                Context ctx = new StaticContext(this, c, md.importationManager);
                ctx.setAdditionalClassLoaderContainer(this.classLoader);
                Visitor v = new NameVisitor(ctx);

                ListIterator it = mparams.listIterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(v);
                }

                it = stmts.listIterator();
                while (it.hasNext()) {
                    final Object o = ((Node) it.next()).acceptVisitor(v);
                    if (o != null) {
                        it.set(o);
                    }
                }

                // pass 2: type checking
                ctx = new StaticContext(this, c, md.importationManager);
                ctx.setAdditionalClassLoaderContainer(this.classLoader);
                v = new TypeChecker(ctx);

                it = mparams.listIterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(v);
                }

                it = stmts.listIterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(v);
                }

                md.variables = ctx.getCurrentScopeVariables();

                // Test of the additional context existence
                if (!name.equals("<clinit>") && !name.equals("<init>")) {
                    try {
                        md.contextField = c.getField("local$Variables$Reference$0");
                    } catch (final NoSuchFieldException e) {
                    }
                }
            }

            // pass 3: evaluation
            context = new StaticContext(this, c, md.variables);
        } else {
            if (md.variables == null) {
                md.importationManager.setClassLoader(this.classLoader);

                // pass 1: names resolution
                Context ctx = new MethodContext(this, c, c, md.importationManager);
                ctx.setAdditionalClassLoaderContainer(this.classLoader);
                Visitor v = new NameVisitor(ctx);

                Context ctx2 = new MethodContext(this, c, c, md.importationManager);
                ctx2.setAdditionalClassLoaderContainer(this.classLoader);
                Visitor v2 = new NameVisitor(ctx2);

                // Initializes the context with the outerclass variables
                Object[][] cc = null;
                try {
                    final Field f = c.getField("local$Variables$Class$0");
                    cc = (Object[][]) f.get(obj);
                    for (int i = 0; i < cc.length; i++) {
                        final Object[] cell = cc[i];
                        if (!((String) cell[0]).equals("this")) {
                            ctx.defineConstant((String) cell[0], cell[1]);
                        }
                    }
                } catch (final Exception e) {
                }

                // Visit the parameters and the body of the method
                ListIterator it = mparams.listIterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(v);
                }

                it = stmts.listIterator();
                while (it.hasNext()) {
                    final Node n = (Node) it.next();
                    Object o = null;
                    if (n.hasProperty(NodeProperties.INSTANCE_INITIALIZER)) {
                        o = n.acceptVisitor(v2);
                    } else {
                        o = n.acceptVisitor(v);
                    }
                    if (o != null) {
                        it.set(o);
                    }
                }

                // pass 2: type checking
                ctx = new MethodContext(this, c, c, md.importationManager);
                ctx.setAdditionalClassLoaderContainer(this.classLoader);
                v = new TypeChecker(ctx);

                ctx2 = new MethodContext(this, c, c, md.importationManager);
                ctx2.setAdditionalClassLoaderContainer(this.classLoader);
                v2 = new TypeChecker(ctx2);

                // Initializes the context with outerclass variables
                if (cc != null) {
                    for (int i = 0; i < cc.length; i++) {
                        final Object[] cell = cc[i];
                        if (!((String) cell[0]).equals("this")) {
                            ctx.defineConstant((String) cell[0], cell[1]);
                        }
                    }
                }

                // Visit the parameters and the body of the method
                it = mparams.listIterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(v);
                }

                it = stmts.listIterator();
                while (it.hasNext()) {
                    final Node n = (Node) it.next();
                    if (n.hasProperty(NodeProperties.INSTANCE_INITIALIZER)) {
                        n.acceptVisitor(v2);
                    } else {
                        n.acceptVisitor(v);
                    }
                }

                md.variables = ctx.getCurrentScopeVariables();

                // Test of the additional context existence
                if (!name.equals("<clinit>") && !name.equals("<init>")) {
                    try {
                        md.contextField = c.getField("local$Variables$Reference$0");
                    } catch (final NoSuchFieldException e) {
                    }
                }
            }

            // pass 3: evaluation
            context = new MethodContext(this, c, obj, md.variables);
        }

        context.setAdditionalClassLoaderContainer(this.classLoader);

        // Set the arguments values
        Iterator it = mparams.iterator();
        int i = 0;
        while (it.hasNext()) {
            context.set(((FormalParameter) it.next()).getName(), params[i++]);
        }

        // Set the final local variables values
        if (md.contextField != null) {
            Map vars = null;
            try {
                vars = (Map) md.contextField.get(obj);
            } catch (final IllegalAccessException e) {
            }
            if (vars != null) {
                it = vars.keySet().iterator();
                while (it.hasNext()) {
                    final String s = (String) it.next();
                    if (!s.equals("this")) {
                        context.setConstant(s, vars.get(s));
                    }
                }
            }
        }

        final Visitor v = new EvaluationVisitor(context);
        it = stmts.iterator();

        try {
            while (it.hasNext()) {
                ((Node) it.next()).acceptVisitor(v);
            }
        } catch (final ReturnException e) {
            return e.getValue();
        }
        return null;
    }

    /**
     * Registers a constructor arguments
     */
    public void registerConstructorArguments(final String sig, final List params, final List exprs, final ImportationManager im) {
        this.localConstructorParameters.add(sig);
        constructorParameters.put(sig, new ConstructorParametersDescriptor(params, exprs, im));
    }

    /**
     * This method is used to implement constructor invocation.
     * 
     * @param key the key used to find the informations about the constructor
     * @param args the arguments passed to this constructor
     * @return the arguments to give to the 'super' or 'this' constructor followed by the new values
     *         of the constructor arguments
     */
    public static Object[] interpretArguments(final String key, final Object[] args) {
        final ConstructorParametersDescriptor cpd = (ConstructorParametersDescriptor) constructorParameters.get(key);
        Class c = null;
        try {
            c = Class.forName(key.substring(0, key.lastIndexOf('#')), true, cpd.interpreter.getClassLoader());
        } catch (final ClassNotFoundException e) {
            // Should never append
            e.printStackTrace();
        }

        return cpd.interpreter.interpretArguments(c, cpd, args);
    }

    /**
     * This method is used to implement constructor invocation.
     * 
     * @param c the declaring class of the constructor
     * @param cpd the parameter descriptor
     * @param args the arguments passed to this constructor
     * @return the arguments to give to the 'super' or 'this' constructor followed by the new values
     *         of the constructor arguments
     */
    protected Object[] interpretArguments(final Class c, final ConstructorParametersDescriptor cpd, final Object[] args) {
        if (cpd.variables == null) {
            cpd.importationManager.setClassLoader(this.classLoader);

            final Context ctx = new StaticContext(this, c, cpd.importationManager);
            ctx.setAdditionalClassLoaderContainer(this.classLoader);
            final Visitor nv = new NameVisitor(ctx);
            final Visitor tc = new TypeChecker(ctx);

            // Check the parameters
            if (cpd.parameters != null) {
                final ListIterator it = cpd.parameters.listIterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(tc);
                }
            }

            if (cpd.arguments != null) {
                ListIterator it = cpd.arguments.listIterator();
                while (it.hasNext()) {
                    final Node root = (Node) it.next();
                    final Object res = root.acceptVisitor(nv);
                    if (res != null) {
                        it.set(res);
                    }
                }

                it = cpd.arguments.listIterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(tc);
                }
            }
            cpd.variables = ctx.getCurrentScopeVariables();
        }

        final Context ctx = new StaticContext(this, c, cpd.variables);
        ctx.setAdditionalClassLoaderContainer(this.classLoader);

        // Set the arguments values
        if (cpd.parameters != null) {
            final Iterator it = cpd.parameters.iterator();
            int i = 0;
            while (it.hasNext()) {
                ctx.set(((FormalParameter) it.next()).getName(), args[i++]);
            }
        }

        Object[] result = new Object[0];

        if (cpd.arguments != null) {
            final Visitor v = new EvaluationVisitor(ctx);
            final ListIterator it = cpd.arguments.listIterator();
            result = new Object[cpd.arguments.size()];
            int i = 0;
            while (it.hasNext()) {
                result[i++] = ((Node) it.next()).acceptVisitor(v);
            }
        }

        return result;
    }

    /**
     * Called before the destruction of the interpreter
     */
    @Override
    protected void finalize() throws Throwable {
        Iterator it = this.localMethods.iterator();
        while (it.hasNext()) {
            methods.remove(it.next());
        }
        it = this.localConstructorParameters.iterator();
        while (it.hasNext()) {
            constructorParameters.remove(it.next());
        }
    }

    /**
     * Used to store the informations about dynamically created methods
     */
    protected class MethodDescriptor {
        Set variables;
        MethodDeclaration method;
        ImportationManager importationManager;
        TreeInterpreter interpreter;
        Field contextField;

        /**
         * Creates a new descriptor
         */
        MethodDescriptor(final MethodDeclaration md, final ImportationManager im) {
            this.method = md;
            this.importationManager = im;
            this.interpreter = TreeInterpreter.this;
        }
    }

    /**
     * Used to store the informations about explicit constructors invocation
     */
    protected class ConstructorParametersDescriptor {
        Set variables;
        List parameters;
        List arguments;
        ImportationManager importationManager;
        TreeInterpreter interpreter;

        /**
         * Creates a new descriptor
         */
        ConstructorParametersDescriptor(final List params, final List args, final ImportationManager im) {
            this.parameters = params;
            this.arguments = args;
            this.importationManager = im;
            this.interpreter = TreeInterpreter.this;
        }
    }
}
