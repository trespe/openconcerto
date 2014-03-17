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

import org.openconcerto.sql.model.Order.Direction;
import org.openconcerto.sql.model.Order.Nulls;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.Step;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author ILM Informatique 10 mai 2004
 */
public final class SQLSelect {

    public static enum ArchiveMode {
        UNARCHIVED, ARCHIVED, BOTH
    }

    public static final ArchiveMode UNARCHIVED = ArchiveMode.UNARCHIVED;
    public static final ArchiveMode ARCHIVED = ArchiveMode.ARCHIVED;
    public static final ArchiveMode BOTH = ArchiveMode.BOTH;

    /**
     * Quote %-escaped parameters. %% : %, %s : quoteString, %i : quote, %f : quote(getFullName()),
     * %n : quote(getName()).
     * 
     * @param pattern a string with %, eg "SELECT * FROM %n where %f like '%%a%%'".
     * @param params the parameters, eg [ /TENSION/, |TENSION.LABEL| ].
     * @return pattern with % replaced, eg SELECT * FROM "TENSION" where "TENSION.LABEL" like '%a%'.
     */
    public static final String quote(final String pattern, Object... params) {
        return SQLBase.quoteStd(pattern, params);
    }

    // [String], eg : [SITE.ID_SITE, AVG(AGE)]
    private final List<String> select;
    // [SQLField], eg : [|SITE.ID_SITE|], known fields in this select (addRawSelect)
    private final List<SQLField> selectFields;
    private Where where;
    private final List<FieldRef> groupBy;
    private Where having;
    // [String]
    private final List<String> order;
    private final FromClause from;
    // all the tables (and their aliases) in this select
    private final AliasedTables declaredTables;
    // {String}, aliases not to include in the FROM clause
    private final Set<String> joinAliases;
    // [String]
    private final List<SQLSelectJoin> joins;

    // la politique générale pour l'exclusion des indéfinis
    private boolean generalExcludeUndefined;
    // [SQLTable => Boolean]
    private Map<SQLTable, Boolean> excludeUndefined;
    // null key for general
    private final Map<SQLTable, ArchiveMode> archivedPolicy;
    // DISTINCT
    private boolean distinct;
    // whether to wait for an UPDATE or DELETE transaction to complete
    private boolean waitTrx;
    // which tables to wait (avoid SELECT FOR UPDATE/SHARE cannot be applied to the nullable side of
    // an outer join)
    private final List<String> waitTrxTables;
    // number of rows to return
    private Integer limit;

    /**
     * Create a new SQLSelect.
     * 
     * @param base the database of the request.
     * @deprecated use {@link #SQLSelect(DBSystemRoot)}
     */
    public SQLSelect(SQLBase base) {
        this(base, false);
    }

    /**
     * Create a new SQLSelect.
     * 
     * @param base the database of the request.
     * @param plain whether this request should automatically add a where clause for archived and
     *        undefined.
     * @deprecated use {@link #SQLSelect(DBSystemRoot, boolean)}
     */
    public SQLSelect(SQLBase base, boolean plain) {
        this(base.getDBSystemRoot(), plain);
    }

    public SQLSelect() {
        this(false);
    }

    public SQLSelect(boolean plain) {
        this((DBSystemRoot) null, plain);
    }

    /**
     * Create a new SQLSelect.
     * 
     * @param sysRoot the database of the request, can be <code>null</code> (it will come from
     *        declared tables).
     * @param plain whether this request should automatically add a where clause for archived and
     *        undefined.
     */
    public SQLSelect(DBSystemRoot sysRoot, boolean plain) {
        this.select = new ArrayList<String>();
        this.selectFields = new ArrayList<SQLField>();
        this.where = null;
        this.groupBy = new ArrayList<FieldRef>();
        this.having = null;
        this.order = new ArrayList<String>();
        this.from = new FromClause();
        this.declaredTables = new AliasedTables(sysRoot);
        this.joinAliases = new HashSet<String>();
        this.joins = new ArrayList<SQLSelectJoin>();
        // false by default cause it slows things down
        this.distinct = false;
        this.excludeUndefined = new HashMap<SQLTable, Boolean>();
        this.archivedPolicy = new HashMap<SQLTable, ArchiveMode>();
        // false by default cause it is quite incompatible (and maybe slow too)
        this.waitTrx = false;
        this.waitTrxTables = new ArrayList<String>();
        if (plain) {
            this.generalExcludeUndefined = false;
            this.setArchivedPolicy(BOTH);
        } else {
            this.generalExcludeUndefined = true;
            this.setArchivedPolicy(UNARCHIVED);
        }
        // otherwise getArchiveWhere() fails
        assert this.archivedPolicy.containsKey(null);
    }

