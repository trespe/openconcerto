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
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.request.UpdateBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

final class ReOrderPostgreSQL extends ReOrder {

    public ReOrderPostgreSQL(final SQLTable t, final Spec spec) {
        super(t, spec);
    }

    public List<String> getSQL(Connection conn) throws SQLException {
        // see http://www.depesz.com/index.php/2007/08/17/rownum-anyone-cumulative-sum-in-one-query/
        // http://explainextended.com/2009/05/05/postgresql-row-numbers/
        // see http://www.postgresql.org/docs/8.4/interactive/functions-window.html
        final SQLField oF = this.t.getOrderField();

        final List<String> res = new ArrayList<String>();

        final String alias = "T";
        final AliasedField tID = new AliasedField(this.t.getKey(), alias);
        final AliasedField tOrder = new AliasedField(oF, alias);
        // SELECT "T"."ID" as "ID"
        // FROM "test"."test"."OBSERVATION" "T"
        // WHERE "T"."ORDRE" between 12 and 25
        // ORDER BY "T"."ORDRE" ASC NULLS LAST, "T"."ID" ASC
        final SQLSelect idsToReorder = new SQLSelect(this.t.getBase(), true);
        idsToReorder.addFrom(this.t, alias);
        idsToReorder.addSelect(tID, null, "ID");
        idsToReorder.setWhere(this.getWhere(tOrder));
        // NULLS LAST needs at least 8.3, but 8.2 is sorting nulls high so no need to specify it
        final String orderDir = conn.getMetaData().nullsAreSortedHigh() ? " ASC" : " ASC NULLS LAST";
        idsToReorder.addRawOrder(tOrder.getFieldRef() + orderDir);
        idsToReorder.addRawOrder(tID.getFieldRef() + " ASC");

        res.add("CREATE TEMP SEQUENCE \"reorderSeq\" MINVALUE 0;");
        // REORDER: ID => INDEX
        res.add("CREATE LOCAL TEMPORARY TABLE REORDER as select M.\"ID\", nextval('\"reorderSeq\"') as index from (\n" + idsToReorder.asString() + ") M;");
        res.add("DROP SEQUENCE \"reorderSeq\";");

        res.add("create local temp table inc(val) as select " + getInc() + ";");
        // remove if using deferrable uniqueness constraints
        res.add(this.t.getBase().quote("UPDATE %f SET %n =  -%n " + this.getWhere(), this.t, oF, oF) + ";");

        final UpdateBuilder update = new UpdateBuilder(this.t);
        update.addTable("REORDER", "M");
        update.addTable("inc", null);
        update.set(oF.getName(), "M.index * inc.val + " + getFirstOrderValue());
        res.add(update.asString() + " where M.\"ID\" = " + this.t.getKey().getFieldRef() + ";");

        // drop tables so we can reorder more than once in the same transaction
        res.add("DROP TABLE REORDER, inc ;");

        return res;
    }
}
