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

import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;

import koala.dynamicjava.interpreter.context.Context;
import koala.dynamicjava.tree.ArrayAccess;
import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This interface represents objets that modify an array
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/28
 */

public class ArrayModifier extends LeftHandSideModifier {
    /**
     * The array expression
     */
    protected ArrayAccess node;

    /**
     * The array reference
     */
    protected Object array;

    /**
     * The cell number
     */
    protected Number cell;

    /**
     * A list used to manage recursive calls
     */
    protected List arrays = new LinkedList();

    /**
     * A list used to manage recursive calls
     */
    protected List cells = new LinkedList();

    /**
     * Creates a new array modifier
     * 
     * @param node the node of that represents this array
     */
    public ArrayModifier(final ArrayAccess node) {
        this.node = node;
    }

    /**
     * Prepares the modifier for modification
     */
    @Override
    public Object prepare(final Visitor v, final Context ctx) {
        this.arrays.add(0, this.array);
        this.cells.add(0, this.cell);

        this.array = this.node.getExpression().acceptVisitor(v);
        Object o = this.node.getCellNumber().acceptVisitor(v);
        if (o instanceof Character) {
            o = new Integer(((Character) o).charValue());
        }
        this.cell = (Number) o;
        return Array.get(this.array, this.cell.intValue());
    }

    /**
     * Sets the value of the underlying left hand side expression
     */
    @Override
    public void modify(final Context ctx, final Object value) {
        try {
            Array.set(this.array, this.cell.intValue(), value);
        } catch (final IllegalArgumentException e) {
            // !!! Hummm ...
            if (e.getMessage().equals("array element type mismatch")) {
                throw new ArrayStoreException();
            }
            throw e;
        } finally {
            this.array = this.arrays.remove(0);
            this.cell = (Number) this.cells.remove(0);
        }
    }
}
