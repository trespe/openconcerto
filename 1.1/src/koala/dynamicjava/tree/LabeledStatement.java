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
 * This class represents the labeled statement nodes of the syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/23
 */

public class LabeledStatement extends Statement {
    /**
     * The label property name
     */
    public final static String LABEL = "label";

    /**
     * The statement property name
     */
    public final static String STATEMENT = "statement";

    /**
     * The label
     */
    private String label;

    /**
     * The statement
     */
    private Node statement;

    /**
     * Creates a new while statement
     * 
     * @param label the label
     * @param stat the statement
     * @exception IllegalArgumentException if label is null or stat is null
     */
    public LabeledStatement(final String label, final Node stat) {
        this(label, stat, null, 0, 0, 0, 0);
    }

    /**
     * Creates a new while statement
     * 
     * @param label the label
     * @param stat the statement
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if label is null or stat is null
     */
    public LabeledStatement(final String label, final Node stat, final String fn, final int bl, final int bc, final int el, final int ec) {
        super(fn, bl, bc, el, ec);

        if (label == null) {
            throw new IllegalArgumentException("label == null");
        }
        if (stat == null) {
            throw new IllegalArgumentException("stat == null");
        }

        this.label = label;
        this.statement = stat;
    }

    /**
     * Gets the label
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Sets the label
     * 
     * @exception IllegalArgumentException if e is null
     */
    public void setLabel(final String s) {
        if (s == null) {
            throw new IllegalArgumentException("s == null");
        }

        firePropertyChange(LABEL, this.label, this.label = s);
    }

    /**
     * Returns the statement
     */
    public Node getStatement() {
        return this.statement;
    }

    /**
     * Sets the statement
     * 
     * @exception IllegalArgumentException if n is null
     */
    public void setStatement(final Node n) {
        if (n == null) {
            throw new IllegalArgumentException("n == null");
        }

        firePropertyChange(STATEMENT, this.statement, this.statement = n);
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
