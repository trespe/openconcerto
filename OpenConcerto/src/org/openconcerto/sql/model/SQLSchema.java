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

import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;
import org.openconcerto.sql.utils.ChangeTable.DeferredClause;
import org.openconcerto.sql.utils.SQLCreateMoveableTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.CopyOnWriteMap;
import org.openconcerto.utils.change.CollectionChangeEventCreator;
import org.openconcerto.xml.JDOMUtils;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.jcip.annotations.GuardedBy;

import org.apache.commons.dbutils.ResultSetHandler;
import org.jdom2.Element;

public final class SQLSchema extends SQLIdentifier {

    /**
     * set this system property to avoid writing to the db (be it CREATE TABLE or
     * INSERT/UPDATE/DELETE)
     */
    public static final String NOAUTO_CREATE_METADATA = "org.openconcerto.sql.noautoCreateMetadata";

    public static final String FWK_TABLENAME_PREFIX = "FWK_";
    static final String METADATA_TABLENAME = FWK_TABLENAME_PREFIX + "SCHEMA_METADATA";
    private static final String VERSION_MDKEY = "VERSION";
    private static final String VERSION_XMLATTR = "schemaVersion";

    public static final void getVersionAttr(final SQLSchema schema, final Appendable sb) {
        final String version = schema.getFullyRefreshedVersion();
        try {
            appendVersionAttr(version, sb);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't append version of " + schema, e);
        }
    }

    public static final void appendVersionAttr(final String version, final StringBuilder sb) {
        try {
            appendVersionAttr(version, (Appendable) sb);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't append version" + version, e);
        }
    }

    public static final void appendVersionAttr(final String version, final Appendable sb) throws IOException {
        if (version != null) {
            sb.append(' ');
            sb.append(VERSION_XMLATTR);
            sb.append("=\"");
            sb.append(JDOMUtils.OUTPUTTER.escapeAttributeEntities(version));
            sb.append('"');
        }
    }

    public static final String getVersion(final Element schemaElem) {
        return schemaElem.getAttributeValue(VERSION_XMLATTR);
    }

    public static final Map<String, String> getVersions(final SQLBase base, final Set<String> schemaNames) {
        // since we haven't an instance of SQLSchema, we can't know if the table exists
        return base.getFwkMetadata(schemaNames, VERSION_MDKEY);
    }

    static private String getVersionSQL(final SQLSyntax syntax) {
        return syntax.getFormatTimestamp("CURRENT_TIMESTAMP", true);
    }

    static SQLCreateMoveableTable getCreateMetadata(final SQLSyntax syntax) throws SQLException {
        if (Boolean.getBoolean(NOAUTO_CREATE_METADATA))
            return null;
        final SQLCreateMoveableTable create = new SQLCreateMoveableTable(syntax, METADATA_TABLENAME);
        create.addVarCharColumn("NAME", 100).addVarCharColumn("VALUE", 250);
        create.setPrimaryKey("NAME");
        create.addOutsideClause(new DeferredClause() {
            @Override
            public String asString(ChangeTable<?> ct, SQLName tableName) {
                return syntax.getInsertOne(tableName, Arrays.asList("NAME", "VALUE"), SQLBase.quoteStringStd(VERSION_MDKEY), getVersionSQL(syntax));
            }

            @Override
            public ClauseType getType() {
                return ClauseType.OTHER;
            }
        });
        return create;
    }

    // last DB structure version when we were fully refreshed
    @GuardedBy("this")
    private String version;
    private final CopyOnWriteMap<String, SQLTable> tables;
    // name -> src
    private final Map<String, String> procedures;
    @GuardedBy("this")
    private boolean fetchAllUndefIDs = true;

    SQLSchema(SQLBase base, String name) {
        super(base, name);
        this.tables = new CopyOnWriteMap<String, SQLTable>();
        this.procedures = new CopyOnWriteMap<String, String>();
    }