    /**
     * Clone un SQLSelect.
     * 
     * @param orig l'instance à cloner.
     */
    public SQLSelect(SQLSelect orig) {
        // ATTN synch les implémentations des attributs (LinkedHashSet, ...)
        this.select = new ArrayList<String>(orig.select);
        this.selectFields = new ArrayList<SQLField>(orig.selectFields);
        this.where = orig.where == null ? null : new Where(orig.where);
        this.groupBy = new ArrayList<FieldRef>(orig.groupBy);
        this.having = orig.having == null ? null : new Where(orig.having);
        this.order = new ArrayList<String>(orig.order);
        this.from = new FromClause(orig.from);
        this.declaredTables = new AliasedTables(orig.declaredTables);
        this.joinAliases = new HashSet<String>(orig.joinAliases);
        this.joins = new ArrayList<SQLSelectJoin>(orig.joins);
        this.generalExcludeUndefined = orig.generalExcludeUndefined;
        this.excludeUndefined = new HashMap<SQLTable, Boolean>(orig.excludeUndefined);
        this.archivedPolicy = new HashMap<SQLTable, ArchiveMode>(orig.archivedPolicy);
        this.distinct = orig.distinct;

        this.waitTrx = orig.waitTrx;
        this.waitTrxTables = new ArrayList<String>(orig.waitTrxTables);
    }

    public final SQLSystem getSQLSystem() {
        final DBSystemRoot sysRoot = this.declaredTables.getSysRoot();
        if (sysRoot == null)
            throw new IllegalStateException("No systemRoot supplied (neither in the constructor nor by adding an item)");
        return sysRoot.getServer().getSQLSystem();
    }

    public String asString() {
        final SQLSystem sys = this.getSQLSystem();

        final StringBuffer result = new StringBuffer(512);
        result.append("SELECT ");
        if (this.distinct)
            result.append("DISTINCT ");
        if (this.getLimit() != null && sys == SQLSystem.MSSQL) {
            result.append("TOP ");
            result.append(this.getLimit());
            result.append(' ');
        }
        result.append(CollectionUtils.join(this.select, ", "));

        result.append("\n " + this.from.getSQL());

        // si c'est null, ca marche
        Where archive = this.where;
        // ne pas exclure les archivés et les indéfinis des joins : SQLSelectJoin does it
        final Collection<String> fromAliases = CollectionUtils.substract(this.declaredTables.getAliases(), this.joinAliases);
        for (final String alias : fromAliases) {
            final SQLTable fromTable = this.declaredTables.getTable(alias);
            // on ignore les lignes archivées
            archive = Where.and(getArchiveWhere(fromTable, alias), archive);
            // on ignore les lignes indéfines
            archive = Where.and(getUndefWhere(fromTable, alias), archive);
        }
        // archive == null si pas d'archive et pas d'undefined
        if (archive != null && archive.getClause() != "") {
            result.append("\n WHERE ");
            result.append(archive.getClause());
        }
        if (!this.groupBy.isEmpty()) {
            result.append("\n GROUP BY ");
            result.append(CollectionUtils.join(this.groupBy, ", ", new ITransformer<FieldRef, String>() {
                @Override
                public String transformChecked(FieldRef input) {
                    return input.getFieldRef();
                }
            }));
        }
        if (this.having != null) {
            result.append("\n HAVING ");
            result.append(this.having.getClause());
        }
        if (!this.order.isEmpty()) {
            result.append("\n ORDER BY ");
            result.append(CollectionUtils.join(this.order, ", "));
        }
        if (this.getLimit() != null && sys != SQLSystem.MSSQL) {
            result.append("\nLIMIT ");
            result.append(this.getLimit());
        }
        // wait for other update trx to finish before selecting
        if (this.waitTrx) {
            if (sys.equals(SQLSystem.POSTGRESQL)) {
                result.append(" FOR SHARE");
                if (this.waitTrxTables.size() > 0)
                    result.append(" OF " + CollectionUtils.join(this.waitTrxTables, ", "));
            } else if (sys.equals(SQLSystem.MYSQL))
                result.append(" LOCK IN SHARE MODE");
        }

        return result.toString();
    }

