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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import koala.dynamicjava.classinfo.ClassInfo;

/**
 * The instances of this class contains classinfos
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/28
 */

public class ClassPool {
    /**
     * The map that contains the classinfos
     */
    protected Map classes = new HashMap(11);

    /**
     * Adds a classinfo to the pool
     * 
     * @param cn the classname
     * @param ci the classinfo
     * @return the given class info
     */
    public ClassInfo add(final String cn, final ClassInfo ci) {
        this.classes.put(cn, ci);

        // Add the inner classes to the pool
        final ClassInfo[] infos = ci.getDeclaredClasses();
        for (int i = 0; i < infos.length; i++) {
            final String s = infos[i].getName();
            if (!this.classes.containsKey(s)) {
                add(s, infos[i]);
            }
        }
        return ci;
    }

    /**
     * Tests whether this pool contains the given class
     * 
     * @param cn the classname
     */
    public boolean contains(final String cn) {
        return this.classes.containsKey(cn);
    }

    /**
     * Returns the class info mapped with the given key
     * 
     * @param cn the classname
     */
    public ClassInfo get(final String cn) {
        return (ClassInfo) this.classes.get(cn);
    }

    /**
     * Gets the first compilable class in the pool
     * 
     * @return null if no class was found
     */
    public ClassInfo getFirstCompilable() {
        final Iterator it = this.classes.keySet().iterator();
        while (it.hasNext()) {
            final ClassInfo ci = (ClassInfo) this.classes.get(it.next());
            if (ci.isCompilable()) {
                return ci;
            }
        }
        return null;
    }
}
