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

import static java.util.Collections.singletonList;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.Index;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ReflectUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Construct a statement about a table.
 * 
 * @author Sylvain
 * @param <T> type of this
 * @see AlterTable
 * @see SQLCreateTable
 */
public abstract class ChangeTable<T extends ChangeTable<T>> {

    public static enum ClauseType {
        ADD_COL, ADD_CONSTRAINT, ADD_INDEX, DROP_COL, DROP_CONSTRAINT, DROP_INDEX, ALTER_COL, OTHER
    }

    protected static enum ConcatStep {
        // drop constraints first since, at least in pg, they depend on indexes
        DROP_FOREIGN(ClauseType.DROP_CONSTRAINT),
        // drop indexes before columns to avoid having to know if the index is dropped because its
        // columns are dropped
        DROP_INDEX(ClauseType.DROP_INDEX), ALTER_TABLE(ClauseType.ADD_COL, ClauseType.DROP_COL, ClauseType.ALTER_COL, ClauseType.OTHER),
        // likewise add indexes before since constraints need them
        ADD_INDEX(ClauseType.ADD_INDEX), ADD_FOREIGN(ClauseType.ADD_CONSTRAINT);

        private final Set<ClauseType> types;

        private ConcatStep(ClauseType... types) {
            this.types = new HashSet<ClauseType>();
            for (final ClauseType t : types)
                this.types.add(t);
        }

        public final Set<ClauseType> getTypes() {
            return this.types;
        }
    }

    /**
     * Compute the SQL needed to create all passed tables, handling foreign key cycles.
     * 
     * @param cts the tables to create.
     * @param r where to create them.
     * @return the SQL needed.
     */
    public static List<String> cat(List<? extends ChangeTable> cts, final String r) {
        return cat(cts, r, false);
    }

    private static List<String> cat(List<? extends ChangeTable> cts, final String r, final boolean forceCat) {
        final List<String> res = new ArrayList<String>();
        for (final ConcatStep step : ConcatStep.values()) {
            for (final ChangeTable<?> ct : cts) {
                final String asString = ct.asString(r, step);
                if (asString != null && asString.length() > 0) {
                    res.add(asString);
                }
            }
        }
        // MySQL needs to have its "alter table add/drop fk" in separate execute()
        // (multiple add would work in 5.0)
        if (!forceCat && cts.size() > 0 && cts.get(0).getSyntax().getSystem() == SQLSystem.MYSQL)
            return res;
        else
            return Collections.singletonList(CollectionUtils.join(res, "\n"));
    }

    public static String catToString(List<? extends ChangeTable> cts, final String r) {
        return cat(cts, r, true).get(0);
    }

    private String name;
    private final SQLSyntax syntax;
    private final List<Object[]> fks;
    private final CollectionMap<ClauseType, String> clauses;
    private final CollectionMap<ClauseType, DeferredClause> inClauses;
    private final CollectionMap<ClauseType, DeferredClause> outClauses;

    public ChangeTable(final SQLSyntax syntax, final String name) {
        super();
        this.syntax = syntax;
        this.name = name;
        this.fks = new ArrayList<Object[]>();
        this.clauses = new CollectionMap<ClauseType, String>();
        this.inClauses = new CollectionMap<ClauseType, DeferredClause>();
        this.outClauses = new CollectionMap<ClauseType, DeferredClause>();

        // check that (T) this; will succeed
        if (this.getClass() != ReflectUtils.getTypeArguments(this, ChangeTable.class).get(0))
            throw new IllegalStateException("illegal subclass: " + this.getClass());
    }

    @SuppressWarnings("unchecked")
    protected final T thisAsT() {
        return (T) this;
    }

    public final SQLSyntax getSyntax() {
        return this.syntax;
    }

    /**
     * Reset this instance's attributes to default values. Ie clauses will be emptied but if the
     * name was changed it won't be changed back to its original value (since it has no default
     * value).
     */
    public void reset() {
        this.fks.clear();
        this.clauses.clear();
        this.inClauses.clear();
        this.outClauses.clear();
    }

    public boolean isEmpty() {
        return this.fks.isEmpty() && this.clauses.isEmpty() && this.inClauses.isEmpty() && this.outClauses.isEmpty();
    }

