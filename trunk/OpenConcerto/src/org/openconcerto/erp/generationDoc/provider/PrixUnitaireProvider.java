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

import org.openconcerto.erp.core.sales.product.element.UniteVenteArticleSQLElement;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;

import java.math.BigDecimal;

public class PrixUnitaireProvider implements SpreadSheetCellValueProvider {

    public PrixUnitaireProvider() {
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
        final BigDecimal pvUnit = mergedQty.multiply(pv);
        return pvUnit;

    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("supplychain.element.unitprice", new PrixUnitaireProvider());
    }

}
