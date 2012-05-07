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
import org.openconcerto.sql.utils.ChangeTable.ConcatStep;
import org.openconcerto.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
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
     * The SQL to update this root.
     * 
     * @param r the name of the updated root, <code>null</code> meaning {@link #getName()}.
     * @param drop whether to first drop the root.
     * @param create whether to create the root, e.g. <code>false</code> if adding some tables to an
     *        existing root.
     * @return the SQL needed.
     */
    public String asString(final String r, final boolean drop, final boolean create) {
        return CollectionUtils.join(asStringList(r, drop, create, EnumSet.noneOf(ConcatStep.class)), "\n");
    }

    public List<String> asStringList(final String r, final boolean drop, final boolean create, final EnumSet<ConcatStep> boundaries) {
        final List<List<String>> lists = this.asLists(r, drop, create, boundaries);
        final List<String> res = new ArrayList<String>(lists.size());
        for (final List<String> l : lists) {
            res.add(CollectionUtils.join(l, "\n"));
        }
        return res;
    }

    /**
     * The SQL to update this root. The first item of the result is the drop/creation of the root
     * and the {@link #addClause(String) clauses}, then <code>boundaries</code> size + 1 items for
     * creating the tables.
     * 
     * @param r the name of the updated root, <code>null</code> meaning {@link #getName()}.
     * @param drop whether to first drop the root.
     * @param create whether to create the root, e.g. <code>false</code> if adding some tables to an
     *        existing root.
     * @param boundaries where to split the SQL statements.
     * @return the SQL needed, the list size is two more than <code>boundaries</code> size.
     * @see ChangeTable#cat(Collection, String, EnumSet)
     */
    public List<List<String>> asLists(final String r, final boolean drop, final boolean create, final EnumSet<ConcatStep> boundaries) {
        final String rootName = r == null ? this.getName() : r;
        final List<String> genClauses = new ArrayList<String>(this.clauses);
        if (create)
            genClauses.add(0, this.getSyntax().getCreateRoot(rootName));
        if (drop)
            genClauses.add(0, this.getSyntax().getDropRoot(rootName));
        final String initRoot = this.getSyntax().getInitRoot(rootName);
        if (initRoot.trim().length() > 0)
            genClauses.add(initRoot);

        final List<List<String>> res = new ArrayList<List<String>>(boundaries.size() + 2);
        res.add(genClauses);
        res.addAll(ChangeTable.cat(this.tables, rootName, boundaries));
        assert res.size() == boundaries.size() + 2;

        return res;
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