    public final SQLBase getBase() {
        return (SQLBase) this.getParent();
    }

    @Override
    protected void onDrop() {
        SQLTable.removeUndefID(this);
        super.onDrop();
    }

    /**
     * The version when this instance was last fully refreshed. In other words, if we refresh tables
     * by names (even if we name them all) this version isn't updated.
     * 
     * @return the version.
     */
    synchronized final String getFullyRefreshedVersion() {
        return this.version;
    }

    synchronized final void setFullyRefreshedVersion(final String vers) {
        this.version = vers;
    }

    // ** procedures

    /**
     * Return the procedures names and if possible their source.
     * 
     * @return the procedures in this schema.
     */
    public final Map<String, String> getProcedures() {
        return Collections.unmodifiableMap(this.procedures);
    }

    final void putProcedures(final Map<String, String> m) {
        this.procedures.putAll(m);
    }

    // clear the attributes that are not preserved (ie SQLTable) but recreated each time (ie
    // procedure)
    void clearNonPersistent() {
        this.procedures.clear();
    }

    // XMLStructureSource always pre-verify so we don't need the system root lock
    void load(Element schemaElem, Set<String> tableNames) {
        this.setFullyRefreshedVersion(getVersion(schemaElem));
        for (final Element elementTable : schemaElem.getChildren("table")) {
            this.refreshTable(elementTable, tableNames);
        }
        final Map<String, String> procMap = new HashMap<String, String>();
        for (final Element procElem : schemaElem.getChild("procedures").getChildren("proc")) {
            final Element src = procElem.getChild("src");
            procMap.put(procElem.getAttributeValue("name"), src == null ? null : src.getText());
        }
        this.putProcedures(procMap);
    }

    /**
     * Fetch table from the DB.
     * 
     * @param tableName the name of the table to fetch.
     * @return the up to date table, <code>null</code> if not found
     * @throws SQLException if an error occurs.
     */
    final SQLTable fetchTable(final String tableName) throws SQLException {
        synchronized (getTreeMutex()) {
            this.getBase().fetchTables(TablesMap.createFromTables(getName(), Collections.singleton(tableName)));
            return this.getTable(tableName);
        }
    }

    void mutateTo(SQLSchema newSchema) {
        assert Thread.holdsLock(this.getDBSystemRoot().getTreeMutex());
        synchronized (this) {
            this.version = newSchema.version;
            this.clearNonPersistent();
            this.putProcedures(newSchema.procedures);
            // since one can refresh only some tables, newSchema is a subset of this
            for (final SQLTable t : newSchema.getTables()) {
                this.getTable(t.getName()).mutateTo(t);
            }
        }
    }

    // ** tables

    final SQLTable addTable(String tableName) {
        synchronized (getTreeMutex()) {
            return this.addTableWithoutSysRootLock(tableName);
        }
    }

    final SQLTable addTableWithoutSysRootLock(String tableName) {
        if (this.contains(tableName))
            throw new IllegalStateException(tableName + " already in " + this);
        final CollectionChangeEventCreator c = this.createChildrenCreator();
        final SQLTable res = new SQLTable(this, tableName);
        this.tables.put(tableName, res);
        this.fireChildrenChanged(c);
        return res;
    }

    private final void refreshTable(Element tableElem, Set<String> tableNames) {
        final String tableName = tableElem.getAttributeValue("name");
        if (tableNames.contains(tableName))
            this.getTable(tableName).loadFields(tableElem);
    }

    /**
     * Refresh the table of the current row of rs.
     * 
     * @param metaData the metadata.
     * @param rs the resultSet from getColumns().
     * @param version the version of the schema.
     * @return whether <code>rs</code> has a next row, <code>null</code> if the current row is not
     *         part of this, and thus rs hasn't moved.
     * @throws SQLException
     */
    final Boolean refreshTable(DatabaseMetaData metaData, ResultSet rs, final String version) throws SQLException {
        synchronized (getTreeMutex()) {
            synchronized (this) {
                final String tableName = rs.getString("TABLE_NAME");
                if (this.contains(tableName)) {
                    return this.getTable(tableName).fetchFields(metaData, rs, version);
                } else {
                    // eg in pg getColumns() return columns of BATIMENT_ID_seq
                    return null;
                }
            }
        }
    }

