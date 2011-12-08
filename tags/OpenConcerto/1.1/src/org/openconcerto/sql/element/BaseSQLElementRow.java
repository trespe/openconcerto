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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;

abstract class BaseSQLElementRow {

    private static final SQLElement getElementStatic(SQLRow r) {
        return Configuration.getInstance().getDirectory().getElement(r.getTable());
    }

    private final SQLElement elem;
    private final SQLRow row;

    public BaseSQLElementRow(SQLRow row) {
        this(getElementStatic(row), row);
    }

    public BaseSQLElementRow(SQLElement element, SQLRow row) {
        if (!row.getTable().equals(element.getTable()))
            throw new IllegalArgumentException(row.getTable() + " != " + element.getTable());
        this.elem = element;
        this.row = row;
    }

    public int hashCode() {
        // le seul attribut qui est à coup sûr égal
        return this.row.getTable().hashCode();
    }

    public abstract boolean equals(Object o);

    public String toString() {
        return this.row + " (" + this.elem + ")";
    }

    protected final SQLElement getElem() {
        return this.elem;
    }

    protected final SQLRow getRow() {
        return this.row;
    }

    protected final SQLElement getElement(SQLRow r) {
        return getElementStatic(r);
    }

}
