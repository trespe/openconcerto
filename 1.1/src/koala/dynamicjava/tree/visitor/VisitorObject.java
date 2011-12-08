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

package koala.dynamicjava.tree.visitor;

import koala.dynamicjava.tree.AddAssignExpression;
import koala.dynamicjava.tree.AddExpression;
import koala.dynamicjava.tree.AndExpression;
import koala.dynamicjava.tree.ArrayAccess;
import koala.dynamicjava.tree.ArrayAllocation;
import koala.dynamicjava.tree.ArrayInitializer;
import koala.dynamicjava.tree.ArrayType;
import koala.dynamicjava.tree.BitAndAssignExpression;
import koala.dynamicjava.tree.BitAndExpression;
import koala.dynamicjava.tree.BitOrAssignExpression;
import koala.dynamicjava.tree.BitOrExpression;
import koala.dynamicjava.tree.BlockStatement;
import koala.dynamicjava.tree.BreakStatement;
import koala.dynamicjava.tree.CastExpression;
import koala.dynamicjava.tree.CatchStatement;
import koala.dynamicjava.tree.ClassAllocation;
import koala.dynamicjava.tree.ClassDeclaration;
import koala.dynamicjava.tree.ClassInitializer;
import koala.dynamicjava.tree.ComplementExpression;
import koala.dynamicjava.tree.ConditionalExpression;
import koala.dynamicjava.tree.ConstructorDeclaration;
import koala.dynamicjava.tree.ConstructorInvocation;
import koala.dynamicjava.tree.ContinueStatement;
import koala.dynamicjava.tree.DivideAssignExpression;
import koala.dynamicjava.tree.DivideExpression;
import koala.dynamicjava.tree.DoStatement;
import koala.dynamicjava.tree.EmptyStatement;
import koala.dynamicjava.tree.EqualExpression;
import koala.dynamicjava.tree.ExclusiveOrAssignExpression;
import koala.dynamicjava.tree.ExclusiveOrExpression;
import koala.dynamicjava.tree.FieldDeclaration;
import koala.dynamicjava.tree.ForStatement;
import koala.dynamicjava.tree.FormalParameter;
import koala.dynamicjava.tree.FunctionCall;
import koala.dynamicjava.tree.GreaterExpression;
import koala.dynamicjava.tree.GreaterOrEqualExpression;
import koala.dynamicjava.tree.IfThenElseStatement;
import koala.dynamicjava.tree.IfThenStatement;
import koala.dynamicjava.tree.ImportDeclaration;
import koala.dynamicjava.tree.InnerAllocation;
import koala.dynamicjava.tree.InnerClassAllocation;
import koala.dynamicjava.tree.InstanceInitializer;
import koala.dynamicjava.tree.InstanceOfExpression;
import koala.dynamicjava.tree.InterfaceDeclaration;
import koala.dynamicjava.tree.LabeledStatement;
import koala.dynamicjava.tree.LessExpression;
import koala.dynamicjava.tree.LessOrEqualExpression;
import koala.dynamicjava.tree.Literal;
import koala.dynamicjava.tree.MethodDeclaration;
import koala.dynamicjava.tree.MinusExpression;
import koala.dynamicjava.tree.MultiplyAssignExpression;
import koala.dynamicjava.tree.MultiplyExpression;
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
import koala.dynamicjava.tree.PrimitiveType;
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
import koala.dynamicjava.tree.SuperFieldAccess;
import koala.dynamicjava.tree.SuperMethodCall;
import koala.dynamicjava.tree.SwitchBlock;
import koala.dynamicjava.tree.SwitchStatement;
import koala.dynamicjava.tree.SynchronizedStatement;
import koala.dynamicjava.tree.ThisExpression;
import koala.dynamicjava.tree.ThrowStatement;
import koala.dynamicjava.tree.TryStatement;
import koala.dynamicjava.tree.TypeExpression;
import koala.dynamicjava.tree.UnsignedShiftRightAssignExpression;
import koala.dynamicjava.tree.UnsignedShiftRightExpression;
import koala.dynamicjava.tree.VariableDeclaration;
import koala.dynamicjava.tree.WhileStatement;

