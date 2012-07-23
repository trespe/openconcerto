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
import org.openconcerto.sql.model.DBStructureItemJDBC;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.SetMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Coalesce multiple refresh.
 * 
 * @author Sylvain
 * @see #add(DBStructureItemJDBC, Set, boolean)
 * @see #getActual(DBSystemRoot, Set)
 */
class ToRefreshSpec {

    static final class TablesByRoot extends SetMap<String, String> {
        TablesByRoot() {
            super(3, Mode.NULL_MEANS_ALL, true);
        }

        TablesByRoot(Map<? extends String, ? extends Set<String>> m) {
            this();
            this.putAllCollections(m);
        }

        @Override
        protected Set<String> createCollection(Collection<? extends String> v) {
            final Set<String> res = new HashSet<String>(Math.max(8, v.size()));
            res.addAll(v);
            return res;
        }
    }

    static final class ToRefreshActual {
        private final TablesByRoot fromXML, fromJDBC;
        private final Set<SQLTable> oldTables, newTables;

        private ToRefreshActual(TablesByRoot fromXML, TablesByRoot fromJDBC, Set<SQLTable> oldTables, Set<SQLTable> newTables) {
            super();
            this.fromXML = fromXML;
            this.fromJDBC = fromJDBC;
            this.oldTables = oldTables;
            this.newTables = newTables;
        }

        public final TablesByRoot getFromXML() {
            return this.fromXML;
        }

        public final TablesByRoot getFromJDBC() {
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

    static private final TablesByRoot createMap(final DBSystemRoot sysRoot) {
        final TablesByRoot res = new TablesByRoot();
        for (final DBRoot r : sysRoot.getChildrenMap().values()) {
            // copy r.getChildrenNames() since they're immutable
            res.put(r.getName(), r.getChildrenNames());
        }
        return res;
    }

    // fill the passed map with current tables in sysRoot
    // i.e. remove inexistent roots and tables and expand null
    static private TablesByRoot fillMap(final TablesByRoot toRefresh, final DBSystemRoot sysRoot) {
        if (toRefresh == null)
            return createMap(sysRoot);

        final TablesByRoot res = new TablesByRoot(toRefresh);
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
    private TablesByRoot tablesFromXML, tablesFromJDBC;

    ToRefreshSpec() {
        super();
        // at first nothing to refresh
        this.tablesFromXML = new TablesByRoot();
        this.tablesFromJDBC = new TablesByRoot();
    }

    public final TablesByRoot getTablesFromXML() {
        return this.tablesFromXML;
    }

    public final TablesByRoot getTablesFromJDBC() {
        return this.tablesFromJDBC;
    }

    private final boolean isInScope(final TablesByRoot m, final SQLTable t) {
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

    public final ToRefreshSpec add(final DBStructureItemJDBC parent, final Set<String> childrenRefreshed, final boolean readCache) {
        final TablesByRoot tablesByRoot = readCache ? this.tablesFromXML : this.tablesFromJDBC;
        // else we already refresh all
        if (tablesByRoot != null) {
            final boolean parentIsSysRoot = parent != null && parent.getDB() instanceof DBSystemRoot;
            if (parent == null || parentIsSysRoot && childrenRefreshed == null) {
                if (readCache)
                    this.tablesFromXML = null;
                else
                    this.tablesFromJDBC = null;
            } else {
                if (parentIsSysRoot) {
                    for (final String s : childrenRefreshed)
                        tablesByRoot.put(s, null);
                } else {
                    final String key = parent.getDBRoot().getName();
                    // else we already refresh the whole root
                    if (!tablesByRoot.containsKey(key) || tablesByRoot.get(key) != null) {
                        if (!(parent.getDB() instanceof DBRoot)) {
                            tablesByRoot.add(key, parent.getAnc(SQLTable.class).getName());
                            // if parent is DBRoot and SQLBase then childrenRefreshed is the null
                            // SQLSchema, meaning all tables in the base
                        } else if (childrenRefreshed == null || parent instanceof SQLBase) {
                            tablesByRoot.put(key, null);
                        } else {
                            tablesByRoot.addAll(key, childrenRefreshed);
                        }
                    }
                }
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
        final TablesByRoot fromXML;
        final TablesByRoot fromJDBC;
        if (this.tablesFromJDBC == null) {
            fromXML = new TablesByRoot();
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
