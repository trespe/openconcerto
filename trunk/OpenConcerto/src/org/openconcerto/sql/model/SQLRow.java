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
 
 /*
 * SQLRow created on 20 mai 2004
 */
package org.openconcerto.sql.model;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLSelect.ArchiveMode;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.DecimalUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.dbutils.ResultSetHandler;

/**
 * Une ligne d'une table. Cette classe décrit une ligne et ne représente pas exactement une ligne
 * réelle, il n'y a pas unicité (cela reviendrait à recréer la base en Java !). Pour charger les
 * valeurs depuis la base manuellement à tout moment utiliser fetchValues(), cette méthode est
 * appelée automatiquement si nécessaire. Les valeurs des champs sont stockées, ainsi toutes les
 * méthodes renvoient l'état de la ligne réelle au moment du dernier fetchValues().
 * <p>
 * Une ligne peut ne pas exister ou être archivée, de plus elle peut ne pas contenir tous les champs
 * de la table. Pour accéder à la valeur des champs il existe getString() et getInt(), pour des
 * demandes plus complexes passer par getObject(). Si un champ qui n'est pas dans la ligne est
 * demandé, un fetchValues() est automatiquement fait.
 * </p>
 * <p>
 * On peut obtenir un ligne en la demandant à sa table, mais si l'on souhaite une SQLRow décrivant
 * une ligne n'existant pas dans la base il faut passer par le constructeur.
 * </p>
 * 
 * @author ILM Informatique 20 mai 2004
 * @see #isValid()
 * @see #getObject(String)
 * @see org.openconcerto.sql.model.SQLTable#getRow(int)
 */
public class SQLRow extends SQLRowAccessor {

    /**
     * Each table must have a row with this ID, that others refer to to indicate the absence of a
     * link.
     * 
     * @deprecated use either {@link SQLRowAccessor#isForeignEmpty(String)} /
     *             {@link SQLRowValues#putEmptyLink(String)} or if you must
     *             {@link SQLTable#getUndefinedID()}
     */
    public static final int UNDEFINED_ID = 1;
    /**
     * No valid database rows should have an ID thats less than MIN_VALID_ID. But remember, you CAN
     * have a SQLRow with any ID.
     */
    public static final int MIN_VALID_ID = 0;
    /** Value representing no ID, no table can have a row with this ID. */
    public static final int NONEXISTANT_ID = MIN_VALID_ID - 1;
    /** <code>true</code> to print a stack trace when fetching missing values */
    public static final boolean printSTForMissingField = false;

    /**
     * Crée une ligne avec les valeurs du ResultSet.
     * 
     * @param table la table de la ligne.
     * @param rs les valeurs.
     * @param onlyTable pass <code>true</code> if <code>rs</code> only contains columns from
     *        <code>table</code>, if unsure pass <code>false</code>. This allows to avoid calling
     *        {@link ResultSetMetaData#getTableName(int)} which is expensive on some systems.
     * @return la ligne correspondante.
     * @throws SQLException si problème lors de l'accès au ResultSet.
     * @see SQLRow#SQLRow(SQLTable, Map)
     * @deprecated use {@link SQLRowListRSH} or {@link SQLRowValuesListFetcher} instead or if you
     *             must use a {@link ResultSet} call
     *             {@link #createFromRS(SQLTable, ResultSet, ResultSetMetaData, boolean)} thus
     *             avoiding the potentially costly {@link ResultSet#getMetaData()}
     */
    public static final SQLRow createFromRS(SQLTable table, ResultSet rs, final boolean onlyTable) throws SQLException {
        return createFromRS(table, rs, rs.getMetaData(), onlyTable);
    }

    public static final SQLRow createFromRS(SQLTable table, ResultSet rs, final ResultSetMetaData rsmd, final boolean onlyTable) throws SQLException {
        return createFromRS(table, rs, getFieldNames(table, rsmd, onlyTable));
    }

    private static final List<String> getFieldNames(SQLTable table, final ResultSetMetaData rsmd, final boolean tableOnly) throws SQLException {
        final int colCount = rsmd.getColumnCount();
        final List<String> names = new ArrayList<String>(colCount);
        for (int i = 1; i <= colCount; i++) {
            // n'inclure que les colonnes de la table demandée
            // use a boolean since some systems (eg pg) require a request to the db to return the
            // table name
            if (tableOnly || rsmd.getTableName(i).equals(table.getName())) {
                names.add(rsmd.getColumnName(i));
            } else {
                names.add(null);
            }
        }

        return names;
    }

