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

import java.util.LinkedList;
import java.util.List;

import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This class represents the while statement nodes of the syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/13
 */

public class WhileStatement extends Statement implements ContinueTarget {
    /**
     * The condition property name
     */
    public final static String CONDITION = "condition";

    /**
     * The body property name
     */
    public final static String BODY = "body";

    /**
     * The condition to evaluate at each loop
     */
    private Expression condition;

    /**
     * The body of this statement
     */
    private Node body;

    /**
     * The labels
     */
    private final List labels;

    /**
     * Creates a new while statement
     * 
     * @param cond the condition to evaluate at each loop
     * @param body the body
     * @exception IllegalArgumentException if cond is null or body is null
     */
    public WhileStatement(final Expression cond, final Node body) {
        this(cond, body, null, 0, 0, 0, 0);
    }

    /**
     * Creates a new while statement
     * 
     * @param cond the condition to evaluate at each loop
     * @param body the body
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if cond is null or body is null
     */
    public WhileStatement(final Expression cond, final Node body, final String fn, final int bl, final int bc, final int el, final int ec) {
        super(fn, bl, bc, el, ec);

        if (cond == null) {
            throw new IllegalArgumentException("cond == null");
        }
        if (body == null) {
            throw new IllegalArgumentException("body == null");
        }

        this.condition = cond;
        this.body = body;
        this.labels = new LinkedList();
    }

    /**
     * Gets the condition to evaluate at each loop
     */
    public Expression getCondition() {
        return this.condition;
    }

    /**
     * Sets the condition to evaluate
     * 
     * @exception IllegalArgumentException if e is null
     */
    public void setCondition(final Expression e) {
        if (e == null) {
            throw new IllegalArgumentException("e == null");
        }

        firePropertyChange(CONDITION, this.condition, this.condition = e);
    }

    /**
     * Returns the body of this statement
     */
    public Node getBody() {
        return this.body;
    }

    /**
     * Sets the body of this statement
     * 
     * @exception IllegalArgumentException if node is null
     */
    public void setBody(final Node node) {
        if (node == null) {
            throw new IllegalArgumentException("node == null");
        }

        firePropertyChange(BODY, this.body, this.body = node);
    }

    /**
     * Adds a label to this statement
     * 
     * @param label the label to add
     * @exception IllegalArgumentException if label is null
     */
    public void addLabel(final String label) {
        if (label == null) {
            throw new IllegalArgumentException("label == null");
        }

        this.labels.add(label);
    }

    /**
     * Test whether this statement has the given label
     * 
     * @return true if this statement has the given label
     */
    public boolean hasLabel(final String label) {
        return this.labels.contains(label);
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
