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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.request.MultipleSQLSelectExecutor;
import org.openconcerto.utils.ExceptionHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// do not use fields DEFAULT since they're not always constants, plus for some systems we need a
// trip to the db anyway to interpret them and they're not easily updateable.
// MAYBE don't piggyback on undefinedID, rather create a defaultValuesID (eg add column in
// FWK_UNDEF_ID) that would be archived so as to not appear in results
public class UndefinedRowValuesCache {
    private static final UndefinedRowValuesCache instance = new UndefinedRowValuesCache();

    public synchronized static UndefinedRowValuesCache getInstance() {
        return instance;
    }

    private final Map<SQLTable, SQLRowValues> map = new HashMap<SQLTable, SQLRowValues>();

    private final SQLElementDirectory getDirectory() {
        return Configuration.getInstance().getDirectory();
    }

    public SQLRowValues getDefaultRowValues(final SQLTable t) {
        SQLRowValues rv = this.map.get(t);
        if (rv == null) {
            rv = new SQLRowValues(t);
            final SQLRow undefRow = t.getRow(t.getUndefinedID());
            if (undefRow == null)
                throw new IllegalStateException(t.getSQLName() + " doesn't contain undef ID " + t.getUndefinedID());
            getDirectory().getElement(t).loadAllSafe(rv, undefRow);
            this.map.put(t, rv);
        }
        return rv;
    }

    public void preload(List<SQLTable> tablesToCache) {
        if (tablesToCache.size() <= 0) {
            throw new IllegalArgumentException("Empty list");
        }
        final List<SQLSelect> queries = new ArrayList<SQLSelect>(tablesToCache.size());
        final int size = tablesToCache.size();
        for (int i = 0; i < size; i++) {
            final SQLTable sqlTable = tablesToCache.get(i);
            final SQLSelect select = new SQLSelect(true);
            select.addSelectStar(sqlTable);
            select.setWhere(sqlTable.getKey(), "=", sqlTable.getUndefinedID());
            queries.add(select);
        }
        final MultipleSQLSelectExecutor executor = new MultipleSQLSelectExecutor(tablesToCache.get(0).getDBSystemRoot(), queries);
        try {
            final List<List<SQLRow>> l = executor.execute();
            if (l.size() != tablesToCache.size()) {
                throw new IllegalStateException("Internal SQL error while preloading");
            }
            for (int i = 0; i < size; i++) {
                final SQLTable sqlTable = tablesToCache.get(i);
                final List<SQLRow> rows = l.get(i);
                if (rows.size() > 0) {
                    final SQLRowValues rv = new SQLRowValues(sqlTable);
                    getDirectory().getElement(sqlTable).loadAllSafe(rv, rows.get(0));
                    this.map.put(sqlTable, rv);
                } else {
                    System.err.println("Warning: no undefined row in table: " + sqlTable.getName() + " id: " + sqlTable.getUndefinedID());
                }
            }
        } catch (SQLException e) {
            ExceptionHandler.handle("Unable to preload tables", e);
        }

    }
}
