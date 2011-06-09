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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An ordered set of enums. The order of enums is not linked with their ordinal position. Eg with
 * the days of the week, we could construct this ordered set : s= {MONDAY, FRIDAY, TUESDAY}.
 * s.getNext(FRIDAY) == TUESDAY ; s.getHops(MONDAY, FRIDAY) == 1;
 * 
 * @author Sylvain
 * 
 * @param <E> the type of enum.
 */
public final class EnumOrderedSet<E extends Enum<E>> extends AbstractSet<E> {

    public static final <T extends Enum<T>> EnumOrderedSet<T> allOf(Class<T> en) {
        return new EnumOrderedSet<T>(EnumSet.allOf(en));
    }

    private final List<E> levels;
    private final Map<E, Integer> indices;

    private EnumOrderedSet(Set<E> set) {
        this.levels = new ArrayList<E>(set);
        this.indices = new HashMap<E, Integer>();
    }

    public EnumOrderedSet(EnumSet<E> set) {
        this((Set<E>) set);
    }

    public EnumOrderedSet(List<E> set) {
        this(new LinkedHashSet<E>(set));
    }

    public final E get(int i) {
        return this.levels.get(i);
    }

    private final int indexOf(E l) {
        Integer res = this.indices.get(l);
        if (res == null) {
            res = this.levels.indexOf(l);
            this.indices.put(l, res);
        }
        return res;
    }

    public final boolean contains(E l) {
        return this.indexOf(l) >= 0;
    }

    public final int getHops(E l1, E l2) {
        final int i2 = this.indexOf(l2);
        final int i1 = this.indexOf(l1);
        if (i1 < 0)
            throw new IllegalArgumentException(l1 + " is not in " + this);
        if (i2 < 0)
            throw new IllegalArgumentException(l2 + " is not in " + this);
        return i2 - i1;
    }

    public final E getPrevious(E l) {
        return this.getFrom(l, -1);
    }

    public final E getNext(E l) {
        return this.getFrom(l, 1);
    }

    /**
     * The n'th item after/before (whether offset is positive or negative) <code>l</code>.
     * 
     * @param l an item.
     * @param offset the offset, eg -1.
     * @return the item offset positions after l, <code>null</code> if it doesn't exist.
     * @throws IllegalArgumentException if <code>l</code> is not in this.
     */
    public final E getFrom(E l, int offset) {
        return this.getFrom(l, offset, false);
    }

    /**
     * The n'th item after/before (whether offset is positive or negative) <code>l</code>.
     * 
     * @param l an item.
     * @param offset the offset, eg -1.
     * @param cyclic whether to cycle, eg -1 from the first is the last.
     * @return the item offset positions after l, <code>null</code> if it doesn't exist (can't
     *         happen if cyclic is <code>true</code>).
     * @throws IllegalArgumentException if <code>l</code> is not in this.
     */
    public final E getFrom(E l, int offset, boolean cyclic) {
        final int i = this.indexOf(l);
        if (i < 0)
            throw new IllegalArgumentException(l + " is not in " + this);
        final int destIndex = cyclic ? (i + offset) % this.levels.size() : i + offset;
        try {
            return this.levels.get(destIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return this.levels.iterator();
    }

    @Override
    public int size() {
        return this.levels.size();
    }

}
