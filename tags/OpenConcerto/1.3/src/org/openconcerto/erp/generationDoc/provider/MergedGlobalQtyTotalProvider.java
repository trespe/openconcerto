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

import org.openconcerto.erp.core.common.ui.NumericFormat;
import org.openconcerto.erp.core.sales.product.element.UniteVenteArticleSQLElement;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;

import java.math.BigDecimal;

public class MergedGlobalQtyTotalProvider implements SpreadSheetCellValueProvider {

    private final boolean shortName;

    public MergedGlobalQtyTotalProvider(boolean shortName) {
        this.shortName = shortName;
    }

    public Object getValue(SpreadSheetCellValueContext context) {
        final SQLRowAccessor row = context.getRow();
        final BigDecimal pv = row.getBigDecimal("PV_HT");
        if (pv.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        final int qte = row.getInt("QTE");
        if (row.getInt("ID_UNITE_VENTE") == UniteVenteArticleSQLElement.A_LA_PIECE) {
            return String.valueOf(qte);
        }

        final BigDecimal qteUV = row.getBigDecimal("QTE_UNITAIRE");
        final BigDecimal mergedQty = qteUV.multiply(new BigDecimal(qte));
        String result = NumericFormat.getQtyDecimalFormat().format(mergedQty);

        final SQLRowAccessor rMode = row.getForeign("ID_UNITE_VENTE");
        result += " " + ((this.shortName) ? rMode.getString("CODE") : rMode.getString("NOM"));
        // 3 x 5.5 meters -> 16.5 meters
        return result;

    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("supplychain.element.qtyunit.merged", new MergedGlobalQtyTotalProvider(false));
        SpreadSheetCellValueProviderManager.put("supplychain.element.qtyunit.merged.short", new MergedGlobalQtyTotalProvider(true));
    }

}
