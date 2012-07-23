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

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Allow to map keys to collections. This map always allow <code>null</code> items for mapped
 * collections, but it may restrict null collections.
 * 
 * @author Sylvain
 * 
 * @param <K> the type of keys maintained by this map
 * @param <C> the type of mapped collections
 * @param <V> the type of elements of the collections
 */
public abstract class CollectionMap2<K, C extends Collection<V>, V> extends HashMap<K, C> {

    static final int DEFAULT_INITIAL_CAPACITY = 16;

    private static final String toStr(final Object o) {
        return o == null ? "null" : "'" + o + "'";
    }

    public static enum Mode {
        /**
         * Mapped collections cannot be <code>null</code>.
         */
        NULL_FORBIDDEN,
        /**
         * Mapped collections can be <code>null</code>, but some methods may throw
         * {@link NullPointerException}.
         * 
         * @see CollectionMap2#addAll(Object, Collection)
         * @see CollectionMap2#removeAll(Object, Collection)
         */
        NULL_ALLOWED,
        /**
         * Mapped collections can be <code>null</code>, meaning every possible item. Thus no method
         * throws {@link NullPointerException}.
         */
        NULL_MEANS_ALL
    }

    static private final Mode DEFAULT_MODE = Mode.NULL_FORBIDDEN;

    private final boolean emptyCollSameAsNoColl;
    private final Mode mode;

    public CollectionMap2() {
        this(DEFAULT_MODE);
    }

    public CollectionMap2(final Mode mode) {
        this(mode, null);
    }

    public CollectionMap2(final Mode mode, final Boolean emptyCollSameAsNoColl) {
        this(DEFAULT_INITIAL_CAPACITY, mode, emptyCollSameAsNoColl);
    }

    public CollectionMap2(int initialCapacity) {
        this(initialCapacity, DEFAULT_MODE, null);
    }

    public CollectionMap2(int initialCapacity, final Mode mode, final Boolean emptyCollSameAsNoColl) {
        super(initialCapacity);
        this.mode = mode;
        this.emptyCollSameAsNoColl = emptyCollSameAsNoColl == null ? mode == Mode.NULL_MEANS_ALL : emptyCollSameAsNoColl;
    }

    public CollectionMap2(Map<? extends K, ? extends Collection<? extends V>> m) {
        // don't use super(Map) since it doesn't copy the collections
        // also its type is more restrictive
        super(m.size());
        if (m instanceof CollectionMap2) {
            final CollectionMap2<?, ?, ?> collM = (CollectionMap2<?, ?, ?>) m;
            this.mode = collM.getMode();
            this.emptyCollSameAsNoColl = collM.isEmptyCollSameAsNoColl();
        } else {
            this.mode = DEFAULT_MODE;
            this.emptyCollSameAsNoColl = this.mode == Mode.NULL_MEANS_ALL;
        }
        this.putAllCollections(m);
    }

    public final Mode getMode() {
        return this.mode;
    }

    public final boolean isEmptyCollSameAsNoColl() {
        return this.emptyCollSameAsNoColl;
    }

    public final C getNonNullIfMissing(Object key) {
        return this.get(key, false, true);
    }

    public final C getNonNull(K key) {
        return this.get(key, false, false);
    }

    private final C getNonNullColl(C res) {
        return res == null ? this.createCollection(Collections.<V> emptySet()) : res;
    }

    public final C get(Object key, final boolean nullIfMissing, final boolean nullIfPresent) {
        if (nullIfMissing == nullIfPresent) {
            final C res = super.get(key);
            if (res != null || nullIfMissing && nullIfPresent) {
                return res;
            } else {
                assert !nullIfMissing && !nullIfPresent;
                return getNonNullColl(null);
            }
        } else if (nullIfMissing) {
            assert !nullIfPresent;
            if (!this.containsKey(key))
                return null;
            else
                return getNonNullColl(super.get(key));
        } else {
            assert !nullIfMissing && nullIfPresent;
            if (this.containsKey(key))
                return super.get(key);
            else
                return getNonNullColl(null);
        }
    }

    public final C getCollection(Object key) {
        return this.get(key, !this.isEmptyCollSameAsNoColl(), true);
    }

    @Override
    public Set<Map.Entry<K, C>> entrySet() {
        if (getMode() == Mode.NULL_FORBIDDEN) {
            // MAYBE cache
            return new EntrySet(super.entrySet());
        } else {
            return super.entrySet();
        }
    }

    private final class EntrySet extends AbstractCollection<Map.Entry<K, C>> implements Set<Map.Entry<K, C>> {

        private final Set<Map.Entry<K, C>> delegate;

        public EntrySet(Set<java.util.Map.Entry<K, C>> delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return this.delegate.contains(o);
        }

        @Override
        public boolean remove(Object o) {
            return this.delegate.remove(o);
        }

        @Override
        public void clear() {
            this.delegate.clear();
        }

