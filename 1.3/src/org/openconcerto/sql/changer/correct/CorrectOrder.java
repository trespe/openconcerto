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
 
 package org.openconcerto.sql.changer.correct;

import static java.util.Collections.singletonList;
import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.changer.convert.OrderToDecimal;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ReOrder;

import java.sql.SQLException;

public class CorrectOrder extends Changer<SQLTable> {

    private boolean force;

    public CorrectOrder(DBSystemRoot b) {
        super(b);
        this.force = false;
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        this.setForce(Boolean.getBoolean("org.openconcerto.sql.changer.force"));
    }

    public final boolean isForce() {
        return this.force;
    }

    public final void setForce(boolean force) {
        this.force = force;
    }

    protected void changeImpl(SQLTable table) throws SQLException {
        this.getStream().print(table.getName() + "... ");
        if (table.isOrdered()) {
            // fix type
            final OrderToDecimal orderToDecimal = new OrderToDecimal(getSystemRoot());
            orderToDecimal.setQuiet(true);
            orderToDecimal.change(table);

            final boolean isUnique = isOrderUnique(table);
            if (!this.isForce() && isUnique && noNulls(table)) {
                this.getStream().println("correctly ordered");
            } else {
                ReOrder.create(table).exec();
                this.getStream().print("order fixed, ");

                if (isUnique) {
                    this.getStream().println("already unique");
                } else {
                    final String order = table.getOrderField().getName();
                    final AlterTable alter = new AlterTable(table).addUniqueConstraint(table.getName() + "_order", singletonList(order));
                    getDS().execute(alter.asString());
                    table.getSchema().updateVersion();
                    this.getStream().println("unique on " + order + " added");
                }
            }
        } else {
            this.getStream().println("not ordered");
        }
    }

    // otherwise IListe bugs when sorting lines
    private boolean noNulls(SQLTable table) {
        final SQLSelect sel = new SQLSelect(table.getBase());
        sel.addSelectFunctionStar("count");
        sel.setWhere(new Where(table.getOrderField(), "is", (Object) null));
        return ((Number) getDS().executeScalar(sel.asString())).intValue() == 0;
    }

    // has table a unique constraint on the order field
    static private final boolean isOrderUnique(SQLTable table) throws SQLException {
        return table.getConstraint(ConstraintType.UNIQUE, singletonList(table.getOrderField().getName())) != null;
    }
}
