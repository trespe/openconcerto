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

import org.openconcerto.sql.request.SQLCache;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.LinkedList;
import java.util.List;

class HandlersStack {
    private final SQLDataSource ds;
    private Connection conn;
    private final LinkedList<ConnectionHandler<?, ?>> stack;
    private boolean changeAllowed;
    // list of transaction points, i.e. first a transaction start and then any number of save
    // points. The list is thus empty in auto-commit mode.
    private final LinkedList<TransactionPoint> txPoints;
    // the cache for each point, items can be null if no cache should be used
    private final LinkedList<SQLCache<List<?>, Object>> caches;

    HandlersStack(final SQLDataSource ds, final Connection conn, final ConnectionHandler<?, ?> handler) {
        super();
        if (conn == null)
            throw new NullPointerException("null connection");
        this.ds = ds;
        this.changeAllowed = false;
        this.conn = conn;
        this.stack = new LinkedList<ConnectionHandler<?, ?>>();
        this.push(handler);
        this.txPoints = new LinkedList<TransactionPoint>();
        this.caches = new LinkedList<SQLCache<List<?>, Object>>();
    }

    public final Connection getConnection() throws IllegalStateException {
        if (this.conn == null)
            throw new IllegalStateException("connection was invalidated");
        return this.conn;
    }

    final void invalidConnection() {
        this.conn = null;
    }

    final HandlersStack push(final ConnectionHandler<?, ?> handler) {
        this.stack.addFirst(handler);
        return this;
    }

    /**
     * Remove the last added ConnectionHandler.
     * 
     * @return <code>true</code> if this is now empty.
     */
    final boolean pop() {
        this.stack.removeFirst();
        return this.stack.isEmpty();
    }

    final void addTxPoint(TransactionPoint txPoint) {
        if (txPoint.getConn() != this.conn)
            throw new IllegalArgumentException("Different connections");
        // the first point should be a transaction start
        assert this.stack.size() > 0 || txPoint.getSavePoint() == null;
        this.addCache();
        this.txPoints.add(txPoint);
    }

    private void addCache() {
        final SQLCache<List<?>, Object> previous = this.getCache();
        final SQLCache<List<?>, Object> current = this.ds.createCache(this);
        this.caches.add(current);
        if (current != null) {
            if (previous != null) {
                current.setParent(previous);
            } else {
                // MAYBE if transaction start, set parent to DS cache for READ_COMMITTED, copy of DS
                // for REPEATABLE_READ and SERIALIZABLE
            }
        }
    }

    private final void removeLastCache() {
        // throws NoSuchElementException, i.e. if last is null it's because the item was null
        final SQLCache<List<?>, Object> last = this.caches.removeLast();
        if (last != null)
            last.clear();
    }

    void updateCache() {
        final int size = this.txPoints.size();
        // cache only needed for transactions
        if (size > 0) {
            clearCache();
            for (int i = 0; i < size - 1; i++) {
                this.caches.add(null);
            }
            this.addCache();
        }
        assert size == this.caches.size();
    }

    private final void clearCache() {
        while (!this.caches.isEmpty()) {
            removeLastCache();
        }
    }

    final TransactionPoint getLastTxPoint() {
        return this.txPoints.peekLast();
    }

    final SQLCache<List<?>, Object> getCache() {
        return this.caches.peekLast();
    }

    private final TransactionPoint removeFirstTxPoint() {
        return this.txPoints.pollFirst();
    }

    private final TransactionPoint removeLastTxPoint() {
        return this.txPoints.pollLast();
    }

    void commit() throws SQLException {
        TransactionPoint txPoint = this.removeFirstTxPoint();
        while (txPoint != null) {
            txPoint.fire(true);
            txPoint = this.removeFirstTxPoint();
        }
        // don't bother trying to merge the cache
        clearCache();
    }

    void rollback() throws SQLException {
        TransactionPoint txPoint = this.removeLastTxPoint();
        while (txPoint != null) {
            txPoint.fire(false);
            txPoint = this.removeLastTxPoint();
        }
        // discard the cache
        clearCache();
    }

    void rollback(Savepoint savepoint) throws SQLException {
        TransactionPoint txPoint = this.removeLastTxPoint();
        while (txPoint.getSavePoint() != savepoint) {
            txPoint.fire(false);
            // discard the cache
            removeLastCache();
            txPoint = this.removeLastTxPoint();
        }
        txPoint.fire(false);
        // discard the cache
        removeLastCache();
        assert this.txPoints.size() == this.caches.size();
    }

    public final boolean isChangeAllowed() {
        return this.changeAllowed;
    }

    final void setChangeAllowed(boolean b) {
        this.changeAllowed = b;
    }
}
