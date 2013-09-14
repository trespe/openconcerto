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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.State;
import org.openconcerto.sql.request.SQLCache;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.ThreadFactory;
import org.openconcerto.utils.cache.CacheResult;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnection;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingConnection;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbcp.SQLNestedException;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.RowProcessor;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.postgresql.util.GT;
import org.postgresql.util.PSQLException;

/**
 * Une source de donnée SQL.
 * 
 * @author ILM Informatique 10 juin 2004
 */
@ThreadSafe
public final class SQLDataSource extends BasicDataSource implements Cloneable {

    // MAYBE add a cache, but ATTN synchronized : one connection per thread, but only one shared DS

    /** A map of supported database systems and associated drivers. */
    static public final Map<SQLSystem, String> DRIVERS;
    static {
        DRIVERS = new HashMap<SQLSystem, String>();
        DRIVERS.put(SQLSystem.MYSQL, "com.mysql.jdbc.Driver");
        DRIVERS.put(SQLSystem.POSTGRESQL, "org.postgresql.Driver");
        DRIVERS.put(SQLSystem.DERBY, "org.apache.derby.jdbc.ClientDriver");
        DRIVERS.put(SQLSystem.H2, "org.h2.Driver");
        DRIVERS.put(SQLSystem.MSSQL, "com.microsoft.sqlserver.jdbc.SQLServerDriver");

        // by default h2 convert database name to upper case (we used to work around it with
        // SQLSystem.getMDName() but in r2251 an equalsIgnoreCase() was replaced by equals())
        // see http://code.google.com/p/h2database/issues/detail?id=204
        System.setProperty("h2.databaseToUpper", "false");
    }

    // timeouts in seconds
    static public final int loginTimeOut = 15;
    static public final int socketTimeOut = 8 * 60;

    static public interface IgnoringRowProcessor extends RowProcessor {

        @Override
        public Map<String, Object> toMap(ResultSet rs) throws SQLException;

        /**
         * Convert the passed result set to a map, ignoring some columns.
         * 
         * @param rs the result set.
         * @param toIgnore which columns' label to ignore.
         * @return a map with all columns of <code>rs</code> except <code>toIgnore</code>.
         * @throws SQLException if an error occurs while reading <code>rs</code>.
         */
        public Map<String, Object> toMap(ResultSet rs, Set<String> toIgnore) throws SQLException;
    }

    // ignoring case-sensitive processor
    static private class IgnoringCSRowProcessor extends BasicRowProcessor implements IgnoringRowProcessor {
        @Override
        public Map<String, Object> toMap(ResultSet rs) throws SQLException {
            return toMap(rs, Collections.<String> emptySet());
        }

