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

import org.openconcerto.sql.model.graph.SQLKey;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.view.list.SQLTableModelSource;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SQLInjector {

    private final SQLTable tableSrc, tableDest;
    private final ArrayList<SQLField> from = new ArrayList<SQLField>();
    private final ArrayList<SQLField> to = new ArrayList<SQLField>();
    private final Map<SQLField, Object> values = new HashMap<SQLField, Object>();
    private final static Map<DBRoot, Map<SQLTable, Map<SQLTable, SQLInjector>>> allRegisteredInjectors = new HashMap<DBRoot, Map<SQLTable, Map<SQLTable, SQLInjector>>>();

    private boolean storeTransfer;
    // maps of injectors that store transfer
    private static Map<DBRoot, Map<SQLTable, Map<SQLTable, SQLInjector>>> injectors = new HashMap<DBRoot, Map<SQLTable, Map<SQLTable, SQLInjector>>>();

    public SQLInjector(final DBRoot r, final String src, final String dest, boolean storeTransfer) {
        this(r.findTable(src), r.findTable(dest), storeTransfer);
    }

    public SQLInjector(SQLTable src, SQLTable dest, boolean storeTransfer) {
        this.tableDest = dest;
        this.tableSrc = src;
        this.storeTransfer = storeTransfer;
        final DBRoot dbRoot = src.getDBRoot();
        Map<SQLTable, Map<SQLTable, SQLInjector>> inj = allRegisteredInjectors.get(dbRoot);
        if (inj == null) {
            inj = new HashMap<SQLTable, Map<SQLTable, SQLInjector>>();
            allRegisteredInjectors.put(dbRoot, inj);
        }
        Map<SQLTable, SQLInjector> srcs = inj.get(src);
        if (srcs == null) {
            srcs = new HashMap<SQLTable, SQLInjector>();
            inj.put(src, srcs);
        }
        srcs.put(dest, this);

        if (storeTransfer) {
            // Register only SQLInjector that store transfer
            inj = injectors.get(dbRoot);
            if (inj == null) {
                inj = new HashMap<SQLTable, Map<SQLTable, SQLInjector>>();
                injectors.put(dbRoot, inj);
            }
            srcs = inj.get(src);
            if (srcs == null) {
                srcs = new HashMap<SQLTable, SQLInjector>();
                inj.put(src, srcs);
            }
            srcs.put(dest, this);
        }
    }

    public synchronized SQLRowValues createRowValuesFrom(int idSrc) {
        final List<SQLRowAccessor> srcRows = new ArrayList<SQLRowAccessor>(1);
        srcRows.add(new SQLImmutableRowValues(getSource().getRow(idSrc).asRowValues()));
        return createRowValuesFrom(srcRows);
    }

    public synchronized SQLRowValues createRowValuesFrom(final SQLRow srcRow) {
        final SQLRowValues rowVals = new SQLRowValues(getDestination());
        if (!srcRow.getTable().equals(getSource()))
            throw new IllegalArgumentException("Row not from source table : " + srcRow);
        merge(srcRow, rowVals);
        return rowVals;
    }

    public synchronized SQLRowValues createRowValuesFrom(final List<? extends SQLRowAccessor> srcRows) {
        final SQLRowValues rowVals = new SQLRowValues(getDestination());
        for (SQLRowAccessor srcRow : srcRows) {
            if (!srcRow.getTable().equals(getSource()))
                throw new IllegalArgumentException("Row not from source table : " + srcRow);
            merge(srcRow, rowVals);
        }
        return rowVals;
    }

    public void commitTransfert(final List<? extends SQLRowAccessor> srcRows, int destId) throws SQLException {

        if (storeTransfer) {
            System.err.println("SQLInjector.commitTransfert() : transfert from " + this.getSource().getName() + " to " + this.getDestination().getName());
            // Transfert
            final SQLTable tableTransfert = getSource().getDBRoot().getTable(getTableTranferName());
            if (tableTransfert == null) {
                throw new IllegalStateException("No table transfer for " + getSource().getName());
            }

            for (SQLRowAccessor srcRow : srcRows) {

                final SQLRowValues rowTransfer = new SQLRowValues(tableTransfert);

                final Set<SQLField> foreignKeysSrc = tableTransfert.getForeignKeys(getSource());
                final Set<SQLField> foreignKeysDest = tableTransfert.getForeignKeys(getDestination());
                if (foreignKeysSrc.isEmpty()) {
                    throw new IllegalStateException("No foreign (src) to " + getSource().getName() + " in " + tableTransfert.getName());
                }
                if (foreignKeysDest.isEmpty()) {
                    throw new IllegalStateException("No foreign (dest) to " + getDestination().getName() + " in " + tableTransfert.getName());
                }
                rowTransfer.put(foreignKeysSrc.iterator().next().getName(), srcRow.getIDNumber());
                rowTransfer.put(foreignKeysDest.iterator().next().getName(), destId);
                // TODO: commit in one shot
                rowTransfer.commit();

            }
        }

    }

    private String getTableTranferName() {
        return "TR_" + getSource().getName();
    }

    protected void merge(SQLRowAccessor srcRow, SQLRowValues rowVals) {
        for (SQLField field : this.values.keySet()) {
            rowVals.put(field.getName(), this.values.get(field));
        }
        final SQLSystem dbSystem = srcRow.getTable().getDBSystemRoot().getServer().getSQLSystem();
        final int size = getFrom().size();
        for (int i = 0; i < size; i++) {

            final SQLField sqlFieldFrom = getFrom().get(i);
            final SQLField sqlFieldTo = getTo().get(i);
            final Object o = srcRow.getObject(sqlFieldFrom.getName());

            // Probleme avec H2 Primary Key en Long et foreignKey en Int
            if (dbSystem == SQLSystem.H2 && sqlFieldFrom.getType().getJavaType() == Long.class && sqlFieldTo.getType().getJavaType() == Integer.class) {
                merge(sqlFieldTo, ((Long) o).intValue(), rowVals);
            } else {
                merge(sqlFieldTo, o, rowVals);
            }
        }
    }

    protected void merge(SQLField field, Object value, SQLRowValues rowVals) {
        rowVals.put(field.getName(), value);
    }

    public synchronized SQLRow insertFrom(final SQLRowAccessor srcRow) throws SQLException {
        return createRowValuesFrom(Arrays.asList(srcRow)).insert();
    }

    // TODO gettable()..getName()..equalsIgnoreCase( by .getTable().equals(
    /**
     * mettre une valeur par défaut pour un champ donné
     * 
     * @param fieldDest
     * @param defaultValue
     */
    protected synchronized final void mapDefaultValues(SQLField fieldDest, Object defaultValue) {
        if (fieldDest.getTable().getName().equalsIgnoreCase(this.tableDest.getName())) {
            this.values.put(fieldDest, defaultValue);
        } else {
            throw new IllegalArgumentException("SQLField " + fieldDest + " is not a field of table " + this.tableDest);
        }
    }

    protected synchronized final void map(SQLField from, SQLField to) throws IllegalArgumentException {
        // Verification de la validité des SQLField
        if (!from.getTable().getName().equalsIgnoreCase(this.tableSrc.getName())) {
            throw new IllegalArgumentException("SQLField " + from + " is not a field of table " + this.tableSrc);
        } else {
            if (!to.getTable().getName().equalsIgnoreCase(this.tableDest.getName())) {
                throw new IllegalArgumentException("SQLField " + to + " is not a field of table " + this.tableDest);
            }
        }

        int index = this.from.indexOf(from);
        if (index > 0) {
            this.to.set(index, to);
        } else {
            this.from.add(from);
            this.to.add(to);
        }
    }

    protected synchronized final void remove(SQLField from, SQLField to) throws IllegalArgumentException {
        // Verification de la validité des SQLField
        if (!from.getTable().getName().equalsIgnoreCase(this.tableSrc.getName())) {
            throw new IllegalArgumentException("SQLField " + from + " is not a field of table " + this.tableSrc);
        } else {
            if (!to.getTable().getName().equalsIgnoreCase(this.tableDest.getName())) {
                throw new IllegalArgumentException("SQLField " + to + " is not a field of table " + this.tableDest);
            }
        }

        int index = this.from.indexOf(from);
        if (this.to.get(index).getName().equalsIgnoreCase(to.getName())) {
            this.to.remove(to);
            this.from.remove(from);
        }
    }

    /**
     * Créer l'association entre les champs portant le nom dans les deux tables
     * 
     */
    public synchronized void createDefaultMap() {
        for (SQLField field : this.tableSrc.getContentFields()) {

            if (this.tableDest.contains(field.getName())) {
                map(field, this.tableDest.getField(field.getName()));
            }
        }
    }

    public synchronized ArrayList<SQLField> getFrom() {
        return this.from;
    }

    public synchronized ArrayList<SQLField> getTo() {
        return this.to;
    }

    /**
     * Creer un SQLInjector par défaut si aucun n'est déja défini
     * 
     * @param src
     * @param dest
     * @return un SQLInjector par défaut si aucun n'est déja défini
     */
    public static synchronized SQLInjector getInjector(SQLTable src, SQLTable dest) {
        SQLInjector injector = getRegistrereddInjector(src, dest);
        if (injector == null) {
            injector = createDefaultInjector(src, dest);
        }
        return injector;
    }

    public static synchronized SQLInjector getRegistrereddInjector(SQLTable src, SQLTable dest) {
        final Map<SQLTable, Map<SQLTable, SQLInjector>> map = allRegisteredInjectors.get(src.getDBRoot());
        if (map == null) {
            return null;
        }
        Map<SQLTable, SQLInjector> m = map.get(src);
        if (m != null) {
            return m.get(dest);
        }
        return null;
    }

    private static synchronized SQLInjector createDefaultInjector(SQLTable src, SQLTable dest) {
        System.err.println("No SQLInjector defined for " + src + " , " + dest + ". SQLInjector created automatically.");
        SQLInjector injector = new SQLInjector(src, dest, false);
        injector.createDefaultMap();
        return injector;
    }

    public synchronized SQLTable getDestination() {
        return this.tableDest;
    }

    public synchronized SQLTable getSource() {
        return this.tableSrc;
    }

    public synchronized static void createTransferTables(DBRoot root) throws SQLException {
        Map<SQLTable, Map<SQLTable, SQLInjector>> map = injectors.get(root);
        if (root == null) {
            System.err.println("No SQLInjector for root " + root);
            return;
        }

        final Set<SQLTable> srcTables = map.keySet();
        if (srcTables.isEmpty()) {
            System.err.println("No SQLInjector for root " + root);
            return;
        }

        final List<SQLCreateTable> createTablesQueries = new ArrayList<SQLCreateTable>();
        // Create table if needed
        for (SQLTable sqlTable : srcTables) {
            final String trTableName = "TR_" + sqlTable.getName();
            if (root.getTable(trTableName) == null) {
                final SQLCreateTable createTable = new SQLCreateTable(root, trTableName);
                createTable.setPlain(false);
                // createTable.addColumn(SQLSyntax.ID_NAME,
                // createTable.getSyntax().getPrimaryIDDefinition());
                createTable.addForeignColumn(SQLKey.PREFIX + sqlTable.getName(), sqlTable);
                createTablesQueries.add(createTable);
            }
        }
        if (createTablesQueries.size() > 0) {
            root.createTables(createTablesQueries);
        }

        // Create transfer fields if needed
        final List<AlterTable> alterTablesQueries = new ArrayList<AlterTable>();
        final TablesMap toRefresh = new TablesMap();
        for (SQLTable srcTable : srcTables) {
            final String trTableName = "TR_" + srcTable.getName();
            final SQLTable transfertTable = root.getTable(trTableName);
            final AlterTable alter = new AlterTable(transfertTable);
            final Set<SQLTable> destTables = map.get(srcTable).keySet();
            for (SQLTable destTable : destTables) {
                final String fk = SQLKey.PREFIX + destTable.getName();
                if (!transfertTable.contains(fk)) {
                    alter.addForeignColumn(fk, destTable);
                }
            }
            if (!alter.isEmpty()) {
                alterTablesQueries.add(alter);
                toRefresh.add(alter.getRootName(), alter.getName());
            }
        }
        for (final String q : ChangeTable.cat(alterTablesQueries)) {
            root.getDBSystemRoot().getDataSource().execute(q);
        }
        root.getSchema().updateVersion();
        root.getDBSystemRoot().refresh(toRefresh, false);

    }

    public void setOnlyTransfered(SQLTableModelSourceOnline tableSource) {
        // needed for distinct
        tableSource.getReq().setLockSelect(false);

        tableSource.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {

                final SQLTable tableTR = getSource().getTable(getTableTranferName());
                // FIXME: preprocess TR_ .. content before join : group by id_src
                final SQLSelectJoin j = input.addBackwardJoin("INNER", null, tableTR.getForeignKeys(getSource()).iterator().next(), null);
                j.setWhere(new Where(tableTR.getForeignKeys(getDestination()).iterator().next(), "!=", getDestination().getUndefinedID()));
                input.setDistinct(true);

                System.err.println(input.asString());
                return input;
            }
        });
    }

    public void setOnlyNotTransfered(SQLTableModelSourceOnline tableSource) {
        tableSource.getReq().setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                final SQLTable tableTR = getSource().getTable(getTableTranferName());

                final Where w = new Where(tableTR.getForeignKeys(getSource()).iterator().next(), "=", input.getAlias(getSource().getKey()));
                input.addJoin("LEFT", tableTR, w);
                final Where w2 = new Where(tableTR.getForeignKeys(getDestination()).iterator().next(), "IS", (Object) null);
                input.setWhere(w2);

                System.err.println(input.asString());
                return input;
            }
        });
    }

    /**
     * register manually a transfer, use with caution
     * 
     * @throws SQLException
     * */
    public void addTransfert(int idFrom, int idTo) throws SQLException {
        System.err.println("SQLInjector.addTransfert() " + idFrom + " -> " + idTo);
        final SQLTable tableTransfert = getSource().getTable(getTableTranferName());
        final SQLRowValues rowTransfer = new SQLRowValues(tableTransfert);

        final Set<SQLField> foreignKeysSrc = tableTransfert.getForeignKeys(getSource());
        final Set<SQLField> foreignKeysDest = tableTransfert.getForeignKeys(getDestination());

        rowTransfer.put(foreignKeysSrc.iterator().next().getName(), idFrom);
        rowTransfer.put(foreignKeysDest.iterator().next().getName(), idTo);

        rowTransfer.commit();

    }

}
