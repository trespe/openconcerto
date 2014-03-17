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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import koala.dynamicjava.interpreter.context.Context;
import koala.dynamicjava.interpreter.error.CatchedExceptionError;
import koala.dynamicjava.interpreter.error.ExecutionError;
import koala.dynamicjava.tree.AddAssignExpression;
import koala.dynamicjava.tree.AddExpression;
import koala.dynamicjava.tree.AndExpression;
import koala.dynamicjava.tree.ArrayAccess;
import koala.dynamicjava.tree.ArrayAllocation;
import koala.dynamicjava.tree.ArrayInitializer;
import koala.dynamicjava.tree.BinaryExpression;
import koala.dynamicjava.tree.BitAndAssignExpression;
import koala.dynamicjava.tree.BitAndExpression;
import koala.dynamicjava.tree.BitOrAssignExpression;
import koala.dynamicjava.tree.BitOrExpression;
import koala.dynamicjava.tree.BlockStatement;
import koala.dynamicjava.tree.CastExpression;
import koala.dynamicjava.tree.CatchStatement;
import koala.dynamicjava.tree.ClassAllocation;
import koala.dynamicjava.tree.ClassDeclaration;
import koala.dynamicjava.tree.ComplementExpression;
import koala.dynamicjava.tree.ConditionalExpression;
import koala.dynamicjava.tree.DivideAssignExpression;
import koala.dynamicjava.tree.DivideExpression;
import koala.dynamicjava.tree.DoStatement;
import koala.dynamicjava.tree.EqualExpression;
import koala.dynamicjava.tree.ExclusiveOrAssignExpression;
import koala.dynamicjava.tree.ExclusiveOrExpression;
import koala.dynamicjava.tree.Expression;
import koala.dynamicjava.tree.ExpressionContainer;
import koala.dynamicjava.tree.ForStatement;
import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.FunctionCall;
import koala.dynamicjava.tree.GreaterExpression;
import koala.dynamicjava.tree.GreaterOrEqualExpression;
import koala.dynamicjava.tree.IdentifierToken;
import koala.dynamicjava.tree.IfThenElseStatement;
import koala.dynamicjava.tree.IfThenStatement;
import koala.dynamicjava.tree.ImportDeclaration;
import koala.dynamicjava.tree.InnerAllocation;
import koala.dynamicjava.tree.InnerClassAllocation;
import koala.dynamicjava.tree.InstanceOfExpression;
import koala.dynamicjava.tree.InterfaceDeclaration;
import koala.dynamicjava.tree.LabeledStatement;
import koala.dynamicjava.tree.LessExpression;
import koala.dynamicjava.tree.LessOrEqualExpression;
import koala.dynamicjava.tree.MinusExpression;
import koala.dynamicjava.tree.MultiplyAssignExpression;
import koala.dynamicjava.tree.MultiplyExpression;
import koala.dynamicjava.tree.Node;
import koala.dynamicjava.tree.NotEqualExpression;
import koala.dynamicjava.tree.NotExpression;
import koala.dynamicjava.tree.ObjectFieldAccess;
import koala.dynamicjava.tree.ObjectMethodCall;
import koala.dynamicjava.tree.OrExpression;
import koala.dynamicjava.tree.PackageDeclaration;
import koala.dynamicjava.tree.PlusExpression;
import koala.dynamicjava.tree.PostDecrement;
import koala.dynamicjava.tree.PostIncrement;
import koala.dynamicjava.tree.PreDecrement;
import koala.dynamicjava.tree.PreIncrement;
import koala.dynamicjava.tree.QualifiedName;
import koala.dynamicjava.tree.ReferenceType;
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
import koala.dynamicjava.tree.SuperMethodCall;
import koala.dynamicjava.tree.SwitchBlock;
import koala.dynamicjava.tree.SwitchStatement;
import koala.dynamicjava.tree.SynchronizedStatement;
import koala.dynamicjava.tree.ThisExpression;
import koala.dynamicjava.tree.ThrowStatement;
import koala.dynamicjava.tree.TreeUtilities;
import koala.dynamicjava.tree.TryStatement;
import koala.dynamicjava.tree.UnsignedShiftRightAssignExpression;
import koala.dynamicjava.tree.UnsignedShiftRightExpression;
import koala.dynamicjava.tree.VariableDeclaration;
import koala.dynamicjava.tree.WhileStatement;
import koala.dynamicjava.tree.visitor.VisitorObject;

