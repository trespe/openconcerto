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

import static java.util.Arrays.asList;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SystemQueryExecutor.QueryExn;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.SQLCreateMoveableTable;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.IncludeExclude;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class JDBCStructureSource extends StructureSource<SQLException> {

    static String getCacheError(final String rootName) {
        return "Cache won't be used for " + rootName + " since there's no metadata table";
    }

    private final Set<String> schemas;
    private final CollectionMap<SQLName, String> tableNames;

    public JDBCStructureSource(SQLBase b, TablesMap scope, Map<String, SQLSchema> newStruct, Set<String> outOfDateSchemas) {
        super(b, scope, newStruct, outOfDateSchemas);
        this.schemas = new HashSet<String>();
        this.tableNames = new CollectionMap<SQLName, String>(new ArrayList<String>(2));
        // if we can't access the metadata directly from the base, obviously the base is not ok
        this.setPreVerify(false);
    }

    private String getTablePattern(final IncludeExclude<String> tables) {
        assert !tables.isNoneIncluded();
        return tables.getSole("%");
    }

    @Override
    Set<String> getOutOfDateSchemas() {
        // by definition, this is up to date
        return Collections.emptySet();
    }

    @Override
    protected void getNames(final Connection conn) throws SQLException {
        final DatabaseMetaData metaData = conn.getMetaData();
        // les tables de tous les schemas
        // don't use getSchemas() since we can't limit to a particular db and it returns db private
        // schemas
        // les tables de la base
        final CollectionMap<SQLName, String> tableNames = new CollectionMap<SQLName, String>(new ArrayList<String>(2));

        // to find empty schemas (with no tables) : all schemas - system schemas
        final Set<String> schemas = this.getJDBCSchemas(metaData);
        this.filterOutOfScope(schemas);
        // getTables() only supports pattern (eg LIKE) so we must make multiple calls
        // copy schemas so we can remove system schemas directly
        for (final String s : new HashSet<String>(schemas)) {
            final IncludeExclude<String> tablesToRefresh = this.getTablesInScope(s);
            if (tablesToRefresh.isNoneIncluded())
                continue;
            final ResultSet rs = metaData.getTables(this.getBase().getMDName(), s, getTablePattern(tablesToRefresh), new String[] { "TABLE", "SYSTEM TABLE", "VIEW" });
            while (rs.next()) {
                final String tableType = rs.getString("TABLE_TYPE");
                final String schemaName = rs.getString("TABLE_SCHEM");
                if (tableType.equals("SYSTEM TABLE")) {
                    schemas.remove(schemaName);
                } else {
                    final String tableName = rs.getString("TABLE_NAME");
                    if (tablesToRefresh.isIncluded(tableName)) {
                        // MySQL needs this.addConnectionProperty("useInformationSchema", "true");
                        // but the time goes from 3.5s to 20s
                        tableNames.putAll(new SQLName(schemaName, tableName), asList(rs.getString("TABLE_TYPE"), rs.getString("REMARKS")));
                    }
                }
            }
        }

        // keep only tables in remaining schemas
        final Iterator<SQLName> iter = tableNames.keySet().iterator();
        while (iter.hasNext()) {
            final SQLName tableName = iter.next();
            if (!schemas.contains(tableName.getItemLenient(-2)))
                iter.remove();
        }

        // create metadata table here to avoid a second refresh
        // null if we shouldn't alter the base
        final SQLCreateMoveableTable createMetadata = SQLSchema.getCreateMetadata(getBase().getServer().getSQLSystem().getSyntax());
        final boolean useCache = getBase().getDBSystemRoot().useCache();
        Statement stmt = null;
        try {
            for (final String schema : schemas) {
                // * if toRefresh != null then the SQLSchema instance is already created (see
                // SQLBase.assureAllTables()) : the table should also already be created
                // * if toRefresh == null && the table isn't in scope then it has already been
                // loaded in externalStruct
                if (getTablesToRefresh(schema) == null && this.getTablesInScope(schema).isIncluded(SQLSchema.METADATA_TABLENAME)) {
                    final SQLName md = new SQLName(schema, SQLSchema.METADATA_TABLENAME);
                    if (!tableNames.containsKey(md)) {
                        // handle systems where schema are not DBRoot (e.g. MySQL)
                        final String rootName = schema == null ? getBase().getName() : schema;
                        if (createMetadata != null) {
                            if (stmt == null)
                                stmt = conn.createStatement();
                            stmt.execute(createMetadata.asString(rootName));
                            tableNames.putAll(md, asList("TABLE", ""));
                        } else if (useCache) {
                            Log.get().warning(getCacheError(rootName));
                        }
                    }
                }
            }
        } finally {
            if (stmt != null)
                stmt.close();
        }

        this.schemas.clear();
        this.schemas.addAll(schemas);
        this.tableNames.clear();
        this.tableNames.putAll(tableNames);
    }

    public Set<String> getSchemas() {
        return this.schemas;
    }

    public Set<SQLName> getTablesNames() {
        return this.tableNames.keySet();
    }

    @Override
    protected void fillTables(final TablesMap newSchemas) throws SQLException {
        this.getBase().getDataSource().useConnection(new ConnectionHandlerNoSetup<Object, SQLException>() {
            @Override
            public Object handle(SQLDataSource ds) throws SQLException {
                _fillTables(newSchemas, ds.getConnection());
                return null;
            }
        });
    }

    protected void _fillTables(final TablesMap newSchemas, final Connection conn) throws SQLException {
        final boolean useCache = getBase().getDBSystemRoot().useCache();

        // for new tables, add ; for existing, refresh
        final DatabaseMetaData metaData = conn.getMetaData();
        // getColumns() only supports pattern (eg LIKE) so we must make multiple calls
        for (final String s : newSchemas.keySet()) {
            final Set<String> tablesToRefresh = newSchemas.get(s);
            assert tablesToRefresh != null : "Null should have been resolved in getNames()";
            if (tablesToRefresh.isEmpty())
                continue;
            final SQLSchema schema = getNewSchema(s);
            final ResultSet rs = metaData.getColumns(this.getBase().getMDName(), s, getTablePattern(IncludeExclude.getNormalized(tablesToRefresh)), null);

            // always fetch version to record in tables since we might decide to use cache later
            String schemaVers = SQLSchema.getVersion(getBase(), s);
            // shouldn't happen since we insert the version with SQLSchema.getCreateMetadata()
            if (schemaVers == null) {
                // don't create table here, but again it should already be created
                schemaVers = schema.updateVersion(false);
            }
            if (schemaVers == null && useCache)
                Log.get().warning("Cache won't be used for " + schema + " since there's no version");
            if (this.getTablesToRefresh(s) == null)
                schema.setFullyRefreshedVersion(schemaVers);

            // handle tables becoming empty (possible in pg)
            final Set<SQLName> tablesWithColumns = new HashSet<SQLName>();

            boolean hasNext = rs.next();
            while (hasNext) {
                final String schemaName = rs.getString("TABLE_SCHEM");
                // that way we can use the variable 'schema'
                assert CompareUtils.equals(schemaName, s);
                final String tableName = rs.getString("TABLE_NAME");
                tablesWithColumns.add(new SQLName(schemaName, tableName));

                final Boolean moved;
                if (tablesToRefresh.contains(tableName)) {
                    moved = schema.refreshTable(metaData, rs, schemaVers);
                } else {
                    moved = null;
                }
                hasNext = moved == null ? rs.next() : moved;
            }

            // tables with no column = all tables - tables with column
            for (final String t : tablesToRefresh) {
                if (!tablesWithColumns.contains(new SQLName(s, t))) {
                    // empty table with no db access
                    schema.getTable(t).emptyFields();
                }
            }
        }

        // type & comment
        for (final SQLName tName : getTablesNames()) {
            final SQLTable t = getNewSchema(tName.getItemLenient(-2)).getTable(tName.getName());
            final List<String> l = (List<String>) this.tableNames.getNonNull(tName);
            t.setType(l.get(0));
            t.setComment(l.get(1));
        }

        final SQLSystem system = getBase().getServer().getSQLSystem();
        // procedures
        final Map<String, Map<String, String>> proceduresBySchema = new HashMap<String, Map<String, String>>();
        for (final String s : newSchemas.keySet()) {
            final ResultSet rsProc = metaData.getProcedures(this.getBase().getMDName(), s, "%");
            while (rsProc.next()) {
                // to ignore case : pg.AbstractJdbc2DatabaseMetaData doesn't quote aliases
                // also to use getColumnLabel() : on h2, getColumnName() returns the base name
                final Map<String, Object> rsMap = new HashMap<String, Object>();
                final ResultSetMetaData rsmd = rsProc.getMetaData();
                final int cols = rsmd.getColumnCount();
                for (int i = 1; i <= cols; i++) {
                    rsMap.put(rsmd.getColumnLabel(i).toUpperCase(), rsProc.getObject(i));
                }

                final String schemaName = (String) rsMap.get("PROCEDURE_SCHEM");
                if (newSchemas.containsKey(schemaName)) {
                    final String procName = (String) rsMap.get("PROCEDURE_NAME");
                    Map<String, String> map = proceduresBySchema.get(schemaName);
                    if (map == null) {
                        map = new HashMap<String, String>();
                        proceduresBySchema.put(schemaName, map);
                    }
                    map.put(procName, null);
                }
            }
        }
        // try to find out more about those procedures
        if (proceduresBySchema.size() > 0) {
            final String sel = system.getSyntax().getFunctionQuery(getBase(), proceduresBySchema.keySet());
            if (sel != null) {
                // don't cache since we don't listen on system tables
                for (final Object o : (List) getBase().getDataSource().execute(sel, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false))) {
                    final Map m = (Map) o;
                    final String schemaName = (String) m.get("schema");
                    final String procName = (String) m.get("name");
                    assert proceduresBySchema.get(schemaName).containsKey(procName) : "metaData.getProcedures() hadn't found " + procName + " in " + schemaName;
                    proceduresBySchema.get(schemaName).put(procName, (String) m.get("src"));
                }
            }
            for (final Entry<String, Map<String, String>> e : proceduresBySchema.entrySet()) {
                getNewSchema(e.getKey()).putProcedures(e.getValue());
            }
        }

        // if no tables exist, there can be no triggers
        // (avoid a query and a special case since "in ()" is not valid)
        final TablesMap sansEmpty = TablesMap.create(newSchemas);
        sansEmpty.removeAllEmptyCollections();
        if (sansEmpty.size() > 0) {
            final ITransformer<Tuple2<String, String>, SQLTable> tableFinder = new ITransformer<Tuple2<String, String>, SQLTable>() {
                @Override
                public SQLTable transformChecked(Tuple2<String, String> input) {
                    return getNewTable(input.get0(), input.get1());
                }
            };
            new JDBCStructureSource.TriggerQueryExecutor(tableFinder).apply(getBase(), sansEmpty);
            new JDBCStructureSource.ColumnsQueryExecutor(tableFinder).apply(getBase(), sansEmpty);
            try {
                new JDBCStructureSource.ConstraintsExecutor(tableFinder).apply(getBase(), sansEmpty);
            } catch (QueryExn e1) {
                // constraints are not essentials, continue
                e1.printStackTrace();
                for (final SQLName tName : getTablesNames()) {
                    final SQLTable t = getNewSchema(tName.getItemLenient(-2)).getTable(tName.getName());
                    t.addConstraint(null);
                }
            }
        }
    }

    static final class TriggerQueryExecutor extends SystemQueryExecutor {
        public TriggerQueryExecutor(ITransformer<Tuple2<String, String>, SQLTable> tableFinder) {
            super(tableFinder);
        }

        @Override
        protected Object getQuery(SQLBase b, TablesMap tables) {
            try {
                return b.getServer().getSQLSystem().getSyntax().getTriggerQuery(b, tables);
            } catch (SQLException e) {
                return e;
            }
        }

        @Override
        protected void apply(SQLTable newTable, Map m) {
            newTable.addTrigger(m);
        }
    }

    static final class ColumnsQueryExecutor extends SystemQueryExecutor {
        public ColumnsQueryExecutor(ITransformer<Tuple2<String, String>, SQLTable> tableFinder) {
            super(tableFinder);
        }

        @Override
        protected String getQuery(SQLBase b, TablesMap tables) {
            return b.getServer().getSQLSystem().getSyntax().getColumnsQuery(b, tables);
        }

        @Override
        protected void apply(SQLTable newTable, Map m) {
            newTable.getField((String) m.get("COLUMN_NAME")).setColsFromInfoSchema(m);
        }
    }

    static final class ConstraintsExecutor extends SystemQueryExecutor {
        public ConstraintsExecutor(ITransformer<Tuple2<String, String>, SQLTable> tableFinder) {
            super(tableFinder);
        }

        @Override
        protected Object getQuery(SQLBase b, TablesMap tables) {
            try {
                return b.getServer().getSQLSystem().getSyntax().getConstraints(b, tables);
            } catch (Exception e) {
                return e;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void apply(SQLTable newTable, Map m) {
            newTable.addConstraint(m);
        }
    }

    @Override
    public void save() {
        for (final String schema : this.getSchemas()) {
            // save() catch all exceptions, this way we save as much schemas as possible
            // it's ok to not have all schemas saved, since XML doesn't rely on the file list
            // (it always gets the complete list from JDBC)
            this.getBase().save(schema);
        }
    }
}
