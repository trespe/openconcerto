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
 
 package org.openconcerto.sql.changer.correct;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.sql.SQLException;

/**
 * Delete unreferenced children.
 * 
 * @author Sylvain
 */
public class DeleteOrphans extends Changer<SQLTable> {

    public DeleteOrphans(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected void changeImpl(final SQLTable t) throws SQLException {
        if (Configuration.getInstance() == null || Configuration.getInstance().getDirectory() == null)
            throw new IllegalStateException("no directory");

        getStream().print(t);
        final SQLElement elem = Configuration.getInstance().getDirectory().getElement(t);
        if (elem == null) {
            getStream().println(" : no element");
            return;
        }

        if (elem.getParentForeignField() != null) {
            final SQLTable elemT = elem.getTable();
            final SQLField parentF = elemT.getField(elem.getParentForeignField());
            final Where undefParent = new Where(parentF, "=", parentF.getForeignTable().getUndefinedID());
            final Where undefW = new Where(elemT.getKey(), "!=", elemT.getUndefinedID());
            getDS().execute("DELETE from " + elemT.getSQLName().quote() + " where " + undefParent.and(undefW));
            getStream().println(" done");
        } else {
            getStream().println(" no parent");
        }
    }
}
