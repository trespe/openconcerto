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

import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Construct a schema with tables.
 * 
 * @author Sylvain
 */
public final class SQLCreateRoot {

    private final SQLSyntax syntax;
    private String name;
    private final List<String> clauses;
    private final List<SQLCreateTableBase<?>> tables;

    public SQLCreateRoot(final SQLSyntax syntax, final String name) {
        super();
        this.syntax = syntax;
        this.name = name;
        this.tables = new ArrayList<SQLCreateTableBase<?>>();
        this.clauses = new ArrayList<String>();
    }

    public final SQLSyntax getSyntax() {
        return this.syntax;
    }

    public final SQLCreateRoot addTable(SQLCreateTableBase<?> t) {
        this.tables.add(t);
        return this;
    }

    public final SQLCreateRoot addClause(String s) {
        this.clauses.add(s);
        return this;
    }

    public String asString() {
        return this.asString(this.getName());
    }

    public final String asString(final String r) {
        return this.asString(r, true, true);
    }

    public String asString(final boolean drop, final boolean create) {
        return asString(this.getName(), drop, create);
    }

    /**
     * The sql to update this root.
     * 
     * @param r the name of the updated root, <code>null</code> meaning {@link #getName()}.
     * @param drop whether to first drop the root.
     * @param create whether to create the root, eg <code>false</code> if adding some tables to an
     *        existing root.
     * @return the sql needed.
     */
    public String asString(final String r, final boolean drop, final boolean create) {
        final String rootName = r == null ? this.getName() : r;
        final List<String> genClauses = new ArrayList<String>(this.clauses);
        if (create)
            genClauses.add(0, this.getSyntax().getCreateRoot(rootName));
        if (drop)
            genClauses.add(0, this.getSyntax().getDropRoot(rootName));

        genClauses.addAll(ChangeTable.cat(this.tables, rootName));
        genClauses.add(this.getSyntax().getInitRoot(rootName));

        return CollectionUtils.join(genClauses, "\n");
    }

    public final String toString() {
        return this.asString(null);
    }

    public final String getName() {
        return this.name;
    }

    public final SQLCreateRoot setName(String name) {
        this.name = name;
        return this;
    }
}
