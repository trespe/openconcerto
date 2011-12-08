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
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.utils.AlterTable;

import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;

/**
 * Change the default and nullable properties of all date fields, then replace 0000 values by NULL.
 * 
 * @author Sylvain CUAZ
 */
public class ConvertToNullDate extends Changer<SQLTable> {

    public ConvertToNullDate(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected EnumSet<SQLSystem> getCompatibleSystems() {
        // others shouldn't have invalid dates
        return EnumSet.of(SQLSystem.MYSQL);
    }

    @Override
    protected void changeImpl(SQLTable t) throws SQLException {
        for (final SQLField f : t.getFields()) {
            if (f.getType().getType() == Types.DATE || f.getType().getType() == Types.TIMESTAMP) {
                getStream().println(f.getType() + ": " + f);
                if (f.isNullable() != Boolean.TRUE || f.getDefaultValue() != null) {
                    // TODO change to "ALTER IGNORE TABLE" otherwise mysql says :
                    // Data truncation: Incorrect datetime value: '0000-00-00 00:00:00' for column
                    // 'DATE' at row 1
                    final AlterTable alterColumn = new AlterTable(t).alterColumn(f.getName(), EnumSet.of(Properties.DEFAULT, Properties.NULLABLE), null, "NULL", true);
                    this.getDS().execute(alterColumn.asString());
                }
                final String nullValue = f.getType().getType() == Types.DATE ? "0000-00-00" : "0000-00-00 00:00:00";
                this.getDS().execute(t.getBase().quote("UPDATE %f SET %n = NULL WHERE %n = '" + nullValue + "'", f.getTable(), f, f));
            }
        }
    }
}