/**
 * This class implements all the methods of Visitor but do nothing (it returns null at each call to
 * 'visit'). This class exists as convenience for creating visitor objects
 * 
 * @author Stephane Hillion
 * @version 1.0 - 1999/04/24
 */

public class VisitorObject implements Visitor {
    /**
     * Visits an PackageDeclaration
     * 
     * @param node the node to visit
     */
    public Object visit(final PackageDeclaration node) {
        return null;
    }

    /**
     * Visits an ImportDeclaration
     * 
     * @param node the node to visit
     */
    public Object visit(final ImportDeclaration node) {
        return null;
    }

    /**
     * Visits an EmptyStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final EmptyStatement node) {
        return null;
    }

    /**
     * Visits a WhileStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final WhileStatement node) {
        return null;
    }

    /**
     * Visits a ForStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final ForStatement node) {
        return null;
    }

    /**
     * Visits a DoStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final DoStatement node) {
        return null;
    }

    /**
     * Visits a SwitchStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final SwitchStatement node) {
        return null;
    }

    /**
     * Visits a SwitchBlock
     * 
     * @param node the node to visit
     */
    public Object visit(final SwitchBlock node) {
        return null;
    }

    /**
     * Visits a LabeledStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final LabeledStatement node) {
        return null;
    }

    /**
     * Visits a BreakStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final BreakStatement node) {
        return null;
    }

    /**
     * Visits a TryStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final TryStatement node) {
        return null;
    }

    /**
     * Visits a CatchStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final CatchStatement node) {
        return null;
    }

    /**
     * Visits a ThrowStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final ThrowStatement node) {
        return null;
    }

    /**
     * Visits a ReturnStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final ReturnStatement node) {
        return null;
    }

    /**
     * Visits a SynchronizedStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final SynchronizedStatement node) {
        return null;
    }

    /**
     * Visits a ContinueStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final ContinueStatement node) {
        return null;
    }

    /**
     * Visits a IfThenStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final IfThenStatement node) {
        return null;
    }

    /**
     * Visits a IfThenElseStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final IfThenElseStatement node) {
        return null;
    }

    /**
     * Visits a Literal
     * 
     * @param node the node to visit
     */
    public Object visit(final Literal node) {
        return null;
    }

    /**
     * Visits a ThisExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ThisExpression node) {
        return null;
    }

    /**
     * Visits a QualifiedName
     * 
     * @param node the node to visit
     */
    public Object visit(final QualifiedName node) {
        return null;
    }

    /**
     * Visits a ObjectFieldAccess
     * 
     * @param node the node to visit
     */
    public Object visit(final ObjectFieldAccess node) {
        return null;
    }

    /**
     * Visits a StaticFieldAccess
     * 
     * @param node the node to visit
     */
    public Object visit(final StaticFieldAccess node) {
        return null;
    }

    /**
     * Visits a ArrayAccess
     * 
     * @param node the node to visit
     */
    public Object visit(final ArrayAccess node) {
        return null;
    }

    /**
     * Visits a SuperFieldAccess
     * 
     * @param node the node to visit
     */
    public Object visit(final SuperFieldAccess node) {
        return null;
    }

    /**
     * Visits a ObjectMethodCall
     * 
     * @param node the node to visit
     */
    public Object visit(final ObjectMethodCall node) {
        return null;
    }

    /**
     * Visits a FunctionCall
     * 
     * @param node the node to visit
     */
    public Object visit(final FunctionCall node) {
        return null;
    }

    /**
     * Visits a StaticMethodCall
     * 
     * @param node the node to visit
     */
    public Object visit(final StaticMethodCall node) {
        return null;
    }

