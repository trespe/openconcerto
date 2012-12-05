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
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.model.SQLRowAccessor;

public class AdresseFullClientValueProvider extends AdresseClientProvider {

    private int type;

    public AdresseFullClientValueProvider(int type) {
        this.type = type;
    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor r = getAdresse(context.getRow(), this.type);
        Ville v = Ville.getVilleFromVilleEtCode(r.getString("VILLE"));
        String result = r.getString("RUE");
        if (v != null) {
            result += "\n" + v.getCodepostal();
            result += " ";
            result += v.getName();
            if (r.getBoolean("HAS_CEDEX")) {
                result += " Cedex";
                String cedex = r.getString("CEDEX");
                if (cedex != null && cedex.trim().length() > 0) {
                    result += " " + cedex;
                }
            }
        }

        return result;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("address.customer.invoice.full", new AdresseFullClientValueProvider(ADRESSE_FACTURATION));
        SpreadSheetCellValueProviderManager.put("address.customer.shipment.full", new AdresseFullClientValueProvider(ADRESSE_LIVRAISON));
    }
}
