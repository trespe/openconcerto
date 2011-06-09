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
import org.openconcerto.sql.model.SystemQueryExecutor.QueryExn;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.dbutils.BasicRowProcessor;

public class JDBCStructureSource extends StructureSource<SQLException> {

    private final Set<String> schemas;
    private final CollectionMap<SQLName, String> tableNames;

    public JDBCStructureSource(SQLBase b, Set<String> scope, Map<String, SQLSchema> newStruct) {
        super(b, scope, newStruct);
        this.schemas = new HashSet<String>();
        this.tableNames = new CollectionMap<SQLName, String>(new ArrayList<String>(2));
        // if we can't access the metadata directly from the base, obviously the base is not ok
        this.setPreVerify(false);
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
            final ResultSet rs = metaData.getTables(this.getBase().getMDName(), s, "%", new String[] { "TABLE", "SYSTEM TABLE", "VIEW" });
            while (rs.next()) {
                final String tableType = rs.getString("TABLE_TYPE");
                final String schemaName = rs.getString("TABLE_SCHEM");
                if (tableType.equals("SYSTEM TABLE"))
                    schemas.remove(schemaName);
                else {
                    final String tableName = rs.getString("TABLE_NAME");
                    // MySQL needs this.addConnectionProperty("useInformationSchema", "true");
                    // but the time goes from 3.5s to 20s
                    tableNames.putAll(new SQLName(schemaName, tableName), asList(rs.getString("TABLE_TYPE"), rs.getString("REMARKS")));
                }
            }
        }

        // keep only tables in remaining schemas
        final Iterator<SQLName> iter = getTablesNames().iterator();
        while (iter.hasNext()) {
            final SQLName tableName = iter.next();
            if (!schemas.contains(tableName.getItemLenient(-2)))
                iter.remove();
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

    protected void fillTables(final Set<String> newSchemas) throws SQLException {
        this.getBase().getDataSource().useConnection(new ConnectionHandlerNoSetup<Object, SQLException>() {
            @Override
            public Object handle(SQLDataSource ds) throws SQLException {
                _fillTables(newSchemas, ds.getConnection());
                return null;
            }
        });
    }

    protected void _fillTables(final Set<String> newSchemas, final Connection conn) throws SQLException {
        // for new tables, add ; for existing, refresh
        final DatabaseMetaData metaData = conn.getMetaData();
        // getColumns() only supports pattern (eg LIKE) so we must make multiple calls
        for (final String s : newSchemas) {
            final ResultSet rs = metaData.getColumns(this.getBase().getMDName(), s, "%", null);

            // handle tables becoming empty (possible in pg)
            final Set<SQLName> tablesWithColumns = new HashSet<SQLName>();

            boolean hasNext = rs.next();
            while (hasNext) {
                final String schemaName = rs.getString("TABLE_SCHEM");
                final String tableName = rs.getString("TABLE_NAME");
                tablesWithColumns.add(new SQLName(schemaName, tableName));

                final Boolean moved;
                if (newSchemas.contains(schemaName)) {
                    final SQLSchema schema = getNewSchema(schemaName);
                    moved = schema.refreshTable(metaData, rs);
                } else {
                    moved = null;
                }
                hasNext = moved == null ? rs.next() : moved;
            }

            // tables with no column = all tables - tables with column
            final SQLSchema schema = getNewSchema(s);
            for (final String t : schema.getTableNames()) {
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
        final CollectionMap<String, String> proceduresBySchema = new CollectionMap<String, String>();
        for (final String s : newSchemas) {
            final ResultSet rsProc = metaData.getProcedures(this.getBase().getMDName(), s, "%");
            while (rsProc.next()) {
                // to ignore case : pg.AbstractJdbc2DatabaseMetaData doesn't quote aliases
                final Map map = BasicRowProcessor.instance().toMap(rsProc);
                final String schemaName = (String) map.get("PROCEDURE_SCHEM");
                if (newSchemas.contains(schemaName)) {
                    final String procName = (String) map.get("PROCEDURE_NAME");
                    proceduresBySchema.put(schemaName, procName);
                    getNewSchema(schemaName).addProcedure(procName);
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
                    final SQLSchema newSchema = getNewSchema((String) m.get("schema"));
                    if (newSchema != null)
                        newSchema.setProcedureSource((String) m.get("name"), (String) m.get("src"));
                }
            }
        }

        // if no schemas exist, there can be no triggers
        // (avoid a query and a special case since "in ()" is not valid)
        if (newSchemas.size() > 0) {
            final ITransformer<Tuple2<String, String>, SQLTable> tableFinder = new ITransformer<Tuple2<String, String>, SQLTable>() {
                @Override
                public SQLTable transformChecked(Tuple2<String, String> input) {
                    return getNewTable(input.get0(), input.get1());
                }
            };
            new JDBCStructureSource.TriggerQueryExecutor(tableFinder).apply(getBase(), newSchemas);
            new JDBCStructureSource.ColumnsQueryExecutor(tableFinder).apply(getBase(), newSchemas);
            try {
                new JDBCStructureSource.ConstraintsExecutor(tableFinder).apply(getBase(), newSchemas);
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
        protected Object getQuery(SQLBase b, Set<String> newSchemas, Set<String> arg2) {
            try {
                return b.getServer().getSQLSystem().getSyntax().getTriggerQuery(b, newSchemas, arg2);
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
        protected String getQuery(SQLBase b, Set<String> newSchemas, Set<String> arg2) {
            return b.getServer().getSQLSystem().getSyntax().getColumnsQuery(b, newSchemas, arg2);
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
        protected Object getQuery(SQLBase b, Set<String> newSchemas, Set<String> arg2) {
            try {
                return b.getServer().getSQLSystem().getSyntax().getConstraints(b, newSchemas, arg2);
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
