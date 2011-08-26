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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.DropTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLCreateTableBase;
import org.openconcerto.utils.CollectionMap;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Allow a module to add columns (and tables).
 * 
 * @author Sylvain
 */
public final class DBContext {

    private final ModuleVersion lastInstalledVersion;
    private final DBRoot root;
    private final List<ChangeTable<?>> changeTables;
    private final List<AlterTableRestricted> alterTables;

    private final Set<String> tables;
    private final CollectionMap<String, SQLField> fields;

    DBContext(ModuleVersion lastInstalledVersion, DBRoot root, final Set<String> tables, final Set<SQLName> fields) {
        super();
        this.lastInstalledVersion = lastInstalledVersion;
        this.root = root;
        this.tables = Collections.unmodifiableSet(tables);
        this.fields = new CollectionMap<String, SQLField>(new HashSet<SQLField>());
        for (final SQLName f : fields) {
            final String tableName = f.getItem(0);
            this.fields.put(tableName, this.root.getTable(tableName).getField(f.getItem(1)));
        }
        this.changeTables = new ArrayList<ChangeTable<?>>();
        this.alterTables = new ArrayList<AlterTableRestricted>();
    }

    public final ModuleVersion getLastInstalledVersion() {
        return this.lastInstalledVersion;
    }

    public final DBRoot getRoot() {
        return this.root;
    }

    public final Set<String> getTablesPreviouslyCreated() {
        return this.tables;
    }

    public final Set<SQLField> getFieldsPreviouslyCreated(String tableName) {
        return (Set<SQLField>) this.fields.getNonNull(tableName);
    }

    private final List<String> getSQL() {
        return ChangeTable.cat(this.changeTables, this.root.getName());
    }

    final void execute() throws SQLException {
        final List<String> sql = this.getSQL();
        // refetch() is costly
        if (sql.size() > 0) {
            for (final String s : sql)
                getRoot().getDBSystemRoot().getDataSource().execute(s);
            getRoot().getSchema().updateVersion();

            // perhaps add a Map parameter to getCreateTable() for the undefined row
            // for now OK to not use an undefined row since we can't modify List/ComboSQLRequet
            for (final String addedTable : getAddedTables())
                SQLTable.setUndefID(getRoot().getSchema(), addedTable, null);
            getRoot().refetch();
        }
    }

    // DDL

    public final SQLCreateTable getCreateTable(final String name) {
        if (this.root.contains(name))
            throw new IllegalArgumentException("Table already exists : " + name);
        final SQLCreateTable res = new SQLCreateTable(this.root, name);
        this.changeTables.add(res);
        return res;
    }

    public final AlterTableRestricted getAlterTable(final String name) {
        final AlterTableRestricted res = new AlterTableRestricted(this, name);
        this.changeTables.add(res.getAlter());
        this.alterTables.add(res);
        return res;
    }

    public final void dropTable(final String name) {
        if (!this.getTablesPreviouslyCreated().contains(name))
            throw new IllegalArgumentException(name + "doesn't belong to this module");
        this.changeTables.add(new DropTable(this.root.getTable(name)));
    }

    // getter

    final List<String> getAddedTables() {
        final List<String> res = new ArrayList<String>();
        for (final ChangeTable<?> a : this.changeTables) {
            if (a instanceof SQLCreateTableBase<?>) {
                res.add(a.getName());
            }
        }
        return res;
    }

    final List<SQLName> getAddedFieldsToExistingTables() {
        final List<SQLName> res = new ArrayList<SQLName>();
        for (final AlterTableRestricted a : this.alterTables) {
            for (final String f : a.getAddedColumns()) {
                res.add(new SQLName(a.getName(), f));
            }
        }
        return res;
    }

    final List<String> getRemovedTables() {
        final List<String> res = new ArrayList<String>();
        for (final ChangeTable<?> a : this.changeTables) {
            if (a instanceof DropTable) {
                res.add(a.getName());
            }
        }
        return res;
    }

    final List<SQLName> getRemovedFieldsFromExistingTables() {
        final List<SQLName> res = new ArrayList<SQLName>();
        for (final AlterTableRestricted a : this.alterTables) {
            for (final String f : a.getRemovedColumns()) {
                res.add(new SQLName(a.getName(), f));
            }
        }
        return res;
    }
}
