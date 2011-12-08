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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A class to encapsulate the use of a connection.
 * 
 * @author Sylvain
 * 
 * @param <T> type of return.
 * @param <X> type of exception.
 * @see SQLDataSource#useConnection(ConnectionHandler)
 */
public abstract class ConnectionHandler<T, X extends Exception> {

    private Exception exn;
    private T res;

    public ConnectionHandler() {
        this.res = null;
        this.exn = null;
    }

    @SuppressWarnings("unchecked")
    protected final T get() throws X, SQLException {
        if (this.exn != null) {
            if (this.exn instanceof RuntimeException)
                throw (RuntimeException) this.exn;
            else if (this.exn instanceof SQLException)
                // handle() throws SQLException
                throw (SQLException) this.exn;
            else
                // handle() throws X
                throw (X) this.exn;
        } else
            return this.res;
    }

    final void compute(final SQLDataSource ds) {
        try {
            this.res = this.handle(ds);
            this.exn = null;
        } catch (Exception e) {
            this.exn = e;
        }
    }

    /**
     * Should setup the state of <code>c</code> for {@link #handle(SQLDataSource)}.
     * 
     * @param c the connection to change.
     * @throws SQLException if an error occurs.
     */
    public abstract void setup(final Connection c) throws SQLException;

    // thrown exns must be rethrown in get()
    public abstract T handle(final SQLDataSource ds) throws SQLException, X;

    /**
     * Whether {@link #restoreState(Connection)} can restore the connection, as if this instance has
     * never used it.
     * 
     * @return <code>true</code> if the connection can be restored.
     */
    public abstract boolean canRestoreState();

    /**
     * Should restore <code>c</code> to its state when {@link #setup(Connection)} was called. NOTE
     * that this will be called even if {@link #setup(Connection)} throws an exception.
     * 
     * @param c the connection.
     * @throws SQLException if the state could not be restored.
     */
    public abstract void restoreState(final Connection c) throws SQLException;

}
