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
 
 package org.openconcerto.sql.navigator;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.ui.list.selection.BaseListStateModel;

public final class ListStateModel extends BaseListStateModel {

    static final int ALL_ID = INVALID_ID - 1;
    private final RowsSQLListModel model;

    public ListStateModel(RowsSQLListModel model) {
        this.model = model;
        this.model.addPropertyChangeListener(getUpdateL(), "updating");
    }

    public int idFromIndex(int rowIndex) {
        final SQLRow r = this.model.getElementAt(rowIndex);
        if (this.model.isALLValue(r))
            return ALL_ID;
        else
            return this.stateIDFromItem(r);
    }

    int stateIDFromItem(SQLRow r) {
        return r.getID();
    }

    public int indexFromID(int id) {
        // all is at the beginning
        if (id == ALL_ID)
            return 0;
        else
            return this.model.indexFromID(id);
    }

}