    Where getArchiveWhere(final SQLTable table, final String alias) {
        final Where res;
        // null key is the default
        final ArchiveMode m = this.archivedPolicy.containsKey(table) ? this.archivedPolicy.get(table) : this.archivedPolicy.get(null);
        assert m != null : "no default policy";
        if (table.isArchivable() && m != BOTH) {
            final Object archiveValue;
            if (table.getArchiveField().getType().getJavaType().equals(Boolean.class)) {
                archiveValue = m == ARCHIVED;
            } else {
                archiveValue = m == ARCHIVED ? 1 : 0;
            }
            res = new Where(this.createRef(alias, table.getArchiveField()), "=", archiveValue);
        } else
            res = null;
        return res;
    }

    Where getUndefWhere(final SQLTable table, final String alias) {
        final Where res;
        final Boolean exclude = this.excludeUndefined.get(table);
        if (table.isRowable() && (exclude == Boolean.TRUE || (exclude == null && this.generalExcludeUndefined))) {
            // no need to use NULL_IS_DATA_NEQ since we're in FROM or JOIN and ID cannot be null
            res = new Where(this.createRef(alias, table.getKey()), "!=", table.getUndefinedID());
        } else
            res = null;
        return res;
    }

    public String toString() {
        return this.asString();
    }

    /**
     * Fields names of the SELECT
     * 
     * @return a list of fields names used by the SELECT
     */
    public List<String> getSelect() {
        return this.select;
    }

    /**
     * Fields of the SELECT
     * 
     * @return a list of fields used by the SELECT
     */
    public final List<SQLField> getSelectFields() {
        return this.selectFields;
    }

    public List<String> getOrder() {
        return this.order;
    }

    public Where getWhere() {
        return this.where;
    }

    public final boolean contains(String alias) {
        return this.declaredTables.contains(alias);
    }

    /**
     * Whether this SELECT already references table (eg by a from or a join). For example, if not
     * you can't ORDER BY with a field of that table.
     * 
     * @param table the table to test.
     * @return <code>true</code> if table is already in this.
     */
    public final boolean contains(SQLTable table) {
        return this.contains(table.getName());
    }

    // *** group by / having

    public SQLSelect addGroupBy(FieldRef f) {
        this.groupBy.add(f);
        return this;
    }

    public SQLSelect setHaving(Where w) {
        this.having = w;
        return this;
    }

    // *** order by

    /**
     * Ajoute un ORDER BY.
     * 
     * @param t a table alias.
     * @return this.
     * @throws IllegalArgumentException si t n'est pas ordonné.
     * @throws IllegalStateException si t n'est pas dans cette requete.
     * @see SQLTable#isOrdered()
     */
    public SQLSelect addOrder(String t) {
        return this.addOrder(this.getTableRef(t));
    }

    public SQLSelect addOrder(TableRef t) {
        return this.addOrder(t, true);
    }

    /**
     * Add an ORDER BY {@link SQLTable#getOrderField() t.ORDER}.
     * 
     * @param t the table.
     * @param fieldMustExist if <code>true</code> then <code>t</code> must be
     *        {@link SQLTable#isOrdered() ordered}.
     * @return this.
     * @throws IllegalArgumentException if <code>t</code> isn't ordered and <code>mustExist</code>
     *         is <code>true</code>.
     */
    public SQLSelect addOrder(TableRef t, final boolean fieldMustExist) {
        final SQLField orderField = t.getTable().getOrderField();
        if (orderField != null)
            this.addFieldOrder(t.getField(orderField.getName()));
        else if (fieldMustExist)
            throw new IllegalArgumentException("table is not ordered : " + t);
        return this;
    }

    public SQLSelect addFieldOrder(FieldRef fieldRef) {
        return this.addFieldOrder(fieldRef, Order.asc());
    }

    public SQLSelect addFieldOrder(FieldRef fieldRef, final Direction dir) {
        return this.addFieldOrder(fieldRef, dir, null);
    }

