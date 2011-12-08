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
import java.util.LinkedList;

class HandlersStack {
    private Connection conn;
    private final LinkedList<ConnectionHandler<?, ?>> stack;
    private boolean changeAllowed;

    HandlersStack(final Connection conn, final ConnectionHandler<?, ?> handler) {
        super();
        if (conn == null)
            throw new NullPointerException("null connection");
        this.changeAllowed = false;
        this.conn = conn;
        this.stack = new LinkedList<ConnectionHandler<?, ?>>();
        this.push(handler);
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

    public final boolean isChangeAllowed() {
        return this.changeAllowed;
    }

    final void setChangeAllowed(boolean b) {
        this.changeAllowed = b;
    }
}
