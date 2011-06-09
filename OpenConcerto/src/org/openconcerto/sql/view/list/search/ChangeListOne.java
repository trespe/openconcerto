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
import org.openconcerto.sql.view.list.search.SearchOne.Mode;

import java.util.Collections;

final class ChangeListOne extends ChangeListRunnable {
    private final ListSQLLine line;
    private final SearchOne search;
    private final int id;

    ChangeListOne(String name, SearchQueue q, ListSQLLine line, int id, SearchOne runnable) {
        super(name, q);
        this.line = line;
        this.id = id;
        this.search = runnable;
    }

    public void run() {
        synchronized (this.getFullList()) {
            final int modifiedIndex = this.fullIndexFromID(this.id);

            if (modifiedIndex < 0) {
                // la ligne n'était dans notre liste
                if (this.line != null) {
                    // mais elle existe : ajout
                    // ATTN on ajoute à la fin, sans se soucier de l'ordre
                    this.getFullList().add(this.line);
                    Collections.sort(this.getFullList());
                    this.search.setMode(Mode.ADD);
                } else {
                    // et elle n'y est toujours pas
                    this.search.setMode(Mode.NO_CHANGE);
                }
            } else {
                // la ligne était dans notre liste
                if (this.line != null) {
                    // mettre à jour
                    this.getFullList().set(modifiedIndex, this.line);
                    Collections.sort(this.getFullList());
                    this.search.setMode(Mode.CHANGE);
                } else {
                    // elle est effacée ou filtrée
                    this.getFullList().remove(modifiedIndex);
                    this.search.setMode(Mode.REMOVE);
                }
            }
        }
    }
}