    @SuppressWarnings("unchecked")
    protected T mutateTo(ChangeTable<?> ct) {
        if (this.getSyntax() != ct.getSyntax())
            throw new IllegalArgumentException("not same syntax: " + this.getSyntax() + " != " + ct.getSyntax());
        this.setName(ct.getName());
        for (final Entry<ClauseType, Collection<String>> e : ct.clauses.entrySet()) {
            for (final String s : e.getValue())
                this.addClause(s, e.getKey());
        }
        for (final DeferredClause c : ct.inClauses.values()) {
            this.addClause(c);
        }
        for (final DeferredClause c : ct.outClauses.values()) {
            this.addOutsideClause(c);
        }
        for (final Object[] fk : ct.fks) {
            // don't create index, it is already added in outside clause
            this.addForeignConstraint((List<String>) fk[0], (SQLName) fk[1], false, (List<String>) fk[2]);
        }
        return thisAsT();
    }

    /**
     * Adds a varchar column not null and with '' as the default.
     * 
     * @param name the name of the column.
     * @param count the number of char.
     * @return this.
     */
    public final T addVarCharColumn(String name, int count) {
        return this.addColumn(name, "varchar(" + count + ") default '' NOT NULL");
    }

    public final T addDateAndTimeColumn(String name) {
        return this.addColumn(name, getSyntax().getDateAndTimeType());
    }

    public abstract T addColumn(String name, String definition);

    public final T addColumn(SQLField f) {
        return this.addColumn(f.getName(), this.getSyntax().getFieldDecl(f));
    }

    public final T addIndex(final Index index) {
        return this.addOutsideClause(getSyntax().getCreateIndex(index));
    }

    public final T addForeignConstraint(Link l, boolean createIndex) {
        return this.addForeignConstraint(l.getCols(), l.getContextualName(), createIndex, l.getRefCols());
    }

    public final T addForeignConstraint(String fieldName, SQLName refTable, String refCols) {
        return this.addForeignConstraint(singletonList(fieldName), refTable, true, singletonList(refCols));
    }

    /**
     * Adds a foreign constraint specifying that <code>fieldName</code> points to
     * <code>refTable</code>.
     * 
     * @param fieldName a field of this table.
     * @param refTable the destination of <code>fieldName</code>.
     * @param createIndex whether to also create an index on <code>fieldName</code>.
     * @param refCols the columns in <code>refTable</code>.
     * @return this.
     */
    public final T addForeignConstraint(final List<String> fieldName, SQLName refTable, boolean createIndex, List<String> refCols) {
        if (refTable.getItemCount() == 0)
            throw new IllegalArgumentException(refTable + " is empty.");
        this.fks.add(new Object[] { fieldName, refTable, refCols });
        if (createIndex)
            this.addOutsideClause(new OutsideClause() {
                @Override
                public ClauseType getType() {
                    return ClauseType.ADD_INDEX;
                }

                @Override
                public String asString(SQLName tableName) {
                    return getSyntax().getCreateIndex("_fki", tableName, fieldName);
                }
            });
        return thisAsT();
    }

    // * addForeignColumn = addColumn + addForeignConstraint

    /**
     * Add a column and its foreign constraint. If <code>table</code> is of length 1 it will be
     * prepended the root name of this table.
     * 
     * @param fk the field name, eg "ID_BAT".
     * @param table the name of the referenced table, eg BATIMENT.
     * @param pk the name of the referenced field, eg "ID".
     * @param defaultVal the default value for the column, eg "1".
     * @return this.
     */
    public T addForeignColumn(String fk, SQLName table, String pk, String defaultVal) {
        this.addColumn(fk, this.getSyntax().getIDType() + " DEFAULT " + defaultVal);
        return this.addForeignConstraint(fk, table, pk);
    }

    public T addForeignColumn(String fk, SQLTable foreignTable) {
        return this.addForeignColumn(fk, foreignTable, true);
    }

    /**
     * Add a column and its foreign constraint
     * 
     * @param fk the field name, eg "ID_BAT".
     * @param foreignTable the referenced table, eg /BATIMENT/.
     * @param absolute <code>true</code> if the link should include the whole name of
     *        <code>foreignTable</code>, <code>false</code> if the link should just be its name.
     * @return this.
     * @see #addForeignColumn(String, SQLName, String, String)
     */
    public T addForeignColumn(String fk, SQLTable foreignTable, final boolean absolute) {
        final String defaultVal = foreignTable.getUndefinedID() == SQLRow.NONEXISTANT_ID ? "NULL" : foreignTable.getUndefinedID() + "";
        final SQLName n = absolute ? foreignTable.getSQLName() : new SQLName(foreignTable.getName());
        return this.addForeignColumn(fk, n, foreignTable.getKey().getName(), defaultVal);
    }

