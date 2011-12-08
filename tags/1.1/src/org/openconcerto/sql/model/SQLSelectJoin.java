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

import org.openconcerto.utils.Tuple2;

import java.util.List;

public class SQLSelectJoin implements SQLItem {

    private static final Tuple2<FieldRef, AliasedTable> NULL_TUPLE = new Tuple2<FieldRef, AliasedTable>(null, null);

    // tries to parse t.ID_LOCAL = l.ID
    static private Tuple2<FieldRef, AliasedTable> parse(final Where w) {
        final List<FieldRef> fields = w.getFields();
        if (fields.size() != 2)
            return NULL_TUPLE;
        final FieldRef pk;
        final FieldRef ff;
        if (fields.get(0).getField().isPrimaryKey()) {
            pk = fields.get(0);
            ff = fields.get(1);
        } else if (fields.get(1).getField().isPrimaryKey()) {
            pk = fields.get(1);
            ff = fields.get(0);
        } else {
            return NULL_TUPLE;
        }
        if (!ff.getField().getTable().getForeignKeys().contains(ff.getField()))
            return NULL_TUPLE;

        return Tuple2.create(ff, new AliasedTable(pk.getField().getTable(), pk.getAlias()));
    }

    private final SQLSelect parent;
    private final String joinType;
    /** the joined table, e.g. OBSERVATION obs */
    private final AliasedTable t;
    /** the where, e.g. rec.ID_LOCAL = circuit.ID_LOCAL */
    private final Where joinW;
    /** the foreign field, e.g. obs.ID_ARTICLE or recept.ID_OBSERVATION */
    private final FieldRef f;
    /** the foreign table, e.g. ARTICLE art or OBSERVATION obs */
    private final AliasedTable foreignTable;
    private Where where;

    SQLSelectJoin(final SQLSelect parent, String joinType, AliasedTable t, FieldRef ff, AliasedTable foreignTable) {
        this(parent, joinType, t, new Where(ff, "=", foreignTable.getKey()), ff, foreignTable);
    }

    SQLSelectJoin(final SQLSelect parent, String joinType, AliasedTable t, Where w) {
        this(parent, joinType, t, w, parse(w));
    }

    private SQLSelectJoin(final SQLSelect parent, String joinType, AliasedTable t, Where w, final Tuple2<FieldRef, AliasedTable> info) {
        this(parent, joinType, t, w, info.get0(), info.get1());
    }

    private SQLSelectJoin(final SQLSelect parent, String joinType, AliasedTable t, Where w, final FieldRef ff, final AliasedTable foreignTable) {
        super();
        this.parent = parent;
        this.joinType = joinType;
        this.joinW = w;

        this.f = ff;
        this.t = t;
        this.foreignTable = foreignTable;
        this.where = null;

        // checked by SQLSelect or provided by parse(Where)
        assert ff == null || ff.getField().getDBSystemRoot().getGraph().getForeignTable(ff.getField()) == foreignTable.getTable();
    }

    /**
     * Set an additional where for this join.
     * 
     * @param w the where to add, can be <code>null</code>, e.g. art."ID_SITE" = 123.
     */
    public final void setWhere(Where w) {
        this.where = w;
    }

    public final Where getWhere() {
        return this.where;
    }

    @Override
    public String getSQL() {
        final Where archiveW = this.parent.getArchiveWhere(getJoinedTable().getTable(), getAlias());
        final Where undefW = this.parent.getUndefWhere(getJoinedTable().getTable(), getAlias());
        return " " + this.joinType + " JOIN " + this.t.getDeclaration() + " on " + this.joinW.and(archiveW).and(undefW).and(getWhere());
    }

    public final String getJoinType() {
        return this.joinType;
    }

    /**
     * The foreign field if the join is a simple t1.fk1 = t2.pk.
     * 
     * @return the foreign field or <code>null</code>, e.g. t1.fk1.
     */
    public final FieldRef getForeignField() {
        return this.f;
    }

    public final boolean hasForeignField() {
        return this.f != null;
    }

    public final String getAlias() {
        return this.getJoinedTable().getAlias();
    }

    public final AliasedTable getJoinedTable() {
        return this.t;
    }

    /**
     * If there's a foreign field, this returns its foreign table.
     * 
     * @return <code>null</code> if {@link #getForeignField()} is <code>null</code>, otherwise its
     *         target, e.g. t2.
     */
    public final AliasedTable getForeignTable() {
        return this.foreignTable;
    }

    @Override
    public String toString() {
        return this.getSQL();
    }
}