    // MAYBE create an opaque class holding names so that we can make this method, getFieldNames()
    // and createListFromRS() public
    static final SQLRow createFromRS(SQLTable table, ResultSet rs, final List<String> names) throws SQLException {
        final int indexCount = names.size();

        final Map<String, Object> m = new HashMap<String, Object>(indexCount);
        for (int i = 0; i < indexCount; i++) {
            final String colName = names.get(i);
            if (colName != null)
                m.put(colName, rs.getObject(i + 1));
        }

        return new SQLRow(table, m);
    }

    /**
     * Create a list of rows using the metadata to find the columns' names.
     * 
     * @param table the table of the rows.
     * @param rs the result set.
     * @param tableOnly <code>true</code> if <code>rs</code> only contains columns from
     *        <code>table</code>.
     * @return the data of the result set as SQLRows.
     * @throws SQLException if an error occurs while reading <code>rs</code>.
     */
    public static final List<SQLRow> createListFromRS(SQLTable table, ResultSet rs, final boolean tableOnly) throws SQLException {
        return createListFromRS(table, rs, getFieldNames(table, rs.getMetaData(), tableOnly));
    }

    /**
     * Create a list of rows without using the metadata.
     * 
     * @param table the table of the rows.
     * @param rs the result set.
     * @param names the name of the field for each column, nulls are ignored, e.g. ["DESIGNATION",
     *        null, "ID"].
     * @return the data of the result set as SQLRows.
     * @throws SQLException if an error occurs while reading <code>rs</code>.
     */
    static final List<SQLRow> createListFromRS(SQLTable table, ResultSet rs, final List<String> names) throws SQLException {
        final List<SQLRow> res = new ArrayList<SQLRow>();
        while (rs.next()) {
            res.add(createFromRS(table, rs, names));
        }
        return res;
    }

    private final int ID;
    private final Number idNumber;
    private Map<String, Object> values;
    private boolean fetched;

    private SQLRow(SQLTable table, Number id) {
        super(table);
        this.fetched = false;
        this.ID = id.intValue();
        this.idNumber = id;
        this.checkTable();
    }

    // public pour pouvoir créer une ligne n'exisant pas
    public SQLRow(SQLTable table, int ID) {
        // have to cast to Number, if you use Integer.valueOf() (or cast to Integer) the resulting
        // Integer is converted to Long
        this(table, table.getKey().getType().getJavaType() == Integer.class ? (Number) ID : Long.valueOf(ID));
    }

    private void checkTable() {
        if (!this.getTable().isRowable())
            throw new IllegalArgumentException(this.getTable() + " is not rowable");
    }

    /**
     * Crée une ligne avec les valeurs fournies. Evite une requête à la base.
     * 
     * @param table la table.
     * @param values les valeurs de la lignes.
     * @throws IllegalArgumentException si values ne contient pas la clef de la table.
     */
    public SQLRow(SQLTable table, Map<String, Object> values) {
        this(table, getID(values, table));
        // faire une copie, sinon backdoor pour changer les valeurs sans qu'on s'en aperçoive
        this.setValues(new HashMap<String, Object>(values));
    }

    private static Number getID(Map<String, Object> values, final SQLTable table) {
        final String keyName = table.getKey().getName();
        if (!values.keySet().contains(keyName))
            throw new IllegalArgumentException(values + " does not contain the key of " + table);
        return (Number) values.get(keyName);
    }

    private Map<String, Object> getValues() {
        if (!this.fetched)
            this.fetchValues();
        return this.values;
    }

    /**
     * Recharge les valeurs des champs depuis la base.
     */
    public void fetchValues() {
        this.fetchValues(true);
    }

    SQLRow fetchValues(final boolean useCache) {
        return this.fetchValues(useCache, useCache);
    }

    @SuppressWarnings("unchecked")
    SQLRow fetchValues(final boolean readCache, final boolean writeCache) {
        final IResultSetHandler handler = new IResultSetHandler(SQLDataSource.MAP_HANDLER) {
            @Override
            public boolean readCache() {
                return readCache;
            }

            @Override
            public boolean writeCache() {
                return writeCache;
            }

            public Set<SQLRow> getCacheModifiers() {
                return Collections.singleton(SQLRow.this);
            }
        };
        this.setValues((Map<String, Object>) this.getTable().getBase().getDataSource().execute(this.getQuery(), handler, false));
        return this;
    }

