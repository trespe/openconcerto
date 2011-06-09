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

    final Map<String, AliasedTable> tables;

    private AliasedTables(Map<String, AliasedTable> m) {
        this.tables = new LinkedHashMap<String, AliasedTable>(m);
    }

    public AliasedTables() {
        this(Collections.<String, AliasedTable> emptyMap());
    }

    AliasedTables(AliasedTables at) {
        this(at.tables);
    }

    /**
     * Adds a new declaration.
     * 
     * @param alias the alias, can be <code>null</code>, eg "obs7".
     * @param t the associated table, eg /OBSERVATION/.
     * @return the added alias, usefull if alias is <code>null</code> since it returns the table
     *         name.
     */
    public AliasedTable add(String alias, SQLTable t) {
        if (alias == null)
            alias = t.getName();

        final AliasedTable res;
        if (!this.contains(alias)) {
            res = new AliasedTable(t, alias);
            this.tables.put(alias, res);
        } else if (this.getTable(alias) != t)
            throw new IllegalArgumentException(t.getSQLName() + " can't be aliased to " + alias + " : " + this.getTable(alias).getSQLName() + " already is");
        else
            res = getAliasedTable(alias);

        return res;
    }

    public AliasedTable add(SQLTable t) {
        return this.add(t.getName(), t);
    }

    public AliasedTable add(FieldRef f) {
        return this.add(f.getAlias(), f.getField().getTable());
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
    public AliasedTable getAlias(SQLTable t) {
        return CollectionUtils.getSole(getAliases(t));
    }

    public List<AliasedTable> getAliases(SQLTable t) {
        final List<AliasedTable> res = new ArrayList<AliasedTable>();
        for (final AliasedTable at : this.tables.values())
            if (at.getTable().equals(t))
                res.add(at);
        return res;
    }

    private AliasedTable getAliasedTable(String alias) {
        return this.tables.get(alias);
    }

    public String getDeclaration(String alias) {
        return getAliasedTable(alias).getDeclaration();
    }

    public boolean contains(String alias) {
        return this.tables.containsKey(alias);
    }

    public LinkedHashSet<String> getAliases() {
        return new LinkedHashSet<String>(this.tables.keySet());
    }
}