    /**
     * Visits a ConstructorInvocation
     * 
     * @param node the node to visit
     */
    public Object visit(final ConstructorInvocation node) {
        return null;
    }

    /**
     * Visits a SuperMethodCall
     * 
     * @param node the node to visit
     */
    public Object visit(final SuperMethodCall node) {
        return null;
    }

    /**
     * Visits a PrimitiveType
     * 
     * @param node the node to visit
     */
    public Object visit(final PrimitiveType node) {
        return null;
    }

    /**
     * Visits a ReferenceType
     * 
     * @param node the node to visit
     */
    public Object visit(final ReferenceType node) {
        return null;
    }

    /**
     * Visits a ArrayType
     * 
     * @param node the node to visit
     */
    public Object visit(final ArrayType node) {
        return null;
    }

    /**
     * Visits a TypeExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final TypeExpression node) {
        return null;
    }

    /**
     * Visits a PostIncrement
     * 
     * @param node the node to visit
     */
    public Object visit(final PostIncrement node) {
        return null;
    }

    /**
     * Visits a PostDecrement
     * 
     * @param node the node to visit
     */
    public Object visit(final PostDecrement node) {
        return null;
    }

    /**
     * Visits a PreIncrement
     * 
     * @param node the node to visit
     */
    public Object visit(final PreIncrement node) {
        return null;
    }

    /**
     * Visits a PreDecrement
     * 
     * @param node the node to visit
     */
    public Object visit(final PreDecrement node) {
        return null;
    }

    /**
     * Visits an ArrayInitializer
     * 
     * @param node the node to visit
     */
    public Object visit(final ArrayInitializer node) {
        return null;
    }

    /**
     * Visits an ArrayAllocation
     * 
     * @param node the node to visit
     */
    public Object visit(final ArrayAllocation node) {
        return null;
    }

    /**
     * Visits an SimpleAllocation
     * 
     * @param node the node to visit
     */
    public Object visit(final SimpleAllocation node) {
        return null;
    }

    /**
     * Visits an ClassAllocation
     * 
     * @param node the node to visit
     */
    public Object visit(final ClassAllocation node) {
        return null;
    }

    /**
     * Visits an InnerAllocation
     * 
     * @param node the node to visit
     */
    public Object visit(final InnerAllocation node) {
        return null;
    }

    /**
     * Visits an InnerClassAllocation
     * 
     * @param node the node to visit
     */
    public Object visit(final InnerClassAllocation node) {
        return null;
    }

