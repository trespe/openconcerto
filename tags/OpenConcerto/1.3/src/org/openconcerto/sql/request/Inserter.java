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

import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLCreateTableBase;
import org.openconcerto.sql.utils.SQLUtils;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Allow to insert some rows into a table and get some feedback.
 * 
 * @author Sylvain
 */
public class Inserter {

    public static enum ReturnMode {
        NO_FIELDS, FIRST_FIELD, ALL_FIELDS;
    }

    public static final class Insertion<T> {
        private final List<T> list;
        private final int count;

        public Insertion(List<T> res, int count) {
            super();
            this.list = res;
            this.count = count;
            assert res == null || res.size() <= count;
        }

        /**
         * The number of rows inserted.
         * 
         * @return number of rows inserted.
         */
        public final int getCount() {
            return this.count;
        }

        /**
         * The list of inserted rows. Can be <code>null</code> if it couldn't be retrieved, e.g.
         * MySQL only supports single primary key. Likewise the size can be less than
         * {@link #getCount()}, e.g. {@link SQLSystem#H2} only support one.
         * 
         * @return the list of inserted rows, or <code>null</code>.
         */
        public final List<T> getRows() {
            return this.list;
        }
    }

    private static final String[] EMPTY_ARRAY = new String[0];

    private final List<String> pk;
    private final DBSystemRoot sysRoot;
    private final SQLName tableName;

    public Inserter(final SQLTable t) {
        this(t.getDBSystemRoot(), t.getSQLName(), t.getPKsNames());
    }

    public Inserter(final SQLCreateTable t) {
        this(t.getRoot().getDBSystemRoot(), new SQLName(t.getRootName(), t.getName()), t.getPrimaryKey());
    }

    public Inserter(final SQLCreateTableBase<?> t, final DBRoot r) {
        this(r.getDBSystemRoot(), new SQLName(r.getName(), t.getName()), t.getPrimaryKey());
    }

    public Inserter(final DBSystemRoot sysRoot, final SQLName tableName, final List<String> pk) {
        super();
        if (sysRoot == null || tableName == null)
            throw new NullPointerException();
        this.sysRoot = sysRoot;
        this.tableName = tableName;
        this.pk = pk;
    }

    protected final SQLSystem getSystem() {
        return this.sysRoot.getServer().getSQLSystem();
    }

    public final Insertion<?> insertReturnFirstField(final String sql) throws SQLException {
        return insertReturnFirstField(sql, true);
    }

    public final Insertion<?> insertReturnFirstField(final String sql, final boolean requireAllRows) throws SQLException {
        return insert(sql, ReturnMode.FIRST_FIELD, requireAllRows);
    }

    /**
     * Insert rows with the passed SQL.
     * 
     * @param sql the SQL specifying the data to be inserted, e.g. ("DESIGNATION") VALUES('A'),
     *        ('B').
     * @return an object to always know the insertion count and possibly the inserted primary keys.
     * @throws SQLException if an error occurs while inserting.
     */
    @SuppressWarnings("unchecked")
    public final Insertion<Object[]> insertReturnAllFields(final String sql) throws SQLException {
        return (Insertion<Object[]>) insert(sql, ReturnMode.ALL_FIELDS, true);
    }

    /**
     * Insert rows with the passed SQL. Should be faster than other insert methods since it doesn't
     * fetch primary keys.
     * 
     * @param sql the SQL specifying the data to be inserted, e.g. ("DESIGNATION") VALUES('A'),
     *        ('B').
     * @return the insertion count.
     * @throws SQLException if an error occurs while inserting.
     */
    public final int insertCount(final String sql) throws SQLException {
        return insert(sql, ReturnMode.NO_FIELDS, false).getCount();
    }

    /**
     * Insert rows with the passed SQL.
     * <p>
     * NOTE: only some systems support returning all rows.
     * </p>
     * 
     * @param sql the SQL specifying the data to be inserted, e.g. ("DESIGNATION") VALUES('A'),
     *        ('B').
     * @param mode which fields should be returned.
     * @param requireAllRows <code>true</code> if there must be a returned row for each inserted
     *        row, only meaningful for <code>mode != {@link ReturnMode#NO_FIELDS}</code>.
     * @return an object to always know the insertion count and possibly the inserted primary keys.
     * @throws SQLException if an error occurs while inserting.
     */
    public final Insertion<?> insert(final String sql, final ReturnMode mode, final boolean requireAllRows) throws SQLException {
        final SQLSystem sys = getSystem();

        // MAYBE instead of throwing exception :
        // 0. check that PK type is serial
        // 1. connection.setTransactionIsolation(TRANSACTION_SERIALIZABLE)
        // 2. previousMax = select max(PK)
        // 3. insert
        // 4. select PK where PK > previousMax
        final boolean requireAllFields = mode != ReturnMode.NO_FIELDS && requireAllRows;
        if (requireAllFields && sys == SQLSystem.H2)
            throw new IllegalArgumentException("H2 use IDENTITY() which only returns the last ID: " + this.tableName);
        if (requireAllFields && sys == SQLSystem.MSSQL)
            throw new IllegalArgumentException("In MS getUpdateCount() is correct but getGeneratedKeys() only returns the last ID: " + this.tableName);

        return SQLUtils.executeAtomic(this.sysRoot.getDataSource(), new ConnectionHandlerNoSetup<Insertion<?>, SQLException>() {
            @Override
            public Insertion<?> handle(SQLDataSource ds) throws SQLException {
                final Statement stmt = ds.getConnection().createStatement();
                try {
                    // ATTN don't call quote() with the passed sql otherwise it will try to parse %
                    final String insertInto = "INSERT INTO " + Inserter.this.tableName.quote() + " " + sql;
                    final int count;
                    // MySQL always return an empty resultSet for anything else than 1 pk
                    final boolean dontGetGK = mode == ReturnMode.NO_FIELDS || Inserter.this.pk.size() == 0 || (sys == SQLSystem.MYSQL && Inserter.this.pk.size() != 1);
                    if (sys == SQLSystem.POSTGRESQL) {
                        // in psql Statement.RETURN_GENERATED_KEYS actually return all fields
                        count = stmt.executeUpdate(insertInto, dontGetGK ? EMPTY_ARRAY : Inserter.this.pk.toArray(EMPTY_ARRAY));
                    } else {
                        count = stmt.executeUpdate(insertInto, dontGetGK ? Statement.NO_GENERATED_KEYS : Statement.RETURN_GENERATED_KEYS);
                    }
                    final List<?> list;
                    // cannot get or doesn't want the list
                    if (dontGetGK) {
                        list = null;
                    } else {
                        list = (List<?>) (mode == ReturnMode.FIRST_FIELD ? SQLDataSource.COLUMN_LIST_HANDLER : SQLDataSource.ARRAY_LIST_HANDLER).handle(stmt.getGeneratedKeys());
                        assert list.size() <= count;
                        if (requireAllRows && list.size() != count)
                            throw new IllegalStateException("Missing keys");
                    }
                    @SuppressWarnings("unchecked")
                    final Insertion<?> res = new Insertion<Object>((List<Object>) list, count);
                    return res;
                } finally {
                    stmt.close();
                }
            }
        });
    }
}
