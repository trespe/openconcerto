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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import koala.dynamicjava.tree.TypeDeclaration;

/**
 * This class is responsible for loading bytecode classes
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/05/18
 */

public class TreeClassLoader extends ClassLoader implements ClassLoaderContainer {
    /**
     * The place where the interpreted classes are stored
     */
    protected Map classes = new HashMap(11);

    /**
     * The syntax trees
     */
    protected Map trees = new HashMap(11);

    /**
     * The interpreter
     */
    protected Interpreter interpreter;

    /**
     * The auxiliary class loader
     */
    protected ClassLoader classLoader;

    /**
     * Creates a new class loader
     * 
     * @param i the object used to interpret the classes
     */
    public TreeClassLoader(final Interpreter i) {
        this.interpreter = i;
    }

    /**
     * Converts an array of bytes into an instance of class Class and links this class.
     * 
     * @exception ClassFormatError if the class could not be defined
     */
    public Class defineClass(final String name, final byte[] code) {
        final Class c = defineClass(name, code, 0, code.length);
        this.classes.put(name, c);
        this.trees.remove(name);
        return c;
    }

    /**
     * Returns the additional class loader that is used for loading classes from the net.
     * 
     * @return null if there is no additional class loader
     */
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Whether a class was defined by this class loader
     */
    public boolean hasDefined(final String name) {
        return this.classes.containsKey(name);
    }

    /**
     * Returns the names of the defined classes in a set
     */
    public Set getClassNames() {
        return this.classes.keySet();
    }

    /**
     * Adds a class syntax tree to the list of the loaded trees
     * 
     * @param name the name of the type
     * @param node the tree
     */
    public void addTree(final String name, final TypeDeclaration node) {
        this.trees.put(name, node);
    }

    /**
     * Gets a tree
     */
    public TypeDeclaration getTree(final String name) {
        return (TypeDeclaration) this.trees.get(name);
    }

    /**
     * Adds an URL in the class path
     */
    public void addURL(final URL url) {
        if (this.classLoader == null) {
            this.classLoader = new URLClassLoader(new URL[] { url });
        } else {
            this.classLoader = new URLClassLoader(new URL[] { url }, this.classLoader);
        }
    }

    /**
     * Finds the specified class.
     * 
     * @param name the name of the class
     * @return the resulting <code>Class</code> object
     * @exception ClassNotFoundException if the class could not be find
     */
    @Override
    protected Class findClass(final String name) throws ClassNotFoundException {
        if (this.classes.containsKey(name)) {
            return (Class) this.classes.get(name);
        }

        try {
            if (this.classLoader != null) {
                return Class.forName(name, true, this.classLoader);
            }
        } catch (final ClassNotFoundException e) {
        }

        return this.interpreter.loadClass(name);
    }
}
