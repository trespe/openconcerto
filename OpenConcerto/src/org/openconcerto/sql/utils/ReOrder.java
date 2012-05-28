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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

/**
 * Reorder some or all rows of a table.
 * 
 * @author Sylvain
 */
public abstract class ReOrder {

    // must be zero so that we can work on negative numbers without breaking the unique constraint
    public static final BigDecimal MIN_ORDER = BigDecimal.ZERO;
    // preferred distance
    public static final BigDecimal DISTANCE = BigDecimal.ONE;

    static public ReOrder create(final SQLTable t) {
        return create(t, ALL);
    }

    static public ReOrder create(final SQLTable t, final int first, final int count) {
        return create(t, new Some(t, first, count));
    }

    static private ReOrder create(final SQLTable t, final Spec spec) {
        final SQLSystem system = t.getBase().getServer().getSQLSystem();
        if (system == SQLSystem.MYSQL) {
            return new ReOrderMySQL(t, spec);
        } else if (system == SQLSystem.POSTGRESQL)
            return new ReOrderPostgreSQL(t, spec);
        else if (system == SQLSystem.H2)
            return new ReOrderH2(t, spec);
        else
            throw new IllegalArgumentException(system + " not supported");
    }

    protected final SQLTable t;
    protected final Spec spec;

    protected ReOrder(final SQLTable t, final Spec spec) {
        this.t = t;
        if (!this.t.isOrdered())
            throw new IllegalArgumentException(t + " is not ordered");
        this.spec = spec;
    }

    protected final boolean isAll() {
        return this.spec == ALL;
    }

    protected final BigDecimal getFirstToReorder() {
        return this.spec.getFirstToReorder();
    }

    protected final BigDecimal getFirstOrderValue() {
        return this.spec.getFirst();
    }

    protected final String getWhere() {
        final Where w = this.spec.getWhere(null);
        return w == null ? "" : " where " + w;
    }

    protected final Where getWhere(final FieldRef f) {
        return this.spec.getWhere(f);
    }

    protected final String getInc() {
        return this.spec.getInc();
    }

    public abstract List<String> getSQL(Connection conn) throws SQLException;

    // MAYBE return affected IDs
    public final void exec() throws SQLException {
        final UpdateBuilder updateUndef = new UpdateBuilder(this.t).set(this.t.getOrderField().getName(), MIN_ORDER.toPlainString());
        updateUndef.setWhere(new Where(this.t.getKey(), "=", this.t.getUndefinedID()));
        SQLUtils.executeAtomic(this.t.getBase().getDataSource(), new ConnectionHandlerNoSetup<Object, SQLException>() {
            @Override
            public Object handle(SQLDataSource ds) throws SQLException, SQLException {
                final Connection conn = ds.getConnection();
                final Statement stmt = conn.createStatement();
                if (isAll()) {
                    // reorder all, undef must be at 0
                    stmt.execute(updateUndef.asString());
                }
                for (final String s : getSQL(conn)) {
                    stmt.execute(s);
                }
                // MAYBE fire only changed IDs
                ReOrder.this.t.fireTableModified(-1, Collections.singletonList(ReOrder.this.t.getOrderField().getName()));
                return null;
            }
        });
    }

    // *** specs

    static private class Some implements Spec {

        private final SQLTable t;
        private final BigDecimal firstToReorder;
        private final BigDecimal first;
        private final BigDecimal lastToReorder;

        public Some(final SQLTable t, final int first, final int count) {
            this.t = t;
            if (count <= 0)
                throw new IllegalArgumentException("Negative Count : " + count);
            // the row with MIN_ORDER cannot be displayed since no row can be moved before it
            // so don't change it
            if (BigDecimal.valueOf(first).compareTo(MIN_ORDER) <= 0) {
                this.firstToReorder = MIN_ORDER.add(t.getOrderULP());
                // make some room before the first non MIN_ORDER row so that another on can came
                // before it
                this.first = MIN_ORDER.add(DISTANCE);
            } else {
                this.firstToReorder = BigDecimal.valueOf(first);
                this.first = this.firstToReorder;
            }
            final BigDecimal simpleLastToReorder = this.firstToReorder.add(BigDecimal.valueOf(count));
            // needed since first can be different than firstToReorder
            this.lastToReorder = simpleLastToReorder.compareTo(this.first) > 0 ? simpleLastToReorder : this.first.add(DISTANCE.movePointRight(1));
            // OK since DISTANCE is a lot greater than the ULP of ORDRE
            assert this.getFirstToReorder().compareTo(this.getFirst()) <= 0 && this.getFirst().compareTo(this.getLast()) < 0 && this.getLast().compareTo(this.getLastToReorder()) <= 0;
        }

        @Override
        public final String getInc() {
            final SQLField oF = this.t.getOrderField();
            final SQLSyntax syntax = SQLSyntax.get(this.t.getServer().getSQLSystem());

            // last order of the whole table
            final SQLSelect selTableLast = new SQLSelect(this.t.getBase(), true);
            selTableLast.addSelect(oF, "MAX");

            // cast inc to order type to avoid truncation error
            final String avgDistance = " cast( " + getLast() + " - " + this.getFirst() + " as " + syntax.getOrderType() + " ) / ( count(*) -1)";
            // if the last order of this Spec is the last order of the table, we can use whatever
            // increment we want, we won't span over existing rows. This can be useful when
            // reordering densely packed rows, but this means that lastOrderValue won't be equal to
            // getLastToReorder().
            final String res = "CASE WHEN max(" + SQLBase.quoteIdentifier(oF.getName()) + ") = (" + selTableLast.asString() + ") then " + ALL.getInc() + " else " + avgDistance + " end";
            return res + " FROM " + this.t.getSQLName().quote() + " where " + this.getWhere(null).getClause();
        }

        @Override
        public final Where getWhere(FieldRef order) {
            if (order == null)
                order = this.t.getOrderField();
            else if (order.getField() != this.t.getOrderField())
                throw new IllegalArgumentException();
            return new Where(order, this.getFirstToReorder(), this.getLastToReorder());
        }

        @Override
        public final BigDecimal getFirstToReorder() {
            return this.firstToReorder;
        }

        private final BigDecimal getLastToReorder() {
            return this.lastToReorder;
        }

        @Override
        public BigDecimal getFirst() {
            return this.first;
        }

        public final BigDecimal getLast() {
            return this.getLastToReorder();
        }
    }

    static private Spec ALL = new Spec() {
        @Override
        public String getInc() {
            return String.valueOf(DISTANCE);
        }

        @Override
        public final Where getWhere(final FieldRef order) {
            return null;
        }

        @Override
        public BigDecimal getFirstToReorder() {
            return MIN_ORDER;
        }

        @Override
        public BigDecimal getFirst() {
            return getFirstToReorder();
        }
    };

    static interface Spec {
        String getInc();

        Where getWhere(final FieldRef order);

        // before reorder
        BigDecimal getFirstToReorder();

        // the first order value after reorder
        BigDecimal getFirst();
    }
}