/**
 * This tree visitor resolves the ambiguity in identifiers in a syntax tree
 * 
 * @author Stephane Hillion
 * @version 1.1 - 1999/10/18
 */

public class NameVisitor extends VisitorObject {
    /**
     * The context
     */
    private final Context context;

    /**
     * Creates a new name visitor
     * 
     * @param ctx the context
     */
    public NameVisitor(final Context ctx) {
        this.context = ctx;
    }

    /**
     * Sets the context's current package
     * 
     * @param node the node to visit
     * @return null
     */
    @Override
    public Object visit(final PackageDeclaration node) {
        this.context.setCurrentPackage(node.getName());
        return null;
    }

    /**
     * Declares the package or class importation in the context
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ImportDeclaration node) {
        if (node.isPackage()) {
            this.context.declarePackageImport(node.getName());
        } else {
            try {
                this.context.declareClassImport(node.getName());
            } catch (final ClassNotFoundException e) {
                throw new CatchedExceptionError(e, node);
            }
        }
        return null;
    }

    /**
     * Visits a WhileStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final WhileStatement node) {
        // Visits the components of this node
        Node n = node.getCondition();
        Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setCondition((Expression) o);
        }

        n = node.getBody();
        o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setBody((Node) o);
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
        // Enter a new scope
        this.context.enterScope();

        List l;
        // Check the statements
        if ((l = node.getInitialization()) != null) {
            visitList(l);
        }

        Node n;
        if ((n = node.getCondition()) != null) {
            final Object o = n.acceptVisitor(this);
            if (o != null) {
                if (o instanceof ReferenceType) {
                    throw new ExecutionError("malformed.expression", n);
                }
                node.setCondition((Expression) o);
            }
        }

        if ((l = node.getUpdate()) != null) {
            visitList(l);
        }

        n = node.getBody();
        final Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setBody((Node) o);
        }

        // Leave the current scope
        this.context.leaveScope();
        return null;
    }

    /**
     * Visits a DoStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final DoStatement node) {
        // Visits the components of this node
        Node n = node.getCondition();
        Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setCondition((Expression) o);
        }

        n = node.getBody();
        o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setBody((Node) o);
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
        // Visits the components of this node
        final Expression exp = node.getSelector();
        final Object o = exp.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", exp);
            }
            node.setSelector((Expression) o);
        }

        final Iterator it = node.getBindings().iterator();
        while (it.hasNext()) {
            ((Node) it.next()).acceptVisitor(this);
        }
        return null;
    }

    /**
     * Visits a SwitchBlock
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SwitchBlock node) {
        final Expression exp = node.getExpression();
        if (exp != null) {
            visitExpressionContainer(node);
        }
        List l;
        if ((l = node.getStatements()) != null) {
            visitList(l);
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
        final Node n = node.getStatement();
        final Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setStatement((Expression) o);
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
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a SynchronizedStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SynchronizedStatement node) {
        // Visits the component of this node
        Node n = node.getLock();
        Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setLock((Expression) o);
        }

        n = node.getBody();
        o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setBody((Node) o);
        }
        return null;
    }

    /**
     * Visits a TryStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final TryStatement node) {
        node.getTryBlock().acceptVisitor(this);
        final Iterator it = node.getCatchStatements().iterator();
        while (it.hasNext()) {
            ((Node) it.next()).acceptVisitor(this);
        }
        Node n;
        if ((n = node.getFinallyBlock()) != null) {
            n.acceptVisitor(this);
        }
        return null;
    }

    /**
     * Visits a CatchStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final CatchStatement node) {
        // Enter a new scope
        this.context.enterScope();

        node.getException().acceptVisitor(this);
        node.getBlock().acceptVisitor(this);
        // Leave the current scope
        this.context.leaveScope();
        return null;
    }

    /**
     * Visits a ReturnStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ReturnStatement node) {
        final Expression e = node.getExpression();
        if (e != null) {
            visitExpressionContainer(node);
        }
        return null;
    }

    /**
     * Visits an IfThenStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final IfThenStatement node) {
        // Visits the components of this node
        Node n = node.getCondition();
        Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setCondition((Expression) o);
        }

        n = node.getThenStatement();
        o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setThenStatement((Node) o);
        }
        return null;
    }

    /**
     * Visits an IfThenElseStatement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final IfThenElseStatement node) {
        // Visits the components of this node
        Node n = node.getCondition();
        Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setCondition((Expression) o);
        }

        n = node.getThenStatement();
        o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setThenStatement((Node) o);
        }

        n = node.getElseStatement();
        o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setElseStatement((Node) o);
        }
        return null;
    }

    /**
     * Visits a VariableDeclaration
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final VariableDeclaration node) {
        // Define the variable
        final String s = node.getName();
        if (this.context.isDefinedVariable(s)) {
            node.setProperty(NodeProperties.ERROR_STRINGS, new String[] { s });
            throw new ExecutionError("variable.redefinition", node);
        }

        if (node.isFinal()) {
            this.context.defineConstant(s, null);
        } else {
            this.context.define(s, null);
        }

        // Visit the initializer
        final Node n = node.getInitializer();
        if (n != null) {
            final Object o = n.acceptVisitor(this);
            if (o != null) {
                if (o instanceof ReferenceType) {
                    throw new ExecutionError("malformed.expression", n);
                }
                node.setInitializer((Expression) o);
            }
        }
        return null;
    }

    /**
     * Visits a SimpleAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SimpleAssignExpression node) {
        // First, visit the right expression
        final Expression right = node.getRightExpression();
        Object o = right.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("right.expression", right);
            }
            node.setRightExpression((Expression) o);
        }

        // Perhaps is this assignment a variable declaration ?
        final Expression left = node.getLeftExpression();
        if (left instanceof QualifiedName) {
            final List ids = ((QualifiedName) left).getIdentifiers();
            final String var = ((IdentifierToken) ids.get(0)).image();
            if (ids.size() == 1 && !this.context.exists(var)) {
                this.context.define(var, null);
            }
        }

        // Visit the left expression
        o = left.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("left.expression", node);
            }
            node.setLeftExpression((Expression) o);
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
        // Enter a new scope
        this.context.enterScope();

        // Visit the nested statements
        visitList(node.getStatements());

        // Leave the current scope
        this.context.leaveScope();

        return null;
    }

    /**
     * Visits an ObjectFieldAccess
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ObjectFieldAccess node) {
        // Visit the expression
        final Object o = node.getExpression().acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                return new StaticFieldAccess((ReferenceType) o, node.getFieldName(), node.getFilename(), node.getBeginLine(), node.getBeginColumn(), node.getEndLine(), node.getEndColumn());
            } else {
                node.setExpression((Expression) o);
            }
        }
        return null;
    }

    /**
     * Visits an ObjectMethodCall
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ObjectMethodCall node) {
        // Check the arguments
        final List args = node.getArguments();
        if (args != null) {
            visitList(args);
        }

        // Check the expression
        final Expression exp = node.getExpression();
        Object o;
        if (exp == null) {
            o = this.context.getDefaultQualifier(node);
        } else {
            o = exp.acceptVisitor(this);
            if (o == null) {
                return null;
            }
        }

        if (o == null) {
            return new FunctionCall(node.getMethodName(), node.getArguments(), node.getFilename(), node.getBeginLine(), node.getBeginColumn(), node.getEndLine(), node.getEndColumn());
        } else if (o instanceof ReferenceType) {
            return new StaticMethodCall((ReferenceType) o, node.getMethodName(), node.getArguments(), node.getFilename(), node.getBeginLine(), node.getBeginColumn(), node.getEndLine(), node
                    .getEndColumn());
        } else {
            node.setExpression((Expression) o);
        }
        return null;
    }

    /**
     * Visits a SuperMethodCall
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SuperMethodCall node) {
        // Check the arguments
        final List args = node.getArguments();
        if (args != null) {
            visitList(args);
        }
        return null;
    }

    /**
     * Visits a ThisExpression
     * 
     * @param node the node to visit
     * @return a qualified name or a field access
     */
    @Override
    public Object visit(final ThisExpression node) {
        return this.context.getDefaultQualifier(node, node.getClassName());
    }