    /**
     * Visits a CastExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final CastExpression node) {
        return null;
    }

    /**
     * Visits a NotExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final NotExpression node) {
        return null;
    }

    /**
     * Visits a ComplementExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ComplementExpression node) {
        return null;
    }

    /**
     * Visits a PlusExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final PlusExpression node) {
        return null;
    }

    /**
     * Visits a MinusExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final MinusExpression node) {
        return null;
    }

    /**
     * Visits a MultiplyExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final MultiplyExpression node) {
        return null;
    }

    /**
     * Visits a DivideExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final DivideExpression node) {
        return null;
    }

    /**
     * Visits a RemainderExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final RemainderExpression node) {
        return null;
    }

    /**
     * Visits a AddExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final AddExpression node) {
        return null;
    }

    /**
     * Visits a SubtractExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final SubtractExpression node) {
        return null;
    }

    /**
     * Visits a ShiftLeftExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ShiftLeftExpression node) {
        return null;
    }

    /**
     * Visits a ShiftRightExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ShiftRightExpression node) {
        return null;
    }

    /**
     * Visits a UnsignedShiftRightExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final UnsignedShiftRightExpression node) {
        return null;
    }

    /**
     * Visits a LessExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final LessExpression node) {
        return null;
    }

    /**
     * Visits a GreaterExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final GreaterExpression node) {
        return null;
    }

    /**
     * Visits a LessOrEqualExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final LessOrEqualExpression node) {
        return null;
    }

    /**
     * Visits a GreaterOrEqualExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final GreaterOrEqualExpression node) {
        return null;
    }

    /**
     * Visits an InstanceOfExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final InstanceOfExpression node) {
        return null;
    }

    /**
     * Visits a EqualExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final EqualExpression node) {
        return null;
    }

    /**
     * Visits a NotEqualExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final NotEqualExpression node) {
        return null;
    }

    /**
     * Visits a BitAndExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final BitAndExpression node) {
        return null;
    }

    /**
     * Visits a ExclusiveOrExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ExclusiveOrExpression node) {
        return null;
    }

    /**
     * Visits a BitOrExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final BitOrExpression node) {
        return null;
    }

    /**
     * Visits a AndExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final AndExpression node) {
        return null;
    }

    /**
     * Visits a OrExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final OrExpression node) {
        return null;
    }

    /**
     * Visits a ConditionalExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ConditionalExpression node) {
        return null;
    }

    /**
     * Visits an SimpleAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final SimpleAssignExpression node) {
        return null;
    }

    /**
     * Visits an MultiplyAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final MultiplyAssignExpression node) {
        return null;
    }

    /**
     * Visits an DivideAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final DivideAssignExpression node) {
        return null;
    }

    /**
     * Visits an RemainderAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final RemainderAssignExpression node) {
        return null;
    }

    /**
     * Visits an AddAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final AddAssignExpression node) {
        return null;
    }

    /**
     * Visits an SubtractAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final SubtractAssignExpression node) {
        return null;
    }

    /**
     * Visits an ShiftLeftAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ShiftLeftAssignExpression node) {
        return null;
    }

    /**
     * Visits an ShiftRightAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ShiftRightAssignExpression node) {
        return null;
    }

    /**
     * Visits an UnsignedShiftRightAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final UnsignedShiftRightAssignExpression node) {
        return null;
    }

    /**
     * Visits a BitAndAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final BitAndAssignExpression node) {
        return null;
    }

    /**
     * Visits a ExclusiveOrAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final ExclusiveOrAssignExpression node) {
        return null;
    }

    /**
     * Visits a BitOrAssignExpression
     * 
     * @param node the node to visit
     */
    public Object visit(final BitOrAssignExpression node) {
        return null;
    }

    /**
     * Visits a BlockStatement
     * 
     * @param node the node to visit
     */
    public Object visit(final BlockStatement node) {
        return null;
    }

    /**
     * Visits a ClassDeclaration
     * 
     * @param node the node to visit
     */
    public Object visit(final ClassDeclaration node) {
        return null;
    }

    /**
     * Visits a InterfaceDeclaration
     * 
     * @param node the node to visit
     */
    public Object visit(final InterfaceDeclaration node) {
        return null;
    }

    /**
     * Visits a ConstructorDeclaration
     * 
     * @param node the node to visit
     */
    public Object visit(final ConstructorDeclaration node) {
        return null;
    }

    /**
     * Visits a MethodDeclaration
     * 
     * @param node the node to visit
     */
    public Object visit(final MethodDeclaration node) {
        return null;
    }

    /**
     * Visits a FormalParameter
     * 
     * @param node the node to visit
     */
    public Object visit(final FormalParameter node) {
        return null;
    }

    /**
     * Visits a FieldDeclaration
     * 
     * @param node the node to visit
     */
    public Object visit(final FieldDeclaration node) {
        return null;
    }

    /**
     * Visits a VariableDeclaration
     * 
     * @param node the node to visit
     */
    public Object visit(final VariableDeclaration node) {
        return null;
    }

    /**
     * Visits a ClassInitializer
     * 
     * @param node the node to visit
     */
    public Object visit(final ClassInitializer node) {
        return null;
    }

    /**
     * Visits a InstanceInitializer
     * 
     * @param node the node to visit
     */
    public Object visit(final InstanceInitializer node) {
        return null;
    }

}
