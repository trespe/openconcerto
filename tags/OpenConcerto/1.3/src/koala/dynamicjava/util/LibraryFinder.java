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

package koala.dynamicjava.util;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The instances of <code>LibraryFinder</code> are used to locate files with given suffixes.
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/19
 */

public class LibraryFinder extends FileFinder {
    /**
     * The file suffixes
     */
    private final List suffixes;

    /**
     * Creates a new library finder
     * 
     * @param suffix the suffix of the source files
     */
    public LibraryFinder() {
        this.suffixes = new LinkedList();
    }

    /**
     * Adds a file suffix, for example ".java"
     */
    public void addSuffix(final String s) {
        this.suffixes.add(s);
    }

    /**
     * Looks for the dynamic class with the given name
     * 
     * @param cname the fully qualified name of the class to find
     * @return the file that contains the class
     * @exception ClassNotFoundException if the class cannot be loaded
     */
    public File findCompilationUnit(final String cname) throws ClassNotFoundException {
        final Iterator it = this.suffixes.iterator();
        while (it.hasNext()) {
            String fname = cname.replace('.', '/') + it.next();
            try {
                return findFile(fname);
            } catch (final IOException e) {
            }
            int n;
            while ((n = fname.lastIndexOf('$')) != -1) {
                fname = fname.substring(0, n) + fname.substring(fname.lastIndexOf('.'), fname.length());
                try {
                    return findFile(fname);
                } catch (final IOException e) {
                }
            }
        }
        throw new ClassNotFoundException(cname);
    }

    /**
     * Finds the path where the given class is possibly stored
     * 
     * @param cname the fully qualified name of the class to find
     * @return the name of the root class
     */
    public String findCompilationUnitName(final String cname) throws ClassNotFoundException {
        final Iterator it = this.suffixes.iterator();
        while (it.hasNext()) {
            String fname = cname.replace('.', '/') + it.next();
            try {
                findFile(fname);
                return cname;
            } catch (final IOException e) {
            }
            int n;
            while ((n = fname.lastIndexOf('$')) != -1) {
                fname = fname.substring(0, n) + fname.substring(fname.lastIndexOf('.'), fname.length());
                try {
                    findFile(fname);
                    final String result = fname.substring(0, fname.indexOf('.'));
                    return result.replace('/', '.');
                } catch (final IOException e) {
                }
            }
        }
        throw new ClassNotFoundException(cname);
    }
}
