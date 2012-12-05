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
 
 package org.openconcerto.sql.request;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.TableRef;
import org.openconcerto.sql.model.Where;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allow to build an UPDATE statement.
 * 
 * @author Sylvain
 */
public class UpdateBuilder {

    private final SQLTable t;
    private final Map<String, String> fields;
    private final List<String> tables;
    private Where where;

    public UpdateBuilder(SQLTable t) {
        super();
        this.t = t;
        this.fields = new LinkedHashMap<String, String>();
        this.tables = new ArrayList<String>();
    }

    public final SQLTable getTable() {
        return this.t;
    }

    public final UpdateBuilder set(final String field, final String value) {
        if (this.getTable().contains(field))
            this.fields.put(field, value);
        else
            throw new IllegalArgumentException("unknown " + field + " in " + this.getTable().getSQLName());
        return this;
    }

    public final Set<String> getFieldsNames() {
        return this.fields.keySet();
    }

    public final boolean isEmpty() {
        return this.fields.isEmpty();
    }

    public final void setWhere(Where where) {
        this.where = where;
    }

    public final Where getWhere() {
        return this.where;
    }

    public final void addTable(final TableRef t) {
        this.tables.add(t.getSQL());
    }

    /**
     * Add table to this UPDATE.
     * 
     * @param sel the select to add.
     * @param alias the alias, cannot be <code>null</code>, e.g. <code>t</code>.
     */
    public final void addTable(final SQLSelect sel, final String alias) {
        this.addRawTable("( " + sel.asString() + " )", SQLBase.quoteIdentifier(alias));
    }

    /**
     * Add table to this UPDATE.
     * 
     * @param definition the table to add, ie either a table name or a sub-select.
     * @param rawAlias the SQL alias, can be <code>null</code>, e.g. <code>"t"</code>.
     */
    public final void addRawTable(final String definition, final String rawAlias) {
        this.tables.add(definition + (rawAlias == null ? "" : " " + rawAlias));
    }

    public final String asString() {
        final String w = this.where == null ? "" : "\nWHERE " + this.where.getClause();
        return "UPDATE " + this.getTable().getServer().getSQLSystem().getSyntax().getUpdate(this.getTable(), unmodifiableList(this.tables), unmodifiableMap(this.fields)) + w;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + this.asString();
    }
}
