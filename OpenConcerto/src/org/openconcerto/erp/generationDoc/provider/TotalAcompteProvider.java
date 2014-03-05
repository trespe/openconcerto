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
 
 package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TotalAcompteProvider implements SpreadSheetCellValueProvider {

    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();
        Calendar c = row.getDate("DATE");

        Collection<? extends SQLRowAccessor> rows = row.getReferentRows(row.getTable().getTable("TR_COMMANDE_CLIENT"));
        long total = 0;
        Set<SQLRowAccessor> facture = new HashSet<SQLRowAccessor>();
        facture.add(row);
        for (SQLRowAccessor sqlRowAccessor : rows) {
            total += getPreviousAcompte(sqlRowAccessor.getForeign("ID_COMMANDE_CLIENT"), facture, c);
        }

        return new BigDecimal(total).movePointLeft(2);
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("sales.account.total", new TotalAcompteProvider());
    }

    public long getPreviousAcompte(SQLRowAccessor sqlRowAccessor, Set<SQLRowAccessor> alreadyAdded, Calendar c) {
        if (sqlRowAccessor == null || sqlRowAccessor.isUndefined()) {
            return 0L;
        }
        Collection<? extends SQLRowAccessor> rows = sqlRowAccessor.getReferentRows(sqlRowAccessor.getTable().getTable("TR_COMMANDE_CLIENT"));
        long l = 0;
        for (SQLRowAccessor sqlRowAccessor2 : rows) {
            SQLRowAccessor rowFact = sqlRowAccessor2.getForeign("ID_SAISIE_VENTE_FACTURE");

            if (rowFact != null && !rowFact.isUndefined() && !alreadyAdded.contains(rowFact) && rowFact.getDate("DATE").before(c)) {
                alreadyAdded.add(rowFact);
                l += rowFact.getLong("T_HT");
            }
        }
        return l;
    }

}
