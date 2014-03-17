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
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.AlterTable;

import java.sql.SQLException;
import java.sql.Types;
import java.util.EnumSet;

/**
 * Change the type of the primary key to int.
 * 
 * @author Sylvain CUAZ
 */
public class ChangeIDToInt extends Changer<SQLTable> {

    public ChangeIDToInt(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected void changeImpl(final SQLTable t) throws SQLException {
        this.getStream().print(t + "... ");
        if (!t.isRowable()) {
            this.getStream().println("not rowable");
        } else {
            final SQLField pk = t.getKey();
            if (pk.getType().getType() == Types.INTEGER) {
                this.getStream().println("already");
            } else {
                getDS().execute(new AlterTable(t).alterColumn(pk.getName(), EnumSet.of(Properties.TYPE), "int", null, null).asString());
                t.getSchema().updateVersion();
                this.getStream().println("done");
            }
        }
    }
}
