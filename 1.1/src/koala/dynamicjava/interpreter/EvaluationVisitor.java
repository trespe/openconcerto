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

package koala.dynamicjava.interpreter;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import koala.dynamicjava.interpreter.context.Context;
import koala.dynamicjava.interpreter.context.GlobalContext;
import koala.dynamicjava.interpreter.error.CatchedExceptionError;
import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.interpreter.modifier.LeftHandSideModifier;
import koala.dynamicjava.interpreter.throwable.BreakException;
import koala.dynamicjava.interpreter.throwable.ContinueException;
import koala.dynamicjava.interpreter.throwable.ReturnException;
import koala.dynamicjava.interpreter.throwable.ThrownException;
import koala.dynamicjava.tree.AddAssignExpression;
import koala.dynamicjava.tree.AddExpression;
import koala.dynamicjava.tree.AndExpression;
import koala.dynamicjava.tree.ArrayAccess;
import koala.dynamicjava.tree.ArrayAllocation;
import koala.dynamicjava.tree.ArrayInitializer;
import koala.dynamicjava.tree.BitAndAssignExpression;
import koala.dynamicjava.tree.BitAndExpression;
import koala.dynamicjava.tree.BitOrAssignExpression;
import koala.dynamicjava.tree.BitOrExpression;
import koala.dynamicjava.tree.BlockStatement;
import koala.dynamicjava.tree.BreakStatement;
import koala.dynamicjava.tree.CastExpression;
import koala.dynamicjava.tree.CatchStatement;
import koala.dynamicjava.tree.ClassAllocation;
import koala.dynamicjava.tree.ComplementExpression;
import koala.dynamicjava.tree.ConditionalExpression;
import koala.dynamicjava.tree.ContinueStatement;
import koala.dynamicjava.tree.DivideAssignExpression;
import koala.dynamicjava.tree.DivideExpression;
import koala.dynamicjava.tree.DoStatement;
import koala.dynamicjava.tree.EqualExpression;
import koala.dynamicjava.tree.ExclusiveOrAssignExpression;
import koala.dynamicjava.tree.ExclusiveOrExpression;
import koala.dynamicjava.tree.Expression;
import koala.dynamicjava.tree.ForStatement;
import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.FunctionCall;
import koala.dynamicjava.tree.GreaterExpression;
import koala.dynamicjava.tree.GreaterOrEqualExpression;
import koala.dynamicjava.tree.IfThenElseStatement;
import koala.dynamicjava.tree.IfThenStatement;
import koala.dynamicjava.tree.InnerAllocation;
import koala.dynamicjava.tree.InstanceOfExpression;
import koala.dynamicjava.tree.LabeledStatement;
import koala.dynamicjava.tree.LessExpression;
import koala.dynamicjava.tree.LessOrEqualExpression;
import koala.dynamicjava.tree.Literal;
import koala.dynamicjava.tree.MethodDeclaration;
import koala.dynamicjava.tree.MinusExpression;
import koala.dynamicjava.tree.MultiplyAssignExpression;
import koala.dynamicjava.tree.MultiplyExpression;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.NotEqualExpression;
import koala.dynamicjava.tree.NotExpression;
import koala.dynamicjava.tree.ObjectFieldAccess;
import koala.dynamicjava.tree.ObjectMethodCall;
import koala.dynamicjava.tree.OrExpression;
import koala.dynamicjava.tree.PlusExpression;
import koala.dynamicjava.tree.PostDecrement;
import koala.dynamicjava.tree.PostIncrement;
import koala.dynamicjava.tree.PreDecrement;
import koala.dynamicjava.tree.PreIncrement;
import koala.dynamicjava.tree.QualifiedName;
import koala.dynamicjava.tree.RemainderAssignExpression;
import koala.dynamicjava.tree.RemainderExpression;
import koala.dynamicjava.tree.ReturnStatement;
import koala.dynamicjava.tree.ShiftLeftAssignExpression;
import koala.dynamicjava.tree.ShiftLeftExpression;
import koala.dynamicjava.tree.ShiftRightAssignExpression;
import koala.dynamicjava.tree.ShiftRightExpression;
import koala.dynamicjava.tree.SimpleAllocation;
import koala.dynamicjava.tree.SimpleAssignExpression;
import koala.dynamicjava.tree.StaticFieldAccess;
import koala.dynamicjava.tree.StaticMethodCall;
import koala.dynamicjava.tree.SubtractAssignExpression;
import koala.dynamicjava.tree.SubtractExpression;
import koala.dynamicjava.tree.SuperFieldAccess;
import koala.dynamicjava.tree.SuperMethodCall;
import koala.dynamicjava.tree.SwitchBlock;
import koala.dynamicjava.tree.SwitchStatement;
import koala.dynamicjava.tree.SynchronizedStatement;
import koala.dynamicjava.tree.ThrowStatement;
import koala.dynamicjava.tree.TryStatement;
import koala.dynamicjava.tree.TypeExpression;
import koala.dynamicjava.tree.UnsignedShiftRightAssignExpression;
import koala.dynamicjava.tree.UnsignedShiftRightExpression;
import koala.dynamicjava.tree.VariableDeclaration;
import koala.dynamicjava.tree.WhileStatement;
import koala.dynamicjava.tree.visitor.Visitor;
import koala.dynamicjava.tree.visitor.VisitorObject;
import koala.dynamicjava.util.Constants;
import koala.dynamicjava.util.ImportationManager;

