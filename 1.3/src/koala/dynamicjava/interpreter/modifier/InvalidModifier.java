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

package koala.dynamicjava.interpreter.modifier;

import koala.dynamicjava.interpreter.context.Context;
import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This interface represents an invalid modifier
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/25
 */

public class InvalidModifier extends LeftHandSideModifier {
    /**
     * The node
     */
    private final Node node;

    /**
     * Creates a new field modifier
     * 
     * @param n the node
     */
    public InvalidModifier(final Node n) {
        this.node = n;
    }

    /**
     * Prepares the modifier for modification
     */
    @Override
    public Object prepare(final Visitor v, final Context ctx) {
        throw new ExecutionError("cannot.modify", this.node);
    }

    /**
     * Sets the value of the underlying left hand side expression
     */
    @Override
    public void modify(final Context ctx, final Object value) {
    }
}
