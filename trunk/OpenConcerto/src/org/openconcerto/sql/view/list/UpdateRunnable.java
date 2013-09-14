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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.view.list.search.SearchQueue;

import java.util.Collections;
import java.util.List;

abstract class UpdateRunnable implements Runnable {

    static final class RmAllRunnable extends ChangeAllRunnable {
        private final UpdateQueue updateQueue;

        private RmAllRunnable(ITableModel model, UpdateQueue updateQueue) {
            super(model);
            this.updateQueue = updateQueue;
        }

        @Override
        protected List<ListSQLLine> getList() {
            // clear the list
            return Collections.emptyList();
        }

        @Override
        protected void done() {
            this.updateQueue.setSleeping(true);
        }
    }

    static UpdateRunnable create(ITableModel model) {
        return new UpdateAllRunnable(model);
    }

    static UpdateRunnable createRmAll(final UpdateQueue updateQueue, ITableModel tableModel) {
        return new RmAllRunnable(tableModel, updateQueue);
    }

    static UpdateRunnable create(ITableModel model, SQLTableEvent evt) {
        return new UpdateOneRunnable(model, evt);
    }

    private final ITableModel model;
    private final SQLRow row;

    protected UpdateRunnable(final ITableModel model, final SQLRow r) {
        this.model = model;
        this.row = r;
    }

    @Override
    public final String toString() {
        return this.getClass().getSimpleName() + "@" + this.hashCode() + " " + this.getTable() + "[" + this.getID() + "] on " + this.model;
    }

    protected final ITableModel getModel() {
        return this.model;
    }

    protected final SQLTableModelLinesSource getReq() {
        return this.getModel().getLinesSource();
    }

    protected final SearchQueue getSearchQ() {
        return this.getModel().getSearchQueue();
    }

    protected final SQLRow getRow() {
        return this.row;
    }

    protected final SQLTable getTable() {
        return this.getRow().getTable();
    }

    protected final int getID() {
        return this.getRow().getID();
    }
}