/**
 * This tree visitor evaluates each node of a syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/10/20
 */

public class EvaluationVisitor extends VisitorObject {
    /**
     * The current context
     */
    private final Context context;

    /**
     * Creates a new visitor
     * 
     * @param ctx the current context
     */
    public EvaluationVisitor(final Context ctx) {
        this.context = ctx;
    }

    /**
     * Visits a WhileStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final WhileStatement node) {
        try {
            while (((Boolean) node.getCondition().acceptVisitor(this)).booleanValue()) {
                try {
                    node.getBody().acceptVisitor(this);
                } catch (final ContinueException e) {
                    // 'continue' statement management
                    if (e.isLabeled() && !node.hasLabel(e.getLabel())) {
                        throw e;
                    }
                }
            }
        } catch (final BreakException e) {
            // 'break' statement management
            if (e.isLabeled() && !node.hasLabel(e.getLabel())) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Visits a ForStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ForStatement node) {
        try {
            final Set vars = (Set) node.getProperty(NodeProperties.VARIABLES);
            this.context.enterScope(vars);

            // Interpret the initialization expressions
            if (node.getInitialization() != null) {
                final Iterator it = node.getInitialization().iterator();
                while (it.hasNext()) {
                    ((Node) it.next()).acceptVisitor(this);
                }
            }

            // Interpret the loop
            try {
                final Expression cond = node.getCondition();
                final List update = node.getUpdate();
                while (cond == null || ((Boolean) cond.acceptVisitor(this)).booleanValue()) {
                    try {
                        node.getBody().acceptVisitor(this);
                    } catch (final ContinueException e) {
                        // 'continue' statement management
                        if (e.isLabeled() && !node.hasLabel(e.getLabel())) {
                            throw e;
                        }
                    }
                    // Interpret the update statements
                    if (update != null) {
                        final Iterator it = update.iterator();
                        while (it.hasNext()) {
                            ((Node) it.next()).acceptVisitor(this);
                        }
                    }
                }
            } catch (final BreakException e) {
                // 'break' statement management
                if (e.isLabeled() && !node.hasLabel(e.getLabel())) {
                    throw e;
                }
            }
        } finally {
            // Always leave the current scope
            this.context.leaveScope();
        }
        return null;
    }

    /**
     * Visits a DoStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final DoStatement node) {
        try {
            // Interpret the loop
            do {
                try {
                    node.getBody().acceptVisitor(this);
                } catch (final ContinueException e) {
                    // 'continue' statement management
                    if (e.isLabeled() && !node.hasLabel(e.getLabel())) {
                        throw e;
                    }
                }
            } while (((Boolean) node.getCondition().acceptVisitor(this)).booleanValue());
        } catch (final BreakException e) {
            // 'break' statement management
            if (e.isLabeled() && !node.hasLabel(e.getLabel())) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Visits a SwitchStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SwitchStatement node) {
        try {
            boolean processed = false;

            // Evaluate the choice expression
            Object o = node.getSelector().acceptVisitor(this);
            if (o instanceof Character) {
                o = new Integer(((Character) o).charValue());
            }
            final Number n = (Number) o;

            // Search for the matching label
            final ListIterator it = node.getBindings().listIterator();
            ListIterator dit = null;
            loop: while (it.hasNext()) {
                SwitchBlock sc = (SwitchBlock) it.next();
                Number l = null;
                if (sc.getExpression() != null) {
                    o = sc.getExpression().acceptVisitor(this);
                    if (o instanceof Character) {
                        o = new Integer(((Character) o).charValue());
                    }
                    l = (Number) o;
                } else {
                    dit = node.getBindings().listIterator(it.nextIndex() - 1);
                }

                if (l != null && n.intValue() == l.intValue()) {
                    processed = true;
                    // When a matching label is found, interpret all the
                    // remaining statements
                    for (;;) {
                        if (sc.getStatements() != null) {
                            final Iterator it2 = sc.getStatements().iterator();
                            while (it2.hasNext()) {
                                ((Node) it2.next()).acceptVisitor(this);
                            }
                        }
                        if (it.hasNext()) {
                            sc = (SwitchBlock) it.next();
                        } else {
                            break loop;
                        }
                    }
                }
            }

            if (!processed && dit != null) {
                SwitchBlock sc = (SwitchBlock) dit.next();
                for (;;) {
                    if (sc.getStatements() != null) {
                        final Iterator it2 = sc.getStatements().iterator();
                        while (it2.hasNext()) {
                            ((Node) it2.next()).acceptVisitor(this);
                        }
                    }
                    if (dit.hasNext()) {
                        sc = (SwitchBlock) dit.next();
                    } else {
                        break;
                    }
                }
            }
        } catch (final BreakException e) {
            // 'break' statement management
            if (e.isLabeled()) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Visits a LabeledStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final LabeledStatement node) {
        try {
            node.getStatement().acceptVisitor(this);
        } catch (final BreakException e) {
            // 'break' statement management
            if (!e.isLabeled() || !e.getLabel().equals(node.getLabel())) {
                throw e;
            }
        }
        return null;
    }

    /**
     * Visits a SynchronizedStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SynchronizedStatement node) {
        synchronized (node.getLock().acceptVisitor(this)) {
            node.getBody().acceptVisitor(this);
        }
        return null;
    }

    /**
     * Visits a BreakStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BreakStatement node) {
        throw new BreakException("unexpected.break", node.getLabel());
    }

    /**
     * Visits a ContinueStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ContinueStatement node) {
        throw new ContinueException("unexpected.continue", node.getLabel());
    }

    /**
     * Visits a TryStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final TryStatement node) {
        boolean handled = false;
        try {
            node.getTryBlock().acceptVisitor(this);
        } catch (final Throwable e) {
            Throwable t = e;
            if (e instanceof ThrownException) {
                t = ((ThrownException) e).getException();
            } else if (e instanceof CatchedExceptionError) {
                t = ((CatchedExceptionError) e).getException();
            }

            // Find the exception handler
            final Iterator it = node.getCatchStatements().iterator();
            while (it.hasNext()) {
                final CatchStatement cs = (CatchStatement) it.next();
                final Class c = NodeProperties.getType(cs.getException().getType());
                if (c.isAssignableFrom(t.getClass())) {
                    handled = true;

                    // Define the exception in a new scope
                    this.context.enterScope();
                    this.context.define(cs.getException().getName(), t);

                    // Interpret the handler
                    cs.getBlock().acceptVisitor(this);
                    break;
                }
            }

            if (!handled) {
                if (e instanceof Error) {
                    throw (Error) e;
                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new CatchedExceptionError((Exception) e, node);
                }
            }
        } finally {
            // Leave the current scope if entered
            if (handled) {
                this.context.leaveScope();
            }

            // Interpret the 'finally' block
            Node n;
            if ((n = node.getFinallyBlock()) != null) {
                n.acceptVisitor(this);
            }
        }
        return null;
    }

    /**
     * Visits a ThrowStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ThrowStatement node) {
        throw new ThrownException((Throwable) node.getExpression().acceptVisitor(this));
    }

    /**
     * Visits a ReturnStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ReturnStatement node) {
        if (node.getExpression() != null) {
            throw new ReturnException("return.statement", node.getExpression().acceptVisitor(this), node);
        } else {
            throw new ReturnException("return.statement", node);
        }
    }

    /**
     * Visits a IfThenStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final IfThenStatement node) {
        if (((Boolean) node.getCondition().acceptVisitor(this)).booleanValue()) {
            node.getThenStatement().acceptVisitor(this);
        }
        return null;
    }

    /**
     * Visits a IfThenElseStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final IfThenElseStatement node) {
        if (((Boolean) node.getCondition().acceptVisitor(this)).booleanValue()) {
            node.getThenStatement().acceptVisitor(this);
        } else {
            node.getElseStatement().acceptVisitor(this);
        }
        return null;
    }

    /**
     * Visits a BlockStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BlockStatement node) {
        try {
            // Enter a new scope and define the local variables
            final Set vars = (Set) node.getProperty(NodeProperties.VARIABLES);
            this.context.enterScope(vars);

            // Interpret the statements
            final Iterator it = node.getStatements().iterator();
            while (it.hasNext()) {
                ((Node) it.next()).acceptVisitor(this);
            }
        } finally {
            // Always leave the current scope
            this.context.leaveScope();
        }
        return null;
    }

    /**
     * Visits a Literal
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final Literal node) {
        return node.getValue();
    }

    /**
     * Visits a VariableDeclaration
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final VariableDeclaration node) {
        final Class c = NodeProperties.getType(node.getType());

        if (node.getInitializer() != null) {
            final Object o = performCast(c, node.getInitializer().acceptVisitor(this));

            if (node.isFinal()) {
                this.context.setConstant(node.getName(), o);
            } else {
                this.context.set(node.getName(), o);
            }
        } else {
            if (node.isFinal()) {
                this.context.setConstant(node.getName(), UninitializedObject.INSTANCE);
            } else {
                this.context.set(node.getName(), UninitializedObject.INSTANCE);
            }
        }
        return null;
    }

    /**
     * Visits an ObjectFieldAccess
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ObjectFieldAccess node) {
        final Class c = NodeProperties.getType(node.getExpression());

        // Evaluate the object
        final Object obj = node.getExpression().acceptVisitor(this);

        if (!c.isArray()) {
            final Field f = (Field) node.getProperty(NodeProperties.FIELD);
            try {
                return f.get(obj);
            } catch (final Exception e) {
                throw new CatchedExceptionError(e, node);
            }
        } else {
            // If the object is an array, the field must be 'length'.
            // This field is not a normal field and it is the only
            // way to get it
            return new Integer(Array.getLength(obj));
        }
    }

    /**
     * Visits an ObjectMethodCall
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ObjectMethodCall node) {
        final Expression exp = node.getExpression();

        // Evaluate the receiver first
        final Object obj = exp.acceptVisitor(this);

        if (node.hasProperty(NodeProperties.METHOD)) {
            final Method m = (Method) node.getProperty(NodeProperties.METHOD);
            final Class[] typs = m.getParameterTypes();

            final List larg = node.getArguments();
            Object[] args = Constants.EMPTY_OBJECT_ARRAY;

            // Fill the arguments
            if (larg != null) {
                args = new Object[larg.size()];
                final Iterator it = larg.iterator();
                int i = 0;
                while (it.hasNext()) {
                    final Object p = ((Expression) it.next()).acceptVisitor(this);
                    args[i] = performCast(typs[i], p);
                    i++;
                }
            }
            // Invoke the method
            try {
                return m.invoke(obj, args);
            } catch (final InvocationTargetException e) {
                if (e.getTargetException() instanceof Error) {
                    throw (Error) e.getTargetException();
                } else if (e.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException) e.getTargetException();
                }
                throw new ThrownException(e.getTargetException());
            } catch (final Exception e) {
                throw new CatchedExceptionError(e, node);
            }
        } else {
            // If the 'method' property is not set, the object must be
            // an array and the called method must be 'clone'.
            // Since the 'clone' method of an array is not a normal
            // method, the only way to invoke it is to simulate its
            // behaviour.
            final Class c = NodeProperties.getType(exp);
            final int len = Array.getLength(obj);
            final Object result = Array.newInstance(c.getComponentType(), len);
            for (int i = 0; i < len; i++) {
                Array.set(result, i, Array.get(obj, i));
            }
            return result;
        }
    }

    /**
     * Visits a StaticFieldAccess
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final StaticFieldAccess node) {
        final Field f = (Field) node.getProperty(NodeProperties.FIELD);
        try {
            return f.get(null);
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }
    }

    /**
     * Visits a SuperFieldAccess
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SuperFieldAccess node) {
        final Field f = (Field) node.getProperty(NodeProperties.FIELD);
        try {
            return f.get(this.context.getHiddenArgument());
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }
    }

    /**
     * Visits a SuperMethodCall
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SuperMethodCall node) {
        final Method m = (Method) node.getProperty(NodeProperties.METHOD);
        final List larg = node.getArguments();
        Object[] args = Constants.EMPTY_OBJECT_ARRAY;

        // Fill the arguments
        if (larg != null) {
            final Iterator it = larg.iterator();
            int i = 0;
            args = new Object[larg.size()];
            while (it.hasNext()) {
                args[i] = ((Expression) it.next()).acceptVisitor(this);
                i++;
            }
        }

        // Invoke the method
        try {
            return m.invoke(this.context.getHiddenArgument(), args);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            } else if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            }
            throw new ThrownException(e.getTargetException());
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }
    }

    /**
     * Visits a StaticMethodCall
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final StaticMethodCall node) {
        final Method m = (Method) node.getProperty(NodeProperties.METHOD);
        final List larg = node.getArguments();
        Object[] args = Constants.EMPTY_OBJECT_ARRAY;

        // Fill the arguments
        if (larg != null) {
            args = new Object[larg.size()];
            final Iterator it = larg.iterator();
            int i = 0;
            while (it.hasNext()) {
                args[i] = ((Expression) it.next()).acceptVisitor(this);
                i++;
            }
        }

        // Invoke the method
        try {
            return m.invoke(null, args);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            } else if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            }
            throw new ThrownException(e.getTargetException());
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }
    }

    /**
     * Visits a SimpleAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SimpleAssignExpression node) {
        final Node ln = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(ln);
        mod.prepare(this, this.context);
        Object val = node.getRightExpression().acceptVisitor(this);
        val = performCast(NodeProperties.getType(node), val);
        mod.modify(this.context, val);
        return val;
    }

    /**
     * Visits a QualifiedName
     * 
     * @param node the node to visit
     * @return the value of the local variable represented by this node
     */
    @Override
    public Object visit(final QualifiedName node) {
        final Object result = this.context.get(node.getRepresentation());
        if (result == UninitializedObject.INSTANCE) {
            node.setProperty(NodeProperties.ERROR_STRINGS, new String[] { node.getRepresentation() });
            throw new ExecutionError("uninitialized.variable", node);
        }
        return result;
    }

