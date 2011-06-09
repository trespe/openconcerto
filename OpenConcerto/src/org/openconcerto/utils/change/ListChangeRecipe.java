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

import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.Transformer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A list of ListChange.
 * 
 * @author Sylvain
 * 
 * @param <T> type of items.
 */
public class ListChangeRecipe<T> implements ListChange<T> {

    private final List<ListChange<T>> changes;
    private final PropertyChangeSupport supp;
    private final Map<List<?>, Pair> boundedLists;

    public ListChangeRecipe() {
        super();
        this.changes = new ArrayList<ListChange<T>>();
        this.supp = new PropertyChangeSupport(this);
        // need IdentityHashMap since List.equals() depend on its items
        // which will change
        this.boundedLists = new IdentityHashMap<List<?>, Pair>();
    }

    public List<ListChange<T>> getChanges() {
        return this.changes;
    }

    public void addListener(PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("changes", l);
    }

    public void rmListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("changes", l);
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
        this.boundedLists.put(l, new Pair<U>(l, transf));
    }

    public <U> void unbind(List<U> l) {
        this.boundedLists.remove(l);
    }

    private final void add(ListChange<T> change) {
        this.changes.add(change);
        // must change bounded lists first, otherwise listeners couldn't access them
        for (final Pair<?> p : this.boundedLists.values()) {
            p.apply(change);
        }
        this.supp.firePropertyChange("changes", null, change);
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

    public final void clear() {
        this.changes.clear();
    }

    public <U> void apply(List<U> l, ITransformer<T, U> transf) {
        for (final ListChange<T> change : this.changes) {
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