    /**
     * Visits a QualifiedName
     * 
     * @param node the node to visit
     * @return a node that depends of the meaning of this name. It could be : a QualifiedName, a
     *         ReferenceType or a FieldAccess.
     */
    @Override
    public Object visit(final QualifiedName node) {
        final List ids = node.getIdentifiers();
        final IdentifierToken t = (IdentifierToken) ids.get(0);

        if (this.context.isDefined(t.image())) {
            // The name starts with a reference to a local variable,
            // end of the name is a sequence of field access
            Expression result = this.context.createName(node, t);
            final Iterator it = ids.iterator();
            it.next();

            IdentifierToken t2;
            while (it.hasNext()) {
                result = new ObjectFieldAccess(result, (t2 = (IdentifierToken) it.next()).image(), node.getFilename(), t.beginLine(), t.beginColumn(), t2.endLine(), t2.endColumn());
            }
            return result;
        }

        // The name must be, or starts with, a class name
        final List l = (List) ((LinkedList) ids).clone();
        boolean b = false;

        while (l.size() > 0) {
            final String s = TreeUtilities.listToName(l);
            boolean check = b = this.context.classExists(s);
            if (check) {
                break;
            }
            l.remove(l.size() - 1);
        }

        if (!b) {
            // It is an error if no matching class or field was found
            node.setProperty(NodeProperties.ERROR_STRINGS, new String[] { t.image() });
            throw new ExecutionError("undefined.class", node);
        }

        // Creates a ReferenceType node
        IdentifierToken t2 = (IdentifierToken) l.get(l.size() - 1);
        final ReferenceType rt = new ReferenceType(l, node.getFilename(), t.beginLine(), t.beginColumn(), t2.endLine(), t2.endColumn());

        if (l.size() != ids.size()) {
            // The end of the name is a sequence of field access
            final ListIterator it = ids.listIterator(l.size());
            Expression result = new StaticFieldAccess(rt, (t2 = (IdentifierToken) it.next()).image(), node.getFilename(), t.beginLine(), t.beginColumn(), t2.endLine(), t2.endColumn());
            while (it.hasNext()) {
                result = new ObjectFieldAccess(result, (t2 = (IdentifierToken) it.next()).image(), node.getFilename(), t.beginLine(), t.beginColumn(), t2.endLine(), t2.endColumn());
            }
            return result;
        } else {
            return rt;
        }
    }

