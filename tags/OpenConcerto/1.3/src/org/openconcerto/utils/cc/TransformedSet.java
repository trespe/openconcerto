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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Creates a Set that translate items between itself and another set. Both sets are still usable.
 * 
 * @author Sylvain
 * 
 * @param <T> type of the original set.
 * @param <TT> type of this set.
 * @see TransformedMap
 */
public class TransformedSet<T, TT> extends AbstractSet<TT> {

    private final Set<T> set;
    private final boolean modifiable;
    private final ITransformer<T, TT> transf;
    private final ITransformer<TT, T> invTransf;

    public TransformedSet(Set<T> set, ITransformer<T, TT> transf, ITransformer<TT, T> invTransf) {
        this(set, transf, invTransf, true);
    }

    public TransformedSet(Set<T> set, ITransformer<T, TT> transf, ITransformer<TT, T> invTransf, final boolean modifiable) {
        super();
        if (transf == null)
            throw new NullPointerException("null transformer");
        this.set = set;
        this.modifiable = modifiable;
        this.transf = transf;
        this.invTransf = invTransf;
        if (this.modifiable && this.invTransf == null)
            throw new IllegalArgumentException("Inverse transformation needed for mutable set");
    }

    private void checkModifiable() {
        if (!this.modifiable)
            throw new IllegalStateException("Unmodifiable set");
    }

    public final boolean add(TT e) {
        checkModifiable();
        return this.set.add(this.invTransf.transformChecked(e));
    }

    public boolean addAll(Collection<? extends TT> c) {
        checkModifiable();
        final List<T> toAdd = transformCollection(c);
        return this.set.addAll(toAdd);
    }

    private List<T> transformCollection(Collection<? extends TT> c) {
        final List<T> toAdd = new ArrayList<T>(c.size());
        for (final TT i : c) {
            toAdd.add(this.invTransf.transformChecked(i));
        }
        return toAdd;
    }

    public final void clear() {
        checkModifiable();
        this.set.clear();
    }

    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        // can throw ClassCastException as per the javadoc
        return this.invTransf == null ? super.contains(o) : this.set.contains(this.invTransf.transformChecked((TT) o));
    }

    @SuppressWarnings("unchecked")
    public boolean containsAll(Collection<?> c) {
        // can throw ClassCastException as per the javadoc
        return this.invTransf == null ? super.containsAll(c) : this.set.containsAll(this.transformCollection((Collection<? extends TT>) c));
    }

    public final boolean isEmpty() {
        return this.set.isEmpty();
    }

    public final Iterator<TT> iterator() {
        final Iterator<T> iter = this.set.iterator();
        return new Iterator<TT>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public TT next() {
                return TransformedSet.this.transf.transformChecked(iter.next());
            }

            @Override
            public void remove() {
                checkModifiable();
                iter.remove();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        checkModifiable();
        // can throw ClassCastException as per the javadoc
        return this.set.remove(this.invTransf.transformChecked((TT) o));
    }

    @SuppressWarnings("unchecked")
    public boolean removeAll(Collection<?> c) {
        checkModifiable();
        // can throw ClassCastException as per the javadoc
        return this.set.removeAll(this.transformCollection((Collection<? extends TT>) c));
    }

    @SuppressWarnings("unchecked")
    public boolean retainAll(Collection<?> c) {
        checkModifiable();
        // can throw ClassCastException as per the javadoc
        return this.set.retainAll(this.transformCollection((Collection<? extends TT>) c));
    }

    public final int size() {
        return this.set.size();
    }

    @SuppressWarnings("unchecked")
    public Object[] toArray() {
        final Object[] res = this.set.toArray();
        for (int i = 0; i < res.length; i++) {
            res[i] = this.transf.transformChecked((T) res[i]);
        }
        return res;
    }
}