    // attention ne vérifie pas que tous les champs soient présents
    private final void setValues(Map<String, Object> values) {
        this.values = values;
        if (!this.fetched)
            this.fetched = true;
    }

    /**
     * Retourne les noms des champs qui ont été chargé depuis la base.
     * 
     * @return les noms des champs qui ont été chargé depuis la base.
     */
    public Set<String> getFields() {
        return Collections.unmodifiableSet(this.getValues().keySet());
    }

    private String getQuery() {
        return "SELECT * FROM " + this.getTable().getSQLName().quote() + " WHERE " + this.getWhere().getClause();
    }

    public Where getWhere() {
        return new Where(this.getTable().getKey(), "=", this.getID());
    }

    /**
     * Est ce que cette ligne existe dans la base de donnée.
     * 
     * @return <code>true</code> si la ligne existait lors de son instanciation.
     */
    public boolean exists() {
        return this.getValues() != null;
    }

    /**
     * Est ce que cette ligne est archivée.
     * 
     * @return <code>true</code> si la ligne était archivée lors de son instanciation.
     */
    public boolean isArchived() {
        // si il n'y a pas de champs archive, elle n'est pas archivée
        if (!this.getTable().isArchivable())
            return false;
        // TODO sortir archive == 1
        if (this.getTable().getArchiveField().getType().getJavaType().equals(Boolean.class))
            return this.getBoolean(this.getTable().getArchiveField().getName()) == Boolean.TRUE;
        else
            return this.getInt(this.getTable().getArchiveField().getName()) == 1;
    }

    /**
     * Est ce que cette ligne existe et n'est pas archivée.
     * 
     * @return <code>true</code> si cette ligne est valide.
     */
    public boolean isValid() {
        return this.exists() && this.getID() >= MIN_VALID_ID && !this.isArchived();
    }

    public boolean isData() {
        return this.isValid() && !this.isUndefined();
    }

    /**
     * Retourne le champ nommé <code>field</code> de cette ligne.
     * 
     * @param field le nom du champ que l'on veut.
     * @return la valeur du champ sous forme d'objet Java, ou <code>null</code> si la valeur est
     *         NULL.
     * @throws IllegalStateException si cette ligne n'existe pas.
     * @throws IllegalArgumentException si cette ligne ne contient pas le champ demandé.
     */
    public final Object getObject(String field) {
        if (!this.exists())
            throw new IllegalStateException("The row " + this + "does not exist.");
        if (!this.getTable().contains(field))
            throw new IllegalArgumentException("The table of the row " + this + " doesn't contain the field '" + field + "'.");
        // pour différencier entre la valeur est NULL (SQL) et la ligne ne contient pas ce champ
        if (!this.getValues().containsKey(field)) {
            // on ne l'a pas fetché
            this.fetchValues();
            // MAYBE mettre un boolean pour choisir si on accède à la base ou pas
            // since we just made a trip to the db we can afford to print at least a message
            final String msg = "The row " + this.simpleToString() + " doesn't contain the field '" + field + "' ; refetching.";
            Log.get().warning(msg);
            if (printSTForMissingField)
                new IllegalArgumentException(msg).printStackTrace();
        }
        return this.getValues().get(field);
    }

    public BigDecimal getOrder() {
        return (BigDecimal) this.getObject(this.getTable().getOrderField().getName());
    }

    /**
     * The free order just after or before this row.
     * 
     * @param after whether to look before or after this row.
     * @return a free order, or <code>null</code> if there's no room left.
     */
    @SuppressWarnings("unchecked")
    public final BigDecimal getOrder(boolean after) {
        final SQLTable t = this.getTable();
        final BigDecimal destOrder = this.getOrder();
        final int diff = (!after) ? -1 : 1;

        final SQLSelect sel = new SQLSelect(t.getBase());
        // pouvoir passer le deuxième en premier: le mettre entre indéfini (0) et le premier
        sel.setExcludeUndefined(false);
        // unique index prend aussi en compte les archivés
        sel.setArchivedPolicy(SQLSelect.BOTH);
        sel.addSelect(t.getKey());
        sel.addSelect(t.getOrderField());
        sel.setWhere(new Where(t.getOrderField(), diff < 0 ? "<" : ">", destOrder));
        sel.addRawOrder(t.getOrderField().getFieldRef() + (diff < 0 ? "DESC" : "ASC"));

        final BigDecimal otherOrder;
        final SQLDataSource ds = t.getBase().getDataSource();
        final Map<String, Object> otherMap = ds.execute1(sel.asString() + " LIMIT 1");
        if (otherMap == null) {
            // dernière ligne de la table
            otherOrder = destOrder.add(BigDecimal.ONE);
        } else {
            final SQLRow otherRow = new SQLRow(t, otherMap);
            otherOrder = otherRow.getOrder();
        }

        final int decDigits = this.getTable().getOrderField().getType().getDecimalDigits().intValue();
        final BigDecimal least = BigDecimal.ONE.scaleByPowerOfTen(-decDigits);
        final BigDecimal distance = destOrder.subtract(otherOrder).abs();
        if (distance.compareTo(least) <= 0)
            return null;
        else {
            final BigDecimal mean = destOrder.add(otherOrder).divide(new BigDecimal(2));
            return DecimalUtils.round(mean, decDigits);
        }
    }

