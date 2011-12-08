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
 
 package org.openconcerto.sql.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import org.apache.commons.dbutils.ResultSetHandler;

public class IResultSetHandler implements ResultSetHandler {

    static final boolean shouldCache(Object result) {
        final boolean shouldCache;
        if (result == REALLY_NULL) {
            shouldCache = true;
        } else if (result == null)
            shouldCache = false;
        else
            shouldCache = true;
        return shouldCache;
    }

    /**
     * If a handler returns null, it is not cached (we assume that you want the code executed each
     * time). If you want to return null and be cached you have to return REALLY_NULL.
     */
    static public final Object REALLY_NULL = new Object();
    static private final Object NOT_YET_COMPUTED = new Object();

    private final ResultSetHandler delegate;
    private final Boolean useCache;
    private Object result;

    public IResultSetHandler(ResultSetHandler rsh) {
        this(rsh, null);
    }

    /**
     * Create a new instance.
     * 
     * @param rsh our delegate.
     * @param useCache will be returned by {@link #readCache()} and {@link #writeCache()}, can be
     *        <code>null</code>.
     */
    public IResultSetHandler(ResultSetHandler rsh, final Boolean useCache) {
        this.delegate = rsh;
        this.useCache = useCache;
        this.result = NOT_YET_COMPUTED;
    }

    /**
     * Whether the cache should be checked for our query.
     * 
     * @return <code>true</code> if the cache is to be checked.
     */
    public boolean readCache() {
        return this.useCache == null ? true : this.useCache;
    }

    /**
     * Whether the cache should be updated with our result.
     * 
     * @return <code>true</code> if the cache is to be updated.
     */
    public boolean writeCache() {
        if (this.useCache != null)
            return this.useCache;
        if (this.result == NOT_YET_COMPUTED)
            throw new IllegalStateException(this + " hasn't yet been used");
        return shouldCache(this.result);
    }

    /**
     * What data is used by this handler. Only used when shouldCache returns <code>true</code>. This
     * implementation returns <code>null</code>.
     * 
     * @return a Set to be passed to {@link org.openconcerto.sql.request.SQLCache#put(Object, Object, Set)}, or
     *         <code>null</code>, meaning all tables of the datasource.
     */
    public Set<? extends SQLData> getCacheModifiers() {
        return null;
    }

    public final Object handle(ResultSet rs) throws SQLException {
        this.result = this.delegate.handle(rs);
        return this.result;
    }

    // rsh are included in the cache of SQLDataSource, so we must implement equals()

    public int hashCode() {
        return this.delegate.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof IResultSetHandler) {
            final IResultSetHandler o = (IResultSetHandler) obj;
            return this.delegate.equals(o.delegate);
        } else
            return false;
    }

}