    final void rmTable(String tableName) {
        synchronized (getTreeMutex()) {
            this.rmTableWithoutSysRootLock(tableName);
        }
    }

    private final void rmTableWithoutSysRootLock(String tableName) {
        final CollectionChangeEventCreator c = this.createChildrenCreator();
        final SQLTable tableToDrop = this.tables.remove(tableName);
        this.fireChildrenChanged(c);
        if (tableToDrop != null)
            tableToDrop.dropped();
    }

    public final SQLTable getTable(String tablename) {
        return this.tables.get(tablename);
    }

    /**
     * Return the tables in this schema.
     * 
     * @return an unmodifiable Set of the tables' names.
     */
    public Set<String> getTableNames() {
        return Collections.unmodifiableSet(this.tables.keySet());
    }

    /**
     * Return all the tables in this schema.
     * 
     * @return a Set of SQLTable.
     */
    public Set<SQLTable> getTables() {
        return new HashSet<SQLTable>(this.tables.values());
    }

    @Override
    public Map<String, SQLTable> getChildrenMap() {
        return this.tables.getImmutable();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getName();
    }

    public String toXML() {
        // always save even without version, as some tables might still be up to date

        // a table is about 16000 characters
        final StringBuilder sb = new StringBuilder(16000 * 16);
        sb.append("<schema ");
        if (this.getName() != null) {
            sb.append(" name=\"");
            sb.append(JDOMUtils.OUTPUTTER.escapeAttributeEntities(this.getName()));
            sb.append('"');
        }
        synchronized (getTreeMutex()) {
            synchronized (this) {
                getVersionAttr(this, sb);
                sb.append(" >\n");

                sb.append("<procedures>\n");
                for (final Entry<String, String> e : this.procedures.entrySet()) {
                    sb.append("<proc name=\"");
                    sb.append(JDOMUtils.OUTPUTTER.escapeAttributeEntities(e.getKey()));
                    sb.append("\" ");
                    if (e.getValue() == null) {
                        sb.append("/>");
                    } else {
                        sb.append("><src>");
                        sb.append(JDOMUtils.OUTPUTTER.escapeElementEntities(e.getValue()));
                        sb.append("</src></proc>\n");
                    }
                }
                sb.append("</procedures>\n");
                for (final SQLTable table : this.getTables()) {
                    // passing our sb to table don't go faster
                    sb.append(table.toXML());
                    sb.append("\n");
                }
                sb.append("</schema>");
            }
        }

        return sb.toString();
    }

    String getFwkMetadata(String name) {
        if (!this.contains(METADATA_TABLENAME))
            return null;

        // we just tested for table existence
        return this.getBase().getFwkMetadata(this.getName(), name);
    }

    boolean setFwkMetadata(String name, String value) throws SQLException {
        return this.setFwkMetadata(name, value, true).get0();
    }