    public SQLRow getForeign(String fieldName) {
        return this.getForeignRow(fieldName);
    }

    public boolean isForeignEmpty(String fieldName) {
        final SQLRow foreignRow = this.getForeignRow(fieldName, SQLRowMode.NO_CHECK);
        return foreignRow == null || foreignRow.isUndefined();
    }

    /**
     * Retourne la ligne sur laquelle pointe le champ passé. Elle peut être archivé ou indéfinie.
     * 
     * @param field le nom de la clef externe.
     * @return la ligne sur laquelle pointe le champ passé, jamais <code>null</code>.
     * @throws IllegalArgumentException si <code>field</code> n'est pas une clef étrangère de la
     *         table de cette ligne.
     * @throws IllegalStateException si <code>field</code> contient l'ID d'une ligne inexistante.
     */
    public SQLRow getForeignRow(String field) {
        return this.getForeignRow(field, SQLRowMode.EXIST);
    }

    /**
     * Retourne la ligne sur laquelle pointe le champ passé.
     * 
     * @param field le nom de la clef externe.
     * @param mode quel type de ligne retourner.
     * @return la ligne sur laquelle pointe le champ passé, ou <code>null</code> si elle ne
     *         correspond pas au mode.
     * @throws IllegalArgumentException si <code>field</code> n'est pas une clef étrangère de la
     *         table de cette ligne.
     * @throws IllegalStateException si <code>field</code> contient l'ID d'une ligne inexistante et
     *         que l'on n'en veut pas (mode.wantExisting() == <code>true</code>).
     */
    public SQLRow getForeignRow(String field, SQLRowMode mode) {
        if (!this.getTable().contains(field)) {
            throw new IllegalArgumentException(field + " is not a field of " + this.getTable());
        }
        final SQLField f = this.getTable().getField(field);
        if (!this.getTable().getForeignKeys().contains(f)) {
            throw new IllegalArgumentException(field + " is not a foreign key of " + this.getTable());
        }
        return this.getUncheckedForeignRow(this.getTable().getBase().getGraph().getForeignLink(f), mode);
    }

    private SQLRow getUncheckedForeignRow(Link foreignLink, SQLRowMode mode) {
        final SQLField field = foreignLink.getLabel();
        final SQLTable foreignTable = foreignLink.getTarget();
        if (this.getObject(field.getName()) == null) {
            return null;
        } else {
            final int foreignID = this.getInt(field.getName());
            final SQLRow foreignRow = new SQLRow(foreignTable, foreignID);
            // we used to check coherence here before all our dbs had real foreign keys
            return mode.filter(foreignRow);
        }
    }

    /**
     * Retourne l'ensemble des lignes de destTable liées à cette ligne.
     * 
     * @param destTable la table dont on veut les lignes, eg "CPI_BT".
     * @return l'ensemble des lignes liées à cette ligne, eg les cpis de LOCAL[5822].
     * @see #getLinkedRows(String)
     */
    public Set<SQLRow> getLinkedRows(String destTable) {
        return this.getDistantRows(Collections.singletonList(destTable));
    }

    /**
     * Retourne l'ensemble des lignes de destTable qui sont pointées par celle-ci.
     * 
     * @param destTable la table dont on veut les lignes, eg "OBSERVATION".
     * @return l'ensemble des lignes liées à cette ligne, eg les lignes pointées par
     *         "ID_OBSERVATION", "ID_OBSERVATION_2", etc.
     * @see #getLinkedRows(String)
     */
    public Set<SQLRow> getForeignRows(String destTable) {
        return this.getForeignRows(destTable, SQLRowMode.DATA);
    }

    public Set<SQLRow> getForeignRows(String destTable, SQLRowMode mode) {
        return new HashSet<SQLRow>(this.getForeignRowsMap(destTable, mode).values());
    }

