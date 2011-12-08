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

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.ExceptionUtils;

import java.sql.SQLException;

class SQLElementRow extends BaseSQLElementRow {

    public static boolean equals(SQLRow r1, SQLRow r2) {
        return new SQLElementRow(r1).equals(new SQLElementRow(r2));
    }

    public SQLElementRow(SQLRow row) {
        super(row);
    }

    public SQLElementRow(SQLElement element, SQLRow row) {
        super(element, row);
    }

    public boolean equals(Object obj) {
        if (obj instanceof SQLElementRow) {
            final SQLElementRow o = (SQLElementRow) obj;
            try {
                return this.getElem().equals(o.getElem()) && this.getElem().equals(this.getRow(), o.getRow());
            } catch (SQLException e) {
                throw ExceptionUtils.createExn(RuntimeException.class, "error while comparing " + this + " and " + o, e);
            }
        } else
            return false;
    }

}
