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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Types supported by {@link Preferences}.
 * 
 * @author Sylvain CUAZ
 * 
 * @param <T> java type.
 */
public abstract class PrefType<T> {
    // don't use enum since they don't support type parameters

    public static final PrefType<String> STRING_TYPE = new PrefType<String>(String.class, null, null) {
        @Override
        public void put(Preferences prefs, String prefKey, Object val) {
            prefs.put(prefKey, String.valueOf(val));
        }

        public String get(Preferences prefs, String prefKey, String def) {
            return prefs.get(prefKey, def);
        }
    };
    public static final PrefType<Boolean> BOOLEAN_TYPE = new PrefType<Boolean>(Boolean.class, Boolean.TYPE, java.lang.Boolean.FALSE) {
        @Override
        public void put(Preferences prefs, String prefKey, Object val) {
            prefs.putBoolean(prefKey, (Boolean) val);
        }

        public Boolean get(Preferences prefs, String prefKey, Boolean def) {
            return prefs.getBoolean(prefKey, def);
        }
    };
    public static final PrefType<Float> FLOAT_TYPE = new PrefType<Float>(Float.class, Float.TYPE, 0f) {
        @Override
        public void put(Preferences prefs, String prefKey, Object val) {
            prefs.putFloat(prefKey, ((Number) val).floatValue());
        }

        public Float get(Preferences prefs, String prefKey, Float def) {
            return prefs.getFloat(prefKey, def);
        }
    };
    public static final PrefType<Double> DOUBLE_TYPE = new PrefType<Double>(Double.class, Double.TYPE, 0d) {
        @Override
        public void put(Preferences prefs, String prefKey, Object val) {
            prefs.putDouble(prefKey, ((Number) val).doubleValue());
        }

        public Double get(Preferences prefs, String prefKey, Double def) {
            return prefs.getDouble(prefKey, def);
        }
    };
    public static final PrefType<Integer> INT_TYPE = new PrefType<Integer>(Integer.class, Integer.TYPE, 0) {
        @Override
        public void put(Preferences prefs, String prefKey, Object val) {
            prefs.putInt(prefKey, ((Number) val).intValue());
        }

        public Integer get(Preferences prefs, String prefKey, Integer def) {
            return prefs.getInt(prefKey, def);
        }
    };
    public static final PrefType<Long> LONG_TYPE = new PrefType<Long>(Long.class, Long.TYPE, 0l) {
        @Override
        public void put(Preferences prefs, String prefKey, Object val) {
            prefs.putLong(prefKey, ((Number) val).longValue());
        }

        public Long get(Preferences prefs, String prefKey, Long def) {
            return prefs.getLong(prefKey, def);
        }
    };

    public static final PrefType<byte[]> BYTE_ARRAY_TYPE = new PrefType<byte[]>(byte[].class, null, null) {
        @Override
        public void put(Preferences prefs, String prefKey, Object val) {
            prefs.putByteArray(prefKey, (byte[]) val);
        }

        public byte[] get(Preferences prefs, String prefKey, byte[] def) {
            return prefs.getByteArray(prefKey, def);
        }
    };

    public static final PrefType<?>[] VALUES = { STRING_TYPE, BOOLEAN_TYPE, FLOAT_TYPE, DOUBLE_TYPE, INT_TYPE, LONG_TYPE, BYTE_ARRAY_TYPE };
    public static final Set<PrefType<?>> VALUES_COLLECTION = Collections.unmodifiableSet(new HashSet<PrefType<?>>(Arrays.asList(VALUES)));

    private final Class<T> clazz, primitiveClass;
    private final T defaultValue;

    private PrefType(Class<T> clazz, Class<T> primitiveClass, T defaultValue) {
        this.clazz = clazz;
        if(this.clazz == null)
            throw new IllegalArgumentException();
        this.primitiveClass = primitiveClass;
        this.defaultValue = defaultValue;
    }

    /**
     * The non primitive type.
     * 
     * @return the non primitive type, never <code>null</code>.
     */
    public final Class<T> getTypeClass() {
        return this.clazz;
    }

    /**
     * The primitive type.
     * 
     * @return the primitive type or <code>null</code>.
     */
    public final Class<T> getPrimitiveTypeClass() {
        return this.primitiveClass;
    }

    /**
     * The default value for this type as specified by the JLS.
     * 
     * @return the default value, e.g. 0.
     * @see http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.12.5
     */
    public final T getDefaultValue() {
        return this.defaultValue;
    }

    public abstract void put(final Preferences prefs, final String prefKey, final Object val);

    public abstract T get(final Preferences prefs, final String prefKey, final T def);

    public final T get(final Preferences prefs, final String prefKey) {
        return this.get(prefs, prefKey, getDefaultValue());
    }
}