    public T addUniqueConstraint(final String name, final List<String> cols) {
        // for many systems (at least pg & h2) constraint names must be unique in a schema
        return this.addClause(new DeferredClause() {
            @Override
            public String asString(ChangeTable<?> ct, SQLName tableName) {
                final String constrName = SQLSyntax.getSchemaUniqueName(tableName.getName(), name);
                return ct.getConstraintPrefix() + "CONSTRAINT " + SQLBase.quoteIdentifier(constrName) + " UNIQUE (" + SQLSyntax.quoteIdentifiers(cols) + ")";
            }

            @Override
            public ClauseType getType() {
                return ClauseType.ADD_CONSTRAINT;
            }
        });
    }

    private final String getConstraintPrefix() {
        return this instanceof AlterTable ? "ADD " : "";
    }

    /**
     * Add a clause inside the "CREATE TABLE".
     * 
     * @param s the clause to add, eg "CONSTRAINT c UNIQUE field".
     * @param type type of clause.
     * @return this.
     */
    public final T addClause(String s, final ClauseType type) {
        this.clauses.put(type, s);
        return thisAsT();
    }

    protected final List<String> getClauses(SQLName tableName, ClauseType type) {
        if (this.inClauses.size() == 0)
            return (List<String>) this.clauses.getNonNull(type);
        else {
            final List<String> res = new ArrayList<String>(this.clauses.getNonNull(type));
            for (final DeferredClause c : this.inClauses.getNonNull(type))
                res.add(c.asString(this, tableName));
            return res;
        }
    }

    protected final List<String> getClauses(SQLName tableName, Collection<ClauseType> types) {
        final List<String> res = new ArrayList<String>();
        for (final ClauseType type : types)
            res.addAll(this.getClauses(tableName, type));
        return res;
    }

    public final T addClause(DeferredClause s) {
        this.inClauses.put(s.getType(), s);
        return thisAsT();
    }

    /**
     * Add a clause outside the "CREATE TABLE".
     * 
     * @param s the clause to add, <code>null</code> being ignored, e.g. "CREATE INDEX ... ;".
     * @return this.
     */
    public final T addOutsideClause(DeferredClause s) {
        if (s != null)
            this.outClauses.put(s.getType(), s);
        return thisAsT();
    }

    protected final void outClausesAsString(final StringBuffer res, final SQLName tableName, final Set<ClauseType> types) {
        final List<DeferredClause> clauses = new ArrayList<DeferredClause>();
        for (final ClauseType type : types)
            clauses.addAll(this.outClauses.getNonNull(type));
        this.modifyOutClauses(clauses);
        if (clauses.size() > 0) {
            res.append("\n\n");
            res.append(CollectionUtils.join(clauses, "\n", new ITransformer<DeferredClause, String>() {
                @Override
                public String transformChecked(DeferredClause input) {
                    return input.asString(ChangeTable.this, tableName);
                }
            }));
        }
    }

    protected void modifyOutClauses(List<DeferredClause> clauses) {
    }

    // we can't implement asString() since our subclasses have more parameters
    // so we implement outClausesAsString()
    public abstract String asString(final String rootName);

    // called by #cat()
    protected abstract String asString(final String rootName, final ConcatStep step);

    // [ CONSTRAINT "BATIMENT_ID_SITE_fkey" FOREIGN KEY ("ID_SITE") REFERENCES "SITE"("ID") ON
    // DELETE CASCADE; ]
    @SuppressWarnings("unchecked")
    protected final List<String> getForeignConstraints(final String rootName) {
        final List<String> res = new ArrayList<String>(this.fks.size());
        for (final Object[] fk : this.fks) {
            final List<String> fieldName = (List<String>) fk[0];
            // resolve relative path, a table is identified by root.table
            final SQLName relRefTable = (SQLName) fk[1];
            final SQLName refTable = relRefTable.getItemCount() == 1 ? new SQLName(rootName, relRefTable.getName()) : relRefTable;
            final List<String> refCols = (List<String>) fk[2];
            res.add(getConstraintPrefix() + this.getSyntax().getFK(this.name + "_", fieldName, refTable, refCols));
        }
        return res;
    }

    @Override
    public String toString() {
        return this.asString(null);
    }

    public final String getName() {
        return this.name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public static interface DeferredClause {
        // ct necessary because CREATE TABLE( CONSTRAINT ) can become ALTER TABLE ADD CONSTRAINT
        // necessary since the full name of the table is only known in #asString(String)
        public String asString(final ChangeTable<?> ct, final SQLName tableName);

        public ClauseType getType();
    }

    public static abstract class OutsideClause implements DeferredClause {
        public abstract String asString(final SQLName tableName);

        @Override
        public String asString(ChangeTable<?> ct, SQLName tableName) {
            return this.asString(tableName);
        }
    }
}
