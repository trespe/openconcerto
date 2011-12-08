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
 
 package org.openconcerto.sql.view.list.search;

import org.openconcerto.sql.view.list.ListSQLLine;

import java.util.Collection;

import javax.swing.SwingUtilities;

public final class SearchOne extends SearchRunnable {

    public enum Mode {
        ADD, REMOVE, CHANGE, NO_CHANGE
    };

    final ListSQLLine modifiedLine;
    final Collection<Integer> modifiedIndex;
    final int id;

    private Mode mode;

    public SearchOne(SearchQueue q, int id, ListSQLLine modifiedLine, final Collection<Integer> modifiedIndex) {
        super(q);
        this.id = id;
        this.modifiedLine = modifiedLine;
        this.modifiedIndex = modifiedIndex;

        this.mode = null;
    }

    public synchronized void setMode(Mode m) {
        if (this.mode != null)
            throw new IllegalStateException("mode not null");
        this.mode = m;
    }

    public void run() {
        synchronized (this) {
            if (this.mode == null) {
                throw new IllegalStateException("null mode");
            }
        }

        if (this.mode == Mode.ADD) {
            if (this.matchFilter(this.modifiedLine))
                add();
        } else if (this.mode == Mode.REMOVE) {
            remove();
        } else if (this.mode == Mode.CHANGE) {
            if (this.matchFilter(this.modifiedLine))
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        SearchOne.this.getAccess().fullListChanged(SearchOne.this.modifiedLine, SearchOne.this.modifiedIndex);
                    }
                });
            else
                remove();
        }
    }

    private void add() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SearchOne.this.getAccess().addToList(SearchOne.this.modifiedLine);
            }
        });
    }

    private void remove() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                SearchOne.this.getAccess().removeFromList(SearchOne.this.id);
            }
        });
    }

}
