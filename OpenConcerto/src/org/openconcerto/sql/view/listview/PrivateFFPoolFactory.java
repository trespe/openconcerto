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
 
 package org.openconcerto.sql.view.listview;

import org.openconcerto.sql.model.SQLTable;

import java.util.List;

// ID_OBSERVATION,_2,_3
public final class PrivateFFPoolFactory extends FFPoolFactory {

    public PrivateFFPoolFactory(SQLTable t, String foreignT, int count, boolean firstSuffixed) {
        super(t, foreignT, count, firstSuffixed);
    }

    public PrivateFFPoolFactory(SQLTable t, String foreignT, List fields) {
        super(t, foreignT, fields);
    }

    public PrivateFFPoolFactory(SQLTable t, String foreignT) {
        super(t, foreignT);
    }

    public ItemPool create(ListSQLView panel) {
        return new PrivateFFItemPool(this, panel);
    }

}
