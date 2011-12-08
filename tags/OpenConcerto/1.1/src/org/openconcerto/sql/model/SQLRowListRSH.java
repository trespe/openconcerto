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

import org.openconcerto.utils.Tuple2;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbutils.ResultSetHandler;

public final class SQLRowListRSH implements ResultSetHandler {

    private static Tuple2<SQLTable, List<String>> getIndexes(SQLSelect sel, final SQLTable passedTable, final boolean findTable) {
        final List<SQLField> selectFields = sel.getSelectFields();
        final int size = selectFields.size();
        if (size == 0)
            throw new IllegalArgumentException("empty select : " + sel);
        final SQLTable t;
        if (findTable) {
            if (passedTable != null)
                throw new IllegalArgumentException("non null table " + passedTable);
            t = selectFields.get(0).getTable();
        } else {
            if (passedTable == null)
                throw new IllegalArgumentException("null table");
            t = passedTable;
        }
        // cannot pass an alias to this method since getSelectFields() returns SQLField and not
        // FieldRef
        final List<AliasedTable> aliases = sel.getAliases(t);
        if (aliases.size() != 1)
            throw new IllegalArgumentException(t + " isn't exactly once : " + aliases);
        final List<String> l = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            final SQLField field = selectFields.get(i);
            if (field.getTable().equals(t))
                l.add(field.getName());
            else if (findTable)
                throw new IllegalArgumentException(field + " is not in " + t);
            else
                l.add(null);
        }
        return Tuple2.create(t, l);
    }

    /**
     * Create a handler that don't need metadata.
     * 
     * @param sel the select that will produce the result set, must only have one table.
     * @return a handler creating a list of {@link SQLRow}.
     */
    static public ResultSetHandler createFromSelect(final SQLSelect sel) {
        return create(getIndexes(sel, null, true));
    }

    /**
     * Create a handler that don't need metadata. Useful since some JDBC drivers perform queries for
     * each metadata.
     * 
     * @param sel the select that will produce the result set.
     * @param t the table for which to create rows, must appear only once in <code>sel</code>.
     * @return a handler creating a list of {@link SQLRow}.
     */
    static public ResultSetHandler createFromSelect(final SQLSelect sel, final SQLTable t) {
        return create(getIndexes(sel, t, false));
    }

    static private ResultSetHandler create(final Tuple2<SQLTable, List<String>> names) {
        return new ResultSetHandler() {
            @Override
            public Object handle(ResultSet rs) throws SQLException {
                return SQLRow.createListFromRS(names.get0(), rs, names.get1());
            }
        };
    }

    @SuppressWarnings("unchecked")
    static public List<SQLRow> execute(final SQLSelect sel) {
        final Tuple2<SQLTable, List<String>> indexes = getIndexes(sel, null, true);
        return (List<SQLRow>) indexes.get0().getDBSystemRoot().getDataSource().execute(sel.asString(), create(indexes));
    }

    private final SQLTable t;
    private final boolean tableOnly;

    public SQLRowListRSH(SQLTable t) {
        this(t, false);
    }

    public SQLRowListRSH(SQLTable t, final boolean tableOnly) {
        super();
        this.t = t;
        this.tableOnly = tableOnly;
    }

    public Object handle(ResultSet rs) throws SQLException {
        return SQLRow.createListFromRS(this.t, rs, this.tableOnly);
    }
}
