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

import org.openconcerto.utils.CompareUtils;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

/**
 * A thread-safe map in which all mutative operations (put, remove, and so on) are implemented by
 * making a fresh copy of the underlying map. The instance of the underlying map can be customized
 * with {@link #copy(Map)}. The underlying immutable map is available with {@link #getImmutable()}.
 * 
 * @author Sylvain
 * 
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
@ThreadSafe
public class CopyOnWriteMap<K, V> extends AbstractMap<K, V> {

    private Map<K, V> immutable;
    private Set<Entry<K, V>> entrySet = null;

    public CopyOnWriteMap() {
        this.immutable = Collections.emptyMap();
    }

    public CopyOnWriteMap(final Map<K, V> m) {
        this.immutable = Collections.unmodifiableMap(copy(m));
    }

    /**
     * Create a copy of the passed map.
     * 
     * @param src the map to copy, not <code>null</code>.
     * @return a shallow copy of <code>src</code>.
     */
    public Map<K, V> copy(Map<? extends K, ? extends V> src) {
        return new HashMap<K, V>(src);
    }

    // write

    @Override
    public synchronized V put(final K key, final V value) {
        final Map<K, V> copy = copy(this.immutable);
        final V res = copy.put(key, value);
        this.immutable = Collections.unmodifiableMap(copy);
        return res;
    }

    @Override
    public synchronized V remove(final Object key) {
        final Map<K, V> copy = copy(this.immutable);
        final V res = copy.remove(key);
        this.immutable = Collections.unmodifiableMap(copy);
        return res;
    }

    @Override
    public synchronized void putAll(final Map<? extends K, ? extends V> m) {
        final Map<K, V> copy = copy(this.immutable);
        copy.putAll(m);
        this.immutable = Collections.unmodifiableMap(copy);
    }

    @Override
    public synchronized void clear() {
        this.immutable = Collections.emptyMap();
    }

    // read

    public synchronized final Map<K, V> getImmutable() {
        return this.immutable;
    }

    @Override
    public synchronized int size() {
        return this.immutable.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.immutable.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(final Object key) {
        return this.immutable.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(final Object value) {
        return this.immutable.containsValue(value);
    }

    @Override
    public synchronized V get(final Object key) {
        return this.immutable.get(key);
    }

    @Override
    public synchronized Set<Entry<K, V>> entrySet() {
        if (this.entrySet == null)
            this.entrySet = new EntrySet();
        return this.entrySet;
    }

    private final class EntrySet extends AbstractSet<Entry<K, V>> {
        public Iterator<Entry<K, V>> iterator() {
            return new Iterator<Entry<K, V>>() {
                private final Iterator<Entry<K, V>> delegate = CopyOnWriteMap.this.immutable.entrySet().iterator();
                private Entry<K, V> current = null;

                @Override
                public boolean hasNext() {
                    return this.delegate.hasNext();
                }

                @Override
                public Entry<K, V> next() {
                    return (this.current = this.delegate.next());
                }

                @Override
                public void remove() {
                    if (this.current == null)
                        throw new IllegalStateException();
                    EntrySet.this.remove(this.current);
                }
            };
        }

        public boolean contains(Object o) {
            if (!(o instanceof Entry))
                return false;
            final Entry<?, ?> e = (Entry<?, ?>) o;
            return CompareUtils.equals(e.getValue(), get(e.getKey()));
        }

        public boolean remove(Object o) {
            if (!(o instanceof Entry))
                return false;
            return CopyOnWriteMap.this.remove(((Entry<?, ?>) o).getKey()) != null;
        }

        public int size() {
            return CopyOnWriteMap.this.size();
        }

        public void clear() {
            CopyOnWriteMap.this.clear();
        }
    }

    // equals

    @Override
    public synchronized boolean equals(final Object o) {
        return this.immutable.equals(o);
    }

    @Override
    public synchronized int hashCode() {
        return this.immutable.hashCode();
    }
}