    public SQLSelect addFieldOrder(FieldRef fieldRef, final Direction dir, final Nulls nulls) {
        // with Derby if you ORDER BY w/o mentioning the field in the select clause
        // you can't get the table names of columns in a result set.
        if (fieldRef.getField().getServer().getSQLSystem().equals(SQLSystem.DERBY))
            this.addSelect(fieldRef);

        return this.addRawOrder(fieldRef.getFieldRef() + dir.getSQL() + (nulls == null ? "" : nulls.getSQL()));
    }

    /**
     * Add an ORDER BY that is not an ORDER field.
     * 
     * @param selectItem an item that appears in the select, either a field reference or an alias.
     * @return this.
     */
    public SQLSelect addRawOrder(String selectItem) {
        this.order.add(selectItem);
        return this;
    }

    public SQLSelect clearOrder() {
        this.order.clear();
        return this;
    }

    /**
     * Ajoute un ORDER BY. Ne fais rien si t n'est pas ordonné.
     * 
     * @param t la table.
     * @return this.
     * @throws IllegalStateException si t n'est pas dans cette requete.
     */
    public SQLSelect addOrderSilent(String t) {
        return this.addOrder(this.getTableRef(t), false);
    }

    // *** select

    /**
     * Ajoute un champ au SELECT.
     * 
     * @param f le champ à ajouter.
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addSelect(FieldRef f) {
        return this.addSelect(f, null);
    }

    /**
     * Permet d'ajouter plusieurs champs.
     * 
     * @param s une collection de FieldRef.
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addAllSelect(Collection<? extends FieldRef> s) {
        for (final FieldRef element : s) {
            this.addSelect(element);
        }
        return this;
    }

    /**
     * Permet d'ajouter plusieurs champs d'une même table sans avoir à les préfixer.
     * 
     * @param t la table.
     * @param s une collection de nom de champs, eg "NOM".
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addAllSelect(TableRef t, Collection<String> s) {
        for (final String fieldName : s) {
            this.addSelect(t.getField(fieldName));
        }
        return this;
    }

    /**
     * Ajoute une fonction d'un champ au SELECT.
     * 
     * @param f le champ, eg "PERSON.AGE".
     * @param function la fonction, eg "AVG".
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addSelect(FieldRef f, String function) {
        return this.addSelect(f, function, null);
    }

    public SQLSelect addSelect(FieldRef f, String function, String alias) {
        String s = f.getFieldRef();
        if (function != null) {
            s = function + "(" + s + ")";
        }
        this.from.add(this.declaredTables.add(f));
        this.selectFields.add(f.getField());
        return this.addRawSelect(s, alias);
    }

    /**
     * To add an item that is not a field.
     * 
     * @param expr any legal exp in a SELECT statement (eg a constant, a complex function, etc).
     * @param alias a name for the expression, may be <code>null</code>.
     * @return this.
     */
    public SQLSelect addRawSelect(String expr, String alias) {
        if (alias != null) {
            expr += " as " + SQLBase.quoteIdentifier(alias);
        }
        this.select.add(expr);
        return this;
    }

    /**
     * Ajoute une fonction prenant * comme paramètre.
     * 
     * @param function la fonction, eg "COUNT".
     * @return this pour pouvoir chaîner.
     */
    public SQLSelect addSelectFunctionStar(String function) {
        return this.addRawSelect(function + "(*)", null);
    }

    public SQLSelect addSelectStar(TableRef table) {
        this.select.add(SQLBase.quoteIdentifier(table.getAlias()) + ".*");
        this.from.add(this.declaredTables.add(table));
        this.selectFields.addAll(table.getTable().getOrderedFields());
        return this;
    }

    // *** from

    public SQLSelect addFrom(SQLTable table, String alias) {
        return this.addFrom(new AliasedTable(table, alias));
    }

    /**
     * Explicitely add a table to the from clause. Rarely needed since tables are auto added by
     * addSelect(), setWhere() and addJoin().
     * 
     * @param t the table to add.
     * @return this.
     */
    public SQLSelect addFrom(TableRef t) {
        this.from.add(this.declaredTables.add(t));
        return this;
    }

    // *** where

    /**
     * Change la clause where de cette requete.
     * 
     * @param w la nouvelle clause, <code>null</code> pour aucune clause.
     * @return this.
     */
    public SQLSelect setWhere(Where w) {
        this.where = w;
        // FIXME si where était non null alors on a ajouté des tables dans FROM
        // qui ne sont peut être plus utiles
        // une solution : ne calculer le from que dans asString() => marche pas car on s'en
        // sert dans addOrder
        if (w != null) {
            for (final FieldRef f : w.getFields()) {
                this.from.add(this.declaredTables.add(f));
            }
        }
        return this;
    }

