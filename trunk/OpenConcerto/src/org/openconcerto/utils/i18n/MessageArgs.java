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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import net.jcip.annotations.ThreadSafe;

import com.ibm.icu.text.MessageFormat;

/**
 * {@link MessageFormat} can take numbered or named arguments, this class unifies both. This class
 * is thread safe if the array or map isn't modified.
 */
@ThreadSafe
public final class MessageArgs {

    static protected boolean isOrdered(final Map<?, ?> m) {
        return m instanceof LinkedHashMap;
    }

    private Object[] array;
    private Map<String, ?> map;

    public MessageArgs(final Object[] array) {
        this(null, array);
    }

    public MessageArgs(final Map<String, ?> map) {
        this(map, null);
    }

    private MessageArgs(final Map<String, ?> map, final Object[] array) {
        super();
        if (map == null && array == null)
            throw new NullPointerException();
        this.array = array;
        this.map = map;
    }

    public final synchronized Object getAll() {
        return this.array != null ? this.array : this.map;
    }

    private synchronized Object[] getArray() {
        if (this.array == null) {
            this.array = new Object[this.map.size()];
            int i = 0;
            if (isOrdered(this.map)) {
                for (final Object v : this.map.values())
                    this.array[i++] = v;
            } else {
                for (final String name : new TreeSet<String>(this.map.keySet()))
                    this.array[i++] = this.map.get(name);
            }
            assert i == this.array.length;
        }
        return this.array;
    }

    protected synchronized Map<String, ?> getMap() {
        if (this.map == null) {
            final int stop = this.array.length;
            final Map<String, Object> res = new HashMap<String, Object>(stop);
            for (int i = 0; i < stop; i++) {
                res.put(String.valueOf(i), this.array[i]);
            }
            this.map = res;
        }
        return this.map;
    }

    public final Object getArgument(int i) {
        return getArray()[i];
    }

    public final Object getArgument(String name) {
        return getMap().get(name);
    }
}
