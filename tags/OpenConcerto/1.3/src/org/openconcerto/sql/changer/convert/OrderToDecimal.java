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
 
 package org.openconcerto.sql.changer.convert;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;

import java.sql.SQLException;

public class OrderToDecimal extends Changer<SQLTable> {

    public OrderToDecimal(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected void changeImpl(SQLTable t) throws SQLException {
        getStream().print(t.getName() + "... ");
        if (!t.isOrdered()) {
            getStream().println("not ordered");
        } else if (getSyntax().isOrder(t.getOrderField())) {
            getStream().println("already");
        } else {
            final String update;
            final SQLSystem system = this.getSystemRoot().getServer().getSQLSystem();
            if (system == SQLSystem.MYSQL) {
                update = SQLSelect.quote("ALTER TABLE %f MODIFY COLUMN %n " + getSyntax().getOrderDefinition(), t, t.getOrderField());
            } else if (system == SQLSystem.POSTGRESQL) {
                // do not "SET DEFAULT NULL" since then it returns "NULL::numeric" as the default
                // and isOrder() returns false
                final String setDef = getSyntax().getOrderDefault() == null ? "ALTER COLUMN %n DROP DEFAULT" : "ALTER COLUMN %n SET DEFAULT " + getSyntax().getOrderDefault();
                update = SQLSelect.quote("ALTER TABLE %f ALTER COLUMN %n TYPE " + getSyntax().getOrderType() + ", ALTER COLUMN %n DROP NOT NULL , " + setDef, t, t.getOrderField(), t.getOrderField(),
                        t.getOrderField());
            } else
                throw new IllegalStateException("not implemented for " + system);
            this.getDS().execute(update);
            t.getSchema().updateVersion();
            getStream().println("done");
        }
    }

}
