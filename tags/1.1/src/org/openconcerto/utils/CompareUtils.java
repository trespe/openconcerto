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
 
 /*
 * Créé le 14 avr. 2005
 */
package org.openconcerto.utils;

import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * @author Sylvain CUAZ
 */
public class CompareUtils {

    /**
     * Compare 2 nombres entier avec longValue().
     * 
     * @param n1 le premier nombre.
     * @param n2 le deuxième nombre.
     * @return 0 si ==, >0 si n1>2.
     */
    public static final int compareIntNumbers(Number n1, Number n2) {
        return compareLong(n1.longValue(), n2.longValue());
    }

    static public final int compareInt(int int1, int int2) {
        if (int1 < int2)
            return -1;
        else if (int1 == int2)
            return 0;
        else
            return +1;
    }

    static public final int compareLong(long int1, long int2) {
        if (int1 < int2)
            return -1;
        else if (int1 == int2)
            return 0;
        else
            return +1;
    }

    /**
     * Renvoie un comparateur qui utilise successivement la liste passée tant que les objets sont
     * égaux.
     * 
     * @param comparators une liste de Comparator.
     * @return le Comparator demandé.
     * @param <T> type of comparator
     */
    static public final <T> Comparator<T> createComparator(final List<? extends Comparator<T>> comparators) {
        return new Comparator<T>() {
            public String toString() {
                return "CompareUtils comparator with " + comparators;
            }

            public int compare(T o1, T o2) {
                int result = 0;
                int i = 0;
                while (i < comparators.size() && result == 0) {
                    final Comparator<T> transf = comparators.get(i);
                    result = transf.compare(o1, o2);
                    i++;
                }
                return result;
            }
        };
    }

    /**
     * Compare 2 objets pouvant être <code>null</code>.
     * 
     * @param o1 the first object, can be <code>null</code>.
     * @param o2 the second object, can be <code>null</code>.
     * @return <code>true</code> if both are <code>null</code> or if o1.equals(o2).
     * @see Object#equals(Object)
     */
    static public final boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 == null || o2 == null)
            return false;
        return o1.equals(o2);
    }

    /**
     * Compare 2 objets pouvant être <code>null</code> avec compareTo(). Useful since for some
     * classes equals() is more specific than compareTo()==0, e.g. {@link BigDecimal#equals(Object)}
     * doesn't compare the numeric value but instance variables (1E2 is not equal to 100 or 100.00).
     * 
     * @param o1 the first object, can be <code>null</code>.
     * @param o2 the second object, can be <code>null</code>.
     * @return <code>true</code> if both are <code>null</code> or if o1.compareTo(o2) == 0.
     * @see Comparable#compareTo(Object)
     */
    static public final <T> boolean equalsWithCompareTo(Comparable<T> o1, T o2) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 == null || o2 == null)
            return false;
        return o1.compareTo(o2) == 0;
    }

    static public interface Equalizer<T> {
        public boolean equals(T o1, T o2);
    }

    static public final <T> boolean equals(List<T> l1, List<T> l2, Equalizer<? super T> comp) {
        return compare(l1, l2, comp, null) == null;
    }

    /**
     * Compare two lists using the provided comparator.
     * 
     * @param <T> type of items
     * @param l1 the first list.
     * @param l2 the second list.
     * @param comp how to compare each item.
     * @param toString how to dispay items, can be <code>null</code>.
     * @return <code>null</code> if the two lists are equal, otherwise a String explaining the
     *         difference.
     */
    static public final <T> String compare(List<T> l1, List<T> l2, Equalizer<? super T> comp, final ITransformer<? super T, String> toString) {
        final int size = l1.size();
        if (size != l2.size())
            return "unequal size";
        for (int i = 0; i < size; i++) {
            final T o1 = l1.get(i);
            final T o2 = l2.get(i);
            if (!comp.equals(o1, o2)) {
                final String s1 = toString == null ? String.valueOf(o1) : toString.transformChecked(o1);
                final String s2 = toString == null ? String.valueOf(o2) : toString.transformChecked(o2);
                return "unequal at " + i + ": " + s1 + " != " + s2;
            }
        }
        return null;
    }
}
