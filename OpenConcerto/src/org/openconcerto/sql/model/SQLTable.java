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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.SystemQueryExecutor.QueryExn;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.SQLCreateMoveableTable;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.Tuple3;
import org.openconcerto.utils.cc.CopyOnWriteMap;
import org.openconcerto.utils.cc.IPredicate;
import org.openconcerto.utils.change.CollectionChangeEventCreator;
import org.openconcerto.xml.JDOMUtils;

import java.math.BigDecimal;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.GuardedBy;

import org.apache.commons.dbutils.ResultSetHandler;
import org.jdom.Element;

/**
 * Une table SQL. Connait ses champs, notamment sa clef primaire et ses clefs externes. Une table
 * peut aussi faire des diagnostic sur son intégrité, ou sur la validité d'une valeur d'un de ses
 * champs. Enfin elle permet d'accéder aux lignes qui la composent.
 * 
 * @author ILM Informatique 4 mai 2004
 * @see #getField(String)
 * @see #getKey()
 * @see #getForeignKeys()
 * @see #checkIntegrity()
 * @see #checkValidity(String, int)
 * @see #getRow(int)
 */
public final class SQLTable extends SQLIdentifier implements SQLData, TableRef {

    /**
     * The {@link DBRoot#setMetadata(String, String) meta data} configuring the policy regarding
     * undefined IDs for a particular root. Can be either :
     * <dl>
     * <dt>min</dt>
     * <dd>for min("ID")</dd>
     * <dt>nonexistant</dt>
     * <dd>(the default) {@link SQLRow#NONEXISTANT_ID}</dd>
     * <dt><i>any other value</i></dt>
     * <dd>parsed as a number</dd>
     * </dl>
     */
    public static final String UNDEFINED_ID_POLICY = "undefined ID policy";
    public static final String undefTable = SQLSchema.FWK_TABLENAME_PREFIX + "UNDEFINED_IDS";
    // {SQLSchema=>{TableName=>UndefID}}
    private static final Map<SQLSchema, Map<String, Number>> UNDEFINED_IDs = new HashMap<SQLSchema, Map<String, Number>>();

    @SuppressWarnings("unchecked")
    private static final Map<String, Number> getUndefIDs(final SQLSchema schema) {
        if (!UNDEFINED_IDs.containsKey(schema)) {
            final Map<String, Number> r;
            if (schema.contains(undefTable)) {
                final SQLBase b = schema.getBase();
                final SQLTable undefT = schema.getTable(undefTable);
                final SQLSelect sel = new SQLSelect(b).addSelectStar(undefT);
                r = (Map<String, Number>) b.getDataSource().execute(sel.asString(), new ResultSetHandler() {
                    public Object handle(ResultSet rs) throws SQLException {
                        final Map<String, Number> res = new HashMap<String, Number>();
                        while (rs.next()) {
                            res.put(rs.getString("TABLENAME"), (Number) rs.getObject("UNDEFINED_ID"));
                        }
                        return res;
                    }
                });
                undefT.addTableModifiedListener(new SQLTableModifiedListener() {
                    @Override
                    public void tableModified(SQLTableEvent evt) {
                        synchronized (UNDEFINED_IDs) {
                            UNDEFINED_IDs.remove(schema);
                            undefT.removeTableModifiedListener(this);
                        }
                    }
                });
            } else {
                r = Collections.emptyMap();
            }
            UNDEFINED_IDs.put(schema, r);
        }
        return UNDEFINED_IDs.get(schema);
    }

    private static final Tuple2<Boolean, Number> getUndefID(SQLSchema b, String tableName) {
        synchronized (UNDEFINED_IDs) {
            final Map<String, Number> map = getUndefIDs(b);
            return Tuple2.create(map.containsKey(tableName), map.get(tableName));
        }
    }

    public static final void setUndefID(SQLSchema schema, String tableName, Integer value) throws SQLException {
        synchronized (UNDEFINED_IDs) {
            final SQLTable undefT = schema.getTable(undefTable);
            final String sql = undefT.getField("UNDEFINED_ID").getType().toString(value);
            final boolean modified;
            final Tuple2<Boolean, Number> currentValue = getUndefID(schema, tableName);
            if (!currentValue.get0()) {
                // INSERT
                SQLRowValues.insertCount(undefT, "(\"TABLENAME\", \"UNDEFINED_ID\") VALUES(" + schema.getBase().quoteString(tableName) + ", " + sql + ")");
                modified = true;
            } else if (!CompareUtils.equals(currentValue.get1(), value)) {
                // UPDATE
                final UpdateBuilder update = new UpdateBuilder(undefT).set("UNDEFINED_ID", sql);
                update.setWhere(new Where(undefT.getField("TABLENAME"), "=", tableName));
                schema.getDBSystemRoot().getDataSource().execute(update.asString());
                modified = true;
            } else {
                modified = false;
            }
            if (modified) {
                schema.updateVersion();
                undefT.fireTableModified(SQLRow.NONEXISTANT_ID);
            }
        }
    }

    private final CopyOnWriteMap<String, SQLField> fields;
    @GuardedBy("this")
    private final Set<SQLField> primaryKeys;
    // the vast majority of our code use getKey(), so cache it for performance
    @GuardedBy("this")
    private SQLField primaryKey;
    // true if there's at most 1 primary key
    @GuardedBy("this")
    private boolean primaryKeyOK;
    @GuardedBy("this")
    private Set<SQLField> keys;
    @GuardedBy("this")
    private final Map<String, Trigger> triggers;
    // null means it couldn't be retrieved
    @GuardedBy("this")
    private Set<Constraint> constraints;
    // always immutable so that fire can iterate safely ; to modify it, simply copy it before
    // (adding listeners is a lot less common than firing events)
    @GuardedBy("listenersMutex")
    private List<SQLTableModifiedListener> tableModifiedListeners;
    private final Object listenersMutex = new String("tableModifiedListeners mutex");
    // the id that foreign keys pointing to this, can use instead of NULL
    // a null value meaning not yet known
    @GuardedBy("this")
    private Integer undefinedID;

    @GuardedBy("this")
    private String comment;
    @GuardedBy("this")
    private String type;

    // empty table
    SQLTable(SQLSchema schema, String name) {
        super(schema, name);
        this.tableModifiedListeners = Collections.emptyList();
        // needed for getOrderedFields()
        this.fields = new CopyOnWriteMap<String, SQLField>() {
            @Override
            public Map<String, SQLField> copy(Map<? extends String, ? extends SQLField> src) {
                return new LinkedHashMap<String, SQLField>(src);
            }
        };
        assert isOrdered(this.fields);
        // order matters (eg for indexes)
        this.primaryKeys = new LinkedHashSet<SQLField>();
        this.primaryKey = null;
        this.primaryKeyOK = true;
        this.keys = null;
        this.triggers = new HashMap<String, Trigger>();
        // by default non-null, ie ok, only set to null on error
        this.constraints = new HashSet<Constraint>();
        // not known
        this.undefinedID = null;
    }

    // *** setter

    synchronized void clearNonPersistent() {
        this.triggers.clear();
        // non-null, see ctor
        this.constraints = new HashSet<Constraint>();
    }

