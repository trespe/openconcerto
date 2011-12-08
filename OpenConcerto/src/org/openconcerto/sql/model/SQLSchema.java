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

import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.utils.change.CollectionChangeEventCreator;
import org.openconcerto.xml.JDOMUtils;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jdom.Element;

public final class SQLSchema extends SQLIdentifier {

    /**
     * set this system property to avoid writing to the db (be it CREATE TABLE or
     * INSERT/UPDATE/DELETE)
     */
    public static final String NOAUTO_CREATE_METADATA = "org.openconcerto.sql.noautoCreateMetadata";

    static final String METADATA_TABLENAME = "FWK_SCHEMA_METADATA";
    private static final String VERSION_MDKEY = "VERSION";
    private static final String VERSION_XMLATTR = "schemaVersion";

    public static final void getVersionAttr(final SQLSchema schema, final Appendable sb) {
        final String version = schema.getVersion();
        if (version != null) {
            try {
                sb.append(' ');
                sb.append(VERSION_XMLATTR);
                sb.append("=\"");
                sb.append(JDOMUtils.OUTPUTTER.escapeAttributeEntities(version));
                sb.append('"');
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't append version of " + schema, e);
            }
        }
    }

    public static final String getVersion(final Element schemaElem) {
        return schemaElem.getAttributeValue(VERSION_XMLATTR);
    }

    public static final String getVersion(final SQLBase base, final String schemaName) {
        return base.getFwkMetadata(schemaName, VERSION_MDKEY);
    }

    // values are null until getTable() or fillTableMap
    private final Map<String, SQLTable> tables;
    // name -> src
    private final Map<String, String> procedures;

    private boolean fetchAllUndefIDs = true;

    SQLSchema(SQLBase base, String name) {
        super(base, name);
        this.tables = new HashMap<String, SQLTable>();
        this.procedures = new HashMap<String, String>();
    }