    /**
     * Visits a SimpleAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SimpleAllocation node) {
        // Visit the arguments
        final List args = node.getArguments();

        if (args != null) {
            visitList(args);
        }
        return null;
    }

    /**
     * Visits an ArrayAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ArrayAllocation node) {
        // Do the checking of the size expressions
        visitList(node.getSizes());

        // Visits the initializer if one
        if (node.getInitialization() != null) {
            node.getInitialization().acceptVisitor(this);
        }
        return null;
    }

    /**
     * Visits a InnerAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final InnerAllocation node) {
        visitExpressionContainer(node);

        // Do the type checking of the arguments
        final List args = node.getArguments();
        if (args != null) {
            visitList(args);
        }
        return null;
    }

    /**
     * Visits a ArrayInitializer
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ArrayInitializer node) {
        visitList(node.getCells());
        return null;
    }

    /**
     * Visits an ArrayAccess
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ArrayAccess node) {
        visitExpressionContainer(node);

        // Visits the cell number expression
        final Node n = node.getCellNumber();
        final Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setCellNumber((Expression) o);
        }
        return null;
    }

    /**
     * Visits a NotExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final NotExpression node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a ComplementExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ComplementExpression node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a PlusExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PlusExpression node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a MinusExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final MinusExpression node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits an AddExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final AddExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits an AddAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final AddAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a SubtractExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SubtractExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits an SubtractAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final SubtractAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a MultiplyExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final MultiplyExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits an MultiplyAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final MultiplyAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a DivideExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final DivideExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits an DivideAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final DivideAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a RemainderExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final RemainderExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a RemainderAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final RemainderAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits an EqualExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final EqualExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a NotEqualExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final NotEqualExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a LessExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final LessExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a LessOrEqualExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final LessOrEqualExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a GreaterExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final GreaterExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a GreaterOrEqualExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final GreaterOrEqualExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a BitAndExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BitAndExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a BitAndAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BitAndAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a ExclusiveOrExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ExclusiveOrExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a ExclusiveOrAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ExclusiveOrAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a BitOrExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BitOrExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a BitOrAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final BitOrAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a ShiftLeftExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ShiftLeftExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a ShiftLeftAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ShiftLeftAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a ShiftRightExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ShiftRightExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a ShiftRightAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ShiftRightAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a UnsignedShiftRightExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final UnsignedShiftRightExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a UnsignedShiftRightAssignExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final UnsignedShiftRightAssignExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits an AndExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final AndExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits an OrExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final OrExpression node) {
        visitBinaryExpression(node);
        return null;
    }

    /**
     * Visits a InstanceOfExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final InstanceOfExpression node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a ConditionalExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ConditionalExpression node) {
        // Check each subexpression
        Node n = node.getConditionExpression();
        Object o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", n);
            }
            node.setConditionExpression((Expression) o);
        }

        n = node.getIfTrueExpression();
        o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.second.operand", n);
            }
            node.setIfTrueExpression((Expression) o);
        }

        n = node.getIfFalseExpression();
        o = n.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.third.operand", n);
            }
            node.setIfFalseExpression((Expression) o);
        }
        return null;
    }

    /**
     * Visits a PostIncrement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PostIncrement node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a PreIncrement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PreIncrement node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a PostDecrement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PostDecrement node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a PreDecrement
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final PreDecrement node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a CastExpression
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final CastExpression node) {
        visitExpressionContainer(node);
        return null;
    }

    /**
     * Visits a ClassAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ClassAllocation node) {
        final List largs = node.getArguments();
        if (largs != null) {
            visitList(largs);
        }
        return null;
    }

    /**
     * Visits an InnerClassAllocation
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final InnerClassAllocation node) {
        visitExpressionContainer(node);
        final List largs = node.getArguments();
        if (largs != null) {
            visitList(largs);
        }
        return null;
    }

    /**
     * Visits a FormalParameter
     * 
     * @param node the node to visit
     * @return the name of the parameter class
     */
    @Override
    public Object visit(final FormalParameter node) {
        if (node.isFinal()) {
            this.context.defineConstant(node.getName(), null);
        } else {
            this.context.define(node.getName(), null);
        }
        return null;
    }