    // * from XML

    @SuppressWarnings("unchecked")
    void loadFields(Element xml) {
        final LinkedHashMap<String, SQLField> newFields = new LinkedHashMap<String, SQLField>();
        for (final Element elementField : (List<Element>) xml.getChildren("field")) {
            final SQLField f = SQLField.create(this, elementField);
            newFields.put(f.getName(), f);
        }

        final Element primary = xml.getChild("primary");
        final List<String> newPrimaryKeys = new ArrayList<String>();
        for (final Element elementField : (List<Element>) primary.getChildren("field")) {
            final String fieldName = elementField.getAttributeValue("name");
            newPrimaryKeys.add(fieldName);
        }

        final String undefAttr = xml.getAttributeValue("undefID");
        synchronized (getTreeMutex()) {
            synchronized (this) {
                this.setState(newFields, newPrimaryKeys, undefAttr == null ? null : Integer.valueOf(undefAttr));

                final Element triggersElem = xml.getChild("triggers");
                if (triggersElem != null)
                    for (final Element triggerElem : (List<Element>) triggersElem.getChildren()) {
                        this.addTrigger(Trigger.fromXML(this, triggerElem));
                    }

                final Element constraintsElem = xml.getChild("constraints");
                if (constraintsElem == null)
                    this.addConstraint((Constraint) null);
                else
                    for (final Element elem : (List<Element>) constraintsElem.getChildren()) {
                        this.addConstraint(Constraint.fromXML(this, elem));
                    }

                final Element commentElem = xml.getChild("comment");
                if (commentElem != null)
                    this.setComment(commentElem.getText());
                this.setType(xml.getAttributeValue("type"));
            }
        }
    }

    synchronized private void addTrigger(final Trigger t) {
        this.triggers.put(t.getName(), t);
    }

    synchronized private void addConstraint(final Constraint c) {
        if (c == null) {
            this.constraints = null;
        } else {
            if (this.constraints == null)
                this.constraints = new HashSet<Constraint>();
            this.constraints.add(c);
        }
    }

    // * from JDBC

    public void fetchFields() throws SQLException {
        this.fetchFields(false);
    }

    void fetchFields(final boolean onlyUseSchema) throws SQLException {
        synchronized (getTreeMutex()) {
            synchronized (this) {
                final boolean removed = this.getBase().getDataSource().useConnection(new ConnectionHandlerNoSetup<Boolean, SQLException>() {
                    @Override
                    public Boolean handle(SQLDataSource ds) throws SQLException {
                        final DatabaseMetaData metaData = ds.getConnection().getMetaData();

                        final ResultSet tableRS = metaData.getTables(getBase().getMDName(), getSchema().getName(), getName(), new String[] { "TABLE", "SYSTEM TABLE", "VIEW" });
                        final boolean removed = !tableRS.next();
                        if (!removed) {
                            setType(tableRS.getString("TABLE_TYPE"));
                            setComment(tableRS.getString("REMARKS"));

                            final ResultSet rs = metaData.getColumns(getBase().getMDName(), getSchema().getName(), getName(), null);
                            // call next() to position the cursor
                            if (!rs.next()) {
                                // empty table
                                emptyFields();
                            } else {
                                fetchFields(metaData, rs);
                            }
                        }

                        return removed;
                    }
                });
                if (removed) {
                    SQLBase.mustContain(this, Collections.<SQLTable> emptySet(), Collections.singleton(this), "tables");
                    if (onlyUseSchema)
                        getSchema().rmTableWithoutSysRootLock(getName());
                    else
                        getSchema().rmTable(getName());
                } else {
                    this.clearNonPersistent();
                    new JDBCStructureSource.TriggerQueryExecutor(null).apply(this);
                    new JDBCStructureSource.ColumnsQueryExecutor(null).apply(this);
                    try {
                        new JDBCStructureSource.ConstraintsExecutor(null).apply(this);
                    } catch (QueryExn e) {
                        // constraints are not essential continue
                        e.printStackTrace();
                        this.addConstraint((Constraint) null);
                    }
                }

                if (!onlyUseSchema) {
                    // we might have added/dropped a foreign key even if the set of tables hasn't
                    // changed
                    this.getDBSystemRoot().descendantsChanged(this, null, false, removed);
                    this.save();
                }
            }
        }
    }

    /**
     * Fetch fields from the passed args.
     * 
     * @param metaData the metadata.
     * @param rs the resultSet of a getColumns(), the cursor must be on a row.
     * @return whether the <code>rs</code> has more row.
     * @throws SQLException if an error occurs.
     * @throws IllegalStateException if the current row of <code>rs</code> doesn't describe this.
     */
    boolean fetchFields(DatabaseMetaData metaData, ResultSet rs) throws SQLException {
        if (!this.isUs(rs))
            throw new IllegalStateException("rs current row does not describe " + this);

        synchronized (getTreeMutex()) {
            synchronized (this) {
                // we need to match the database ordering of fields
                final LinkedHashMap<String, SQLField> newFields = new LinkedHashMap<String, SQLField>();
                // fields
                boolean hasNext = true;
                while (hasNext && this.isUs(rs)) {
                    final SQLField f = SQLField.create(this, rs);
                    newFields.put(f.getName(), f);
                    hasNext = rs.next();
                }

                final List<String> newPrimaryKeys = new ArrayList<String>();
                final ResultSet pkRS = metaData.getPrimaryKeys(this.getBase().getMDName(), this.getSchema().getName(), this.getName());
                while (pkRS.next()) {
                    newPrimaryKeys.add(pkRS.getString("COLUMN_NAME"));
                }

                this.setState(newFields, newPrimaryKeys, null);

                return hasNext;
            }
        }
    }

    void emptyFields() {
        this.setState(new LinkedHashMap<String, SQLField>(), Collections.<String> emptyList(), null);
    }

    private boolean isUs(final ResultSet rs) throws SQLException {
        final String n = rs.getString("TABLE_NAME");
        final String s = rs.getString("TABLE_SCHEM");
        return n.equals(this.getName()) && CompareUtils.equals(s, this.getSchema().getName());
    }

    @SuppressWarnings("unchecked")
    void addTrigger(Map m) {
        this.addTrigger(new Trigger(this, m));
    }

    void addConstraint(Map<String, Object> m) {
        this.addConstraint(m == null ? null : new Constraint(this, m));
    }

    // must be called in setState() after fields have been set (for isRowable())
    private int fetchUndefID() {
        final int res;
        final SQLField pk;
        synchronized (this) {
            pk = isRowable() ? this.getKey() : null;
        }
        if (pk != null) {
            final Tuple2<Boolean, Number> currentValue = getUndefID(this.getSchema(), this.getName());
            if (!currentValue.get0()) {
                // no row
                res = this.findMinID(pk);
            } else {
                // a row
                final Number id = currentValue.get1();
                res = id == null ? SQLRow.NONEXISTANT_ID : id.intValue();
            }
        } else
            res = SQLRow.NONEXISTANT_ID;
        return res;
    }

