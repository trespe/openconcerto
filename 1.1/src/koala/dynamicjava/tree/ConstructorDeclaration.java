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
import java.util.ListIterator;

import koala.dynamicjava.tree.visitor.Visitor;

/**
 * This class represents constructor declarations in an AST
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/05/23
 */

public class ConstructorDeclaration extends Node {
    /**
     * The accessFlags property name
     */
    public final static String ACCESS_FLAGS = "accessFlags";

    /**
     * The name property name
     */
    public final static String NAME = "name";

    /**
     * The parameters property name
     */
    public final static String PARAMETERS = "parameters";

    /**
     * The exceptions property name
     */
    public final static String EXCEPTIONS = "exceptions";

    /**
     * The statements property name
     */
    public final static String STATEMENTS = "statements";

    /**
     * The access flags
     */
    private int accessFlags;

    /**
     * The name of this constructor
     */
    private String name;

    /**
     * The parameters
     */
    private List parameters;

    /**
     * The exceptions
     */
    private List exceptions;

    /**
     * The explicit constructor invocation
     */
    private ConstructorInvocation constructorInvocation;

    /**
     * The statements
     */
    private List statements;

    /**
     * Creates a new method declaration
     * 
     * @param flags the access flags
     * @param name the name of this constructor
     * @param params the parameters list
     * @param excepts the exception list (a list of list of token)
     * @param eci the explicit constructor invocation
     * @param stmts the statements
     * @exception IllegalArgumentException if name is null or params is null or excepts is null or
     *            stmts is null
     */
    public ConstructorDeclaration(final int flags, final String name, final List params, final List excepts, final ConstructorInvocation eci, final List stmts) {
        this(flags, name, params, excepts, eci, stmts, null, 0, 0, 0, 0);
    }

    /**
     * Creates a new method declaration
     * 
     * @param flags the access flags
     * @param name the name of this constructor
     * @param params the parameters list
     * @param excepts the exception list (a list of list of token)
     * @param eci the explicit constructor invocation
     * @param stmts the statements
     * @param fn the filename
     * @param bl the begin line
     * @param bc the begin column
     * @param el the end line
     * @param ec the end column
     * @exception IllegalArgumentException if name is null or params is null or excepts is null or
     *            stmts is null
     */
    public ConstructorDeclaration(final int flags, final String name, final List params, final List excepts, final ConstructorInvocation eci, final List stmts, final String fn, final int bl,
            final int bc, final int el, final int ec) {
        super(fn, bl, bc, el, ec);

        if (name == null) {
            throw new IllegalArgumentException("name == null");
        }
        if (params == null) {
            throw new IllegalArgumentException("params == null");
        }
        if (excepts == null) {
            throw new IllegalArgumentException("excepts == null");
        }
        if (stmts == null) {
            throw new IllegalArgumentException("stmts == null");
        }

        this.accessFlags = flags;
        this.name = name;
        this.parameters = params;
        this.constructorInvocation = eci;
        this.statements = stmts;
        this.exceptions = excepts;

        final ListIterator it = excepts.listIterator();
        while (it.hasNext()) {
            it.set(TreeUtilities.listToName((List) it.next()));
        }
    }

    /**
     * Returns the access flags for this constructor
     */
    public int getAccessFlags() {
        return this.accessFlags;
    }

    /**
     * Sets the access flags for this constructor
     */
    public void setAccessFlags(final int f) {
        firePropertyChange(ACCESS_FLAGS, this.accessFlags, this.accessFlags = f);
    }

    /**
     * Returns the name of this constructor
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the constructor's name
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
     * Returns the parameters list
     */
    public List getParameters() {
        return this.parameters;
    }

    /**
     * Sets the parameters
     */
    public void setParameters(final List l) {
        firePropertyChange(PARAMETERS, this.parameters, this.parameters = l);
    }

    /**
     * Returns the list of the exception thrown by this method
     * 
     * @return a list of string
     */
    public List getExceptions() {
        return this.exceptions;
    }

    /**
     * Sets the exceptions thrown by this method
     * 
     * @param l a list of string
     * @exception IllegalArgumentException if l is null
     */
    public void setExceptions(final List l) {
        if (l == null) {
            throw new IllegalArgumentException("l == null");
        }

        firePropertyChange(EXCEPTIONS, this.exceptions, this.exceptions = l);
    }

    /**
     * The explicit constructor invocation if one or null
     */
    public ConstructorInvocation getConstructorInvocation() {
        return this.constructorInvocation;
    }

    /**
     * Sets the constructor invocation
     */
    public void setConstructorInvocation(final ConstructorInvocation ci) {
        this.constructorInvocation = ci;
    }

    /**
     * Returns the statements
     */
    public List getStatements() {
        return this.statements;
    }

    /**
     * Sets the statements
     * 
     * @exception IllegalArgumentException if l is null
     */
    public void setStatements(final List l) {
        if (l == null) {
            throw new IllegalArgumentException("l == null");
        }

        firePropertyChange(STATEMENTS, this.statements, this.statements = l);
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
