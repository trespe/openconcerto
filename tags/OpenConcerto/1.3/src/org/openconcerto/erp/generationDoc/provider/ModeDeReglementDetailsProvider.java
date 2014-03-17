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
 
 /*
 * Créé le 25 oct. 2012
 */
package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.erp.core.finance.payment.element.TypeReglementSQLElement;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProvider;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ModeDeReglementDetailsProvider implements SpreadSheetCellValueProvider {

    public ModeDeReglementDetailsProvider() {

    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();
        SQLRowAccessor rowMdR = row.getForeign("ID_MODE_REGLEMENT");
        if (rowMdR.getBoolean("COMPTANT")) {
            if (rowMdR.getInt("ID_TYPE_REGLEMENT") == TypeReglementSQLElement.CHEQUE) {
                String s = "Facture acquitée par v/règlement par chèque Banque ";
                String banque = rowMdR.getString("ETS");
                String num = rowMdR.getString("NUMERO");
                Calendar date = rowMdR.getDate("DATE");
                String du = "";
                if (date != null) {
                    DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                    du = " du " + format.format(date.getTime());
                }
                s += banque + " N°" + num + du;
                return s;
            }
        }
        return null;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("invoice.paiement.details", new ModeDeReglementDetailsProvider());

    }

}
