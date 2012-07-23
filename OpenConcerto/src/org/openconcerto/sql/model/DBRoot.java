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

import org.openconcerto.sql.model.SQLRowValuesCluster.Insert;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.ChangeTable.ConcatStep;
import org.openconcerto.sql.utils.ChangeTable.FCSpec;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLCreateRoot;
import org.openconcerto.sql.utils.SQLCreateTableBase;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQL_URL;
import org.openconcerto.utils.CollectionUtils;

import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import net.jcip.annotations.ThreadSafe;

/**
 * The root of a database, in mysql a SQLBase, in postgresql a SQLSchema.
 * 
 * @author Sylvain
 */
@ThreadSafe
public final class DBRoot extends DBStructureItemDB {

    static DBRoot get(SQLBase b, String n) {
        final DBRoot ancestor = b.getDBRoot();
        final DBStructureItemDB parent = ancestor == null ? b.getDB() : ancestor.getParent();
        return (DBRoot) parent.getChild(n);
    }

    DBRoot(DBStructureItemJDBC delegate) {
        super(delegate);
    }

    public final SQLBase getBase() {
        return this.getJDBC().getAncestor(SQLBase.class);
    }

    public SQLTable getTable(String name) {
        return (SQLTable) getJDBC(this.getChild(name));
    }

    protected final Map<String, SQLTable> getTablesMap() {
        return this.getSchema().getChildrenMap();
    }

    public SQLTable getTableDesc(String name) {
        return this.getDescLenient(name, SQLTable.class);
    }

    public SQLTable findTable(String name) {
        return this.findTable(name, false);
    }

    public SQLTable findTable(String name, final boolean mustExist) {
        final SQLTable res = this.getTable(name);
        if (res != null)
            return res;
        else
            return this.getDBSystemRoot().findTable(name, mustExist);
    }

    /**
     * Return the tables of this root.
     * 
     * @return our tables.
     */
    public Set<SQLTable> getTables() {
        return getJDBC().getDescendants(SQLTable.class);
    }

    /**
     * Create the passed table in this root.
     * 
     * @param createTable how to create the table.
     * @return the newly created table.
     * @throws SQLException if an error occurs.
     */
    public final SQLTable createTable(final SQLCreateTableBase<?> createTable) throws SQLException {
        return this.createTable(createTable, null);
    }

    public final SQLTable createTable(final SQLCreateTableBase<?> createTable, final Map<String, ?> undefinedNonDefaultValues) throws SQLException {
        synchronized (this.getDBSystemRoot().getTreeMutex()) {
            this.createTables(Collections.<SQLCreateTableBase<?>, Map<String, ?>> singletonMap(createTable, undefinedNonDefaultValues));
            return this.getTable(createTable.getName());
        }
    }

    public final void createTables(final SQLCreateTableBase<?>... createTables) throws SQLException {
        this.createTables(Arrays.asList(createTables));
    }

    public final void createTables(final Collection<? extends SQLCreateTableBase<?>> createTables) throws SQLException {
        this.createTables(CollectionUtils.fillMap(new HashMap<SQLCreateTableBase<?>, Map<String, ?>>(), createTables), false);
    }

    /**
     * Create the passed tables and undefined rows, then {@link #refetch()}.
     * 
     * @param undefinedNonDefaultValues the undefined row for each table, if the value is
     *        <code>null</code> then no undefined row will be created.
     * @throws SQLException if an error occurs.
     */
    public final void createTables(final Map<? extends SQLCreateTableBase<?>, ? extends Map<String, ?>> undefinedNonDefaultValues) throws SQLException {
        this.createTables(undefinedNonDefaultValues, !Collections.singleton(null).containsAll(undefinedNonDefaultValues.values()));
    }

