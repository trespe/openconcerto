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

package koala.dynamicjava.tree;

import java.util.List;

import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This class represents the static method call nodes of the syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/01
 */

public class StaticMethodCall extends MethodCall {
    /**
     * The methodType property name
     */
    public final static String METHOD_TYPE = "methodType";

    /**
     * The type on which this method call applies
     */
    private ReferenceType methodType;

    /**
     * Creates a new node
     * 
     * @param typ the type on which this method call applies
     * @param mn the field name
     * @param args the arguments. Can be null.
     * @exception IllegalArgumentException if typ is null or mn is null
     */
    public StaticMethodCall(final ReferenceType typ, final String mn, final List args) {
        this(typ, mn, args, null, 0, 0, 0, 0);
    }

    /**
     * Creates a new node
     * 
     * @param typ the type on which this method call applies
     * @param mn the field name
     * @param args the arguments. Can be null.
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if typ is null or mn is null
     */
    public StaticMethodCall(final ReferenceType typ, final String mn, final List args, final String fn, final int bl, final int bc, final int el, final int ec) {
        super(mn, args, fn, bl, bc, el, ec);

        if (typ == null) {
            throw new IllegalArgumentException("typ == null");
        }

        this.methodType = typ;
    }

    /**
     * Returns the type on which this method call applies
     */
    public ReferenceType getMethodType() {
        return this.methodType;
    }

    /**
     * Sets the declaring type of the method
     * 
     * @exception IllegalArgumentException if t is null
     */
    public void setMethodType(final ReferenceType t) {
        if (t == null) {
            throw new IllegalArgumentException("t == null");
        }

        firePropertyChange(METHOD_TYPE, this.methodType, this.methodType = t);
    }

    /**
     * Allows a visitor to traverse the tree
     * 
     * @param visitor the visitor to accept
     */
    @Override
    public Object acceptVisitor(final Visitor visitor) {
        return visitor.visit(this);
    }
}
