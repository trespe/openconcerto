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
 * This class represents the binary expression nodes of the syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/04/25
 */

public class ConditionalExpression extends Expression {
    /**
     * The conditionExpression property name
     */
    public final static String CONDITION_EXPRESSION = "conditionExpression";

    /**
     * The ifTrueExpression property name
     */
    public final static String IF_TRUE_EXPRESSION = "ifTrueExpression";

    /**
     * The ifFalseExpression property name
     */
    public final static String IF_FALSE_EXPRESSION = "ifFalseExpression";

    /**
     * The condition expression
     */
    private Expression conditionExpression;

    /**
     * The if true expression
     */
    private Expression ifTrueExpression;

    /**
     * The if false expression
     */
    private Expression ifFalseExpression;

    /**
     * Initializes the expression
     * 
     * @param cexp the condition expression
     * @param texp the if true expression
     * @param fexp the if false expression
     * @exception IllegalArgumentException if cexp is null or texp is null or fexp is null
     */
    public ConditionalExpression(final Expression cexp, final Expression texp, final Expression fexp) {
        this(cexp, texp, fexp, null, 0, 0, 0, 0);
    }

    /**
     * Initializes the expression
     * 
     * @param cexp the condition expression
     * @param texp the if true expression
     * @param fexp the if false expression
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if cexp is null or texp is null or fexp is null
     */
    public ConditionalExpression(final Expression cexp, final Expression texp, final Expression fexp, final String fn, final int bl, final int bc, final int el, final int ec) {
        super(fn, bl, bc, el, ec);

        if (cexp == null) {
            throw new IllegalArgumentException("cexp == null");
        }
        if (texp == null) {
            throw new IllegalArgumentException("texp == null");
        }
        if (fexp == null) {
            throw new IllegalArgumentException("fexp == null");
        }

        this.conditionExpression = cexp;
        this.ifTrueExpression = texp;
        this.ifFalseExpression = fexp;
    }

    /**
     * Returns the condition expression
     */
    public Expression getConditionExpression() {
        return this.conditionExpression;
    }

    /**
     * Sets the condition expression
     * 
     * @exception IllegalArgumentException if e is null
     */
    public void setConditionExpression(final Expression e) {
        if (e == null) {
            throw new IllegalArgumentException("e == null");
        }

        firePropertyChange(CONDITION_EXPRESSION, this.conditionExpression, this.conditionExpression = e);
    }

    /**
     * Returns the if true expression
     */
    public Expression getIfTrueExpression() {
        return this.ifTrueExpression;
    }

    /**
     * Sets the if true expression
     * 
     * @exception IllegalArgumentException if e is null
     */
    public void setIfTrueExpression(final Expression e) {
        if (e == null) {
            throw new IllegalArgumentException("e == null");
        }

        firePropertyChange(IF_TRUE_EXPRESSION, this.ifTrueExpression, this.ifTrueExpression = e);
    }

    /**
     * Returns the if false expression
     */
    public Expression getIfFalseExpression() {
        return this.ifFalseExpression;
    }

    /**
     * Sets the if false expression
     * 
     * @exception IllegalArgumentException if e is null
     */
    public void setIfFalseExpression(final Expression e) {
        if (e == null) {
            throw new IllegalArgumentException("e == null");
        }

        firePropertyChange(IF_FALSE_EXPRESSION, this.ifFalseExpression, this.ifFalseExpression = e);
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