        @Override
        public Iterator<Map.Entry<K, C>> iterator() {
            return new Iterator<Map.Entry<K, C>>() {

                private final Iterator<Map.Entry<K, C>> delegateIter = EntrySet.this.delegate.iterator();

                @Override
                public boolean hasNext() {
                    return this.delegateIter.hasNext();
                }

                @Override
                public Map.Entry<K, C> next() {
                    final Map.Entry<K, C> delegate = this.delegateIter.next();
                    return new Map.Entry<K, C>() {
                        @Override
                        public K getKey() {
                            return delegate.getKey();
                        }

                        @Override
                        public C getValue() {
                            return delegate.getValue();
                        }

                        @Override
                        public C setValue(C value) {
                            if (value == null)
                                throw new NullPointerException("Putting null collection for " + toStr(getKey()));
                            return delegate.setValue(value);
                        }
                    };
                }

                @Override
                public void remove() {
                    this.delegateIter.remove();
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            return this.delegate.equals(o);
        }

        @Override
        public int hashCode() {
            return this.delegate.hashCode();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return this.delegate.removeAll(c);
        }
    }

    @Override
    public final C put(K key, C value) {
        return this.putCollection(key, value);
    }

    // copy passed collection
    public final C putCollection(K key, Collection<? extends V> value) {
        if (value == null && this.getMode() == Mode.NULL_FORBIDDEN)
            throw new NullPointerException("Putting null collection for " + toStr(key));
        return super.put(key, value == null ? null : createCollection(value));
    }

    public void putAllCollections(Map<? extends K, ? extends Collection<? extends V>> m) {
        for (final Map.Entry<? extends K, ? extends Collection<? extends V>> e : m.entrySet()) {
            this.putCollection(e.getKey(), e.getValue());
        }
    }

    // ** add/remove collection

    public final void add(K k, V v) {
        this.addAll(k, Collections.singleton(v));
    }

    public final void addAll(K k, Collection<? extends V> v) {
        final boolean nullIsAll = getMode() == Mode.NULL_MEANS_ALL;
        if (v == null && !nullIsAll)
            throw new NullPointerException("Adding null collection for " + toStr(k));
        if (v == null || !this.containsKey(k)) {
            this.putCollection(k, v);
        } else {
            final C currentColl = this.get(k);
            if (nullIsAll && currentColl == null) {
                // ignore since we can't add something to everything
            } else {
                // will throw if currentCol is null
                currentColl.addAll(v);
            }
        }
    }

    public final void addAll(Map<? extends K, ? extends Collection<? extends V>> mm) {
        for (final Map.Entry<? extends K, ? extends Collection<? extends V>> e : mm.entrySet()) {
            this.addAll(e.getKey(), e.getValue());
        }
    }

    public final void removeAll(K k, Collection<? extends V> v) {
        this.removeAll(k, v, null);
    }

    private final void removeAll(K k, Collection<? extends V> v, final Iterator<Map.Entry<K, C>> iter) {
        boolean removeK = false;
        if (getMode() == Mode.NULL_MEANS_ALL) {
            if (v == null) {
                removeK = true;
            } else if (v.size() > 0) {
                final C currentColl = this.get(k);
                if (currentColl == null)
                    throw new IllegalStateException("Cannot remove from all for " + toStr(k));
                currentColl.removeAll(v);
                if (currentColl.isEmpty())
                    removeK = true;
            }
        } else if (this.containsKey(k)) {
            final C currentColl = this.get(k);
            if (currentColl == null && v == null) {
                // since containsKey() and coll == null
                assert getMode() == Mode.NULL_ALLOWED;
                removeK = true;
            } else {
                if (v == null)
                    throw new NullPointerException("Removing null collection for " + toStr(k));
                currentColl.removeAll(v);
                if (currentColl.isEmpty())
                    removeK = true;
            }
        }
        if (removeK)
            if (iter == null)
                this.remove(k);
            else
                iter.remove();
    }

    public final void removeAll(Map<? extends K, ? extends Collection<? extends V>> mm) {
        // iterate on this to allow mm.removeAll(mm)
        final Iterator<Map.Entry<K, C>> iter = this.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<K, C> e = iter.next();
            final K key = e.getKey();
            if (mm.containsKey(key))
                this.removeAll(key, mm.get(key), iter);
        }
    }

    // ** remove empty/null collections

    public final C removeIfEmpty(K k) {
        final C v = this.get(k);
        if (v != null && v.isEmpty())
            return this.remove(k);
        else
            return null;
    }

    public final void removeIfNull(K k) {
        if (this.get(k) == null)
            this.remove(k);
    }

    public final void removeAllEmptyCollections() {
        this.removeAll(true);
    }

    public final void removeAllNullCollections() {
        this.removeAll(false);
    }

    private final void removeAll(final boolean emptyOrNull) {
        final Iterator<Map.Entry<K, C>> iter = this.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<K, C> e = iter.next();
            final C val = e.getValue();
            if ((emptyOrNull && val != null && val.isEmpty()) || (!emptyOrNull && val == null))
                iter.remove();
        }
    }

    protected abstract C createCollection(Collection<? extends V> v);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (this.emptyCollSameAsNoColl ? 1231 : 1237);
        result = prime * result + this.mode.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        // no need to test createCollection(), since values are tested by super.equals()
        final CollectionMap2<?, ?, ?> other = (CollectionMap2<?, ?, ?>) obj;
        return this.emptyCollSameAsNoColl == other.emptyCollSameAsNoColl && this.mode == other.mode;
    }
}
