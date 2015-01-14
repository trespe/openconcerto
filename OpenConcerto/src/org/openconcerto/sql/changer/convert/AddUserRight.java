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
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.users.rights.UserRightSQLElement;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.SQLCreateTable;

import java.sql.SQLException;
import java.util.List;

/**
 * Add rights tables.
 * 
 * @author Sylvain
 * @see UserRightsManager
 */
public class AddUserRight extends Changer<DBRoot> {

    public AddUserRight(DBSystemRoot b) {
        super(b);
    }

    @Override
    protected void changeImpl(DBRoot r) throws SQLException {
        this.getStream().println(r + "... ");
        final UserManager userMngr = UserManager.getInstance();
        if (userMngr == null)
            throw new IllegalStateException("No user manager");
        final SQLTable userT = userMngr.getTable();

        final List<SQLCreateTable> createTables = UserRightSQLElement.getCreateTables(userT);
        if (createTables.size() == 0) {
            getStream().println("Tables already created");
        } else {
            r.createTables(createTables);
            getStream().println("Tables created");
        }

        final AlterTable alterTable = new AlterTable(userT);
        for (final String f : new String[] { UserRightsManager.SUPERUSER_FIELD, UserRightsManager.ADMIN_FIELD }) {
            if (!userT.contains(f))
                alterTable.addColumn(f, "boolean not null default false");
        }
        if (!alterTable.isEmpty()) {
            getDS().execute(alterTable.toString());
            r.getSchema().updateVersion();
            getStream().println("Fields created in " + userT);
        }
    }
}
