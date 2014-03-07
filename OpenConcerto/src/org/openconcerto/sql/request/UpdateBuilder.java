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
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.TableRef;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.Tuple3.List3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    // alias -> definition, t field, field of the joined table
    private final Map<String, List3<String>> virtualJoins;
    private final Map<String, Boolean> virtualJoinsOptimized;

    public UpdateBuilder(SQLTable t) {
        super();
        this.t = t;
        this.fields = new LinkedHashMap<String, String>();
        this.tables = new ArrayList<String>();
        this.virtualJoins = new HashMap<String, List3<String>>(4);
        this.virtualJoinsOptimized = new HashMap<String, Boolean>(4);
    }

    public final SQLTable getTable() {
        return this.t;
    }

    private final void checkField(final String field) {
        checkField(field, getTable());
    }

    private final void checkField(final String field, final TableRef t) {
        if (!t.getTable().contains(field))
            throw new IllegalArgumentException("unknown " + field + " in " + t.getSQL());
    }

    private final void checkField(final SQLField field) {
        if (this.getTable() != field.getTable())
            throw new IllegalArgumentException(field + " not in " + this.getTable().getSQLName());
    }

    public final UpdateBuilder set(final String field, final String value) {
        this.checkField(field);
        this.fields.put(field, value);
        return this;
    }

    public final UpdateBuilder setObject(final String fieldName, final Object value) {
        this.fields.put(fieldName, getTable().getField(fieldName).getType().toString(value));
        return this;
    }

    public final UpdateBuilder setObject(final SQLField field, final Object value) {
        this.checkField(field);
        this.fields.put(field.getName(), field.getType().toString(value));
        return this;
    }

    private final boolean isJoinVirtual(final String alias) {
        if (!this.virtualJoins.containsKey(alias))
            throw new IllegalArgumentException("Not a join " + alias);
        return getTable().getServer().getSQLSystem() == SQLSystem.H2 || this.virtualJoinsOptimized.get(alias) == Boolean.FALSE;
    }

    /**
     * Set the passed field to the value of a field from a virtual join.
     * 
     * @param field a field in the {@link #getTable() table} to update.
     * @param joinAlias the alias of the virtual join.
     * @param joinedTableField a field from the joined table.
     * @return this.
     * @see #setFromVirtualJoin(String, String, String)
     */
    public final UpdateBuilder setFromVirtualJoinField(final String field, final String joinAlias, final String joinedTableField) {
        return this.setFromVirtualJoin(field, joinAlias, new SQLName(joinAlias, joinedTableField).quote());
    }

    /**
     * Set the passed field to the passed SQL value from a virtual join.
     * 
     * @param field a field in the {@link #getTable() table} to update.
     * @param joinAlias the alias of the virtual join.
     * @param value the SQL, e.g. a quoted field from the joined table or an arbitrary expression.
     * @return this.
     * @see #setFromVirtualJoinField(String, String, String)
     * @see #addVirtualJoin(TableRef, String)
     */
    public final UpdateBuilder setFromVirtualJoin(final String field, final String joinAlias, final String value) {
        final String val;
        if (this.isJoinVirtual(joinAlias)) {
            final List3<String> virtualJoin = this.virtualJoins.get(joinAlias);
            val = "( select " + value + " from " + virtualJoin.get0() + " where " + getWhere(joinAlias, virtualJoin) + " )";
        } else {
            val = value;
        }
        return this.set(field, val);
    }

    private final String getWhere(final String joinAlias, final List3<String> virtualJoin) {
        assert this.virtualJoins.get(joinAlias) == virtualJoin;
        final SQLName joinedTableFieldName = new SQLName(joinAlias, virtualJoin.get2());
        return getTable().getField(virtualJoin.get1()).getSQLNameUntilDBRoot(false) + " = " + joinedTableFieldName.quote();
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

    public final void addBackwardVirtualJoin(final TableRef t, final String joinedTableField) {
        checkField(joinedTableField, t);
        this.addVirtualJoin(t.getSQL(), t.getAlias(), true, joinedTableField, getTable().getKey().getName());
    }

    public final void addForwardVirtualJoin(final TableRef t, final String joinField) {
        checkField(joinField, getTable());
        this.addVirtualJoin(t.getSQL(), t.getAlias(), true, t.getKey().getField().getName(), joinField);
    }

    public final void addVirtualJoin(final String definition, final String alias, final String joinedTableField) {
        this.addVirtualJoin(definition, alias, false, joinedTableField, getTable().getKey().getName());
    }

    public final void addVirtualJoin(final String definition, final String alias, final boolean aliasAlreadyDefined, final String joinedTableField, final String field) {
        this.addVirtualJoin(definition, alias, aliasAlreadyDefined, joinedTableField, field, true);
    }

    /**
     * Add a virtual join to this UPDATE. Some systems don't support
     * {@link #addRawTable(String, String) multiple tables}, this method is virtual in the sense
     * that it emulates the behaviour using sub-queries.
     * 
     * @param definition the definition of a table, e.g. simply "root"."t" or a VALUES expression.
     * @param alias the alias, cannot be <code>null</code>.
     * @param aliasAlreadyDefined if <code>true</code> the <code>alias</code> won't be appended to
     *        the <code>definition</code>. Needed for
     *        {@link SQLSyntax#getConstantTable(List, String, List) constant tables} since the alias
     *        is already inside the definition, e.g. ( VALUES ... ) as "constTable"(field1, ...) .
     * @param joinedTableField the field in the joined table that will match <code>field</code> of
     *        the update {@link #getTable() table}.
     * @param field the field in the update {@link #getTable() table}.
     * @param optimize if <code>true</code> and if the system supports it, the virtual join will use
     *        the multiple table support.
     */
    public final void addVirtualJoin(final String definition, final String alias, final boolean aliasAlreadyDefined, final String joinedTableField, final String field, final boolean optimize) {
        if (alias == null)
            throw new NullPointerException("No alias");
        if (this.virtualJoins.containsKey(alias))
            throw new IllegalStateException("Alias already exists : " + alias);
        this.checkField(field);
        final String completeDef = aliasAlreadyDefined ? definition : definition + ' ' + SQLBase.quoteIdentifier(alias);
        this.virtualJoins.put(alias, new List3<String>(completeDef, field, joinedTableField));
        this.virtualJoinsOptimized.put(alias, optimize);
    }

    public final String asString() {
        // add tables and where for virtual joins
        Where computedWhere = this.where;
        final List<String> computedTables = new ArrayList<String>(this.tables);
        for (final Entry<String, List3<String>> e : this.virtualJoins.entrySet()) {
            final String joinAlias = e.getKey();
            final List3<String> virtualJoin = e.getValue();
            final Where w;
            if (this.isJoinVirtual(joinAlias)) {
                w = Where.createRaw(SQLBase.quoteIdentifier(virtualJoin.get1()) + " in ( select " + SQLBase.quoteIdentifier(virtualJoin.get2()) + " from " + virtualJoin.get0() + " )");
            } else {
                w = Where.createRaw(getWhere(joinAlias, virtualJoin));
                computedTables.add(virtualJoin.get0());
            }
            computedWhere = w.and(computedWhere);
        }
        final String w = computedWhere == null ? "" : "\nWHERE " + computedWhere.getClause();
        return "UPDATE " + this.getTable().getServer().getSQLSystem().getSyntax().getUpdate(this.getTable(), unmodifiableList(computedTables), unmodifiableMap(this.fields)) + w;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": " + this.asString();
    }
}