    /**
     * Set the value of a metadata.
     * 
     * @param name name of the metadata, e.g. "Customer".
     * @param sqlExpr SQL value of the metadata, e.g. "'ACME, inc'".
     * @param createTable whether the metadata table should be automatically created if necessary.
     * @return <code>true</code> if the value was set, <code>false</code> otherwise ; the new value
     *         (<code>null</code> if the value wasn't set, i.e. if the value cannot be
     *         <code>null</code> the boolean isn't needed).
     * @throws SQLException if an error occurs while setting the value.
     */
    Tuple2<Boolean, String> setFwkMetadata(String name, String sqlExpr, boolean createTable) throws SQLException {
        if (Boolean.getBoolean(NOAUTO_CREATE_METADATA))
            return Tuple2.create(false, null);

        final SQLSystem sys = getServer().getSQLSystem();
        final SQLSyntax syntax = sys.getSyntax();
        final SQLDataSource ds = this.getDBSystemRoot().getDataSource();
        synchronized (this.getTreeMutex()) {
            // don't refresh until after the insert, that way if the refresh triggers an access to
            // the metadata name will already be set to value.
            final boolean shouldRefresh;
            if (createTable && !this.contains(METADATA_TABLENAME)) {
                final SQLCreateMoveableTable create = getCreateMetadata(syntax);
                ds.execute(create.asString(getDBRoot().getName()));
                shouldRefresh = true;
            } else {
                shouldRefresh = false;
            }

            final Tuple2<Boolean, String> res;
            if (createTable || this.contains(METADATA_TABLENAME)) {
                // don't use SQLRowValues, cause it means getting the SQLTable and thus calling
                // fetchTables(), but setFwkMetadata() might itself be called by fetchTables()
                // furthermore SQLRowValues support only rowable tables

                final List<String> queries = new ArrayList<String>();

                final SQLName tableName = new SQLName(this.getBase().getName(), this.getName(), METADATA_TABLENAME);
                final String where = " WHERE " + SQLBase.quoteIdentifier("NAME") + " = " + getBase().quoteString(name);
                queries.add("DELETE FROM " + tableName.quote() + where);

                final String returning = sys == SQLSystem.POSTGRESQL ? " RETURNING " + SQLBase.quoteIdentifier("VALUE") : "";
                final String ins = syntax.getInsertOne(tableName, Arrays.asList("NAME", "VALUE"), getBase().quoteString(name), sqlExpr) + returning;
                queries.add(ins);

                final List<? extends ResultSetHandler> handlers;
                if (returning.length() == 0) {
                    queries.add("SELECT " + SQLBase.quoteIdentifier("VALUE") + " FROM " + tableName.quote() + where);
                    handlers = Arrays.asList(null, null, SQLDataSource.SCALAR_HANDLER);
                } else {
                    handlers = Arrays.asList(null, SQLDataSource.SCALAR_HANDLER);
                }

                final List<?> ress = SQLUtils.executeMultiple(getDBSystemRoot(), queries, handlers);
                res = Tuple2.create(true, (String) ress.get(ress.size() - 1));
            } else {
                res = Tuple2.create(false, null);
            }
            if (shouldRefresh)
                this.fetchTable(METADATA_TABLENAME);
            return res;
        }
    }

    /**
     * The current version in the database.
     * 
     * @return current version in the database.
     */
    public final String getVersion() {
        return this.getFwkMetadata(VERSION_MDKEY);
    }

    // TODO assure that the updated version is different that current one and unique
    // If we have a fast in-memory DB, some additions might get lost
    // Perhaps something like VERSION = date seconds || '_' || (select cast( substring(indexof '_')
    // as int ) + 1 from VERSION) ; e.g. 20110701-1021_01352
    public final String updateVersion() throws SQLException {
        return this.updateVersion(true);
    }

    final String updateVersion(boolean createTable) throws SQLException {
        return this.setFwkMetadata(SQLSchema.VERSION_MDKEY, getVersionSQL(SQLSyntax.get(this)), createTable).get1();
    }

    public synchronized final void setFetchAllUndefinedIDs(final boolean b) {
        this.fetchAllUndefIDs = b;
    }

    /**
     * A boolean indicating if one {@link SQLTable#getUndefinedID()} should fetch IDs for the whole
     * schema or just that table. The default is true which is faster but requires that all tables
     * are coherent.
     * 
     * @return <code>true</code> if all undefined IDs are fetched together.
     */
    public synchronized final boolean isFetchAllUndefinedIDs() {
        return this.fetchAllUndefIDs;
    }
}
