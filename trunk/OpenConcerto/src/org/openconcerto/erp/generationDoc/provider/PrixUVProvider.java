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
import java.text.DecimalFormat;

public class PrixUVProvider implements SpreadSheetCellValueProvider {

    private final boolean shortName;
    private final DecimalFormat decimalFormat = new DecimalFormat("##,##0.00#");

    public PrixUVProvider(boolean shortName) {
        this.shortName = shortName;
    }

    public Object getValue(SpreadSheetCellValueContext context) {
        final SQLRowAccessor row = context.getRow();
        final BigDecimal pv = row.getBigDecimal("PV_HT");
        if (pv.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        String result = decimalFormat.format(pv);
        final SQLRowAccessor rMode = row.getForeign("ID_UNITE_VENTE");
        result += "€ / " + ((this.shortName) ? rMode.getString("CODE") : rMode.getString("NOM"));
        return result;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("supplychain.element.salesunit.price", new PrixUVProvider(false));
        SpreadSheetCellValueProviderManager.put("supplychain.element.salesunit.price.short", new PrixUVProvider(true));
    }

}
