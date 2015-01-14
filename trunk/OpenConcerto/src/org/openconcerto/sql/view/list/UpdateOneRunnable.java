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

import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.ListMap;

import java.util.Collection;

final class UpdateOneRunnable extends AbstractUpdateOneRunnable {
    private final SQLTableEvent evt;

    public UpdateOneRunnable(ITableModel model, SQLTableEvent evt) {
        super(model, evt.getRow());
        this.evt = evt;
    }

    @Override
    public void run() {
        if (this.getTable() == this.getReq().getParent().getPrimaryTable()) {
            final ListSQLLine line = this.getReq().get(this.getID());
            // handle deleted rows (i.e. line == null) by using this.getID(), must be done before
            // finding lines
            if (line == null)
                this.getReq().fireLineChanged(this.getID(), line, null);
            final ListMap<Path, ListSQLLine> affectedPaths = this.getAffectedPaths();
            // the line should be in the list (since SQLTableModelLinesSource.get()
            // returned it), so if not yet part of the list add it.
            if (line != null && affectedPaths.getNonNull(Path.get(getTable())).isEmpty())
                line.clearCache();
            // then, update affectedPaths (it's not because the changed table is the primary
            // table, that it's not also referenced, e.g. CIRCUIT.ORIGINE)
            updateLines(affectedPaths);
        } else {
            // eg CONTACT[3] has changed
            updateLines(this.getAffectedPaths());
        }
    }

    @Override
    protected Collection<String> getModifedFields() {
        return this.evt.getFieldNames();
    }
}
