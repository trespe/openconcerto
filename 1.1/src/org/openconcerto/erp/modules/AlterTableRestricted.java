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

import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.SQLCreateTableBase;
import org.openconcerto.utils.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Construct an ALTER TABLE statement with only "add" clauses or clauses that modify fields
 * belonging to a module.
 * 
 * @author Sylvain
 */
public final class AlterTableRestricted {

    private final DBContext ctxt;
    private final SQLTable table;
    private final Set<SQLField> previouslyCreatedFields;
    private final AlterTable alter;
    private final Set<String> addedColumns, removedColumns;

    AlterTableRestricted(DBContext ctxt, String tableName) {
        this.ctxt = ctxt;
        this.table = ctxt.getRoot().getTable(tableName);
        this.previouslyCreatedFields = ctxt.getFieldsPreviouslyCreated(tableName);
        this.alter = new AlterTable(this.table);
        this.addedColumns = new HashSet<String>();
        this.removedColumns = new HashSet<String>();
    }

    final AlterTable getAlter() {
        return this.alter;
    }

    final Set<String> getAddedColumns() {
        return this.addedColumns;
    }

    final Set<String> getRemovedColumns() {
        return this.removedColumns;
    }

    public final SQLSyntax getSyntax() {
        return this.alter.getSyntax();
    }

    public final String getName() {
        return this.alter.getName();
    }

    private void addCol(String name) {
        if (this.table.contains(name))
            throw new IllegalArgumentException(name + " already exists");
        if (!this.addedColumns.add(name))
            throw new IllegalArgumentException(name + " already added to this");
    }

    // to alter or drop
    private void checkCol(String name) {
        final SQLField f = this.table.getField(name);
        if (!this.previouslyCreatedFields.contains(f))
            throw new IllegalArgumentException(f + " doesn't belong to this module");
    }

    public final AlterTable addColumn(String name, String definition) {
        addCol(name);
        return this.alter.addColumn(name, definition);
    }

    public final AlterTable addVarCharColumn(String name, int count) {
        addCol(name);
        return this.alter.addVarCharColumn(name, count);
    }

    public final AlterTable addDateAndTimeColumn(String name) {
        addCol(name);
        return this.alter.addDateAndTimeColumn(name);
    }

    public final AlterTable addIntegerColumn(String name, int defaultVal) {
        addCol(name);
        return this.alter.addIntegerColumn(name, defaultVal);
    }

    public AlterTable addForeignColumn(String fk, String table) {
        return this.addForeignColumn(fk, new SQLName(table));
    }

    public AlterTable addForeignColumn(String fk, SQLName tableName) {
        SQLCreateTableBase<?> createTable;
        if (tableName.getItemCount() == 1 && (createTable = this.ctxt.getCreateTables().get(tableName.getFirst())) != null) {
            addCol(fk);
            return this.alter.addForeignColumn(fk, createTable);
        } else {
            return this.addForeignColumn(fk, this.table.getDesc(tableName, SQLTable.class));
        }
    }

    public AlterTable addForeignColumn(String fk, SQLTable foreignTable) {
        addCol(fk);
        return this.alter.addForeignColumn(fk, foreignTable);
    }

    public AlterTable addUniqueConstraint(String name, List<String> cols) {
        if (!this.addedColumns.containsAll(cols))
            throw new IllegalArgumentException("Can only add constraint to added columns : " + CollectionUtils.substract(cols, this.addedColumns));
        return this.alter.addUniqueConstraint(name, cols);
    }

    public final AlterTable alterColumn(String fname, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        checkCol(fname);
        return this.alter.alterColumn(fname, toAlter, type, defaultVal, nullable);
    }

    public final AlterTable alterColumnNullable(String f, boolean b) {
        checkCol(f);
        return this.alter.alterColumnNullable(f, b);
    }

    public final AlterTable dropColumn(String name) {
        checkCol(name);
        this.removedColumns.add(name);
        return this.alter.dropColumn(name);
    }

    public boolean isEmpty() {
        return this.alter.isEmpty();
    }

    @Override
    public int hashCode() {
        return this.alter.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this.alter.equals(obj);
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.alter.toString();
    }
}