    // no undef id found
    private int findMinID(SQLField pk) {
        final String debugUndef = "fwk_sql.debug.undefined_id";
        if (System.getProperty(debugUndef) != null)
            Log.get().warning("The system property '" + debugUndef + "' is deprecated, use the '" + UNDEFINED_ID_POLICY + "' metadata");

        final String policy = getSchema().getFwkMetadata(UNDEFINED_ID_POLICY);
        if (Boolean.getBoolean(debugUndef) || "min".equals(policy)) {
            final SQLSelect sel = new SQLSelect(this.getBase(), true).addSelect(pk, "min");
            final Number undef = (Number) this.getBase().getDataSource().executeScalar(sel.asString());
            if (undef == null) {
                // empty table
                throw new IllegalStateException(this + " is empty, can not infer UNDEFINED_ID");
            } else {
                final String update = "INSERT into " + new SQLName(this.getDBRoot().getName(), undefTable) + " VALUES('" + this.getName() + "', " + undef + ");";
                Log.get().config("the first row (which should be the undefined):\n" + update);
                return undef.intValue();
            }
        } else if (policy != null && !"nonexistant".equals(policy)) {
            final int res = Integer.parseInt(policy);
            if (res < SQLRow.MIN_VALID_ID)
                throw new IllegalStateException("ID is not valid : " + res);
            return res;
        } else {
            // by default assume NULL is used
            return SQLRow.NONEXISTANT_ID;
        }
    }

    // * from Java

    void mutateTo(SQLTable table) {
        synchronized (getTreeMutex()) {
            synchronized (this) {
                this.clearNonPersistent();
                this.setState(table.fields, table.getPKsNames(), table.undefinedID);
                this.triggers.putAll(table.triggers);
                if (table.constraints == null)
                    this.constraints = null;
                else {
                    this.constraints.addAll(table.constraints);
                }
                this.setType(table.getType());
                this.setComment(table.getComment());
            }
        }
    }

    // * update attributes

    static private <K, V> boolean isOrdered(Map<K, V> m) {
        if (m instanceof CopyOnWriteMap)
            return isOrdered(((CopyOnWriteMap<K, V>) m).copy(Collections.<K, V> emptyMap()));
        return (m instanceof LinkedHashMap);
    }

    private synchronized void setState(Map<String, SQLField> fields, final List<String> primaryKeys, final Integer undef) {
        assert isOrdered(fields);
        // checks new fields' table (don't use ==, see below)
        for (final SQLField newField : fields.values()) {
            if (!newField.getTable().getSQLName().equals(this.getSQLName()))
                throw new IllegalArgumentException(newField + " is in table " + newField.getTable().getSQLName() + " not us: " + this.getSQLName());
        }
        synchronized (getTreeMutex()) {
            synchronized (this) {
                final CollectionChangeEventCreator c = this.createChildrenCreator();

                if (!fields.keySet().containsAll(this.getFieldsName())) {
                    for (String removed : CollectionUtils.substract(this.getFieldsName(), fields.keySet())) {
                        this.fields.remove(removed).dropped();
                    }
                }

                for (final SQLField newField : fields.values()) {
                    if (getChildrenNames().contains(newField.getName())) {
                        // re-use old instances by refreshing existing ones
                        this.getField(newField.getName()).mutateTo(newField);
                    } else {
                        final SQLField fieldToAdd;
                        // happens when the new structure is loaded in-memory
                        // before the current one is mutated to it
                        // (we already checked the fullname of the table)
                        if (newField.getTable() != this)
                            fieldToAdd = new SQLField(this, newField);
                        else
                            fieldToAdd = newField;
                        this.fields.put(newField.getName(), fieldToAdd);
                    }
                }

                this.primaryKeys.clear();
                for (final String pk : primaryKeys)
                    this.primaryKeys.add(this.getField(pk));
                this.primaryKey = primaryKeys.size() == 1 ? this.getField(primaryKeys.get(0)) : null;
                this.primaryKeyOK = primaryKeys.size() <= 1;

                // don't fetch the ID now as it could be too early (e.g. we just created the table
                // but haven't inserted the undefined row)
                this.undefinedID = undef;
                this.fireChildrenChanged(c);
            }
        }
    }

    // *** getter

    synchronized void setType(String type) {
        this.type = type;
    }

    public synchronized final String getType() {
        return this.type;
    }

    synchronized void setComment(String comm) {
        this.comment = comm;
    }

    public synchronized final String getComment() {
        return this.comment;
    }

    public synchronized final Trigger getTrigger(String name) {
        return this.triggers.get(name);
    }

    public synchronized final Map<String, Trigger> getTriggers() {
        return Collections.unmodifiableMap(this.triggers);
    }

    /**
     * The constraints on this table.
     * 
     * @return the constraints or <code>null</code> if they couldn't be retrieved.
     */
    public synchronized final Set<Constraint> getAllConstraints() {
        return this.constraints == null ? null : Collections.unmodifiableSet(this.constraints);
    }

    /**
     * The CHECK and UNIQUE constraints on this table. This is useful since FOREIGN KEY and PRIMARY
     * KEY are already available through {@link #getForeignKeys()} and {@link #getPrimaryKeys()}.
     * 
     * @return the constraints or <code>null</code> if they couldn't be retrieved.
     */
    public synchronized final Set<Constraint> getConstraints() {
        if (this.constraints == null)
            return null;
        final Set<Constraint> res = new HashSet<Constraint>();
        for (final Constraint c : this.constraints) {
            if (c.getType() != ConstraintType.FOREIGN_KEY && c.getType() != ConstraintType.PRIMARY_KEY) {
                res.add(c);
            }
        }
        return res;
    }

