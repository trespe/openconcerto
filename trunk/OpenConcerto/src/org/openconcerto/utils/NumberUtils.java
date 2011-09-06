/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NumberUtils {

    /**
     * Whether <code>n</code> has a non-zero fractional part.
     * 
     * @param n a number.
     * @return <code>true</code> if there is a non-zero fractional part, e.g. <code>true</code> for
     *         1.3d and <code>false</code> for <code>new BigDecimal("1.00")</code>.
     */
    static public final boolean hasFractionalPart(Number n) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte || n instanceof BigInteger || n instanceof AtomicLong || n instanceof AtomicInteger)
            return false;

        final BigDecimal bd;
        if (n instanceof BigDecimal)
            bd = (BigDecimal) n;
        else if (n instanceof Double || n instanceof Float)
            bd = new BigDecimal(n.doubleValue());
        else
            bd = new BigDecimal(n.toString());
        return DecimalUtils.decimalDigits(bd) > 0;
    }

    static final int MAX_LONG_LENGTH = String.valueOf(Long.MAX_VALUE).length();

    static public final int intDigits(final long l) {
        final long x = Math.abs(l);
        long p = 10;
        int i = 1;
        while (x >= p && i < MAX_LONG_LENGTH) {
            p = 10 * p;
            i++;
        }
        return i;
    }

    /**
     * The number of digits of the integer part in decimal representation.
     * 
     * @param n a number, e.g. 123.45.
     * @return the number of digits of the integer part, e.g. 3.
     */
    static public final int intDigits(Number n) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte || n instanceof AtomicLong || n instanceof AtomicInteger)
            return intDigits(n.longValue());

        final BigDecimal bd;
        if (n instanceof BigDecimal)
            bd = (BigDecimal) n;
        else if (n instanceof BigInteger)
            bd = new BigDecimal((BigInteger) n);
        else if (n instanceof Double || n instanceof Float)
            bd = new BigDecimal(n.doubleValue());
        else
            bd = new BigDecimal(n.toString());
        return DecimalUtils.intDigits(bd);
    }

    /**
     * High precision divide.
     * 
     * @param n the dividend.
     * @param d the divisor.
     * @return <code>n / d</code>.
     * @see DecimalUtils#HIGH_PRECISION
     */
    static public Number divide(Number n, double d) {
        if (d == 1)
            return n;
        if (n instanceof BigDecimal) {
            return ((BigDecimal) n).divide(new BigDecimal(d), DecimalUtils.HIGH_PRECISION);
        } else if (n instanceof BigInteger) {
            return new BigDecimal((BigInteger) n).divide(new BigDecimal(d), DecimalUtils.HIGH_PRECISION);
        } else {
            return n.doubleValue() / d;
        }
    }
}
