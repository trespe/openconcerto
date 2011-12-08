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
import org.openconcerto.sql.model.SQLDataListener;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.utils.cache.CacheWatcher;

/**
 * A listener to invalidate cache results when their data is modified. Currently datum can either be
 * a SQLTable or a SQLRow.
 * 
 * @param <K> type of keys
 */
public class SQLCacheWatcher<K> extends CacheWatcher<K, SQLData> {

    private final SQLTableModifiedListener listener;

    SQLCacheWatcher(final SQLCache<K, ?> c, final SQLData t) {
        super(c, t);
        this.listener = t.createTableListener(new SQLDataListener() {
            public void dataChanged() {
                clearCache();
            }
        });
        this.getTable().addPremierTableModifiedListener(this.listener);
    }

    private final SQLTable getTable() {
        return this.getData().getTable();
    }

    @Override
    protected void dying() {
        this.getTable().removeTableModifiedListener(this.listener);
    }

}
