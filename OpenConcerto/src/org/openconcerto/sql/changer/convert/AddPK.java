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
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.AlterTable;

import java.sql.SQLException;
import java.util.EnumSet;

/**
 * Add the primary key (ID or PKEYID if ID exists)
 * 
 */
public class AddPK extends Changer<SQLTable> {

    public AddPK(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected EnumSet<SQLSystem> getCompatibleSystems() {
        return EnumSet.of(SQLSystem.MYSQL, SQLSystem.POSTGRESQL, SQLSystem.H2);
    }

    protected void changeImpl(SQLTable t) throws SQLException {
        if (t.getKey() == null) {
            final String name;
            if (t.contains("ID")) {
                name = "PKEYID";
            } else {
                name = "ID";
            }
            final AlterTable alter = new AlterTable(t);
            alter.addColumn(name, getSyntax().getPrimaryIDDefinition());
            final String s = alter.asString();
            this.getDS().execute(s);
        }
    }

}