    /**
     * Visits a TypeExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final TypeExpression node) {
        return node.getProperty(NodeProperties.VALUE);
    }

    /**
     * Visits a SimpleAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SimpleAllocation node) {
        final List larg = node.getArguments();
        Object[] args = Constants.EMPTY_OBJECT_ARRAY;

        // Fill the arguments
        if (larg != null) {
            args = new Object[larg.size()];
            final Iterator it = larg.iterator();
            int i = 0;
            while (it.hasNext()) {
                args[i++] = ((Expression) it.next()).acceptVisitor(this);
            }
        }

        return this.context.invokeConstructor(node, args);
    }

    /**
     * Visits an ArrayAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ArrayAllocation node) {
        // Visits the initializer if one
        if (node.getInitialization() != null) {
            return node.getInitialization().acceptVisitor(this);
        }

        // Evaluate the size expressions
        final int[] dims = new int[node.getSizes().size()];
        final Iterator it = node.getSizes().iterator();
        int i = 0;
        while (it.hasNext()) {
            final Number n = (Number) ((Expression) it.next()).acceptVisitor(this);
            dims[i++] = n.intValue();
        }

        // Create the array
        if (node.getDimension() != dims.length) {
            Class c = NodeProperties.getComponentType(node);
            c = Array.newInstance(c, 0).getClass();
            return Array.newInstance(c, dims);
        } else {
            return Array.newInstance(NodeProperties.getComponentType(node), dims);
        }
    }

    /**
     * Visits a ArrayInitializer
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ArrayInitializer node) {
        final Object result = Array.newInstance(NodeProperties.getType(node.getElementType()), node.getCells().size());

        final Iterator it = node.getCells().iterator();
        int i = 0;
        while (it.hasNext()) {
            final Object o = ((Expression) it.next()).acceptVisitor(this);
            Array.set(result, i++, o);
        }
        return result;
    }

    /**
     * Visits an ArrayAccess
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ArrayAccess node) {
        final Object t = node.getExpression().acceptVisitor(this);
        Object o = node.getCellNumber().acceptVisitor(this);
        if (o instanceof Character) {
            o = new Integer(((Character) o).charValue());
        }
        return Array.get(t, ((Number) o).intValue());
    }

    /**
     * Visits a InnerAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final InnerAllocation node) {
        final Constructor cons = (Constructor) node.getProperty(NodeProperties.CONSTRUCTOR);
        NodeProperties.getType(node);

        final List larg = node.getArguments();
        Object[] args = null;

        if (larg != null) {
            args = new Object[larg.size() + 1];
            args[0] = node.getExpression().acceptVisitor(this);

            final Iterator it = larg.iterator();
            int i = 1;
            while (it.hasNext()) {
                args[i++] = ((Expression) it.next()).acceptVisitor(this);
            }
        } else {
            args = new Object[] { node.getExpression().acceptVisitor(this) };
        }

        // Invoke the constructor
        try {
            return cons.newInstance(args);
        } catch (final InvocationTargetException e) {
            if (e.getTargetException() instanceof Error) {
                throw (Error) e.getTargetException();
            } else if (e.getTargetException() instanceof RuntimeException) {
                throw (RuntimeException) e.getTargetException();
            }
            throw new ThrownException(e.getTargetException());
        } catch (final Exception e) {
            throw new CatchedExceptionError(e, node);
        }
    }

    /**
     * Visits a ClassAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ClassAllocation node) {
        final List larg = node.getArguments();
        Object[] args = Constants.EMPTY_OBJECT_ARRAY;

        // Fill the arguments
        if (larg != null) {
            args = new Object[larg.size()];
            final Iterator it = larg.iterator();
            int i = 0;
            while (it.hasNext()) {
                args[i++] = ((Expression) it.next()).acceptVisitor(this);
            }
        }

        return this.context.invokeConstructor(node, args);
    }

    /**
     * Visits a NotExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final NotExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Boolean b = (Boolean) node.getExpression().acceptVisitor(this);
            if (b.booleanValue()) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        }
    }

    /**
     * Visits a ComplementExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ComplementExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Class c = NodeProperties.getType(node);
            Object o = node.getExpression().acceptVisitor(this);

            if (o instanceof Character) {
                o = new Integer(((Character) o).charValue());
            }
            if (c == int.class) {
                return new Integer(~((Number) o).intValue());
            } else {
                return new Long(~((Number) o).longValue());
            }
        }
    }

    /**
     * Visits a PlusExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PlusExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.plus(NodeProperties.getType(node), node.getExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits a MinusExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final MinusExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.minus(NodeProperties.getType(node), node.getExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits a AddExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final AddExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.add(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits an AddAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final AddAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.add(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits a SubtractExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SubtractExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.subtract(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits an SubtractAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SubtractAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.subtract(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits a MultiplyExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final MultiplyExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.multiply(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits an MultiplyAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final MultiplyAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.multiply(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits a DivideExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final DivideExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.divide(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits an DivideAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final DivideAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.divide(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits a RemainderExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final RemainderExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.remainder(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits an RemainderAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final RemainderAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.remainder(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits an EqualExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final EqualExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Node ln = node.getLeftExpression();
            final Node rn = node.getRightExpression();
            return InterpreterUtilities.equalTo(NodeProperties.getType(ln), NodeProperties.getType(rn), ln.acceptVisitor(this), rn.acceptVisitor(this));
        }
    }

    /**
     * Visits a NotEqualExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final NotEqualExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Node ln = node.getLeftExpression();
            final Node rn = node.getRightExpression();
            return InterpreterUtilities.notEqualTo(NodeProperties.getType(ln), NodeProperties.getType(rn), ln.acceptVisitor(this), rn.acceptVisitor(this));
        }
    }

    /**
     * Visits a LessExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final LessExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Node ln = node.getLeftExpression();
            final Node rn = node.getRightExpression();
            return InterpreterUtilities.lessThan(ln.acceptVisitor(this), rn.acceptVisitor(this));
        }
    }

    /**
     * Visits a LessOrEqualExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final LessOrEqualExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Node ln = node.getLeftExpression();
            final Node rn = node.getRightExpression();
            return InterpreterUtilities.lessOrEqual(ln.acceptVisitor(this), rn.acceptVisitor(this));
        }
    }

    /**
     * Visits a GreaterExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final GreaterExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Node ln = node.getLeftExpression();
            final Node rn = node.getRightExpression();
            return InterpreterUtilities.greaterThan(ln.acceptVisitor(this), rn.acceptVisitor(this));
        }
    }

    /**
     * Visits a GreaterOrEqualExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final GreaterOrEqualExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Node ln = node.getLeftExpression();
            final Node rn = node.getRightExpression();
            return InterpreterUtilities.greaterOrEqual(ln.acceptVisitor(this), rn.acceptVisitor(this));
        }
    }

    /**
     * Visits a InstanceOfExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final InstanceOfExpression node) {
        final Object v = node.getExpression().acceptVisitor(this);
        final Class c = NodeProperties.getType(node.getReferenceType());

        return c.isInstance(v) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Visits a ConditionalExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ConditionalExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            final Boolean b = (Boolean) node.getConditionExpression().acceptVisitor(this);
            if (b.booleanValue()) {
                return node.getIfTrueExpression().acceptVisitor(this);
            } else {
                return node.getIfFalseExpression().acceptVisitor(this);
            }
        }
    }

    /**
     * Visits a PostIncrement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PostIncrement node) {
        final Node exp = node.getExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(exp);
        final Object v = mod.prepare(this, this.context);

        mod.modify(this.context, InterpreterUtilities.add(NodeProperties.getType(node), v, InterpreterUtilities.ONE));
        return v;
    }

    /**
     * Visits a PreIncrement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PreIncrement node) {
        final Node exp = node.getExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(exp);
        Object v = mod.prepare(this, this.context);

        mod.modify(this.context, v = InterpreterUtilities.add(NodeProperties.getType(node), v, InterpreterUtilities.ONE));
        return v;
    }

    /**
     * Visits a PostDecrement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PostDecrement node) {
        final Node exp = node.getExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(exp);
        final Object v = mod.prepare(this, this.context);

        mod.modify(this.context, InterpreterUtilities.subtract(NodeProperties.getType(node), v, InterpreterUtilities.ONE));
        return v;
    }

    /**
     * Visits a PreDecrement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PreDecrement node) {
        final Node exp = node.getExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(exp);
        Object v = mod.prepare(this, this.context);

        mod.modify(this.context, v = InterpreterUtilities.subtract(NodeProperties.getType(node), v, InterpreterUtilities.ONE));
        return v;
    }

    /**
     * Visits a CastExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final CastExpression node) {
        return performCast(NodeProperties.getType(node), node.getExpression().acceptVisitor(this));
    }

    /**
     * Visits a BitAndExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BitAndExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.bitAnd(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits a BitAndAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BitAndAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.bitAnd(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        NodeProperties.getModifier(left).modify(this.context, result);
        return result;
    }

    /**
     * Visits a ExclusiveOrExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ExclusiveOrExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.xOr(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits a ExclusiveOrAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ExclusiveOrAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.xOr(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits a BitOrExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BitOrExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.bitOr(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits a BitOrAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BitOrAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.bitOr(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits a ShiftLeftExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ShiftLeftExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.shiftLeft(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits a ShiftLeftAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ShiftLeftAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.shiftLeft(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits a ShiftRightExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ShiftRightExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.shiftRight(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits a ShiftRightAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ShiftRightAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.shiftRight(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits a UnsignedShiftRightExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final UnsignedShiftRightExpression node) {
        if (node.hasProperty(NodeProperties.VALUE)) {
            // The expression is constant
            return node.getProperty(NodeProperties.VALUE);
        } else {
            return InterpreterUtilities.unsignedShiftRight(NodeProperties.getType(node), node.getLeftExpression().acceptVisitor(this), node.getRightExpression().acceptVisitor(this));
        }
    }

    /**
     * Visits a UnsignedShiftRightAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final UnsignedShiftRightAssignExpression node) {
        final Node left = node.getLeftExpression();
        final LeftHandSideModifier mod = NodeProperties.getModifier(left);
        final Object lhs = mod.prepare(this, this.context);

        // Perform the operation
        Object result = InterpreterUtilities.unsignedShiftRight(NodeProperties.getType(node), lhs, node.getRightExpression().acceptVisitor(this));

        // Cast the result
        result = performCast(NodeProperties.getType(left), result);

        // Modify the variable and return
        mod.modify(this.context, result);
        return result;
    }

    /**
     * Visits an AndExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final AndExpression node) {
        Expression exp = node.getLeftExpression();
        boolean b = ((Boolean) exp.acceptVisitor(this)).booleanValue();
        if (b) {
            exp = node.getRightExpression();
            b = ((Boolean) exp.acceptVisitor(this)).booleanValue();
            return b ? Boolean.TRUE : Boolean.FALSE;
        }
        return Boolean.FALSE;
    }

    /**
     * Visits an OrExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final OrExpression node) {
        Expression exp = node.getLeftExpression();
        boolean b = ((Boolean) exp.acceptVisitor(this)).booleanValue();
        if (!b) {
            exp = node.getRightExpression();
            b = ((Boolean) exp.acceptVisitor(this)).booleanValue();
            return b ? Boolean.TRUE : Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * Visits a FunctionCall
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final FunctionCall node) {
        MethodDeclaration md;
        md = (MethodDeclaration) node.getProperty(NodeProperties.FUNCTION);

        // Enter a new scope and define the parameters as local variables
        final Context c = new GlobalContext(this.context.getInterpreter());
        if (node.getArguments() != null) {
            final Iterator it = md.getParameters().iterator();
            final Iterator it2 = node.getArguments().iterator();
            while (it.hasNext()) {
                final FormalParameter fp = (FormalParameter) it.next();
                if (fp.isFinal()) {
                    c.setConstant(fp.getName(), ((Node) it2.next()).acceptVisitor(this));
                } else {
                    c.setVariable(fp.getName(), ((Node) it2.next()).acceptVisitor(this));
                }
            }
        }

        // Do the type checking of the body if needed
        final Node body = md.getBody();
        if (!body.hasProperty("visited")) {
            body.setProperty("visited", null);
            final ImportationManager im = (ImportationManager) md.getProperty(NodeProperties.IMPORTATION_MANAGER);
            Context ctx = new GlobalContext(this.context.getInterpreter());
            ctx.setImportationManager(im);

            Visitor v = new NameVisitor(ctx);
            Iterator it = md.getParameters().iterator();
            while (it.hasNext()) {
                ((Node) it.next()).acceptVisitor(v);
            }
            body.acceptVisitor(v);

            ctx = new GlobalContext(this.context.getInterpreter());
            ctx.setImportationManager(im);
            ctx.setFunctions((List) md.getProperty(NodeProperties.FUNCTIONS));

            v = new TypeChecker(ctx);
            it = md.getParameters().iterator();
            while (it.hasNext()) {
                ((Node) it.next()).acceptVisitor(v);
            }
            body.acceptVisitor(v);
        }

        // Interpret the body of the function
        try {
            body.acceptVisitor(new EvaluationVisitor(c));
        } catch (final ReturnException e) {
            return e.getValue();
        }
        return null;
    }

    /**
     * Performs a dynamic cast. This method acts on primitive wrappers.
     * 
     * @param tc the target class
     * @param o the object to cast
     */
    private static Object performCast(final Class tc, Object o) {
        final Class ec = o != null ? o.getClass() : null;

        if (tc != ec && tc.isPrimitive() && ec != null) {
            if (tc != char.class && ec == Character.class) {
                o = new Integer(((Character) o).charValue());
            } else if (tc == byte.class) {
                o = new Byte(((Number) o).byteValue());
            } else if (tc == short.class) {
                o = new Short(((Number) o).shortValue());
            } else if (tc == int.class) {
                o = new Integer(((Number) o).intValue());
            } else if (tc == long.class) {
                o = new Long(((Number) o).longValue());
            } else if (tc == float.class) {
                o = new Float(((Number) o).floatValue());
            } else if (tc == double.class) {
                o = new Double(((Number) o).doubleValue());
            } else if (tc == char.class && ec != Character.class) {
                o = new Character((char) ((Number) o).shortValue());
            }
        }
        return o;
    }
}