    /**
     * Returns a specific constraint.
     * 
     * @param type type of constraint, e.g. {@link ConstraintType#UNIQUE}.
     * @param cols the fields names, e.g. ["NAME"].
     * @return the matching constraint, <code>null</code> if it cannot be found or if constraints
     *         couldn't be retrieved.
     */
    public synchronized final Constraint getConstraint(ConstraintType type, List<String> cols) {
        if (this.constraints == null)
            return null;
        for (final Constraint c : this.constraints) {
            if (c.getType() == type && c.getCols().equals(cols)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Whether rows of this table can be represented as SQLRow.
     * 
     * @return <code>true</code> if rows of this table can be represented as SQLRow.
     */
    public synchronized boolean isRowable() {
        return this.getPrimaryKeys().size() == 1 && Number.class.isAssignableFrom(this.getKey().getType().getJavaType());
    }

    public SQLSchema getSchema() {
        return (SQLSchema) this.getParent();
    }

    public SQLBase getBase() {
        return this.getSchema().getBase();
    }

    /**
     * Return the primary key of this table.
     * 
     * @return the field which is the key of this table, or <code>null</code> if it doesn't exist.
     * @throws IllegalStateException if there's more than one primary key.
     */
    @Override
    public synchronized SQLField getKey() {
        if (!this.primaryKeyOK)
            throw new IllegalStateException(this + " has more than 1 primary key: " + this.getPrimaryKeys());
        return this.primaryKey;
    }

    /**
     * Return the primary keys of this table.
     * 
     * @return the fields (SQLField) which are the keys of this table, can be empty.
     */
    public synchronized Set<SQLField> getPrimaryKeys() {
        return Collections.unmodifiableSet(this.primaryKeys);
    }

    /**
     * Return the foreign keys of this table.
     * 
     * @return a Set of SQLField which are foreign keys of this table.
     */
    public Set<SQLField> getForeignKeys() {
        return this.getDBSystemRoot().getGraph().getForeignKeys(this);
    }

    public Set<String> getForeignKeysNames() {
        return DatabaseGraph.getNames(this.getDBSystemRoot().getGraph().getForeignLinks(this));
    }

    public Set<List<SQLField>> getForeignKeysFields() {
        return this.getDBSystemRoot().getGraph().getForeignKeysFields(this);
    }

    public Set<SQLField> getForeignKeys(String foreignTable) {
        return this.getForeignKeys(this.getTable(foreignTable));
    }

    public Set<SQLField> getForeignKeys(SQLTable foreignTable) {
        return this.getDBSystemRoot().getGraph().getForeignFields(this, foreignTable);
    }

    public SQLTable getForeignTable(String foreignField) {
        return this.getField(foreignField).getForeignTable();
    }

    public SQLTable findReferentTable(String tableName) {
        return this.getDBSystemRoot().getGraph().findReferentTable(this, tableName);
    }

    /**
     * Renvoie toutes les clefs de cette table. C'est à dire les clefs primaires plus les clefs
     * externes.
     * 
     * @return toutes les clefs de cette table, can be empty.
     */
    public synchronized Set<SQLField> getKeys() {
        if (this.keys == null) {
            // getForeignKeys cree un nouveau set a chaque fois, pas besoin de dupliquer
            this.keys = this.getForeignKeys();
            this.keys.addAll(this.getPrimaryKeys());
        }
        return this.keys;
    }

    public String toString() {
        return "/" + this.getName() + "/";
    }

    /**
     * Return the field named <i>fieldName </i> in this table.
     * 
     * @param fieldName the name of the field.
     * @return the matching field, never <code>null</code>.
     * @throws IllegalArgumentException if the field is not in this table.
     * @see #getFieldRaw(String)
     */
    @Override
    public SQLField getField(String fieldName) {
        SQLField res = this.getFieldRaw(fieldName);
        if (res == null) {
            throw new IllegalArgumentException("unknown field " + fieldName + " in " + this.getName() + ". The table " + this.getName() + " contains the followins fields: " + this.getFieldsName());
        }
        return res;
    }

    /**
     * Return the field named <i>fieldName</i> in this table.
     * 
     * @param fieldName the name of the field.
     * @return the matching field or <code>null</code> if none exists.
     */
    public SQLField getFieldRaw(String fieldName) {
        return this.fields.get(fieldName);
    }

    /**
     * Return all the fields in this table.
     * 
     * @return a Set of the fields.
     */
    public Set<SQLField> getFields() {
        return new HashSet<SQLField>(this.fields.values());
    }

    /**
     * Retourne les champs du contenu de cette table. C'est à dire ni la clef primaire, ni les
     * champs d'archive et d'ordre.
     * 
     * @return les champs du contenu de cette table.
     */
    public Set<SQLField> getContentFields() {
        return this.getContentFields(false);
    }

    public synchronized Set<SQLField> getContentFields(final boolean includeMetadata) {
        final Set<SQLField> res = this.getFields();
        res.removeAll(this.getPrimaryKeys());
        res.remove(this.getArchiveField());
        res.remove(this.getOrderField());
        if (!includeMetadata) {
            res.remove(this.getCreationDateField());
            res.remove(this.getCreationUserField());
            res.remove(this.getModifDateField());
            res.remove(this.getModifUserField());
        }
        return res;
    }

    /**
     * Retourne les champs du contenu local de cette table. C'est à dire uniquement les champs du
     * contenu qui ne sont pas des clefs externes.
     * 
     * @return les champs du contenu local de cette table.
     * @see #getContentFields()
     */
    public synchronized Set<SQLField> getLocalContentFields() {
        Set<SQLField> res = this.getContentFields();
        res.removeAll(this.getForeignKeys());
        return res;
    }

    /**
     * Return the names of all the fields.
     * 
     * @return the names of all the fields.
     */
    public Set<String> getFieldsName() {
        return this.fields.keySet();
    }

    /**
     * Return all the fields in this table. The order is the same across reboot.
     * 
     * @return a List of the fields.
     */
    public List<SQLField> getOrderedFields() {
        return new ArrayList<SQLField>(this.fields.values());
    }

    @Override
    public Map<String, SQLField> getChildrenMap() {
        return this.fields.getImmutable();
    }

    public final SQLTable getTable(String name) {
        return this.getDesc(name, SQLTable.class);
    }

    /**
     * Retourne le nombre total de lignes contenues dans cette table.
     * 
     * @return le nombre de lignes de cette table.
     */
    public int getRowCount() {
        final SQLSelect sel = new SQLSelect(this.getBase(), true).addSelectFunctionStar("count").addFrom(this);
        final Number count = (Number) this.getBase().getDataSource().execute(sel.asString(), new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, false));
        return count.intValue();
    }

    /**
     * The maximum value of the order field.
     * 
     * @return the maximum value of the order field, or -1 if this table is empty.
     */
    public BigDecimal getMaxOrder() {
        return this.getMaxOrder(true);
    }

    synchronized BigDecimal getMaxOrder(Boolean useCache) {
        if (!this.isOrdered())
            throw new IllegalStateException(this + " is not ordered");

        final SQLSelect sel = new SQLSelect(this.getBase(), true).addSelect(this.getOrderField(), "max");
        try {
            final BigDecimal maxOrder = (BigDecimal) this.getBase().getDataSource().execute(sel.asString(), new IResultSetHandler(SQLDataSource.SCALAR_HANDLER, useCache));
            return maxOrder == null ? BigDecimal.ONE.negate() : maxOrder;
        } catch (ClassCastException e) {
            throw new IllegalStateException(this.getOrderField().getSQLName() + " must be " + SQLSyntax.get(this).getOrderDefinition(), e);
        }
    }

    /**
     * Retourne la ligne correspondant à l'ID passé.
     * 
     * @param ID l'identifiant de la ligne à retourner.
     * @return une ligne existant dans la base sinon <code>null</code>.
     * @see #getValidRow(int)
     */
    public SQLRow getRow(int ID) {
        SQLRow row = this.getUncheckedRow(ID);
        return row.exists() ? row : null;
    }

    /**
     * Retourne une la ligne demandée sans faire aucune vérification.
     * 
     * @param ID l'identifiant de la ligne à retourner.
     * @return la ligne demandée, jamais <code>null</code>.
     */
    private SQLRow getUncheckedRow(int ID) {
        return new SQLRow(this, ID);
    }

    /**
     * Retourne la ligne valide correspondant à l'ID passé.
     * 
     * @param ID l'identifiant de la ligne à retourner.
     * @return une ligne existante et non archivée dans la base sinon <code>null</code>.
     * @see SQLRow#isValid()
     */
    public SQLRow getValidRow(int ID) {
        SQLRow row = this.getRow(ID);
        return row.isValid() ? row : null;
    }

    /**
     * Vérifie la validité de cet ID. C'est à dire qu'il existe une ligne non archivée avec cet ID,
     * dans cette table.
     * 
     * @param ID l'identifiant.
     * @return <code>null</code> si l'ID est valide, sinon une SQLRow qui est soit inexistante, soit
     *         archivée.
     */
    public SQLRow checkValidity(int ID) {
        SQLRow row = this.getUncheckedRow(ID);
        // l'inverse de getValidRow()
        return row.isValid() ? null : row;
    }

    /**
     * Vérifie cette table est intègre. C'est à dire que toutes ses clefs externes pointent sur des
     * lignes existantes et non effacées. Cette méthode retourne une liste constituée de triplet :
     * SQLRow (la ligne incohérente), SQLField (le champ incohérent), SQLRow (la ligne invalide de
     * la table étrangère).
     * 
     * @return a list of inconsistencies or <code>null</code> if this table is not rowable.
     */
    public List<Tuple3<SQLRow, SQLField, SQLRow>> checkIntegrity() {
        final SQLField pk;
        final Set<SQLField> fks;
        synchronized (this) {
            if (!this.isRowable())
                return null;
            pk = this.getKey();
            fks = this.getForeignKeys();
        }

        final List<Tuple3<SQLRow, SQLField, SQLRow>> inconsistencies = new ArrayList<Tuple3<SQLRow, SQLField, SQLRow>>();
        // si on a pas de relation externe, c'est OK
        if (!fks.isEmpty()) {
            SQLSelect sel = new SQLSelect(this.getBase());
            // on ne vérifie pas les lignes archivées mais l'indéfinie oui.
            sel.setExcludeUndefined(false);
            sel.addSelect(pk);
            sel.addAllSelect(fks);
            this.getBase().getDataSource().execute(sel.asString(), new ResultSetHandler() {
                public Object handle(ResultSet rs) throws SQLException {
                    while (rs.next()) {
                        for (final SQLField fk : fks) {
                            final SQLRow pb = SQLTable.this.checkValidity(fk.getName(), rs.getInt(fk.getFullName()));
                            if (pb != null) {
                                final SQLRow row = SQLTable.this.getRow(rs.getInt(pk.getFullName()));
                                inconsistencies.add(Tuple3.create(row, fk, pb));
                            }
                        }
                    }
                    // on s'en sert pas
                    return null;
                }
            });
        }

        return inconsistencies;
    }

    /**
     * Vérifie que l'on peut affecter <code>foreignID</code> au champ <code>foreignKey</code> de
     * cette table. C'est à dire vérifie que la table sur laquelle pointe <code>foreignKey</code>
     * contient bien une ligne d'ID <code>foreignID</code> et de plus qu'elle n'a pas été archivée.
     * 
     * @param foreignKey le nom du champ.
     * @param foreignID l'ID que l'on souhaite tester.
     * @return une SQLRow décrivant l'incohérence ou <code>null</code> sinon.
     * @throws IllegalArgumentException si le champ passé n'est pas une clef étrangère.
     * @see #checkValidity(int)
     */
    public SQLRow checkValidity(String foreignKey, int foreignID) {
        final SQLField fk = this.getField(foreignKey);
        final SQLTable foreignTable = this.getDBSystemRoot().getGraph().getForeignTable(fk);
        if (foreignTable == null)
            throw new IllegalArgumentException("Impossible de tester '" + foreignKey + "' avec " + foreignID + " dans " + this + ". Ce n'est pas une clef étrangère.");
        return foreignTable.checkValidity(foreignID);
    }

    public SQLRow checkValidity(String foreignKey, Number foreignID) {
        // NULL is valid
        if (foreignID == null)
            return null;
        else
            return this.checkValidity(foreignKey, foreignID.intValue());
    }

    public boolean isOrdered() {
        return this.getOrderField() != null;
    }

    public SQLField getOrderField() {
        return this.getFieldRaw(orderField);
    }

    /**
     * The number of fractional digits of the order field.
     * 
     * @return the number of fractional digits of the order field.
     */
    public final int getOrderDecimalDigits() {
        return this.getOrderField().getType().getDecimalDigits().intValue();
    }

    public final BigDecimal getOrderULP() {
        return BigDecimal.ONE.scaleByPowerOfTen(-this.getOrderDecimalDigits());
    }

    public boolean isArchivable() {
        return this.getArchiveField() != null;
    }

    public SQLField getArchiveField() {
        return this.getFieldRaw(archiveField);
    }

    public SQLField getCreationDateField() {
        return this.getFieldRaw("CREATION_DATE");
    }

    public SQLField getCreationUserField() {
        return this.getFieldRaw("ID_USER_COMMON_CREATE");
    }

    public SQLField getModifDateField() {
        return this.getFieldRaw("MODIFICATION_DATE");
    }

    public SQLField getModifUserField() {
        return this.getFieldRaw("ID_USER_COMMON_MODIFY");
    }

    /**
     * The id of this table which means empty. Tables that aren't rowable or which use NULL to
     * signify empty have no UNDEFINED_ID.
     * 
     * @return the empty id or {@link SQLRow#NONEXISTANT_ID} if this table has no UNDEFINED_ID.
     */
    public final int getUndefinedID() {
        return this.getUndefinedID(false).intValue();
    }

    private final Integer getUndefinedID(final boolean internal) {
        Integer res = null;
        synchronized (this) {
            if (this.undefinedID != null)
                res = this.undefinedID;
        }
        if (res == null) {
            if (!internal && this.getSchema().isFetchAllUndefinedIDs()) {
                // init all undefined, MAYBE one request with UNION ALL
                for (final SQLTable sibling : this.getSchema().getTables()) {
                    Integer siblingRes = getUndefinedID(true);
                    assert siblingRes != null;
                    if (sibling == this)
                        res = siblingRes;
                }
                // save all tables
                this.getBase().save(this.getSchema().getName());
            } else {
                res = this.fetchUndefID();
                synchronized (this) {
                    this.undefinedID = res;
                }
                if (!internal)
                    this.save();
            }
        }
        return res;
    }

    public final Number getUndefinedIDNumber() {
        final int res = this.getUndefinedID();
        if (res == SQLRow.NONEXISTANT_ID)
            return null;
        else
            return res;
    }

    // save just this table
    final void save() {
        // (for now save all tables)
        this.getBase().save(this.getSchema().getName());
    }

    // static

    static private final String orderField = "ORDRE";
    static private final String archiveField = "ARCHIVE";

    // /////// ******** OLD CODE ********

    /*
     * Gestion des événements
     */

    public void addTableModifiedListener(SQLTableModifiedListener l) {
        this.addTableModifiedListener(l, false);
    }

    public void addPremierTableModifiedListener(SQLTableModifiedListener l) {
        this.addTableModifiedListener(l, true);
    }

    private void addTableModifiedListener(SQLTableModifiedListener l, final boolean before) {
        synchronized (this.listenersMutex) {
            final List<SQLTableModifiedListener> newListeners = new ArrayList<SQLTableModifiedListener>(this.tableModifiedListeners.size() + 1);
            if (before)
                newListeners.add(l);
            newListeners.addAll(this.tableModifiedListeners);
            if (!before)
                newListeners.add(l);
            this.tableModifiedListeners = Collections.unmodifiableList(newListeners);
        }
    }

    public void removeTableModifiedListener(SQLTableModifiedListener l) {
        synchronized (this.listenersMutex) {
            final List<SQLTableModifiedListener> newListeners = new ArrayList<SQLTableModifiedListener>(this.tableModifiedListeners);
            if (newListeners.remove(l))
                this.tableModifiedListeners = Collections.unmodifiableList(newListeners);
        }
    }

    private static final class BridgeListener implements SQLTableModifiedListener {

        private final SQLTableListener l;

        private BridgeListener(SQLTableListener l) {
            super();
            this.l = l;
        }

        @Override
        public void tableModified(SQLTableEvent evt) {
            final Mode mode = evt.getMode();
            if (mode == Mode.ROW_ADDED)
                this.l.rowAdded(evt.getTable(), evt.getId());
            else if (mode == Mode.ROW_UPDATED)
                this.l.rowModified(evt.getTable(), evt.getId());
            else if (mode == Mode.ROW_DELETED)
                this.l.rowDeleted(evt.getTable(), evt.getId());
        }

        @Override
        public int hashCode() {
            return this.l.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof BridgeListener && this.l.equals(((BridgeListener) obj).l);
        }
    }

    /**
     * Ajoute un listener sur cette table.
     * 
     * @param l the listener.
     * @deprecated use {@link #addTableModifiedListener(SQLTableModifiedListener)}
     */
    public void addTableListener(SQLTableListener l) {
        this.addTableModifiedListener(new BridgeListener(l));
    }

    public void removeTableListener(SQLTableListener l) {
        this.removeTableModifiedListener(new BridgeListener(l));
    }

    /**
     * Previent tous les listeners de la table qu'il y a eu une modification ou ajout si modif de
     * d'une ligne particuliere.
     * 
     * @param id -1 signifie tout est modifié.
     */
    public void fireTableModified(final int id) {
        this.fire(Mode.ROW_UPDATED, id);
    }

    public void fireRowAdded(final int id) {
        this.fire(Mode.ROW_ADDED, id);
    }

    public void fireRowDeleted(final int id) {
        this.fire(Mode.ROW_DELETED, id);
    }

    public void fireTableModified(final int id, Collection<String> fields) {
        this.fire(new SQLTableEvent(this, id, Mode.ROW_UPDATED, fields));
    }

    private void fire(final Mode mode, final int id) {
        this.fire(new SQLTableEvent(this, id, mode, null));
    }

    public final void fire(SQLTableEvent evt) {
        this.fireTableModified(evt);
    }

    static private final ThreadLocal<LinkedList<Tuple2<Iterator<SQLTableModifiedListener>, SQLTableEvent>>> events = new ThreadLocal<LinkedList<Tuple2<Iterator<SQLTableModifiedListener>, SQLTableEvent>>>() {
        @Override
        protected LinkedList<Tuple2<Iterator<SQLTableModifiedListener>, SQLTableEvent>> initialValue() {
            return new LinkedList<Tuple2<Iterator<SQLTableModifiedListener>, SQLTableEvent>>();
        }
    };

    // allow to maintain the dispatching of events in order when a listener itself fires an event
    static private void fireTableModified(Tuple2<Iterator<SQLTableModifiedListener>, SQLTableEvent> newTuple) {
        final LinkedList<Tuple2<Iterator<SQLTableModifiedListener>, SQLTableEvent>> linkedList = events.get();
        // add new event
        linkedList.addLast(newTuple);
        // process all pending events
        Tuple2<Iterator<SQLTableModifiedListener>, SQLTableEvent> currentTuple;
        while ((currentTuple = linkedList.peekFirst()) != null) {
            final Iterator<SQLTableModifiedListener> iter = currentTuple.get0();
            final SQLTableEvent currentEvt = currentTuple.get1();
            while (iter.hasNext()) {
                final SQLTableModifiedListener l = iter.next();
                l.tableModified(currentEvt);
            }
            // not removeFirst() since the item might have been already removed
            linkedList.pollFirst();
        }
    }

    private void fireTableModified(final SQLTableEvent evt) {
        // no need to copy since this.tableModifiedListeners is immutable
        final List<SQLTableModifiedListener> dispatchingListeners;
        synchronized (this.listenersMutex) {
            dispatchingListeners = this.tableModifiedListeners;
        }
        fireTableModified(Tuple2.create(dispatchingListeners.iterator(), evt));
    }

    public synchronized String toXML() {
        final StringBuilder sb = new StringBuilder(16000);
        sb.append("<table name=\"");
        sb.append(JDOMUtils.OUTPUTTER.escapeAttributeEntities(this.getName()));
        sb.append("\"");

        final String schemaName = this.getSchema().getName();
        if (schemaName != null) {
            sb.append(" schema=\"");
            sb.append(JDOMUtils.OUTPUTTER.escapeAttributeEntities(schemaName));
            sb.append('"');
        }

        if (this.undefinedID != null) {
            sb.append(" undefID=\"");
            sb.append(this.undefinedID);
            sb.append('"');
        }

        if (getType() != null) {
            sb.append(" type=\"");
            sb.append(JDOMUtils.OUTPUTTER.escapeAttributeEntities(getType()));
            sb.append('"');
        }

        sb.append(">\n");

        if (this.getComment() != null) {
            sb.append("<comment>");
            sb.append(JDOMUtils.OUTPUTTER.escapeElementEntities(this.getComment()));
            sb.append("</comment>\n");
        }
        for (SQLField field : this.fields.values()) {
            sb.append(field.toXML());
        }
        sb.append("<primary>\n");
        for (SQLField element : this.primaryKeys) {
            sb.append(element.toXML());
        }
        sb.append("</primary>\n");
        // avoid writing unneeded chars
        if (this.triggers.size() > 0) {
            sb.append("<triggers>\n");
            for (Trigger t : this.triggers.values()) {
                sb.append(t.toXML());
            }
            sb.append("</triggers>\n");
        }
        if (this.constraints != null) {
            sb.append("<constraints>\n");
            for (Constraint t : this.constraints) {
                sb.append(t.toXML());
            }
            sb.append("</constraints>\n");
        }
        sb.append("</table>");
        return sb.toString();
    }

    @Override
    public SQLTableModifiedListener createTableListener(final SQLDataListener l) {
        return new SQLTableModifiedListener() {
            @Override
            public void tableModified(SQLTableEvent evt) {
                l.dataChanged();
            }
        };
    }

    @Override
    public SQLTable getTable() {
        return this;
    }

    @Override
    public String getAlias() {
        return getName();
    }

    @Override
    public String getSQL() {
        return getSQLName().quote();
    }

    public boolean equalsDesc(SQLTable o) {
        return this.equalsDesc(o, true) == null;
    }

    /**
     * Compare this table and its descendants. This do not compare undefinedID as it isn't part of
     * the structure per se.
     * 
     * @param o the table to compare.
     * @param compareName whether to also compare the name, useful for comparing 2 tables in the
     *        same schema.
     * @return <code>null</code> if attributes and children of this and <code>o</code> are equals,
     *         otherwise a String explaining the differences.
     */
    public String equalsDesc(SQLTable o, boolean compareName) {
        return this.equalsDesc(o, null, compareName);
    }

    // ATTN otherSystem can be null, meaning compare exactly (even if the system of this table and
    // the system of the other table do not support the same features and thus tables cannot be
    // equal)
    // if otherSystem isn't null, then this method is more lenient and return true if the two tables
    // are the closest possible. NOTE that otherSystem is not required to be the system of the other
    // table, it might be something else if the other table was loaded into a system different than
    // the one which created the dump.
    public synchronized String equalsDesc(SQLTable o, SQLSystem otherSystem, boolean compareName) {
        if (o == null)
            return "other table is null";
        final boolean name = !compareName || this.getName().equals(o.getName());
        if (!name)
            return "name unequal : " + this.getName() + " " + o.getName();
        // TODO triggers, but wait for the dumping of functions
        // which mean wait for psql 8.4 pg_get_functiondef()
        // if (this.getServer().getSQLSystem() == o.getServer().getSQLSystem()) {
        // if (!this.getTriggers().equals(o.getTriggers()))
        // return "triggers unequal : " + this.getTriggers() + " " + o.getTriggers();
        // } else {
        // if (!this.getTriggers().keySet().equals(o.getTriggers().keySet()))
        // return "triggers names unequal : " + this.getTriggers() + " " + o.getTriggers();
        // }
        final boolean checkComment = otherSystem == null || this.getServer().getSQLSystem().isTablesCommentSupported() && otherSystem.isTablesCommentSupported();
        if (checkComment && !CompareUtils.equals(this.getComment(), o.getComment()))
            return "comment unequal : '" + this.getComment() + "' != '" + o.getComment() + "'";
        if (!CompareUtils.equals(this.getConstraints(), o.getConstraints()))
            return "constraints unequal : '" + this.getConstraints() + "' != '" + o.getConstraints() + "'";
        return this.equalsChildren(o, otherSystem);
    }

    private synchronized String equalsChildren(SQLTable o, SQLSystem otherSystem) {
        if (!this.getChildrenNames().equals(o.getChildrenNames()))
            return "fields differences: " + this.getChildrenNames() + "\n" + o.getChildrenNames();

        final String noLink = equalsChildrenNoLink(o, otherSystem);
        if (noLink != null)
            return noLink;

        // foreign keys
        final Set<Link> thisLinks = this.getDBSystemRoot().getGraph().getForeignLinks(this);
        final Set<Link> oLinks = o.getDBSystemRoot().getGraph().getForeignLinks(o);
        if (thisLinks.size() != oLinks.size())
            return "different number of foreign keys " + thisLinks + " != " + oLinks;
        for (final Link l : thisLinks) {
            final Link ol = o.getDBSystemRoot().getGraph().getForeignLink(o, l.getCols());
            if (ol == null)
                return "no foreign key for " + l.getLabel();
            final SQLName thisPath = l.getTarget().getContextualSQLName(this);
            final SQLName oPath = ol.getTarget().getContextualSQLName(o);
            if (thisPath.getItemCount() != oPath.getItemCount())
                return "unequal path size : " + thisPath + " != " + oPath;
            if (!thisPath.getName().equals(oPath.getName()))
                return "unequal referenced table name : " + thisPath.getName() + " != " + oPath.getName();
            final SQLSystem thisSystem = this.getServer().getSQLSystem();
            if (!getRule(l.getUpdateRule(), thisSystem, otherSystem).equals(getRule(ol.getUpdateRule(), thisSystem, otherSystem)))
                return "unequal update rule for " + l + ": " + l.getUpdateRule() + " != " + ol.getUpdateRule();
            if (!getRule(l.getDeleteRule(), thisSystem, otherSystem).equals(getRule(ol.getDeleteRule(), thisSystem, otherSystem)))
                return "unequal delete rule for " + l + ": " + l.getDeleteRule() + " != " + ol.getDeleteRule();
        }

        // indexes
        try {
            // order irrelevant
            final Set<Index> thisIndexesSet = new HashSet<Index>(this.getIndexes());
            final Set<Index> oIndexesSet = new HashSet<Index>(o.getIndexes());
            if (!thisIndexesSet.equals(oIndexesSet))
                return "indexes differences: " + thisIndexesSet + "\n" + oIndexesSet;
        } catch (SQLException e) {
            // MAYBE fetch indexes with the rest to avoid exn now
            return "couldn't get indexes: " + ExceptionUtils.getStackTrace(e);
        }

        return null;
    }

    private final Rule getRule(Rule r, SQLSystem thisSystem, SQLSystem otherSystem) {
        // compare exactly
        if (otherSystem == null)
            return r;
        // see http://code.google.com/p/h2database/issues/detail?id=352
        if (r == Rule.NO_ACTION && (thisSystem == SQLSystem.H2 || otherSystem == SQLSystem.H2))
            return Rule.RESTRICT;
        else
            return r;
    }

    /**
     * Compare the fields of this table, ignoring foreign constraints.
     * 
     * @param o the table to compare.
     * @param otherSystem the system <code>o</code> originates from, can be <code>null</code>.
     * @return <code>null</code> if each fields of this exists in <code>o</code> and is equal to it.
     */
    public synchronized final String equalsChildrenNoLink(SQLTable o, SQLSystem otherSystem) {
        for (final SQLField f : this.getFields()) {
            final SQLField oField = o.getField(f.getName());
            final boolean isPrimary = this.getPrimaryKeys().contains(f);
            if (isPrimary != o.getPrimaryKeys().contains(oField))
                return f + " is a primary not in " + o.getPrimaryKeys();
            final String equalsDesc = f.equalsDesc(oField, otherSystem, !isPrimary);
            if (equalsDesc != null)
                return equalsDesc;
        }
        return null;
    }

    public final SQLCreateMoveableTable getCreateTable() {
        return this.getCreateTable(this.getServer().getSQLSystem());
    }

    public synchronized final SQLCreateMoveableTable getCreateTable(final SQLSystem system) {
        final SQLSyntax syntax = SQLSyntax.get(system);
        final SQLCreateMoveableTable res = new SQLCreateMoveableTable(syntax, this.getName());
        for (final SQLField f : this.getOrderedFields()) {
            res.addColumn(f);
        }
        // primary keys
        res.setPrimaryKey(getPKsNames());
        // foreign keys
        for (final Link l : this.getDBSystemRoot().getGraph().getForeignLinks(this))
            // don't generate explicit CREATE INDEX for fk, we generate all indexes below
            // (this also avoid creating a fk index that wasn't there)
            res.addForeignConstraint(l, false);
        // constraints
        if (this.constraints != null)
            for (final Constraint added : this.getConstraints()) {
                if (added.getType() == ConstraintType.UNIQUE) {
                    res.addUniqueConstraint(added.getName(), added.getCols());
                } else
                    throw new UnsupportedOperationException("unsupported constraint: " + added);
            }
        // indexes
        try {
            final IPredicate<Index> pred = system.autoCreatesFKIndex() ? new IPredicate<Index>() {
                @Override
                public boolean evaluateChecked(Index i) {
                    // if auto create index, do not output current one, as it would be redundant
                    // (plus its name could clash with the automatic one)
                    return !getForeignKeysFields().contains(i.getFields());
                }
            } : null;
            for (final ChangeTable.OutsideClause c : syntax.getCreateIndexes(this, pred))
                res.addOutsideClause(c);
        } catch (SQLException e) {
            // MAYBE fetch indexes with the rest to avoid exn now
            throw new IllegalStateException("could not get indexes", e);
        }
        if (this.getComment() != null)
            res.addOutsideClause(syntax.getSetTableComment(getComment()));
        return res;
    }

    public final List<String> getPKsNames() {
        return this.getPKsNames(new ArrayList<String>());
    }

    public synchronized final <C extends Collection<String>> C getPKsNames(C pks) {
        for (final SQLField f : this.getPrimaryKeys()) {
            pks.add(f.getName());
        }
        return pks;
    }

    public final String[] getPKsNamesArray() {
        return getPKsNames().toArray(new String[0]);
    }

    /**
     * Return the indexes mapped by column names. Ie a key will have as value every index that
     * mentions it, and a multi-column index will be in several entries. Note: this is not robust
     * since {@link Index#getCols()} isn't.
     * 
     * @return the indexes mapped by column names.
     * @throws SQLException if an error occurs.
     */
    public final CollectionMap<String, Index> getIndexesByField() throws SQLException {
        final List<Index> indexes = this.getIndexes();
        final CollectionMap<String, Index> res = new CollectionMap<String, Index>(new HashSet<Index>(4), indexes.size());
        for (final Index i : indexes)
            for (final String col : i.getCols())
                res.put(col, i);
        return res;
    }

    /**
     * Return the indexes on the passed columns names. Note: this is not robust since
     * {@link Index#getCols()} isn't.
     * 
     * @param cols fields names.
     * @return the matching indexes.
     * @throws SQLException if an error occurs.
     */
    public final List<Index> getIndexes(final List<String> cols) throws SQLException {
        final List<Index> res = new ArrayList<Index>();
        for (final Index i : this.getIndexes())
            if (i.getCols().equals(cols))
                res.add(i);
        return res;
    }

    /**
     * Return the indexes of this table. Except the primary key as every system generates it
     * automatically.
     * 
     * @return the list of indexes.
     * @throws SQLException if an error occurs.
     */
    public synchronized final List<Index> getIndexes() throws SQLException {
        // in pg, a unique constraint creates a unique index that is not removeable
        // (except of course if we drop the constraint)
        // in mysql unique constraints and indexes are one and the same thing
        // so we must return them only in one (either getConstraints() or getIndexes())
        // anyway in all systems, a unique constraint or index achieve the same function
        // and so only generates the constraint and not the index
        final Set<List<String>> uniqConstraints;
        if (this.constraints != null) {
            uniqConstraints = new HashSet<List<String>>();
            for (final Constraint c : this.constraints) {
                if (c.getType() == ConstraintType.UNIQUE)
                    uniqConstraints.add(c.getCols());
            }
        } else
            uniqConstraints = Collections.emptySet();

        final List<Index> indexes = new ArrayList<Index>();
        Index currentIndex = null;
        for (final Map<String, Object> norm : this.getServer().getSQLSystem().getSyntax().getIndexInfo(this)) {
            final Index index = new Index(norm);
            final short seq = ((Number) norm.get("ORDINAL_POSITION")).shortValue();
            if (seq == 1) {
                if (canAdd(currentIndex, uniqConstraints))
                    indexes.add(currentIndex);
                currentIndex = index;
            } else {
                // continuing a multi-field index
                currentIndex.add(index);
            }
        }
        if (canAdd(currentIndex, uniqConstraints))
            indexes.add(currentIndex);

        // MAYBE another request to find out index.getMethod() (eg pg.getIndexesReq())
        return indexes;
    }

    private boolean canAdd(final Index currentIndex, final Set<List<String>> uniqConstraints) {
        if (currentIndex == null || currentIndex.isPKIndex())
            return false;

        return !currentIndex.isUnique() || !uniqConstraints.contains(currentIndex.getCols());
    }

    public final class Index {

        private final String name;
        private final List<String> attrs;
        private final List<String> cols;
        private final boolean unique;
        private String method;
        private String filter;

        Index(final Map<String, Object> row) {
            this((String) row.get("INDEX_NAME"), (String) row.get("COLUMN_NAME"), (Boolean) row.get("NON_UNIQUE"), (String) row.get("FILTER_CONDITION"));
        }

        Index(final String name, String col, Boolean nonUnique, String filter) {
            super();
            this.name = name;
            this.attrs = new ArrayList<String>();
            this.cols = new ArrayList<String>();
            this.unique = !nonUnique;
            this.method = null;
            this.filter = filter;

            this.add(this.name, col, this.unique);
        }

        public final SQLTable getTable() {
            return SQLTable.this;
        }

        /**
         * Adds a column to this multi-field index.
         * 
         * @param name the name of the index.
         * @param col the column to add.
         * @param unique whether the index is unique.
         * @throws IllegalStateException if <code>name</code> and <code>unique</code> are not the
         *         same as these.
         */
        final void add(final String name, String col, boolean unique) {
            if (!name.equals(this.name) || this.unique != unique)
                throw new IllegalStateException("incoherence");
            this.attrs.add(col);
            if (getTable().contains(col))
                this.cols.add(col);
        }

        final void add(final Index o) {
            this.add(o.getName(), o.cols.get(0), o.unique);
        }

        public final String getName() {
            return this.name;
        }

        public final boolean isUnique() {
            return this.unique;
        }

        /**
         * All attributes forming this index.
         * 
         * @return the components of this index, eg ["lower(name)", "age"].
         */
        public final List<String> getAttrs() {
            return this.attrs;
        }

        /**
         * The table columns in this index. Note that due to db system limitation this list is
         * incomplete (eg missing name).
         * 
         * @return the columns, eg ["age"].
         */
        public final List<String> getCols() {
            return this.cols;
        }

        public final List<SQLField> getFields() {
            final List<SQLField> res = new ArrayList<SQLField>(this.getCols().size());
            for (final String f : this.getCols())
                res.add(getTable().getField(f));
            return res;
        }

        public final void setMethod(String method) {
            this.method = method;
        }

        public final String getMethod() {
            return this.method;
        }

        /**
         * Filter for partial index.
         * 
         * @return the where clause or <code>null</code>.
         */
        public final String getFilter() {
            return this.filter;
        }

        final boolean isPKIndex() {
            return this.isUnique() && this.getAttrs().equals(getPKsNames());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " " + this.getName() + " unique: " + this.isUnique() + " cols: " + this.getAttrs();
        }

        // ATTN don't use name since it is often auto-generated (eg by a UNIQUE field)
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Index) {
                final Index o = (Index) obj;
                return this.isUnique() == o.isUnique() && this.getAttrs().equals(o.getAttrs());
            } else
                return false;
        }

        // ATTN use cols, so use only after cols are done
        @Override
        public int hashCode() {
            return this.getAttrs().hashCode() + ((Boolean) this.isUnique()).hashCode();
        }
    }
}
