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

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import koala.dynamicjava.interpreter.context.Context;
import koala.dynamicjava.interpreter.error.CatchedExceptionError;
import koala.dynamicjava.tree.ObjectFieldAccess;
import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This interface represents the objets that modify an object field
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/11/28
 */

public class ObjectFieldModifier extends LeftHandSideModifier {
    /**
     * The field
     */
    protected Field field;

    /**
     * The node
     */
    protected ObjectFieldAccess node;

    /**
     * The field
     */
    protected Object fieldObject;

    /**
     * The list used to manage recursive calls
     */
    protected List fields = new LinkedList();

    /**
     * Creates a new field modifier
     * 
     * @param f the field to modify
     * @param n the field access node
     */
    public ObjectFieldModifier(final Field f, final ObjectFieldAccess n) {
        this.field = f;
        this.node = n;
    }

    /**
     * Prepares the modifier for modification
     */
    @Override
    public Object prepare(final Visitor v, final Context ctx) {
        this.fields.add(0, this.fieldObject);

        this.fieldObject = this.node.getExpression().acceptVisitor(v);
        try {
            return this.field.get(this.fieldObject);
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, this.node);
        }
    }

    /**
     * Sets the value of the underlying left hand side expression
     */
    @Override
    public void modify(final Context ctx, final Object value) {
        try {
            this.field.set(this.fieldObject, value);
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, this.node);
        } finally {
            this.fieldObject = this.fields.remove(0);
        }
    }
}
