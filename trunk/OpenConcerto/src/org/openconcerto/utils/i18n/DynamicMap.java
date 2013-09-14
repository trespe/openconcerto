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
 
 package org.openconcerto.utils.i18n;

import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Value;
import org.openconcerto.utils.cc.ITransformer;

import java.awt.Color;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A map that can dynamically add entries as they are asked. Say you need to pass a map of colours
 * indexed by their hexadecimal number representation. If you know which colours are required, you
 * can put them in the map. But if the code which is passed the map can ask any values ? Of course,
 * in that case the Map interface isn't suited, but if it's code you don't control, this class can
 * help. In this example, we can overload {@link #createValue(String)} to call
 * {@link Color#decode(String)}. Then every time {@link #containsKey(Object)} or
 * {@link #get(Object)} is called the colour will be added to the map.
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> See {@link HashMap} on how to
 * be thread safe.
 * </p>
 * 
 * @author Sylvain
 * 
 * @param <V> type of values
 */
// not using generic for key, since containsKey() and get() take Object, but we need to be able to
// know if the passed instance is a subclass of K. Since Java erases types, we can test with
// String.class.isInstance() but not Tuple2<String, Integer>.class.isInstance().
public class DynamicMap<V> implements Map<String, V> {

    private final Map<String, V> delegate;
    private final ITransformer<? super Tuple2<DynamicMap<V>, String>, ? extends V> createValue;

    public DynamicMap() {
        this(new HashMap<String, V>());
    }

    /**
     * Create a new instance using the passed map. NOTE : the map will be modified.
     * 
     * @param delegate the map to use.
     */
    public DynamicMap(Map<String, V> delegate) {
        this(delegate, null);
    }

    public DynamicMap(Map<String, V> delegate, ITransformer<? super Tuple2<DynamicMap<V>, String>, ? extends V> createValue) {
        super();
        this.delegate = delegate;
        this.createValue = createValue;
    }

    /**
     * Called when a key isn't present in the underlying map.
     * 
     * @param key the missing key.
     * @return a value that will be added to the underlying map, otherwise {@link Value#getNone()}.
     */
    protected Value<? extends V> createValue(final String key) {
        return Value.fromNonNull(this.createValueNonNull(key));
    }

    protected V createValueNonNull(final String key) {
        if (this.createValue == null)
            return null;
        else
            return this.createValue.transformChecked(Tuple2.create(this, key));
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        final boolean res = handleContains(key);
        if (res) {
            return res;
        } else if (String.class.isInstance(key)) {
            final String k = (String) key;
            final Value<? extends V> newValue = this.createValue(k);
            if (newValue.hasValue()) {
                this.delegate.put(k, newValue.getValue());
                return true;
            }
        }
        return false;
    }

    protected boolean handleContains(Object key) {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (!this.containsKey(key))
            return null;
        return handleGet(key);
    }

    protected V handleGet(Object key) {
        return this.delegate.get(key);
    }

    @Override
    public V put(String key, V value) {
        return this.delegate.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return this.delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        this.delegate.putAll(m);
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public Set<String> keySet() {
        return this.delegate.keySet();
    }

    @Override
    public Collection<V> values() {
        return this.delegate.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, V>> entrySet() {
        return this.delegate.entrySet();
    }
}
