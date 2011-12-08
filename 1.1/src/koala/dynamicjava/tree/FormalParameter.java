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

import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This class represents the method parameters in the syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/11
 */

public class FormalParameter extends Node {
    /**
     * The final property name
     */
    public final static String FINAL = "final";

    /**
     * The type property name
     */
    public final static String TYPE = "type";

    /**
     * The name property name
     */
    public final static String NAME = "name";

    /**
     * Is this parameter final?
     */
    private final boolean finalParameter;

    /**
     * The type of this parameter
     */
    private Type type;

    /**
     * The name of this parameter
     */
    private String name;

    /**
     * Initializes the node
     * 
     * @param f is the parameter final?
     * @param t the type of the parameter
     * @param n the name of the parameter
     * @exception IllegalArgumentException if t is null or n is null
     */
    public FormalParameter(final boolean f, final Type t, final String n) {
        this(f, t, n, null, 0, 0, 0, 0);
    }

    /**
     * Initializes the node
     * 
     * @param f is the parameter final?
     * @param t the type of the parameter
     * @param n the name of the parameter
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if t is null or n is null
     */
    public FormalParameter(final boolean f, final Type t, final String n, final String fn, final int bl, final int bc, final int el, final int ec) {
        super(fn, bl, bc, el, ec);

        if (t == null) {
            throw new IllegalArgumentException("t == null");
        }
        if (n == null) {
            throw new IllegalArgumentException("n == null");
        }

        this.finalParameter = f;
        this.type = t;
        this.name = n;
    }

    /**
     * Is this parameter final?
     */
    public boolean isFinal() {
        return this.finalParameter;
    }

    /**
     * Returns the declaring type of this parameter
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Sets the type of this parameter
     * 
     * @exception IllegalArgumentException if t is null
     */
    public void setType(final Type t) {
        if (t == null) {
            throw new IllegalArgumentException("t == null");
        }

        firePropertyChange(TYPE, this.type, this.type = t);
    }

    /**
     * The name of this parameter
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets this parameter's name
     * 
     * @exception IllegalArgumentException if s is null
     */
    public void setName(final String s) {
        if (s == null) {
            throw new IllegalArgumentException("s == null");
        }

        firePropertyChange(NAME, this.name, this.name = s);
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