    public Set<SQLRow> getForeignRows() {
        return this.getForeignRows(SQLRowMode.DATA);
    }

    public Set<SQLRow> getForeignRows(SQLRowMode mode) {
        return new HashSet<SQLRow>(this.getForeignRowsMap(mode).values());
    }

    /**
     * Retourne les lignes de destTable liées à cette ligne, indexées par les clefs externes.
     * 
     * @param destTable la table dont on veut les lignes.
     * @return les lignes de destTable liées à cette ligne.
     */
    public Map<SQLField, SQLRow> getForeignRowsMap(String destTable) {
        return this.getForeignRowsMap(destTable, SQLRowMode.DATA);
    }

    public Map<SQLField, SQLRow> getForeignRowsMap(String destTable, SQLRowMode mode) {
        final Set<Link> links = this.getTable().getDBSystemRoot().getGraph().getForeignLinks(this.getTable(), this.getTable().getTable(destTable));
        return this.foreignLinksToMap(links, mode);
    }

    public Map<SQLField, SQLRow> getForeignRowsMap() {
        return this.getForeignRowsMap(SQLRowMode.DATA);
    }

    public Map<SQLField, SQLRow> getForeignRowsMap(SQLRowMode mode) {
        final Set<Link> links = this.getTable().getBase().getGraph().getForeignLinks(this.getTable());
        return this.foreignLinksToMap(links, mode);
    }

    private Map<SQLField, SQLRow> foreignLinksToMap(Collection<Link> links, SQLRowMode mode) {
        final Map<SQLField, SQLRow> res = new HashMap<SQLField, SQLRow>();
        for (final Link l : links) {
            final SQLRow fr = this.getUncheckedForeignRow(l, mode);
            if (fr != null)
                res.put(l.getLabel(), fr);
        }
        return res;
    }

    /**
     * Fait la jointure entre cette ligne et les tables passées.
     * 
     * @param path le chemin de la jointure.
     * @return la ligne correspondante.
     * @throws IllegalArgumentException si le path est mauvais.
     * @throws IllegalStateException si le path ne méne pas à une ligne unique.
     * @see #getDistantRows(List)
     */
    public SQLRow getDistantRow(List<String> path) {
        Set rows = this.getDistantRows(path);
        if (rows.size() != 1)
            throw new IllegalStateException("the path " + path + " does not lead to a unique row (" + rows.size() + ")");
        return (SQLRow) rows.iterator().next();
    }

    /**
     * Fait la jointure entre cette ligne et les tables passées.
     * 
     * @param path le chemin de la jointure.
     * @return un ensemble de lignes de la dernière table du chemin, dans l'ordre.
     * @throws IllegalArgumentException si le path est mauvais.
     */
    public Set<SQLRow> getDistantRows(List<String> path) {
        // on veut tous les champs de la derniere table et rien d'autre
        final List<List> fields = new ArrayList<List>(Collections.nCopies(path.size() - 1, Collections.EMPTY_LIST));
        fields.add(null);
        final Set<List<SQLRow>> s = this.getRowsOnPath(path, fields);
        final Set<SQLRow> res = new LinkedHashSet<SQLRow>(s.size());
        for (final List<SQLRow> l : s) {
            res.add(l.get(0));
        }
        return res;
    }