    public SQLSelect setWhere(FieldRef field, String op, int i) {
        return this.setWhere(new Where(field, op, i));
    }

    /**
     * Ajoute le Where passé à celui de ce select.
     * 
     * @param w le Where à ajouter.
     * @return this.
     */
    public SQLSelect andWhere(Where w) {
        return this.setWhere(Where.and(this.getWhere(), w));
    }

    // *** join

    // simple joins (with foreign field)

    /**
     * Add a join to this SELECT. Eg if <code>f</code> is |BATIMENT.ID_SITE|, then "join SITE on
     * BATIMENT.ID_SITE = SITE.ID" will be added.
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param f a foreign key, eg |BATIMENT.ID_SITE|.
     * @return the added join.
     */
    public SQLSelectJoin addJoin(String joinType, FieldRef f) {
        return this.addJoin(joinType, f, null);
    }

    /**
     * Add a join to this SELECT. Eg if <code>f</code> is bat.ID_SITE and <code>alias</code> is "s",
     * then "join SITE s on bat.ID_SITE = s.ID" will be added.
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param f a foreign key, eg obs.ID_ARTICLE_2.
     * @param alias the alias for joined table, can be <code>null</code>, eg "art2".
     * @return the added join.
     */
    public SQLSelectJoin addJoin(String joinType, FieldRef f, final String alias) {
        final SQLTable foreignTable = f.getField().getForeignTable();
        // check that f is contained in this
        this.getTable(f.getAlias());
        // handle null
        final TableRef aliased = this.declaredTables.add(alias, foreignTable);
        return this.addJoin(new SQLSelectJoin(this, joinType, aliased, f, aliased));
    }

    // arbitrary joins

    /**
     * Add a join to this SELECT, inferring the joined table from the where.
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param w the where joining the new table.
     * @return the added join.
     * @throws IllegalArgumentException if <code>w</code> hasn't exactly one table not yet
     *         {@link #contains(String) contained} in this.
     */
    public SQLSelectJoin addJoin(String joinType, final Where w) {
        final Set<AliasedTable> tables = new HashSet<AliasedTable>();
        for (final FieldRef f : w.getFields()) {
            if (!this.contains(f.getAlias())) {
                tables.add(new AliasedTable(f.getField().getTable(), f.getAlias()));
            }
        }
        if (tables.size() == 0)
            throw new IllegalArgumentException("No tables to add in " + w);
        if (tables.size() > 1)
            throw new IllegalArgumentException("More than one table to add (" + tables + ") in " + w);
        final AliasedTable joinedTable = tables.iterator().next();
        return addJoin(joinType, joinedTable.getTable(), joinedTable.getAlias(), w);
    }

    public SQLSelectJoin addJoin(String joinType, SQLTable joinedTable, final Where w) {
        return this.addJoin(joinType, joinedTable, null, w);
    }

    public SQLSelectJoin addJoin(String joinType, SQLTable joinedTable, final String alias, final Where w) {
        // handle null
        final TableRef aliased = this.declaredTables.add(alias, joinedTable);
        return this.addJoin(new SQLSelectJoin(this, joinType, aliased, w));
    }

    /**
     * Add a join that goes backward through a foreign key, eg LEFT JOIN "KD_2006"."BATIMENT" "bat"
     * on "s"."ID" = "bat"."ID_SITE".
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param joinAlias the alias for the joined table, must not exist, eg "bat".
     * @param ff the foreign field, eg |BATIMENT.ID_SITE|.
     * @param foreignTableAlias the alias for the foreign table, must exist, eg "sit" or
     *        <code>null</code> for "SITE".
     * @return the added join.
     */
    public SQLSelectJoin addBackwardJoin(String joinType, final String joinAlias, SQLField ff, final String foreignTableAlias) {
        return this.addBackwardJoin(joinType, new AliasedField(ff, joinAlias), foreignTableAlias);
    }

