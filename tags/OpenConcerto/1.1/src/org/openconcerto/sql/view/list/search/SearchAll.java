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

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

public final class SearchAll extends SearchRunnable {

    public SearchAll(SearchQueue q) {
        super(q);
    }

    public void run() {
        // at first we have nothing to search
        if (this.getFullList() != null) {
            final List<ListSQLLine> newList = this.filter(this.getFullList());
            if (newList != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        SearchAll.this.getAccess().setList(newList);
                    }
                });
            }
        }
    }

    /**
     * Filter a list.
     * 
     * @param tmp the list to filter.
     * @return the filtered list, or <code>null</code> if interrupted.
     */
    private List<ListSQLLine> filter(List<ListSQLLine> tmp) {
        if (tmp == null)
            throw new NullPointerException();
        List<ListSQLLine> res;
        if (!isFiltered())
            res = new ArrayList<ListSQLLine>(tmp);
        else {
            res = new ArrayList<ListSQLLine>(tmp.size());
            for (final ListSQLLine line : tmp) {
                // clear the interrupt flag
                if (Thread.interrupted()) {
                    res = null;
                    break;
                }
                if (this.matchFilterUnsafe(line))
                    res.add(line);
            }
        }
        return res;
    }

}
