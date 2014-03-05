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
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.Index;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ReflectUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

    public static enum ConcatStep {
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
     * Allow to change names of tables.
     * 
     * @author Sylvain
     */
    public static class NameTransformer {

        /**
         * Transformer that does nothing.
         */
        public static final NameTransformer NOP = new NameTransformer();

        /**
         * Called once for each {@link ChangeTable}.
         * 
         * @param tableName the original table name.
         * @return the name that will be used.
         */
        public SQLName transformTableName(final SQLName tableName) {
            return tableName;
        }

        /**
         * Called once for each foreign key.
         * 
         * @param rootName the name of the root of the table.
         * @param tableName the name of the table.
         * @param linkDest the name of the destination table.
         * @return the name that will be used to reference the foreign table.
         */
        public SQLName transformLinkDestTableName(final String rootName, final String tableName, final SQLName linkDest) {
            return transformTableName(linkDest.getItemCount() == 1 ? new SQLName(rootName, linkDest.getName()) : linkDest);
        }
    }

    public static class ChangeRootNameTransformer extends NameTransformer {

        private final String r;

        public ChangeRootNameTransformer(String r) {
            super();
            this.r = r;
        }

        @Override
        public SQLName transformTableName(final SQLName tableName) {
            return new SQLName(this.r, tableName.getName());
        }

        @Override
        public SQLName transformLinkDestTableName(final String rootName, final String tableName, final SQLName linkDest) {
            return linkDest.getItemCount() == 1 ? transformTableName(new SQLName(rootName, linkDest.getName())) : linkDest;
        }
    }

    public static final Set<ClauseType> ORDERED_TYPES;

    static {
        final Set<ClauseType> tmp = new LinkedHashSet<ClauseType>(ClauseType.values().length);
        for (final ConcatStep step : ConcatStep.values())
            tmp.addAll(step.getTypes());
        assert tmp.equals(EnumSet.allOf(ClauseType.class)) : "ConcatStep is missing some types : " + tmp;
        ORDERED_TYPES = Collections.unmodifiableSet(tmp);
    }

    /**
     * Compute the SQL needed to create all passed tables, handling foreign key cycles.
     * 
     * @param cts the tables to create.
     * @param r where to create them.
     * @return the SQL needed.
     */
    public static List<String> cat(List<? extends ChangeTable<?>> cts, final String r) {
        return cat(cts, new ChangeRootNameTransformer(r));
    }

    public static List<String> cat(final List<? extends ChangeTable<?>> cts) {
        return cat(cts, NameTransformer.NOP);
    }

    public static List<String> cat(final List<? extends ChangeTable<?>> cts, final NameTransformer transf) {
        return cat(cts, transf, false);
    }

    /**
     * Compute the SQL needed to create all passed tables split at the passed boundaries. E.g. if
     * you wanted to create tables without constraints, insert some data and then add constraints,
     * you would pass <code>EnumSet.of(ConcatStep.ADD_CONSTRAINT)</code>.
     * 
     * @param cts the tables to create.
     * @param r where to create them.
     * @param boundaries where to split the SQL statements.
     * @return the SQL needed, by definition the list size is one more than <code>boundaries</code>
     *         size, e.g. if no boundaries are passed all SQL will be in one list.
     */
    public static List<List<String>> cat(final Collection<? extends ChangeTable<?>> cts, final String r, final EnumSet<ConcatStep> boundaries) {
        if (r == null)
            throw new NullPointerException("r is null");
        return cat(cts, new ChangeRootNameTransformer(r), boundaries);
    }

    public static List<List<String>> cat(final Collection<? extends ChangeTable<?>> cts, final NameTransformer transf, final EnumSet<ConcatStep> boundaries) {
        final List<List<String>> res = new ArrayList<List<String>>();
        List<String> current = null;
        for (final ConcatStep step : ConcatStep.values()) {
            if (current == null || boundaries.contains(step)) {
                current = new ArrayList<String>();
                res.add(current);
            }
            for (final ChangeTable<?> ct : cts) {
                final String asString = ct.asString(transf, step);
                if (asString != null && asString.length() > 0) {
                    current.add(asString);
                }
            }

        }
        assert res.size() == boundaries.size() + 1;
        return res;
    }

    private static List<String> cat(List<? extends ChangeTable<?>> cts, final NameTransformer transf, final boolean forceCat) {
        final List<String> res = cat(cts, transf, EnumSet.noneOf(ConcatStep.class)).get(0);
        // don't return [""] because the caller might test the size of the result and assume that
        // the DB was changed
        // MySQL needs to have its "alter table add/drop fk" in separate execute()
        // (multiple add would work in 5.0)
        if (!forceCat && (cts.size() == 0 || cts.get(0).getSyntax().getSystem() == SQLSystem.MYSQL))
            return res;
        else
            return Collections.singletonList(CollectionUtils.join(res, "\n"));
    }

    public static String catToString(List<? extends ChangeTable<?>> cts, final String r) {
        return cat(cts, new ChangeRootNameTransformer(r), true).get(0);
    }

    // allow to factor column name from table and FCSpec
    public static final class ForeignColSpec {

        static public ForeignColSpec fromCreateTable(SQLCreateTableBase<?> createTable) {
            final List<String> primaryKey = createTable.getPrimaryKey();
            if (primaryKey.size() != 1)
                throw new IllegalArgumentException("Not exactly one field in the foreign primary key : " + primaryKey);
            return new ForeignColSpec(null, new SQLName(createTable.getName()), primaryKey.get(0), null);
        }

        static public ForeignColSpec fromTable(SQLTable foreignTable) {
            return fromTable(foreignTable, true);
        }

        static public ForeignColSpec fromTable(SQLTable foreignTable, final boolean absolute) {
            if (foreignTable == null)
                throw new NullPointerException("null table");
            final String defaultVal = foreignTable.getKey().getType().toString(foreignTable.getUndefinedIDNumber());
            final SQLName n = absolute ? foreignTable.getSQLName() : new SQLName(foreignTable.getName());
            return new ForeignColSpec(null, n, foreignTable.getKey().getName(), defaultVal);
        }

        private String fk;
        private final SQLName table;
        private final String pk;
        private final String defaultVal;

        public ForeignColSpec(String fk, SQLName table, String pk, String defaultVal) {
            super();
            this.table = table;
            this.setColumnName(fk);
            this.pk = pk;
            this.defaultVal = defaultVal;
        }

        public final ForeignColSpec setColumnNameFromTable() {
            return this.setColumnNameWithSuffix("");
        }

        public final ForeignColSpec setColumnNameWithSuffix(final String suffix) {
            return this.setColumnName(SQLKey.PREFIX + getTable().getName() + (suffix.length() == 0 ? "" : "_" + suffix));
        }

        public final ForeignColSpec setColumnName(final String fk) {
            if (fk == null)
                this.setColumnNameFromTable();
            else
                this.fk = fk;
            return this;
        }

        public final String getColumnName() {
            return this.fk;
        }

        public final SQLName getTable() {
            return this.table;
        }

        public final String getPrimaryKeyName() {
            return this.pk;
        }

        public final String getDefaultVal() {
            return this.defaultVal;
        }

        public final FCSpec createFCSpec(final Rule updateRule, final Rule deleteRule) {
            return new FCSpec(Collections.singletonList(this.getColumnName()), this.getTable(), Collections.singletonList(this.getPrimaryKeyName()), updateRule, deleteRule);
        }
    }

    public static final class FCSpec {

        static public FCSpec createFromLink(final Link l) {
            return createFromLink(l, l.getTarget());
        }

        /**
         * Create an instance using an existing link but pointing to another table.
         * 
         * @param l an existing link, e.g. root1.LOCAL pointing to root1.BATIMENT.
         * @param newDest the new destination for the link, e.g. root2.BATIMENT.
         * @return a new instance, e.g. root1.LOCAL pointing to root2.BATIMENT.
         * @throws IllegalArgumentException if <code>newDest</code> is not compatible with
         *         <code>l.{@link Link#getTarget() getTarget()}</code>.
         */
        static public FCSpec createFromLink(final Link l, final SQLTable newDest) {
            if (newDest != l.getTarget()) {
                final List<SQLField> ffs = l.getFields();
                final Set<SQLField> pks = newDest.getPrimaryKeys();
                if (ffs.size() != pks.size())
                    throw new IllegalArgumentException("Size mismatch : " + ffs + " " + pks);
                int i = 0;
                for (final SQLField pk : pks) {
                    if (!ffs.get(i).getType().equals(pk.getType()))
                        throw new IllegalArgumentException("Type mismatch " + ffs.get(i) + " " + pk);
                    i++;
                }
            }
            return new FCSpec(l.getCols(), newDest.getContextualSQLName(l.getSource()), newDest.getPKsNames(), l.getUpdateRule(), l.getDeleteRule());
        }

        private final List<String> cols;
        private final SQLName refTable;
        private final List<String> refCols;
        private final Rule updateRule, deleteRule;

        public FCSpec(List<String> cols, SQLName refTable, List<String> refCols, final Rule updateRule, final Rule deleteRule) {
            super();
            if (refTable.getItemCount() == 0)
                throw new IllegalArgumentException(refTable + " is empty.");
            this.cols = Collections.unmodifiableList(new ArrayList<String>(cols));
            this.refTable = refTable;
            this.refCols = Collections.unmodifiableList(new ArrayList<String>(refCols));
            this.updateRule = updateRule;
            this.deleteRule = deleteRule;
        }

        public final List<String> getCols() {
            return this.cols;
        }

        public final SQLName getRefTable() {
            return this.refTable;
        }

        public final List<String> getRefCols() {
            return this.refCols;
        }

        public final Rule getUpdateRule() {
            return this.updateRule;
        }

        public final Rule getDeleteRule() {
            return this.deleteRule;
        }
    }

    private String rootName, name;
    private final SQLSyntax syntax;
    private final List<FCSpec> fks;
    private final CollectionMap<ClauseType, String> clauses;
    private final CollectionMap<ClauseType, DeferredClause> inClauses;
    private final CollectionMap<ClauseType, DeferredClause> outClauses;

    public ChangeTable(final SQLSyntax syntax, final String rootName, final String name) {
        super();
        this.syntax = syntax;
        this.rootName = rootName;
        this.name = name;
        this.fks = new ArrayList<FCSpec>();
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
        for (final FCSpec fk : ct.fks) {
            // don't create index, it is already added in outside clause
            this.addForeignConstraint(fk, false);
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

    /**
     * Adds a non-null integer column.
     * 
     * @param name the name of the column.
     * @param defaultVal the default value of the column.
     * @return this.
     */
    public final T addIntegerColumn(String name, int defaultVal) {
        return this.addIntegerColumn(name, defaultVal, false);
    }

    /**
     * Adds an integer column.
     * 
     * @param name the name of the column.
     * @param defaultVal the default value of the column, can be <code>null</code>.
     * @param nullable whether the column accepts NULL.
     * @return this.
     */
    public final T addIntegerColumn(String name, Integer defaultVal, boolean nullable) {
        return this.addNumberColumn(name, Integer.class, defaultVal, nullable);
    }

    public final T addLongColumn(String name, Long defaultVal, boolean nullable) {
        return this.addNumberColumn(name, Long.class, defaultVal, nullable);
    }

    public final T addShortColumn(String name, Short defaultVal, boolean nullable) {
        return this.addNumberColumn(name, Short.class, defaultVal, nullable);
    }

    /**
     * Adds a number column.
     * 
     * @param name the name of the column.
     * @param javaType the java class, it must be supported by the {@link #getSyntax() syntax}, e.g.
     *        Double.class.
     * @param defaultVal the default value of the column, can be <code>null</code>, e.g. 3.14.
     * @param nullable whether the column accepts NULL.
     * @return this.
     * @see SQLSyntax#getTypeNames(Class)
     */
    public final <N extends Number> T addNumberColumn(String name, Class<N> javaType, N defaultVal, boolean nullable) {
        final Set<String> typeNames = getSyntax().getTypeNames(javaType);
        if (typeNames.size() == 0)
            throw new IllegalArgumentException(javaType + " isn't supported by " + getSyntax());
        return this.addColumn(name, typeNames.iterator().next(), getNumberDefault(defaultVal), nullable);
    }

    final String getNumberDefault(final Number defaultVal) {
        return defaultVal == null ? null : defaultVal.toString();
    }

    /**
     * Adds a decimal column.
     * 
     * @param name the name of the column.
     * @param precision the total number of digits.
     * @param scale the number of digits after the decimal point.
     * @param defaultVal the default value of the column, can be <code>null</code>, e.g. 3.14.
     * @param nullable whether the column accepts NULL.
     * @return this.
     * @see SQLSyntax#getDecimal(int, int)
     * @see SQLSyntax#getDecimalIntPart(int, int)
     */
    public final T addDecimalColumn(String name, int precision, int scale, BigDecimal defaultVal, boolean nullable) {
        return this.addColumn(name, getSyntax().getDecimal(precision, scale), getNumberDefault(defaultVal), nullable);
    }

    public final T addBooleanColumn(String name, Boolean defaultVal, boolean nullable) {
        return this.addColumn(name, getSyntax().getBooleanType(), SQLType.getBoolean(getSyntax()).toString(defaultVal), nullable);
    }

    /**
     * Adds a column.
     * 
     * @param name the name of the column.
     * @param sqlType the SQL type, e.g. "double precision" or "varchar(32)".
     * @param defaultVal the SQL default value of the column, can be <code>null</code>, e.g. "3.14"
     *        or "'small text'".
     * @param nullable whether the column accepts NULL.
     * @return this.
     */
    public final T addColumn(String name, String sqlType, String defaultVal, boolean nullable) {
        return this.addColumn(name, getSyntax().getFieldDecl(sqlType, defaultVal, nullable));
    }

    public abstract T addColumn(String name, String definition);

    public final T addColumn(SQLField f) {
        return this.addColumn(f.getName(), f);
    }

    public final T addColumn(final String name, SQLField f) {
        return this.addColumn(name, this.getSyntax().getFieldDecl(f));
    }

    public final T addIndex(final Index index) {
        return this.addOutsideClause(getSyntax().getCreateIndex(index));
    }

    public final T addForeignConstraint(Link l, boolean createIndex) {
        return this.addForeignConstraint(FCSpec.createFromLink(l), createIndex);
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
        return this.addForeignConstraint(new FCSpec(fieldName, refTable, refCols, null, null), createIndex);
    }

    public final T addForeignConstraint(final FCSpec fkSpec, boolean createIndex) {
        this.fks.add(fkSpec);
        if (createIndex)
            this.addOutsideClause(new OutsideClause() {
                @Override
                public ClauseType getType() {
                    return ClauseType.ADD_INDEX;
                }

                @Override
                public String asString(SQLName tableName) {
                    return getSyntax().getCreateIndex("_fki", tableName, fkSpec.getCols());
                }
            });
        return thisAsT();
    }

    public final T removeForeignConstraint(final FCSpec fkSpec) {
        this.fks.remove(fkSpec);
        return thisAsT();
    }

    public final List<FCSpec> getForeignConstraints() {
        return Collections.unmodifiableList(this.fks);
    }

    // * addForeignColumn = addColumn + addForeignConstraint

    public T addForeignColumn(SQLCreateTableBase<?> createTable) {
        return this.addForeignColumn(ForeignColSpec.fromCreateTable(createTable));
    }

    /**
     * Add a foreign column to a table not yet created.
     * 
     * @param suffix the suffix of the column, used to tell apart multiple columns pointing to the
     *        same table, e.g. "" or "2".
     * @param createTable the table the new column must point to.
     * @return this.
     * @see #addForeignColumn(String, SQLCreateTableBase)
     */
    public T addForeignColumnWithSuffix(String suffix, SQLCreateTableBase<?> createTable) {
        return this.addForeignColumn(ForeignColSpec.fromCreateTable(createTable).setColumnNameWithSuffix(suffix));
    }

    /**
     * Add a foreign column to a table not yet created. Note: this method assumes that the foreign
     * table will be created in the same root as this table, like with
     * {@link ChangeTable#cat(List, String)}.
     * 
     * @param fk the field name, e.g. "ID_BAT".
     * @param createTable the table the new column must point to.
     * @return this.
     * @see #addForeignColumn(String, SQLName, String, String)
     */
    public T addForeignColumn(String fk, SQLCreateTableBase<?> createTable) {
        return this.addForeignColumn(ForeignColSpec.fromCreateTable(createTable).setColumnName(fk));
    }

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
        return this.addForeignColumn(new ForeignColSpec(fk, table, pk, defaultVal));
    }

    public T addForeignColumn(final ForeignColSpec spec) {
        return this.addForeignColumn(spec, null, null);
    }

    public T addForeignColumn(final ForeignColSpec spec, final Rule updateRule, final Rule deleteRule) {
        this.addColumn(spec.getColumnName(), this.getSyntax().getIDType() + " DEFAULT " + spec.getDefaultVal());
        return this.addForeignConstraint(spec.createFCSpec(updateRule, deleteRule), true);
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
        return this.addForeignColumn(ForeignColSpec.fromTable(foreignTable, absolute).setColumnName(fk));
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

    protected abstract String getConstraintPrefix();

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

    public final String asString() {
        return this.asString(NameTransformer.NOP);
    }

    public final String asString(final String rootName) {
        return this.asString(new ChangeRootNameTransformer(rootName));
    }

    // we can't implement asString() since our subclasses have more parameters
    // so we implement outClausesAsString()
    public abstract String asString(final NameTransformer transf);

    // called by #cat()
    protected abstract String asString(final NameTransformer transf, final ConcatStep step);

    // [ CONSTRAINT "BATIMENT_ID_SITE_fkey" FOREIGN KEY ("ID_SITE") REFERENCES "SITE"("ID") ON
    // DELETE CASCADE; ]
    protected final List<String> getForeignConstraints(final NameTransformer transf) {
        final List<String> res = new ArrayList<String>(this.fks.size());
        for (final FCSpec fk : this.fks) {
            // resolve relative path, a table is identified by root.table
            final SQLName relRefTable = fk.getRefTable();
            final SQLName refTable = transf.transformLinkDestTableName(getRootName(), getName(), relRefTable);
            res.add(getConstraintPrefix() + this.getSyntax().getFK(this.name + "_", fk.getCols(), refTable, fk.getRefCols(), fk.getUpdateRule(), fk.getDeleteRule()));
        }
        return res;
    }

    @Override
    public String toString() {
        return this.asString();
    }

    public final String getName() {
        return this.name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final String getRootName() {
        return this.rootName;
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
