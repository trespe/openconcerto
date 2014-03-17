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

import org.openconcerto.sql.model.ConnectionHandler;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRequestLog;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.utils.RTInterruptedException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbutils.ResultSetHandler;

import com.mysql.jdbc.ConnectionProperties;

public class SQLUtils {

    /**
     * Return the first chained exception with a non null SQL state.
     * 
     * @param exn an exception.
     * @return the first SQLException with a non-<code>null</code>
     *         {@link SQLException#getSQLState()}, <code>null</code> if not found.
     */
    static public final SQLException findWithSQLState(final Exception exn) {
        Throwable e = exn;
        while (e != null) {
            if (e instanceof SQLException) {
                final SQLException sqlExn = (SQLException) e;
                if (sqlExn.getSQLState() != null) {
                    return sqlExn;
                }
            }
            e = e.getCause();
        }
        return null;
    }

    public interface SQLFactory<T> {

        public T create() throws SQLException;

    }

    /**
     * Use a single transaction to execute <code>f</code> : it is either committed or rollbacked.
     * 
     * @param <T> type of factory
     * @param ds the datasource where f should be executed.
     * @param f the factory to execute.
     * @return what f returns.
     * @throws SQLException if a pb occurs.
     */
    public static <T> T executeAtomic(final SQLDataSource ds, final SQLFactory<T> f) throws SQLException {
        return executeAtomic(ds, new ConnectionHandlerNoSetup<T, SQLException>() {
            @Override
            public T handle(SQLDataSource ds) throws SQLException {
                return f.create();
            }
        });
    }

