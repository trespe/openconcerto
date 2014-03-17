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

import java.util.HashMap;
import java.util.Map;

/**
 * A buffered version of the importation manager
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/11/28
 */

public class BufferedImportationManager extends ImportationManager {
    /**
     * The class buffer
     */
    protected Map buffer = new HashMap(11);

    /**
     * Creates a new importation manager.
     * 
     * @param cl the class loader to use
     */
    public BufferedImportationManager(final ClassLoader cl) {
        super(cl);
    }

    /**
     * Returns a copy of this object
     */
    @Override
    public Object clone() {
        return new BufferedImportationManager(this);
    }

    /**
     * Sets the current package. This has no influence on the behaviour of the
     * <code>lookupClass</code> method.
     * 
     * @param pkg the package name
     */
    @Override
    public void setCurrentPackage(final String pkg) {
        super.setCurrentPackage(pkg);
        this.buffer.clear();
    }

    /**
     * Declares a new import-on-demand clause
     * 
     * @param pkg the package name
     */
    @Override
    public void declarePackageImport(final String pkg) {
        super.declarePackageImport(pkg);
        if (this.buffer == null) {
            this.buffer = new HashMap(11);
        }
        this.buffer.clear();
    }

    /**
     * Declares a new single-type-import clause
     * 
     * @param cname the fully qualified class name
     * @exception ClassNotFoundException if the class cannot be found
     */
    @Override
    public void declareClassImport(final String cname) throws ClassNotFoundException {
        super.declareClassImport(cname);
        this.buffer.clear();
    }

    /**
     * Loads the class that match to the given name in the source file
     * 
     * @param cname the name of the class to find
     * @param ccname the name of the current class or null
     * @return the class found
     * @exception ClassNotFoundException if the class cannot be loaded
     */
    @Override
    public Class lookupClass(final String cname, final String ccname) throws ClassNotFoundException {
        Map m = (Map) this.buffer.get(ccname);
        if (m != null) {
            final Class c = (Class) m.get(cname);
            if (c != null) {
                return c;
            }
        }

        final Class c = super.lookupClass(cname, ccname);

        if (m == null) {
            m = new HashMap(11);
            this.buffer.put(ccname, m);
        }
        m.put(cname, c);

        return c;
    }

    /**
     * Copy constructor
     */
    protected BufferedImportationManager(final ImportationManager im) {
        super(im);
    }
}