    /**
     * Add a join that goes backward through a foreign key, eg LEFT JOIN "KD_2006"."BATIMENT" "bat"
     * on "s"."ID" = "bat"."ID_SITE".
     * 
     * @param joinType can be INNER, LEFT or RIGHT.
     * @param ff the foreign field, the alias must not exist, e.g. bat.ID_SITE.
     * @param foreignTableAlias the alias for the foreign table, must exist, e.g. "sit" or
     *        <code>null</code> for "SITE".
     * @return the added join.
     */
    public SQLSelectJoin addBackwardJoin(String joinType, final FieldRef ff, final String foreignTableAlias) {
        final SQLTable foreignTable = ff.getField().getForeignTable();
        // handle null foreignTableAlias
        // verify that the alias already exists
        final TableRef aliasedFT = this.getTableRef(foreignTableAlias == null ? foreignTable.getName() : foreignTableAlias);
        // verify aliasedFT coherence
        if (aliasedFT.getTable() != foreignTable)
            throw new IllegalArgumentException("wrong alias: " + aliasedFT + " is not an alias to the target of " + ff);

        final TableRef aliased = this.declaredTables.add(ff);
        return this.addJoin(new SQLSelectJoin(this, joinType, aliased, ff, aliasedFT));
    }

    private final SQLSelectJoin addJoin(SQLSelectJoin j) {
        // first check if the joined table is not already in this from
        this.from.add(j);
        this.joinAliases.add(j.getAlias());
        this.joins.add(j);
        return j;
    }

    public final List<SQLSelectJoin> getJoins() {
        return Collections.unmodifiableList(this.joins);
    }

    /**
     * Get the join going through <code>ff</code>, regardless of its alias.
     * 
     * @param ff a foreign field, eg |BATIMENT.ID_SITE|.
     * @return the corresponding join or <code>null</code> if none found, eg LEFT JOIN "test"."SITE"
     *         "s" on "bat"."ID_SITE"="s"."ID"
     */
    public final SQLSelectJoin getJoinFromField(SQLField ff) {
        for (final SQLSelectJoin j : this.joins) {
            if (j.hasForeignField() && j.getForeignField().getField().equals(ff)) {
                return j;
            }
        }
        return null;
    }

    /**
     * The first join adding the passed table.
     * 
     * @param t the table to search for, e.g. /LOCAL/.
     * @return the first matching join or <code>null</code> if none found, eg LEFT JOIN
     *         "test"."LOCAL" "l" on "r"."ID_LOCAL"="l"."ID"
     */
    public final SQLSelectJoin findJoinAdding(SQLTable t) {
        for (final SQLSelectJoin j : this.joins) {
            if (j.getJoinedTable().getTable().equals(t)) {
                return j;
            }
        }
        return null;
    }

    /**
     * The join adding the passed table alias.
     * 
     * @param alias a table alias, e.g. "l".
     * @return the matching join or <code>null</code> if none found, eg LEFT JOIN "test"."LOCAL" "l"
     *         on "r"."ID_LOCAL"="l"."ID"
     */
    public final SQLSelectJoin getJoinAdding(final String alias) {
        for (final SQLSelectJoin j : this.joins) {
            if (j.getAlias().equals(alias)) {
                return j;
            }
        }
        return null;
    }

    /**
     * Get the join going through <code>ff</code>, matching its alias.
     * 
     * @param ff a foreign field, eg |BATIMENT.ID_SITE|.
     * @return the corresponding join or <code>null</code> if none found, eg <code>null</code> if
     *         this only contains LEFT JOIN "test"."SITE" "s" on "bat"."ID_SITE"="s"."ID"
     */
    public final SQLSelectJoin getJoin(FieldRef ff) {
        for (final SQLSelectJoin j : this.joins) {
            // handle AliasedField equals( SQLField )
            if (j.hasForeignField() && j.getForeignField().getFieldRef().equals(ff.getFieldRef())) {
                return j;
            }
        }
        return null;
    }

    private SQLSelectJoin getJoin(SQLField ff, String foreignTableAlias) {
        for (final SQLSelectJoin j : this.joins) {
            if (j.hasForeignField() && j.getForeignField().getField().equals(ff) && j.getForeignTable().getAlias().equals(foreignTableAlias)) {
                return j;
            }
        }
        return null;
    }

