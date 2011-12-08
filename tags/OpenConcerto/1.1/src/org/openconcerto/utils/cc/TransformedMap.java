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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Creates a Map that translate items between itself and another map. Both maps are still usable.
 * 
 * @author Sylvain
 * 
 * @param <K> type of keys of the original map.
 * @param <KT> type of keys of this map.
 * @param <V> type of values.
 * @see TransformedSet
 */
public class TransformedMap<K, KT, V> extends AbstractMap<KT, V> {

    private final Map<K, V> map;
    private final ITransformer<K, KT> transf;
    private final ITransformer<KT, K> invTransf;

    public TransformedMap(Map<K, V> map, ITransformer<K, KT> transf, ITransformer<KT, K> invTransf) {
        super();
        this.map = map;
        this.transf = transf;
        this.invTransf = invTransf;
    }

    public final void clear() {
        this.map.clear();
    }

    public final boolean isEmpty() {
        return this.map.isEmpty();
    }

    public final int size() {
        return this.map.size();
    }

    // **keys

    @SuppressWarnings("unchecked")
    public final boolean containsKey(Object key) {
        // can throw ClassCastException as per the javadoc
        return this.map.containsKey(this.invTransf.transformChecked((KT) key));
    }

    @SuppressWarnings("unchecked")
    public final V get(Object key) {
        // can throw ClassCastException as per the javadoc
        return this.map.get(this.invTransf.transformChecked((KT) key));
    }

    @SuppressWarnings("unchecked")
    public final V remove(Object key) {
        // can throw ClassCastException as per the javadoc
        return this.map.remove(this.invTransf.transformChecked((KT) key));
    }

    public final Set<KT> keySet() {
        return new TransformedSet<K, KT>(this.map.keySet(), this.transf, this.invTransf);
    }

    // ** values

    public final Collection<V> values() {
        return this.map.values();
    }

    public final boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    // ** both

    @Override
    public Set<Entry<KT, V>> entrySet() {
        return new TransformedSet<Entry<K, V>, Entry<KT, V>>(this.map.entrySet(), new ITransformer<Entry<K, V>, Entry<KT, V>>() {
            @Override
            public Entry<KT, V> transformChecked(final Entry<K, V> input) {
                return new Entry<KT, V>() {
                    @Override
                    public final KT getKey() {
                        return TransformedMap.this.transf.transformChecked(input.getKey());
                    }

                    @Override
                    public final V getValue() {
                        return input.getValue();
                    }

                    @Override
                    public final V setValue(V value) {
                        return input.setValue(value);
                    }
                };
            }
        }, new ITransformer<Entry<KT, V>, Entry<K, V>>() {
            @Override
            public Entry<K, V> transformChecked(final Entry<KT, V> input) {
                return new Entry<K, V>() {
                    @Override
                    public final K getKey() {
                        return TransformedMap.this.invTransf.transformChecked(input.getKey());
                    }

                    @Override
                    public final V getValue() {
                        return input.getValue();
                    }

                    @Override
                    public final V setValue(V value) {
                        return input.setValue(value);
                    }
                };
            }
        });
    }

    public final V put(KT key, V value) {
        return this.map.put(this.invTransf.transformChecked(key), value);
    }

    // keep the putAll() of AbstractMap which just calls #put()
}
