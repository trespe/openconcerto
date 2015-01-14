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

public class FormatedGlobalQtyTotalProvider implements SpreadSheetCellValueProvider {

    private final boolean shortName;

    private static enum Type {
        NORMAL, SHIPMENT
    }

    private final Type type;

    private FormatedGlobalQtyTotalProvider(Type t, boolean shortName) {
        this.shortName = shortName;
        this.type = t;
    }

    public Object getValue(SpreadSheetCellValueContext context) {
        final SQLRowAccessor row = context.getRow();
        final BigDecimal pv = row.getBigDecimal("PV_HT");
        if (pv.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        final int qte = row.getInt(this.type == Type.NORMAL ? "QTE" : "QTE_LIVREE");

        if (row.getInt("ID_UNITE_VENTE") == UniteVenteArticleSQLElement.A_LA_PIECE) {
            return String.valueOf(qte);
        }
        String result = "";
        if (qte > 0) {
            if (qte > 1) {
                result += qte + " x ";
            }
            final BigDecimal qteUV = row.getBigDecimal("QTE_UNITAIRE");

            result += NumericFormat.getQtyDecimalFormat().format(qteUV);
            final SQLRowAccessor rMode = row.getForeign("ID_UNITE_VENTE");
            result += " " + ((this.shortName) ? rMode.getString("CODE") : rMode.getString("NOM"));
            // 3 x 5.5 meters
            // 1 x 6.3 meters -> 6.3 meters
        }
        return result;

    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("supplychain.element.qtyunit.short", new FormatedGlobalQtyTotalProvider(Type.NORMAL, true));
        SpreadSheetCellValueProviderManager.put("supplychain.element.qtyunit", new FormatedGlobalQtyTotalProvider(Type.NORMAL, false));
        SpreadSheetCellValueProviderManager.put("supplychain.element.qtyunit.deliver.short", new FormatedGlobalQtyTotalProvider(Type.SHIPMENT, true));
        SpreadSheetCellValueProviderManager.put("supplychain.element.qtyunit.deliver", new FormatedGlobalQtyTotalProvider(Type.SHIPMENT, false));
    }
}
