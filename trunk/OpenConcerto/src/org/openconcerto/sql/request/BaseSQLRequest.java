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
 
 package org.openconcerto.sql.request;

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTableModifiedListener;

import java.util.Collection;
import java.util.Set;

abstract class BaseSQLRequest {

    public BaseSQLRequest() {
        super();
    }

    public final Set<SQLTable> getTables() {
        return new SQLFieldsSet(this.getAllFields()).getTables();
    }

    /**
     * Tous les champs qui intéressent cette requête. Souvent les champs après expansion.
     * 
     * @return les champs qui intéressent cette requête.
     */
    protected abstract Collection<SQLField> getAllFields();

    public final void addTableListener(SQLTableModifiedListener l) {
        for (final SQLTable t : this.getTables()) {
            t.addTableModifiedListener(l);
        }
    }

    public final void removeTableListener(SQLTableModifiedListener l) {
        for (final SQLTable t : this.getTables()) {
            t.removeTableModifiedListener(l);
        }
    }
}
