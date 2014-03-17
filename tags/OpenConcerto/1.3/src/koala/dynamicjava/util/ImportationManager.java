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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * The instances of this class manages the importation clauses.
 * 
 * The <code>declarePackageImport</code> method imports a new package. This one has the highest
 * priority over the imported packages when a lookup is made to find a class.
 * 
 * The <code>declareClassImport</code> method imports a new class. This one has the highest priority
 * over the same suffix imported class.
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/28
 */

public class ImportationManager implements Cloneable {
    /**
     * This list contains the import-on-demand clauses.
     */
    protected List importOnDemandClauses = new LinkedList();

    /**
     * This set contains the single-type-import clauses.
     */
    protected List singleTypeImportClauses = new LinkedList();

    /**
     * This string contains the name of the current package
     */
    protected String currentPackage;

    /**
     * The class loader that must be used
     */
    protected ClassLoader classLoader;

    /**
     * Creates a new importation manager. The manager is initialized with two predefined
     * import-on-demand clauses: "java.lang" (the java language package) and "" (the anonymous
     * package)
     * 
     * @param cl the class loader to use
     */
    public ImportationManager(final ClassLoader cl) {
        this.classLoader = cl;
        declarePackageImport("java.lang");
        setCurrentPackage("");
    }

    /**
     * Returns a copy of this object
     */
    @Override
    public Object clone() {
        return new ImportationManager(this);
    }

    /**
     * Sets the class loader
     */
    public void setClassLoader(final ClassLoader cl) {
        this.classLoader = cl;
    }

    /**
     * Sets the current package. This has no influence on the behaviour of the
     * <code>lookupClass</code> method.
     * 
     * @param pkg the package name
     */
    public void setCurrentPackage(final String pkg) {
        this.currentPackage = pkg;
    }

    /**
     * Returns the current package
     */
    public String getCurrentPackage() {
        return this.currentPackage;
    }

    /**
     * Returns the import-on-demand clauses
     */
    public List getImportOnDemandClauses() {
        return this.importOnDemandClauses;
    }

    /**
     * Returns the single-type-import clauses
     */
    public List getSingleTypeImportClauses() {
        return this.singleTypeImportClauses;
    }

    /**
     * Declares a new import-on-demand clause
     * 
     * @param pkg the package name
     */
    public void declarePackageImport(final String pkg) {
        if (this.importOnDemandClauses.size() == 0 || !this.importOnDemandClauses.get(0).equals(pkg)) {
            this.importOnDemandClauses.remove(pkg);
            this.importOnDemandClauses.add(0, pkg);
        }
    }

    /**
     * Declares a new single-type-import clause
     * 
     * @param cname the fully qualified class name
     * @exception ClassNotFoundException if the class cannot be found
     */
    public void declareClassImport(final String cname) throws ClassNotFoundException {
        try {
            // A previous importation of this class is removed to avoid a new
            // existance verification and to speed up further loadings.
            if (!this.singleTypeImportClauses.remove(cname)) {
                Class.forName(cname, true, this.classLoader);
            }
        } catch (final ClassNotFoundException e) {
            // try to find an inner class with this name
            final Class c = findInnerClass(cname);
            this.singleTypeImportClauses.remove(c == null ? c.getName() : cname);
            this.singleTypeImportClauses.add(0, c == null ? c.getName() : cname);
        } finally {
            this.singleTypeImportClauses.add(0, cname);
        }
    }

