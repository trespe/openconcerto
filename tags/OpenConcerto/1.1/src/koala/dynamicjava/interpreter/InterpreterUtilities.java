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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import koala.dynamicjava.util.AmbiguousFieldException;
import koala.dynamicjava.util.ReflectionUtilities;

/**
 * This class contains a collection of utility methods for interpretation
 * 
 * @author Stephane Hillion
 * @version 1.2 - 1999/11/21
 */

public class InterpreterUtilities {
    public final static Byte ONE = new Byte((byte) 1);

    // Equality //////////////////////////////////////////////////////////

    /**
     * Returns the value of an equal to operation
     * 
     * @param lc the class of the left operand
     * @param rc the class of the right operand
     * @param l the left operand
     * @param r the right operand
     */
    public static Object equalTo(final Class lc, final Class rc, final Object l, final Object r) {
        return equalityOperation(lc, rc, l, r, EqualToPredicate.INSTANCE);
    }

    /**
     * Returns the value of a not equal to operation
     * 
     * @param lc the class of the left operand
     * @param rc the class of the right operand
     * @param l the left operand
     * @param r the right operand
     */
    public static Object notEqualTo(final Class lc, final Class rc, final Object l, final Object r) {
        return equalityOperation(lc, rc, l, r, NotEqualToPredicate.INSTANCE);
    }

