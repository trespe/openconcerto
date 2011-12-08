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

import org.openconcerto.sql.view.list.ListAccess;
import org.openconcerto.sql.view.list.ListSQLLine;
import org.openconcerto.sql.view.search.SearchSpec;

import java.util.Collections;
import java.util.List;

abstract class SearchRunnable implements Runnable {

    private final SearchQueue q;

    public SearchRunnable(SearchQueue q) {
        this.q = q;
    }

    /**
     * Is this list filtered.
     * 
     * @return <code>true</code> if the filter is not empty.
     */
    protected final boolean isFiltered() {
        return this.getSearch() != null && !this.getSearch().isEmpty();
    }

    protected final boolean matchFilter(ListSQLLine line) {
        if (this.isFiltered())
            return this.matchFilterUnsafe(line);
        else
            return true;
    }

    // ATTN only call if this.isFiltered() otherwise it might throw NPE
    protected final boolean matchFilterUnsafe(ListSQLLine line) {
        // ne chercher que sur les colonnes affichées
        final int columnCount = this.q.getModel().getColumnCount();
        return this.getSearch().match(line.getList(columnCount).subList(0, columnCount));
    }

    public String toString() {
        return this.getClass().getSimpleName() + " on " + this.q;
    }

    protected final ListAccess getAccess() {
        return this.q.getAccess();
    }

    protected final SearchSpec getSearch() {
        return this.q.getSearch();
    }

    protected final List<ListSQLLine> getFullList() {
        return Collections.unmodifiableList(this.q.getFullList());
    }
}
