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
 * This class represents the for statement nodes of the syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/23
 */

public class ForStatement extends Statement implements ContinueTarget {
    /**
     * The initialization property name
     */
    public final static String INITIALIZATION = "initialization";

    /**
     * The condition property name
     */
    public final static String CONDITION = "condition";

    /**
     * The update property name
     */
    public final static String UPDATE = "update";

    /**
     * The body property name
     */
    public final static String BODY = "body";

    /**
     * The initialization statements
     */
    private List initialization;

    /**
     * The condition to evaluate at each loop
     */
    private Expression condition;

    /**
     * The update statements
     */
    private List update;

    /**
     * The body of this statement
     */
    private Node body;

    /**
     * The labels
     */
    private final List labels;

    /**
     * Creates a new for statement
     * 
     * @param init the initialization statements
     * @param cond the condition to evaluate at each loop
     * @param updt the update statements
     * @param body the body
     * @exception IllegalArgumentException if body is null
     */
    public ForStatement(final List init, final Expression cond, final List updt, final Node body) {
        this(init, cond, updt, body, null, 0, 0, 0, 0);
    }

    /**
     * Creates a new for statement
     * 
     * @param init the initialization statements
     * @param cond the condition to evaluate at each loop
     * @param updt the update statements
     * @param body the body
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if body is null
     */
    public ForStatement(final List init, final Expression cond, final List updt, final Node body, final String fn, final int bl, final int bc, final int el, final int ec) {
        super(fn, bl, bc, el, ec);

        if (body == null) {
            throw new IllegalArgumentException("body == null");
        }

        this.initialization = init;
        this.condition = cond;
        this.update = updt;
        this.body = body;
        this.labels = new LinkedList();
    }

    /**
     * Gets the initialization statements
     */
    public List getInitialization() {
        return this.initialization;
    }

    /**
     * Sets the initialization statements
     */
    public void setInitialization(final List l) {
        firePropertyChange(INITIALIZATION, this.initialization, this.initialization = l);
    }

    /**
     * Gets the condition to evaluate at each loop
     */
    public Expression getCondition() {
        return this.condition;
    }

    /**
     * Sets the condition to evaluate
     */
    public void setCondition(final Expression e) {
        firePropertyChange(CONDITION, this.condition, this.condition = e);
    }

    /**
     * Gets the update statements
     */
    public List getUpdate() {
        return this.update;
    }

    /**
     * Sets the update statements
     */
    public void setUpdate(final List l) {
        firePropertyChange(UPDATE, this.update, this.update = l);
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
