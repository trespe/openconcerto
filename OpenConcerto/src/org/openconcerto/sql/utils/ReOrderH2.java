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

import org.openconcerto.sql.model.AliasedField;
import org.openconcerto.sql.model.Order;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

final class ReOrderH2 extends ReOrder {

    public ReOrderH2(final SQLTable t, final Spec spec) {
        super(t, spec);
    }

    public List<String> getSQL(Connection conn) {
        final SQLField oF = this.t.getOrderField();

        final List<String> res = new ArrayList<String>();
        res.add("SET @inc to SELECT " + getInc() + ";");
        res.add(this.t.getBase().quote("UPDATE %f SET %n =  -%n " + this.getWhere(), this.t, oF, oF));

        final String alias = "T";
        final AliasedField tID = new AliasedField(this.t.getKey(), alias);
        final AliasedField tOrder = new AliasedField(oF, alias);
        // SELECT "T"."ID"
        // FROM "test"."test"."OBSERVATION" "T"
        // WHERE "T"."ORDRE" <= -2
        // ORDER BY "T"."ORDRE" DESC NULLS LAST, "T"."ID" ASC
        final SQLSelect idsToReorder = new SQLSelect(true);
        idsToReorder.addFrom(this.t, alias);
        idsToReorder.addSelect(tID);
        final Where updateNulls = this.isAll() ? new Where(tOrder, "is", (Object) null) : null;
        final Where w = new Where(tOrder, "<=", this.getFirstToReorder().negate()).or(updateNulls);
        idsToReorder.setWhere(w);
        idsToReorder.addFieldOrder(tOrder, Order.desc(), Order.nullsLast());
        idsToReorder.addFieldOrder(tID, Order.asc());
        // REORDER: ID => ORDRE
        res.add("DROP TABLE IF EXISTS REORDER ;");
        final String idName = this.t.getKey().getName();
        res.add("CREATE LOCAL TEMPORARY TABLE REORDER as select M.ID, " + this.getFirstOrderValue() + " +(M.ind - 1) * @inc as ORDRE from (\n" + "SELECT rownum() as ind, "
                + new SQLName(idName).quote() + " as ID from (" + idsToReorder.asString() + ") ) M;");

        res.add(this.t.getBase().quote("UPDATE %f %i SET %n = (\n" +
        //
                "        select M.ORDRE from REORDER M where M.ID = " + tID.getFieldRef() + ")\nwhere " + w.getClause() + ";", this.t, alias, oF));

        return res;
    }
}
