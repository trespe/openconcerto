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

import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;

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

    protected final int getFirst() {
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
        final Connection conn = this.t.getBase().getDataSource().getConnection();
        final UpdateBuilder updateUndef = new UpdateBuilder(this.t).set(this.t.getOrderField().getName(), "0");
        updateUndef.setWhere(new Where(this.t.getKey(), "=", this.t.getUndefinedID()));
        SQLUtils.executeAtomic(conn, new SQLFactory<Object>() {
            @Override
            public Object create() throws SQLException {
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
        private final int first;
        private final int count;

        public Some(final SQLTable t, final int first, final int count) {
            this.t = t;
            if (first <= 0)
                throw new IllegalArgumentException();
            this.first = first;
            if (count <= 0)
                throw new IllegalArgumentException();
            this.count = count;
        }

        @Override
        public final String getInc() {
            final SQLField oF = this.t.getOrderField();
            final SQLSyntax syntax = SQLSyntax.get(this.t.getServer().getSQLSystem());
            // cast inc to order type to avoid truncation error
            return SQLSelect.quote(" cast( ( max( %n ) - min( %n ) ) / ( count(*) -1) as " + syntax.getOrderType() + ") FROM %f where " + this.getWhere(null).getClause(), oF, oF, this.t);
        }

        @Override
        public final Where getWhere(FieldRef order) {
            if (order == null)
                order = this.t.getOrderField();
            else if (order.getField() != this.t.getOrderField())
                throw new IllegalArgumentException();
            return new Where(order, this.first, this.first + this.count);
        }

        @Override
        public int getFirst() {
            return this.first;
        }

    }

    static private Spec ALL = new Spec() {
        @Override
        public String getInc() {
            return "1";
        }

        @Override
        public final Where getWhere(final FieldRef order) {
            return null;
        }

        @Override
        public int getFirst() {
            return 0;
        }
    };

    static interface Spec {
        String getInc();

        Where getWhere(final FieldRef order);

        int getFirst();
    }
}
