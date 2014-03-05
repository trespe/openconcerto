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
 
 package org.openconcerto.erp.utils.correct;

import org.openconcerto.sql.changer.Changer;
import org.openconcerto.sql.model.ConnectionHandlerNoSetup;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.UpdateBuilder;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Renumber MOUVEMENT according to the date of its ECRITUREs.
 * 
 * @author Sylvain CUAZ
 */
public class RenumberMouvement extends Changer<DBRoot> {

    private Date dateMin;

    public RenumberMouvement(DBSystemRoot b) {
        super(b);
    }

    public final void setDateMin(Date dateMin) {
        this.dateMin = dateMin;
    }

    @Override
    public void setUpFromSystemProperties() {
        super.setUpFromSystemProperties();
        final String dateMin = System.getProperty("mvt.dateMin", "").trim();
        if (dateMin.length() > 0)
            this.setDateMin(Date.valueOf(dateMin));
    }

    @Override
    protected void changeImpl(DBRoot societeRoot) throws SQLException {
        final SQLTable ecritureT = societeRoot.getTable("ECRITURE");
        final SQLField dateF = ecritureT.getField("DATE");
        final SQLField mvtF = ecritureT.getField("ID_MOUVEMENT");
        final SQLTable mvtT = ecritureT.getForeignTable(mvtF.getName());

        // check undefined
        {
            final SQLSelect sel = new SQLSelect();
            sel.addSelectFunctionStar("count");
            sel.addFrom(ecritureT);
            sel.setWhere(new Where(mvtF, "=", mvtT.getUndefinedID()));
            final int count = ((Number) getDS().executeScalar(sel.asString())).intValue();
            if (count != 0)
                throw new IllegalStateException(count + " ecritures on undefined mouvement");
        }

        // check same date for all ecritures in one mouvement
        {
            final SQLSelect sel = new SQLSelect();
            // for each mixed mouvement, the ecriture count
            sel.addSelect(mvtF);
            sel.addSelectFunctionStar("count");
            sel.addFrom(ecritureT);
            sel.addGroupBy(mvtF);
            sel.setHaving(Where.createRaw("min(" + dateF.getFieldRef() + ") != max(" + dateF.getFieldRef() + ")", dateF));
            @SuppressWarnings("unchecked")
            final List<Number> mixedMvts = (List<Number>) getDS().executeCol(sel.asString());
            if (mixedMvts.size() > 0)
                throw new IllegalStateException(mixedMvts.size() + " mouvement(s) with mixed dates : " + mixedMvts);
        }

        final int firstNumber;
        if (this.dateMin == null) {
            firstNumber = 1;
        } else {
            // select all IDs before dateMin
            final SQLSelect selID = createSelect(dateF, mvtF, null);
            final SQLSelect selFirstNumber = new SQLSelect();
            selFirstNumber.addSelect(mvtT.getField("NUMERO"), "max");
            selFirstNumber.setWhere(Where.createRaw(mvtT.getKey().getFieldRef() + " in ( " + selID + " )"));
            firstNumber = ((Number) getDS().executeScalar(selFirstNumber.asString())).intValue() + 1;
        }

        final UpdateBuilder update = new UpdateBuilder(mvtT);
        update.addVirtualJoin("(" + createSelect(dateF, mvtF, firstNumber).asString() + ")", "m", "ID_MOUVEMENT");
        update.setFromVirtualJoinField("NUMERO", "m", "rn");

        final int updatedCount = getDS().useConnection(new ConnectionHandlerNoSetup<Integer, SQLException>() {
            @Override
            public Integer handle(final SQLDataSource ds) throws SQLException, SQLException {
                final Statement stmt = ds.getConnection().createStatement();
                final int res = stmt.executeUpdate(update.asString());
                stmt.close();
                return res;
            }
        });
        final String date = this.dateMin != null ? " at or after " + this.dateMin : "";
        getStream().println("Updated " + updatedCount + " rows" + date + " numbering from " + firstNumber);
    }

    protected SQLSelect createSelect(final SQLField dateF, final SQLField mvtF, final Integer offset) {
        final String func = "min";
        final String funcCall = func + "(" + dateF.getFieldRef() + ")";
        final SQLSelect sel = new SQLSelect();
        sel.addSelect(mvtF);
        if (offset != null) {
            sel.addSelect(dateF, func);
            // -1 since row_number() starts at 1
            sel.addRawSelect("row_number() OVER (ORDER BY " + funcCall + "," + mvtF.getFieldRef() + ") - 1 + " + offset, "rn");
        }
        sel.addGroupBy(mvtF);
        if (this.dateMin != null) {
            Where w = Where.createRaw(funcCall + " >= '" + this.dateMin.toString() + "'");
            if (offset == null)
                w = w.not();
            sel.setHaving(w);
        }
        return sel;
    }
}
