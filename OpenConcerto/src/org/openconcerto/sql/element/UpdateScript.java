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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.SetMap;

import java.sql.SQLException;

/**
 * Apply updates to a row (including archiving obsolete privates).
 * 
 * @author Sylvain
 */
public final class UpdateScript {
    private final SQLRowValues updateRow;
    private final SetMap<SQLElement, SQLRowAccessor> toArchive;

    UpdateScript(final SQLTable t) {
        this.updateRow = new SQLRowValues(t);
        this.toArchive = new SetMap<SQLElement, SQLRowAccessor>();
    }

    final SQLRowValues getUpdateRow() {
        return this.updateRow;
    }

    final void addToArchive(SQLElement elem, SQLRowAccessor r) {
        this.toArchive.add(elem, r);
    }

    final void put(String field, UpdateScript s) {
        this.getUpdateRow().put(field, s.getUpdateRow());
        this.toArchive.merge(s.toArchive);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getUpdateRow() + " toArchive: " + this.toArchive;
    }

    public final void exec() throws SQLException {
        this.getUpdateRow().commit();
        for (final SQLElement elem : this.toArchive.keySet()) {
            for (final SQLRowAccessor v : this.toArchive.getNonNull(elem))
                elem.archive(v.getID());
        }
    }
}
