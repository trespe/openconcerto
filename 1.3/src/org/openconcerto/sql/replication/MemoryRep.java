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
 
 package org.openconcerto.sql.replication;

import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.IResultSetHandler;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.CSVHandler;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.SQLCreateMoveableTable;
import org.openconcerto.sql.utils.SQLCreateRoot;
import org.openconcerto.sql.utils.SQLCreateTableBase;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.ChangeTable.FCSpec;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.RTInterruptedException;
import org.openconcerto.utils.ThreadFactory;
import org.openconcerto.utils.cc.IClosure;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Allow to replicate some tables in memory.
 * 
 * @author Sylvain
 */
@ThreadSafe
public class MemoryRep {

    static final short MAX_CANCELED = 10;
    static private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(new ThreadFactory(MemoryRep.class.getName(), true));

    // final thread-safe objects
    private final DBSystemRoot master, slave;
    // final immutable
    private final TablesMap tables;
    // final immutable
    private final String singleRootName;
    @GuardedBy("this")
    private ScheduledFuture<?> future;
    @GuardedBy("this")
    private Future<?> manualFuture;
    @GuardedBy("this")
    private short canceledCount;
    // final thread-safe object
    private final AtomicInteger count;

    public MemoryRep(final SQLTable table) {
        this(table.getDBSystemRoot(), TablesMap.createByRootFromTable(table));
    }

    public MemoryRep(final DBSystemRoot master, final TablesMap tables) {
        this.master = master;
        this.tables = TablesMap.create(tables);
        if (this.tables.size() == 1) {
            this.singleRootName = this.tables.keySet().iterator().next();
            if (this.singleRootName == null)
                throw new IllegalStateException();
        } else {
            this.singleRootName = null;
        }
        // private in-memory database
        this.slave = new SQLServer(SQLSystem.H2, "mem", null, null, null, new IClosure<DBSystemRoot>() {
            @Override
            public void executeChecked(DBSystemRoot input) {
                input.setRootsToMap(tables.keySet());
                // don't waste time on cache for transient data
                input.initUseCache(false);
            }
        }, new IClosure<SQLDataSource>() {
            @Override
            public void executeChecked(SQLDataSource input) {
                // one and only one connection since base is private
                input.setInitialSize(1);
                input.setMaxActive(1);
                input.setMinIdle(0);
                input.setMaxIdle(1);
                input.setTimeBetweenEvictionRunsMillis(-1);
                input.setBlockWhenExhausted(true);
                // allow to break free (by throwing an exception) of deadlocks :
                // * in replicateData() we take the one and only connection and eventually need the
                // lock on structure items (schema and tables)
                // * some other thread takes the lock on a structure item and then tries to execute
                // a query, waiting on the one and only connection.
                // Three minutes should be enough, since we only load data from files into memory,
                // and our clients can only do SELECTs
                input.setMaxWait(TimeUnit.MINUTES.toMillis(3));
            }
        }).getSystemRoot("");
        // slave is a copy so it needn't have checks (and it simplify replicate())
        this.slave.getDataSource().execute(this.slave.getServer().getSQLSystem().getSyntax().disableFKChecks(null));
        this.count = new AtomicInteger(0);
        this.canceledCount = 0;
    }

