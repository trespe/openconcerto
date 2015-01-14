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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.utils.CollectionUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.GuardedBy;

import org.h2.api.Trigger;
import org.h2.util.StringUtils;

public class PartialUniqueTrigger implements Trigger {

    private final List<String> columns;
    private final String where;
    // for each column, its position in the table
    @GuardedBy("this")
    private int[] indexes;
    @GuardedBy("this")
    private SQLName tableName = null;
    @GuardedBy("this")
    private String triggerName = null;

    public PartialUniqueTrigger(List<String> columns, String where) {
        super();
        this.columns = new ArrayList<String>(columns);
        this.indexes = null;
        if (where == null)
            throw new IllegalArgumentException("Should be using a real index");
        this.where = where;
    }

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type) throws SQLException {
        if (before)
            throw new IllegalArgumentException("Only after is supported");
        if (type == Trigger.SELECT)
            throw new IllegalArgumentException("Only DML is supported");
        synchronized (this) {
            this.tableName = new SQLName(schemaName, tableName);
            this.triggerName = triggerName;
            this.indexes = null;
        }
    }

    public synchronized int[] getIndexes(final Connection conn) throws SQLException {
        if (this.indexes == null) {
            final Set<String> toFind = new HashSet<String>(this.columns);
            if (toFind.size() < this.columns.size())
                throw new IllegalStateException("Duplicate columns : " + this.columns);
            this.indexes = new int[this.columns.size()];
            final ResultSet rs = conn.getMetaData().getColumns(null, this.tableName.getFirst(), this.tableName.getName(), null);
            try {
                int i = 0;
                while (rs.next()) {
                    final String column = rs.getString("COLUMN_NAME");
                    final int index = this.columns.indexOf(column);
                    if (index >= 0) {
                        toFind.remove(column);
                        this.indexes[index] = i;
                    } // else not part of the constraint
                    i++;
                }
            } finally {
                rs.close();
            }
            if (!toFind.isEmpty())
                throw new IllegalStateException("Columns not found : " + toFind);

        }
        return this.indexes;
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        final SQLName name;
        final String triggerName;
        final int[] indexes;
        synchronized (this) {
            name = this.tableName;
            triggerName = this.triggerName;
            indexes = this.getIndexes(conn);
        }
        final int stop = indexes.length;
        final List<String> whereList = new ArrayList<String>(stop + 1);
        whereList.add(this.where);
        final List<Object> newValues = new ArrayList<Object>(stop);
        for (int i = 0; i < stop; i++) {
            final int index = indexes[i];
            final Object newValue = newRow[index];
            // null is equal with nothing
            if (newValue == null)
                return;

            final String colName = this.columns.get(i);
            whereList.add(StringUtils.quoteIdentifier(colName) + " = ?");
            newValues.add(newValue);
        }

        final Number count;
        final PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) from " + name + " where " + CollectionUtils.join(whereList, " and "), Statement.NO_GENERATED_KEYS);
        try {
            // SQL starts at 1
            int i = 1;
            for (final Object newValue : newValues) {
                stmt.setObject(i, newValue);
                i++;
            }

            final ResultSet rs = stmt.executeQuery();
            count = (Number) SQLDataSource.SCALAR_HANDLER.handle(rs);
            rs.close();
        } finally {
            // also close rs
            stmt.close();
        }

        if (count.intValue() > 1)
            throw new SQLException("Duplicate entries for " + triggerName + " on " + this.columns + " : " + newValues, "23505");
    }

    @Override
    public void close() throws SQLException {
    }

    @Override
    public void remove() throws SQLException {
    }
}
