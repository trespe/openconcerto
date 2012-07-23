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
import static java.util.Collections.singletonList;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.graph.Link;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.ITransformer;

import java.sql.SQLException;
import java.util.ArrayList;
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

    public MergeTable(DBSystemRoot b) {
        super(b);
        this.destTable = null;
    }

    public final void setDestTable(SQLTable destTable) {
        this.destTable = destTable;
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
            public String transformChecked(SQLField input) {
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

        final SQLSyntax syntax = t.getServer().getSQLSystem().getSyntax();
        final Set<SQLTable> toRefresh = new HashSet<SQLTable>();
        SQLUtils.executeAtomic(t.getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, SQLException>() {
            @Override
            public Object handle(SQLDataSource ds) throws SQLException {
                // copy all data of t into destTable
                final List<Number> insertedIDs = SQLRowValues.insertIDs(MergeTable.this.destTable, fields + " " + sel.asString());
                // handle undefined
                insertedIDs.add(0, MergeTable.this.destTable.getUndefinedIDNumber());
                final List<Number> oldIDs = ds.executeCol(selOldIDs.asString());
                final Number oldUndef = t.getUndefinedIDNumber();
                if (oldUndef == null)
                    // MAYBE should do a second update with "where %n is NULL"
                    throw new UnsupportedOperationException("old undef is null, not yet supported");
                oldIDs.add(0, oldUndef);
                final int size = insertedIDs.size();
                if (size != oldIDs.size())
                    throw new IllegalStateException("size mismatch: " + size + " != " + oldIDs.size());

                // load the mapping in the db
                final SQLName mapName = new SQLName("MAP");
                final SQLCreateTable createTable = new SQLCreateTable(t.getDBRoot(), mapName.getFirst());
                createTable.setPlain(true);
                createTable.setTemporary(true);
                createTable.addColumn("OLD_ID", syntax.getIDType());
                createTable.addColumn("NEW_ID", syntax.getIDType());
                ds.execute(createTable.asString());
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    sb.append("(" + oldIDs.get(i) + ", " + insertedIDs.get(i) + ")");
                    if (i < size - 1)
                        sb.append(",");
                }
                ds.execute(t.getBase().quote("INSERT INTO %i(%i, %i) VALUES" + sb, mapName, "OLD_ID", "NEW_ID"));

                // for each link to t, point it to destTable
                final Set<Link> referentLinks = t.getDBSystemRoot().getGraph().getReferentLinks(t);
                for (final Link refLink : referentLinks) {
                    final SQLField refKey = refLink.getLabel();
                    final SQLTable refTable = refKey.getTable();

                    // drop constraint
                    // if no name, we can assume that there's no actual constraint
                    // just that the fwk has infered the link, so we don't need to drop anything
                    // if we're mistaken "drop table" will fail (as should the UPDATE) and the
                    // transaction will be rollbacked
                    if (refLink.getName() != null) {
                        final AlterTable dropFK = new AlterTable(refTable);
                        dropFK.dropForeignConstraint(refLink.getName());
                        ds.execute(dropFK.asString());
                    }

                    // update the field using the map
                    final String start;
                    if (t.getServer().getSQLSystem() == SQLSystem.MYSQL)
                        start = t.getBase().quote("UPDATE %f, %i set %n = %i", refTable, mapName, refKey, new SQLName("MAP", "NEW_ID"));
                    else
                        start = t.getBase().quote("UPDATE %f set %n = %i FROM %i", refTable, refKey, new SQLName("MAP", "NEW_ID"), mapName);
                    ds.execute(start + t.getBase().quote(" where %n = %i", refKey, new SQLName("MAP", "OLD_ID")));

                    // re-add constraint
                    final AlterTable addFK = new AlterTable(refTable);
                    // don't create an index : if there was one it's still there, if there wasn't
                    // don't alter the table silently (use AddFK if you want that)
                    addFK.addForeignConstraint(singletonList(refKey.getName()), MergeTable.this.destTable.getContextualSQLName(refTable), false, MergeTable.this.destTable.getPKsNames());
                    ds.execute(addFK.asString());

                    toRefresh.add(refTable);
                }

                // all data has been copied, and every link removed
                // we can now safely drop t
                ds.execute(t.getBase().quote("DROP TABLE %f", t));

                toRefresh.add(t);

                return null;
            }
        });
        for (final SQLTable table : toRefresh) {
            table.fetchFields();
        }
    }

    private final SQLSelect createSelect(final SQLTable t) {
        final SQLSelect sel = new SQLSelect(t.getBase(), true);
        // undefined is not copied
        sel.setExcludeUndefined(true);
        // necessary so that ids are returned in the same order every time
        sel.addFieldOrder(t.getOrderField());
        return sel;
    }

}