    /**
     * Visits a ClassDeclaration
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final ClassDeclaration node) {
        this.context.defineClass(node);
        return null;
    }

    /**
     * Visits an InterfaceDeclaration
     * 
     * @param node the node to visit
     */
    @Override
    public Object visit(final InterfaceDeclaration node) {
        this.context.defineClass(node);
        return null;
    }

    /**
     * Visits an expression container
     */
    private void visitExpressionContainer(final ExpressionContainer node) {
        final Expression exp = node.getExpression();
        final Object o = exp.acceptVisitor(this);
        if (o != null) {
            if (o instanceof ReferenceType) {
                throw new ExecutionError("malformed.expression", exp);
            }
            node.setExpression((Expression) o);
        }
    }

    /**
     * Visits the subexpressions of a BinaryExpression
     */
    private void visitBinaryExpression(final BinaryExpression node) {
        // Visit the left expression
        Object o = node.getLeftExpression().acceptVisitor(this);
        if (o != null) {
            if (o instanceof Expression) {
                node.setLeftExpression((Expression) o);
            } else {
                throw new ExecutionError("left.operand", node);
            }
        }

        // Visit the right expression
        o = node.getRightExpression().acceptVisitor(this);
        if (o != null) {
            if (o instanceof Expression) {
                node.setRightExpression((Expression) o);
            } else {
                throw new ExecutionError("right.operand", node);
            }
        }
    }

    /**
     * Visits a list of node
     */
    private void visitList(final List l) {
        Node n;
        final ListIterator it = l.listIterator();
        while (it.hasNext()) {
            final Object o = (n = (Node) it.next()).acceptVisitor(this);
            if (o != null) {
                if (o instanceof ReferenceType) {
                    throw new ExecutionError("malformed.expression", n);
                }
                it.set(o);
            }
        }
    }
}