    private final void createTables(final Map<? extends SQLCreateTableBase<?>, ? extends Map<String, ?>> undefinedNonDefaultValues, final boolean atLeast1UndefRow) throws SQLException {
        final int size = undefinedNonDefaultValues.size();
        final String soleTableName;
        if (size == 0)
            return;
        else if (size == 1)
            soleTableName = undefinedNonDefaultValues.keySet().iterator().next().getName();
        else
            soleTableName = null;

        SQLUtils.executeAtomic(getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, SQLException>() {
            @Override
            public Object handle(SQLDataSource ds) throws SQLException {
                // don't create foreign constraints now, so we can insert undefined with cycles
                final List<List<String>> createTablesSQL = ChangeTable.cat(undefinedNonDefaultValues.keySet(), getName(), EnumSet.of(ConcatStep.ADD_FOREIGN));
                for (final String sql : createTablesSQL.get(0))
                    ds.execute(sql);
                final Map<SQLCreateTableBase<?>, Number> newUndefIDs;
                final Map<SQLTable, SQLCreateTableBase<?>> newTables;
                if (atLeast1UndefRow) {
                    newUndefIDs = new HashMap<SQLCreateTableBase<?>, Number>();
                    newTables = new HashMap<SQLTable, SQLCreateTableBase<?>>();
                    refetch(soleTableName);
                } else {
                    newUndefIDs = Collections.emptyMap();
                    newTables = null;
                }
                for (final Entry<? extends SQLCreateTableBase<?>, ? extends Map<String, ?>> e : undefinedNonDefaultValues.entrySet()) {
                    final SQLCreateTableBase<?> createTable = e.getKey();
                    final String tableName = createTable.getName();
                    final Map<String, ?> m = e.getValue();
                    // insert undefined row if requested and record its ID
                    if (m == null) {
                        SQLTable.setUndefID(getSchema(), tableName, null);
                    } else {
                        final SQLTable t = getTable(tableName);
                        newUndefIDs.put(createTable, null);
                        newTables.put(t, createTable);
                        if (t.isRowable()) {
                            final SQLRowValues vals = new SQLRowValues(t, m);
                            if (t.isOrdered())
                                vals.put(t.getOrderField().getName(), ReOrder.MIN_ORDER);
                            for (final SQLField f : t.getContentFields()) {
                                if (!vals.getFields().contains(f.getName()) && f.isNullable() != Boolean.TRUE && f.getDefaultValue() == null) {
                                    final Class<?> javaType = f.getType().getJavaType();
                                    final Object o;
                                    if (String.class.isAssignableFrom(javaType))
                                        o = "";
                                    else if (Number.class.isAssignableFrom(javaType))
                                        o = 0;
                                    else if (Boolean.class.isAssignableFrom(javaType))
                                        o = Boolean.FALSE;
                                    else if (Date.class.isAssignableFrom(javaType))
                                        o = new Date(0);
                                    else
                                        throw new UnsupportedOperationException("cannot find value for " + f.getSQLName());
                                    vals.put(f.getName(), o);
                                }
                            }
                            // PK from the DB, but use our order
                            // don't try to validate since table has neither undefined row nor
                            // constraints
                            vals.getGraph().store(new Insert(false, true), false);
                            final SQLRow undefRow = vals.getGraph().getRow(vals);
                            SQLTable.setUndefID(getSchema(), tableName, undefRow.getID());
                            newUndefIDs.put(createTable, undefRow.getIDNumber());
                        }
                    }
                }
                // update undefined rows pointing to other undefined rows
                // set default value for created foreign fields
                for (final Entry<SQLCreateTableBase<?>, Number> e : newUndefIDs.entrySet()) {
                    final SQLCreateTableBase<?> createTable = e.getKey();
                    final SQLTable t = getTable(createTable.getName());

                    final Number undefID = e.getValue();
                    final UpdateBuilder update;
                    if (undefID != null) {
                        update = new UpdateBuilder(t);
                        update.setWhere(new Where(t.getKey(), "=", undefID));
                    } else {
                        update = null;
                    }

                    final AlterTable alterTable = new AlterTable(t);
                    for (final FCSpec fc : createTable.getForeignConstraints()) {
                        if (fc.getCols().size() == 1) {
                            final SQLTable targetT = t.getDescLenient(fc.getRefTable(), SQLTable.class);
                            final Number foreignUndefID = newUndefIDs.get(newTables.get(targetT));
                            if (foreignUndefID != null) {
                                final String ffName = fc.getCols().get(0);
                                final String foreignUndefIDSQL = t.getField(ffName).getType().toString(foreignUndefID);
                                alterTable.alterColumnDefault(ffName, foreignUndefIDSQL);
                                update.set(ffName, foreignUndefIDSQL);
                            }
                        }
                    }

                    if (update != null && !update.isEmpty())
                        ds.execute(update.asString());
                    if (!alterTable.isEmpty())
                        ds.execute(alterTable.asString());
                }
                for (final String sql : createTablesSQL.get(1))
                    ds.execute(sql);
                // always execute updateVersion() after setUndefID() to avoid DB transactions
                // deadlock since setUndefID() itself ends with updateVersion().
                getSchema().updateVersion();
                return null;
            }
        });
        this.refetch(soleTableName);
    }

    public SQLField getField(String name) {
        return this.getDesc(name, SQLField.class);
    }

