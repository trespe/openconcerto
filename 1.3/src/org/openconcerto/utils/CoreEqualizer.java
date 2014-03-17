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

import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Tries to compare the core value of objects. E.g. {@link NumberUtils#compare(Number, Number)
 * compare numeric value} of numbers and only compare time of Calendar objects and ignore other
 * properties (e.g. {@link Calendar#setMinimalDaysInFirstWeek(int)}.
 * 
 * @author Sylvain
 */
public class CoreEqualizer {

    static private final CoreEqualizer INSTANCE = new CoreEqualizer(true);
    static private final CoreEqualizer MAP_KEYS_INSTANCE = new CoreEqualizer(false);

    static public final CoreEqualizer getInstance() {
        return INSTANCE;
    }

    static public final CoreEqualizer getMapKeysInstance() {
        return MAP_KEYS_INSTANCE;
    }

    private final boolean standardMapKeys;

    public CoreEqualizer(final boolean standardMapKeys) {
        this.standardMapKeys = standardMapKeys;
    }

    public final boolean contains(final Collection<?> coll, final Object item) {
        for (final Object o : coll)
            if (equals(o, item))
                return true;
        return false;
    }

    private final boolean equals(final Collection<?> a, final Collection<?> b) {
        if (a.size() != b.size())
            return false;
        if (a instanceof Set) {
            for (final Object o : a) {
                if (!contains(b, o))
                    return false;
            }
            return true;
        } else {
            final Iterator<?> iterA = a.iterator();
            final Iterator<?> iterB = b.iterator();
            while (iterA.hasNext()) {
                final Object aV = iterA.next();
                final Object bV = iterB.next();
                if (!equals(aV, bV))
                    return false;
            }
            return true;
        }
    }

    private final <K, V> Entry<K, V> get(final Map<K, V> map, final Object key) {
        for (final Entry<K, V> e : map.entrySet()) {
            if (equals(e.getKey(), key))
                return e;
        }
        return null;
    }

    private final boolean equals(final Map<?, ?> a, final Map<?, ?> b) {
        if (a.size() != b.size())
            return false;
        for (final Entry<?, ?> e : a.entrySet()) {
            final Object key = e.getKey();
            final Object aV = e.getValue();
            final Object bV;
            if (this.standardMapKeys) {
                if (aV == null && !b.containsKey(key))
                    return false;
                bV = b.get(key);
            } else {
                final Entry<?, ?> bEntry = get(b, key);
                if (bEntry == null)
                    return false;
                bV = bEntry.getValue();
            }
            if (!equals(aV, bV))
                return false;
        }
        return true;
    }

    public final boolean equals(final Object a, final Object b) {
        if (a == null) {
            return b == null;
        } else if (a instanceof Number) {
            return NumberUtils.areNumericallyEqual((Number) a, (Number) b);
        } else if (a instanceof Comparable) {
            @SuppressWarnings("unchecked")
            final Comparable<Object> comp = (Comparable<Object>) a;
            return CompareUtils.equalsWithCompareTo(comp, b);
        } else if (a instanceof Map) {
            return (b instanceof Map) && equals((Map<?, ?>) a, (Map<?, ?>) b);
        } else if (a instanceof Collection) {
            return (b instanceof Collection) && equals((Collection<?>) a, (Collection<?>) b);
        } else {
            return a.equals(b);
        }
    }
}
