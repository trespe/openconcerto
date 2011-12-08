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

package koala.dynamicjava.classfile;

/**
 * The classes derived from this one are used to represents class members
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/06
 */

public abstract class MemberIdentifier {
    /**
     * The declaring class for this member
     */
    private final String declaringClass;

    /**
     * The name of this member
     */
    private final String name;

    /**
     * The type of this member in JVM format
     */
    private final String type;

    /**
     * Initializes the identifier
     * 
     * @param dc the declaring class of this member
     * @param n the name of this member
     * @param t the type of this member in JVM format
     */
    public MemberIdentifier(final String dc, final String n, final String t) {
        this.declaringClass = dc;
        this.name = n;
        this.type = t;
    }

    /**
     * Returns the declaring class of this member
     */
    public String getDeclaringClass() {
        return this.declaringClass;
    }

    /**
     * Returns the name of this member
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the type of this member in JVM format
     */
    public String getType() {
        return this.type;
    }

    /**
     * Indicates whether some other object is equal to this one
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null || !getClass().equals(other.getClass())) {
            return false;
        }
        final MemberIdentifier mi = (MemberIdentifier) other;
        return this.name.equals(mi.name) && this.type.equals(mi.type) && this.declaringClass.equals(mi.declaringClass);
    }

    /**
     * Returns a hash code value for this object
     */
    @Override
    public int hashCode() {
        return this.name.hashCode() + this.type.hashCode() + this.declaringClass.hashCode();
    }
}