    /**
     * Retourne les lignes distantes, plus les lignes intermédiaire du chemin. Par exemple
     * SITE[128].getRowsOnPath("BATIMENT,LOCAL", [null, "DESIGNATION"]) retourne tous les locaux du
     * site (seul DESIGNATION est chargé) avec tous les champs de leurs bâtiments.
     * 
     * @param path le chemin dans le graphe de la base.
     * @param fields un liste de des champs, chaque élément est :
     *        <ul>
     *        <li><code>null</code> pour tous les champs</li>
     *        <li>une String eg "DESIGNATION,NUMERO"</li>
     *        <li>une Collection de SQLField ou de nom complet de champs (prefixés)</li>
     *        </ul>
     * @return un ensemble de List de SQLRow.
     */
    public Set<List<SQLRow>> getRowsOnPath(final List<String> path, final List<?> fields) {
        final int pathSize = path.size();
        if (pathSize == 0)
            throw new IllegalArgumentException("path is empty");
        if (pathSize != fields.size())
            throw new IllegalArgumentException("path and fields size mismatch : " + pathSize + " != " + fields.size());
        final Set<List<SQLRow>> res = new LinkedHashSet<List<SQLRow>>();

        List<String> pathWMe = new ArrayList<String>(pathSize + 1);
        pathWMe.add(this.getTable().getName());
        pathWMe.addAll(path);
        final Path p = Path.create(this.getTable().getDBRoot(), pathWMe);

        Where where = this.getTable().getBase().getGraph().getJointure(p);
        // ne pas oublier de sélectionner notre ligne
        where = where.and(this.getWhere());

        final SQLSelect select = new SQLSelect(this.getTable().getBase());

        final List<Collection> fieldsCols = new ArrayList<Collection>(pathSize);
        for (int i = 0; i < pathSize; i++) {
            final Object fieldsName = fields.get(i);
            // +1 car p contient cette ligne
            final SQLTable t = p.getTable(i + 1);
            final Collection fieldsCol;
            if (fieldsName == null) {
                fieldsCol = t.getFields();
            } else if (fieldsName instanceof String) {
                fieldsCol = SQLRow.toList((String) fieldsName);
            } else {
                fieldsCol = (Collection) fieldsName;
            }
            fieldsCols.add(fieldsCol);

            // les tables qui ne nous interessent pas
            if (fieldsCol.size() > 0) {
                // toujours mettre l'ID
                select.addSelect(t.getKey());
                // plus les champs demandés
                if (fieldsName instanceof String)
                    select.addAllSelect(t, fieldsCol);
                else
                    select.addAllSelect(fieldsCol);
            }
        }
        // dans tous les cas mettre l'ID de la dernière table
        final SQLTable lastTable = p.getLast();
        select.addSelect(lastTable.getKey());

        // on ajoute une SQLRow pour chaque ID trouvé
        select.setWhere(where).addOrderSilent(lastTable.getName());
        this.getTable().getBase().getDataSource().execute(select.asString(), new ResultSetHandler() {

            public Object handle(ResultSet rs) throws SQLException {
                final ResultSetMetaData rsmd = rs.getMetaData();
                while (rs.next()) {
                    final List<SQLRow> rows = new ArrayList<SQLRow>(pathSize);
                    for (int i = 0; i < pathSize; i++) {
                        // les tables qui ne nous interessent pas
                        if (fieldsCols.get(i).size() > 0) {
                            // +1 car p contient cette ligne
                            final SQLTable t = p.getTable(i + 1);
                            rows.add(SQLRow.createFromRS(t, rs, rsmd, pathSize == 1));
                        }
                    }
                    res.add(rows);
                }
                return null;
            }
        });

        return res;
    }

    /**
     * Retourne les lignes pointant sur celle ci.
     * 
     * @return les lignes pointant sur celle ci.
     */
    public final List<SQLRow> getReferentRows() {
        return this.getReferentRows((Set<SQLTable>) null);
    }

    @Override
    public final List<SQLRow> getReferentRows(SQLTable refTable) {
        return this.getReferentRows(Collections.singleton(refTable));
    }

    /**
     * Retourne les lignes des tables spécifiées pointant sur celle ci.
     * 
     * @param tables les tables voulues, <code>null</code> pour toutes.
     * @return les SQLRow pointant sur celle ci.
     */
    public final List<SQLRow> getReferentRows(Set<SQLTable> tables) {
        return this.getReferentRows(tables, SQLSelect.UNARCHIVED);
    }

    /**
     * Returns the rows of tables that points to this row.
     * 
     * @param tables a Set of tables, or <code>null</code> for all of them.
     * @param archived <code>SQLSelect.UNARCHIVED</code>, <code>SQLSelect.ARCHIVED</code> or
     *        <code>SQLSelect.BOTH</code>.
     * @return a List of SQLRow that points to this.
     */
    public final List<SQLRow> getReferentRows(Set<SQLTable> tables, ArchiveMode archived) {
        return new ArrayList<SQLRow>(this.getReferentRowsByLink(tables, archived).values());
    }

    public final CollectionMap<Link, SQLRow> getReferentRowsByLink() {
        return this.getReferentRowsByLink(null);
    }

    public final CollectionMap<Link, SQLRow> getReferentRowsByLink(Set<SQLTable> tables) {
        return this.getReferentRowsByLink(tables, SQLSelect.UNARCHIVED);
    }

