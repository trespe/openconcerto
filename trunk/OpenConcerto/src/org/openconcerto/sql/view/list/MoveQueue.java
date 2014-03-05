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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.OrderComparator;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.SleepingQueue;
import org.openconcerto.utils.Tuple2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public final class MoveQueue extends SleepingQueue {

    private final ITableModel tableModel;

    MoveQueue(ITableModel model) {
        super(MoveQueue.class.getSimpleName() + " on " + model);
        this.tableModel = model;
    }

    public void move(final List<? extends SQLRowAccessor> rows, final int inc) {
        if (inc == 0 || rows.size() == 0)
            return;

        final boolean after = inc > 0;
        final List<? extends SQLRowAccessor> l = new ArrayList<SQLRowAccessor>(rows);
        Collections.sort(l, after ? Collections.reverseOrder(OrderComparator.INSTANCE) : OrderComparator.INSTANCE);
        final int id = l.get(0).getID();
        this.put(new Runnable() {
            public void run() {
                final FutureTask<ListSQLLine> destID = new FutureTask<ListSQLLine>(new Callable<ListSQLLine>() {
                    @Override
                    public ListSQLLine call() {
                        return MoveQueue.this.tableModel.getDestLine(id, inc);
                    }
                });
                MoveQueue.this.tableModel.invokeLater(destID);
                try {
                    if (destID.get() != null) {
                        SQLUtils.executeAtomic(MoveQueue.this.tableModel.getTable().getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, Exception>() {
                            @Override
                            public Object handle(SQLDataSource ds) throws Exception {
                                moveQuick(l, after, destID.get().getRow().asRow());
                                return null;
                            }
                        });
                    }
                } catch (Exception e) {
                    throw ExceptionUtils.createExn(IllegalStateException.class, "move failed", e);
                }
            }
        });
    }

    // row index as returned by JTable.DropLocation.getRow()
    public void moveTo(final List<? extends SQLRowAccessor> rows, final int rowIndex) {
        if (rows.size() == 0)
            return;

        this.put(new Runnable() {
            public void run() {
                final FutureTask<Tuple2<Boolean, ListSQLLine>> future = new FutureTask<Tuple2<Boolean, ListSQLLine>>(new Callable<Tuple2<Boolean, ListSQLLine>>() {
                    @Override
                    public Tuple2<Boolean, ListSQLLine> call() {
                        assert rowIndex >= 0;
                        final int rowCount = MoveQueue.this.tableModel.getRowCount();
                        final boolean after = rowIndex >= rowCount;
                        final int index = after ? rowCount - 1 : rowIndex;
                        return Tuple2.create(after, MoveQueue.this.tableModel.getRow(index));
                    }
                });
                MoveQueue.this.tableModel.invokeLater(future);
                try {
                    final Tuple2<Boolean, ListSQLLine> line = future.get();
                    final boolean after = line.get0();
                    final List<? extends SQLRowAccessor> l = new ArrayList<SQLRowAccessor>(rows);
                    Collections.sort(l, after ? Collections.reverseOrder(OrderComparator.INSTANCE) : OrderComparator.INSTANCE);
                    SQLUtils.executeAtomic(MoveQueue.this.tableModel.getTable().getDBSystemRoot().getDataSource(), new ConnectionHandlerNoSetup<Object, Exception>() {
                        @Override
                        public Object handle(SQLDataSource ds) throws Exception {
                            moveQuick(l, after, line.get1().getRow().asRow());
                            return null;
                        }
                    });
                } catch (Exception e) {
                    throw ExceptionUtils.createExn(IllegalStateException.class, "move failed", e);
                }
            }
        });
    }

    final void moveQuick(final List<? extends SQLRowAccessor> srcRows, final boolean after, final SQLRow destRow) throws SQLException {
        final int rowCount = srcRows.size();
        // if only some rows are moved, update one by one (avoids refreshing the whole list)
        if (rowCount < 5 && rowCount < (this.tableModel.getRowCount() / 3)) {
            final SQLRowValues vals = new SQLRowValues(getTable());
            for (final SQLRowAccessor srcRow : srcRows) {
                assert srcRow.getTable() == vals.getTable();
                vals.setOrder(destRow, after).update(srcRow.getID());
            }
        } else {
            // update all rows at once and refresh the whole list
            moveAtOnce(getTable(), srcRows, rowCount, after, destRow);
        }
    }

    private SQLTable getTable() {
        return this.tableModel.getTable();
    }

    static public void moveAtOnce(final List<? extends SQLRowAccessor> srcRows, final boolean after, final SQLRow destRow) throws SQLException {
        final int rowCount = srcRows.size();
        if (rowCount == 0)
            return;

        final SQLTable t = srcRows.get(0).getTable();
        moveAtOnce(t, new ArrayList<SQLRowAccessor>(srcRows), rowCount, after, destRow);
    }

    // srcRows will be modified
    static private void moveAtOnce(final SQLTable t, final List<? extends SQLRowAccessor> srcRows, final int rowCount, final boolean after, final SQLRow destRow) throws SQLException {
        // ULP * 10 to give a little breathing room
        final BigDecimal minDistance = t.getOrderULP().scaleByPowerOfTen(1);
        final BigDecimal places = BigDecimal.valueOf(rowCount + 1);
        // the minimum room so that we can move all rows
        final BigDecimal room = minDistance.multiply(places);

        final BigDecimal destOrder = destRow.getOrder();
        final SQLRow nextRow = destRow.getRow(true);
        final BigDecimal inc;
        final boolean destRowReordered;
        if (nextRow == null) {
            // if destRow is the last row, we can choose whatever increment we want
            inc = ReOrder.DISTANCE;
            // but we need to move destRow if we want to add before it
            destRowReordered = false;
        } else {
            final BigDecimal nextOrder = nextRow.getOrder();
            assert nextOrder.compareTo(destOrder) > 0;
            final BigDecimal diff = nextOrder.subtract(destOrder);
            if (diff.compareTo(room) < 0) {
                // if there's not enough room, reorder to squeeze rows upwards
                // since we keep increasing count, we will eventually reorder all rows afterwards
                int count = 100;
                final int tableRowCount = t.getRowCount();
                boolean reordered = false;
                while (!reordered) {
                    // only push destRow upwards if we want to add before
                    reordered = ReOrder.create(t, destOrder, !after, count, destOrder.add(room)).exec();
                    if (!reordered && count > tableRowCount)
                        throw new IllegalStateException("Unable to reorder " + count + " rows in " + t);
                    count *= 10;
                }
                inc = minDistance;
                destRowReordered = true;
            } else {
                // truncate
                inc = DecimalUtils.round(diff.divide(places, DecimalUtils.HIGH_PRECISION), t.getOrderDecimalDigits(), RoundingMode.DOWN);
                destRowReordered = false;
            }
        }
        assert inc.compareTo(minDistance) >= 0;

        BigDecimal newOrder = destOrder;
        // by definition if we want to add after, destOrder should remain unchanged
        if (after) {
            newOrder = newOrder.add(inc);
        }
        final List<List<String>> newOrdersAndIDs = new ArrayList<List<String>>(rowCount);
        // we go from newOrder and up, we need to have the source rows in ascending order
        Collections.sort(srcRows, OrderComparator.INSTANCE);
        for (final SQLRowAccessor srcRow : srcRows) {
            newOrdersAndIDs.add(Arrays.asList(srcRow.getIDNumber().toString(), newOrder.toPlainString()));
            newOrder = newOrder.add(inc);
        }
        // move out before general request as most DB systems haven't got DEFERRABLE constraints
        if (!after && !destRowReordered) {
            final UpdateBuilder updateDestRow = new UpdateBuilder(t);
            updateDestRow.setObject(t.getOrderField(), newOrder);
            updateDestRow.setWhere(destRow.getWhere());
            t.getDBSystemRoot().getDataSource().execute(updateDestRow.asString());
        }

        final SQLSyntax syntax = SQLSyntax.get(t);
        final UpdateBuilder update = new UpdateBuilder(t);
        final String constantTableAlias = "newOrdersAndIDs";
        update.addVirtualJoin(syntax.getConstantTable(newOrdersAndIDs, constantTableAlias, Arrays.asList("ID", "newOrder")), constantTableAlias, true, "ID", t.getKey().getName());
        update.setFromVirtualJoinField(t.getOrderField().getName(), constantTableAlias, "newOrder");
        t.getDBSystemRoot().getDataSource().execute(update.asString());

        t.fireTableModified(SQLRow.NONEXISTANT_ID, Collections.singletonList(t.getOrderField().getName()));
    }
}
