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

import java.util.Comparator;

public final class ArrayComparator<C> implements Comparator<Object[]> {

    static public <T extends Comparable<? super T>> ArrayComparator<T> createNatural(final int i, final Class<T> clazz) {
        return new ArrayComparator<T>(i, clazz, CompareUtils.<T> naturalOrder());
    }

    private final int i;
    private final Class<C> clazz;
    private final Comparator<? super C> comp;

    /**
     * Will compare the item <code>i</code> of arrays.
     * 
     * @param i the index.
     * @param clazz the class at the index <code>i</code>, cannot be <code>null</code>.
     * @param comp the comparator to use, cannot be <code>null</code>.
     * @see #createNatural(int, Class)
     */
    public ArrayComparator(final int i, final Class<C> clazz, final Comparator<? super C> comp) {
        super();
        if (i < 0)
            throw new IllegalArgumentException("Negative index : " + i);
        this.i = i;
        if (clazz == null)
            throw new IllegalArgumentException("Null class");
        this.clazz = clazz;
        if (comp == null)
            throw new IllegalArgumentException("Null comparator");
        this.comp = comp;
    }

    @Override
    public int compare(Object[] o1, Object[] o2) {
        final C i1 = this.clazz.cast(o1[this.i]);
        final C i2 = this.clazz.cast(o2[this.i]);
        return this.comp.compare(i1, i2);
    }
}
