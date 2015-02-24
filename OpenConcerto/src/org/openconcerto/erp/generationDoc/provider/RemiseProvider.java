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

import org.openconcerto.erp.core.common.ui.Acompte;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.utils.GestionDevise;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RemiseProvider extends UserInitialsValueProvider {

    public enum TypeAffichage {
        GLOBAL, LIGNE, NORMAL
    };

    public final TypeAffichage type;

    public RemiseProvider(TypeAffichage t) {
        this.type = t;
    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();

        final BigDecimal montant = row.getBigDecimal("MONTANT_REMISE");
        BigDecimal remise = (BigDecimal) row.getObject("POURCENT_REMISE");

        Acompte a = new Acompte(remise, montant);

        if (a == null) {
            return null;
        } else if (a.getPercent() != null) {
            return a.getPercent().setScale(2, RoundingMode.HALF_UP).toString() + "%";
        } else if (montant != null) {
            return GestionDevise.currencyToString(montant);
        } else {
            return "";
        }
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("remise.global", new RemiseProvider(TypeAffichage.GLOBAL));
        SpreadSheetCellValueProviderManager.put("remise.line", new RemiseProvider(TypeAffichage.LIGNE));
        SpreadSheetCellValueProviderManager.put("remise", new RemiseProvider(TypeAffichage.NORMAL));
    }
}
