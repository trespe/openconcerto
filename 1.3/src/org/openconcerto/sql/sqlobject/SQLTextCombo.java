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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.sql.utils.SQLCreateMoveableTable;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.ITextComboCache;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An ITextCombo with the cache from COMPLETION.
 * 
 * @author Sylvain CUAZ
 */
public class SQLTextCombo extends org.openconcerto.ui.component.ITextCombo implements RowItemViewComponent {

    static public final String getTableName() {
        return "COMPLETION";
    }

    static public final String getRefFieldName() {
        return "CHAMP";
    }

    static public final String getValueFieldName() {
        return "LABEL";
    }

    static public final SQLCreateMoveableTable getCreateTable(SQLSyntax syntax) {
        final SQLCreateMoveableTable createTable = new SQLCreateMoveableTable(syntax, getTableName());
        createTable.addVarCharColumn(getRefFieldName(), 100);
        createTable.addVarCharColumn(getValueFieldName(), 200);
        createTable.setPrimaryKey(getRefFieldName(), getValueFieldName());
        return createTable;
    }

    public SQLTextCombo() {
        super();
    }

    public SQLTextCombo(boolean locked) {
        super(locked);
    }

    public SQLTextCombo(ComboLockedMode mode) {
        super(mode);
    }

    @Override
    public void init(SQLRowItemView v) {
        if (!this.hasCache()) {
            final ITextComboCacheSQL cache = new ITextComboCacheSQL(v.getField());
            if (cache.isValid())
                this.initCache(cache);
        }
    }

    static public class ITextComboCacheSQL implements ITextComboCache {

        private final String field;
        private final SQLTable t;
        private final List<String> cache;
        private boolean loadedOnce;

        public ITextComboCacheSQL(final SQLField f) {
            this(f.getDBRoot(), f.getFullName());
        }

        public ITextComboCacheSQL(final DBRoot r, final String id) {
            this.field = id;
            this.t = r.findTable(getTableName());
            if (!this.isValid())
                Log.get().warning("no completion found for " + this.field);
            this.cache = new ArrayList<String>();
            this.loadedOnce = false;
        }

        public final boolean isValid() {
            return this.t != null;
        }

        private final SQLDataSource getDS() {
            return this.t.getDBSystemRoot().getDataSource();
        }

        public List<String> loadCache(final boolean dsCache) {
            final SQLSelect sel = new SQLSelect();
            sel.addSelect(this.t.getField(getValueFieldName()));
            sel.setWhere(new Where(this.t.getField(getRefFieldName()), "=", this.field));
            // predictable order
            sel.addFieldOrder(this.t.getField(getValueFieldName()));
            // ignore DS cache to allow the fetching of rows modified by another VM
            @SuppressWarnings("unchecked")
            final List<String> items = (List<String>) this.getDS().execute(sel.asString(), new IResultSetHandler(SQLDataSource.COLUMN_LIST_HANDLER) {
                @Override
                public boolean readCache() {
                    return dsCache;
                }

                @Override
                public boolean writeCache() {
                    return true;
                }
            });
            this.cache.clear();
            this.cache.addAll(items);

            return this.cache;
        }

        public List<String> getCache() {
            if (!this.loadedOnce) {
                this.loadCache(true);
                this.loadedOnce = true;
            }
            return this.cache;
        }

        public void addToCache(String string) {
            if (!this.cache.contains(string)) {
                final Map<String, Object> m = new HashMap<String, Object>();
                m.put(getRefFieldName(), this.field);
                m.put(getValueFieldName(), string);
                try {
                    // the primary key is not generated so don't let SQLRowValues remove it.
                    new SQLRowValues(this.t, m).insert(true, false);
                } catch (SQLException e) {
                    // e.g. some other VM hasn't already added it
                    e.printStackTrace();
                }
                // add anyway since we didn't contain it
                this.cache.add(string);
            }
        }

        public void deleteFromCache(String string) {
            final Where w = new Where(this.t.getField(getRefFieldName()), "=", this.field).and(new Where(this.t.getField(getValueFieldName()), "=", string));
            this.getDS().executeScalar("DELETE FROM " + this.t.getSQLName().quote() + " WHERE " + w.getClause());
            this.cache.removeAll(Collections.singleton(string));
        }

        @Override
        public String toString() {
            return this.getClass().getName() + " on " + this.field;
        }

    }
}