    public final CollectionMap<Link, SQLRow> getReferentRowsByLink(Set<SQLTable> tables, ArchiveMode archived) {
        // ArrayList since getReferentRows() is ordered
        final CollectionMap<Link, SQLRow> res = new CollectionMap<Link, SQLRow>(new ArrayList<SQLRow>());
        final Set<Link> links = this.getTable().getBase().getGraph().getReferentLinks(this.getTable());
        for (final Link l : links) {
            final SQLTable src = l.getSource();
            if (tables == null || tables != null && tables.contains(src)) {
                res.putAll(l, this.getReferentRows(l.getLabel(), archived));
            }
        }
        return res;
    }

    /**
     * Returns the rows that points to this row by the refField.
     * 
     * @param refField a SQLField that points to the table of this row, eg BATIMENT.ID_SITE.
     * @return a List of SQLRow that points to this, eg [BATIMENT[123], BATIMENT[124]].
     */
    public List<SQLRow> getReferentRows(final SQLField refField) {
        return this.getReferentRows(refField, SQLSelect.UNARCHIVED);
    }

    public List<SQLRow> getReferentRows(final SQLField refField, final ArchiveMode archived) {
        return this.getReferentRows(refField, archived, null);
    }

    /**
     * Returns the rows that points to this row by <code>refField</code>.
     * 
     * @param refField a SQLField that points to the table of this row, eg BATIMENT.ID_SITE.
     * @param archived specify which rows should be returned.
     * @param fields the list of fields the rows will have, <code>null</code> meaning all.
     * @return a List of SQLRow that points to this, eg [BATIMENT[123], BATIMENT[124]].
     */
    @SuppressWarnings("unchecked")
    public List<SQLRow> getReferentRows(final SQLField refField, final ArchiveMode archived, final Collection<String> fields) {
        final SQLTable foreignTable = refField.getTable().getBase().getGraph().getForeignTable(refField);
        if (!foreignTable.equals(this.getTable())) {
            throw new IllegalArgumentException(refField + " doesn't point to " + this.getTable());
        }

        final SQLTable src = refField.getTable();
        final SQLSelect sel = new SQLSelect(this.getTable().getBase());
        if (fields == null)
            sel.addSelectStar(src);
        else {
            sel.addSelect(src.getKey());
            for (final String f : fields)
                sel.addSelect(src.getField(f));
        }
        sel.setWhere(new Where(refField, "=", this.getID()));
        sel.setArchivedPolicy(archived);
        sel.addOrderSilent(src.getName());
        // - if some other criteria need to be applied, we could pass an SQLRowMode (instead of
        // just ArchiveMode) and modify the SQLSelect accordingly

        return (List<SQLRow>) this.getTable().getBase().getDataSource().execute(sel.asString(), new SQLRowListRSH(src, true));
    }

    /**
     * Toutes les lignes qui touchent cette lignes. C'est à dire les lignes pointées par les clefs
     * externes plus lignes qui pointent sur cette ligne.
     * 
     * @return les lignes qui touchent cette lignes.
     */
    private Set<SQLRow> getConnectedRows() {
        Set<SQLRow> res = new HashSet<SQLRow>();
        res.addAll(this.getReferentRows((Set<SQLTable>) null, SQLSelect.BOTH));
        res.addAll(this.getForeignRows(SQLRowMode.EXIST));
        return res;
    }

    /**
     * Trouve les lignes archivées reliées à celle ci par moins de maxLength liens.
     * 
     * @param maxLength la longeur maximale du chemin entre les lignes retournées et celle ci.
     * @return les lignes archivées reliées à celle ci.
     */
    public Set<SQLRow> findDistantArchived(int maxLength) {
        return this.findDistantArchived(maxLength, new HashSet<SQLRow>(), 0);
    }

    private Set<SQLRow> findDistantArchived(final int maxLength, final Set<SQLRow> been, int length) {
        final Set<SQLRow> res = new HashSet<SQLRow>();

        if (maxLength == length)
            return res;

        // on avance d'un cran
        been.add(this);
        length++;

        // on garde les lignes à appeler récursivement pour la fin
        // car on veut parcourir en largeur d'abord
        final Set<SQLRow> rec = new HashSet<SQLRow>();
        Iterator<SQLRow> iter = this.getConnectedRows().iterator();
        while (iter.hasNext()) {
            final SQLRow row = iter.next();
            if (!been.contains(row)) {
                if (row.isArchived()) {
                    res.add(row);
                } else {
                    rec.add(row);
                }
            }
        }
        iter = rec.iterator();
        while (iter.hasNext()) {
            final SQLRow row = iter.next();
            res.addAll(row.findDistantArchived(maxLength, been, length));
        }
        return res;
    }

