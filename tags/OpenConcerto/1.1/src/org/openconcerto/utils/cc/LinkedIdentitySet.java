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
 
 package org.openconcerto.utils.cc;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An IdentitySet maintaining insertion order.
 * 
 * @param <E> the type of elements maintained by this set
 * @see IdentityHashSet
 */
public final class LinkedIdentitySet<E> extends AbstractSet<E> implements IdentitySet<E>, Cloneable {

    private final LinkedList<E> list;

    public LinkedIdentitySet() {
        this.list = new LinkedList<E>();
    }

    public LinkedIdentitySet(Collection<? extends E> c) {
        this.list = new LinkedList<E>();
        this.addAll(c);
    }

    public final List<E> getList() {
        return Collections.unmodifiableList(this.list);
    }

    @Override
    public boolean add(E e) {
        if (this.contains(e))
            return false;
        else {
            this.list.add(e);
            return true;
        }
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        // let the super which calls add() since we don't want to build a map to use Map.addAll()
        return super.addAll(c);
    }

    @Override
    public void clear() {
        this.list.clear();
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public boolean isEmpty() {
        return this.list.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return this.list.iterator();
    }

    @Override
    public boolean contains(Object o) {
        Iterator<E> e = iterator();
        while (e.hasNext())
            if (o == e.next())
                return true;
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        // let the super which calls contains()
        return super.containsAll(c);
    }

    @Override
    public boolean remove(Object o) {
        Iterator<E> e = iterator();
        while (e.hasNext()) {
            if (o == e.next()) {
                e.remove();
                return true;
            }
        }
        return false;
    }

    /*
     * (From IdentityHashMap) Must revert from AbstractSet's impl to AbstractCollection's, as the
     * former contains an optimization that results in incorrect behavior when c is a smaller
     * "normal" (non-identity-based) Set.
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Iterator<E> i = iterator(); i.hasNext();) {
            if (c.contains(i.next())) {
                i.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // let the super which calls remove()
        return super.retainAll(c);
    }

    @Override
    public Object[] toArray() {
        return this.list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.list.toArray(a);
    }

    /**
     * Returns a shallow copy of this <tt>HashSet</tt> instance: the elements themselves are not
     * cloned.
     * 
     * @return a shallow copy of this set
     */
    public Object clone() {
        return new LinkedIdentitySet<E>(this);
    }

}
