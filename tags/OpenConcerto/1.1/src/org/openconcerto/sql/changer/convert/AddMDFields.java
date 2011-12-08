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
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.AlterTable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AddMDFields extends Changer<SQLTable> {

    private final static List<String> fields;
    static {
        fields = new ArrayList<String>();
        fields.add("ID_USER_COMMON_CREATE");
        fields.add("ID_USER_COMMON_MODIFY");
        fields.add("CREATION_DATE");
        fields.add("MODIFICATION_DATE");
    }

    private static Collection<String> getFKFields() {
        return fields.subList(0, 2);
    }

    private static Collection<String> getDateFields() {
        return fields.subList(2, 4);
    }

    public AddMDFields(DBSystemRoot b) {
        super(b);
    }

    protected void changeImpl(SQLTable table) throws SQLException {
        if (testTable(table) && !table.getFieldsName().containsAll(fields)) {
            final SQLTable userT = table.getDBRoot().findTable("USER_COMMON");
            if (userT == null) {
                throw new IllegalStateException("No table USER_COMMON found");
            }
            final AlterTable alter = new AlterTable(table);
            for (final String fk : getFKFields()) {
                if (!table.contains(fk))
                    alter.addForeignColumn(fk, userT);
            }
            for (final String fk : getDateFields()) {
                if (!table.contains(fk))
                    alter.addDateAndTimeColumn(fk);
            }

            getDS().execute(alter.asString());
            this.getStream().println("added metadata fields on " + table.getSQLName());
        }
    }

    // limit to standard tables
    private boolean testTable(SQLTable table) {
        return table.isRowable() && table.getOrderField() != null;
    }
}
