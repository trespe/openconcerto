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
 
 package org.openconcerto.sql.changer.convert;

import static org.openconcerto.utils.CollectionUtils.substract;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.AliasedTable;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSchema;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.DatabaseGraph;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ChangeTable.FCSpec;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Merge the passed table into the {@link #setDestTable(SQLTable) destination table}. Ie it copies
 * all the data to the destination, then update each referencing link, and finally drop the source
 * table.
 * 
 * @author Sylvain
 */
public class MergeTable extends Changer<SQLTable> {

    public static final String MERGE_DEST_TABLE = "merge.destTable";

    private SQLTable destTable;
    private final Set<List<String>> forceFF;

    public MergeTable(final DBSystemRoot b) {
        super(b);
        this.destTable = null;
        this.forceFF = new HashSet<List<String>>();
    }

    public final void setDestTable(final SQLTable destTable) {
        this.destTable = destTable;
    }

    public final void forceFF(String ff) {
        this.forceFF(Collections.singletonList(ff));
    }

    public final void forceFF(List<String> cols) {
        this.forceFF.add(cols);
    }

    @Override
    protected Class<? extends DBStructureItem<?>> getMaxLevel() {
        // avoid deleting all tables
        return SQLTable.class;
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        final String prop = System.getProperty(MERGE_DEST_TABLE);
        if (prop == null)
            throw new IllegalStateException("the system property " + MERGE_DEST_TABLE + " is not defined");
        this.setDestTable(getSystemRoot().getDesc(prop, SQLTable.class));
    }

    @Override
    protected void changeImpl(final SQLTable t) throws SQLException {
        // print tables right away, so we don't need to repeat them in error msg
        this.getStream().println("merging " + t.getSQLName() + " into " + this.destTable.getSQLName() + "... ");

        if (!this.destTable.getChildrenNames().containsAll(t.getChildrenNames()))
            throw new IllegalArgumentException(this.destTable.getSQLName() + " lacks " + substract(t.getChildrenNames(), this.destTable.getChildrenNames()));
        // check that t is compatible with destTable
        final String noLink = t.equalsChildrenNoLink(this.destTable, null);
        if (noLink != null)
            throw new IllegalArgumentException(noLink);

        // fields to be copied to the destTable
        final List<SQLField> fieldsNoPKNoOrder = new ArrayList<SQLField>(t.getFields());
        // the primary key will be automatically generated
        fieldsNoPKNoOrder.remove(t.getKey());
        // ORDER will be at the end (we will offset it)
        final SQLField orderF = t.getOrderField();
        fieldsNoPKNoOrder.remove(orderF);
        fieldsNoPKNoOrder.add(orderF);
        final String fields = "(" + CollectionUtils.join(fieldsNoPKNoOrder, ",", new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(final SQLField input) {
                return SQLBase.quoteIdentifier(input.getName());
            }
        }) + ")";
        final SQLSelect sel = createSelect(t);
        // ORDER has to be computed
        fieldsNoPKNoOrder.remove(fieldsNoPKNoOrder.size() - 1);
        sel.addAllSelect(fieldsNoPKNoOrder);
        // offset by max of the destination table to avoid conflicts
        sel.addRawSelect(t.getBase().quote("%n + ( SELECT MAX(%n)+100 FROM %f ) ", orderF, this.destTable.getOrderField(), this.destTable), null);

        final SQLSelect selOldIDs = createSelect(t);
        selOldIDs.addSelect(t.getKey());
        final List<Number> oldIDs = getDS().executeCol(selOldIDs.asString());

        // if we copy no rows, no need to check constraints
        final boolean noRowsToMerge = oldIDs.size() == 0;
        final DatabaseGraph graph = t.getDBSystemRoot().getGraph();
        // check that transferred data from t still points to the same rows
        final Set<Link> selfRefLinks = new HashSet<Link>();
        for (final Link l : graph.getForeignLinks(t)) {
            final Link destLink = graph.getForeignLink(this.destTable, l.getCols());
            if (destLink == null)
                throw new IllegalStateException("No link for " + l.getCols() + " in " + this.destTable.getSQL());
            final SQLTable destTableTarget = destLink.getTarget();
            if (destTableTarget == destLink.getSource()) {
                selfRefLinks.add(destLink);
            } else if (destTableTarget != l.getTarget()) {
                final String s = "Not pointing to the same table for " + l + " " + destTableTarget.getSQL() + " != " + l.getTarget().getSQL();
                final List<String> reasonsToContinue = new ArrayList<String>();
                if (noRowsToMerge)
                    reasonsToContinue.add("but source table is empty");
                if (l.getTarget().getRowCount(false) == 0)
                    reasonsToContinue.add("but the link target is empty");
                if (this.forceFF.contains(l.getCols()))
                    reasonsToContinue.add("but link is forced");

                if (reasonsToContinue.size() == 0)
                    throw new IllegalStateException(s);

                getStream().println("WARNING: " + s);
                getStream().println(CollectionUtils.join(reasonsToContinue, ";\n"));
            }
        }

        final SQLSyntax syntax = t.getServer().getSQLSystem().getSyntax();
        final Set<SQLTable> toRefresh = new HashSet<SQLTable>();
        SQLUtils.executeAtomic(t.getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, SQLException>() {
            @Override
            public Object handle(final SQLDataSource ds) throws SQLException {
                // drop self reference links before inserting
                final AlterTable dropSelfFK = new AlterTable(MergeTable.this.destTable);
                for (final Link selfRef : selfRefLinks) {
                    dropSelfFK.dropForeignConstraint(selfRef.getName());
                }
                if (!dropSelfFK.isEmpty())
                    ds.execute(dropSelfFK.asString());

                // copy all data of t into destTable
                final List<Number> insertedIDs = SQLRowValues.insertIDs(MergeTable.this.destTable, fields + " " + sel.asString());
                // handle undefined
                insertedIDs.add(0, MergeTable.this.destTable.getUndefinedIDNumber());
                oldIDs.add(0, t.getUndefinedIDNumber());
                final int size = insertedIDs.size();
                if (size != oldIDs.size())
                    throw new IllegalStateException("size mismatch: " + size + " != " + oldIDs.size());

                // load the mapping in the db
                final SQLName mapName = new SQLName(t.getDBRoot().getName(), "MAP_" + MergeTable.class.getSimpleName() + System.currentTimeMillis());
                final SQLCreateTable createTable = new SQLCreateTable(t.getDBRoot(), mapName.getName());
                createTable.setPlain(true);
                // cannot use temporary table since we need a SQLTable for UpdateBuilder
                createTable.addColumn("OLD_ID", syntax.getIDType());
                createTable.addColumn("NEW_ID", syntax.getIDType());
                ds.execute(createTable.asString());
                final SQLTable mapT = t.getDBRoot().refetchTable(mapName.getName());

                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    sb.append("(" + oldIDs.get(i) + ", " + insertedIDs.get(i) + ")");
                    if (i < size - 1)
                        sb.append(",");
                }
                ds.execute(t.getBase().quote("INSERT INTO %i(%i, %i) VALUES" + sb, mapName, "OLD_ID", "NEW_ID"));

                // for each link to t, point it to destTable
                for (final Link selfRef : selfRefLinks) {
                    toRefresh.add(updateLink(selfRef, mapT));
                }
                for (final Link refLink : graph.getReferentLinks(t)) {
                    // self links are already taken care of
                    // (we don't want to update t)
                    if (refLink.getSource() != t)
                        toRefresh.add(updateLink(refLink, mapT));
                }

                // all data has been copied, and every link removed
                // we can now safely drop t
                ds.execute(t.getBase().quote("DROP TABLE %f", t));
                ds.execute("DROP TABLE " + mapName.quote());

                toRefresh.add(t);
                toRefresh.add(mapT);

                return null;
            }

            public SQLTable updateLink(final Link refLink, final SQLTable mapT) {
                final SQLField refKey = refLink.getLabel();
                final SQLTable refTable = refKey.getTable();
                final SQLDataSource ds = refTable.getDBSystemRoot().getDataSource();
                final boolean selfLink = refLink.getSource() == refLink.getTarget();
                assert refTable != t;

                // drop constraint
                // * if selfLink, already dropped
                // * if no name, we can assume that there's no actual constraint
                // just that the fwk has inferred the link, so we don't need to drop anything
                // if we're mistaken "drop table" will fail (as should the UPDATE) and the
                // transaction will be rollbacked
                if (!selfLink && refLink.getName() != null) {
                    final AlterTable dropFK = new AlterTable(refTable);
                    dropFK.dropForeignConstraint(refLink.getName());
                    ds.execute(dropFK.asString());
                }

                // update the field using the map
                final UpdateBuilder update = new UpdateBuilder(refTable);
                final AliasedTable alias1 = new AliasedTable(mapT, "m");
                update.addTable(alias1);
                update.set(refKey.getName(), alias1.getField("NEW_ID").getFieldRef());
                update.setWhere(new Where(refKey, Where.NULL_IS_DATA_EQ, alias1.getField("OLD_ID")));
                if (selfLink) {
                    // only update new rows (old rows can have the same IDs but they point to old
                    // foreign rows, they must not be updated)
                    final AliasedTable onlyNew = new AliasedTable(mapT, "onlyNew");
                    update.addTable(onlyNew);
                    // we added the undefined to NEW_ID, but it wasn't copied from t so don't update
                    final Where w = new Where(refTable.getKey(), Where.NULL_IS_DATA_EQ, onlyNew.getField("NEW_ID")).and(new Where(refTable.getKey(), Where.NULL_IS_DATA_NEQ, refTable
                            .getUndefinedIDNumber()));
                    update.setWhere(update.getWhere().and(w));
                }
                ds.execute(update.asString());

                // re-add constraint
                final AlterTable addFK = new AlterTable(refTable);
                // don't create an index : if there was one it's still there, if there wasn't
                // don't alter the table silently (use AddFK if you want that)
                addFK.addForeignConstraint(FCSpec.createFromLink(refLink, MergeTable.this.destTable), false);
                ds.execute(addFK.asString());
                return refTable;
            }
        });
        final TablesMap tables = new TablesMap();
        final Set<SQLSchema> schemas = new HashSet<SQLSchema>();
        for (final SQLTable table : toRefresh) {
            tables.add(table.getDBRoot().getName(), table.getName());
            schemas.add(table.getSchema());
        }
        t.getDBSystemRoot().refresh(tables, false);
        for (final SQLSchema schema : schemas) {
            schema.updateVersion();
        }
    }

    private final SQLSelect createSelect(final SQLTable t) {
        final SQLSelect sel = new SQLSelect(true);
        // undefined is not copied
        sel.setExcludeUndefined(true);
        // necessary so that ids are returned in the same order every time
        sel.addFieldOrder(t.getOrderField());
        return sel;
    }

}
