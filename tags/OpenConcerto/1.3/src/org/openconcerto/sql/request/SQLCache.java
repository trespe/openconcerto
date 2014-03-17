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
 
 package org.openconcerto.sql.request;

import org.openconcerto.sql.model.SQLData;
import org.openconcerto.utils.cache.CacheWatcherFactory;
import org.openconcerto.utils.cache.ICache;

/**
 * To keep result related to some SQLTables in cache. The results will be automatically invalidated
 * after some period of time or when a table is modified.
 * 
 * <img src="doc-files/cache.png"/>
 * 
 * @author Sylvain CUAZ
 * @param <K> type of keys
 * @param <V> type of value
 */
public final class SQLCache<K, V> extends ICache<K, V, SQLData> {

    private final boolean clearAfterTx;

    public SQLCache() {
        this(60);
    }

    public SQLCache(int delay) {
        this(delay, -1);
    }

    public SQLCache(int delay, int size) {
        this(delay, size, null);
    }

    /**
     * Creates a cache with the given parameters.
     * 
     * @param delay the delay in seconds before a key is cleared.
     * @param size the maximum size of the cache, negative means no limit.
     * @param name name of this cache and associated thread.
     * @throws IllegalArgumentException if size is 0.
     */
    public SQLCache(int delay, int size, final String name) {
        // clearAfterTx = true : READ_COMMITTED but even for the current transaction. To use cache
        // inside a transaction, another instance must be used (as in SQLDataSource).
        this(delay, size, name, true);
    }

    public SQLCache(int delay, int size, final String name, final boolean clearAfterTx) {
        super(delay, size, name);
        this.clearAfterTx = clearAfterTx;
        this.setWatcherFactory(new CacheWatcherFactory<K, SQLData>() {
            public SQLCacheWatcher<K> createWatcher(ICache<K, ?, SQLData> cache, SQLData o) {
                final SQLCache<K, ?> c = (SQLCache<K, ?>) cache;
                return new SQLCacheWatcher<K>(c, o);
            }
        });
    }

    public final boolean isClearedAfterTransaction() {
        return this.clearAfterTx;
    }
}
