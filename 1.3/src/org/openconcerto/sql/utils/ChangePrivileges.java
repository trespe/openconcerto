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

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionMap2.Mode;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.SetMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Allow to grant or revoke privileges of users/roles.
 * 
 * @author Sylvain
 */
public class ChangePrivileges {

    static public enum PrivelegeAction {
        GRANT, REVOKE
    }

    static public enum TablePrivilege {
        SELECT, INSERT, UPDATE, DELETE
    }

    private final String name;
    private final SetMap<SQLName, TablePrivilege> privilegesToGrant;
    private final SetMap<SQLName, TablePrivilege> privilegesToRevoke;

    public ChangePrivileges(String userName) {
        super();
        this.name = userName;
        this.privilegesToGrant = new SetMap<SQLName, TablePrivilege>(Mode.NULL_MEANS_ALL);
        this.privilegesToRevoke = new SetMap<SQLName, TablePrivilege>(Mode.NULL_MEANS_ALL);
    }

    public final ChangePrivileges grantTablePrivilege(final SQLName t, final TablePrivilege priv) {
        return this.changeTablePrivilege(PrivelegeAction.GRANT, t, priv);
    }

    public final ChangePrivileges revokeTablePrivilege(final SQLName t, final TablePrivilege priv) {
        return this.changeTablePrivilege(PrivelegeAction.REVOKE, t, priv);
    }

    public final ChangePrivileges changeTablePrivilege(final PrivelegeAction grant, final DBRoot r, final TablePrivilege priv) {
        this.changeTablePrivilege(grant, new SQLName(r.getName()), priv);
        return this;
    }

    public final ChangePrivileges changeTablePrivilege(final PrivelegeAction grant, final SQLTable t, final TablePrivilege priv) {
        return this.changeTablePrivilege(grant, t.getSQLNameUntilDBRoot(true), priv);
    }

    /**
     * Grant or revoke a table privilege for the passed object.
     * 
     * @param grant whether to grant or revoke.
     * @param t size of 1 refer to root, size of 2 to a table.
     * @param priv the privilege to change.
     * @return this.
     * @throws IllegalArgumentException if size of t is invalid.
     */
    public final ChangePrivileges changeTablePrivilege(final PrivelegeAction grant, final SQLName t, final TablePrivilege priv) throws IllegalArgumentException {
        return this.addTablePrivilege(grant == PrivelegeAction.GRANT ? this.privilegesToGrant : this.privilegesToRevoke, t, priv);
    }

    private final ChangePrivileges addTablePrivilege(final SetMap<SQLName, TablePrivilege> m, final SQLName t, final TablePrivilege priv) throws IllegalArgumentException {
        if (t.getItemCount() == 0 || t.getItemCount() > 2)
            throw new IllegalArgumentException("not root.table :" + t);
        if (priv == null)
            m.put(t, null);
        else
            m.add(t, priv);
        return this;
    }

    /**
     * Make sure this instance won't grant nor revoke any privilege from the passed object. I.e.
     * only useful if <code>t</code> was passed to a method the change table privileges.
     * 
     * @param t a root or table name.
     * @return this.
     */
    public final ChangePrivileges removeTablePrivilege(final SQLName t) {
        if (t.getItemCount() != 2)
            throw new IllegalArgumentException("not root.table :" + t);
        this.privilegesToGrant.remove(t);
        this.privilegesToRevoke.remove(t);
        return this;
    }

    private final Set<String> getTableNames(final DBSystemRoot sysRoot, final SQLName name) {
        final String rootName = name.getFirst();

        final Set<String> tables;
        if (name.getItemCount() == 2) {
            tables = Collections.singleton(name.getName());
        } else {
            assert name.getItemCount() == 1;
            tables = sysRoot.getRoot(rootName).getChildrenNames();
        }

        return tables;
    }

    public final List<String> getStatements(final DBSystemRoot sysRoot) {
        final List<String> res = new ArrayList<String>();
        final String qName = SQLBase.quoteIdentifier(this.name);
        final Set<String> roots = new HashSet<String>();
        for (final Entry<SQLName, Set<TablePrivilege>> e : this.privilegesToGrant.entrySet()) {
            final String privs = e.getValue() == null ? "ALL" : CollectionUtils.join(e.getValue(), ", ");
            final SQLName name = e.getKey();
            final String rootName = name.getFirst();
            for (final String tableName : getTableNames(sysRoot, name)) {
                res.add("GRANT " + privs + " ON " + new SQLName(rootName, tableName).quote() + " TO " + qName);
            }
            roots.add(rootName);
        }
        if (sysRoot.getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
            for (final String schema : roots) {
                res.add(0, "GRANT USAGE ON SCHEMA " + SQLBase.quoteIdentifier(schema) + " TO " + qName);
            }
        }
        roots.clear();
        // if privilege specified twice, revoke
        for (final Entry<SQLName, Set<TablePrivilege>> e : this.privilegesToRevoke.entrySet()) {
            final boolean allPriv = e.getValue() == null;
            final String privs = allPriv ? "ALL" : CollectionUtils.join(e.getValue(), ", ");

            final SQLName name = e.getKey();
            final String rootName = name.getFirst();
            for (final String tableName : getTableNames(sysRoot, name)) {
                res.add("REVOKE " + privs + " ON " + new SQLName(rootName, tableName).quote() + " FROM " + qName);
            }

            if (allPriv && name.getItemCount() == 1)
                roots.add(rootName);
        }
        if (sysRoot.getServer().getSQLSystem() == SQLSystem.POSTGRESQL) {
            for (final String schema : roots) {
                res.add(0, "REVOKE USAGE ON SCHEMA " + SQLBase.quoteIdentifier(schema) + " FROM " + qName);
            }
        }
        return res;
    }

    public final void execute(final DBSystemRoot sysRoot) {
        final SQLDataSource ds = sysRoot.getDataSource();
        for (final String s : this.getStatements(sysRoot)) {
            ds.execute(s);
        }
    }
}
