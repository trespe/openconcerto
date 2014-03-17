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
import java.util.Collection;

public class TotalCommandeClientProvider implements SpreadSheetCellValueProvider {

    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();
        Collection<? extends SQLRowAccessor> rows = row.getReferentRows(row.getTable().getTable("TR_COMMANDE_CLIENT"));
        long total = 0;
        for (SQLRowAccessor sqlRowAccessor : rows) {
            if (!sqlRowAccessor.isForeignEmpty("ID_COMMANDE_CLIENT")) {
                SQLRowAccessor rowCmd = sqlRowAccessor.getForeign("ID_COMMANDE_CLIENT");
                total += rowCmd.getLong("T_HT");
            }
        }
        return new BigDecimal(total).movePointLeft(2);
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("sales.account.command.total", new TotalCommandeClientProvider());
    }

}
