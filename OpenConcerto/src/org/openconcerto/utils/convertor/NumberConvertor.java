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
 
 package org.openconcerto.utils.convertor;

public abstract class NumberConvertor<T extends Number, U extends Number> implements ValueConvertor<T, U> {

    public static final NumberConvertor<Integer, Long> INT_TO_LONG = new NumberConvertor<Integer, Long>() {
        @Override
        public Long convert(Integer o) {
            return o.longValue();
        }

        @Override
        public Integer unconvert(Long o) {
            return o.intValue();
        }
    };

    public static final NumberConvertor<Short, Integer> SHORT_TO_INT = new NumberConvertor<Short, Integer>() {
        @Override
        public Integer convert(Short o) {
            return o.intValue();
        }

        @Override
        public Short unconvert(Integer o) {
            return o.shortValue();
        }
    };

    /**
     * Convert from one class of {@link Number} to another. Necessary since new Integer(123) isn't
     * equal to new Long(123).
     * 
     * @param <N> type of desired Number.
     * @param n the instance to convert, e.g. new Integer(123).
     * @param clazz desired class of Number, e.g. Long.class.
     * @return <code>n</code> as an instance of <code>clazz</code>, e.g. new Long(123).
     */
    public static <N extends Number> N convert(Number n, Class<N> clazz) {
        final Number res;
        if (n.getClass() == clazz) {
            res = n;
        } else if (clazz == Short.class) {
            res = n.shortValue();
        } else if (clazz == Integer.class) {
            res = n.intValue();
        } else if (clazz == Long.class) {
            res = n.longValue();
        } else if (clazz == Byte.class) {
            res = n.byteValue();
        } else if (clazz == Double.class) {
            res = n.doubleValue();
        } else if (clazz == Float.class) {
            res = n.floatValue();
        } else {
            throw new IllegalArgumentException("unknown class: " + clazz);
        }
        return clazz.cast(res);
    }
}
