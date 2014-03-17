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

import java.util.List;

/**
 * An UpdateRunnable that modifies the whole list. Ie any prior update is useless.
 * 
 * @author Sylvain
 */
abstract class ChangeAllRunnable extends UpdateRunnable {

    public ChangeAllRunnable(ITableModel model) {
        super(model, new SQLRow(model.getTable(), SQLRow.NONEXISTANT_ID));
    }

    public final void run() {
        final List<ListSQLLine> tmp = getList();
        if (tmp != null)
            this.getSearchQ().setFullList(tmp);
        this.done();
    }

    /**
     * The new list.
     * 
     * @return the new list, or <code>null</code> to do nothing.
     */
    protected abstract List<ListSQLLine> getList();

    protected void done() {
    }
}
