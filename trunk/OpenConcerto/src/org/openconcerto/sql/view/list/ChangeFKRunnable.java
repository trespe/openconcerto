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
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.ListMap;

import java.util.Collection;

final class ChangeFKRunnable extends AbstractUpdateOneRunnable {

    private final ListSQLLine l;
    private final Path p;

    /**
     * Change the foreign key at the end of the passed path to point to <code>id</code>. Furthermore
     * it fetch the new id from the DB and updates all other lines that are affected.
     * 
     * @param l the line having a foreign key changed.
     * @param p the path to the foreign key, e.g. RECEPTEUR.ID_OBSERVATION.ID_TENSION.
     * @param id the new id.
     */
    public ChangeFKRunnable(ListSQLLine l, Path p, int id) {
        super(l.getSrc().getModel(), new SQLRow(p.getLast(), id));
        this.l = l;
        this.p = p;
    }

    public void run() {
        final ListMap<Path, ListSQLLine> affectedPaths = getAffectedPaths();
        // updateLines() do not check previous ID, it just load the new values at the specified path
        // thus we just add our path to the affected paths and it will change to the new ID.
        affectedPaths.add(this.p, this.l);
        updateLines(affectedPaths);
    }

    @Override
    protected Collection<String> getModifedFields() {
        return null;
    }

}
