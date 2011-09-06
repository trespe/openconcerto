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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.request.BaseFillSQLRequest;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.cc.ITransformer;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

abstract class AbstractUpdateOneRunnable extends UpdateRunnable {

    public AbstractUpdateOneRunnable(ITableModel model, SQLTable table, int id) {
        super(model, table, id);
        if (this.getID() < SQLRow.MIN_VALID_ID)
            throw new IllegalArgumentException("id is not valid : " + this.getID());
    }

    protected final CollectionMap<Path, ListSQLLine> getAffectedPaths() {
        return this.getSearchQ().getAffectedPaths(this.getTable(), this.getID());
    }

    protected final void updateLines(CollectionMap<Path, ListSQLLine> paths) {
        for (final Entry<Path, Collection<ListSQLLine>> e : paths.entrySet()) {
            // eg SITE.ID_CONTACT_CHEF
            final Path p = e.getKey();
            // eg [SQLRowValues(SITE), SQLRowValues(SITE)]
            final List<ListSQLLine> lines = (List<ListSQLLine>) e.getValue();
            if (!lines.isEmpty()) {
                // deepCopy() instead of new SQLRowValues() otherwise the used line's graph will be
                // modified (eg the new instance would be linked to it)
                final SQLRowValues proto = getModel().getLinesSource().getParent().getMaxGraph().followPath(p).deepCopy();
                // keep only what has changed, eg CONTACT.NOM
                proto.retainAll(getModifedFields());
                // fetch the changed rowValues
                // ATTN this doesn't use the original fetcher that was used in the updateAll
                // MAYBE add a slower but accurate mode using the updateAll fetcher (and thus
                // reloading rows from the primary table and not just the changed rows)
                final SQLRowValuesListFetcher fetcher = new SQLRowValuesListFetcher(proto);
                BaseFillSQLRequest.setupForeign(fetcher);
                final ITransformer<SQLSelect, SQLSelect> transf = new ITransformer<SQLSelect, SQLSelect>() {
                    @Override
                    public SQLSelect transformChecked(SQLSelect input) {
                        if (ListSQLRequest.lockSelect)
                            input.addWaitPreviousWriteTXTable(getTable().getName());
                        return input.setWhere(new Where(getTable().getKey(), "=", getID()));
                    }
                };
                fetcher.setSelTransf(transf);
                final List<SQLRowValues> fetched = fetcher.fetch();
                if (fetched.size() > 1)
                    throw new IllegalStateException("more than one row fetched for " + this + " with " + fetcher.getReq() + " :\n" + fetched);

                if (fetched.size() == 0) {
                    Log.get().fine("no row fetched for " + this + ", lines have been changed without the TableModel knowing : " + lines + " req :\n" + fetcher.getReq());
                    getModel().updateAll();
                } else {
                    final SQLRowValues soleFetched = fetched.get(0);
                    // copy it to each affected lines
                    for (final ListSQLLine line : lines) {
                        line.loadAt(soleFetched.deepCopy(), p);
                    }
                }
            }
        }
    }

    protected abstract Collection<String> getModifedFields();
}