    /**
     * Use a single transaction to execute <code>h</code> : it is either committed or rollbacked.
     * 
     * @param <T> type of return
     * @param <X> type of exception of <code>h</code>
     * @param ds the datasource where h should be executed.
     * @param h the code to execute.
     * @return what h returns.
     * @throws SQLException if a pb occurs.
     * @throws X if <code>h</code> throw it.
     */
    public static <T, X extends Exception> T executeAtomic(final SQLDataSource ds, final ConnectionHandlerNoSetup<T, X> h) throws SQLException, X {
        return ds.useConnection(new ConnectionHandler<T, X>() {

            private Boolean autoCommit = null;

            @Override
            public boolean canRestoreState() {
                return true;
            }

            @Override
            public void setup(Connection conn) throws SQLException {
                this.autoCommit = conn.getAutoCommit();
                if (this.autoCommit) {
                    conn.setAutoCommit(false);
                }
            }

            @Override
            public T handle(final SQLDataSource ds) throws X, SQLException {
                return h.handle(ds);
            }

            @Override
            public void restoreState(Connection conn) throws SQLException {
                // can be null if getAutoCommit() failed, in that case nothing to do
                if (this.autoCommit == Boolean.TRUE) {
                    try {
                        this.get();
                        conn.commit();
                    } catch (Exception e) {
                        conn.rollback();
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }
            }
        });
    }

    /**
     * If conn is in autocommit, unset it, try to execute f, if an exception is raised rollback
     * otherwise commit ; finally set autocommit. Otherwise just execute f as we assume the calling
     * method handles transactions.
     * 
     * @param <T> type of factory
     * 
     * @param conn the connection.
     * @param f will be executed.
     * @return what f returns.
     * @throws SQLException if a pb occurs.
     */
    public static <T> T executeAtomic(final Connection conn, final SQLFactory<T> f) throws SQLException {
        // create a transaction if we aren't in any, otherwise do nothing
        final boolean autoCommit = conn.getAutoCommit();
        final T res;
        if (autoCommit) {
            conn.setAutoCommit(false);
            try {
                res = f.create();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } else {
            res = f.create();
        }

        return res;
    }

    /**
     * Creates a pseudo sequence with an arbitrary type (not just bigint as real sequences). These
     * statements create 2 functions : <code>next_<i>seqName</i>()</code> and
     * <code>reset_<i>seqName</i>()</code>.
     * 
     * @param seqName the name of the sequence.
     * @param sqlType its SQL type, eg "decimal(16,8)".
     * @param minVal the starting value, eg "0.123".
     * @param inc the increment, eg "3.14".
     * @return the SQL statements.
     */
    public static List<String> createPostgreSQLSeq(String seqName, String sqlType, String minVal, String inc) {
        final List<String> res = new ArrayList<String>();
        final String genT = seqName + "_generator";
        res.add("DROP TABLE if exists " + genT);
        res.add("CREATE TABLE " + genT + " ( " + decl(new String[] { "minVal", "inc", "currentVal", "tmpVal" }, sqlType) + ")");

        String body = "UPDATE " + genT + " set tmpVal = currentVal, currentVal = currentVal + inc ;";
        body += "SELECT tmpVal from " + genT + ";";
        res.addAll(createFunction("next_" + seqName, sqlType, body));

        body = "update " + genT + " set currentVal = minVal ;";
        body += "select currentVal from " + genT + ";";
        res.addAll(createFunction("reset_" + seqName, sqlType, body));

        res.add("INSERT INTO " + genT + " values(" + minVal + ", (" + inc + ") )");
        res.add("SELECT " + "reset_" + seqName + "()");

        return res;
    }

    /**
     * A list of declaration.
     * 
     * @param cols columns name, eg ["min", "inc"].
     * @param type SQL type, eg "int8".
     * @return declaration, eg "min int8, inc int8".
     */
    private static String decl(String[] cols, String type) {
        String res = "";
        for (String col : cols) {
            res += col + " " + type + ",";
        }
        // remove last ,
        return res.substring(0, res.length() - 1);
    }

    /**
     * Creates an SQL function (dropping it beforehand).
     * 
     * @param name the name of the function.
     * @param type the SQL return type.
     * @param body the body of the function.
     * @return the SQL statements.
     */
    private static List<String> createFunction(String name, String type, String body) {
        final List<String> res = new ArrayList<String>();

        res.add("DROP FUNCTION if exists " + name + "()");
        String f = "CREATE FUNCTION " + name + "() RETURNS " + type + " AS $createFunction$ ";
        f += body;
        f += " $createFunction$ LANGUAGE SQL";
        res.add(f);

        return res;
    }

    static public final String SPLIT_DELIMITER = "$jdbcDelimiter$";
    static private final Pattern splitMySQLQueries = Pattern.compile(";\r?\n");
    static public final Pattern SPLIT_PATTERN = Pattern.compile(SPLIT_DELIMITER, Pattern.LITERAL);

    /**
     * Split a SQL script so that it can be executed. For MySQL the script is split at ';' for
     * others at {@link #SPLIT_DELIMITER}.
     * 
     * @param sql the script to execute.
     * @param sysRoot where to execute.
     * @throws SQLException if an exception happens.
     */
    static public void executeScript(final String sql, final DBSystemRoot sysRoot) throws SQLException {
        // Bug 1: MySQL jdbc cannot execute what MySQL QueryBrowser can
        // ie before 5.1 you could execute a string with multiple CREATE TABLE,
        // but in 5.1 each execute must have exactly one query
        // Bug 2: MySQL does not have the concept of dollar quoted strings
        // so we have to help it and split the query (eg around trigger and functions)
        final SQLSystem sys = sysRoot.getServer().getSQLSystem();
        final Pattern p = sys == SQLSystem.MYSQL || sys == SQLSystem.MSSQL ? splitMySQLQueries : SPLIT_PATTERN;
        executeScript(sql, sysRoot, p);
    }

    static public void executeScript(final String sql, final DBSystemRoot sysRoot, final Pattern p) throws SQLException {
        try {
            for (final String s : p.split(sql)) {
                final String trimmed = s.trim();
                if (trimmed.length() > 0)
                    sysRoot.getDataSource().execute(trimmed, null);
            }
        } catch (final Exception e) {
            throw new SQLException("unable to execute " + sql, e);
        }
    }

    /**
     * Execute all queries at once if possible.
     * 
     * @param sysRoot where to execute.
     * @param queries what to execute.
     * @param handlers how to process the result sets, items can be <code>null</code>.
     * @return the results of the handlers.
     * @throws SQLException if an error occur
     * @throws RTInterruptedException if the current thread is interrupted.
     * @see {@link SQLSystem#isMultipleResultSetsSupported()}
     */
    static public List<?> executeMultiple(final DBSystemRoot sysRoot, final List<String> queries, final List<? extends ResultSetHandler> handlers) throws SQLException, RTInterruptedException {
        final int size = handlers.size();
        if (queries.size() != size)
            throw new IllegalArgumentException("Size mismatch " + queries + " / " + handlers);
        final List<Object> results = new ArrayList<Object>(size);

        final SQLSystem system = sysRoot.getServer().getSQLSystem();
        if (system.isMultipleResultSetsSupported()) {
            final long timeMs = System.currentTimeMillis();
            final long time = System.nanoTime();
            final long afterCache = time;

            final StringBuilder sb = new StringBuilder(256 * size);
            for (final String q : queries) {
                sb.append(q);
                if (!q.trim().endsWith(";"))
                    sb.append(';');
                sb.append('\n');
            }
            final String query = sb.toString();
            sysRoot.getDataSource().useConnection(new ConnectionHandlerNoSetup<Object, SQLException>() {
                @Override
                public Object handle(SQLDataSource ds) throws SQLException {
                    final Connection conn = ds.getConnection();

                    if (system == SQLSystem.MYSQL) {
                        final ConnectionProperties connectionProperties = (ConnectionProperties) ((DelegatingConnection) conn).getInnermostDelegate();
                        if (!connectionProperties.getAllowMultiQueries()) {
                            throw new IllegalStateException("Multi queries not allowed and the setting can only be set before connecting");
                        }
                    }

                    final long afterQueryInfo = System.nanoTime();
                    final long afterExecute, afterHandle;
                    final Statement stmt = conn.createStatement();
                    try {
                        if (Thread.currentThread().isInterrupted())
                            throw new RTInterruptedException("Interrupted before executing : " + query);
                        stmt.execute(query);
                        afterExecute = System.nanoTime();
                        for (final ResultSetHandler h : handlers) {
                            if (Thread.currentThread().isInterrupted())
                                throw new RTInterruptedException("Interrupted while handling results : " + query);
                            results.add(h == null ? null : h.handle(stmt.getResultSet()));
                            stmt.getMoreResults();
                        }
                        afterHandle = System.nanoTime();
                    } finally {
                        stmt.close();
                    }
                    SQLRequestLog.log(query, "executeMultiple", conn, timeMs, time, afterCache, afterQueryInfo, afterExecute, afterHandle, System.nanoTime());
                    return null;
                }
            });
        } else {
            // use the same connection to allow some insert/update followed by a select
            sysRoot.getDataSource().useConnection(new ConnectionHandlerNoSetup<Object, SQLException>() {
                @Override
                public Object handle(SQLDataSource ds) throws SQLException {
                    for (int i = 0; i < size; i++) {
                        final ResultSetHandler rsh = handlers.get(i);
                        // since the other if clause cannot support cache and this clause doesn't
                        // have any table to fire, don't use cache
                        results.add(sysRoot.getDataSource().execute(queries.get(i), rsh == null ? null : new IResultSetHandler(rsh, false)));
                    }
                    return null;
                }
            });
        }
        return results;
    }
}