        // on ne veut pas de CaseInsensitiveMap
        @Override
        public Map<String, Object> toMap(ResultSet rs, Set<String> toIgnore) throws SQLException {
            final Map<String, Object> result = new HashMap<String, Object>();
            final ResultSetMetaData rsmd = rs.getMetaData();
            final int cols = rsmd.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                // ATTN use label not base name (eg "des" in "SELECT DESIGNATION as des FROM ...")
                final String label = rsmd.getColumnLabel(i);
                if (!toIgnore.contains(label))
                    result.put(label, rs.getObject(i));
            }
            return result;
        }
    }

    static public final IgnoringRowProcessor ROW_PROC = new IgnoringCSRowProcessor();
    // all thread safe
    public static final ColumnListHandler COLUMN_LIST_HANDLER = new ColumnListHandler();
    public static final ArrayListHandler ARRAY_LIST_HANDLER = new ArrayListHandler();
    public static final ArrayHandler ARRAY_HANDLER = new ArrayHandler();
    public static final ScalarHandler SCALAR_HANDLER = new ScalarHandler();
    public static final MapListHandler MAP_LIST_HANDLER = new MapListHandler(ROW_PROC);
    public static final MapHandler MAP_HANDLER = new MapHandler(ROW_PROC);

    // Cache, linked to cacheEnable and tables
    @GuardedBy("this")
    private SQLCache<List<?>, Object> cache;
    @GuardedBy("this")
    private boolean cacheEnabled;
    // tables that can be used in queries (and thus can impact the cache)
    @GuardedBy("this")
    private Set<SQLTable> tables;

    private static int count = 0; // compteur de requetes

    private final SQLServer server;
    // no need to synchronize multiple call to this attribute since we only access the
    // Thread.currentThread() key
    @GuardedBy("handlers")
    private final Map<Thread, HandlersStack> handlers;

    @GuardedBy("this")
    private ExecutorService exec = null;

    private final Object setInitialShemaLock = new String("initialShemaWriteLock");
    // linked to initialSchema and uptodate
    @GuardedBy("this")
    private boolean initialShemaSet;
    @GuardedBy("this")
    private String initialShema;
    // which Connection have the right default schema
    @GuardedBy("this")
    private final Map<Connection, Object> schemaUptodate;
    // which Connection aren't invalidated
    @GuardedBy("this")
    private final Map<Connection, Object> uptodate;

    private volatile int retryWait;
    @GuardedBy("this")
    private boolean blockWhenExhausted;
    @GuardedBy("this")
    private long softMinEvictableIdleTimeMillis;

    @GuardedBy("this")
    private int txIsolation;
    @GuardedBy("this")
    private Integer dbTxIsolation;
    @GuardedBy("this")
    private boolean checkOnceDBTxIsolation;

    private final ReentrantLock testLock = new ReentrantLock();

    public SQLDataSource(SQLServer server, String base, String login, String pass) {
        this(server, server.getURL(base), login, pass, Collections.<SQLTable> emptySet());
    }

    private SQLDataSource(SQLServer server, String url, String login, String pass, Set<SQLTable> tables) {
        this(server);

        final SQLSystem system = server.getSQLSystem();
        if (!DRIVERS.containsKey(system))
            throw new IllegalArgumentException("unknown database system: " + system);

        this.setDriverClassName(DRIVERS.get(system));
        this.setUrl("jdbc:" + system.getJDBCName() + ":" + url);

        this.setUsername(login);
        this.setPassword(pass);
        this.setTables(tables);

        if (this.server.getSQLSystem() == SQLSystem.MYSQL) {
            this.addConnectionProperty("transformedBitIsBoolean", "true");
            // by default allowMultiQueries, since it's more convenient (i.e. pass String around
            // instead of List<String>) and faster (less trips to the server, allow
            // SQLUtils.executeMultiple())
            this.addConnectionProperty("allowMultiQueries", "true");
        } else if (this.server.getSQLSystem() == SQLSystem.H2) {
            this.addConnectionProperty("CACHE_SIZE", "32000");
        }
        this.setLoginTimeout(loginTimeOut);
        this.setSocketTimeout(socketTimeOut);
        this.setRetryWait(3);
        // ATTN DO NOT call execute() or any method that might create a connection
        // since at this point dsInit() has not been called and thus connection properties might be
        // missing (eg allowMultiQueries). And the faulty connection will stay in the pool.
    }

    @Override
    public final void setLoginTimeout(int timeout) {
        if (this.server.getSQLSystem() == SQLSystem.MYSQL) {
            this.addConnectionProperty("connectTimeout", timeout + "000");
        } else if (this.server.getSQLSystem() == SQLSystem.POSTGRESQL) {
            this.addConnectionProperty("loginTimeout", timeout + "");
        }
    }

    public final void setSocketTimeout(int timeout) {
        if (this.server.getSQLSystem() == SQLSystem.MYSQL) {
            this.addConnectionProperty("socketTimeout", timeout + "000");
        } else if (this.server.getSQLSystem() == SQLSystem.H2) {
            this.addConnectionProperty("QUERY_TIMEOUT", timeout + "000");
        } else if (this.server.getSQLSystem() == SQLSystem.POSTGRESQL) {
            this.addConnectionProperty("socketTimeout", timeout + "");
        }
    }

    public final void setRetryWait(int retryWait) {
        this.retryWait = retryWait;
    }

    synchronized void setTables(Set<SQLTable> tables) {
        // don't change the cache if we're only adding tables
        final boolean update = this.cache == null || !tables.containsAll(this.tables);
        this.tables = Collections.unmodifiableSet(new HashSet<SQLTable>(tables));
        if (update)
            updateCache();
    }

    private synchronized void updateCache() {
        if (this.cache != null)
            this.cache.clear();
        this.cache = createCache(this);
        for (final HandlersStack s : this.handlers.values()) {
            s.updateCache();
        }
    }

    synchronized SQLCache<List<?>, Object> createCache(final Object o) {
        final SQLCache<List<?>, Object> res;
        if (this.isCacheEnabled() && this.tables.size() > 0)
            // the general cache should wait for transactions to end, but the cache of transactions
            // must not.
            res = new SQLCache<List<?>, Object>(30, 30, "results of " + o.getClass().getSimpleName(), o == this);
        else
            res = null;
        return res;
    }

    /**
     * Enable or disable the cache. ATTN if you enable the cache you must
     * {@link SQLTable#fire(SQLTableEvent) fire} table events, or use a class that does like
     * {@link SQLRowValues}.
     * 
     * @param b <code>true</code> to enable the cache.
     */
    public final synchronized void setCacheEnabled(boolean b) {
        if (this.cacheEnabled != b) {
            this.cacheEnabled = b;
            updateCache();
        }
    }

    public final synchronized boolean isCacheEnabled() {
        return this.cacheEnabled;
    }

    /* pour le clonage */
    private SQLDataSource(SQLServer server) {
        this.server = server;
        // on a besoin d'une implementation synchronisée
        this.handlers = new Hashtable<Thread, HandlersStack>();
        // weak, since this is only a hint to avoid initializing the connection
        // on each borrowal
        this.schemaUptodate = new WeakHashMap<Connection, Object>();
        this.uptodate = new WeakHashMap<Connection, Object>();
        this.initialShemaSet = false;
        this.initialShema = null;

        // see #getNewConnection(boolean)
        this.setValidationQuery("SELECT 1");
        this.setTestOnBorrow(false);

        this.setInitialSize(3);
        this.setMaxActive(48);
        // creating connections is quite costly so make sure we always have a couple free
        this.setMinIdle(2);
        // but not too much as it can lock out other users (the server has a max connection count)
        this.setMaxIdle(16);
        this.setBlockWhenExhausted(false);
        // check 5 connections every 4 seconds
        this.setTimeBetweenEvictionRunsMillis(4000);
        this.setNumTestsPerEvictionRun(5);
        // kill extra (above minIdle) connections after 40s
        this.setSoftMinEvictableIdleTimeMillis(TimeUnit.SECONDS.toMillis(40));
        // kill idle connections after 30 minutes (even if it means re-creating some new ones
        // immediately afterwards to ensure minIdle connections)
        this.setMinEvictableIdleTimeMillis(TimeUnit.MINUTES.toMillis(30));

        // the default of many systems
        this.txIsolation = Connection.TRANSACTION_READ_COMMITTED;
        // by definition unknown without a connection
        this.dbTxIsolation = null;
        // it's rare that DB configuration changes, and it's expensive to add a trip to the server
        // for each new connection
        this.checkOnceDBTxIsolation = true;

        // see #createDataSource() for properties not supported by this class
        this.tables = Collections.emptySet();
        this.cache = null;
        this.cacheEnabled = false;
    }

    /**
     * Exécute la requête et retourne le résultat sous forme de liste de map. Si la requete va
     * retourner beaucoup de lignes, il est peut-être préférable d'utiliser un ResultSetHandler.
     * 
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     * @see MapListHandler
     * @see #execute(String, ResultSetHandler)
     */
    public List execute(String query) {
        return (List) this.execute(query, MAP_LIST_HANDLER);
    }

    /**
     * Exécute la requête et retourne la première colonne uniquement.
     * 
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     * @see ColumnListHandler
     * @see #execute(String, ResultSetHandler)
     */
    public List executeCol(String query) {
        return (List) this.execute(query, COLUMN_LIST_HANDLER);
    }

    /**
     * Exécute la requête et retourne le résultat sous forme de liste de tableau. Si la requete va
     * retourner beaucoup de lignes, il est peut-être préférable d'utiliser un ResultSetHandler.
     * 
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     * @see ArrayListHandler
     * @see #execute(String, ResultSetHandler)
     */
    public List executeA(String query) {
        return (List) this.execute(query, ARRAY_LIST_HANDLER);
    }

    /**
     * Exécute la requête et retourne la première ligne du résultat sous forme de map.
     * 
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     * @see MapHandler
     * @see #execute(String)
     */
    public Map execute1(String query) {
        return (Map) this.execute(query, MAP_HANDLER);
    }

    /**
     * Exécute la requête et retourne la première ligne du résultat sous forme de tableau.
     * 
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     * @see ArrayHandler
     * @see #executeA(String)
     */
    public Object[] executeA1(String query) {
        return (Object[]) this.execute(query, ARRAY_HANDLER);
    }

    /**
     * Exécute la requête et retourne la valeur de la premiere colonne de la premiere ligne.
     * 
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     */
    public Object executeScalar(String query) {
        return this.execute(query, SCALAR_HANDLER);
    }

    /**
     * Exécute la requête et passe le résultat au ResultSetHandler.
     * 
     * @param query le requête à exécuter.
     * @param rsh le handler à utiliser, ou <code>null</code>.
     * @return le résultat du handler, <code>null</code> si rsh est <code>null</code>.
     * @see #execute(String)
     */
    public Object execute(String query, ResultSetHandler rsh) {
        return this.execute(query, rsh, null);
    }

    /**
     * Execute <code>query</code> within <code>c</code>, passing the result set to <code>rsh</code>.
     * 
     * @param query the query to perform.
     * @param rsh what to do with the result, can be <code>null</code>.
     * @param changeState whether <code>query</code> changes the state of a connection.
     * @return the result of <code>rsh</code>, <code>null</code> if rsh or the resultSet is
     *         <code>null</code>.
     * @throws RTInterruptedException if the current thread is interrupted while waiting for the
     *         cache or for the database.
     */
    public final Object execute(final String query, final ResultSetHandler rsh, final boolean changeState) throws RTInterruptedException {
        return this.execute(query, rsh, changeState, null);
    }

    private Object execute(final String query, final ResultSetHandler rsh, final Connection c) throws RTInterruptedException {
        // false since the vast majority of request do NOT change the state
        return this.execute(query, rsh, false, c);
    }

    /**
     * Execute <code>query</code> within <code>c</code>, passing the result set to <code>rsh</code>.
     * 
     * @param query the query to perform.
     * @param rsh what to do with the result, can be <code>null</code>.
     * @param changeState whether <code>query</code> changes the state of a connection.
     * @param passedConn the sql connection to use.
     * @return the result of <code>rsh</code>, <code>null</code> if rsh or the resultSet is
     *         <code>null</code>.
     * @throws RTInterruptedException if the current thread is interrupted while waiting for the
     *         cache or for the database.
     */
    private Object execute(final String query, final ResultSetHandler rsh, final boolean changeState, final Connection passedConn) throws RTInterruptedException {
        final long timeMs = System.currentTimeMillis();
        final long time = System.nanoTime();
        // some systems refuse to execute nothing
        if (query.length() == 0) {
            SQLRequestLog.log(query, "Pas de requête.", timeMs, time);
            return null;
        }

        final IResultSetHandler irsh = rsh instanceof IResultSetHandler ? (IResultSetHandler) rsh : null;
        final SQLCache<List<?>, Object> cache;
        synchronized (this) {
            // transactions are isolated from one another, so their caches should be too
            final HandlersStack handlersStack = getHandlersStack();
            if (handlersStack != null && handlersStack.getCache() != null)
                cache = handlersStack.getCache();
            else
                cache = this.cache;
        }
        final List<Object> key = cache != null && query.startsWith("SELECT") ? Arrays.asList(new Object[] { query, rsh }) : null;
        if (key != null && (irsh == null || irsh.readCache())) {
            final CacheResult<Object> l = cache.check(key);
            if (l.getState() == CacheResult.State.INTERRUPTED)
                throw new RTInterruptedException("interrupted while waiting for the cache");
            else if (l.getState() == CacheResult.State.VALID) {
                // cache actif
                if (State.DEBUG)
                    State.INSTANCE.addCacheHit();
                SQLRequestLog.log(query, "En cache.", timeMs, time);
                return l.getRes();
            }
        }

        Object result = null;
        QueryInfo info = null;
        final long afterCache = System.nanoTime();
        final long afterQueryInfo, afterExecute, afterHandle;
        try {
            info = new QueryInfo(query, changeState, passedConn);
            try {
                afterQueryInfo = System.nanoTime();
                final Object[] res = this.executeTwice(info);
                final Statement stmt = (Statement) res[0];
                ResultSet rs = (ResultSet) res[1];
                // TODO 1. rename #execute(String) to #executeN(String)
                // and make #execute(String) do #execute(String, null)
                // 2. let null rs pass to rsh
                // otherwise you write ds.execute("req", new ResultSetHandler() {
                // public Object handle(ResultSet rs) throws SQLException {
                // return "OK";
                // }
                // });
                // and OK won't be returned if "req" returns a null rs.
                afterExecute = System.nanoTime();
                if (rsh != null && rs != null) {
                    if (this.getSystem() == SQLSystem.DERBY || this.getSystem() == SQLSystem.POSTGRESQL) {
                        rs = new SQLResultSet(rs);
                    }

                    result = rsh.handle(rs);
                }
                afterHandle = System.nanoTime();

                stmt.close();
                // if key was added to the cache
                if (key != null) {
                    synchronized (this) {
                        putInCache(cache, irsh, key, result, true);
                    }
                }
                info.releaseConnection();
            } catch (SQLException exn) {
                // don't usually do a getSchema() as it access the db
                throw new IllegalStateException("Impossible d'accéder au résultat de " + query + "\n in " + this, exn);
            }
        } catch (RuntimeException e) {
            // for each #check() there must be a #removeRunning()
            // let the cache know we ain't gonna tell it the result
            if (cache != null && key != null)
                cache.removeRunning(key);
            if (info != null)
                info.releaseConnection(e);
            throw e;
        }

        SQLRequestLog.log(query, "", info.getConnection(), timeMs, time, afterCache, afterQueryInfo, afterExecute, afterHandle, System.nanoTime());

        return result;
    }

    private synchronized void putInCache(final SQLCache<List<?>, Object> cache, final IResultSetHandler irsh, final List<Object> key, Object result, final boolean removeRunning) {
        if (irsh != null && irsh.writeCache()) {
            cache.put(key, result, irsh.getCacheModifiers() == null ? this.tables : irsh.getCacheModifiers());
        } else if (irsh == null && IResultSetHandler.shouldCache(result)) {
            cache.put(key, result, this.tables);
        } else if (removeRunning) {
            cache.removeRunning(key);
        }
    }

    private synchronized final ExecutorService getExec() {
        if (this.exec == null) {
            // not daemon since we want the connections to be returned
            final ThreadFactory factory = new ThreadFactory(SQLDataSource.class.getSimpleName() + " " + this.toString() + " exec n° ", false);
            // a rather larger number of threads since all they do is wait severals seconds
            this.exec = new ThreadPoolExecutor(0, 32, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), factory);
        }
        return this.exec;
    }

    private final class QueryInfo {
        private final String query;
        // whether query change the state of our connection
        private final boolean changeState;
        // can change if private
        private Connection c;
        // whether we acquired a new connection (and thus can do whatever we want with it)
        private final boolean privateConnection;

        QueryInfo(String query, boolean changeState, final Connection passedConn) {
            super();
            this.query = query;
            this.changeState = changeState;

            // if passedConn is provided use it, else we need to find one
            boolean acquiredConnection = false;
            final Connection foundConn;
            if (passedConn != null)
                foundConn = passedConn;
            else if (!handlingConnection()) {
                foundConn = getNewConnection();
                acquiredConnection = true;
            } else {
                final HandlersStack threadHandlers = getHandlersStack();
                if (!changeState || threadHandlers.isChangeAllowed()) {
                    foundConn = threadHandlers.getConnection();
                } else {
                    throw new IllegalStateException("the passed query change the connection's state and the current thread has a connection which will thus be changed."
                            + " A possible solution is to execute it in the setup() of a ConnectionHandler\n" + query);
                }
            }

            this.privateConnection = acquiredConnection;
            this.c = foundConn;
        }

        public final Connection getConnection() {
            return this.c;
        }

        public final String getQuery() {
            return this.query;
        }

        void releaseConnection(RuntimeException e) {
            // MySQL reste des fois bloqué dans SocketInputStream.socketRead0()
            // (le serveur ayant tué la query)
            if (e instanceof InterruptedQuery && getSystem() == SQLSystem.MYSQL) {
                final ExecutorThread thread = ((InterruptedQuery) e).getThread();

                if (this.privateConnection) {
                    if (this.changeState)
                        // no need to try to save the connection, it is no longer valid
                        this.releaseConnection();
                    else {
                        // test if the connection is still valid before returning it to the pool
                        getExec().execute(new Runnable() {
                            public void run() {
                                // on attend un peu
                                try {
                                    thread.join(1500);
                                    // pour voir si on meurt
                                    if (thread.isAlive()) {
                                        Log.get().warning(getFailedCancelMsg());
                                        closeConnection(getConnection());
                                    } else {
                                        // la connexion est ok, on la remet dans le pool
                                        returnConnection(getConnection());
                                    }
                                } catch (InterruptedException e) {
                                    // the datasource is closing
                                    Log.get().fine("Interrupted while joining " + getQuery());
                                    closeConnection(getConnection());
                                }
                            }
                        });
                    }
                } else {
                    // try to save the connection since it is used by others
                    try {
                        // clear the interrupt status set by InterruptedQuery
                        // so that we can wait on thread
                        Thread.interrupted();
                        thread.join(500);
                    } catch (InterruptedException e2) {
                        System.err.println("ignore, we are already interrupted");
                        e2.printStackTrace();
                    }
                    // remettre le flag pour les méthodes appelantes.
                    Thread.currentThread().interrupt();

                    // connection is still stuck
                    if (thread.isAlive()) {
                        throw new IllegalStateException(getFailedCancelMsg(), e);
                    } else
                        this.releaseConnection();
                }
            } else
                this.releaseConnection();
        }

        void releaseConnection() {
            // have we borrowed a connection, otherwise it is not our responsibility to release it.
            if (this.privateConnection) {
                if (this.changeState)
                    // the connection is no longer in a pristine state so close it
                    closeConnection(this.getConnection());
                else
                    // otherwise we can reuse it
                    returnConnection(this.getConnection());
            }
        }

        private final String getFailedCancelMsg() {
            return "cancel of " + System.identityHashCode(getConnection()) + " failed for " + getQuery();
        }

        // an error has occured, try within another connection if possible
        final Connection obtainNewConnection() {
            if (!this.privateConnection)
                return null;
            else {
                // ATTN should be sure that our connection was not already closed,
                // see #closeConnection()
                closeConnection(this.getConnection());
                this.c = borrowConnection(true);
                return this.getConnection();
            }
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + " private connection: " + this.privateConnection + " query: " + this.getQuery();
        }
    }

    /**
     * Whether the current thread has called {@link #useConnection(ConnectionHandler)}.
     * 
     * @return <code>true</code> if within <code>useConnection()</code> and thus safe to call
     *         {@link #getConnection()}.
     */
    public final boolean handlingConnection() {
        return this.handlers.containsKey(Thread.currentThread());
    }

    private final HandlersStack getHandlersStack() {
        return this.handlers.get(Thread.currentThread());
    }

    /**
     * Use a single connection to execute <code>handler</code>.
     * 
     * @param <T> type of return.
     * @param <X> type of exception.
     * @param handler what to do with the connection.
     * @return what <code>handler</code> returned.
     * @throws SQLException if an exception happens in setup() or restore().
     * @throws X if handle() throws an exception.
     * @see ConnectionHandler
     */
    public final <T, X extends Exception> T useConnection(ConnectionHandler<T, X> handler) throws SQLException, X {
        final HandlersStack h;
        if (!this.handlingConnection()) {
            h = new HandlersStack(this, this.getNewConnection(), handler);
            this.handlers.put(Thread.currentThread(), h);
        } else if (handler.canRestoreState()) {
            h = this.getHandlersStack().push(handler);
        } else
            throw new IllegalStateException("this thread has already called useConnection() and thus expect its state, but the passed handler cannot restore state: " + handler);

        Connection conn = null;
        Exception exn = null;
        try {
            conn = h.getConnection();
            h.setChangeAllowed(true);
            handler.setup(conn);
            h.setChangeAllowed(false);
            handler.compute(this);
        } catch (Exception e) {
            h.setChangeAllowed(false);
            exn = e;
        }

        // in all cases (thanks to the above catch), try to restore the state
        // if conn is null setup() was never called
        boolean pristineState = conn == null;
        if (!pristineState && handler.canRestoreState()) {
            h.setChangeAllowed(true);
            try {
                handler.restoreState(conn);
                pristineState = true;
            } catch (Exception e) {
                if (exn == null)
                    exn = e;
                else
                    // the original exn as the source
                    exn = new SQLException("could not restore state: " + ExceptionUtils.getStackTrace(e), exn);
            }
            h.setChangeAllowed(false);
        }

        // ATTN conn can be null (return/closeConnection() accept it)
        if (h.pop()) {
            // remove if this thread has no handlers left
            this.handlers.remove(Thread.currentThread());
            if (pristineState)
                this.returnConnection(conn);
            else
                this.closeConnection(conn);
        } else {
            // connection is still used
            if (!pristineState) {
                h.invalidConnection();
                this.closeConnection(conn);
            }
            // else the top handler will release the connection
        }
        if (exn != null)
            if (exn instanceof RuntimeException)
                throw (RuntimeException) exn;
            else
                throw (SQLException) exn;
        else
            return handler.get();
    }

    // this method create a Statement, don't forget to close it when you're done
    private Object[] executeTwice(QueryInfo queryInfo) throws SQLException {
        final String query = queryInfo.getQuery();
        Object[] res;
        try {
            res = executeOnce(query, queryInfo.getConnection());
        } catch (SQLException exn) {
            if (State.DEBUG)
                State.INSTANCE.addFailedRequest(query);
            // maybe this was a network problem, so wait a little
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RTInterruptedException(e.getMessage() + " : " + query, exn);
            }
            // and try to obtain a new connection
            try {
                final Connection otherConn = queryInfo.obtainNewConnection();
                if (otherConn != null) {
                    res = executeOnce(query, otherConn);
                } else
                    throw exn;
            } catch (Exception e) {
                if (e == exn)
                    throw exn;
                else
                    throw new SQLException("second exec failed: " + e.getLocalizedMessage(), exn);
            }
            // only log if the second succeeds (otherwise it's thrown)
            Log.get().log(Level.INFO, "executeOnce() failed for " + queryInfo, exn);
        }
        return res;
    }

    private Object[] executeOnce(String query, Connection c) throws SQLException {
        final Statement stmt = c.createStatement();
        final ResultSet rs = execute(query, stmt);
        return new Object[] { stmt, rs };
    }

    /**
     * Exécute la requête et retourne le résultat. Attention le resultSet peut cesser d'être valide
     * a tout moment, de plus cette méthode ne ferme pas le statement qu'elle crée, la méthode
     * préférée est execute()
     * 
     * @param query le requête à exécuter.
     * @return le résultat de la requête.
     * @deprecated replaced by execute().
     * @see #execute(String)
     */
    public ResultSet executeRaw(String query) {
        try {
            return execute(query, this.getStatement());
        } catch (SQLException e) {
            try {
                return execute(query, this.getStatement());
            } catch (SQLException ex) {
                ExceptionHandler.handle("Impossible d'executer la query: " + query, ex);
                return null;
            }
        }
    }

    /**
     * Retourne un nouveau statement. Attention, la fermeture est à la charge de l'appelant.
     * 
     * @return un nouveau statement.
     * @throws SQLException if an error occurs.
     */
    private Statement getStatement() throws SQLException {
        return this.getConnection().createStatement();
    }

    /**
     * Execute la requete avec le statement passé. Attention cette méthode ne peut fermer le
     * statement car elle retourne directement le resultSet.
     * 
     * @param query le requête à exécuter.
     * @param stmt le statement.
     * @return le résultat de la requête, should never be null according to the spec but Derby don't
     *         care.
     * @throws SQLException si erreur lors de l'exécution de la requête.
     */
    private ResultSet execute(String query, Statement stmt) throws SQLException, RTInterruptedException {
        // System.err.println("\n" + count + "*** " + query + "\n");

        if (State.DEBUG)
            State.INSTANCE.beginRequest(query);

        // test before calling JDBC methods and creating threads
        if (Thread.currentThread().isInterrupted()) {
            throw new RTInterruptedException("request interrupted : " + query);
        }

        final long t1 = System.currentTimeMillis();
        ResultSet rs = null;
        try {
            // MAYBE un truc un peu plus formel
            if (query.startsWith("INSERT") || query.startsWith("UPDATE") || query.startsWith("DELETE") || query.startsWith("ALTER") || query.startsWith("DROP") || query.startsWith("SET")) {
                final boolean returnGenK = (query.startsWith("INSERT") || query.startsWith("UPDATE")) && stmt.getConnection().getMetaData().supportsGetGeneratedKeys();
                stmt.executeUpdate(query, returnGenK ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS);
                rs = returnGenK ? stmt.getGeneratedKeys() : null;
            } else {
                // TODO en faire qu'un seul par Connection
                final ExecutorThread thr = new ExecutorThread(stmt, query);
                // on lance l'exécution
                thr.start();
                // et on attend soit qu'elle finisse soit qu'on soit interrompu
                try {
                    rs = thr.getRs();
                } catch (InterruptedException e) {
                    thr.stopQuery();
                    throw new InterruptedQuery("request interrupted : " + query, e, thr);
                }
            }
        } finally {
            if (State.DEBUG)
                State.INSTANCE.endRequest(query);
        }
        long t2 = System.currentTimeMillis();
        // obviously very long queries tend to last longer, that's normal so don't warn
        if (t2 - t1 > 1000 && query.length() < 1000) {
            System.err.println("Warning:" + (t2 - t1) + "ms pour :" + query);
        }

        count++;
        return rs;
    }

    private final class InterruptedQuery extends RTInterruptedException {

        private final ExecutorThread thread;

        InterruptedQuery(String message, Throwable cause, ExecutorThread thr) {
            super(message, cause);
            this.thread = thr;
        }

        public final ExecutorThread getThread() {
            return this.thread;
        }
    }

    private static int executorSerial = 0;

    private final class ExecutorThread extends Thread {

        private final Statement stmt;
        private final String query;

        private ResultSet rs;
        private Exception exn;
        private boolean canceled;

        public ExecutorThread(Statement stmt, String query) {
            super(executorSerial++ + " ExecutorThread on " + query);
            this.stmt = stmt;
            this.query = query;
            this.canceled = false;
        }

        public void run() {
            synchronized (this) {
                if (this.canceled)
                    return;
            }

            ResultSet rs = null;
            try {
                // do not use executeQuery since this.query might contain several statements
                this.stmt.execute(this.query);
                synchronized (this) {
                    if (this.canceled)
                        return;
                }
                rs = this.stmt.getResultSet();
            } catch (Exception e) {
                // can only be SQLException or RuntimeException
                // eg MySQLStatementCancelledException if stopQuery() was called
                this.exn = e;
            }
            this.rs = rs;
        }

        public void stopQuery() throws SQLException {
            this.stmt.cancel();
            synchronized (this) {
                this.canceled = true;
            }
        }

        public ResultSet getRs() throws SQLException, InterruptedException {
            this.join();
            // pas besoin de synchronized puisque seule notre thread ecrit les var
            // et qu'elle est maintenant terminée
            if (this.exn != null) {
                if (this.exn instanceof SQLException)
                    throw (SQLException) this.exn;
                else
                    throw (RuntimeException) this.exn;
            }
            return this.rs;
        }
    }

    /**
     * All connections obtained with {@link #getConnection()} will be closed immediately, but
     * threads in {@link #useConnection(ConnectionHandler)} will get to keep them. After the last
     * thread returns from {@link #useConnection(ConnectionHandler)} there won't be any connection
     * left open. This instance will be permanently closed, it cannot be reused later.
     * 
     * @throws SQLException if a database error occurs
     */
    public synchronized void close() throws SQLException {
        @SuppressWarnings("rawtypes")
        final GenericObjectPool pool = this.connectionPool;
        super.close();
        // super close and unset our pool, but we need to keep it
        // to allow used connections to be closed, see #closeConnection(Connection)
        this.connectionPool = pool;

        // interrupt to force waiting threads to close their connections
        if (this.exec != null) {
            this.exec.shutdownNow();
            this.exec = null;
        }

        // uptodate was cleared by closeConnection()
        // the handlers will clear themselves
        // the cache is expected to be cleared (when all connections are closed)
        if (this.getBorrowedConnectionCount() == 0)
            noConnectionIsOpen();
        // ATTN keep tables to be able to reopen
    }

    private synchronized void noConnectionIsOpen() {
        assert this.connectionPool.getNumIdle() + this.connectionPool.getNumActive() == 0;
        if (this.cache != null)
            this.cache.clear();
    }

    /**
     * Retourne la connection à cette source de donnée.
     * 
     * @return la connection à cette source de donnée.
     * @throws IllegalStateException if not called from within useConnection().
     * @see #useConnection(ConnectionHandler)
     * @see #handlingConnection()
     */
    public final Connection getConnection() {
        final HandlersStack res = this.getHandlersStack();
        if (res == null)
            throw new IllegalStateException("useConnection() wasn't called");
        return res.getConnection();
    }

    public final TransactionPoint getTransactionPoint() {
        final HandlersStack handlersStack = this.getHandlersStack();
        if (handlersStack == null)
            return null;
        return handlersStack.getLastTxPoint();
    }

    /**
     * Retourne une connection à cette source de donnée (generally
     * {@link #useConnection(ConnectionHandler)} should be used). Si la connexion échoue cette
     * méthode va réessayer quelques secondes plus tard.
     * <p>
     * Note : you <b>must</b> return this connection (e.g. use try/finally).
     * <p>
     * 
     * @return une connection à cette source de donnée.
     * @see #returnConnection(Connection)
     * @see #closeConnection(Connection)
     */
    protected final Connection getNewConnection() {
        try {
            return this.borrowConnection(false);
        } catch (RTInterruptedException e) {
            throw e;
        } catch (Exception e) {
            return this.borrowConnection(true);
        }
    }

    /**
     * Borrow a new connection from the pool, optionally purging invalid connections with the
     * validation query.
     * 
     * @param test if <code>true</code> then testOnBorrow will be set.
     * @return the new connection.
     */
    private final Connection borrowConnection(final boolean test) {
        if (test) {
            this.testLock.lock();
            // invalidate all bad connections
            setTestOnBorrow(true);
        }
        try {
            final Connection res = this.getRawConnection();
            try {
                initConnection(res);
                return res;
            } catch (RuntimeException e) {
                this.closeConnection(res);
                throw e;
            }
        } finally {
            if (test) {
                setTestOnBorrow(false);
                this.testLock.unlock();
            }
        }
    }

    // initialize the passed connection if needed
    protected final void initConnection(final Connection res) {
        boolean setSchema = false;
        String schemaToSet = null;
        synchronized (this) {
            if (!this.schemaUptodate.containsKey(res)) {
                if (this.initialShemaSet) {
                    setSchema = true;
                    schemaToSet = this.initialShema;
                }
                // safe to put before setSchema() since res cannot be passed to
                // release/closeConnection()
                this.schemaUptodate.put(res, null);
            }
            // a connection from the pool is up to date since we close all idle connections in
            // invalidateAllConnections() and borrowed ones are closed before they return to the
            // pool
            this.uptodate.put(res, null);
        }
        // warmup the connection (executing a bogus simple query, like "SELECT 1") could help but in
        // general doesn't since we often do getDS().execute() and thus our warm up thread will run
        // after the execute(), making it useless.
        if (setSchema)
            this.setSchema(schemaToSet, res);
    }

    private static final String pgInterrupted = GT.tr("Interrupted while attempting to connect.");

    private Connection getRawConnection() {
        assert !Thread.holdsLock(this) : "super.getConnection() might block (see setWhenExhaustedAction()), and since return/closeConnection() need this lock, this method cannot wait while holding the lock";
        Connection result = null;
        try {
            result = super.getConnection();
        } catch (SQLException e1) {
            // try to know if interrupt, TODO cleanup : patch pg Driver.java to fill the cause
            if (e1.getCause() instanceof InterruptedException || (e1 instanceof PSQLException && e1.getMessage().equals(pgInterrupted))) {
                throw new RTInterruptedException(e1);
            }
            final int retryWait = this.retryWait;
            if (retryWait == 0)
                throw new IllegalStateException("Impossible d'obtenir une connexion sur " + this, e1);
            try {
                // on attend un petit peu
                Thread.sleep(retryWait * 1000);
                // avant de réessayer
                result = super.getConnection();
            } catch (InterruptedException e) {
                throw new RTInterruptedException("interrupted while waiting for a second try", e);
            } catch (Exception e) {
                throw new IllegalStateException("Impossible d'obtenir une connexion sur " + this + ": " + e.getLocalizedMessage(), e1);
            }
        }
        if (State.DEBUG)
            State.INSTANCE.connectionCreated();
        return result;
    }

    public final int getBorrowedConnectionCount() {
        return this.connectionPool.getNumActive();
    }

    public synchronized boolean blocksWhenExhausted() {
        return this.blockWhenExhausted;
    }

    public synchronized void setBlockWhenExhausted(boolean block) {
        this.blockWhenExhausted = block;
        if (this.connectionPool != null) {
            this.connectionPool.setWhenExhaustedAction(block ? GenericObjectPool.WHEN_EXHAUSTED_BLOCK : GenericObjectPool.WHEN_EXHAUSTED_GROW);
        }
    }

    public synchronized final long getSoftMinEvictableIdleTimeMillis() {
        return this.softMinEvictableIdleTimeMillis;
    }

    public synchronized final void setSoftMinEvictableIdleTimeMillis(long millis) {
        this.softMinEvictableIdleTimeMillis = millis;
        if (this.connectionPool != null) {
            this.connectionPool.setSoftMinEvictableIdleTimeMillis(millis);
        }
    }

    /**
     * Whether the database defaut transaction isolation is check only once for this instance. If
     * <code>false</code>, every new connection will have its
     * {@link Connection#setTransactionIsolation(int) isolation set}. If <code>true</code> the
     * isolation will only be set if the {@link #setInitialTransactionIsolation(int) requested one}
     * differs from the DB one. In other words, if you want to optimize DB access, the DB
     * configuration must match the datasource configuration.
     * 
     * @param checkOnce <code>true</code> to check only once the DB transaction isolation.
     */
    public synchronized final void setTransactionIsolationCheckedOnce(final boolean checkOnce) {
        this.checkOnceDBTxIsolation = checkOnce;
        this.dbTxIsolation = null;
    }

    public synchronized final boolean isTransactionIsolationCheckedOnce() {
        return this.checkOnceDBTxIsolation;
    }

    // don't use setDefaultTransactionIsolation() in super since it makes extra requests each time a
    // connection is borrowed
    public final void setInitialTransactionIsolation(int level) {
        if (level != Connection.TRANSACTION_READ_UNCOMMITTED && level != Connection.TRANSACTION_READ_COMMITTED && level != Connection.TRANSACTION_REPEATABLE_READ
                && level != Connection.TRANSACTION_SERIALIZABLE)
            throw new IllegalArgumentException("Invalid value :" + level);
        synchronized (this) {
            if (this.txIsolation != level) {
                this.txIsolation = level;
                // perhaps do like setInitialSchema() : i.e. call setTransactionIsolation() on
                // existing connections
                this.invalidateAllConnections(false);
            }
        }
    }

    public synchronized final int getInitialTransactionIsolation() {
        return this.txIsolation;
    }

    public synchronized final Integer getDBTransactionIsolation() {
        return this.dbTxIsolation;
    }

    final synchronized void setTransactionIsolation(Connection conn) throws SQLException {
        if (this.dbTxIsolation == null) {
            this.dbTxIsolation = conn.getTransactionIsolation();
            assert this.dbTxIsolation != null;
        }
        // no need to try to change the level if the DB doesn't support transactions
        if (this.dbTxIsolation != Connection.TRANSACTION_NONE && (!this.checkOnceDBTxIsolation || this.dbTxIsolation != this.txIsolation)) {
            // if not check once, it's the desired action, so don't log
            if (this.checkOnceDBTxIsolation)
                Log.get().config("Setting transaction isolation to " + this.txIsolation);
            conn.setTransactionIsolation(this.txIsolation);
        }
    }

    // allow to know transaction states
    private final class TransactionPoolableConnection extends PoolableConnection {
        // perhaps call getAutoCommit() once to have the initial value
        @GuardedBy("this")
        private boolean autoCommit = true;

        private TransactionPoolableConnection(Connection conn, @SuppressWarnings("rawtypes") ObjectPool pool, AbandonedConfig config) {
            super(conn, pool, config);
        }

        private HandlersStack getNonNullHandlersStack() throws SQLException {
            final HandlersStack res = getHandlersStack();
            if (res == null)
                throw new SQLException("Unsafe transaction, call useConnection() or SQLUtils.executeAtomic()");
            return res;
        }

        @Override
        public synchronized void setAutoCommit(boolean autoCommit) throws SQLException {
            if (this.autoCommit != autoCommit) {
                // don't call setAutoCommit() if no stack
                final HandlersStack handlersStack = getNonNullHandlersStack();
                super.setAutoCommit(autoCommit);
                this.autoCommit = autoCommit;
                if (this.autoCommit)
                    // some delegates of the super implementation might have already called our
                    // commit(), but in this case, the following commit will be a no-op
                    handlersStack.commit();
                else
                    handlersStack.addTxPoint(new TransactionPoint(this));
            }
        }

        @Override
        public synchronized void commit() throws SQLException {
            super.commit();
            final HandlersStack handlersStack = getNonNullHandlersStack();
            handlersStack.commit();
            handlersStack.addTxPoint(new TransactionPoint(this));
        }

        @Override
        public synchronized void rollback() throws SQLException {
            super.rollback();
            final HandlersStack handlersStack = getNonNullHandlersStack();
            handlersStack.rollback();
            assert !this.autoCommit;
            handlersStack.addTxPoint(new TransactionPoint(this));
        }

        @Override
        public synchronized Savepoint setSavepoint() throws SQLException {
            // don't call setSavepoint() if no stack
            final HandlersStack handlersStack = getNonNullHandlersStack();
            final Savepoint res = super.setSavepoint();
            handlersStack.addTxPoint(new TransactionPoint(this, res, false));
            return res;
        }

        @Override
        public synchronized Savepoint setSavepoint(String name) throws SQLException {
            // don't call setSavepoint() if no stack
            final HandlersStack handlersStack = getNonNullHandlersStack();
            final Savepoint res = super.setSavepoint(name);
            handlersStack.addTxPoint(new TransactionPoint(this, res, true));
            return res;
        }

        @Override
        public synchronized void rollback(Savepoint savepoint) throws SQLException {
            super.rollback(savepoint);
            getNonNullHandlersStack().rollback(savepoint);
        }

        @Override
        public synchronized void releaseSavepoint(Savepoint savepoint) throws SQLException {
            // don't bother merging TransactionPoint
            super.releaseSavepoint(savepoint);
        }
    }

    @Override
    protected void createPoolableConnectionFactory(ConnectionFactory driverConnectionFactory, @SuppressWarnings("rawtypes") KeyedObjectPoolFactory statementPoolFactory, AbandonedConfig configuration)
            throws SQLException {
        PoolableConnectionFactory connectionFactory = null;
        try {
            connectionFactory = new PoolableConnectionFactory(driverConnectionFactory, this.connectionPool, statementPoolFactory, this.validationQuery, this.validationQueryTimeout,
                    this.connectionInitSqls, this.defaultReadOnly, this.defaultAutoCommit, this.defaultTransactionIsolation, this.defaultCatalog, configuration) {
                @Override
                public Object makeObject() throws Exception {
                    Connection conn = this._connFactory.createConnection();
                    if (conn == null) {
                        throw new IllegalStateException("Connection factory returned null from createConnection");
                    }
                    initializeConnection(conn);
                    setTransactionIsolation(conn);
                    if (null != this._stmtPoolFactory) {
                        @SuppressWarnings("rawtypes")
                        KeyedObjectPool stmtpool = this._stmtPoolFactory.createPool();
                        conn = new PoolingConnection(conn, stmtpool);
                        stmtpool.setFactory((PoolingConnection) conn);
                    }
                    return new TransactionPoolableConnection(conn, this._pool, this._config);
                }
            };
            validateConnectionFactory(connectionFactory);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Cannot create PoolableConnectionFactory", e);
        }
    }

    @Override
    protected void createConnectionPool() {
        super.createConnectionPool();
        // methods not defined in superclass and thus not called in super
        this.connectionPool.setLifo(true);
        this.setBlockWhenExhausted(this.blockWhenExhausted);
        this.connectionPool.setSoftMinEvictableIdleTimeMillis(this.softMinEvictableIdleTimeMillis);
    }

    @Override
    protected void createDataSourceInstance() throws SQLException {
        // PoolingDataSource returns a PoolGuardConnectionWrapper that complicates a lot of
        // things for nothing, so overload to simply return an object of the pool
        this.dataSource = new PoolingDataSource(this.connectionPool) {

            // we'll migrate to plain SQLException when our superclass does
            @SuppressWarnings("deprecation")
            @Override
            public Connection getConnection() throws SQLException {
                try {
                    return (Connection) this._pool.borrowObject();
                } catch (SQLException e) {
                    throw e;
                } catch (NoSuchElementException e) {
                    throw new SQLNestedException("Cannot get a connection, pool exhausted", e);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SQLNestedException("Cannot get a connection, general error", e);
                }
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * To return a connection to the pool.
     * 
     * @param con a connection obtained with getRawConnection(), can be <code>null</code>.
     */
    protected void returnConnection(final Connection con) {
        if (con != null) {
            // if !this.initialShemaSet the out of date cannot be brought up to date
            final boolean unrecoverableOutOfDate;
            synchronized (this) {
                unrecoverableOutOfDate = !this.uptodate.containsKey(con) || !this.initialShemaSet && !this.schemaUptodate.containsKey(con);
            }
            if (isClosed() || unrecoverableOutOfDate)
                // if closed : don't fill the pool (which will have thrown an exception anyway)
                // if we shouldn't set the schema, we must close all previous connections
                // so that we get new ones from the db with the current setting
                this.closeConnection(con);
            else {
                try {
                    // our connectionPool use PoolableConnectionFactory which creates
                    // PoolableConnection whose close() actually does a returnObject()
                    con.close();
                } catch (Exception e) {
                    /* tant pis */
                    Log.get().log(Level.FINE, "Could not return " + con, e);
                }
                if (State.DEBUG)
                    State.INSTANCE.connectionRemoved();
            }
        }
    }

    /**
     * To actually close a connection to the db (and remove it from the pool).
     * 
     * @param con a connection obtained with getRawConnection(), can be <code>null</code>.
     */
    protected void closeConnection(final Connection con) {
        // Neither BasicDataSource nor PoolingDataSource provide a closeConnection()
        // so we implement one here
        if (con != null) {
            synchronized (this) {
                this.uptodate.remove(con);
                this.schemaUptodate.remove(con);
            }
            try {
                // ATTN this always does _numActive--, so we can't call it multiple times
                // with the same object
                this.connectionPool.invalidateObject(con);
            } catch (Exception e) {
                /* tant pis */
                Log.get().log(Level.FINE, "Could not close " + con, e);
            }
            // the last connection is being returned
            if (this.isClosed() && this.getBorrowedConnectionCount() == 0) {
                noConnectionIsOpen();
            }
        }
    }

    /**
     * Invalidates all open connections. This immediately closes idle connections. Borrowed ones are
     * marked as invalid, so that they are closed on return. In other words, after this method
     * returns, no existing connection will be provided.
     */
    public final void invalidateAllConnections() {
        this.invalidateAllConnections(false);
    }

    public final void invalidateAllConnections(final boolean preventIdleConnections) {
        // usefull since Evictor of GenericObjectPool might call ensureMinIdle()
        if (preventIdleConnections) {
            this.setMinIdle(0);
            this.setMaxIdle(0);
        }
        synchronized (this) {
            // otherwise nothing to invalidate
            if (this.connectionPool != null) {
                // closes all idle connections
                this.connectionPool.clear();
                // borrowed connections will be closed on return
                this.uptodate.clear();
            }
        }
    }

    /**
     * From now on, every new connection will have its default schema set to schemaName.
     * 
     * @param schemaName the name of the initial default schema, <code>null</code> to remove any
     *        default schema.
     */
    public void setInitialSchema(String schemaName) {
        if (schemaName != null || this.server.getSQLSystem().isClearingPathSupported()) {
            this.setInitialSchema(true, schemaName);
        } else if (this.server.getSQLSystem().isDBPathEmpty()) {
            this.unsetInitialSchema();
        } else
            throw new IllegalArgumentException(this + " cannot have no default schema");
    }

    /**
     * From now on, connections won't have their default schema set by this. Of course the SQL
     * server might have set one.
     */
    public void unsetInitialSchema() {
        this.setInitialSchema(false, null);
    }

    private final void setInitialSchema(final boolean set, final String schemaName) {
        synchronized (this.setInitialShemaLock) {
            synchronized (this) {
                // even if schemaName no longer exists, and thus the following test would fail, the
                // next initConnection() will correctly fail
                if (this.initialShemaSet == set && CompareUtils.equals(this.initialShema, schemaName))
                    return;
            }
            final Connection newConn;
            if (set) {
                // test if schemaName is valid
                newConn = this.getNewConnection();
                try {
                    this.setSchema(schemaName, newConn);
                } catch (RuntimeException e) {
                    this.closeConnection(newConn);
                    throw e;
                }
                // don't return connection right now otherwise it might be deemed unrecoverable
            } else {
                newConn = null;
            }
            synchronized (this) {
                this.initialShemaSet = set;
                this.initialShema = schemaName;
                this.schemaUptodate.clear();
                if (!set)
                    // by definition we don't want to modify the connection,
                    // so empty the pool, that way new connections will be created
                    // the borrowed ones will be closed when returned
                    this.connectionPool.clear();
                else
                    this.schemaUptodate.put(newConn, null);
            }
            this.returnConnection(newConn);
        }
    }

    public synchronized final String getInitialSchema() {
        return this.initialShema;
    }

    /**
     * Set the default schema of the current thread's connection. NOTE: pointless if not in
     * {@link #useConnection(ConnectionHandler)} since otherwise a connection will be borrowed then
     * closed.
     * 
     * @param schemaName the name of the new default schema.
     */
    public void setSchema(String schemaName) {
        this.setSchema(schemaName, null);
    }

    private void setSchema(String schemaName, Connection c) {
        final String q;
        if (this.getSystem() == SQLSystem.MYSQL) {
            if (schemaName == null) {
                if (this.getSchema(c) != null)
                    throw new IllegalArgumentException("cannot unset DATABASE in MySQL");
                else
                    // nothing to do
                    q = null;
            } else
                q = "USE " + schemaName;
        } else if (this.getSystem() == SQLSystem.DERBY) {
            q = "SET SCHEMA " + SQLBase.quoteIdentifier(schemaName);
        } else if (this.getSystem() == SQLSystem.H2) {
            q = "SET SCHEMA " + SQLBase.quoteIdentifier(schemaName);
            // TODO use the line below, but for now it is only used after schema()
            // plus there's no function to read it back
            // q = "set SCHEMA_SEARCH_PATH " + SQLBase.quoteIdentifier(schemaName == null ? "" :
            // schemaName);
        } else if (this.getSystem() == SQLSystem.POSTGRESQL) {
            if (schemaName == null) {
                // SET cannot empty the path
                q = "select set_config('search_path', '', false)";
            } else {
                q = "set session search_path to " + SQLBase.quoteIdentifier(schemaName);
            }
        } else if (this.getSystem() == SQLSystem.MSSQL) {
            if (schemaName == null)
                throw new IllegalArgumentException("cannot unset default schema in " + this.getSystem());
            else
                q = "alter user " + getUsername() + " with default_schema = " + SQLBase.quoteIdentifier(schemaName);
        } else {
            throw new UnsupportedOperationException();
        }

        if (q != null)
            this.execute(q, null, true, c);
    }

    public final String getSchema() {
        return this.getSchema(null);
    }

    private String getSchema(Connection c) {
        final String q;
        if (this.getSystem() == SQLSystem.MYSQL)
            q = "select DATABASE(); ";
        else if (this.getSystem() == SQLSystem.DERBY)
            q = "select CURRENT SCHEMA;";
        else if (this.getSystem() == SQLSystem.POSTGRESQL) {
            q = "select (current_schemas(false))[1];";
        } else if (this.getSystem() == SQLSystem.H2) {
            q = "select SCHEMA();";
        } else if (this.getSystem() == SQLSystem.MSSQL) {
            q = "select SCHEMA_NAME();";
        } else
            throw new UnsupportedOperationException();

        return (String) this.execute(q, SCALAR_HANDLER, c);
    }

    public String toString() {
        return this.getUrl();
    }

    private final SQLSystem getSystem() {
        return this.server.getSQLSystem();
    }

    public Object clone() {
        SQLDataSource ds = new SQLDataSource(this.server);
        ds.setUrl(this.getUrl());
        ds.setUsername(this.getUsername());
        ds.setPassword(this.getPassword());
        ds.setDriverClassName(this.getDriverClassName());
        return ds;
    }
}