    /**
     * Start the automatic replication. When this method returns the structure is copied (
     * {@link #getSlaveTable(String, String)} works), but not the data : use the returned future
     * before accessing the data.
     * 
     * @param period the period between successive replications.
     * @param unit the time unit of the period parameter.
     * @return a future representing the pending data replication.
     * @throws InterruptedException if the creation of structure was interrupted.
     * @throws ExecutionException if the creation of structure has failed.
     */
    public synchronized final Future<?> start(final long period, final TimeUnit unit) throws InterruptedException, ExecutionException {
        if (this.future != null) {
            if (this.future.isCancelled())
                throw new IllegalStateException("Already stopped");
            else
                throw new IllegalStateException("Already started");
        }
        exec.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                replicateStruct();
                return null;
            }
        }).get();
        final Future<?> res = submitReplicate();
        // start after period since we just submitted a replicate()
        this.future = exec.scheduleAtFixedRate(getRunnable(), period, period, unit);
        return res;
    }

    public synchronized final boolean hasStopped() {
        return this.future != null && this.future.isCancelled();
    }

    /**
     * Stop the replication. It can not be started again.
     * 
     * @return a future representing the pending structure deletion, or <code>null</code> if this
     *         wasn't started or already stopped.
     */
    public final Future<?> stop() {
        synchronized (this) {
            if (this.future == null || this.future.isCancelled())
                return null;
            this.future.cancel(true);
            this.manualFuture.cancel(true);
        }
        // use exec to be sure not to destroy the server before replicate() notices the interruption
        return exec.submit(new Runnable() {
            @Override
            public void run() {
                MemoryRep.this.slave.getServer().destroy();
            }
        });
    }

    public final DBSystemRoot getSlave() {
        return this.slave;
    }

    private final void checkTable(final String root, final String tableName) {
        if (!this.tables.containsKey(root))
            throw new IllegalArgumentException("Root not replicated : " + root + " " + tableName);
        if (!this.tables.get(root).contains(tableName))
            throw new IllegalArgumentException("Table not replicated : " + root + " " + tableName);
    }

    public final SQLTable getMasterTable(final String tableName) {
        return this.getMasterTable(this.singleRootName, tableName);
    }

    public final SQLTable getMasterTable(final String root, final String tableName) {
        checkTable(root, tableName);
        return this.master.getRoot(root).getTable(tableName);
    }

    public final SQLTable getSlaveTable(final String tableName) {
        return this.getSlaveTable(this.singleRootName, tableName);
    }

    public final SQLTable getSlaveTable(final String root, final String tableName) {
        checkTable(root, tableName);
        return this.slave.getRoot(root).getTable(tableName);
    }

    private final Runnable getRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    replicateData();
                } catch (Exception e) {
                    // TODO keep it to throw it elsewhere (scheduleAtFixedRate() cannot use
                    // Callable)
                    e.printStackTrace();
                }

            }
        };
    }

    /**
     * Force a manual replication.
     * 
     * @return a future representing the pending data replication.
     */
    public synchronized final Future<?> submitReplicate() {
        final boolean canceled;
        // make sure we don't cancel all tasks
        if (this.manualFuture != null && this.canceledCount < MAX_CANCELED) {
            // false if already canceled or done
            canceled = this.manualFuture.cancel(true);
        } else {
            canceled = false;
        }
        if (canceled)
            this.canceledCount++;
        else
            this.canceledCount = 0;
        this.manualFuture = exec.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                replicateData();
                return null;
            }
        });
        return this.manualFuture;
    }

    private final synchronized Future<?> getManualFuture() {
        return this.manualFuture;
    }

    /**
     * Wait on the last submitted manual replication.
     * 
     * @throws ExecutionException if the computation threw an exception.
     * @throws InterruptedException if the current thread was interrupted while waiting.
     * @see #submitReplicate()
     * @see #executeModification(IClosure)
     */
    public final void waitOnLastManualFuture() throws InterruptedException, ExecutionException {
        // don't return the future, that way only the caller of submitReplicate() can cancel it
        Future<?> f = getManualFuture();
        boolean done = false;
        while (!done) {
            try {
                f.get();
                done = true;
            } catch (CancellationException e) {
                if (hasStopped()) {
                    done = true;
                } else {
                    // canceled by the caller of submitReplicate() or by a new submitReplicate()
                    // since f was canceled, data isn't up to date, so see if we can wait on a more
                    // recent update
                    final Future<?> old = f;
                    f = getManualFuture();
                    done = old == f;
                }
                if (done)
                    throw e;
            }
        }
    }

    protected final void replicateStruct() throws SQLException, IOException {
        final SQLSystem slaveSystem = this.slave.getServer().getSQLSystem();
        final SQLDataSource slaveDS = this.slave.getDataSource();
        final List<SQLCreateTableBase<?>> createTables = new ArrayList<SQLCreateTableBase<?>>();
        // undefined IDs by table by root
        final Map<String, Map<String, Number>> undefIDs = new HashMap<String, Map<String, Number>>();
        for (final Entry<String, Set<String>> e : this.tables.entrySet()) {
            final String rootName = e.getKey();
            final Set<String> tableNames = e.getValue();
            slaveDS.execute(new SQLCreateRoot(slaveSystem.getSyntax(), rootName).asString());
            final DBRoot root = this.master.getRoot(rootName);

            final Map<String, Number> rootUndefIDs = new HashMap<String, Number>(tableNames.size());
            undefIDs.put(rootName, rootUndefIDs);

            for (final String tableName : tableNames) {
                final SQLTable masterTable = root.getTable(tableName);
                final SQLCreateMoveableTable ct = masterTable.getCreateTable(slaveSystem);
                // remove constraints towards non-copied tables
                for (final FCSpec fc : new ArrayList<FCSpec>(ct.getForeignConstraints())) {
                    final SQLName refTable = new SQLName(rootName, tableName).resolve(fc.getRefTable());
                    final String refTableName = refTable.getItem(-1);
                    final String refRootName = refTable.getItem(-2);
                    if (!this.tables.containsKey(refRootName) || !this.tables.get(refRootName).contains(refTableName))
                        ct.removeForeignConstraint(fc);
                }
                createTables.add(ct);
                rootUndefIDs.put(tableName, masterTable.getUndefinedIDNumber());
            }
        }
        // refresh empty roots
        this.slave.refetch();
        // set undefined IDs
        for (final Entry<String, Map<String, Number>> e : undefIDs.entrySet()) {
            final SQLSchema schema = this.slave.getRoot(e.getKey()).getSchema();
            SQLTable.setUndefIDs(schema, e.getValue());
        }
        // create tables
        for (final String s : ChangeTable.cat(createTables))
            slaveDS.execute(s);
        // final refresh
        this.slave.refetch();
    }

    // only called from the executor
    protected final void replicateData() throws SQLException, IOException, InterruptedException {
        final SQLSyntax slaveSyntax = SQLSyntax.get(this.slave);
        final File tempDir = FileUtils.createTempDir(getClass().getCanonicalName() + "_StoreData");
        try {
            final List<String> queries = new ArrayList<String>();
            final List<ResultSetHandler> handlers = new ArrayList<ResultSetHandler>();
            final Map<File, SQLTable> files = new HashMap<File, SQLTable>();
            for (final Entry<String, Set<String>> e : this.tables.entrySet()) {
                if (Thread.interrupted())
                    throw new InterruptedException("While creating handlers");
                final String rootName = e.getKey();
                final File rootDir = new File(tempDir, rootName);
                FileUtils.mkdir_p(rootDir);
                final DBRoot root = this.master.getRoot(rootName);
                final DBRoot slaveRoot = this.slave.getRoot(rootName);
                for (final String tableName : e.getValue()) {
                    final SQLTable masterT = root.getTable(tableName);
                    final SQLSelect select = new SQLSelect(true).addSelectStar(masterT);
                    queries.add(select.asString());
                    // don't use cache to be sure to have up to date data
                    handlers.add(new IResultSetHandler(new ResultSetHandler() {

                        private final CSVHandler csvH = new CSVHandler(masterT.getOrderedFields());

                        @Override
                        public Object handle(ResultSet rs) throws SQLException {
                            final File tempFile = new File(rootDir, FileUtils.FILENAME_ESCAPER.escape(tableName) + ".csv");
                            assert !tempFile.exists();
                            try {
                                FileUtils.write(this.csvH.handle(rs), tempFile);
                                files.put(tempFile, slaveRoot.getTable(tableName));
                            } catch (IOException e) {
                                throw new SQLException(e);
                            }
                            return null;
                        }
                    }, false));
                }
            }
            try {
                SQLUtils.executeAtomic(this.master.getDataSource(), new ConnectionHandlerNoSetup<Object, SQLException>() {
                    @Override
                    public Object handle(SQLDataSource ds) throws SQLException {
                        SQLUtils.executeMultiple(MemoryRep.this.master, queries, handlers);
                        return null;
                    }
                });
            } catch (RTInterruptedException e) {
                final InterruptedException exn = new InterruptedException("Interrupted while querying the master");
                exn.initCause(e);
                throw exn;
            }
            SQLUtils.executeAtomic(this.slave.getDataSource(), new ConnectionHandlerNoSetup<Object, IOException>() {
                @Override
                public Object handle(SQLDataSource ds) throws SQLException, IOException {
                    for (final Entry<File, SQLTable> e : files.entrySet()) {
                        final SQLTable slaveT = e.getValue();
                        // loadData() fires table modified
                        slaveSyntax.loadData(e.getKey(), slaveT, true);
                    }
                    return null;
                }
            });
            this.count.incrementAndGet();
        } finally {
            FileUtils.rm_R(tempDir);
        }
    }

    final int getCount() {
        return this.count.get();
    }

    public Future<?> executeModification(final IClosure<SQLDataSource> cl) {
        // change master
        cl.executeChecked(this.master.getDataSource());
        // update slave
        return submitReplicate();
    }
}