    /**
     * Loads the class that match to the given name in the source file
     * 
     * @param cname the name of the class to find
     * @param ccname the name of the current class or null
     * @return the class found
     * @exception ClassNotFoundException if the class cannot be loaded
     */
    public Class lookupClass(final String cname, final String ccname) throws ClassNotFoundException {
        final String str = cname.replace('.', '$');

        // Search for the full name ...

        // ... in the current package ...
        final String t = this.currentPackage.equals("") ? cname : this.currentPackage + "." + cname;
        try {
            return Class.forName(t, false, this.classLoader);
        } catch (final ClassNotFoundException e) {
        }

        if (cname.indexOf('.') != -1) {
            try {
                return Class.forName(cname, false, this.classLoader);
            } catch (final ClassNotFoundException e) {
            }
        }

        // try to find an inner class with this name
        try {
            return findInnerClass(t);
        } catch (final ClassNotFoundException e) {
        }

        // ... in the single-type-import clauses ...
        Iterator it = this.singleTypeImportClauses.iterator();
        while (it.hasNext()) {
            final String s = (String) it.next();
            if (hasSuffix(s, cname) || hasSuffix(s, str)) {
                return Class.forName(s, false, this.classLoader);
            }
            // It is perhaps an innerclass of an imported class
            // ie. a.b.C and C$D
            final int i = str.indexOf('$');
            if (i != -1) {
                try {
                    if (hasSuffix(s, str.substring(0, i))) {
                        return Class.forName(s + str.substring(i, str.length()), false, this.classLoader);
                    }
                } catch (final ClassNotFoundException e) {
                }
            }
        }

        if (ccname != null) {
            // ... in the current class ...
            try {
                return Class.forName(ccname + "$" + str, false, this.classLoader);
            } catch (final ClassNotFoundException e) {
            }

            // ... it is perhaps an outer class
            it = getOuterNames(ccname).iterator();
            String tmp = ccname;
            while (it.hasNext()) {
                final String s = (String) it.next();
                final int i = tmp.lastIndexOf(s) + s.length();
                tmp = tmp.substring(0, i);
                if (s.equals(cname)) {
                    return Class.forName(tmp, false, this.classLoader);
                }
            }

            // ... or the class itself
            if (ccname.endsWith(cname)) {
                final int i = ccname.lastIndexOf(cname);
                if (i > 0 && ccname.charAt(i - 1) == '$') {
                    return Class.forName(ccname, false, this.classLoader);
                }
            }
        }

        // ... with the import-on-demand clauses as prefix
        it = this.importOnDemandClauses.iterator();
        while (it.hasNext()) {
            final String s = (String) it.next();
            try {
                return Class.forName(s + "." + str, false, this.classLoader);
            } catch (final ClassNotFoundException e) {
            }
        }

        throw new ClassNotFoundException(cname);
    }

    /**
     * Returns a list of the outer classes names
     */
    protected List getOuterNames(String cname) {
        final List l = new LinkedList();
        int i;
        while ((i = cname.lastIndexOf('$')) != -1) {
            cname = cname.substring(0, i);
            if ((i = cname.lastIndexOf('$')) != -1) {
                l.add(cname.substring(i + 1, cname.length()));
            } else if ((i = cname.lastIndexOf('.')) != -1) {
                l.add(cname.substring(i + 1, cname.length()));
            } else {
                l.add(cname);
            }
        }
        return l;
    }

    /**
     * Searches for an inner class from its name in the dotted notation
     */
    protected Class findInnerClass(String s) throws ClassNotFoundException {
        int n;
        while ((n = s.lastIndexOf('.')) != -1) {
            s = s.substring(0, n) + '$' + s.substring(n + 1, s.length());
            try {
                return Class.forName(s, false, this.classLoader);
            } catch (final ClassNotFoundException e) {
            }
        }
        throw new ClassNotFoundException(s);
    }

    /**
     * Copy constructor
     */
    protected ImportationManager(final ImportationManager im) {
        this.importOnDemandClauses = (List) ((LinkedList) im.importOnDemandClauses).clone();
        this.singleTypeImportClauses = (List) ((LinkedList) im.singleTypeImportClauses).clone();
        this.currentPackage = im.currentPackage;
        this.classLoader = im.classLoader;
    }

    /**
     * Tests whether the fully qualified class name c1 ends with c2
     */
    protected boolean hasSuffix(final String c1, final String c2) {
        final int i = c1.lastIndexOf('.');
        String s = c1;
        if (i != -1) {
            s = c1.substring(i + 1, c1.length());
        }
        return s.equals(c2);
    }
}
