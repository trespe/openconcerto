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
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;

import java.sql.SQLException;
import java.util.EnumSet;

public class SetDefaultCharset extends Changer<DBRoot> {

    public SetDefaultCharset(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected EnumSet<SQLSystem> getCompatibleSystems() {
        return EnumSet.of(SQLSystem.MYSQL);
    }

    protected void changeImpl(DBRoot root) throws SQLException {
        this.getStream().println(root + "... ");
        this.getDS().execute("ALTER DATABASE CHARACTER SET default");

        for (final SQLTable t : root.getTables()) {
            this.getStream().print("Converting " + t + "... ");
            final String req = "ALTER table " + t.getSQLName().quote() + " convert to character set default";
            this.getDS().execute(req);
            this.getStream().println("done");
        }
    }
}
