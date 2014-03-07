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
 
 package org.openconcerto.sql.model.graph;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLTable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Coalesce multiple refresh.
 * 
 * @author Sylvain
 * @see #add(TablesMap, boolean)
 * @see #getActual(DBSystemRoot, Set)
 */
class ToRefreshSpec {

    static final class ToRefreshActual {
        private final TablesMap fromXML, fromJDBC;
        private final Set<SQLTable> oldTables, newTables;

        private ToRefreshActual(TablesMap fromXML, TablesMap fromJDBC, Set<SQLTable> oldTables, Set<SQLTable> newTables) {
            super();
            this.fromXML = fromXML;
            this.fromJDBC = fromJDBC;
            this.oldTables = oldTables;
            this.newTables = newTables;
        }

        public final TablesMap getFromXML() {
            return this.fromXML;
        }

        public final TablesMap getFromJDBC() {
            return this.fromJDBC;
        }

        // current graph tables in scope
        public final Set<SQLTable> getOldTablesInScope() {
            return this.oldTables;
        }

        public final Set<SQLTable> getNewTablesInScope() {
            return this.newTables;
        }
    }

    static private final TablesMap createMap(final DBSystemRoot sysRoot) {
        final TablesMap res = new TablesMap();
        for (final DBRoot r : sysRoot.getChildrenMap().values()) {
            // copy r.getChildrenNames() since they're immutable
            res.put(r.getName(), r.getChildrenNames());
        }
        return res;
    }

    // fill the passed map with current tables in sysRoot
    // i.e. remove inexistent roots and tables and expand null
    static private TablesMap fillMap(final TablesMap toRefresh, final DBSystemRoot sysRoot) {
        if (toRefresh == null)
            return createMap(sysRoot);

        final TablesMap res = TablesMap.create(toRefresh);
        for (final Entry<String, Set<String>> e : toRefresh.entrySet()) {
            final String rootName = e.getKey();
            if (!sysRoot.contains(rootName)) {
                res.remove(rootName);
            } else {
                final Set<String> newTableNames = sysRoot.getRoot(rootName).getChildrenNames();
                if (e.getValue() == null) {
                    res.put(rootName, newTableNames);
                } else {
                    res.get(rootName).retainAll(newTableNames);
                }
            }
        }
        return res;
    }

    // null value meaning all tables, null map meaning all roots
    private TablesMap tablesFromXML, tablesFromJDBC;

    ToRefreshSpec() {
        super();
        // at first nothing to refresh
        this.tablesFromXML = new TablesMap();
        this.tablesFromJDBC = new TablesMap();
    }

    public final TablesMap getTablesFromXML() {
        return this.tablesFromXML;
    }

    public final TablesMap getTablesFromJDBC() {
        return this.tablesFromJDBC;
    }

    private final boolean isInScope(final TablesMap m, final SQLTable t) {
        final String rootName = t.getDBRoot().getName();
        if (!m.containsKey(rootName))
            return false;
        final Collection<String> values = m.get(rootName);
        return values == null || values.contains(t.getName());
    }

    private final boolean isAnyInScope() {
        return this.tablesFromXML == null || this.tablesFromJDBC == null;
    }

    public final boolean isInScope(final SQLTable t) {
        if (this.isAnyInScope())
            return true;
        return this.isInScope(this.tablesFromXML, t) || this.isInScope(this.tablesFromJDBC, t);
    }

    public final ToRefreshSpec add(final TablesMap tablesRefreshed, final boolean readCache) {
        final TablesMap tablesByRoot = readCache ? this.tablesFromXML : this.tablesFromJDBC;
        // else we already refresh all
        if (tablesByRoot != null) {
            if (tablesRefreshed == null) {
                if (readCache)
                    this.tablesFromXML = null;
                else
                    this.tablesFromJDBC = null;
            } else {
                tablesByRoot.merge(tablesRefreshed);
            }
        }
        return this;
    }

    private final Set<SQLTable> getTablesInScope(final Set<SQLTable> tables) {
        if (this.isAnyInScope())
            return new HashSet<SQLTable>(tables);

        final Set<SQLTable> res = new HashSet<SQLTable>();
        for (final SQLTable t : tables) {
            if (isInScope(t))
                res.add(t);
        }
        return res;
    }

    public final ToRefreshActual getActual(final DBSystemRoot sysRoot, final Set<SQLTable> currentGraphTables) {
        final TablesMap fromXML;
        final TablesMap fromJDBC;
        if (this.tablesFromJDBC == null) {
            fromXML = new TablesMap();
            fromJDBC = createMap(sysRoot);
        } else {
            fromXML = fillMap(this.tablesFromXML, sysRoot);
            fromJDBC = fillMap(this.tablesFromJDBC, sysRoot);
            // don't load twice the same items
            fromXML.removeAll(fromJDBC);
            fromXML.removeAllEmptyCollections();
        }
        final Set<SQLTable> toAdd = getTablesInScope(sysRoot.getDescs(SQLTable.class));
        final Set<SQLTable> toRm = getTablesInScope(currentGraphTables);
        return new ToRefreshActual(fromXML, fromJDBC, toRm, toAdd);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " :\nfrom XML " + this.getTablesFromXML() + "\nfrom JDBC " + this.getTablesFromJDBC();
    }
}