    // ATTN peut faire une requête si archive n'est pas chargé
    public String toString() {
        String res = this.simpleToString();
        if (!this.exists()) {
            res = "?" + res + "?";
        } else if (this.isArchived()) {
            res = "(" + res + ")";
        }
        return res;
    }

    public String simpleToString() {
        return this.getTable().getName() + "[" + this.ID + "]";
    }

    /**
     * Renvoie tous les champs de cette ligne, clef comprises. En général on ne veut pas les valeurs
     * des clefs, voir getAllValues().
     * <p>
     * Les valeurs de cette map sont les valeurs retournées par getObject().
     * </p>
     * 
     * @return tous les champs de cette ligne.
     * @see #getAllValues()
     * @see #getObject(String)
     */
    @Override
    public Map<String, Object> getAbsolutelyAll() {
        return Collections.unmodifiableMap(this.getValues());
    }

    /**
     * Retourne toutes les valeurs de cette lignes, sans les clefs ni les champs d'ordre et
     * d'archive.
     * 
     * @return toutes les valeurs de cette lignes.
     * @see #getAbsolutelyAll()
     */
    public Map<String, Object> getAllValues() {
        // commence par tout copier
        final Map<String, Object> res = new HashMap<String, Object>(this.getValues());
        final Set keys = this.getTable().getKeys();
        // puis on enlève les clefs, l'ordre et l'archive
        CollectionUtils.filter(res.keySet(), new Predicate() {
            public boolean evaluate(Object object) {
                final SQLField field = getTable().getField((String) object);
                return !keys.contains(field) && field != getTable().getOrderField() && field != getTable().getArchiveField();
            }
        });
        return res;
    }

    /**
     * Creates a SQLRowValues with absolutely all the values of this row. ATTN the values are as
     * always the ones at the moment of the last fetching.
     * 
     * <pre>
     * SQLRow r = table.getRow(123); // [a=&gt;'26', b=&gt; '25']
     * r.createUpdateRow().put(&quot;a&quot;, 1).update();
     * r.createUpdateRow().put(&quot;b&quot;, 2).update();
     * </pre>
     * 
     * You could think that r now equals [a=>1, b=>2]. No, actually it's [a=>'26', b=>2], because
     * the second line overwrote the first one. The best solution is to use only one SQLRowValues
     * (hence only one access to the DB), otherwise use createEmptyUpdateRow().
     * 
     * @see #createEmptyUpdateRow()
     * @return a SQLRowValues on this SQLRow.
     */
    public SQLRowValues createUpdateRow() {
        final SQLRowValues res = new SQLRowValues(this.getTable());
        res.loadAbsolutelyAll(this);
        return res;
    }

    /**
     * Creates a SQLRowValues with just this ID, and no other values.
     * 
     * @return a SQLRowValues on this SQLRow.
     */
    @Override
    public SQLRowValues createEmptyUpdateRow() {
        final SQLRowValues res = new SQLRowValues(this.getTable());
        res.put(this.getTable().getKey().getName(), this.getIDNumber());
        return res;
    }

    /**
     * Gets the unique (among this table at least) identifier of this row.
     * 
     * @return an int greater than {@link #MIN_VALID_ID} if this is valid.
     */
    @Override
    public int getID() {
        return this.ID;
    }

    @Override
    public Number getIDNumber() {
        return this.idNumber;
    }

    @Override
    public SQLRow asRow() {
        return this;
    }

    @Override
    public final SQLRowValues asRowValues() {
        return this.createUpdateRow();
    }

    /**
     * Note : ne compare pas les valeurs des champs de cette ligne.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object other) {
        if (!(other instanceof SQLRow))
            return false;
        SQLRow o = (SQLRow) other;
        return this.equalsAsRow(o);
    }

    public int hashCode() {
        return this.hashCodeAsRow();
    }

    /**
     * Transforme un chemin en une liste de nom de table. Si path est "" alors retourne une liste
     * vide.
     * 
     * @param path le chemin, eg "BATIMENT,LOCAL".
     * @return une liste de String, eg ["BATIMENT","LOCAL"].
     */
    static public List<String> toList(String path) {
        return Arrays.asList(toArray(path));
    }

    static private String[] toArray(String path) {
        if (path.length() == 0)
            return new String[0];
        else
            // ATTN ',' : no spaces
            return path.split(",");
    }

    public SQLTableListener createTableListener(SQLDataListener l) {
        return new SQLTableListenerData<SQLRow>(this, l);
    }

}