    /**
     * Return the value of a metadata for this root.
     * 
     * @param name name of the metadata, eg "Customer".
     * @return value of the metadata or <code>null</code> if it doesn't exist, eg "ACME, inc".
     */
    public final String getMetadata(final String name) {
        return getSchema().getFwkMetadata(name);
    }

    // since by definition DBRoot is one level above SQLTable, there's only one schema below it
    public final SQLSchema getSchema() {
        return (SQLSchema) this.getJDBC().getNonNullDBParent();
    }

    /**
     * Set the value of a metadata.
     * 
     * @param name name of the metadata, eg "Customer".
     * @param value value of the metadata, eg "ACME, inc".
     * @return <code>true</code> if the value was set, <code>false</code> otherwise.
     * @throws SQLException if an error occurs while setting the value.
     */
    public final boolean setMetadata(final String name, final String value) throws SQLException {
        return getSchema().setFwkMetadata(name, value);
    }

    public final DatabaseGraph getGraph() {
        return this.getDBSystemRoot().getGraph();
    }

    /**
     * Refresh this from the database.
     * 
     * @throws SQLException if an error occurs.
     */
    public void refetch() throws SQLException {
        this.refetch(null);
    }

    public SQLTable refetch(final String tableName) throws SQLException {
        if (tableName == null) {
            this.getSchema().refetch();
            return null;
        } else {
            return this.getSchema().fetchTable(tableName);
        }
    }

    public final SQLCreateRoot getDefinitionSQL(final SQLSystem sys) {
        final SQLCreateRoot res = new SQLCreateRoot(sys.getSyntax(), this.getName());
        // order by name to be able to do diffs
        for (final SQLTable table : new TreeMap<String, SQLTable>(this.getTablesMap()).values()) {
            res.addTable(table.getCreateTable(sys));
        }
        return res;
    }

    public final String equalsDesc(final DBRoot o) {
        return this.equalsDesc(o, null);
    }

    public final String equalsDesc(final DBRoot o, final SQLSystem otherSystem) {
        if (this == o)
            return null;
        if (null == o)
            return "other is null";

        final Map<String, SQLTable> thisTables = this.getTablesMap();
        final Map<String, SQLTable> oTables = o.getTablesMap();

        if (!thisTables.keySet().equals(oTables.keySet()))
            return "unequal table names: " + thisTables.keySet() + " != " + oTables.keySet();

        for (final Entry<String, SQLTable> e : thisTables.entrySet()) {
            final String name = e.getKey();
            final SQLTable t = e.getValue();
            final String eqDesc = t.equalsDesc(oTables.get(name), otherSystem, true);
            if (eqDesc != null)
                return "unequal " + name + ": " + eqDesc;
        }
        return null;
    }

    /**
     * Return the url pointing to this root.
     * 
     * @return the url or <code>null</code> if this cannot be represented as an {@link SQL_URL} (eg
     *         jdbc:h2:file:/a/b/c).
     */
    public final SQL_URL getURL() {
        final String hostname = this.getServer().getHostname();
        if (hostname == null)
            return null;
        final SQLSystem system = this.getServer().getSQLSystem();
        String url = system.name().toLowerCase() + "://" + this.getDBSystemRoot().getDataSource().getUsername() + "@" + hostname + "/";
        // handle systems w/o systemRoot
        if (system.getDBLevel(DBSystemRoot.class) != HierarchyLevel.SQLSERVER) {
            url += this.getDBSystemRoot().getName() + "/";
        }
        url += this.getName();
        try {
            return SQL_URL.create(url);
        } catch (URISyntaxException e) {
            // should not happen
            throw new IllegalStateException("could not produce url for " + this, e);
        }
    }

    /**
     * A string with the content of this root.
     * 
     * <pre>
     *   /TYPE_COURANT/
     *   ID_TYPE_COURANT t: 4 def: null
     *   LABEL t: 12 def: 
     *   ARCHIVE t: 4 def: 0
     *   ORDRE t: 4 def: 1
     * </pre>
     * 
     * @return the content of this.
     */
    public String dump() {
        String res = "";
        for (final SQLTable table : new TreeMap<String, SQLTable>(this.getTablesMap()).values()) {
            res += table + "\n";
            for (final SQLField f : new TreeMap<String, SQLField>(table.getChildrenMap()).values()) {
                res += f.getName() + " t: " + f.getType() + " def: " + f.getDefaultValue() + "\n";
            }
            res += "\n";
        }
        return res;
    }
}
