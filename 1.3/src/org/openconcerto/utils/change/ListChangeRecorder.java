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
 
 package org.openconcerto.utils.change;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A class that wraps a list, to detect every change made to it. The changes are available with
 * {@link #getRecipe()}.
 * 
 * @author Sylvain
 * 
 * @param <E> type of items.
 */
public class ListChangeRecorder<E> extends AbstractList<E> {

    private final List<E> delegate;
    private final ListChangeRecipe<E> recipe;

    public ListChangeRecorder(List<E> delegate) {
        this(delegate, false);
    }

    public ListChangeRecorder(List<E> delegate, final boolean keepHistory) {
        super();
        this.delegate = delegate;
        this.recipe = new ListChangeRecipe<E>(keepHistory);
    }

    public ListChangeRecipe<E> getRecipe() {
        return this.recipe;
    }

    // inherit from AbstractList to gain iterators & sublist
    // they only call regular methods of this class, so every change made through them is recorded
    // overload a maximum of methods to keep the original behaviour/optimization of our delegate

    // ** read only

    public E get(int index) {
        return this.delegate.get(index);
    }

    public int size() {
        return this.delegate.size();
    }

    public Object[] toArray() {
        return this.delegate.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return this.delegate.toArray(a);
    }

    public boolean contains(Object o) {
        return this.delegate.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return this.delegate.containsAll(c);
    }

    public boolean equals(Object o) {
        return this.delegate.equals(o);
    }

    public int hashCode() {
        return this.delegate.hashCode();
    }

    public int indexOf(Object o) {
        return this.delegate.indexOf(o);
    }

    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    public int lastIndexOf(Object o) {
        return this.delegate.lastIndexOf(o);
    }

    // ** write, always change the delegate before notifying the recipe
    // otherwise the listeners will be told of the changes before they even happened

    public boolean add(E e) {
        final boolean res = this.delegate.add(e);
        // -1 since this just grew by one
        this.recipe.add(this.size() - 1, Collections.singleton(e));
        return res;
    }

    public void add(int index, E e) {
        this.delegate.add(index, e);
        this.recipe.add(index, Collections.singleton(e));
    }

    public boolean addAll(Collection<? extends E> c) {
        final int size = this.size();
        final boolean res = this.delegate.addAll(c);
        this.recipe.add(size, c);
        return res;
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        final boolean res = this.delegate.addAll(index, c);
        this.recipe.add(index, c);
        return res;
    }

    public void clear() {
        final List<E> copy = (List<E>) ListChangeIndex.copy(this);
        this.delegate.clear();
        this.recipe.remove(0, copy.size() - 1, copy);
    }

    public E set(int index, E element) {
        final E res = this.delegate.set(index, element);
        this.recipe.set(index, res, element);
        return res;
    }

    public E remove(int index) {
        final E res = this.delegate.remove(index);
        this.recipe.remove(index, index, Collections.singletonList(res));
        return res;
    }

    // objects

    public boolean remove(Object o) {
        final int index = this.indexOf(o);
        if (index < 0)
            return false;
        else {
            this.remove(index);
            return true;
        }
    }

    public boolean removeAll(Collection<?> c) {
        return this.changeAll(c, true);
    }

    public boolean retainAll(Collection<?> c) {
        return this.changeAll(c, false);
    }

    private boolean changeAll(Collection<?> c, boolean remove) {
        boolean modified = false;
        Iterator<?> e = iterator();
        while (e.hasNext()) {
            if (c.contains(e.next()) == remove) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

}