    /**
     * Returns the value of an equality operation
     * 
     * @param lc the class of the left operand
     * @param rc the class of the right operand
     * @param l the left operand
     * @param r the right operand
     * @param p the predicate to use
     */
    protected static Object equalityOperation(final Class lc, final Class rc, Object l, Object r, final BinaryPredicate p) {
        if (lc.isPrimitive()) {
            if (lc == boolean.class) {
                return p.invoke(((Boolean) l).booleanValue(), ((Boolean) r).booleanValue()) ? Boolean.TRUE : Boolean.FALSE;
            } else {
                if (lc == char.class) {
                    l = new Integer(((Character) l).charValue());
                }
                if (rc == char.class) {
                    r = new Integer(((Character) r).charValue());
                }
                return p.invoke(((Number) l).doubleValue(), ((Number) r).doubleValue()) ? Boolean.TRUE : Boolean.FALSE;
            }
        } else {
            return p.invoke(l, r) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

    /**
     * To encapsulate a boolean binary operator
     */
    protected static abstract class BinaryPredicate {
        abstract boolean invoke(boolean l, boolean r);

        abstract boolean invoke(double l, double r);

        abstract boolean invoke(Object l, Object r);
    }

    /**
     * To encapsulate ==
     */
    protected static class EqualToPredicate extends BinaryPredicate {
        final static EqualToPredicate INSTANCE = new EqualToPredicate();

        @Override
        boolean invoke(final boolean l, final boolean r) {
            return l == r;
        }

        @Override
        boolean invoke(final double l, final double r) {
            return l == r;
        }

        @Override
        boolean invoke(final Object l, final Object r) {
            return l == r;
        }
    }

    /**
     * To encapsulate !=
     */
    protected static class NotEqualToPredicate extends BinaryPredicate {
        final static NotEqualToPredicate INSTANCE = new NotEqualToPredicate();

        @Override
        boolean invoke(final boolean l, final boolean r) {
            return l != r;
        }

        @Override
        boolean invoke(final double l, final double r) {
            return l != r;
        }

        @Override
        boolean invoke(final Object l, final Object r) {
            return l != r;
        }
    }

    // Arithmetic operations /////////////////////////////////////////////

    /**
     * Returns the value of an addition
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object add(final Class c, final Object l, final Object r) {
        if (c == String.class) {
            return "" + l + r;
        }
        return binaryArithmeticOperation(c, l, r, AddOperation.INSTANCE);
    }

    /**
     * Returns the value of a subtraction
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object subtract(final Class c, final Object l, final Object r) {
        return binaryArithmeticOperation(c, l, r, SubtractOperation.INSTANCE);
    }

    /**
     * Returns the value of a product
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object multiply(final Class c, final Object l, final Object r) {
        return binaryArithmeticOperation(c, l, r, MultiplyOperation.INSTANCE);
    }

    /**
     * Returns the value of a division
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object divide(final Class c, final Object l, final Object r) {
        return binaryArithmeticOperation(c, l, r, DivideOperation.INSTANCE);
    }

    /**
     * Returns the value of remainder of a division
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object remainder(final Class c, final Object l, final Object r) {
        return binaryArithmeticOperation(c, l, r, RemainderOperation.INSTANCE);
    }

    /**
     * Returns the value of a binary arithmetic operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     * @param o the operation
     */
    protected static Object binaryArithmeticOperation(final Class c, Object l, Object r, final BinaryArithmeticOperation o) {
        if (l instanceof Character) {
            l = new Integer(((Character) l).charValue());
        }
        if (r instanceof Character) {
            r = new Integer(((Character) r).charValue());
        }
        if (c == int.class) {
            return new Integer(o.invoke(((Number) l).intValue(), ((Number) r).intValue()));
        } else if (c == long.class) {
            return new Long(o.invoke(((Number) l).longValue(), ((Number) r).longValue()));
        } else if (c == float.class) {
            return new Float(o.invoke(((Number) l).floatValue(), ((Number) r).floatValue()));
        } else {
            return new Double(o.invoke(((Number) l).doubleValue(), ((Number) r).doubleValue()));
        }
    }

    /**
     * To encapsulate a binary operator
     */
    protected static abstract class BinaryArithmeticOperation {
        abstract int invoke(int l, int r);

        abstract long invoke(long l, long r);

        abstract float invoke(float l, float r);

        abstract double invoke(double l, double r);
    }

    /**
     * To encapsulate +
     */
    protected static class AddOperation extends BinaryArithmeticOperation {
        final static AddOperation INSTANCE = new AddOperation();

        @Override
        int invoke(final int l, final int r) {
            return l + r;
        }

        @Override
        long invoke(final long l, final long r) {
            return l + r;
        }

        @Override
        float invoke(final float l, final float r) {
            return l + r;
        }

        @Override
        double invoke(final double l, final double r) {
            return l + r;
        }
    }

    /**
     * To encapsulate -
     */
    protected static class SubtractOperation extends BinaryArithmeticOperation {
        final static SubtractOperation INSTANCE = new SubtractOperation();

        @Override
        int invoke(final int l, final int r) {
            return l - r;
        }

        @Override
        long invoke(final long l, final long r) {
            return l - r;
        }

        @Override
        float invoke(final float l, final float r) {
            return l - r;
        }

        @Override
        double invoke(final double l, final double r) {
            return l - r;
        }
    }

    /**
     * To encapsulate *
     */
    protected static class MultiplyOperation extends BinaryArithmeticOperation {
        final static MultiplyOperation INSTANCE = new MultiplyOperation();

        @Override
        int invoke(final int l, final int r) {
            return l * r;
        }

        @Override
        long invoke(final long l, final long r) {
            return l * r;
        }

        @Override
        float invoke(final float l, final float r) {
            return l * r;
        }

        @Override
        double invoke(final double l, final double r) {
            return l * r;
        }
    }

    /**
     * To encapsulate /
     */
    protected static class DivideOperation extends BinaryArithmeticOperation {
        final static DivideOperation INSTANCE = new DivideOperation();

        @Override
        int invoke(final int l, final int r) {
            return l / r;
        }

        @Override
        long invoke(final long l, final long r) {
            return l / r;
        }

        @Override
        float invoke(final float l, final float r) {
            return l / r;
        }

        @Override
        double invoke(final double l, final double r) {
            return l / r;
        }
    }

    /**
     * To encapsulate %
     */
    protected static class RemainderOperation extends BinaryArithmeticOperation {
        final static RemainderOperation INSTANCE = new RemainderOperation();

        @Override
        int invoke(final int l, final int r) {
            return l % r;
        }

        @Override
        long invoke(final long l, final long r) {
            return l % r;
        }

        @Override
        float invoke(final float l, final float r) {
            return l % r;
        }

        @Override
        double invoke(final double l, final double r) {
            return l % r;
        }
    }

    // Relational operations /////////////////////////////////////////////

    /**
     * Returns the value of a less than operation
     * 
     * @param l the left operand
     * @param r the right operand
     */
    public static Object lessThan(final Object l, final Object r) {
        return relationalOperation(l, r, LessThanOperation.INSTANCE);
    }

    /**
     * Returns the value of a less or equal operation
     * 
     * @param l the left operand
     * @param r the right operand
     */
    public static Object lessOrEqual(final Object l, final Object r) {
        return relationalOperation(l, r, LessOrEqualOperation.INSTANCE);
    }

    /**
     * Returns the value of a greater than operation
     * 
     * @param l the left operand
     * @param r the right operand
     */
    public static Object greaterThan(final Object l, final Object r) {
        return relationalOperation(l, r, GreaterThanOperation.INSTANCE);
    }

    /**
     * Returns the value of a greater or equal operation
     * 
     * @param l the left operand
     * @param r the right operand
     */
    public static Object greaterOrEqual(final Object l, final Object r) {
        return relationalOperation(l, r, GreaterOrEqualOperation.INSTANCE);
    }

    /**
     * Returns the value of a relational operation
     * 
     * @param l the left operand
     * @param r the right operand
     * @param o the operation
     */
    protected static Object relationalOperation(Object l, Object r, final RelationalOperation o) {
        if (l instanceof Character) {
            l = new Integer(((Character) l).charValue());
        }
        if (r instanceof Character) {
            r = new Integer(((Character) r).charValue());
        }
        return o.invoke(((Number) l).doubleValue(), ((Number) r).doubleValue()) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * To encapsulate a relational operation
     */
    protected static abstract class RelationalOperation {
        abstract boolean invoke(double l, double r);
    }

    /**
     * To encapsulate <
     */
    protected static class LessThanOperation extends RelationalOperation {
        final static LessThanOperation INSTANCE = new LessThanOperation();

        @Override
        boolean invoke(final double l, final double r) {
            return l < r;
        }
    }

    /**
     * To encapsulate <=
     */
    protected static class LessOrEqualOperation extends RelationalOperation {
        final static LessOrEqualOperation INSTANCE = new LessOrEqualOperation();

        @Override
        boolean invoke(final double l, final double r) {
            return l <= r;
        }
    }

    /**
     * To encapsulate >
     */
    protected static class GreaterThanOperation extends RelationalOperation {
        final static GreaterThanOperation INSTANCE = new GreaterThanOperation();

        @Override
        boolean invoke(final double l, final double r) {
            return l > r;
        }
    }

    /**
     * To encapsulate >=
     */
    protected static class GreaterOrEqualOperation extends RelationalOperation {
        final static GreaterOrEqualOperation INSTANCE = new GreaterOrEqualOperation();

        @Override
        boolean invoke(final double l, final double r) {
            return l >= r;
        }
    }

    // Bitwise/Logical operators /////////////////////////////////////////

    /**
     * Returns the value of a bit and operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object bitAnd(final Class c, final Object l, final Object r) {
        return bitwiseOperation(c, l, r, BitAndOperation.INSTANCE);
    }

    /**
     * Returns the value of a xor operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object xOr(final Class c, final Object l, final Object r) {
        return bitwiseOperation(c, l, r, XOrOperation.INSTANCE);
    }

    /**
     * Returns the value of a bit or operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object bitOr(final Class c, final Object l, final Object r) {
        return bitwiseOperation(c, l, r, BitOrOperation.INSTANCE);
    }

    /**
     * Returns the value of a bitwise operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     * @param o the operation
     */
    protected static Object bitwiseOperation(final Class c, Object l, Object r, final BitwiseOperation o) {
        if (c == boolean.class) {
            return new Boolean(o.invoke(((Boolean) l).booleanValue(), ((Boolean) r).booleanValue()));
        } else {
            if (l instanceof Character) {
                l = new Integer(((Character) l).charValue());
            }
            if (r instanceof Character) {
                r = new Integer(((Character) r).charValue());
            }
            if (c == int.class) {
                return new Integer(o.invoke(((Number) l).intValue(), ((Number) r).intValue()));
            } else {
                return new Long(o.invoke(((Number) l).longValue(), ((Number) r).longValue()));
            }
        }
    }

    /**
     * To encapsulate a bitwise operator
     */
    protected static abstract class BitwiseOperation {
        abstract boolean invoke(boolean l, boolean r);

        abstract int invoke(int l, int r);

        abstract long invoke(long l, long r);
    }

    /**
     * To encapsulate &
     */
    protected static class BitAndOperation extends BitwiseOperation {
        final static BitAndOperation INSTANCE = new BitAndOperation();

        @Override
        boolean invoke(final boolean l, final boolean r) {
            return l & r;
        }

        @Override
        int invoke(final int l, final int r) {
            return l & r;
        }

        @Override
        long invoke(final long l, final long r) {
            return l & r;
        }
    }

    /**
     * To encapsulate |
     */
    protected static class BitOrOperation extends BitwiseOperation {
        final static BitOrOperation INSTANCE = new BitOrOperation();

        @Override
        boolean invoke(final boolean l, final boolean r) {
            return l | r;
        }

        @Override
        int invoke(final int l, final int r) {
            return l | r;
        }

        @Override
        long invoke(final long l, final long r) {
            return l | r;
        }
    }

    /**
     * To encapsulate ^
     */
    protected static class XOrOperation extends BitwiseOperation {
        final static XOrOperation INSTANCE = new XOrOperation();

        @Override
        boolean invoke(final boolean l, final boolean r) {
            return l ^ r;
        }

        @Override
        int invoke(final int l, final int r) {
            return l ^ r;
        }

        @Override
        long invoke(final long l, final long r) {
            return l ^ r;
        }
    }

    // Shift operations //////////////////////////////////////////////////

    /**
     * Returns the value of a shift left operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object shiftLeft(final Class c, final Object l, final Object r) {
        return shiftOperation(c, l, r, ShiftLeftOperation.INSTANCE);
    }

    /**
     * Returns the value of a shift right operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object shiftRight(final Class c, final Object l, final Object r) {
        return shiftOperation(c, l, r, ShiftRightOperation.INSTANCE);
    }

    /**
     * Returns the value of an unsigned shift right operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     */
    public static Object unsignedShiftRight(final Class c, final Object l, final Object r) {
        return shiftOperation(c, l, r, UnsignedShiftRightOperation.INSTANCE);
    }

    /**
     * Returns the value of a shift operation
     * 
     * @param c the class of the result
     * @param l the left operand
     * @param r the right operand
     * @param o the operation
     */
    protected static Object shiftOperation(final Class c, Object l, Object r, final ShiftOperation o) {
        if (l instanceof Character) {
            l = new Integer(((Character) l).charValue());
        }
        if (r instanceof Character) {
            r = new Integer(((Character) r).charValue());
        }
        if (c == int.class) {
            return new Integer(o.invoke(((Number) l).intValue(), ((Number) r).intValue()));
        } else {
            return new Long(o.invoke(((Number) l).longValue(), ((Number) r).intValue()));
        }
    }

    /**
     * To encapsulate a shift operator
     */
    protected static abstract class ShiftOperation {
        abstract int invoke(int l, int r);

        abstract long invoke(long l, int r);
    }

    /**
     * To encapsulate <<
     */
    protected static class ShiftLeftOperation extends ShiftOperation {
        final static ShiftLeftOperation INSTANCE = new ShiftLeftOperation();

        @Override
        int invoke(final int l, final int r) {
            return l << r;
        }

        @Override
        long invoke(final long l, final int r) {
            return l << r;
        }
    }

    /**
     * To encapsulate >>
     */
    protected static class ShiftRightOperation extends ShiftOperation {
        final static ShiftRightOperation INSTANCE = new ShiftRightOperation();

        @Override
        int invoke(final int l, final int r) {
            return l >> r;
        }

        @Override
        long invoke(final long l, final int r) {
            return l >> r;
        }
    }

    /**
     * To encapsulate >>>
     */
    protected static class UnsignedShiftRightOperation extends ShiftOperation {
        final static UnsignedShiftRightOperation INSTANCE = new UnsignedShiftRightOperation();

        @Override
        int invoke(final int l, final int r) {
            return l >>> r;
        }

        @Override
        long invoke(final long l, final int r) {
            return l >>> r;
        }
    }

    // Unary operations //////////////////////////////////////////////////

    /**
     * Returns the value of an unary + operation
     * 
     * @param c the class of the result
     * @param o the operand
     */
    public static Object plus(final Class c, final Object o) {
        return unaryOperation(c, o, PlusOperation.INSTANCE);
    }

    /**
     * Returns the value of an unary - operation
     * 
     * @param c the class of the result
     * @param o the operand
     */
    public static Object minus(final Class c, final Object o) {
        return unaryOperation(c, o, MinusOperation.INSTANCE);
    }

    /**
     * Returns the value of an unary operation
     * 
     * @param c the class of the result
     * @param o the operand
     * @param u the operation
     */
    public static Object unaryOperation(final Class c, Object o, final UnaryOperation u) {
        if (o instanceof Character) {
            o = new Integer(((Character) o).charValue());
        }
        if (c == int.class) {
            return new Integer(u.invoke(((Number) o).intValue()));
        } else if (c == long.class) {
            return new Long(u.invoke(((Number) o).longValue()));
        } else if (c == float.class) {
            return new Float(u.invoke(((Number) o).floatValue()));
        } else {
            return new Double(u.invoke(((Number) o).doubleValue()));
        }
    }

    /**
     * To encapsulate an unary operator
     */
    protected static abstract class UnaryOperation {
        abstract int invoke(int o);

        abstract long invoke(long o);

        abstract float invoke(float o);

        abstract double invoke(double o);
    }

    /**
     * To encapulate +
     */
    protected static class PlusOperation extends UnaryOperation {
        final static PlusOperation INSTANCE = new PlusOperation();

        @Override
        int invoke(final int o) {
            return +o;
        }

        @Override
        long invoke(final long o) {
            return +o;
        }

        @Override
        float invoke(final float o) {
            return +o;
        }

        @Override
        double invoke(final double o) {
            return +o;
        }
    }

    /**
     * To encapulate -
     */
    protected static class MinusOperation extends UnaryOperation {
        final static MinusOperation INSTANCE = new MinusOperation();

        @Override
        int invoke(final int o) {
            return -o;
        }

        @Override
        long invoke(final long o) {
            return -o;
        }

        @Override
        float invoke(final float o) {
            return -o;
        }

        @Override
        double invoke(final double o) {
            return -o;
        }
    }

    // Miscellaneous //////////////////////////////////////////////////////

    /**
     * Returns the declaring class of the given class
     */
    public static Class getDeclaringClass(final Class c) {
        Class result = c.getDeclaringClass();
        if (result == null) {
            try {
                final Field f = c.getField("declaring$Class$Reference$0");
                result = (Class) f.get(null);
            } catch (final Exception e) {
            }
        }
        return result;
    }

    /**
     * Returns a field with the given name declared in one of the outer classes of the given class
     * 
     * @param cl the inner class
     * @param name the name of the field
     */
    public static Field getOuterField(final Class cl, final String name) throws NoSuchFieldException, AmbiguousFieldException {
        boolean sc = Modifier.isStatic(cl.getModifiers());
        Class c = cl != null ? getDeclaringClass(cl) : null;
        while (c != null) {
            try {
                final Field f = ReflectionUtilities.getField(c, name);
                if (!sc || Modifier.isStatic(f.getModifiers())) {
                    return f;
                }
            } catch (final NoSuchFieldException e) {
            }
            sc |= Modifier.isStatic(c.getModifiers());
            c = getDeclaringClass(c);
        }
        throw new NoSuchFieldException(name);
    }

    /**
     * Looks up for a method in an outer classes of this class.
     * 
     * @param cl the inner class
     * @param name the name of the method
     * @param ac the arguments classes (possibly not the exact declaring classes)
     */
    public static Method lookupOuterMethod(final Class cl, final String name, final Class[] ac) throws NoSuchMethodException {
        boolean sc = Modifier.isStatic(cl.getModifiers());
        Class c = cl != null ? getDeclaringClass(cl) : null;
        while (c != null) {
            try {
                final Method m = ReflectionUtilities.lookupMethod(c, name, ac);
                if (!sc || Modifier.isStatic(m.getModifiers())) {
                    return m;
                }
            } catch (final NoSuchMethodException e) {
            }
            sc |= Modifier.isStatic(c.getModifiers());
            c = getDeclaringClass(c);
        }
        throw new NoSuchMethodException(name);
    }

    public static boolean isValidAssignment(final Class lc, final Object val) {
        final Class rc = val == null ? null : val.getClass();
        if (lc != null) {
            if (lc.isPrimitive()) {
                if (lc == boolean.class && rc != Boolean.class) {
                    return false;
                } else if (lc == byte.class && rc != Byte.class) {
                    if (rc == Integer.class) {
                        final Number n = (Number) val;
                        return n.intValue() == n.byteValue();
                    }
                    return false;
                } else if ((lc == short.class || rc == Character.class) && rc != Byte.class && rc != Short.class && rc != Character.class) {
                    if (rc == Integer.class) {
                        final Number n = (Number) val;
                        return n.intValue() == n.shortValue();
                    }
                    return false;
                } else if (lc == int.class && rc != Byte.class && rc != Short.class && rc != Character.class && rc != Integer.class) {
                    return false;
                } else if (lc == long.class && rc != Byte.class && rc != Short.class && rc != Character.class && rc != Integer.class && rc != Long.class) {
                    return false;
                } else if (lc == float.class && rc != Byte.class && rc != Short.class && rc != Character.class && rc != Integer.class && rc != Long.class && rc != Float.class) {
                    return false;
                } else if (lc == double.class && rc != Byte.class && rc != Short.class && rc != Character.class && rc != Integer.class && rc != Long.class && rc != Float.class && rc != Double.class) {
                    return false;
                }
            } else if (rc != null) {
                if (!lc.isAssignableFrom(rc) && !rc.isAssignableFrom(lc)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This class contains only static methods, so it is not useful to create instances of it.
     */
    protected InterpreterUtilities() {
    }
}
