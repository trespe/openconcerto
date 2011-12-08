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
 * This class represents the array allocation nodes of the syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/04/25
 */

public class ArrayAllocation extends Allocation {
    /**
     * The type descriptor
     */
    private final TypeDescriptor typeDescriptor;

    /**
     * Initializes the expression
     * 
     * @param tp the type prefix
     * @param td the type descriptor
     * @exception IllegalArgumentException if tp is null or td is null
     */
    public ArrayAllocation(final Type tp, final TypeDescriptor td) {
        this(tp, td, null, 0, 0, 0, 0);
    }

    /**
     * Initializes the expression
     * 
     * @param tp the type prefix
     * @param td the type descriptor
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if tp is null or td is null
     */
    public ArrayAllocation(final Type tp, final TypeDescriptor td, final String fn, final int bl, final int bc, final int el, final int ec) {
        super(tp, fn, bl, bc, el, ec);

        if (td == null) {
            throw new IllegalArgumentException("td == null");
        }

        this.typeDescriptor = td;
        td.initialize(tp);
    }

    /**
     * Returns the dimension of the array
     */
    public int getDimension() {
        return this.typeDescriptor.dimension;
    }

    /**
     * Returns the size expressions
     */
    public List getSizes() {
        return this.typeDescriptor.sizes;
    }

    /**
     * Returns the initialization expression
     */
    public ArrayInitializer getInitialization() {
        return this.typeDescriptor.initialization;
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

    /**
     * This class contains informations about the array to create
     */
    public static class TypeDescriptor {
        /**
         * The array dimension sizes
         */
        List sizes;

        /**
         * The array dimension
         */
        int dimension;

        /**
         * The initialization expression
         */
        ArrayInitializer initialization;

        /**
         * The end line
         */
        public int endLine;

        /**
         * The end column
         */
        public int endColumn;

        /**
         * Creates a new type descriptor
         */
        public TypeDescriptor(final List sizes, final int dim, final ArrayInitializer init, final int el, final int ec) {
            this.sizes = sizes;
            this.dimension = dim;
            this.initialization = init;
            this.endLine = el;
            this.endColumn = ec;
        }

        /**
         * Initializes the type descriptor
         */
        void initialize(final Type t) {
            if (this.initialization != null) {
                final Type et = this.dimension > 1 ? new ArrayType(t, this.dimension, t.getFilename(), t.getBeginLine(), t.getBeginColumn(), this.endLine, this.endColumn) : t;
                this.initialization.setElementType(et);
            }
        }
    }
}
