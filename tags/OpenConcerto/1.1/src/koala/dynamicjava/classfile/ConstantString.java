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
 * This class represents a constant string
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/06
 */

public class ConstantString {
    /**
     * The value of this constant
     */
    private final String value;

    /**
     * Creates a new constant
     */
    public ConstantString(final String v) {
        this.value = v;
    }

    /**
     * Returns the value of this constant
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Indicates whether some other object is equal to this one
     */
    @Override
    public boolean equals(final Object other) {
        if (other == null || !(other instanceof ConstantString)) {
            return false;
        }
        return this.value.equals(((ConstantString) other).value);
    }

    /**
     * Returns a hash code value for this object
     */
    @Override
    public int hashCode() {
        return "ConstantString".hashCode() + this.value.hashCode();
    }
}