    /**
     * Assure that there's a path from <code>tableAlias</code> through <code>p</code>, adding the
     * missing joins.
     * 
     * @param tableAlias the table at the start, eg "loc".
     * @param p the path that must be added, eg LOCAL-BATIMENT-SITE.
     * @return the alias of the last table of the path, "sit".
     */
    public TableRef assurePath(String tableAlias, Path p) {
        return this.followPath(tableAlias, p, true);
    }

    public TableRef followPath(String tableAlias, Path p) {
        return this.followPath(tableAlias, p, false);
    }

    /**
     * Return the alias at the end of the passed path.
     * 
     * @param tableAlias the table at the start, eg "loc".
     * @param p the path to follow, eg LOCAL-BATIMENT-SITE.
     * @param create <code>true</code> if missing joins should be created.
     * @return the alias of the last table of the path or <code>null</code>, eg "sit".
     */
    public TableRef followPath(String tableAlias, Path p, final boolean create) {
        final TableRef firstTableRef = this.getTableRef(tableAlias);
        final SQLTable firstTable = firstTableRef.getTable();
        if (!p.getFirst().equals(firstTable) && !p.getLast().equals(firstTable))
            throw new IllegalArgumentException("neither ends of " + p + " is " + firstTable);
        else if (!p.getFirst().equals(firstTable))
            return followPath(tableAlias, p.reverse(), create);

        TableRef current = firstTableRef;
        for (int i = 0; i < p.length(); i++) {
            final Step step = p.getStep(i);
            // |BATIMENT.ID_SITE|
            final SQLField ff = step.getSingleField();
            // TODO handle multi-link:
            // JOIN test.article art ON obs.ID_ARTICLE_1 = art.ID or obs.ID_ARTICLE_2 = art.ID
            if (ff == null)
                throw new IllegalArgumentException(p + " has more than 1 link at index " + i);
            final SQLSelectJoin j;
            // are we currently at the start of the foreign field or at the destination
            final boolean forward = step.getDirection() == org.openconcerto.sql.model.graph.Link.Direction.FOREIGN;
            if (forward) {
                // bat.ID_SITE
                j = this.getJoin(current.getField(ff.getName()));
            } else {
                // sit.ID
                // on cherche (1 alias de ff.getTable()).ff = current.ID
                j = this.getJoin(ff, current.getAlias());
            }
            if (j != null)
                current = j.getJoinedTable();
            else if (create) {
                // we must add a join
                final String uniqAlias = getUniqueAlias("assurePath_" + i);
                final SQLSelectJoin createdJoin;
                if (forward)
                    // JOIN test.SITE uniqAlias on current.ID_SITE = uniqAlias.ID
                    createdJoin = this.addJoin("LEFT", current.getField(ff.getName()), uniqAlias);
                else
                    // JOIN test.BATIMENT uniqAlias on uniqAlias.ID_SITE = current.ID
                    createdJoin = this.addBackwardJoin("LEFT", uniqAlias, ff, current.getAlias());
                current = createdJoin.getJoinedTable();
            } else
                return null;
        }

        return current;
    }

    public boolean isExcludeUndefined() {
        return this.generalExcludeUndefined;
    }

    public void setExcludeUndefined(boolean excludeUndefined) {
        this.generalExcludeUndefined = excludeUndefined;
    }

    public void setExcludeUndefined(boolean exclude, SQLTable table) {
        this.excludeUndefined.put(table, Boolean.valueOf(exclude));
    }

    public void setArchivedPolicy(ArchiveMode policy) {
        this.setArchivedPolicy(null, policy);
    }

    public void setArchivedPolicy(SQLTable t, ArchiveMode policy) {
        this.archivedPolicy.put(t, policy);
    }

    public final void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    /**
     * Whether this SELECT should wait until all committed transaction are complete. This prevent a
     * SELECT following an UPDATE from seeing rows as they were before. NOTE that this may conflict
     * with other clauses (GROUP BY, DISTINCT, etc.).
     * 
     * @param waitTrx <code>true</code> if this select should wait.
     */
    public void setWaitPreviousWriteTX(boolean waitTrx) {
        this.waitTrx = waitTrx;
    }

    public void addWaitPreviousWriteTXTable(String table) {
        this.setWaitPreviousWriteTX(true);
        this.waitTrxTables.add(SQLBase.quoteIdentifier(table));
    }

    /**
     * Set the maximum number of rows to return.
     * 
     * @param limit the number of rows, <code>null</code> meaning no limit
     * @return this.
     */
    public SQLSelect setLimit(final Integer limit) {
        this.limit = limit;
        return this;
    }

