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

import java.util.Collections;
import java.util.List;

final class ChangeListAll extends ChangeListRunnable {
    private final List<ListSQLLine> l;

    ChangeListAll(String name, SearchQueue q, List<ListSQLLine> l) {
        super(name, q);
        this.l = l;
    }

    public void run() {
        this.getFullList().clear();
        this.getFullList().addAll(this.l);
        // MAYBE order the SELECT to avoid sort()
        // but comparing ints (field ORDRE) is quite fast : 170ms for 100,000 items
        Collections.sort(this.getFullList());
    }
}
