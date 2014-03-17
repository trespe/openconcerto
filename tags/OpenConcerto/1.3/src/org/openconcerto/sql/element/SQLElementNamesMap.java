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
 
 package org.openconcerto.sql.element;

import org.openconcerto.utils.i18n.Phrase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public abstract class SQLElementNamesMap<K> extends SQLElementNames {

    static public class ByClass extends SQLElementNamesMap<Class<? extends SQLElement>> {
        public ByClass(Locale locale) {
            super(locale);
        }

        @Override
        protected Class<? extends SQLElement> getKey(SQLElement elem) {
            return elem.getClass();
        }
    }

    static public class ByCode extends SQLElementNamesMap<String> {
        public ByCode(Locale locale) {
            super(locale);
        }

        @Override
        protected String getKey(SQLElement elem) {
            return elem.getCode();
        }
    }

    private final Map<K, Phrase> names;

    public SQLElementNamesMap(Locale locale) {
        super(locale);
        this.names = new HashMap<K, Phrase>();
    }

    @Override
    protected final Phrase _getName(SQLElement elem) {
        return this.handleGetName(getKey(elem));
    }

    /**
     * Use this map only, i.e. the key is not an {@link SQLElement} and no recursion occurs.
     * 
     * @param key the key.
     * @return the name if present, <code>null</code> otherwise.
     * @see #getName(SQLElement)
     */
    public final synchronized Phrase handleGetName(K key) {
        return this.names.get(key);
    }

    protected abstract K getKey(SQLElement elem);

    protected final synchronized void put(K key, Phrase value) {
        this.names.put(key, value);
    }
}
