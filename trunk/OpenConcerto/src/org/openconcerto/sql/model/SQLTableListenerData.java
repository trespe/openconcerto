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

final class SQLTableListenerData<R extends SQLRowAccessor> implements SQLTableModifiedListener {

    private final R row;
    private final SQLDataListener l;

    SQLTableListenerData(R row, SQLDataListener l) {
        this.row = row;
        this.l = l;
    }

    @Override
    public void tableModified(SQLTableEvent evt) {
        final int id = evt.getId();
        // if the row id was cached as non-existent and evt mode is ADDED, now it is
        if (id < SQLRow.MIN_VALID_ID || this.row.getID() == id)
            this.l.dataChanged();
    }
}