    public final Integer getLimit() {
        return this.limit;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SQLSelect)
            // MAYBE use instance variables
            return this.asString().equals(((SQLSelect) o).asString());
        else
            return false;
    }

    @Override
    public int hashCode() {
        // don't use asString() which is more CPU intensive
        return this.select.hashCode() + this.from.getSQL().hashCode() + (this.where == null ? 0 : this.where.hashCode());
    }

    /**
     * Returns the table designated in this select by name.
     * 
     * @param name a table name or an alias, eg "OBSERVATION" or "art2".
     * @return the table named <code>name</code>.
     * @throws IllegalArgumentException if <code>name</code> is unknown to this select.
     */
    public final SQLTable getTable(String name) {
        return this.getTableRef(name).getTable();
    }

    public final TableRef getTableRef(String alias) {
        final TableRef res = this.declaredTables.getAliasedTable(alias);
        if (res == null)
            throw new IllegalArgumentException("alias not in this select : " + alias);
        return res;
    }

    /**
     * Return the alias for the passed table.
     * 
     * @param t a table.
     * @return the alias for <code>t</code>, or <code>null</code> if <code>t</code> is not exactly
     *         once in this.
     */
    public final TableRef getAlias(SQLTable t) {
        return this.declaredTables.getAlias(t);
    }

    public final List<TableRef> getAliases(SQLTable t) {
        return this.declaredTables.getAliases(t);
    }

    public final FieldRef getAlias(SQLField f) {
        return this.getAlias(f.getTable()).getField(f.getName());
    }

    /**
     * See http://www.postgresql.org/docs/8.2/interactive/sql-syntax-lexical.html#SQL-SYNTAX-
     * IDENTIFIERS
     */
    static final int maxAliasLength = 63;

    /**
     * Return an unused alias in this select.
     * 
     * @param seed the wanted name, eg "tableAlias".
     * @return a unique alias with the maximum possible of <code>seed</code>, eg "tableAl_1234".
     */
    public final String getUniqueAlias(String seed) {
        if (seed.length() > maxAliasLength)
            seed = seed.substring(0, maxAliasLength);

        if (!this.contains(seed)) {
            return seed;
        } else {
            long time = 1;
            for (int i = 0; i < 50; i++) {
                final String res;
                final String cat = seed + "_" + time;
                if (cat.length() > maxAliasLength)
                    res = seed.substring(0, seed.length() - (cat.length() - maxAliasLength)) + "_" + time;
                else
                    res = cat;
                if (!this.contains(res))
                    return res;
                else
                    time += 1;
            }
            // quit
            return null;
        }
    }

    private final FieldRef createRef(String alias, SQLField f) {
        return createRef(alias, f, true);
    }

    /**
     * Creates a FieldRef from the passed alias and field.
     * 
     * @param alias the table alias, eg "obs".
     * @param f the field, eg |OBSERVATION.ID_TENSION|.
     * @param mustExist if the table name/alias must already exist in this select.
     * @return the corresponding FieldRef.
     * @throws IllegalArgumentException if <code>mustExist</code> is <code>true</code> and this does
     *         not contain alias.
     */
    private final FieldRef createRef(String alias, SQLField f, boolean mustExist) {
        if (mustExist && !this.contains(alias))
            throw new IllegalArgumentException("unknown alias " + alias);
        return new AliasedField(f, alias);
    }

    /**
     * Return all fields known to this instance. NOTE the fields used in ORDER BY are not returned.
     * 
     * @return all fields known to this instance.
     */
    public final Set<SQLField> getFields() {
        final Set<SQLField> res = new HashSet<SQLField>(this.getSelectFields());
        for (final SQLSelectJoin j : getJoins())
            res.addAll(getFields(j.getWhere()));
        res.addAll(getFields(this.getWhere()));
        for (final FieldRef gb : this.groupBy)
            res.add(gb.getField());
        res.addAll(getFields(this.having));
        // MAYBE add order

        return res;
    }

    private static final Set<SQLField> getFields(Where w) {
        if (w != null) {
            final Set<SQLField> res = new HashSet<SQLField>();
            for (final FieldRef v : w.getFields())
                res.add(v.getField());
            return res;
        } else
            return Collections.emptySet();
    }

}
