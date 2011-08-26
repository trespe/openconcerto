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

import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.Transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Allow to propagate ListChange to listeners and bound lists. Can also store all changes that it
 * was notified and replay them with {@link #apply(List, ITransformer)}.
 * 
 * @author Sylvain
 * 
 * @param <T> type of items.
 * @see #bind(List, ITransformer)
 * @see #addListener(IClosure)
 */
public class ListChangeRecipe<T> implements ListChange<T> {

    private final List<ListChangeIndex<T>> changes;
    // don't use PropertyChangeSupport since it isn't type safe and we're only interested in the
    // last change (and not the whole property, i.e. the whole list)
    private final List<IClosure<? super ListChangeIndex<T>>> listeners;
    private final Map<List<?>, Pair<?>> boundLists;

    /**
     * Create a new instance. Recording is only necessary for {@link #apply(List, ITransformer)}.
     * 
     * @param record <code>true</code> if all changes should be kept (this will leak memory until
     *        {@link #clear()} is called).
     */
    public ListChangeRecipe(final boolean record) {
        super();
        this.changes = record ? new ArrayList<ListChangeIndex<T>>() : null;
        this.listeners = new ArrayList<IClosure<? super ListChangeIndex<T>>>();
        // need IdentityHashMap since List.equals() depend on its items
        // which will change
        this.boundLists = new IdentityHashMap<List<?>, Pair<?>>();
    }

    public final boolean recordChanges() {
        return this.changes != null;
    }

    public final List<ListChangeIndex<T>> getChanges() {
        if (!this.recordChanges())
            throw new IllegalStateException("This instance wasn't created to record changes");
        return this.changes;
    }

    public void addListener(IClosure<? super ListChangeIndex<T>> l) {
        this.listeners.add(l);
    }

    public void rmListener(IClosure<? super ListChangeIndex<T>> l) {
        this.listeners.remove(l);
    }

    public void bind(List<T> l) {
        this.bind(l, Transformer.<T> nopTransformer());
    }

    /**
     * From now on, every change added to this will be applied immediately to <code>l</code>.
     * 
     * @param <U> type of items of <code>l</code>.
     * @param l the list to keep in sync.
     * @param transf the transformer.
     */
    public <U> void bind(List<U> l, ITransformer<T, U> transf) {
        this.boundLists.put(l, new Pair<U>(l, transf));
    }

    public <U> void unbind(List<U> l) {
        this.boundLists.remove(l);
    }

    private final void add(ListChangeIndex<T> change) {
        if (this.recordChanges())
            this.changes.add(change);
        // must change bounded lists first, otherwise listeners couldn't access them
        for (final Pair<?> p : this.boundLists.values()) {
            p.apply(change);
        }
        for (final IClosure<? super ListChangeIndex<T>> l : this.listeners)
            l.executeChecked(change);
    }

    public void add(int index0, Collection<? extends T> c) {
        this.add(new ListChangeIndex.Add<T>(index0, c));
    }

    public void remove(int index0, int index1, List<T> removed) {
        this.add(new ListChangeIndex.Rm<T>(index0, index1, removed));
    }

    public void set(int index0, T old, T newItem) {
        this.add(new ListChangeIndex.Set<T>(index0, old, newItem));
    }

    /**
     * Clear all recorded changes. In general should be called after
     * {@link #apply(List, ITransformer)}.
     */
    public final void clear() {
        this.getChanges().clear();
    }

    /**
     * Apply all changes since the last {@link #clear()}.
     * 
     * @param <U> type of list
     * @param l the list to change.
     * @param transf transform items between this and <code>l</code>.
     * @throws IllegalStateException if this instance doesn't {@link #recordChanges() record
     *         changes}.
     */
    @Override
    public <U> void apply(List<U> l, ITransformer<T, U> transf) throws IllegalStateException {
        for (final ListChange<T> change : this.getChanges()) {
            change.apply(l, transf);
        }
    }

    private final class Pair<U> {

        private final List<U> l;
        private final ITransformer<T, U> transf;

        public Pair(List<U> l, ITransformer<T, U> transf) {
            super();
            this.l = l;
            this.transf = transf;
        }

        void apply(ListChange<T> change) {
            change.apply(this.l, this.transf);
        }
    }
}
