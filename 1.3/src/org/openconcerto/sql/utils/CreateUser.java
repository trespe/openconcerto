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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLSystem;

import java.util.ArrayList;
import java.util.List;

public class CreateUser {

    private final String name, password;
    private final ChangePrivileges grant;

    public CreateUser(String name, String pass) {
        super();
        this.name = name;
        this.password = pass;
        this.grant = new ChangePrivileges(name);
    }

    public final ChangePrivileges getGrant() {
        return this.grant;
    }

    public final List<String> getStatements(final DBSystemRoot sysRoot) {
        final List<String> res = new ArrayList<String>();
        final String qName = SQLBase.quoteIdentifier(this.name);
        // TODO move SQLBase.quoteString() to DBSystemRoot
        final String passKeyWord = sysRoot.getServer().getSQLSystem() == SQLSystem.MYSQL ? " IDENTIFIED BY " : " PASSWORD ";
        res.add("CREATE USER " + qName + passKeyWord + SQLBase.quoteStringStd(this.password));
        res.addAll(this.grant.getStatements(sysRoot));
        return res;
    }

    public final void execute(final DBSystemRoot sysRoot) {
        final SQLDataSource ds = sysRoot.getDataSource();
        for (final String s : this.getStatements(sysRoot)) {
            ds.execute(s);
        }
    }
}
