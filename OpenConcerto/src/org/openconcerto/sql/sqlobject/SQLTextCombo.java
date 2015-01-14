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

    static public final SQLCreateMoveableTable getCreateTable(final SQLSyntax syntax) {
        final SQLCreateMoveableTable createTable = new SQLCreateMoveableTable(syntax, getTableName());
        createTable.addVarCharColumn(getRefFieldName(), 100);
        createTable.addVarCharColumn(getValueFieldName(), 200);
        createTable.setPrimaryKey(getRefFieldName(), getValueFieldName());
        return createTable;
    }

    public SQLTextCombo() {
        super();
    }

    public SQLTextCombo(final boolean locked) {
        super(locked);
    }

    public SQLTextCombo(final ComboLockedMode mode) {
        super(mode);
    }

    @Override
    public void init(final SQLRowItemView v) {
        if (!this.hasCache()) {
            final ITextComboCacheSQL cache = new ITextComboCacheSQL(v.getField());
            if (cache.isValid())
                this.initCache(cache);
        }
    }

    static public class ITextComboCacheSQL extends AbstractComboCacheSQL {

        private final String id;

        public ITextComboCacheSQL(final SQLField f) {
            this(f.getDBRoot(), f.getFullName());
        }

        public ITextComboCacheSQL(final DBRoot r, final String id) {
            super(r.findTable(getTableName()), getValueFieldName());
            this.id = id;
        }

        @Override
        public final boolean isValid() {
            return this.getTable() != null;
        }

        @Override
        protected Where createWhere() {
            return new Where(this.getTable().getField(getRefFieldName()), "=", this.id);
        }

        @Override
        public void addToCache(final String string) {
            if (!this.cache.contains(string)) {
                final Map<String, Object> m = new HashMap<String, Object>();
                m.put(getRefFieldName(), this.id);
                m.put(getValueFieldName(), string);
                try {
                    // the primary key is not generated so don't let SQLRowValues remove it.
                    new SQLRowValues(this.getTable(), m).insert(true, false);
                } catch (final SQLException e) {
                    // e.g. some other VM hasn't already added it
                    e.printStackTrace();
                }
                // add anyway since we didn't contain it
                this.cache.add(string);
            }
        }

        @Override
        public void deleteFromCache(final String string) {
            final Where w = new Where(this.getTable().getField(getRefFieldName()), "=", this.id).and(new Where(this.getField(), "=", string));
            this.getDS().executeScalar("DELETE FROM " + this.getTable().getSQLName().quote() + " WHERE " + w.getClause());
            this.cache.removeAll(Collections.singleton(string));
        }

        @Override
        public String toString() {
            return super.toString() + "/" + this.id;
        }
    }

    static public class ITextComboCacheExistingValues extends AbstractComboCacheSQL {

        public ITextComboCacheExistingValues(final SQLField f) {
            super(f);
        }

        @Override
        public boolean isValid() {
            return String.class.isAssignableFrom(this.getField().getType().getJavaType());
        }

        @Override
        protected Where createWhere() {
            return null;
        }

        @Override
        public void addToCache(final String string) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteFromCache(final String string) {
            throw new UnsupportedOperationException();
        }
    }

    // a cache that takes values from an SQL field
    static public abstract class AbstractComboCacheSQL implements ITextComboCache {

        // values are in this field (e.g. COMPLETION.LABEL)
        private final SQLField f;
        protected final List<String> cache;
        private boolean loadedOnce;

        protected AbstractComboCacheSQL(final SQLTable t, final String fieldName) {
            this(t == null ? null : t.getField(fieldName));
        }

        protected AbstractComboCacheSQL(final SQLField f) {
            this.f = f;
            this.cache = new ArrayList<String>();
            this.loadedOnce = false;
            if (!this.isValid())
                Log.get().warning("no completion found for " + this);
        }

        protected final SQLDataSource getDS() {
            return this.f.getDBSystemRoot().getDataSource();
        }

        @Override
        public List<String> loadCache(final boolean dsCache) {
            final SQLSelect sel = new SQLSelect();
            sel.addSelect(getField());
            sel.setWhere(createWhere());
            // predictable order
            sel.addFieldOrder(getField());
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

        protected final SQLTable getTable() {
            return this.getField().getTable();
        }

        protected final SQLField getField() {
            return this.f;
        }

        protected abstract Where createWhere();

        @Override
        public List<String> getCache() {
            if (!this.loadedOnce) {
                this.loadCache(true);
                this.loadedOnce = true;
            }
            return this.cache;
        }

        @Override
        public String toString() {
            return this.getClass().getName() + " on " + this.getField();
        }

    }
}
