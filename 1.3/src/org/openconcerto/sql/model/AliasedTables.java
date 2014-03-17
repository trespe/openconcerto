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
 
 package org.openconcerto.sql.model;

import org.openconcerto.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A set of table aliases, eg {OBSERVATION, OBSERVATION obs, ARTICLE art2}.
 * 
 * @author Sylvain CUAZ
 */
class AliasedTables {

    private final Map<String, TableRef> tables;
    private DBSystemRoot sysRoot;

    // not public or we would need to check coherence between parameters
    private AliasedTables(Map<String, TableRef> m, DBSystemRoot sysRoot) {
        this.tables = new LinkedHashMap<String, TableRef>(m);
        this.sysRoot = sysRoot;
    }

    public AliasedTables() {
        this((DBSystemRoot) null);
    }

    public AliasedTables(final DBSystemRoot sysRoot) {
        this(Collections.<String, TableRef> emptyMap(), sysRoot);
    }

    AliasedTables(AliasedTables at) {
        this(at.tables, at.sysRoot);
    }

    /**
     * Adds a new declaration if not already present.
     * 
     * @param alias the alias, can be <code>null</code>, e.g. "obs7".
     * @param t the associated table, e.g. /OBSERVATION/.
     * @return the added table reference.
     */
    public TableRef add(String alias, SQLTable t) {
        return this.add(new AliasedTable(t, alias));
    }

    /**
     * Adds a new declaration if not already present.
     * 
     * @param table the table to add, e.g. /OBSERVATION/.
     * @return the added table reference, can be different than the parameter if the alias was
     *         already present.
     */
    public TableRef add(TableRef table) {
        final boolean nullSysRoot = this.sysRoot == null;
        if (!nullSysRoot && this.sysRoot != table.getTable().getDBSystemRoot())
            throw new IllegalArgumentException(table + " not in " + this.sysRoot);
        final String alias = table.getAlias();
        final TableRef res;
        if (!this.contains(alias)) {
            res = table;
            this.tables.put(alias, res);
            if (nullSysRoot)
                this.sysRoot = table.getTable().getDBSystemRoot();
        } else if (this.getTable(alias) != table.getTable()) {
            throw new IllegalArgumentException(table.getTable().getSQLName() + " can't be aliased to " + alias + " : " + this.getTable(alias).getSQLName() + " already is");
        } else {
            res = getAliasedTable(alias);
        }

        return res;
    }

    public TableRef add(FieldRef f) {
        return this.add(f.getTableRef());
    }

    public final DBSystemRoot getSysRoot() {
        return this.sysRoot;
    }

    public SQLTable getTable(String alias) {
        return getAliasedTable(alias).getTable();
    }

    /**
     * Return the alias for the passed table.
     * 
     * @param t a table.
     * @return the alias for <code>t</code>, or <code>null</code> if <code>t</code> is not exactly
     *         once in this.
     */
    public TableRef getAlias(SQLTable t) {
        return CollectionUtils.getSole(getAliases(t));
    }

    public List<TableRef> getAliases(SQLTable t) {
        final List<TableRef> res = new ArrayList<TableRef>();
        for (final TableRef at : this.tables.values())
            if (at.getTable().equals(t))
                res.add(at);
        return res;
    }

    public TableRef getAliasedTable(String alias) {
        return this.tables.get(alias);
    }

    public String getDeclaration(String alias) {
        return getAliasedTable(alias).getSQL();
    }

    public boolean contains(String alias) {
        return this.tables.containsKey(alias);
    }

    public LinkedHashSet<String> getAliases() {
        return new LinkedHashSet<String>(this.tables.keySet());
    }
}
