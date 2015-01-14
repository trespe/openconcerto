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

import java.util.Map;

/**
 * Null can be ambiguous, e.g. {@link Map#get(Object)}. This class allows to avoid the problem.
 * 
 * @author Sylvain
 * @param <V> type of value.
 */
public abstract class Value<V> {

    @SuppressWarnings("rawtypes")
    static private final Value NONE = new Value(false) {
        @Override
        public Object getValue() {
            throw new IllegalStateException(this.toString());
        }

        @Override
        public String toString() {
            return "No Value";
        }
    };

    /**
     * I.e. {@link Map#containsKey(Object)} is <code>false</code>.
     * 
     * @return the no value instance, i.e. {@link Value#hasValue()} <code>false</code>.
     */
    @SuppressWarnings("unchecked")
    public static <V> Value<V> getNone() {
        return NONE;
    }

    static private final class Some<V> extends Value<V> {

        private final V val;

        public Some(final V val) {
            super(true);
            this.val = val;
        }

        @Override
        public V getValue() {
            return this.val;
        }

        @Override
        public String toString() {
            return "Value <" + this.getValue() + '>';
        }
    };

    /**
     * I.e. {@link Map#containsKey(Object)} is <code>true</code>.
     * 
     * @param value e.g. the value contained in the Map, possibly <code>null</code>.
     * @return a instance with {@link #hasValue()} <code>true</code>.
     */
    public static <V> Value<V> getSome(final V value) {
        return new Value.Some<V>(value);
    }

    /**
     * Usefull if <code>null</code> value actually means none, i.e. the map cannot contain
     * <code>null</code>.
     * 
     * @param value the value.
     * @return {@link #getNone()} if <code>value</code> is <code>null</code>,
     *         {@link #getSome(Object)} otherwise.
     */
    public static <V> Value<V> fromNonNull(final V value) {
        return value == null ? Value.<V> getNone() : getSome(value);
    }

    private final boolean hasValue;

    private Value(boolean hasValue) {
        super();
        this.hasValue = hasValue;
    }

    public final boolean hasValue() {
        return this.hasValue;
    }

    public abstract V getValue();
}
