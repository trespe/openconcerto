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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TinyMap<K, V> implements Map<K, V> {
    private final ArrayList<K> keys;
    private final ArrayList<V> values;

    public TinyMap() {
        this(10);
    }

    public TinyMap(int initialCapacity) {
        keys = new ArrayList<K>(initialCapacity);
        values = new ArrayList<V>(initialCapacity);
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public boolean isEmpty() {
        return keys.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return keys.contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values.contains(value);
    }

    @Override
    public V get(Object key) {
        final int size = this.keys.size();
        for (int i = 0; i < size; i++) {
            if (this.keys.get(i).equals(key)) {
                return this.values.get(i);
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        final int size = this.keys.size();
        for (int i = 0; i < size; i++) {
            if (this.keys.get(i).equals(key)) {
                final V old = this.values.get(i);
                this.values.set(i, value);
                return old;
            }
        }
        this.keys.add(key);
        this.values.add(value);
        return null;
    }

    @Override
    public V remove(Object key) {
        final int size = this.keys.size();
        for (int i = 0; i < size; i++) {
            if (this.keys.get(i).equals(key)) {
                this.keys.remove(i);
                return this.values.remove(i);
            }
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        final Set<? extends K> keySet = m.keySet();
        for (Iterator<? extends K> iterator = keySet.iterator(); iterator.hasNext();) {
            K key = (K) iterator.next();
            put(key, m.get(key));
        }
    }

    @Override
    public void clear() {
        this.keys.clear();
        this.values.clear();
    }

    // Views

    @Override
    public Set<K> keySet() {
        return new HashSet<K>(this.keys) {
            @Override
            public boolean remove(Object o) {
                TinyMap.this.remove(o);
                return super.remove(o);
            }

            @Override
            public void clear() {
                clear();
                super.clear();
            }
        };
    }

    @Override
    public Collection<V> values() {

        return new ArrayList<V>(this.values()) {
            @Override
            public V remove(int index) {
                keys.remove(index);
                values.remove(index);
                return super.remove(index);
            }

            @Override
            public boolean remove(Object o) {
                int index = values.indexOf(o);
                if (index >= 0) {
                    keys.remove(index);
                    values.remove(index);
                }
                return super.remove(o);
            }

            @Override
            public void clear() {
                clear();
                super.clear();
            }
        };
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        final Set<Entry<K, V>> set = new HashSet<Map.Entry<K, V>>() {
            @Override
            public boolean remove(Object o) {
                Map.Entry<K, V> entry = (Map.Entry<K, V>) o;
                int index = values.indexOf(entry.getValue());
                if (index >= 0) {
                    keys.remove(index);
                    values.remove(index);
                }
                return super.remove(o);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                    Entry<K, V> entry = (Entry<K, V>) iterator.next();
                    int index = values.indexOf(entry.getValue());
                    if (index >= 0) {
                        keys.remove(index);
                        values.remove(index);
                    }
                }
                return super.removeAll(c);
            }

            @Override
            public void clear() {
                clear();
                super.clear();
            }

        };
        final int size = this.keys.size();
        for (int i = 0; i < size; i++) {
            set.add(new SimpleImmutableEntry<K, V>(this.keys.get(i), this.values.get(i)));
        }
        return set;
    }

}
