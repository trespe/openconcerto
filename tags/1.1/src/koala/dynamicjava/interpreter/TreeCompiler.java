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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import koala.dynamicjava.classinfo.ClassFinder;
import koala.dynamicjava.classinfo.ClassInfo;
import koala.dynamicjava.classinfo.TreeClassInfo;
import koala.dynamicjava.interpreter.context.Context;
import koala.dynamicjava.interpreter.context.GlobalContext;
import koala.dynamicjava.interpreter.error.CatchedExceptionError;
import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.parser.wrapper.ParserFactory;
import koala.dynamicjava.parser.wrapper.SourceCodeParser;
import koala.dynamicjava.tree.ClassDeclaration;
import koala.dynamicjava.tree.ImportDeclaration;
import koala.dynamicjava.tree.InterfaceDeclaration;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.PackageDeclaration;
import koala.dynamicjava.tree.TypeDeclaration;
import koala.dynamicjava.tree.visitor.Visitor;
import koala.dynamicjava.tree.visitor.VisitorObject;
import koala.dynamicjava.util.ImportationManager;
import koala.dynamicjava.util.LibraryFinder;

/**
 * This class contains methods to manage the creation of classes.
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/28
 */

public class TreeCompiler {
    /**
     * The interpreter
     */
    protected Interpreter interpreter;

    /**
     * The classloader
     */
    protected TreeClassLoader classLoader;

    /**
     * The class info loader
     */
    protected ClassInfoLoader classInfoLoader;

    /**
     * The class pool
     */
    protected ClassPool classPool = new ClassPool();

    /**
     * Creates a new compiler
     * 
     * @param i the current interpreter
     */
    public TreeCompiler(final Interpreter i) {
        this.interpreter = i;
        this.classLoader = (TreeClassLoader) this.interpreter.getClassLoader();
        this.classInfoLoader = new ClassInfoLoader();
    }

    /**
     * Compiles a compilation unit
     * 
     * @param name the name of the class to compile
     */
    public Class compile(final String name) throws ClassNotFoundException {
        loadClass(name);
        return compileClasses(name);
    }

    /**
     * Compiles all the classes in the class pool
     * 
     * @param name the name of the class to return
     */
    public Class compileClasses(final String name) throws ClassNotFoundException {
        Class result = null;
        if (this.classPool.contains(name)) {
            ClassInfo ci;
            while ((ci = this.classPool.getFirstCompilable()) != null) {
                if (!classExists(ci.getName())) {
                    final Class c = compileClass(ci, name);
                    if (c != null) {
                        result = c;
                    }
                } else {
                    ci.setCompilable(false);
                }
            }
        }

        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        return result;
    }

    /**
     * Compiles a single class
     * 
     * @param td the type declaration
     * @param im the importation manager
     */
    public Class compileTree(final Context ctx, final TypeDeclaration td) {
        final ClassFinder cf = new TreeClassFinder(ctx, this.interpreter, this.classPool);
        final ClassInfo ci = new TreeClassInfo(td, cf);
        this.classPool.add(ci.getName(), ci);
        try {
            return compileClasses(ci.getName());
        } catch (final ClassNotFoundException e) {
            td.setProperty(NodeProperties.ERROR_STRINGS, new String[] { td.getName() });
            throw new ExecutionError("undefined.or.defined.class", td);
        }
    }

    /**
     * Compiles the given class info
     * 
     * @param ci the class info to compile
     * @param name the name of the class to return
     */
    protected Class compileClass(final ClassInfo ci, final String name) {
        Class result = null;

        // Compile first the superclass and interfaces if needed
        ClassInfo t = ci.getSuperclass();
        if (t.isCompilable() && !classExists(t.getName())) {
            final Class c = compileClass(t, name);
            if (c != null) {
                result = c;
            }
        }

        final ClassInfo[] ti = ci.getInterfaces();
        for (int i = 0; i < ti.length; i++) {
            t = ti[i];
            if (t.isCompilable() && !classExists(t.getName())) {
                final Class c = compileClass(t, name);
                if (c != null) {
                    result = c;
                }
            }
        }

        // Then compile the class
        final Class c = new ClassInfoCompiler(ci).compile();
        ci.setCompilable(false);
        if (name.equals(c.getName())) {
            result = c;
        }

        return result;
    }

    /**
     * Whether a class exists in a compiled form
     */
    protected boolean classExists(final String name) {
        return this.classLoader.hasDefined(name);
    }

