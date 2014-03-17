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

import koala.dynamicjava.classinfo.ClassFinder;
import koala.dynamicjava.classinfo.ClassInfo;
import koala.dynamicjava.classinfo.JavaClassInfo;
import koala.dynamicjava.classinfo.TreeClassInfo;
import koala.dynamicjava.interpreter.context.Context;
import koala.dynamicjava.tree.TypeDeclaration;
import koala.dynamicjava.util.ImportationManager;

/**
 * The instances of the classes that implements this interface are used to find the fully qualified
 * name of classes and to manage the loading of these classes.
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/28
 */

public class TreeClassFinder implements ClassFinder {
    /**
     * The context
     */
    protected Context context;

    /**
     * The current interpreter
     */
    protected Interpreter interpreter;

    /**
     * The class pool
     */
    protected ClassPool classPool;

    /**
     * Creates a new class finder
     * 
     * @param ctx the context
     * @param i the current interpreter
     * @param cp the class pool
     */
    public TreeClassFinder(final Context ctx, final Interpreter i, final ClassPool cp) {
        this.context = ctx;
        this.interpreter = i;
        this.classPool = cp;
    }

    /**
     * Returns the current interpreter
     */
    public Interpreter getInterpreter() {
        return this.interpreter;
    }

    /**
     * Returns the current package
     */
    public String getCurrentPackage() {
        return this.context.getCurrentPackage();
    }

    /**
     * Returns the importation manager
     */
    public ImportationManager getImportationManager() {
        return this.context.getImportationManager();
    }

    /**
     * Loads the class info that match the given name in the source file
     * 
     * @param cname the name of the class to find
     * @return the class info
     * @exception ClassNotFoundException if the class cannot be loaded
     */
    public ClassInfo lookupClass(final String cname) throws ClassNotFoundException {
        if (this.classPool.contains(cname)) {
            return this.classPool.get(cname);
        }
        try {
            return new JavaClassInfo(this.context.lookupClass(cname, null));
        } catch (final TreeCompiler.PseudoError e) {
            return e.getClassInfo();
        }
    }

    /**
     * Loads the class info that match the given name in the source file
     * 
     * @param cname the name of the class to find
     * @param cinfo the context where 'cname' was found
     * @return the class info
     * @exception ClassNotFoundException if the class cannot be loaded
     */
    public ClassInfo lookupClass(final String cname, final ClassInfo cinfo) throws ClassNotFoundException {
        final String name = cinfo.getName();
        if (this.classPool.contains(cname)) {
            return this.classPool.get(cname);
        } else {
            // cname represents perhaps an inner class
            final String s = name + "$" + cname;
            if (this.classPool.contains(s)) {
                return this.classPool.get(s);
            }
        }
        try {
            return new JavaClassInfo(this.context.lookupClass(cname, name));
        } catch (final ClassNotFoundException e) {
            // look after an inner class of the declaring class
            ClassInfo ci = cinfo.getDeclaringClass();
            try {
                if (ci != null) {
                    return new JavaClassInfo(this.context.lookupClass(ci.getName() + "$" + cname));
                }
                throw new ClassNotFoundException(cname);
            } catch (final ClassNotFoundException ex) {
                // Look after an inner class of an ancestor
                ci = cinfo;
                while ((ci = ci.getSuperclass()) != null) {
                    try {
                        return new JavaClassInfo(this.context.lookupClass(ci.getName() + "$" + cname));
                    } catch (final ClassNotFoundException e2) {
                    } catch (final TreeCompiler.PseudoError e2) {
                        return e2.getClassInfo();
                    }
                }
            } catch (final TreeCompiler.PseudoError ex) {
                return ex.getClassInfo();
            }
        } catch (final TreeCompiler.PseudoError e) {
            return e.getClassInfo();
        }
        throw new ClassNotFoundException(cname);
    }

    /**
     * Adds a type declaration in the class info list
     * 
     * @param cname the name of the class
     * @param decl the type declaration
     */
    public ClassInfo addClassInfo(final String cname, final TypeDeclaration decl) {
        return this.classPool.add(cname, new TreeClassInfo(decl, this));
    }
}
