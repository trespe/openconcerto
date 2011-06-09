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

import org.openconcerto.utils.CopyUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class ListChangeIndex<T> implements ListChange<T> {

    protected final static <T> Collection<T> copy(Collection<T> col) {
        Collection<T> res;
        try {
            // tries to keep the same class
            res = CopyUtils.copy(col);
        } catch (RuntimeException e) {
            // but this doesn't always work (see sublist())
            // so just use a plain ArrayList
            res = new ArrayList<T>(col);
        }
        return res;
    }

    private final int index0;
    private final int index1;

    public ListChangeIndex(int index0, int index1) {
        super();
        this.index0 = index0;
        this.index1 = index1;
    }

    protected final int getIndex0() {
        return this.index0;
    }

    protected final int getIndex1() {
        return this.index1;
    }

    public abstract Collection<? extends T> getItemsAdded();

    public abstract List<? extends T> getItemsRemoved();

    public static class Rm<T> extends ListChangeIndex<T> {

        private final List<T> removed;

        public Rm(int index0, int index1, final List<T> removed) {
            super(index0, index1);
            // ok to cast : either it copies it : List<T>, either it uses an arrayList which
            // implements List<T>
            this.removed = (List<T>) copy(removed);
        }

        public <U> void apply(List<U> l, ITransformer<T, U> transf) {
            // sublist exclusive
            l.subList(this.getIndex0(), this.getIndex1() + 1).clear();
        }

        public List<? extends T> getItemsAdded() {
            return Collections.emptyList();
        }

        public List<T> getItemsRemoved() {
            return this.removed;
        }

        @Override
        public String toString() {
            return "@" + this.getIndex0() + ";" + this.getIndex1() + " removed " + this.getItemsRemoved();
        }
    }

    public static class Add<T> extends ListChangeIndex<T> {

        private final Collection<? extends T> added;

        public Add(int index0, Collection<? extends T> added) {
            super(index0, index0);
            this.added = copy(added);
        }

        public <U> void apply(List<U> l, ITransformer<T, U> transf) {
            final List<U> toAdd = new ArrayList<U>();
            for (final T t : this.added) {
                toAdd.add(transf.transformChecked(t));
            }
            l.addAll(this.getIndex0(), toAdd);
        }

        public Collection<? extends T> getItemsAdded() {
            return this.added;
        }

        public List<? extends T> getItemsRemoved() {
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "@" + this.getIndex0() + " added " + this.getItemsAdded();
        }
    }

    public static class Set<T> extends ListChangeIndex<T> {

        private final T removed;
        private final T added;

        public Set(int index, T removed, T added) {
            super(index, index);
            this.removed = removed;
            this.added = added;
        }

        public <U> void apply(List<U> l, ITransformer<T, U> transf) {
            l.set(this.getIndex0(), transf.transformChecked(this.added));
        }

        public List<? extends T> getItemsAdded() {
            return Collections.singletonList(this.added);
        }

        public List<? extends T> getItemsRemoved() {
            return Collections.singletonList(this.removed);
        }

        @Override
        public String toString() {
            return "@" + this.getIndex0() + " replaced " + this.removed + " by " + this.added;
        }
    }

}