    public final SQLBase getBase() {
        return (SQLBase) this.getParent();
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

    final void addProcedure(String procName) {
        this.procedures.put(procName, null);
    }

    final void setProcedureSource(String procName, String src) {
        this.procedures.put(procName, src);
    }

    // clear the attributes that are not preserved (ie SQLTable) but recreated each time (ie
    // procedure)
    void clearNonPersistent() {
        this.procedures.clear();
    }

    void load(Element schemaElem) {
        final List l = schemaElem.getChildren("table");
        for (int i = 0; i < l.size(); i++) {
            final Element elementTable = (Element) l.get(i);
            this.refreshTable(elementTable);
        }
        for (final Object proc : schemaElem.getChild("procedures").getChildren("proc")) {
            final Element procElem = (Element) proc;
            final Element src = procElem.getChild("src");
            this.setProcedureSource(procElem.getAttributeValue("name"), src == null ? null : src.getText());
        }
    }

    void mutateTo(SQLSchema newSchema) {
        this.clearNonPersistent();
        this.procedures.putAll(newSchema.procedures);

        for (final SQLTable t : this.getTables()) {
            t.mutateTo(newSchema.getTable(t.getName()));
        }
    }

    // ** tables

    final void addTable(String tableName) {
        if (this.contains(tableName))
            throw new IllegalStateException(tableName + " already in " + this);
        final CollectionChangeEventCreator c = this.createChildrenCreator();
        this.tables.put(tableName, new SQLTable(this, tableName));
        this.fireChildrenChanged(c);
    }

    final void refreshTable(Element tableElem) {
        final String tableName = tableElem.getAttributeValue("name");
        this.getTable(tableName).loadFields(tableElem);
    }

    /**
     * Refresh the table of the current row of rs.
     * 
     * @param metaData the metadata.
     * @param rs the resultSet from getColumns().
     * @return whether <code>rs</code> has a next row, <code>null</code> if the current row is not
     *         part of this, and thus rs hasn't moved.
     * @throws SQLException
     */
    final Boolean refreshTable(DatabaseMetaData metaData, ResultSet rs) throws SQLException {
        final String tableName = rs.getString("TABLE_NAME");
        if (this.contains(tableName)) {
            return this.getTable(tableName).fetchFields(metaData, rs);
        } else {
            // eg in pg getColumns() return columns of BATIMENT_ID_seq
            return null;
        }
    }

    final void rmTable(String tableName) {
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
    public SQLIdentifier getChild(String name) {
        return this.getTable(name);
    }

    @Override
    public Set<String> getChildrenNames() {
        return this.tables.keySet();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getName();
    }

    public String toXML() {
        // a table is about 16000 characters
        final StringBuilder sb = new StringBuilder(16000 * 16);
        sb.append("<schema ");
        if (this.getName() != null) {
            sb.append(" name=\"");
            sb.append(JDOMUtils.OUTPUTTER.escapeAttributeEntities(this.getName()));
            sb.append('"');
        }
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

        return sb.toString();
    }

    String getFwkMetadata(String name) {
        if (!this.contains(METADATA_TABLENAME))
            return null;

        return this.getBase().getFwkMetadata(this.getName(), name);
    }

    boolean setFwkMetadata(String name, String value) throws SQLException {
        return this.setFwkMetadata(name, value, true);
    }

    /**
     * Set the value of a metadata.
     * 
     * @param name name of the metadata, eg "Customer".
     * @param value value of the metadata, eg "ACME, inc".
     * @param createTable whether the metadata table should be automatically created if necessary.
     * @return <code>true</code> if the value was set, <code>false</code> otherwise.
     * @throws SQLException if an error occurs while setting the value.
     */
    boolean setFwkMetadata(String name, String value, boolean createTable) throws SQLException {
        if (Boolean.getBoolean(NOAUTO_CREATE_METADATA))
            return false;
        // don't refresh until after the insert, that way if the refresh triggers an access to the
        // metadata name will already be set to value.
        final boolean shouldRefresh;
        if (createTable && !this.contains(METADATA_TABLENAME)) {
            final SQLCreateTable create = new SQLCreateTable(this.getDBRoot(), METADATA_TABLENAME);
            create.setPlain(true);
            create.addVarCharColumn("NAME", 100).addVarCharColumn("VALUE", 250);
            create.setPrimaryKey("NAME");
            this.getBase().getDataSource().execute(create.asString());
            shouldRefresh = true;
        } else
            shouldRefresh = false;

        final boolean res;
        if (createTable || this.contains(METADATA_TABLENAME)) {
            // don't use SQLRowValues, cause it means getting the SQLTable and thus calling
            // fetchTables(), but setFwkMetadata() might itself be called by fetchTables()
            // furthermore SQLRowValues support only rowable tables
            final SQLName tableName = new SQLName(this.getBase().getName(), this.getName(), METADATA_TABLENAME);
            final String del = SQLSelect.quote("DELETE FROM %i WHERE %i = %s", tableName, "NAME", name);
            final String ins = SQLSelect.quote("INSERT INTO %i(%i,%i) VALUES(%s,%s)", tableName, "NAME", "VALUE", name, value);
            SQLUtils.executeAtomic(this.getBase().getDataSource(), new SQLFactory<Object>() {
                public Object create() throws SQLException {
                    getBase().getDataSource().execute(del);
                    getBase().getDataSource().execute(ins);
                    return null;
                }
            });
            res = true;
        } else
            res = false;
        if (shouldRefresh)
            this.getBase().fetchTables(Collections.singleton(this.getName()));
        return res;
    }

    public final String getVersion() {
        return this.getFwkMetadata(VERSION_MDKEY);
    }

    // TODO assure that the updated version is different that current one and unique
    // For now the resolution is the millisecond, this poses 2 problems :
    // 1/ if we have a fast in-memory DB, some additions might get lost
    // 2/ if not everyone's time is correct ; if client1 calls updateVersion(), then client2 makes
    // some changes and calls updateVersion(), his clock might be returning the same time than
    // client1 had when he called updateVersion().
    // Perhaps something like VERSION = date seconds || '_' || (select cast( substring(indexof '_')
    // as int ) + 1 from VERSION) ; e.g. 20110701-1021_01352
    public final String updateVersion() throws SQLException {
        final String res = XMLStructureSource.XMLDATE_FMT.format(new Date());
        this.setFwkMetadata(SQLSchema.VERSION_MDKEY, res);
        return res;
    }

    public final void setFetchAllUndefinedIDs(final boolean b) {
        this.fetchAllUndefIDs = b;
    }

    /**
     * A boolean indicating if one {@link SQLTable#getUndefinedID()} should fetch IDs for the whole
     * schema or just that table. The default is true which is faster but requires that all tables
     * are coherent.
     * 
     * @return <code>true</code> if all undefined IDs are fetched together.
     */
    public final boolean isFetchAllUndefinedIDs() {
        return this.fetchAllUndefIDs;
    }
}