    /**
     * Searches for a class, loads its class info structure
     */
    protected void loadClass(final String name) throws ClassNotFoundException {
        if (this.classPool.contains(name)) {
            return;
        }

        // Is there a tree associated with this name ?
        TypeDeclaration td = this.classLoader.getTree(name);
        if (td != null) {
            final ImportationManager im = (ImportationManager) td.getProperty(NodeProperties.IMPORTATION_MANAGER);
            final Context ctx = new GlobalContext(this.interpreter, this.classInfoLoader);
            im.setClassLoader(this.classInfoLoader);
            ctx.setImportationManager(im);
            final ClassFinder cfinder = new TreeClassFinder(ctx, this.interpreter, this.classPool);
            this.classPool.add(name, new TreeClassInfo(td, cfinder));
            return;
        }

        // Is the class tree already loaded ?
        final LibraryFinder lf = this.interpreter.getLibraryFinder();
        try {
            final String cun = lf.findCompilationUnitName(name);
            td = this.classLoader.getTree(cun);
            if (td != null) {
                final ImportationManager im = (ImportationManager) td.getProperty(NodeProperties.IMPORTATION_MANAGER);
                final Context ctx = new GlobalContext(this.interpreter, this.classInfoLoader);
                im.setClassLoader(this.classInfoLoader);
                ctx.setImportationManager(im);
                final ClassFinder cfinder = new TreeClassFinder(ctx, this.interpreter, this.classPool);
                this.classPool.add(cun, new TreeClassInfo(td, cfinder));
                return;
            }
        } catch (final ClassNotFoundException e) {
        }

        try {
            final File f = lf.findCompilationUnit(name);
            final FileInputStream fis = new FileInputStream(f);

            final ParserFactory pf = this.interpreter.getParserFactory();
            final SourceCodeParser p = pf.createParser(fis, f.getCanonicalPath());
            final List stmts = p.parseCompilationUnit();

            final Iterator it = stmts.iterator();
            final Visitor v = new CompilationUnitVisitor();
            while (it.hasNext()) {
                ((Node) it.next()).acceptVisitor(v);
            }
        } catch (final IOException e) {
            throw new ClassNotFoundException(name);
        }
    }

    /**
     * To create the class infos for a compilation unit
     */
    protected class CompilationUnitVisitor extends VisitorObject {
        /**
         * The context
         */
        protected Context context = new GlobalContext(TreeCompiler.this.interpreter, TreeCompiler.this.classInfoLoader);

        /**
         * The class finder
         */
        protected ClassFinder classFinder = new TreeClassFinder(this.context, TreeCompiler.this.interpreter, TreeCompiler.this.classPool);

        /**
         * Visits a PackageDeclaration
         * 
         * @param node the node to visit
         * @return null
         */
        @Override
        public Object visit(final PackageDeclaration node) {
            this.context.setCurrentPackage(node.getName());
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
                this.context.declarePackageImport(node.getName());
            } else {
                try {
                    this.context.declareClassImport(node.getName());
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
         * Visits a type declaration
         * 
         * @param node the node to visit
         */
        protected Object visitType(final TypeDeclaration node) {
            String cname = this.classFinder.getCurrentPackage();
            cname = (cname.equals("") ? "" : cname + ".") + node.getName();
            TreeCompiler.this.classPool.add(cname, new TreeClassInfo(node, this.classFinder));
            return null;
        }
    }

    /**
     * To load class infos instead of classes
     */
    protected class ClassInfoLoader extends ClassLoader {
        /**
         * Finds the specified class.
         * 
         * @param name the name of the class
         * @return the resulting <code>Class</code> object
         * @exception ClassNotFoundException if the class could not be find
         */
        @Override
        protected Class findClass(final String name) throws ClassNotFoundException {
            TreeCompiler.this.loadClass(name);
            if (TreeCompiler.this.classPool.contains(name)) {
                throw new PseudoError(TreeCompiler.this.classPool.get(name));
            } else {
                throw new ClassNotFoundException(name);
            }
        }
    }

    /**
     * To test the existance of a class without loading it
     */
    public class PseudoError extends Error {
        /**
         * The exception content
         */
        protected ClassInfo classInfo;

        /**
         * Creates a new error
         */
        PseudoError(final ClassInfo ci) {
            this.classInfo = ci;
        }

        /**
         * Returns the class info
         */
        public ClassInfo getClassInfo() {
            return this.classInfo;
        }
    }
}
