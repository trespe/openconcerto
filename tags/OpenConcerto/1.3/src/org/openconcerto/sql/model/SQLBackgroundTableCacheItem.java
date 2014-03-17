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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SQLBackgroundTableCacheItem implements SQLTableModifiedListener {

    private SQLTable table;
    private int timeout;
    private List<SQLRow> rows = new ArrayList<SQLRow>();
    private long lastReload;// time in millis

    public SQLBackgroundTableCacheItem(final SQLTable t, final int second) {
        this.table = t;
        this.timeout = second;

    }

    @Override
    public void tableModified(SQLTableEvent evt) {
        this.lastReload = 0;
        reloadFromDbIfNeeded();
    }

    @SuppressWarnings("unchecked")
    public synchronized void reloadFromDbIfNeeded() {
        final long delta = System.currentTimeMillis() - this.lastReload;
        if (delta / 1000 > this.timeout) {
            final SQLSelect sel = new SQLSelect();
            sel.addSelectStar(this.table);
            this.rows = Collections.unmodifiableList((List<SQLRow>) this.table.getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel, this.table)));
            this.lastReload = System.currentTimeMillis();
        }
    }

    public synchronized SQLRow getFirstRowContains(final int value, final SQLField field) {
        for (SQLRow r : this.rows) {
            if (r.getInt(field.getName()) == value && !r.isArchived()) {
                return r;
            }
        }
        return null;
    }

    public synchronized SQLRow getFirstRowContains(final String value, final SQLField field) {
        for (SQLRow r : this.rows) {
            if (r.getString(field.getName()).equals(value) && !r.isArchived()) {
                return r;
            }
        }
        return null;
    }

    public synchronized SQLRow getRowFromId(final int i) {
        return getFirstRowContains(i, this.table.getKey());
    }

    public synchronized List<SQLRow> getRows() {
        return this.rows;
    }

    public SQLTable getTable() {
        return this.table;
    }
}
